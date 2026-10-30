/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025-2026] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package com.sun.enterprise.v3.server;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.deploy.shared.InputJarArchive;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.DeploymentTracing;
import org.glassfish.internal.deployment.JandexIndexer;
import org.glassfish.internal.deployment.analysis.DeploymentSpan;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jboss.jandex.IndexReader;
import org.jboss.jandex.IndexWriter;
import org.jvnet.hk2.annotations.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Builds the Jandex indexes of a deployment, see {@link JandexIndexer}.
 * <p>
 * The root archive is indexed together with all nested archives (jars in WEB-INF/lib, EAR
 * modules and libraries), the nested ones in parallel. Indexes of library jars are cached under
 * {@code <instance-root>/jandex-cache}, keyed by jar name, size and checksum; cache files
 * are written once the deployment has succeeded.
 */
@Service
public class JandexIndexerImpl implements JandexIndexer, EventListener {

    private static final Logger LOGGER = KernelLoggerInfo.getLogger();
    private static final String INDEX_METADATA_KEY = JandexIndexer.class.getName() + ".indexes";
    private static final String CACHE_DIRECTORY = "jandex-cache";
    private static final String CACHE_SUFFIX = ".idx.gz";
    private static final String PREBUILT_INDEX = "META-INF/jandex.idx";
    /** nested archive entries */
    private static final Pattern ARCHIVE_ENTRY = Pattern.compile("\\.([jwr]ar)$");

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private Events events;

    @Inject
    private PayaraExecutorService executorService;

