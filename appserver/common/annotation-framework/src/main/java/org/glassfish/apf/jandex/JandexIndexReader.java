/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.apf.jandex;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.deploy.shared.JarArchive;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.apf.impl.AnnotationUtils;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.deployment.JandexIndexer;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jvnet.hk2.annotations.Service;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import static org.glassfish.internal.deployment.Deployment.DEPLOYMENT_SUCCESS;

@Service
public class JandexIndexReader implements JandexIndexer, EventListener {
    private static final String JANDEX_INDEX_METADATA_KEY = JandexIndexer.class.getName() + ".index";

    @Inject
    ArchiveFactory archiveFactory;
    @Inject
    ServerEnvironment serverEnvironment;
    @Inject
    Events events;
    @Inject
    PayaraExecutorService payaraExecutorService;

    private static final Lock errorsLock = new ReentrantLock();
    private static final Queue<Runnable> delayedCacheAdditions = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void init() {
        events.register(this);
    }

    @Override
    public void index(DeploymentContext deploymentContext) throws IOException {
        if (getIndexMap(deploymentContext) == null) {
            Map<String, Index> indexMap = new ConcurrentHashMap<>();
            deploymentContext.addTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, indexMap);
            indexMap.put(deploymentContext.getSource().getURI().toString(), indexArchive(deploymentContext));
            indexSubArchives(deploymentContext, true);
            if (DeploymentUtils.useWarLibraries(deploymentContext)) {
                DeploymentUtils.getWarLibraryCache().keySet()
                        .forEach(path -> getOrCreateIndex(deploymentContext, Path.of(path).toUri()));
            }
        }
    }

    @Override
    public void reindex(DeploymentContext deploymentContext) throws IOException {
        deploymentContext.removeTransientAppMetaData(JANDEX_INDEX_METADATA_KEY);
        index(deploymentContext);
    }

    @Override
    public Index getRootIndex(DeploymentContext deploymentContext) {
        return getIndexMap(deploymentContext).get(deploymentContext.getSource().getURI().toString());
    }

    @Override
    public Map<String, Index> getAllIndexes(DeploymentContext deploymentContext) {
        return getIndexMap(deploymentContext);
    }

    @Override
    public Map<String, Index> getIndexesByURI(DeploymentContext deploymentContext, Collection<URI> uris) {
        Map<String, Index> result = new HashMap<>();
        uris.forEach(uri ->  result.put(uri.toString(), getOrCreateIndex(deploymentContext, uri)));
        return result;
    }

    @Override
    public boolean isJakartaEEApplication(DeploymentContext deploymentContext) throws IOException {
        // Check if the application contains any Jakarta EE specific annotations or classes
        return getRootIndex(deploymentContext).getKnownClasses().stream()
                .flatMap(clazz -> clazz.annotations().stream())
                .anyMatch(annotation -> annotation.name().toString().startsWith("jakarta."));
    }

    @Override
    public boolean hasAnyAnnotations(DeploymentContext deploymentContext, List<URI> uris, String... annotations) {
        var indexMap = getIndexMap(deploymentContext);
        if (indexMap == null) {
            return false;
        }
        for (URI uri : uris) {
            Index index = indexMap.get(uri.toString());
            if (index != null) {
                for (String annotation : annotations) {
                    if (!index.getAnnotations(annotation).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Index> getIndexMap(DeploymentContext deploymentContext) {
        return deploymentContext.getTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, Map.class);
    }

    Index getIndexFromArchive(ReadableArchive archive) throws IOException {
        Index index = readIndex(archive, "META-INF/jandex.idx");
        if (index == null) {
            index = readIndex(archive, "WEB-INF/classes/META-INF/jandex.idx");
        }
        return index;
    }

    private Index getOrCreateIndex(DeploymentContext deploymentContext, URI uri) {
        Map<String, Index> indexMap = getIndexMap(deploymentContext);
        return indexMap.computeIfAbsent(uri.toString(), key -> {
            try {
                DeploymentUtils.WarLibraryDescriptor descriptor = DeploymentUtils.getWarLibraryCache().get(uri.getPath());
                return descriptor != null ? descriptor.getIndex() : indexOrGetFromCache(deploymentContext, archiveFactory.openArchive(uri));
            } catch (IOException e) {
                return null;
            }
        });
    }

    private Index readIndex(ReadableArchive archive, String path) throws IOException {
        try (InputStream stream = archive.getEntry(path)) {
            if (stream != null) {
                return new IndexReader(stream).read();
            }
        }
        return null;
    }

    private Index indexArchive(DeploymentContext deploymentContext) throws IOException {
        Index index = getIndexFromArchive(deploymentContext.getSource());
        if (index == null) {
            index = indexSubArchives(deploymentContext, false);
        }
        return index;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private Index indexSubArchives(DeploymentContext deploymentContext, boolean indexSubArchivesOnly) throws IOException {
        getCachePath(deploymentContext.getSource(), "none").getParent().toFile().mkdirs();
        return indexArchive(deploymentContext, deploymentContext.getSource(), indexSubArchivesOnly);
    }

    private Index indexArchive(DeploymentContext context, ReadableArchive archive,
                               boolean indexSubArchivesOnly) throws IOException {
        Indexer indexer = new Indexer();
        StringBuilder errors = new StringBuilder();
        List<Callable<Void>> innerJarTasks = new ArrayList<>();

        archive.entries().asIterator().forEachRemaining(entry -> {
            // we need to check that there is no exploded directory by this name.
            String explodedName = entry
                    .replace(".jar", "_jar")
                    .replace(".war", "_war")
                    .replace(".rar", "_rar");
            int slashIndex = explodedName.indexOf('/');
            if (slashIndex != -1 && (entry.endsWith(".jar") || entry.endsWith(".war") || entry.endsWith(".rar"))) {
                explodedName = explodedName.substring(0, slashIndex + 1);
            }
            try {
                boolean explodedNameExists = archive.exists(explodedName);
                if (!indexSubArchivesOnly && entry.endsWith(".class")) {
                    try (InputStream stream = archive.getEntry(entry)) {
                        if (stream != null) {
                            indexer.index(stream);
                        }
                    }
                }
                if (!explodedNameExists || indexSubArchivesOnly && entry.endsWith(".jar")) {
                    innerJarTasks.add(() -> indexArchiveInnerJAR(context, archive, entry, errors));
                } else if (indexSubArchivesOnly && entry.endsWith(".war") || entry.endsWith(".rar")) {
                    innerJarTasks.add(() -> indexArchiveInnerJAR(context, archive, entry, errors));
                }
            } catch (IOException e) {
                appendError(archive, entry, errors);
            }
        });
        ForkJoinPool.commonPool().invokeAll(innerJarTasks);
        if (errors.length() > 0) {
            throw new IOException(errors.toString());
        }
        return indexer.complete();
    }

    private static void appendError(ReadableArchive archive, String entry, StringBuilder errors) {
        errorsLock.lock();
        try {
            if (errors.length() == 0) {
                errors.append(String.format("Unable to index %s from archive %s ", entry, archive.getName()));
            }
        } finally {
            errorsLock.unlock();
        }
    }

    private Void indexArchiveInnerJAR(DeploymentContext context, ReadableArchive archive, String entry, StringBuilder errors) {
        try {
            ReadableArchive subArchive = archive.getSubArchive(entry);
            if (subArchive != null) {
                Index index = indexOrGetFromCache(context, subArchive);
                getIndexMap(context).put(subArchive.getURI().toString(), index);
            }
        } catch (IOException e) {
            appendError(archive, entry, errors);
        }
        return null;
    }

    private Index indexOrGetFromCache(DeploymentContext context, ReadableArchive subArchive) throws IOException {
        String subArchivePath = subArchive.getURI().getPath();
        if (subArchivePath.endsWith(".jar") || subArchivePath.endsWith("_war/") || subArchivePath.endsWith("_rar/")) {
            Index index;
            boolean isExploded = subArchivePath.endsWith("/");
            if (isExploded) {
                index = getIndexFromArchive(subArchive);
            } else {
                index = getCachedIndex(subArchive);
            }
            if (index == null) {
                index = indexArchive(context, subArchive, false);
                if (!isExploded && !subArchivePath.endsWith("-SNAPSHOT.jar")) {
                    var finalIndex = index;
                    delayedCacheAdditions.offer(() -> cacheIndex(subArchive, finalIndex));
                }
            }
            return index;
        } else {
            return getRootIndex(context);
        }
    }

    private Index getCachedIndex(ReadableArchive archive) throws IOException {
        if (!getCachePath(archive, "size").toFile().exists()
                || !getCachePath(archive, "crc").toFile().exists()) {
            return null;
        }
        if (Long.parseLong(Files.readString(getCachePath(archive, "size"))) != archive.getArchiveSize()) {
            return null;
        }
        if (Long.parseLong(Files.readString(getCachePath(archive, "crc"))) != getChecksum(archive)) {
            return null;
        }
        try (var stream = new GZIPInputStream(Files.newInputStream(getCachePath(archive,"idx")))) {
            return new IndexReader(stream).read();
        }
    }

    private void cacheIndex(ReadableArchive archive, Index index) {
        try (var stream = new GZIPOutputStream(Files.newOutputStream(getCachePath(archive, "idx")))) {
            IndexWriter indexWriter = new IndexWriter(stream);
            indexWriter.write(index);
            Files.writeString(getCachePath(archive, "crc"), String.valueOf(getChecksum(archive)));
            Files.writeString(getCachePath(archive, "size"), String.valueOf(archive.getArchiveSize()));
        } catch (IOException e) {
            AnnotationUtils.getLogger().log(Level.WARNING, e, () -> "Failed to cache Jandex index for " + archive.getName());
        }
    }

    private long getChecksum(ReadableArchive archive) {
        long crc = 0;
        if (archive instanceof JarArchive) {
            var jarArchive = (JarArchive) archive;
            crc = jarArchive.getArchiveCrc();
        }
        return crc;
    }

    private Path getCachePath(ReadableArchive archive, String suffix) {
        return Path.of(serverEnvironment.getInstanceRoot() + "/jandex-cache/" + archive.getName() + "." + suffix);
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(DEPLOYMENT_SUCCESS)) {
            // drain the queue of delayed cache additions
            while (!delayedCacheAdditions.isEmpty()) {
                payaraExecutorService.submit(delayedCacheAdditions.poll());
            }
        }
    }
}
