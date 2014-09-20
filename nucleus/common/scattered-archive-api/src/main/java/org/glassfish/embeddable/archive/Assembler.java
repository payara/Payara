/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.embeddable.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * @author bhavanishankar@java.net
 */

class Assembler {

    public URI assemble(ScatteredArchive archive) throws IOException {
        switch (archive.type) {
            case WAR:
                return assembleWAR(archive.name, archive.rootDirectory,
                        archive.classpaths, archive.metadatas);
            case JAR:
                return assembleJAR(archive.name, archive.rootDirectory,
                        archive.classpaths, archive.metadatas);
            case RAR:
                return assembleRAR(archive.name, archive.rootDirectory,
                        archive.classpaths, archive.metadatas);
        }
        return null; // will never come here.
    }

    public URI assemble(ScatteredEnterpriseArchive archive) throws IOException {
        return assembleEAR(archive.name, archive.archives, archive.metadatas);
    }

    private URI assembleEAR(String name, Map<String, File> archives,
                            Map<String, File> metadatas) throws IOException {
        File ear = new File(System.getProperty("java.io.tmpdir"), name + ".ear");
        ear.deleteOnExit();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(ear));
        for (Map.Entry<String, File> me : metadatas.entrySet()) {
            tranferFile(me.getValue(), jos, me.getKey(), false);
        }
        for (Map.Entry<String, File> ame : archives.entrySet()) {
            File archive = ame.getValue();
            if (archive.isDirectory()) {
                archive = new File(assembleJAR(ame.getKey(), archive,
                        Collections.EMPTY_LIST, Collections.EMPTY_MAP));
            }
            tranferFile(archive, jos, ame.getKey(), false);
        }
        jos.close();
        return ear.toURI();

    }

    URI assembleWAR(String name, File rootDirectory, List<File> classpaths,
                    Map<String, File> metadatas) throws IOException {
        File archive = new File(System.getProperty("java.io.tmpdir"), name + ".war");
        archive.deleteOnExit();

        JarOutputStream jos = new JarOutputStream(new FileOutputStream(archive));

        transferDir(rootDirectory, jos, "");
        for (Map.Entry<String, File> me : metadatas.entrySet()) {
            tranferFile(me.getValue(), jos, me.getKey(), false);
        }
        for (File classpath : classpaths) {
            if (classpath.isDirectory()) {
                transferDir(classpath, jos, "WEB-INF/classes/");
            } else {
                tranferFile(classpath, jos, "WEB-INF/lib/" + classpath.getName(), false);
            }
        }
        jos.close();
        return archive.toURI();
    }

    URI assembleJAR(String name, File rootDirectory, List<File> classpaths,
                    Map<String, File> metadatas) throws IOException {
        File archive = new File(System.getProperty("java.io.tmpdir"), name + ".jar");
        archive.deleteOnExit();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(archive));
        transferDir(rootDirectory, jos, "");
        for (Map.Entry<String, File> me : metadatas.entrySet()) {
            tranferFile(me.getValue(), jos, me.getKey(), false);
        }
        for (File classpath : classpaths) {
            if (classpath.isDirectory()) {
                transferDir(classpath, jos, "");
            } else {
                tranferFile(classpath, jos, "", true);
            }
        }
        jos.close();
        return archive.toURI();
    }

    URI assembleRAR(String name, File rootDirectory, List<File> classpaths,
                    Map<String, File> metadatas) throws IOException {
        File rar = new File(System.getProperty("java.io.tmpdir"), name + ".rar");
        rar.deleteOnExit();
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(rar));
        transferDir(rootDirectory, jos, "");
        for (Map.Entry<String, File> me : metadatas.entrySet()) {
            tranferFile(me.getValue(), jos, me.getKey(), false);
        }

        // Make a single connector jar out of all the classpath directories and add it to the RAR file.
        List<File> classpathDirs = new ArrayList();
        for (File classpath : classpaths) {
            if (classpath.isDirectory()) {
                classpathDirs.add(classpath);
            }
        }
        if (!classpathDirs.isEmpty()) {
            // Compute a unique connector jar name
            String connectorJarName = name;
            List<String> topLevelFileNames = new ArrayList();
            topLevelFileNames.addAll(getFileNames(classpaths));
            topLevelFileNames.addAll(getFileNames(rootDirectory));
            int count = 0;
            while (topLevelFileNames.contains(connectorJarName + ".jar")) {
                connectorJarName = name + "_" + count;
                ++count;
            }
            File connectorJar = new File(assembleJAR(connectorJarName, null,
                    classpathDirs, Collections.EMPTY_MAP));
            tranferFile(connectorJar, jos, connectorJar.getName(), false);
        }

        // Add all the classpath files to the RAR files.
        for (File classpath : classpaths) {
            if (!classpath.isDirectory()) {
                tranferFile(classpath, jos, classpath.getName(), false);
            }
        }
        jos.close();
        return rar.toURI();
    }

    void transferDir(File dir, JarOutputStream jos, String entryNamePrefix)
            throws IOException {
        transferDir(dir, dir, jos, entryNamePrefix);
    }

    void transferDir(File basedir, File dir, JarOutputStream jos, String entryNamePrefix)
            throws IOException {
        if (dir == null || jos == null) {
            return;
        }
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String entryName = entryNamePrefix +
                        f.getPath().substring(basedir.getPath().length() + 1);
                tranferFile(f, jos, entryName, false);
            } else {
                transferDir(basedir, f, jos, entryNamePrefix);
            }
        }
    }

    void tranferFile(File file, JarOutputStream jos, String entryName, boolean explodeFile)
            throws IOException {
        if (explodeFile) {
            tranferEntries(file, jos);
        } else {
            transferFile(file, jos, entryName);
        }
    }

    void transferFile(File file, JarOutputStream jos, String entryName) throws IOException {
        if (file == null || jos == null || entryName == null) {
            return;
        }
        ZipEntry entry = new ZipEntry(entryName);
        try {
            jos.putNextEntry(entry);
        } catch (ZipException ex) {
            return;
        }
        FileInputStream fin = new FileInputStream(file);
        transferContents(fin, jos);
        jos.closeEntry();
    }

    void tranferEntries(File file, JarOutputStream jos) throws IOException {
        if (file == null || jos == null) {
            return;
        }
        JarFile jarFile = new JarFile(file);
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory() && !exclude(entry)) {
                InputStream in = jarFile.getInputStream(entry);
                try {
                    jos.putNextEntry(new ZipEntry(entry.getName()));
                } catch (ZipException ex) {
                    continue;
                }
                transferContents(in, jos);
                jos.closeEntry();
            }
        }
    }

    void transferContents(InputStream fin, JarOutputStream jos)
            throws IOException {
        if (fin == null || jos == null) {
            return;
        }
        int read = 0;
        byte[] buffer = new byte[8192];
        while ((read = fin.read(buffer, 0, buffer.length)) != -1) {
            jos.write(buffer, 0, read);
        }
        jos.flush();
    }

    private List<String> getFileNames(File directory) {
        if (directory != null && directory.exists() && directory.isDirectory()) {
            return getFileNames(Arrays.asList(directory.listFiles()));
        }
        return Collections.EMPTY_LIST;
    }

    private List<String> getFileNames(List<File> files) {
        List<String> result = new ArrayList();
        for (File f : files) {
            if (!f.isDirectory()) {
                result.add(f.getName());
            }
        }
        return result;
    }

    private static final List<Pattern> EXCLUDE_JAR_ENTRIES = new ArrayList();

    static {
        EXCLUDE_JAR_ENTRIES.add(Pattern.compile("META-INF/MANIFEST\\.MF"));
        EXCLUDE_JAR_ENTRIES.add(Pattern.compile("META-INF/.*\\.RSA"));
        EXCLUDE_JAR_ENTRIES.add(Pattern.compile("META-INF/.*\\.inf"));
        EXCLUDE_JAR_ENTRIES.add(Pattern.compile("META-INF/.*\\.SF"));
//        EXCLUDE_FILES.add(Pattern.compile("\\.svn.*"));
//        EXCLUDE_FILES.add(Pattern.compile("WEB-INF/\\.svn.*"));
    }

    private boolean exclude(JarEntry entry) {
        for (Pattern p : EXCLUDE_JAR_ENTRIES) {
            if (p.matcher(entry.getName()).matches()) {
                return true;
            }
        }
        return false;
    }
}
