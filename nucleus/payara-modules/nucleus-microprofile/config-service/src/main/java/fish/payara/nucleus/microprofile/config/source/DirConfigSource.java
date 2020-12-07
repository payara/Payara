/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.nucleus.microprofile.config.source;

import fish.payara.nucleus.executorservice.PayaraExecutorService;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toMap;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Config Source that reads properties from files from a directory
 * where filename is the property name and file contents is the property value.
 * @since 5.2020.7
 */
public class DirConfigSource extends PayaraConfigSource implements ConfigSource {

    static final class DirProperty {
        final String property;
        final FileTime lastModifiedTime;
        final Path path;
        final int pathDepth;
        
        DirProperty(String property, FileTime lastModifiedTime, Path path, int pathDepth) {
            this.property = property;
            this.lastModifiedTime = lastModifiedTime;
            this.path = path;
            this.pathDepth = pathDepth;
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirProperty that = (DirProperty) o;
            return pathDepth == that.pathDepth &&
                property.equals(that.property) &&
                lastModifiedTime.equals(that.lastModifiedTime) &&
                path.equals(that.path);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(property, lastModifiedTime, path, pathDepth);
        }
    }
    
    final class DirPropertyWatcher implements Runnable {
    
        private final Logger logger = Logger.getLogger(DirConfigSource.class.getName());
        private final WatchService watcher = FileSystems.getDefault().newWatchService();
        private final ConcurrentHashMap<WatchKey, Path> watchedFileKeys = new ConcurrentHashMap<>();
        
        DirPropertyWatcher(Path topmostDir) throws IOException {
            if (Files.exists(topmostDir) && Files.isDirectory(topmostDir) && Files.isReadable(topmostDir)) {
                registerAll(topmostDir);
            } else {
                throw new IOException("Given directory '"+topmostDir+"' is no directory or cannot be read.");
            }
        }
        
