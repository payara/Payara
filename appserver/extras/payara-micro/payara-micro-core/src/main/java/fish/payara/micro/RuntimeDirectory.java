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
package fish.payara.micro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Class for manipulating the Payara Micro runtime directory
 *
 * @author steve
 */
class RuntimeDirectory {

    private final File directory;
    private boolean isTempDir = true;
    private boolean unpacked = false;
    
    private static final String INSTANCE_ROOT_PROPERTY = "com.sun.aas.instanceRoot";
    private static final String INSTANCE_ROOTURI_PROPERTY = "com.sun.aas.instanceRootURI";
    private static final String INSTALL_ROOT_PROPERTY = "com.sun.aas.installRoot";
    private static final String INSTALL_ROOTURI_PROPERTY = "com.sun.aas.installRootURI";
    private static final String JAR_DOMAIN_DIR = "MICRO-INF/domain/";
    private File domainXML;

    /**
     * Default constructor unpacks into a temporary directory
     */
    RuntimeDirectory() throws IOException {
        String tmpDir = System.getProperty("glassfish.embedded.tmpdir");
        if (tmpDir == null) {
            tmpDir = System.getProperty("java.io.tmpdir");
        }
        directory = File.createTempFile("payaramicro-", "tmp", new File(tmpDir));
        if (!directory.delete() || !directory.mkdir()) { // convert the file into a directory.
            throw new IOException("cannot create directory: " + directory.getAbsolutePath());
        }
        directory.deleteOnExit();
        setSystemProperties();
    }

    /**
     * Specifies the runtime directory
     *
     * @param directory
     */
    RuntimeDirectory(File directory) {
        this.directory = directory;
        isTempDir = false;
        setSystemProperties();
    }

    public File getDirectory() {
        return directory;
    }

    public void unpackRuntime() throws URISyntaxException, IOException {

        // make a docroot here
        new File(directory, "docroot").mkdirs();

        // create a config dir and unpack
        File configDir = new File(directory, "config");
        configDir.mkdirs();

        // Get our configuration files
        CodeSource src = PayaraMicro.class
                .getProtectionDomain().getCodeSource();
        if (src != null) {
            // find the root jar
            String jars[] = src.getLocation().toURI().getSchemeSpecificPart().split("!");
            File file = new File(new URI(jars[0]));

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
        unpacked = true;
        
        if (domainXML != null) {
         Files.copy(domainXML.toPath(), directory.toPath().resolve("domain.xml"));            
        }
    }

    private void setSystemProperties() {

        // Set up system properties now we know where we will be installed
        System.setProperty(INSTANCE_ROOT_PROPERTY, directory.getAbsolutePath());
        System.setProperty(INSTANCE_ROOTURI_PROPERTY, directory.toURI().toString());
        System.setProperty(INSTALL_ROOT_PROPERTY, directory.getAbsolutePath());
        System.setProperty(INSTALL_ROOTURI_PROPERTY, directory.toURI().toString());
    }

    void setDomainXML(File alternateDomainXML) throws IOException {
        domainXML = alternateDomainXML;
        if (unpacked) {
         Files.copy(alternateDomainXML.toPath(), directory.toPath().resolve("domain.xml"));
        }
    }

}
