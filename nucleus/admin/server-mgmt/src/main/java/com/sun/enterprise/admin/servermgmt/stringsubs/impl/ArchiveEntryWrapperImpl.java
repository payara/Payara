/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.servermgmt.stringsubs.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarException;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.logging.LogHelper;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.Archive;
import com.sun.enterprise.admin.servermgmt.xml.stringsubs.MemberEntry;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * Handles the operation related with an archive string substitution process.
 * i.e extracting the substitutable entries from jar and rebuilding the jar
 * after substitution operation.
 */
public class ArchiveEntryWrapperImpl implements ArchiveEntryWrapper {
    private static final Logger LOGGER = SLogger.getLogger();
    
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(ArchiveEntryWrapperImpl.class);
    
    /** Prefix for the extraction directory. */
    private static final String EXTRACTION_DIR_PREFIX  = "ext";
    /** A buffer for better jar performance during extraction. */
    private static final byte[] BUFFER = new byte[10000];

    private Archive archive;
    private ArchiveEntryWrapperImpl parent = null;
    private File extractDir = null;
    private JarFile jar = null;
    /** List of all substitutable archive entries including the entries from nested archive. */
    private List<ArchiveMember> allArchiveMembers;
    /** Extracted entries from an archive, doesn't include entry from nested archive. */
    private final Map<String, File> extractedEntries = new HashMap<String, File>();
    /** Substitutable entries count for an archive, doesn't include entry from nested archive. */
    private final AtomicInteger noOfExtractedEntries = new AtomicInteger();

    /**
     * Construct an {@link ArchiveEntryWrapperImpl} for a given archive entry.
     *
     * @throws IOException If any IO error occurs.
     */

    ArchiveEntryWrapperImpl(Archive archive) throws IOException {
        this(archive, null, null);
    }

    /**
     * Construct an {@link ArchiveEntryWrapperImpl} for a given archive entry.
     *
     * @param archive An {@link Archive} entry.
     * @param archivePath Path where the archive resides. <code>null</code>
     * if the archive name contains the path.
     * @throws IOException If any IO error occurs.
     */
    private ArchiveEntryWrapperImpl(Archive archive, String archivePath, ArchiveEntryWrapperImpl parent)
            throws IOException {
        this.archive = archive;
        this.jar = archivePath == null || archivePath.isEmpty() ? new JarFile(archive.getName()) : new JarFile(archivePath + archive.getName());
        this.parent = parent;
        // Create a directory to extract archive members.
        extractDir = SubstitutionFileUtil.setupDir(EXTRACTION_DIR_PREFIX);
        extract();
    }

    @Override
    public ArchiveEntryWrapper getParentArchive() {
        return parent;
    }

    @Override
    public List<? extends ArchiveMember> getSubstitutables() {
        return allArchiveMembers != null ? allArchiveMembers : new ArrayList<ArchiveMember>(1);
    }

    @Override
    public void notifyCompletion() {
        if (noOfExtractedEntries.decrementAndGet() <= 0) {
            try {
                updateArchive();
                SubstitutionFileUtil.removeDir(extractDir);
                if (parent != null) {
                    parent.notifyCompletion();
                }
            }
            catch (IOException e) {
                SubstitutionFileUtil.removeDir(extractDir);
                LogHelper.log(LOGGER, Level.WARNING, SLogger.ERR_ARCHIVE_SUBSTITUTION, e, archive.getName());
            }
        }
    }