    private final Queue<Runnable> pendingCacheWrites = new ConcurrentLinkedQueue<>();

    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void index(DeploymentContext deploymentContext) throws IOException {
        if (deploymentContext.getTransientAppMetaData(INDEX_METADATA_KEY, Map.class) != null) {
            return;
        }
        Map<String, Index> indexes = new ConcurrentHashMap<>();
        deploymentContext.addTransientAppMetaData(INDEX_METADATA_KEY, indexes);

        ReadableArchive source = deploymentContext.getSource();
        StructuredDeploymentTracing tracing = StructuredDeploymentTracing.load(deploymentContext);
        try (DeploymentSpan span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, source.getName())) {
            indexRootArchive(source, indexes, tracing);
        }
        if (DeploymentUtils.useWarLibraries(deploymentContext)) {
            for (String warLibrary : DeploymentUtils.getWarLibraryCache().keySet()) {
                resolve(deploymentContext, indexes, Path.of(warLibrary).toUri(), tracing);
            }
        }
        if (!Boolean.parseBoolean(deploymentContext.getAppProps().getProperty(DeploymentProperties.SKIP_SCAN_EXTERNAL_LIB))) {
            try {
                for (URI externalLibrary : DeploymentUtils.getExternalLibraries(source)) {
                    resolve(deploymentContext, indexes, externalLibrary, tracing);
                }
                for (URI externalLibrary : deploymentContext.getAppLibs()) {
                    resolve(deploymentContext, indexes, externalLibrary, tracing);
                }
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, e, () -> "Unable to determine the external libraries of " + source.getName());
            }
        }
    }

    @Override
    public Index getRootIndex(DeploymentContext deploymentContext) {
        Map<String, Index> indexes = findIndexes(deploymentContext);
        return indexes == null ? null : indexes.get(key(deploymentContext.getSource().getURI()));
    }

    @Override
    public Map<String, Index> getAllIndexes(DeploymentContext deploymentContext) {
        Map<String, Index> indexes = findIndexes(deploymentContext);
        return indexes == null ? Collections.emptyMap() : Collections.unmodifiableMap(indexes);
    }

    @Override
    public Index getIndex(DeploymentContext deploymentContext, URI archive) {
        Map.Entry<String, Index> resolved = resolve(deploymentContext, indexes(deploymentContext), archive, null);
        return resolved == null ? null : resolved.getValue();
    }

    @Override
    public Map<String, Index> getIndexesByURI(DeploymentContext deploymentContext, Collection<URI> archives) {
        Map<String, Index> indexes = indexes(deploymentContext);
        Map<String, Index> result = new LinkedHashMap<>();
        for (URI archive : archives) {
            Map.Entry<String, Index> resolved = resolve(deploymentContext, indexes, archive, null);
            if (resolved != null) {
                result.put(resolved.getKey(), resolved.getValue());
            }
        }
        return result;
    }

    @Override
    public boolean hasAnyAnnotations(DeploymentContext deploymentContext, Collection<URI> archives, String... annotationNames) {
        Map<String, Index> indexes = findIndexes(deploymentContext);
        if (indexes == null) {
            return false;
        }
        for (URI archive : archives) {
            Index index = indexes.get(key(archive));
            if (index != null && index.hasAnnotations(annotationNames)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void event(Event<?> event) {
        if (event.is(Deployment.DEPLOYMENT_SUCCESS)) {
            for (Runnable write; (write = pendingCacheWrites.poll()) != null;) {
                executorService.submit(write);
            }
        }
    }

    private void indexRootArchive(ReadableArchive source, Map<String, Index> indexes,
                                  StructuredDeploymentTracing tracing) throws IOException {
        List<String> entries = Collections.list(source.entries());
        Set<String> subArchives = new LinkedHashSet<>();
        // modules expanded next to their archive are indexed through the archive
        Set<String> expandedEntries = new HashSet<>();
        for (String entry : entries) {
            Matcher matcher = ARCHIVE_ENTRY.matcher(entry);
            if (matcher.find()) {
                subArchives.add(entry);
                String expanded = entry.substring(0, matcher.start()) + "_" + matcher.group(1);
                if (source.isDirectory(expanded)) {
                    expandedEntries.add(expanded + "/");
                }
            }
        }
        Index rootIndex = readPrebuiltIndex(source);
        if (rootIndex == null) {
            rootIndex = new Index(JandexIndexer.indexArchive(source,
                    entry -> expandedEntries.stream().noneMatch(entry::startsWith)));
        }
        indexes.put(key(source.getURI()), rootIndex);
        indexSubArchives(source, subArchives, indexes, tracing);
    }

    private void indexSubArchives(ReadableArchive parent, Collection<String> subArchives, Map<String, Index> indexes,
                                  StructuredDeploymentTracing tracing) throws IOException {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (String subArchive : subArchives) {
            tasks.add(() -> {
                indexSubArchive(parent, subArchive, indexes, tracing);
                return null;
            });
        }
        try {
            for (Future<Void> task : executorService.getUnderlyingExecutorService().invokeAll(tasks)) {
                task.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        } catch (ExecutionException e) {
            throw new IOException(e.getCause());
        }
    }

    private void indexSubArchive(ReadableArchive parent, String name, Map<String, Index> indexes,
                                 StructuredDeploymentTracing tracing) throws IOException {
        try (ReadableArchive subArchive = parent.getSubArchive(name)) {
            if (subArchive != null) {
                try (DeploymentSpan span = tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, subArchive.getName())) {
                    indexes.computeIfAbsent(key(subArchive.getURI()), key -> indexOrGetFromCache(subArchive));
                }
            }
        }
    }

    /**
     * Resolve the index for an archive URI: the archive itself, the root archive for a directory inside the
     * deployment, a shared WAR library, or an archive that is indexed on demand.
     */
    private Map.Entry<String, Index> resolve(DeploymentContext deploymentContext, Map<String, Index> indexes, URI uri,
                                             StructuredDeploymentTracing tracing) {
        String key = key(uri);
        Index index = indexes.get(key);
        if (index != null) {
            return Map.entry(key, index);
        }
        if (key.endsWith("/")) {
            String rootKey = key(deploymentContext.getSource().getURI());
            Index rootIndex = indexes.get(rootKey);
            return rootIndex == null ? null : Map.entry(rootKey, rootIndex);
        }
        DeploymentUtils.WarLibraryDescriptor warLibrary = DeploymentUtils.getWarLibraryCache().get(uri.getPath());
        if (warLibrary != null && warLibrary.getIndex() != null) {
            indexes.put(key, warLibrary.getIndex());
            return Map.entry(key, warLibrary.getIndex());
        }
        index = indexes.computeIfAbsent(key, k -> indexArchive(uri, tracing));
        return index == null ? null : Map.entry(key, index);
    }

    private Index indexArchive(URI uri, StructuredDeploymentTracing tracing) {
        try (ReadableArchive archive = archiveFactory.openArchive(uri);
             DeploymentSpan span = tracing == null ? null
                     : tracing.startSpan(DeploymentTracing.AppStage.CLASS_SCANNING, archive.getName())) {
            return indexOrGetFromCache(archive);
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.FINE, e, () -> "Unable to index " + uri);
            return null;
        }
    }

    private Index indexOrGetFromCache(ReadableArchive archive) {
        try {
            Index index = readPrebuiltIndex(archive);
            if (index != null) {
                return index;
            }
            Path cacheFile = cacheFile(archive);
            if (cacheFile != null) {
                index = readCache(cacheFile);
                if (index != null) {
                    return index;
                }
            }
            index = new Index(JandexIndexer.indexArchive(archive, entry -> true));
            if (cacheFile != null) {
                org.jboss.jandex.Index jandexIndex = index.getIndex();
                pendingCacheWrites.offer(() -> writeCache(cacheFile, jandexIndex));
            }
            return index;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @return the index shipped with the archive (e.g. built by the jandex-maven-plugin), or null
     */
    private static Index readPrebuiltIndex(ReadableArchive archive) throws IOException {
        for (String path : new String[]{PREBUILT_INDEX, "WEB-INF/classes/" + PREBUILT_INDEX}) {
            try (InputStream stream = archive.getEntry(path)) {
                if (stream != null) {
                    return new Index(new IndexReader(stream).read());
                }
            }
        }
        return null;
    }

    /**
     * @return the cache file of a library jar, or null if the archive is not cacheable
     * (directories, nested archives, snapshot builds)
     */
    private Path cacheFile(ReadableArchive archive) {
        if (!(archive instanceof InputJarArchive) || archive.getName().contains("-SNAPSHOT")) {
            return null;
        }
        long crc = ((InputJarArchive) archive).getArchiveCrc();
        if (crc < 0) {
            return null;
        }
        return Path.of(serverEnvironment.getInstanceRoot().getPath(), CACHE_DIRECTORY,
                archive.getName() + "-" + archive.getArchiveSize() + "-" + Long.toHexString(crc) + CACHE_SUFFIX);
    }

    private static Index readCache(Path cacheFile) {
        if (!Files.exists(cacheFile)) {
            return null;
        }
        try (InputStream stream = new GZIPInputStream(new BufferedInputStream(Files.newInputStream(cacheFile)))) {
            return new Index(new IndexReader(stream).read());
        } catch (IOException | RuntimeException e) {
            LOGGER.log(Level.FINE, e, () -> "Ignoring unreadable Jandex index cache " + cacheFile);
            try {
                Files.deleteIfExists(cacheFile);
            } catch (IOException ignored) {
                // nothing to do
            }
            return null;
        }
    }

    private static void writeCache(Path cacheFile, org.jboss.jandex.Index index) {
        try {
            Files.createDirectories(cacheFile.getParent());
            Path temporary = Files.createTempFile(cacheFile.getParent(), cacheFile.getFileName().toString(), ".tmp");
            try (OutputStream stream = new GZIPOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                new IndexWriter(stream).write(index);
            }
            Files.move(temporary, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e, () -> "Unable to cache Jandex index " + cacheFile);
        }
    }

    /**
     * @return the indexes of the deployment, or null if it was not indexed
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Index> findIndexes(DeploymentContext deploymentContext) {
        return deploymentContext.getTransientAppMetaData(INDEX_METADATA_KEY, Map.class);
    }

    /**
     * @return the indexes of the deployment, creating an empty map for a deployment context whose archive
     * did not require annotation scanning, so that archives can still be indexed on demand
     */
    private static Map<String, Index> indexes(DeploymentContext deploymentContext) {
        Map<String, Index> indexes = findIndexes(deploymentContext);
        if (indexes == null) {
            synchronized (deploymentContext) {
                indexes = findIndexes(deploymentContext);
                if (indexes == null) {
                    indexes = new ConcurrentHashMap<>();
                    deploymentContext.addTransientAppMetaData(INDEX_METADATA_KEY, indexes);
                }
            }
        }
        return indexes;
    }

    /**
     * @return map key of an archive URI
     */
    static String key(URI uri) {
        return uri.toString();
    }
}