        void registerAll(Path dir) throws IOException {
            // register file watchers recursively (they don't attach to subdirs...)
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        void register(Path dir) throws IOException {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchedFileKeys.putIfAbsent(key, dir);
        }
        
        @Override
        public void run() {
            while (true) {
                // wait infinitely until we receive an event (or the executor is shutting down)
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException ex) {
                    return;
                }
                
                Path workDir = watchedFileKeys.get(key);
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
            
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path path = workDir.resolve(fileName);
            
                    try {
                        // new directory to be watched and traversed
                        if (kind == ENTRY_CREATE && Files.isDirectory(path) && !Files.isHidden(path) && Files.isReadable(path)) {
                            registerAll(path);
                            initializePropertiesFromPath(path);
                        }
                        // new or updated file found
                        if (Files.isRegularFile(path) && (kind == ENTRY_CREATE || kind == ENTRY_MODIFY)) {
                            updatePropertyFromPath(path, Files.readAttributes(path, BasicFileAttributes.class));
                        }
                        if (Files.isRegularFile(path) && (kind == ENTRY_DELETE)) {
                            removePropertyFromPath(path);
                        }
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Could not process event '"+kind+"' on '"+path+"'", e);
                    }
                }
    
                // Reset key (obligatory) and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    watchedFileKeys.remove(key);
                    
                    // all directories became inaccessible, even topmostDir!
                    if (watchedFileKeys.isEmpty()) break;
                }
            }
        }
    }
    
    private static final Logger logger = Logger.getLogger(DirConfigSource.class.getName());
    private Path directory;
    private ConcurrentHashMap<String, DirProperty> properties = new ConcurrentHashMap<>();
    
    public DirConfigSource(PayaraExecutorService executorService) {
        try {
            // get the directory from the app server config
            this.directory = findDir();
            // create the watcher for the directory
            executorService.submit(new DirPropertyWatcher(this.directory));
            // initial loading
            initializePropertiesFromPath(this.directory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during setup of MicroProfile Config Directory Source", e);
        }
    }

    // Used for testing only with explicit dependency injection
    DirConfigSource(Path directory, PayaraExecutorService executorService) {
        super(true);
        this.directory = directory;
        try {
            initializePropertiesFromPath(this.directory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during setup of MicroProfile Config Directory Source", e);
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return unmodifiableMap(properties.entrySet().stream()
                                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().property)));
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public int getOrdinal() {
        return Integer.parseInt(configService.getMPConfig().getSecretDirOrdinality());
    }

    @Override
    public String getValue(String property) {
        DirProperty result = properties.get(property);
        return result == null ? null : result.property;
    }

    @Override
    public String getName() {
        return "Directory";
    }

    private Path findDir() throws IOException {
        String path = configService.getMPConfig().getSecretDir();
        List<Path> candidates = Arrays.asList(
                                    Paths.get(path),
                                    // let's try it relative to server environment root
                                    Paths.get(System.getProperty("com.sun.aas.instanceRoot"), path)
                                );
        for (Path candidate : candidates) {
            if (Files.exists(candidate) || Files.isDirectory(candidate) || Files.isReadable(candidate)) {
                return candidate;
            }
        }
        throw new IOException("Given MPCONFIG directory '"+path+"' is no directory or cannot be read.");
    }
    
    void initializePropertiesFromPath(Path topmostDir) throws IOException {
        if (Files.exists(topmostDir) && Files.isDirectory(topmostDir) && Files.isReadable(topmostDir)) {
            // initialize properties on first run
            Files.walkFileTree(topmostDir, new SimpleFileVisitor<Path>() {
                // Ignore hidden directories
                @Override
                public FileVisitResult preVisitDirectory(java.nio.file.Path dir, BasicFileAttributes attrs) throws IOException {
                    return dir.toFile().isHidden() ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                }
    
                // Read and ingest all files and dirs present
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes mainAtts) throws IOException {
                    updatePropertyFromPath(path, mainAtts);
                    return FileVisitResult.CONTINUE;
                }
            });
        } else {
            throw new IOException("Given directory '"+topmostDir+"' is no directory or cannot be read.");
        }
    }
    
    void updatePropertyFromPath(Path path, BasicFileAttributes mainAtts) throws IOException {
        // do not read hidden files, as K8s Secret filenames are symlinks to hidden files with data.
        // also ignore files > 512KB, as they are most likely no text config files...
        if (Files.isRegularFile(path) && ! Files.isHidden(path) && Files.isReadable(path) && mainAtts.size() < 512*1024) {
            // retrieve the property name from the file path
            String property = parsePropertyNameFromPath(path);
            
            // Conflict handling:
            // When this property is already present, check how to solve the conflict.
            // This property file will be skipped if the file we already have is deeper in the file tree...
            if (checkLongestMatchForPath(property, path)) {
                return;
            }
            
            properties.put(property, readPropertyFromPath(path, mainAtts));
        }
    }
    
    void removePropertyFromPath(Path path) {
        String property = parsePropertyNameFromPath(path);
    
        // not present? go away silently.
        if (! properties.containsKey(property)) return;
    
        // only delete from the map if the file that has been deleted is the same as the one stored in the map
        // -> deleting a file less specific but matching a property should not remove from the map
        // -> deleting a file more specific than in map shouldn't occur (it had to slip through longest match check then).
        if (path.equals(properties.get(property).path)) {
            properties.remove(property);
        }
        
    }
    
    String parsePropertyNameFromPath(Path path) {
        // 1. get relative path based on the config dir ("/config"),
        String property = directory.relativize(path.getParent()).toString();
        // 2. ignore all file suffixes after last dot
        property += path.getFileName().toString().substring(0, path.getFileName().toString().lastIndexOf('.')-1);
        // 3. replace all path seps with a ".",
        property = property.replace(File.separatorChar, '.');
        // so "/config/foo/bar/test/one.txt" becomes "foo/bar/test/one.txt" becomes "foo.bar.test.one" property name
        return property;
    }
    
    /**
     * Check if the path given is a more specific path to a value for the given property
     * @param property
     * @param path
     * @return true if more specific, false if not
     */
    boolean checkLongestMatchForPath(String property, Path path) {
        // Make path relative to config directory
        // NOTE: we will never have a path containing "..", as our tree walkers are always inside this "root".
        Path relativePath = directory.relativize(path);
        
        // No property -> path is new and more specific
        if (! properties.containsKey(property))
            return true;
        DirProperty old = properties.get(property);
        
        // Check if this element has a higher path depth (longest match)
        // Example: "foo.bar/test/one.txt" (depth 2) wins over "foo.bar.test.one.txt" (depth 0)
        boolean depth = old.pathDepth > relativePath.getNameCount();
        
        // In case that both pathes have the same depth, we need to check on the position of dots.
        // Example: /config/foo.bar/test/one.txt is less specific than /config/foo/bar.test/one.txt
        if (old.pathDepth == relativePath.getNameCount()) {
            String oldPath = old.path.toString();
            String newPath = path.toAbsolutePath().toString();
            int offset = 0;
            while (offset > -1) {
                if (newPath.indexOf(".", offset) > oldPath.indexOf(".", offset)) return true;
                offset = oldPath.indexOf(".", offset + 1);
            }
        }
        return depth;
    }
    
    DirProperty readPropertyFromPath(Path path, BasicFileAttributes mainAtts) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
            return new DirProperty(
                new String(Files.readAllBytes(path), StandardCharsets.UTF_8),
                mainAtts.lastModifiedTime(),
                path.toAbsolutePath(),
                directory.relativize(path).getNameCount()
            );
        }
        return null;
    }

}