    /**
     * Extract all the substitutable entries for an archive.
     * It also takes care of extracting substitutable entries
     * from nested archives.
     * 
     * @throws IOException If any IO error occurs during extraction.
     */
    private void extract() throws IOException {
        for(Object object : archive.getArchiveOrMemberEntry()) {
            String extratFilePath = extractDir.getAbsolutePath() + File.separator;
            if (object instanceof Archive) {
                Archive archive = (Archive)object;
                File file = new File(extratFilePath + archive.getName());
                try {
                    extractEntry(archive.getName(), file);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                extractedEntries.put(archive.getName(), file);
                new ArchiveEntryWrapperImpl(archive, extratFilePath, this);
                noOfExtractedEntries.incrementAndGet();
            } else if (object instanceof MemberEntry) {
                MemberEntry entry = (MemberEntry)object;
                File file = new File(extratFilePath + entry.getName());
                try {
                    extractEntry(entry.getName(), file);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                extractedEntries.put(entry.getName(), file);
                getAllArchiveMemberList().add(new ArchiveMemberHandler(file, this));
                noOfExtractedEntries.incrementAndGet();
            } else {
            	LOGGER.log(Level.WARNING, SLogger.INVALID_ARCHIVE_ENTRY, 
            			new Object[] {object, archive.getName()});
            }
        }
    }

    /**
     * Gets the list which stores all the substitutable entries of an archive.
     *
     * @return List storing the substitutable member entries.
     */
    private List<ArchiveMember> getAllArchiveMemberList() {
        ArchiveEntryWrapperImpl current = this;
        while (current.parent != null) {
            current = current.parent;
        }
        if (current.allArchiveMembers == null ) {
            current.allArchiveMembers = new ArrayList<ArchiveMember>();
        }
        return current.allArchiveMembers;
    }

    /**
     * Extracts entry from jar into the given file.
     *
     * @param jarEntry The jar entry to be extracted.
     * @param file The File to extract the jar entry into.
     * @throws IOException if problem occurred in accessing the jar.
     * @throws IllegalArgumentException If entry is not present in the jar.
     */
    private void extractEntry(String name, File file) throws IOException {
        if (jar == null) {
            throw new JarException("Jar file is closed.");
        }

        JarEntry jarEntry = jar.getJarEntry(name);
        if (jarEntry == null) {
    		String msg = STRINGS.get("invalidArchiveEntry", name, jar.getName());
            throw new IllegalArgumentException(msg);
        }

        if (jarEntry.isDirectory() && !file.exists()) {
            if (!file.mkdirs()) {
                LOGGER.log(Level.INFO, SLogger.DIR_CREATION_ERROR, file.getAbsolutePath());
            }
            return;
        }

        InputStream in = null;
        BufferedOutputStream outputStream = null;
        try {
            in = jar.getInputStream(jarEntry);
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            int i = 0;
            while ((i = in.read(BUFFER)) != -1) {
                outputStream.write(BUFFER, 0, i);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                	if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, STRINGS.get("errorInClosingStream", jar.getName()), e);                		
                	}
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                	if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.log(Level.FINER, STRINGS.get("errorInClosingStream", file.getPath()), e);                		
                	}
                }
            }
        }
    }

    /**
     * Updates the jar with the extracted entries.
     * 
     * @throws IOException If any error occurs during update process.
     */
    void updateArchive() throws IOException {
        if (extractedEntries.isEmpty()) {
        	if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.log(Level.FINER, STRINGS.get("noArchiveEntryToUpdate", archive.getName()));        		
        	}
            return;
        }
        if (jar == null) {
            throw new JarException("Jar file is not in open state, jar path.");
        }
        File tempJarFile = null;
        File jarFile = null;
        FileOutputStream fos = null;
        JarOutputStream jos = null;
        boolean success = false;
        try {
            String jarEntryName = null;
            jarFile = new File(jar.getName());
            tempJarFile = File.createTempFile("helper", ".jar", jarFile.getParentFile());
            fos = new FileOutputStream(tempJarFile);
            jos = new JarOutputStream(fos);
            InputStream is = null;
            Set<String> extractedJarEntries = extractedEntries.keySet();

            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements();) {
                jarEntryName = e.nextElement().getName();
                if (extractedJarEntries.contains(jarEntryName)) {
                    File file = extractedEntries.get(jarEntryName);
                    if(file != null) {
                        JarEntry entry = new JarEntry(jarEntryName);
                        is = new FileInputStream(file);
                        appendEntry(jos, entry, is);
                    }
                } else {
                    JarEntry je = jar.getJarEntry(jarEntryName);
                    is = jar.getInputStream(je);
                    appendEntry(jos, je, is);
                }
            }
            success = true;
        }
        catch (Exception e) {
        	LogHelper.log(LOGGER, Level.SEVERE, SLogger.ERR_CLOSING_STREAM, e, archive.getName());
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch(IOException e) {
                	if (LOGGER.isLoggable(Level.FINER)) {
                		LOGGER.log(Level.FINER, STRINGS.get("errorInClosingStream", jarFile.getPath()), e);
                	}
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch(IOException e) {
                	if (LOGGER.isLoggable(Level.FINER)) {
                		LOGGER.log(Level.FINER, STRINGS.get("errorInClosingStream", jarFile.getPath()), e);
                	}
                }
            }
            try {
                jar.close();
            } catch(IOException e) {
            	if (LOGGER.isLoggable(Level.FINER)) {
            		LOGGER.log(Level.FINER, "Problem occurred while closing the jar file : " + jarFile.getPath(), e);
            	}
            }

            if(jarFile != null && !jarFile.delete()) {
                if (tempJarFile != null && !tempJarFile.delete()) {
                    LOGGER.log(Level.SEVERE, SLogger.ERR_CLOSING_STREAM, tempJarFile.getAbsolutePath());
                }
                throw new IOException(jarFile.getPath() + " did not get updated. Unable to delete.");
            } else if(!success) {
                if (tempJarFile != null && !tempJarFile.delete()) {
                    LOGGER.log(Level.INFO, STRINGS.get("errorInClosingStream", tempJarFile.getAbsolutePath()));
                }
            }
            else if (tempJarFile != null && !tempJarFile.renameTo(jarFile)) {
                LOGGER.log(Level.SEVERE, SLogger.ERR_RENAMING_JAR,
                        new Object[] {tempJarFile.getName(),  jarFile.getName()});
            }
        }
    }

    /**
     * Adds the given entry into the jar and writes the entry data from the InputStream.
     *
     * @param jarEntry The jar entry we are writing as a JarEntry.
     * @param is The InputStream to read the jar entry data from.
     * @throws IOException if there are problems accessing the jar.
     */
    private  void appendEntry(JarOutputStream jos, JarEntry jarEntry, InputStream is)
            throws IOException {
        jos.putNextEntry(jarEntry);
        if(!jarEntry.isDirectory()) {
            int i = 0;
            while ((i = is.read(BUFFER)) != -1) {
                jos.write(BUFFER, 0, i);
            }
            is.close();
            jos.flush();
        }
        jos.closeEntry();
    }
}