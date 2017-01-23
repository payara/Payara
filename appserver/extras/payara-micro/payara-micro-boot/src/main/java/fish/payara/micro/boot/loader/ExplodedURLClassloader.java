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
package fish.payara.micro.boot.loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author steve
 */
public class ExplodedURLClassloader extends URLClassLoader {

    private final File explodedDir;
    private boolean deleteOnExit = false;
    private static final String JAR_DOMAIN_DIR = "MICRO-INF/runtime/";
    private static final String LIB_DOMAIN_DIR = "MICRO-INF/lib/";

    public ExplodedURLClassloader(File explodeTo) throws IOException {
        super(new URL[0]);
        explodedDir = explodeTo;
        System.setProperty("fish.payara.micro.tmpdir", explodedDir.getAbsolutePath());
        explodeJars();
    }

    public ExplodedURLClassloader() throws IOException {
        super(new URL[0]);
        File directory;
        directory = File.createTempFile("payaramicro-rt", "tmp");
        System.setProperty("fish.payara.micro.tmpdir", directory.getAbsolutePath());
        if (!directory.delete() || !directory.mkdir()) { // convert the file into a directory.
            throw new IOException("Unable to create temporary runtime directory");
        }
        deleteOnExit = true;
        directory.deleteOnExit();
        explodedDir = directory;
        explodeJars();
    }

    private void explodeJars() throws IOException {

        // create a runtime jar directory
        File runtimeDir = new File(explodedDir, "runtime");
        runtimeDir.mkdirs();
        if (deleteOnExit) {
            runtimeDir.deleteOnExit();
        }
        
        // create a lib directory
        File libDir = new File(explodedDir,"lib");
        libDir.mkdirs();
        if (deleteOnExit) {
            libDir.deleteOnExit();
        }
        
        // sets the system property used in the server.policy file for permissions
        System.setProperty("fish.payara.micro.UnpackDir", explodedDir.getAbsolutePath());

        // Get our jar files
        CodeSource src = ExplodedURLClassloader.class
                .getProtectionDomain().getCodeSource();
        if (src != null) {
            try {
                // find the root jar
                String jars[] = src.getLocation().toURI().getSchemeSpecificPart().split("!");
                File file = new File(jars[0]);

                JarFile jar = new JarFile(file);
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String fileName = null;
                    if (entry.getName().startsWith(JAR_DOMAIN_DIR)) {
                        fileName = entry.getName().substring(JAR_DOMAIN_DIR.length());
                    } else if (entry.getName().startsWith(LIB_DOMAIN_DIR)) {
                        fileName = entry.getName().substring(LIB_DOMAIN_DIR.length());
                    }
                    
                    if (fileName != null) {
                        File outputFile = new File(runtimeDir, fileName);
                        if (deleteOnExit) {
                            outputFile.deleteOnExit();
                        }
                        super.addURL(outputFile.getAbsoluteFile().toURI().toURL());


                        if (entry.isDirectory()) {
                            outputFile.mkdirs();
                        } else {
                            // write out the jar file
                            try (InputStream is = jar.getInputStream(entry)) {
                                Files.copy(is, outputFile.toPath(),StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            } catch (URISyntaxException ex) {
                Logger.getLogger(ExplodedURLClassloader.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
