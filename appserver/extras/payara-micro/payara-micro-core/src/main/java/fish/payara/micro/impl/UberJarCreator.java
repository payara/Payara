/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2019 Payara Foundation and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 *
 * Class for creating the UberJar
 *
 * @author steve
 */
public class UberJarCreator {

    private File outputFile;
    private String mainClassName;
    private final List<File> runtimeJars = new LinkedList<>();
    private final List<File> domainFiles = new LinkedList<>();
    private File domainDir;
    private final List<File> libs = new LinkedList<>();
    private final List<File> classes = new LinkedList<>();
    private final List<File> deployments = new LinkedList<>();
    private final Map<String, URL> deploymentURLs = new HashMap<>();
    private List<File> copiedFiles = new LinkedList();

    private File deploymentDir;
    private File copyDirectory;
    private final Properties bootProperties = new Properties();
    private Properties loggingProperties;
    private File loggingPropertiesFile;
    private File domainXML;
    private File alternateHZConfigFile;
    private File preBootCommands;
    private File postBootCommands;
    private Properties contextRoots;

    private static final Logger LOGGER = Logger.getLogger(UberJarCreator.class.getName());
    private File postDeployCommands;

    UberJarCreator(String fileName) {
        this(new File(fileName));
    }

    UberJarCreator(File filename) {
        outputFile = filename;
    }

