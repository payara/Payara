/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.micro.impl;

import com.sun.enterprise.glassfish.bootstrap.JarUtil;
import fish.payara.micro.PayaraMicro;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class for manipulating the Payara Micro runtime directory
 *
 * @author steve
 */
class RuntimeDirectory {

    private final File directory;
    private boolean isTempDir = true;

    private static final String INSTANCE_ROOT_PROPERTY = "com.sun.aas.instanceRoot";
    private static final String INSTANCE_ROOTURI_PROPERTY = "com.sun.aas.instanceRootURI";
    private static final String INSTALL_ROOT_PROPERTY = "com.sun.aas.installRoot";
    private static final String INSTALL_ROOTURI_PROPERTY = "com.sun.aas.installRootURI";
    private static final String JAR_DOMAIN_DIR = "MICRO-INF/domain/";
    private File domainXML;
    private File configDir;

    /**
     * Default constructor unpacks into a temporary directory
     */
    RuntimeDirectory() throws IOException, URISyntaxException {
        // check if we have exploded our runtime
        String runTimeDir = System.getProperty("fish.payara.micro.tmpdir");
        if (runTimeDir == null) {
            String tmpDir = System.getProperty("glassfish.embedded.tmpdir");
            if (tmpDir == null) {
                tmpDir = System.getProperty("java.io.tmpdir");
            }
            directory = File.createTempFile("payaramicro-", "tmp", new File(tmpDir));
            if (!directory.delete() || !directory.mkdir()) { // convert the file into a directory.
                throw new IOException("cannot create directory: " + directory.getAbsolutePath());
            }
            directory.deleteOnExit();
        } else {
            directory = new File(runTimeDir);
        }
        setSystemProperties();
        unpackRuntime();
    }

    /**
     * Specifies the runtime directory
     *
     * @param directory
     */
    RuntimeDirectory(File directory) throws URISyntaxException, IOException {
        this.directory = directory;
        isTempDir = false;
        setSystemProperties();
        unpackRuntime();
    }

    public File getDirectory() {
        return directory;
    }

