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
package fish.payara.payaramicro.boot;

import fish.payara.payaramicro.boot.loader.ExecutableArchiveLauncher;
import fish.payara.payaramicro.boot.loader.archive.Archive;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * This class boots a Payara Micro Executable jar It establishes the Payara
 * Micro Executable ClassLoader onto the main thread and then boots standard
 * Payara Micro.
 *
 * @author steve
 */
public class PayaraMicroLauncher extends ExecutableArchiveLauncher {

    private static final String MICRO_MAIN = "fish.payara.micro.PayaraMicro";
    private static final String INSTANCE_ROOT_PROPERTY = "com.sun.aas.instanceRoot";
    private static final String INSTANCE_ROOTURI_PROPERTY = "com.sun.aas.instanceRootURI";
    private static final String INSTALL_ROOT_PROPERTY = "com.sun.aas.installRoot";
    private static final String INSTALL_ROOTURI_PROPERTY = "com.sun.aas.installRootURI";
    private static final String JAR_DOMAIN_DIR = "MICRO-INF/domain/";
    private static final String JAR_MODULES_DIR = "MICRO-INF/runtime";
    private static final String JAR_CLASSES_DIR = "MICRO-INF/classes";
    private static final String JAR_LIB_DIR = "MICRO-INF/lib";
    private static final String BOOT_PROPS_FILE = "/MICRO-INF/payara-boot.properties";

    public static void main(String args[]) throws Exception {
        PayaraMicroLauncher launcher = new PayaraMicroLauncher();
        launcher.setBootProperties();
        launcher.unPackRuntime(args);
        launcher.launch(args);
    }

    @Override
    protected boolean isNestedArchive(Archive.Entry entry) {
        boolean result = false;
        if (entry.isDirectory() && entry.getName().equals(JAR_CLASSES_DIR)) {
            result = true;
        } else if ((entry.getName().startsWith(JAR_LIB_DIR) || entry.getName().startsWith(JAR_MODULES_DIR)) && !entry.getName().endsWith(".gitkeep")) {
            result = true;
        }
        return result;
    }

    @Override
    protected void postProcessClassPathArchives(List<Archive> archives) throws Exception {
        archives.add(0, getArchive());
    }

    @Override
    protected String getMainClass() throws Exception {
        return MICRO_MAIN;
    }

    private void unPackRuntime(String args[]) throws IOException, URISyntaxException {

        String tmpDir = null;
        File instanceRoot = null;
        // check whether a rootDirectory has been specified
        for (int i = 0; i < args.length; i++) {
            if ("--rootDir".equals(args[i])) {
                tmpDir = args[i + 1];
                instanceRoot = new File(tmpDir);
                if (!instanceRoot.exists() || !instanceRoot.isDirectory()) {
                    throw new IOException("Specified Root Directory " + instanceRoot.getAbsolutePath() + " Is not writable or not a directory");
                }
            }
        }

        // unpack all the config files
        // we need to create a temporary instance root
        if (instanceRoot == null) {
            if (tmpDir == null) {
                tmpDir = System.getProperty("glassfish.embedded.tmpdir");
            }

            if (tmpDir == null) {
                tmpDir = System.getProperty("java.io.tmpdir");
            }
            instanceRoot = File.createTempFile("payaramicro-", "tmp", new File(tmpDir));
            if (!instanceRoot.delete() || !instanceRoot.mkdir()) { // convert the file into a directory.
                throw new IOException("cannot create directory: " + instanceRoot.getAbsolutePath());
            }
            instanceRoot.deleteOnExit();
        }

        // Set up system properties now we know where we will be installed
        System.setProperty(INSTANCE_ROOT_PROPERTY, instanceRoot.getAbsolutePath());
        System.setProperty(INSTANCE_ROOTURI_PROPERTY, instanceRoot.toURI().toString());
        System.setProperty(INSTALL_ROOT_PROPERTY, instanceRoot.getAbsolutePath());
        System.setProperty(INSTALL_ROOTURI_PROPERTY, instanceRoot.toURI().toString());

        // make a docroot here
        new File(instanceRoot, "docroot").mkdirs();

        // create a config dir and unpack
        File configDir = new File(instanceRoot, "config");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
              
        // Get our configuration files
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        if (src != null) {
            File file = new File(src.getLocation().toURI().getSchemeSpecificPart());
            JarFile jar = new JarFile(file);
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(JAR_DOMAIN_DIR)) {
                    String fileName = entry.getName().substring(JAR_DOMAIN_DIR.length());
                    File outputFile = new File(configDir, fileName);

                    // only unpack if an existing file is not there
                    if (!outputFile.exists()) {
                        if (entry.isDirectory()) {
                            outputFile.mkdirs();
                        } else {
                            // write out the conifugration file
                            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                                InputStream is = jar.getInputStream(entry);
                                byte[] buffer = new byte[4096];
                                int bytesRead = 0;
                                while ((bytesRead = is.read(buffer)) != -1) {
                                    fos.write(buffer, 0, bytesRead);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void setBootProperties() throws IOException {
        Properties bootProperties = new Properties();
        try (InputStream is = this.getClass().getResourceAsStream(BOOT_PROPS_FILE)) {
            if (is != null) {
                bootProperties.load(is);
                for (String key : bootProperties.stringPropertyNames()) {
                    // do not override an existing system property
                    if (System.getProperty(key) == null) {
                        System.setProperty(key, bootProperties.getProperty(key));
                    }
                }
            }
        }
    }

}
