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
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.deployment.DeploymentTracing;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jboss.jandex.Indexer;
import org.jvnet.hk2.annotations.Service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import static org.glassfish.internal.deployment.Deployment.DEPLOYMENT_SUCCESS;

@Service
public class JandexIndexReader implements JandexIndexer, EventListener {
    private static final String JANDEX_INDEX_METADATA_KEY = JandexIndexer.class.getName() + ".index";
    private static final Pattern ARCHIVE_EXTENSION_PATTERN = Pattern.compile("\\.(?=\\war$)");

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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void index(DeploymentContext deploymentContext) throws IOException {
        StructuredDeploymentTracing tracing = StructuredDeploymentTracing.load(deploymentContext);
        try (var span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, deploymentContext.getSource().getName())) {
            getCachePath(deploymentContext.getSource().getName(), "none").getParent().toFile().mkdirs();

            if (getIndexMap(deploymentContext) == null) {
                Map<String, Index> indexMap = new ConcurrentHashMap<>();
                deploymentContext.addTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, indexMap);
                indexMap.put(deploymentContext.getSource().getURI().toString(),
                        indexRootArchive(deploymentContext, tracing));
                if (DeploymentUtils.useWarLibraries(deploymentContext)) {
                    DeploymentUtils.getWarLibraryCache().keySet()
                            .forEach(path -> getOrCreateIndex(deploymentContext, Path.of(path).toUri(), tracing));
                }
                indexExternalLibraries(deploymentContext, tracing);
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
        uris.forEach(uri ->  result.put(uri.toString(), getOrCreateIndex(deploymentContext, uri,
                StructuredDeploymentTracing.createDisabled(uri.toString()))));
        return result;
    }

    @Override
    public boolean isJakartaEEApplication(DeploymentContext deploymentContext) throws IOException {
        // Check if the application contains any Jakarta EE specific annotations or classes
        // TODO: doesn't work with EAR quite yet, root deployment for EARs is wrong
        return getRootIndex(deploymentContext).getIndex().getKnownClasses().stream()
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
                    if (!index.getIndex().getAnnotations(annotation).isEmpty()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Index> getIndexMap(DeploymentContext deploymentContext) {
        return deploymentContext.getTransientAppMetaData(JANDEX_INDEX_METADATA_KEY, Map.class);
    }

    Index getIndexFromArchive(ReadableArchive archive) throws IOException {
        Index index = readIndex(archive, "META-INF/jandex.idx");
        if (index == null) {
            index = readIndex(archive, "WEB-INF/classes/META-INF/jandex.idx");
        }
        return index;
    }

    private Index getOrCreateIndex(DeploymentContext deploymentContext, URI uri, StructuredDeploymentTracing tracing) {
        if (isEndsWithSlash(uri.toString())) {
            return getRootIndex(deploymentContext);
        }
        Map<String, Index> indexMap = getIndexMap(deploymentContext);
        return indexMap.computeIfAbsent(uri.toString(), key -> {
            try {
                DeploymentUtils.WarLibraryDescriptor descriptor = DeploymentUtils.getWarLibraryCache().get(uri.getPath());
                return descriptor != null ? descriptor.getIndex() : indexOrGetFromCacheWithSpan(archiveFactory.openArchive(uri), tracing);
            } catch (IOException | ClassNotFoundException e) {
                return null;
            }
        });
    }

    private Index indexOrGetFromCacheWithSpan(ReadableArchive archive,
                                              StructuredDeploymentTracing tracing) throws IOException, ClassNotFoundException {
        try (var span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, archive.getName())) {
            return indexOrGetFromCache(archive);
        }
    }

    private static boolean isEndsWithSlash(String path) {
        return path.endsWith(File.separator) || path.endsWith("/");
    }

    private Index indexRootArchive(DeploymentContext deploymentContext, StructuredDeploymentTracing tracing) throws IOException {
        Index index = getIndexFromArchive(deploymentContext.getSource());
        ArrayList<String> subArchives = new ArrayList<>();
        if (index == null) {
            index = indexArchive(deploymentContext.getSource(), subArchives);
        } else {
            filterEntries(deploymentContext.getSource(), JandexIndexReader::checkIfSubArchive, subArchives);
        }
        indexSubArchives(deploymentContext, deploymentContext.getSource(), subArchives, tracing);
        return index;
    }

    private void indexSubArchives(DeploymentContext deploymentContext, ReadableArchive rootArchive,
                                  List<String> subArchives, StructuredDeploymentTracing tracing) throws IOException {
        List<Callable<Void>> innerJarTasks = new ArrayList<>();
        StringBuilder errors = new StringBuilder();
        subArchives.forEach(subArchive -> innerJarTasks.add(
                () -> indexSubArchive(deploymentContext, rootArchive, subArchive, errors, tracing)));
        ForkJoinPool.commonPool().invokeAll(innerJarTasks);
        if (errors.length() > 0) {
            throw new IOException(errors.toString());
        }
    }

    private Index indexArchive(ReadableArchive archive, List<String> innerArchives) throws IOException {
        Indexer indexer = new Indexer();
        StringBuilder errors = new StringBuilder();

        filterEntries(archive, (entry, performIndex) -> {
            try {
                if (performIndex && entry.endsWith(".class")) {
                    try (InputStream stream = archive.getEntry(entry)) {
                        if (stream != null) {
                            indexer.index(stream);
                        }
                    }
                }
            } catch (IOException e) {
                appendError(archive, entry, errors);
            }
            return checkIfSubArchive(entry, performIndex);
        }, innerArchives);

        if (errors.length() > 0) {
            throw new IOException(errors.toString());
        }
        return new Index(indexer.complete());
    }

    private static void filterEntries(ReadableArchive archive, BiPredicate<String, Boolean> filter,
                                      List<String> entries) {
        Set<String> explodedEntries = new HashSet<>();
        archive.entries().asIterator().forEachRemaining(entry -> {
            String explodedEntry = toExplodedName(entry);
            if (explodedEntry != null && archive.isDirectory(explodedEntry)) {
                explodedEntries.add(explodedEntry);
            }
            if (filter.test(entry, !startsWithAny(entry, explodedEntries))) {
                entries.add(entry);
            }
        });
    }

    private static boolean startsWithAny(String entry, Set<String> explodedEntries) {
        return explodedEntries.stream().anyMatch(entry::startsWith);
    }

    private static String toExplodedName(String entry) {
        Matcher matcher = ARCHIVE_EXTENSION_PATTERN.matcher(entry);
        return matcher.find() ? matcher.replaceAll("_") : null;
    }

    private static boolean checkIfSubArchive(String entry, boolean doit) {
        return ARCHIVE_EXTENSION_PATTERN.matcher(entry).find();
    }

    private Void indexSubArchive(DeploymentContext context, ReadableArchive archive,
                                 String entry, StringBuilder errors, StructuredDeploymentTracing tracing) {
        try (ReadableArchive subArchive = archive.getSubArchive(entry);
             var span = subArchive != null
                     ? tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, subArchive.getName()) : null) {
            if (subArchive != null) {
                Index index = indexOrGetFromCache(subArchive);
                getIndexMap(context).put(subArchive.getURI().toString(), index);
            }
        } catch (IOException | ClassNotFoundException e) {
            appendError(archive, entry, errors);
        }
        return null;
    }