    public void setOutputFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }

    public void setLoggingPropertiesFile(File loggingPropertiesFile) {
        this.loggingPropertiesFile = loggingPropertiesFile;
    }

    public void setAlternateHZConfigFile(File alternateHZConfigFile) {
        this.alternateHZConfigFile = alternateHZConfigFile;
    }
    
    public void setContextRoots(Properties props) {
        contextRoots = props;
    }

    public void setDeploymentDir(File deploymentDir) {
        this.deploymentDir = deploymentDir;
    }

    public void setDomainDir(File domainDir) {
        this.domainDir = domainDir;
    }

    public void setPreBootCommands(File preBootCommands) {
        this.preBootCommands = preBootCommands;
    }

    public void setPostBootCommands(File postBootCommands) {
        this.postBootCommands = postBootCommands;
    }

    public void setPostDeployCommands(File postDeployCommands) {
        this.postDeployCommands = postDeployCommands;
    }

    /**
     * Directory to be copied into the root of the uber Jar file
     * @param copyDirectory
     */
    public void setDirectoryToCopy(File copyDirectory){
        this.copyDirectory = copyDirectory;
    }

    public void addRuntimeJar(File jar) {
        runtimeJars.add(jar);
    }

    public void addDomainFile(File file) {
        domainFiles.add(file);
    }

    public void addLibraryJar(File jar) {
        libs.add(jar);
    }

    public void addDeployment(File jar) {
        deployments.add(jar);
    }

    public void addDeployment(String name, URL url) {
        deploymentURLs.put(name, url);
    }

    public void setDomainXML(File domainXML) {
        this.domainXML = domainXML;
    }

    public void addBootProperties(Properties props) {
        Enumeration names = props.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            bootProperties.setProperty(name, props.getProperty(name));
        }
    }

    public void addBootProperty(String key, String value) {
        bootProperties.setProperty(key, value);
    }

    public void buildUberJar() {
        long start = System.currentTimeMillis();
        LOGGER.info("Building Uber Jar... " + outputFile.getName());
        String entryString;
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile));) {
            // get the current payara micro jar
            URL url = this.getClass().getClassLoader().getResource("MICRO-INF/payara-boot.properties");
            JarURLConnection urlcon = (JarURLConnection) url.openConnection();

            // copy all entries from the existing jar file
            JarFile jFile = urlcon.getJarFile();
            Enumeration<JarEntry> entries = jFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                if (entry.getName().contains("MICRO-INF/domain/") && domainDir != null) {
                    // skip the entry as we will add later
                } else {
                    JarEntry newEntry = new JarEntry(entry.getName());
                    if (entry.getName().endsWith("jar") || entry.getName().endsWith("rar")) {
                        newEntry.setMethod(entry.STORED);
                        newEntry.setSize(entry.getSize());
                        newEntry.setCrc(entry.getCrc());
                        if (entry.getMethod() == JarEntry.STORED) {
                            newEntry.setCompressedSize(entry.getCompressedSize());
                        }
                    }
                    jos.putNextEntry(newEntry);
                    try (InputStream is = getInputStream(jFile, entry)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead = 0;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            jos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                jos.flush();
                jos.closeEntry();
            }

            if (!libs.isEmpty()) {
                for (File lib : libs){
                    JarEntry libEntry = new JarEntry("MICRO-INF/lib/" + lib.getName());
                    libEntry.setMethod(JarEntry.STORED);
                    libEntry.setSize(lib.length());

                    try (CheckedInputStream check = new CheckedInputStream(new FileInputStream(lib), new CRC32());
                         BufferedInputStream in = new BufferedInputStream(check)) {
                        while (in.read(new byte[3000]) != -1){
                            //read in file completly
                        }
                        libEntry.setCrc(check.getChecksum().getValue());
                        jos.putNextEntry(libEntry);
                        Files.copy(lib.toPath(), jos);
                        jos.flush();
                        jos.closeEntry();
                    }
                }
            }

            if (deployments != null) {
                for (File deployment : deployments) {
                    JarEntry deploymentEntry = new JarEntry("MICRO-INF/deploy/" + deployment.getName());
                    jos.putNextEntry(deploymentEntry);
                    Files.copy(deployment.toPath(), jos);
                    jos.flush();
                    jos.closeEntry();
                }
            }

            if (deploymentDir != null) {
                for (File deployment : deploymentDir.listFiles()) {
                    if (deployment.isFile()) {
                        JarEntry deploymentEntry = new JarEntry("MICRO-INF/deploy/" + deployment.getName());
                        jos.putNextEntry(deploymentEntry);
                        Files.copy(deployment.toPath(), jos);
                        jos.flush();
                        jos.closeEntry();

                    }
                }
            }

            if (copyDirectory != null) {
                String basePath = copyDirectory.getCanonicalPath().replaceAll(copyDirectory + "$", "");
                List<File> filesToCopy = fillFiles(copyDirectory);
                for (File file : filesToCopy) {

                        JarEntry deploymentEntry = new JarEntry(file.getCanonicalPath().replace(basePath, ""));
                        jos.putNextEntry(deploymentEntry);
                        Files.copy(file.toPath(), jos);
                        jos.flush();
                        jos.closeEntry();
                }
            }

            // add deployment URLs
            for (Map.Entry<String, URL> deploymentMapEntry : deploymentURLs.entrySet()) {
                URL deployment = deploymentMapEntry.getValue();
                String name = deploymentMapEntry.getKey();
                try (InputStream is = deployment.openStream()) {
                    JarEntry deploymentEntry = new JarEntry("MICRO-INF/deploy/" + name);
                    jos.putNextEntry(deploymentEntry);
                    byte[] buffer = new byte[4096];
                    int bytesRead = 0;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead);
                    }
                    jos.flush();
                    jos.closeEntry();
                } catch (IOException ioe) {
                    LOGGER.log(Level.WARNING, "Error adding deployment " + name + " to the Uber Jar Skipping...", ioe);
                }
            }

            // write the system properties file
            JarEntry je = new JarEntry("MICRO-INF/deploy/payaramicro.properties");
            jos.putNextEntry(je);
            bootProperties.store(jos, "");
            jos.flush();
            jos.closeEntry();
            
            // write context roots
            if (contextRoots != null) {
                JarEntry crs = new JarEntry("MICRO-INF/deploy/contexts.properties");
                jos.putNextEntry(crs);
                contextRoots.store(jos, "");
                jos.flush();
                jos.closeEntry();                
            }

            // add the alternate hazelcast config to the uberJar
            if (alternateHZConfigFile != null) {
                JarEntry hzXml = new JarEntry("MICRO-INF/domain/hzconfig.xml");
                jos.putNextEntry(hzXml);
                Files.copy(alternateHZConfigFile.toPath(), jos);
                jos.flush();
                jos.closeEntry();

            }

            if (domainDir != null) {
                // package up all files in the domain dir but remember to override if specified
                JarEntry domainEntry = new JarEntry("MICRO-INF/domain/");
                jos.putNextEntry(domainEntry);
                // we only care about the config directory
                File configDir = new File(domainDir,"config");
                if (!configDir.exists()) {
                    throw new IOException("Config directory is not in the root directory please check " +domainDir.getAbsolutePath() + " contains a valid domain");
                }
                for (File domainFile : configDir.listFiles()) {
                    if (domainFile.isFile()) {
                        JarEntry configEntry = new JarEntry("MICRO-INF/domain/" + domainFile.getName());
                        jos.putNextEntry(configEntry);

                        if (domainFile.getName().equals("domain.xml") && domainXML != null) {
                            domainFile = domainXML;
                        } else if (domainFile.getName().equals("logging.properties") && (loggingPropertiesFile != null)) {
                            domainFile = loggingPropertiesFile;
                        }
                        Files.copy(domainFile.toPath(), jos);
                        jos.flush();
                        jos.closeEntry();
                    } else if (domainFile.isDirectory() && domainFile.getName().equals("branding")) {
                        JarEntry brandingEntry = new JarEntry("MICRO-INF/domain/branding/");
                        jos.putNextEntry(brandingEntry);
                        for (File brandingFile : domainFile.listFiles()) {
                            JarEntry brandingFileEntry = new JarEntry("MICRO-INF/domain/branding/" + brandingFile.getName());
                            jos.putNextEntry(brandingFileEntry);
                            Files.copy(brandingFile.toPath(), jos);
                            jos.flush();
                            jos.closeEntry();
                        }
                    }

                }

                File applicationsDir = new File(domainDir, "applications");
                if (applicationsDir.exists()){
                    for (File app : fillFiles(applicationsDir)){
                        String path = app.getPath();
                        if (path.endsWith(".war") || path.endsWith(".jar") || path.endsWith(".rar") || path.endsWith(".ear")){
                            JarEntry appEntry = new JarEntry("MICRO-INF/deploy/" + app.getName());
                            appEntry.setSize(app.length());
                            try (CheckedInputStream check = new CheckedInputStream(new FileInputStream(app), new CRC32());
                                 BufferedInputStream in = new BufferedInputStream(check)) {
                                while (in.read(new byte[300]) != -1){
                                //read in file completly
                                }
                                appEntry.setCrc(check.getChecksum().getValue());
                                jos.putNextEntry(appEntry);
                                Files.copy(app.toPath(), jos);
                                jos.flush();
                                jos.closeEntry();
                            }
                        }
                    }
                }
            }
            LOGGER.info("Built Uber Jar " + outputFile.getAbsolutePath() + " in " + (System.currentTimeMillis() - start) + " (ms)");

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error creating Uber Jar " + outputFile.getAbsolutePath(), ex);
        }

    }

    private InputStream getInputStream(JarFile jFile, JarEntry entry) throws IOException {
        if (entry.toString().contains("MICRO-INF/domain/logging.properties") && (loggingPropertiesFile != null)) {
            return new FileInputStream(loggingPropertiesFile);
        } else if (entry.toString().contains("MICRO-INF/post-boot-commands.txt") && (postBootCommands != null)) {
            return new FileInputStream(postBootCommands);
        } else if (entry.toString().contains("MICRO-INF/pre-boot-commands.txt") && (preBootCommands != null)) {
            return new FileInputStream(preBootCommands);
        } else if (entry.toString().contains("MICRO-INF/post-deploy-commands.txt") && (postDeployCommands != null)) {
            return new FileInputStream(postDeployCommands);
        } else if (entry.toString().contains("MICRO-INF/domain/domain.xml") && (domainXML != null)) {
            return new FileInputStream(domainXML);
        } else if (entry.toString().contains("MICRO-INF/domain/keystore.jks") && (System.getProperty("javax.net.ssl.keyStore") != null)) {
            return new FileInputStream(System.getProperty("javax.net.ssl.keyStore"));
        } else if (entry.toString().contains("MICRO-INF/domain/cacerts.jks") && (System.getProperty("javax.net.ssl.trustStore") != null)) {
            return new FileInputStream(System.getProperty("javax.net.ssl.trustStore"));
        } else {
            return jFile.getInputStream(entry);
        }
    }

    /**
     * Returns a list of all files in directory and subdirectories
     * @param directory The parent directory to search within
     * @return
     */
    public static List<File> fillFiles(File directory){
        List<File> allFiles = new LinkedList<>();
        for (File file : directory.listFiles()){
            if (file.isDirectory()){
                allFiles.addAll(fillFiles(file));
            } else {
                if (file.canRead()){
                    allFiles.add(file);
                } else {
                    LOGGER.log(Level.WARNING, "Unable to read file " + file.getAbsolutePath() + ", skipping...");
                }
            }
        }
        return allFiles;
    }

    /**
     * Returns a list of files parsed from a separated list of files.
     * @param fileList list of files
     * @param separator separator used in the list of files
     * @return the list of files
     */
    public static List<File> parseFileList(String fileList, String separator) {
        String[] allJars = fileList.split(separator);
        List<File> files = new ArrayList<>();
        for (String jarName : allJars) {
            File library = new File(jarName);
            if (library.isDirectory()) {
                files.addAll(UberJarCreator.fillFiles(library));
            } else {
                files.add(library);
            }
        }
        return files;
    }

}
