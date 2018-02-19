/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
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

    public SecretsDirConfigSource() {
        findFile();
        loadProperties();
    }
    
    SecretsDirConfigSource(Path directory) {
        super(true);
        secretsDir = directory;
        loadProperties();

    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
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
                // check the last modified time
                FileTime ft = storedModifiedTimes.get(property);
                Path path = Paths.get(secretsDir.toString(), property);
                if (Files.exists(path) && Files.getLastModifiedTime(path).compareTo(ft) > 0) {
                    // file  has been modified since last check
                    result = readFile(property);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    properties.put(property, result);
                }
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            // check whether there is a file there now as there wasn't before
            Path path = Paths.get(secretsDir.toString(), property);
            if (Files.exists(path) && Files.isRegularFile(path) && Files.isReadable(path)) {
                try {
                    result = readFile(property);
                    storedModifiedTimes.put(property, Files.getLastModifiedTime(path));
                    properties.put(property, result);
                } catch (IOException ex) {
                    Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return "Secrets Directory";
    }

    private void findFile() {
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

    private String readFile(String name) {
        String result = null;
        if (Files.exists(secretsDir) && Files.isDirectory(secretsDir) && Files.isReadable(secretsDir)) {
            try {
                Path file = Paths.get(secretsDir.toString(), name);
                if (Files.exists(file) && Files.isReadable(file)) {
                    StringBuilder collector = new StringBuilder();
                    for (String line : Files.readAllLines(file)) {
                        collector.append(line);
                    }
                    result = collector.toString();
                }
            } catch (IOException ex) {
                Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return result;
    }

    private void loadProperties() {
        properties = new ConcurrentHashMap<>();
        storedModifiedTimes = new ConcurrentHashMap<>();
        if (Files.exists(secretsDir) && Files.isDirectory(secretsDir) && Files.isReadable(secretsDir)) {
            File files[] = secretsDir.toFile().listFiles();
            for (File file : files) {
                try {
                    if (file.isFile() && file.canRead()) {
                        properties.put(file.getName(), readFile(file.getName()));
                        storedModifiedTimes.put(file.getName(), Files.getLastModifiedTime(file.toPath()));
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SecretsDirConfigSource.class.getName()).log(Level.SEVERE, "Unable to read file in the directory", ex);
                }
            }
        }
    }

}
