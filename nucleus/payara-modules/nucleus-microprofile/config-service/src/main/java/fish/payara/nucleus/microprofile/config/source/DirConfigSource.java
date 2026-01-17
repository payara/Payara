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
package fish.payara.nucleus.microprofile.config.source;

import fish.payara.nucleus.microprofile.config.spi.ConfigProviderResolverImpl;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Collections.unmodifiableMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
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
        final String propertyValue;
        final FileTime lastModifiedTime;
        final Path path;
        
        DirProperty(String propertyValue, FileTime lastModifiedTime, Path path) {
            this.propertyValue = propertyValue;
            this.lastModifiedTime = lastModifiedTime;
            this.path = path;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DirProperty that = (DirProperty) o;
            return propertyValue.equals(that.propertyValue) &&
                lastModifiedTime.equals(that.lastModifiedTime) &&
                path.equals(that.path);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(propertyValue, lastModifiedTime, path);
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
    
        /**
         * Register file watchers recursively (as they don't attach themselfs to sub directories...)
         * and initialize values from files present and suitable.
         * @param dir Topmost directory to start recursive traversal from
         * @throws IOException
         */
        final void registerAll(Path dir) throws IOException {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    // only register subdirectories if the directory itself is suitable.
                    if ( isAptDir(dir) ) {
                        register(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                }
    
                // Read and ingest all files and dirs present
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes mainAtts) throws IOException {
                    // file will be checked before upserting.
                    upsertPropertyFromPath(path, mainAtts);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        
        final void register(Path dir) throws IOException {
            WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchedFileKeys.putIfAbsent(key, dir);
            logger.finer("MPCONFIG DirConfigSource: registered \""+dir+"\" as key \""+key+"\".");
        }
        
        @Override
        public final void run() {
            // wait infinitely until we receive an event (or the executor is shutting down)
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException ex) {
                logger.info("MPCONFIG DirConfigSource: shutting down watcher thread.");
                return;
            }
            
            Path workDir = watchedFileKeys.get(key);
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
        
                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path fileName = ev.context();
                Path path = workDir.resolve(fileName);

                logger.finer("MPCONFIG DirConfigSource: detected change: "+fileName.toString()+" : "+kind.toString());
        
                try {
                    // new directory to be watched and traversed
                    if (kind == ENTRY_CREATE && isAptDir(path)) {
                        logger.finer("MPCONFIG DirConfigSource: registering new paths.");
                        registerAll(path);
                    }
                    // new or updated file found (new = create + modify on content save)
                    // or new symlink found (symlinks are create only!) (also, aptness of file is checked inside update routine)
                    if ( kind == ENTRY_MODIFY || (kind == ENTRY_CREATE && Files.isSymbolicLink(path)) ) {
                        logger.finer("MPCONFIG DirConfigSource: processing new or updated file \""+path.toString()+"\".");
                        BasicFileAttributes atts = Files.readAttributes(path, BasicFileAttributes.class);
                        upsertPropertyFromPath(path, atts);
                    }
                    if (Files.notExists(path) && ! watchedFileKeys.containsValue(path) && kind == ENTRY_DELETE) {
                        logger.finer("MPCONFIG DirConfigSource: removing deleted file \""+path.toString()+"\".");
                        removePropertyFromPath(path);
                    }
                } catch (IOException e) {
                    logger.log(WARNING, "MPCONFIG DirConfigSource: could not process event '"+kind+"' on '"+path+"'", e);
                }
            }

            // Reset key (obligatory) and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                logger.finer("MPCONFIG DirConfigSource: removing watcher for key \""+key+"\".");
                watchedFileKeys.remove(key);
            }
        }
    }
    
    private static final Logger logger = Logger.getLogger(DirConfigSource.class.getName());
    public static final String DEFAULT_DIR = "secrets";
    private Path directory;
    private final ConcurrentHashMap<String, DirProperty> properties = new ConcurrentHashMap<>();
    
    public DirConfigSource() {
        try {
            // get the directory from the app server config
            Optional<Path> dir = findDir();
            if (dir.isPresent()) {
                this.directory = dir.get();
                // create the watcher for the directory
                configService.getExecutor().scheduleWithFixedDelay(createWatcher(this.directory), 0, 1, SECONDS);
            }
        } catch (IOException e) {
            logger.log(SEVERE, "MPCONFIG DirConfigSource: error during setup.", e);
        }
    }

    // Used for testing only with explicit dependency injection
    // Used for testing only with explicit dependency injection
    DirConfigSource(Path directory, ConfigProviderResolverImpl configService) {
        super(configService);
        this.directory = directory;
    }
    
    DirPropertyWatcher createWatcher(Path topmostDirectory) throws IOException {
        return new DirPropertyWatcher(topmostDirectory);
    }

    @Override
    public Map<String, String> getProperties() {
        return unmodifiableMap(properties.entrySet().stream()
                                    .collect(toMap(Map.Entry::getKey, e -> e.getValue().propertyValue)));
    }
    
    // Used for testing only
    void setProperties(Map<String, DirProperty> properties) {
        this.properties.clear();
        this.properties.putAll(properties);
    }

    @Override
    public Set<String> getPropertyNames() {
        return properties.keySet();
    }

    @Override
    public int getOrdinal() {
        String storedOrdinal = getValue("config_ordinal");
        if (storedOrdinal != null) {
            return Integer.parseInt(storedOrdinal);
        }
        return Integer.parseInt(configService.getMPConfig().getSecretDirOrdinality());
    }

    @Override
    public String getValue(String property) {
        DirProperty result = properties.get(property);
        return result == null ? null : result.propertyValue;
    }

    @Override
    public String getName() {
        return "Directory";
    }

    Optional<Path> findDir() throws IOException {
        String path = configService.getMPConfig().getSecretDir();
        if (path == null)
            return Optional.empty();
        
        // adding all pathes where to look for the directory...
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(path));
        // let's try it relative to server environment root (<PAYARA-HOME>/glassfish/domains/<DOMAIN>/)
        if ( ! Paths.get(path).isAbsolute())
            candidates.add(Paths.get(System.getProperty("com.sun.aas.instanceRoot"), path).normalize());
        
        for (Path candidate : candidates) {
            if (isAptDir(candidate)) {
                return Optional.of(candidate);
            }
        }
        
        // Log that the configured directory is not resolving.
        Level lvl = SEVERE;
        // Reduce log level to fine if default setting, as the admin might simply not use this functionality.
        if(path.equals(DEFAULT_DIR))
            lvl = FINE;
        logger.log(lvl, "Given MPCONFIG directory '" + path + "' is no directory, cannot be read or has a leading dot.");
        return Optional.empty();
    }
    
    /**
     * Upserting a property from a file. Checking for suitability of the file, too.
     * @param path Path to the file
     * @param mainAtts The attributes of the file (like mod time etc)
     * @return true if file was suitable, false if not.
     * @throws IOException In case anything goes bonkers.
     */
    final boolean upsertPropertyFromPath(Path path, BasicFileAttributes mainAtts) throws IOException {
        // check for a suitable file first, return aptness.
        if ( isAptFile(path, mainAtts) ) {
            // retrieve the property name from the file path
            String property = parsePropertyNameFromPath(path, this.directory);
            
            // Conflict handling:
            // When this property is not already present, check how to solve the conflict.
            // This property file will be skipped if the file we already have is deeper in the file tree...
            if (isLongestMatchForPath(property, path)) {
                properties.put(property, readPropertyFromPath(path, mainAtts, this.directory));
                return true;
            }
        }
        return false;
    }
    
    final void removePropertyFromPath(Path path) {
        String property = parsePropertyNameFromPath(path, this.directory);
        // not present? go away silently.
        if (! properties.containsKey(property)) return;
        
        // only delete from the map if the file that has been deleted is the same as the one stored in the map
        // -> deleting a file less specific but matching a property should not remove from the map
        // -> deleting a file more specific than in map shouldn't occur (it had to slip through longest match check then).
        if (path.equals(properties.get(property).path)) {
            properties.remove(property);
        }
    }
    
    /**
     * Check if a new path to a given property is a more specific match for it.
     * If both old and new paths are the same, it's still treated as more specific, allowing for updates.
     * @param property
     * @param newPath
     * @return true if new path more specific or equal to old path of property, false if not.
     */
    final boolean isLongestMatchForPath(String property, Path newPath) {
        if (newPath == null || property == null || property.isEmpty())
            return false;
        // No property -> path is new and more specific
        if (! properties.containsKey(property))
            return true;
        DirProperty old = properties.get(property);
        return isLongestMatchForPath(this.directory, old.path, newPath);
    }
    
    /**
     * Check if the new path given is a more specific path to a property file for a given old path
     * Same path is more specific, too, to allow updates to the same file.
     * @param rootDir
     * @param oldPath
     * @param newPath
     * @return true if more specific, false if not
     */
    static final boolean isLongestMatchForPath(Path rootDir, Path oldPath, Path newPath) {
        // Old and new path are the same -> "more" specific (update case)
        if (oldPath.equals(newPath))
            return true;
        
        // Make pathes relative to config directory and count the "levels" (path depth)
        // NOTE: we will never have a path containing "..", as our tree walkers are always inside this "root".
        int oldPathDepth = rootDir.relativize(oldPath).getNameCount();
        int newPathDepth = rootDir.relativize(newPath).getNameCount();
        
        // Check if this element has a higher path depth (longest match)
        // Example: "foo.bar/test/one.txt" (depth 2) wins over "foo.bar.test.one.txt" (depth 0)
        if (oldPathDepth < newPathDepth)
            return true;
        
        // In case that both pathes have the same depth, we need to check on the position of dots.
        // Example: /config/foo.bar/test/one.txt is less specific than /config/foo/bar.test/one.txt
        if (oldPathDepth == newPathDepth) {
            String oldPathS = oldPath.toString();
            String newPathS = newPath.toAbsolutePath().toString();
            int offset = 0;
            while (offset > -1) {
                if (newPathS.indexOf(".", offset) > oldPathS.indexOf(".", offset)) return true;
                offset = oldPathS.indexOf(".", offset + 1);
            }
        }
        
        // All other cases: no, it's not more specific.
        return false;
    }
    
    /**
     * Check path to be a directory, readable by us, but ignore if starting with a "."
     * (as done for K8s secrets mounts).
     * @param path
     * @return true if suitable, false if not.
     * @throws IOException when path cannot be resolved, maybe because of a broken symlink.
     */
    public final static boolean isAptDir(Path path) throws IOException {
        return path != null && Files.exists(path) &&
            Files.isDirectory(path) && Files.isReadable(path) &&
            !path.getFileName().toString().startsWith(".");
    }
    
    /**
     * Check if the file exists (follows symlinks), is a regular file (following symlinks), is readable (following symlinks),
     * the filename does not start with a "." (not following the symlink!) and the file is less than 512 KB.
     * @param path
     * @return true if suitable file, false if not.
     * @throws IOException when path cannot be accessed or resolved etc.
     */
    public final static boolean isAptFile(Path path, BasicFileAttributes atts) throws IOException {
        return path != null && Files.exists(path) &&
            Files.isRegularFile(path) && Files.isReadable(path) &&
            !path.getFileName().toString().startsWith(".") &&
            atts.size() < 512*1024;
    }
    
    /**
     * Parsing the relative path into a configuration property name.
     * Using dots to mark scopes based on directories plus keeping dots used in the files name.
     * @param path The path to the property file
     * @return The property name
     */
    public final static String parsePropertyNameFromPath(Path path, Path rootDir) {
        // 1. get relative path based on the config dir ("/config"),
        String property = "";
        if (! path.getParent().equals(rootDir))
            property += rootDir.relativize(path.getParent()).toString() + File.separatorChar;
        // 2. add the file name (might be used for mangling in the future)
        property += path.getFileName();
        // 3. replace all path seps with a ".",
        property = property.replace(File.separatorChar, '.');
        // so "/config/foo/bar/test/one.txt" becomes "foo/bar/test/one.txt" becomes "foo.bar.test.one" property name
        return property;
    }
    
    /**
     * Actually read the data from the file, assuming UTF-8 formatted content, creating properties from it.
     * @param path The file to read
     * @param mainAtts The files basic attributes like mod time, ...
     * @return The configuration property this path represents
     * @throws IOException When reading fails.
     */
    static final DirProperty readPropertyFromPath(Path path, BasicFileAttributes mainAtts, Path rootPath) throws IOException {
        if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
            return new DirProperty(
                new String(Files.readAllBytes(path), StandardCharsets.UTF_8),
                mainAtts.lastModifiedTime(),
                path.toAbsolutePath()
            );
        }
        throw new IOException("Cannot read property from '"+path.toString()+"'.");
    }
}