    private Index indexOrGetFromCache(ReadableArchive subArchive) throws IOException, ClassNotFoundException {
        String subArchivePath = subArchive.getURI().getPath();
        Index index = getIndexFromArchive(subArchive);
        if (index == null) {
            index = getCachedIndex(subArchive);
        }
        if (index == null) {
            List<String> innerArchives = new ArrayList<>();
            index = indexArchive(subArchive, innerArchives);
            if (!subArchivePath.endsWith("-SNAPSHOT.jar")
                    && !subArchivePath.endsWith("-SNAPSHOT-tests.jar")
                    && !isEndsWithSlash(subArchivePath)) {
                var finalIndex = index;
                var archiveName = subArchive.getName();
                var archiveSize = subArchive.getArchiveSize();
                var archiveChecksum = getChecksum(subArchive);
                delayedCacheAdditions.offer(() -> cacheIndex(archiveName, archiveSize, archiveChecksum, finalIndex));
            }
        }
        return index;
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

    private Index getCachedIndex(ReadableArchive archive) throws IOException, ClassNotFoundException {
        if (!getCachePath(archive.getName(), "idx").toFile().exists()) {
            return null;
        }
        try (var stream = new GZIPInputStream(Files.newInputStream(getCachePath(archive.getName(),"idx")))) {
            var objectInputStream = new ObjectInputStream(stream);
            long archiveSize = objectInputStream.readLong();
            long archiveChecksum = objectInputStream.readLong();
            if (archiveSize != archive.getArchiveSize() || archiveChecksum != getChecksum(archive)) {
                return null;
            }
            Index index = (Index) objectInputStream.readObject();
            index.setIndex(new IndexReader(stream).read());
            return index;
        }
    }

    private void cacheIndex(String archiveName, long archiveSize, long archiveChecksum, Index index) {
        try (var stream = new GZIPOutputStream(Files.newOutputStream(getCachePath(archiveName, "idx")))) {
            var objectOutputStream = new ObjectOutputStream(stream);
            objectOutputStream.writeLong(archiveSize);
            objectOutputStream.writeLong(archiveChecksum);
            objectOutputStream.writeObject(index);
            IndexWriter indexWriter = new IndexWriter(stream);
            indexWriter.write(index.getIndex());
        } catch (IOException e) {
            AnnotationUtils.getLogger().log(Level.WARNING, e, () -> "Failed to cache Jandex index for " + archiveName);
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

    private Path getCachePath(String archiveName, String suffix) {
        return Path.of(serverEnvironment.getInstanceRoot() + "/jandex-cache/" + archiveName + "." + suffix);
    }

    private Index readIndex(ReadableArchive archive, String path) throws IOException {
        try (InputStream stream = archive.getEntry(path)) {
            if (stream != null) {
                return new Index(new IndexReader(stream).read());
            }
        }
        return null;
    }

    private void indexExternalLibraries(DeploymentContext deploymentContext,
                                        StructuredDeploymentTracing tracing) throws IOException {
        boolean skipScanExternalLibProp = Boolean.valueOf(deploymentContext.getAppProps()
                .getProperty(DeploymentProperties.SKIP_SCAN_EXTERNAL_LIB));
        for (URI externalLib : getExternalLibraries(skipScanExternalLibProp, deploymentContext)) {
                getOrCreateIndex(deploymentContext, externalLib, tracing);
        }
    }

    private List<URI> getExternalLibraries(boolean skipScanExternalLibProp,
                                           DeploymentContext deploymentContext) {
        if (skipScanExternalLibProp) {
            // if we skip scanning external libraries, we should just
            // return an empty list here
            return Collections.emptyList();
        }
        try {
            return Stream.concat(DeploymentUtils.getExternalLibraries(deploymentContext.getSource()).stream(),
                    deploymentContext.getAppLibs().stream()).collect(Collectors.toList());
        } catch (URISyntaxException e) {
            AnnotationUtils.getLogger().log(Level.WARNING, e, () -> "Failed to get external libraries");
            return Collections.emptyList();
        }
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
