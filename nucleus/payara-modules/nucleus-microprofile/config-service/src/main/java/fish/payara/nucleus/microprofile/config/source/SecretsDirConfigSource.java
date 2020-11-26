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

import static java.util.Collections.unmodifiableMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Config Source that reads properties from files from a directory
 * where filename is the property name and file contents is the property value.
 * @since 4.1.181
 * @author steve
 */
public class SecretsDirConfigSource extends PayaraConfigSource implements ConfigSource {

    private Path secretsDir;
    private ConcurrentHashMap<String, String> properties;
    private ConcurrentHashMap<String, FileTime> storedModifiedTimes;
    private ConcurrentHashMap<String, Path> storedPaths;

    public SecretsDirConfigSource() {
        findDir();
        loadProperties();
    }

    SecretsDirConfigSource(Path directory) {
        super(true);
        secretsDir = directory;
        loadProperties();
    }

    @Override
    public Map<String, String> getProperties() {
        return unmodifiableMap(properties);
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
        String result = properties.get(property);
        if (result != null) {
            try {
                // check existence (secret mount might have gone away) and the last modified time
                FileTime ft = storedModifiedTimes.get(property);
                Path path = storedPaths.get(property);
                if (Files.exists(path) && Files.getLastModifiedTime(path).compareTo(ft) > 0) {
                    // file has been modified since last check, re-read content
                    result = readFile(path);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    properties.put(property, result);
                }
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, "Unable to read file in the directory", ex);
            }
        } else {
            // check whether there is a file there now as there wasn't before
            // --> the list of possible paths is used "first match, first serve".
            List<Path> paths = Arrays.asList(Paths.get(secretsDir.toString(), property),
                                             Paths.get(secretsDir.toString(), property.replace('.', File.separatorChar)));
            for (Path path : paths) {
                try {
                    result = readFile(path);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    storedPaths.put(property, path);
                    properties.put(property, result);
                    break;
                } catch (IOException ex) {
                    Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, "Unable to read file in the directory", ex);
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "Secrets Directory";
    }

    private void findDir() {
        secretsDir = Paths.get(configService.getMPConfig().getSecretDir());

        if (!Files.exists(secretsDir) || !Files.isDirectory(secretsDir) || !Files.isReadable(secretsDir)) {
            // let's try it relative to server environment root
            String instancePath = System.getProperty("com.sun.aas.instanceRoot");
            Path test = Paths.get(instancePath, secretsDir.toString());
            if (Files.exists(test) && Files.isDirectory(test) && Files.isReadable(test)) {
                secretsDir = test;
            }
        }
    }

    private String readFile(Path file) throws IOException {
        String result = null;
        if (Files.exists(file) && Files.isRegularFile(file) && Files.isReadable(file)) {
            StringBuilder collector = new StringBuilder();
            for (String line : Files.readAllLines(file)) {
                collector.append(line);
            }
            result = collector.toString();
        }
        return result;
    }

    private void loadProperties() {
        properties = new ConcurrentHashMap<>();
        storedModifiedTimes = new ConcurrentHashMap<>();
        storedPaths = new ConcurrentHashMap<>();
        if (Files.exists(secretsDir) && Files.isDirectory(secretsDir) && Files.isReadable(secretsDir)) {
            try {
                Files.walkFileTree(secretsDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        return dir.toFile().isHidden() ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
                    }
    
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes mainAtts) throws IOException {
                        File file = path.toFile();
                        // do not read hidden files, as K8s Secret filenames are symlinks to hidden files with data.
                        if (file.isFile() && ! file.isHidden() && file.canRead()) {
                            // 1. get relative path based on the secrets dir ("/foobar"),
                            // 2. replace all path seps with a ".",
                            // so "/foobar/test/foo/bar" becomes "test/foo/bar" becomes "test.foo.bar" property name
                            String property = secretsDir.relativize(path).toString().replace(File.separatorChar, '.');
                            
                            properties.put(property, readFile(path));
                            storedModifiedTimes.put(property, mainAtts.lastModifiedTime());
                            storedPaths.put(property, path);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, "Unable to read file in the directory", ex);
            }
        }
    }

}