    private void unpackRuntime() throws URISyntaxException, IOException {

        // make a docroot here
        new File(directory, "docroot").mkdirs();

        // create a config dir and unpack
        configDir = new File(directory, "config");
        configDir.mkdirs();

        // Get our configuration files
        CodeSource src = PayaraMicro.class
                .getProtectionDomain().getCodeSource();
        if (src != null) {
            // find the root jar
            String jars[] = src.getLocation().toURI().getSchemeSpecificPart().split("!");
            File file = new File(jars[0]);

            JarFile jar = new JarFile(file);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(JAR_DOMAIN_DIR)) {
                    String fileName = entry.getName().substring(JAR_DOMAIN_DIR.length());
                    File outputFile = new File(configDir, fileName);

                    if (isTempDir) {
                        outputFile.deleteOnExit();
                    }

                    // only unpack if an existing file is not there
                    if (!outputFile.exists()) {
                        if (entry.isDirectory()) {
                            outputFile.mkdirs();
                        } else {
                            // write out the conifugration file
                            try (InputStream is = jar.getInputStream(entry)) {
                                Files.copy(is, outputFile.toPath());
                            }
                        }
                    }
                }
            }
        } else {
            throw new IOException("Unable to find the runtime to unpack");
        }

        // sort out the security properties
        configureSecurity();
        
        JarUtil.extractRars(directory.getAbsolutePath());
        JarUtil.setEnv(directory.getAbsolutePath());
    }

    private void setSystemProperties() {

        // Set up system properties now we know where we will be installed
        System.setProperty(INSTANCE_ROOT_PROPERTY, directory.getAbsolutePath());
        System.setProperty(INSTANCE_ROOTURI_PROPERTY, directory.toURI().toString());
        System.setProperty(INSTALL_ROOT_PROPERTY, directory.getAbsolutePath());
        System.setProperty(INSTALL_ROOTURI_PROPERTY, directory.toURI().toString());
    }

    // copies security files referenced via system properties into the runtime directory
    private void configureSecurity() {

        // Set security properties PAYARA-803
        Path loginConfPath = configDir.toPath().resolve("login.conf");
        if (System.getProperty("java.security.auth.login.config") != null) {
            // copy into the runtime directory the referenced file
            Path referencedConfig = Paths.get(System.getProperty("java.security.auth.login.config"));
            if (referencedConfig.toFile().exists()) {
                try {
                    Files.copy(referencedConfig, loginConfPath, StandardCopyOption.REPLACE_EXISTING);
                    System.setProperty("java.security.auth.login.config", loginConfPath.toAbsolutePath().toString());
                } catch (IOException ex) {
                    Logger.getLogger(RuntimeDirectory.class.getName()).log(Level.WARNING, "Cannot copy over the referenced login config, using in place", ex);
                    System.setProperty("java.security.auth.login.config", referencedConfig.toAbsolutePath().toString());
                }
            }
        } else {
            System.setProperty("java.security.auth.login.config", loginConfPath.toAbsolutePath().toString());
        }

        Path serverPolicyPath = configDir.toPath().resolve("server.policy");
        if (System.getProperty("java.security.policy") != null) {
            // copy into the runtime directory the referenced file
            Path referencedConfig = Paths.get(System.getProperty("java.security.policy"));
            if (referencedConfig.toFile().exists()) {
                try {
                    Files.copy(referencedConfig, serverPolicyPath, StandardCopyOption.REPLACE_EXISTING);
                    System.setProperty("java.security.policy", serverPolicyPath.toAbsolutePath().toString());
                } catch (IOException ex) {
                    Logger.getLogger(RuntimeDirectory.class.getName()).log(Level.WARNING, "Cannot copy over the referenced server policy file, using in place", ex);
                    System.setProperty("java.security.policy", referencedConfig.toAbsolutePath().toString());
                }
            }
        } else {
           System.setProperty("java.security.policy", serverPolicyPath.toAbsolutePath().toString());
        }

        Path keystorePath = configDir.toPath().resolve("keystore.jks");
        if (System.getProperty("javax.net.ssl.keyStore") != null) {
            // copy into the runtime directory the referenced file
            Path referencedConfig = Paths.get(System.getProperty("javax.net.ssl.keyStore"));
            if (referencedConfig.toFile().exists()) {
                try {
                    Files.copy(referencedConfig, keystorePath, StandardCopyOption.REPLACE_EXISTING);
                    System.setProperty("javax.net.ssl.keyStore", keystorePath.toAbsolutePath().toString());
                } catch (IOException ex) {
                    Logger.getLogger(RuntimeDirectory.class.getName()).log(Level.WARNING, "Cannot copy over the referenced keystore file, using in place", ex);
                    System.setProperty("javax.net.ssl.keyStore", referencedConfig.toAbsolutePath().toString());
                }
            }
        } else {
            System.setProperty("javax.net.ssl.keyStore", keystorePath.toAbsolutePath().toString());
        }

        Path truststorePath = configDir.toPath().resolve("cacerts.jks");
        if (System.getProperty("javax.net.ssl.trustStore") != null) {
            // copy into the runtime directory the referenced file
            Path referencedConfig = Paths.get(System.getProperty("javax.net.ssl.trustStore"));
            if (referencedConfig.toFile().exists()) {
                try {
                    Files.copy(referencedConfig, truststorePath, StandardCopyOption.REPLACE_EXISTING);
                    System.setProperty("javax.net.ssl.trustStore", truststorePath.toAbsolutePath().toString());
                } catch (IOException ex) {
                    Logger.getLogger(RuntimeDirectory.class.getName()).log(Level.WARNING, "Cannot copy over the referenced keystore file, using in place", ex);
                    System.setProperty("javax.net.ssl.trustStore", referencedConfig.toAbsolutePath().toString());
                }
            }
        } else {
            System.setProperty("javax.net.ssl.trustStore", truststorePath.toAbsolutePath().toString());
        }
    }

    void setDomainXML(File alternateDomainXML) throws IOException {
        Files.copy(alternateDomainXML.toPath(), configDir.toPath().resolve("domain.xml"),StandardCopyOption.REPLACE_EXISTING);
    }
    
    void setDomainXML(InputStream alternateDomainXML) throws IOException {
        Files.copy(alternateDomainXML, configDir.toPath().resolve("domain.xml"),StandardCopyOption.REPLACE_EXISTING);
    }
    
    void setLoggingProperties(File alternativeFile) throws IOException {
        Files.copy(alternativeFile.toPath(), configDir.toPath().resolve("logging.properties"),StandardCopyOption.REPLACE_EXISTING);
    }
    
    File getLoggingProperties() {
        return configDir.toPath().resolve("logging.properties").toFile();        
    }
    
    File getConfigDirectory() {
        return configDir;
    }

    File getDomainXML() {
        return configDir.toPath().resolve("domain.xml").toFile();
    }

    void setHZConfigFile(File alternateHZConfigFile) throws IOException {
        if (alternateHZConfigFile.canRead()) {
            Files.copy(alternateHZConfigFile.toPath(), configDir.toPath().resolve(alternateHZConfigFile.getName()),StandardCopyOption.REPLACE_EXISTING);
        }
    }

}
