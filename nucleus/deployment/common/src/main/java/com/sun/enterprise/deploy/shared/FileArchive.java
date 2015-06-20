/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deploy.shared;

import com.sun.enterprise.deployment.deploy.shared.Util;
import com.sun.enterprise.util.io.FileUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.net.URI;
import java.nio.file.Files;

import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

/**
 * This implementation of the Archive interface maps to a directory/file
 * structure.
 * <p>
 * If the directory underlying the FileArchive is created by GlassFish
 * then FileArchive filters its contents so only
 * those files more recent than the creation of the archive itself are visible to
 * consumers.
 * <p>
 * The main motivation is to hide unwanted "left-over" files
 * from previous deployments that might linger, especially on Windows,
 * after the previous app had been undeployed.  (Deployment uses a FileArchive
 * to extract the user's JAR-based archive into the applications directory.)
 * Historically such left-over files arise after GlassFish expands an archive
 * into its exploded form but then some
 * code opens but does not close a file in that exploded directory tree.
 * <p>
 * An open left-over file can be overwritten-in-place on Windows, and
 * this happens when a caller invokes {@link #putNextEntry(java.lang.String) }
 * to create a new entry (file) inside the archive.  But a
 * left-over file that is not in the new app but is
 * still open by GlassFish cannot be deleted or renamed on Windows and so it will
 * remain in the expansion directory.  Such left-over files, if not filtered out,
 * can confuse GlassFish and the application.  By "stamping" the archive
 * creation date we can filter out such old, left-over files.
 * <p>
 * To support this feature, when FileArchive creates a directory it stores a
 * marker file there, the contents of which records the creation date/time of
 * the archive.  We cannot just use the lastModified value for the top-level
 * directory. Users might legitimately use "touch .reload" in the applications/appName
 * directory to trigger a dynamic reload of the app. If .reload does not already
 * exist then touch creates it, and this would update the lastModified of the
 * directory file.
 *
 * @author Jerome Dochez
 * @author Tim Quinn
 */
@Service(name="file")
@PerLookup
public class FileArchive extends AbstractReadableArchive implements WritableArchive {

    private final static Level DEBUG_LEVEL = Level.FINE;

    @Inject
    ArchiveFactory archiveFactory;
    
    // the archive abstraction directory.
    File archive = null;
    URI uri = null;
    
    // the currently opened entry
    OutputStream os=null;

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Attempt to list files in {0} failed, perhaps because that is not a valid directory or because file permissions do not allow GlassFish to access it", level="WARNING")
    private static final String FILE_LIST_FAILURE = "NCLS-DEPLOYMENT-00022";

    @LogMessageInfo(message = "Ignoring {0} because the containing archive {1} recorded it as a pre-existing stale file", level="WARNING")
    private static final String STALE_FILES_SKIPPED = "NCLS-DEPLOYMENT-00023";

    /*
     * tracks stale files in the archive and filters the archive's contents to
     * exclude stale entries
     */
    private StaleFileManager staleFileManager;
    
    /*
     * Records whether open or create has been invoked.  Otherwise we can't
     * be sure that the staleFileManager field has been set.
     */
    private boolean isOpenedOrCreated = false;

    /** 
     * Open an abstract archive
     * @param uri path to the archive
     */
    @Override
    public void open(URI uri) throws IOException {
        if (!uri.getScheme().equals("file")) {
            throw new IOException("Wrong scheme for FileArchive : " + uri.getScheme());
        }
        this.uri = uri;
        archive = new File(uri);
        if (!archive.exists()) {
            throw new FileNotFoundException(uri.getSchemeSpecificPart());
        }
        isOpenedOrCreated = true;
        staleFileManager = StaleFileManager.Util.getInstance(archive);
    }

    /**
     * @see #open(URI)
     * @param uri a string representing URI
     */
    public void open(String uri) throws IOException
    {
        open(URI.create(uri));
    }

    /**
     * Get the size of the archive
     * @return tje the size of this archive or -1 on error
     */
    @Override
    public long getArchiveSize() throws NullPointerException, SecurityException {
        if(uri == null) {
            return -1;
        }
        File tmpFile = new File(uri);
        return(tmpFile.length());
    }
    
    /** 
     * creates a new abstract archive with the given path
     * @param uri path to create the archive
     */
    @Override
    public void create(URI uri) throws IOException {


        this.uri = uri;
        archive = new File(uri);
        /*
         * Get the stale file manager before creating the directories; it's
         * slightly faster that way.
         */
        staleFileManager = StaleFileManager.Util.getInstance(archive);
        if (!archive.exists() && !archive.mkdirs()) {
          throw new IOException("Unable to create directory for " + archive.getAbsolutePath());
        }
        isOpenedOrCreated = true;
    }

    /**
     * Close a previously returned sub archive
     *
     * @param subArchive output stream to close
     * @link Archive.getSubArchive}
     */
    @Override
    public void closeEntry(WritableArchive subArchive) throws IOException {
        subArchive.close();

    }

    /**
     * close the abstract archive
     */
    @Override
    public void close() throws IOException {
    }
           
    /**
     * delete the archive
     */
    @Override
    public boolean delete() {
        // delete the directory structure...
        try {
            final boolean result = deleteDir(archive);
            /*
             * Create the stale file marker file, if needed.
             */
            StaleFileManager.Util.markDeletedArchive(this);
            return result;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isDirectory(String name) {
        final File candidate = new File(this.archive, name);
        return isEntryValid(candidate) && candidate.isDirectory();
    }

    /**
     * @return an @see java.util.Enumeration of entries in this abstract
     * archive
     */
    @Override
    public Enumeration entries() {
        final List namesList = new ArrayList();
        getListOfFiles(archive, namesList, null);
        return Collections.enumeration(namesList);
    }

    /**
     * Returns the enumeration of first level directories in this
     * archive
     * @return enumeration of directories under the root of this archive
     */
    @Override
    public Collection<String> getDirectories() throws IOException {
        List<String> results = new ArrayList<String>();
        if (archive != null)  {
            for (File f : archive.listFiles()) {
                if (f.isDirectory() && isEntryValid(f)) {
                    results.add(f.getName());
                }
            }
        }
        return results;
    }

    /**
     *  @return an @see java.util.Enumeration of entries in this abstract
     * archive, providing the list of embedded archive to not count their 
     * entries as part of this archive
     */
     public Enumeration entries(Enumeration embeddedArchives) {
     	List<String> nameList = new ArrayList<String>();
        List massagedNames = new ArrayList();
	while (embeddedArchives.hasMoreElements()) {
		String subArchiveName  = (String) embeddedArchives.nextElement();
                massagedNames.add(FileUtils.makeFriendlyFilenameExtension(subArchiveName));
	}        
     	getListOfFiles(archive, nameList, massagedNames);
     	return Collections.enumeration(nameList);
     }

    /** 
     * Returns an enumeration of the module file entries with the
     * specified prefix.  All elements in the enumeration are of 
     * type String.  Each String represents a file name relative 
     * to the root of the module. 
     * 
     * @param prefix the prefix of entries to be included
     * @return an enumeration of the archive file entries. 
     */ 
    @Override
    public Enumeration<String> entries(String prefix) {
        prefix = prefix.replace('/', File.separatorChar);
        File file = new File(archive, prefix);
        List<String> namesList = new ArrayList<String>();
        getListOfFiles(file, namesList, null);
        return Collections.enumeration(namesList);
    }
    
    /**
     * @return true if this archive exists
     */
    @Override
    public boolean exists() {
        if (archive != null) {
            return archive.exists();
        }
        return false;
    }

    /**
     *
     * create or obtain an embedded archive within this abstraction.
     *
     * @param name name of the embedded archive.
     */
    @Override
    public ReadableArchive getSubArchive(String name) throws IOException {
        String subEntryName = getFileSubArchivePath(name);
        File subEntry = new File(subEntryName);
        if (subEntry.exists() && isEntryValid(subEntry)) {
            deplLogger.log(DEBUG_LEVEL, "FileArchive.getSubArchive for {0} found that it is valid",
                    subEntry.getAbsolutePath());
            ReadableArchive result = archiveFactory.openArchive(subEntry);
            if (result instanceof AbstractReadableArchive) {
                ((AbstractReadableArchive) result).setParentArchive(this);
            }
            return result;
        } else if (subEntry.exists()) {
            deplLogger.log(DEBUG_LEVEL, "FileArchive.getSubArchive for {0} found that it is not a valid entry; it is stale",
                    subEntry.getAbsolutePath());
        }
        return null;
    }
    /**
     * create or obtain an embedded archive within this abstraction.
     *
     * @param name name of the embedded archive.
     */
    @Override
    public WritableArchive createSubArchive(String name) throws IOException {
        String subEntryName = getFileSubArchivePath(name);
        File subEntry = new File(subEntryName);
        if (!subEntry.exists()) {
            // time to create a new sub directory
            if (!subEntry.exists() && !subEntry.mkdirs()) {
              throw new IOException("Unable to create directory for " + subEntry.getAbsolutePath());
            }
            deplLogger.log(DEBUG_LEVEL, "FileArchive.createSubArchive created dirs for {0}",
                subEntry.getAbsolutePath());
        } else {
            deplLogger.log(DEBUG_LEVEL, "FileArchive.createSubArchive found existing dir for {0}",
                    subEntry.getAbsolutePath());
            /*
             * This subdirectory already exists, so it might be marked as
             * stale. Because this invocation is creating the subarchive in
             * the current archive, the subdirectory is no longer stale.
             */
            staleFileManager().recordValidEntry(subEntry);
        }
        final WritableArchive result = archiveFactory.createArchive(subEntry);
        if (result instanceof AbstractReadableArchive) {
            ((AbstractReadableArchive) result).setParentArchive(this);
        }
        return result;
    }

    /**
     *
     * create or obtain an embedded archive within this abstraction.
     *
     * @param name name of the embedded archive.
     */
    private String getFileSubArchivePath(String name) throws IOException {
       // Convert name to native form. See bug #6345029 for more details.
       name = name.replace('/', File.separatorChar);
       File file = new File(name);
       File subDir;
       if (file.isAbsolute()) {
           subDir = file;
       } else {
           // first we try to see if a sub directory with the right file
           // name exist
           subDir = new File(archive, FileUtils.makeFriendlyFilenameExtension(name));
       	   if (!subDir.exists()) {       	  
               // now we try to open a sub jar file...
               subDir = new File(archive, name);
               if (!subDir.exists()) {
                   // ok, nothing worked, reassing the name to the 
                   // sub directory one
                  subDir = new File(archive, FileUtils.makeFriendlyFilenameExtension(name));
              }                  
       	   }
       }
       return subDir.getPath();
    }
    
    /**
     * Returns the existence of the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.     
     * @return the existence the given entry name.
     */
    @Override
    public boolean exists(String name) throws IOException {
        name = name.replace('/', File.separatorChar);
        File input = new File(archive, name);
        return input.exists() && isEntryValid(input);
    }

    /**
     * @return a @see java.io.InputStream for an existing entry in
     * the current abstract archive
     * @param name the entry name
     */
    @Override
    public InputStream getEntry(String name) throws IOException {
            
        File input = getEntryFile(name);
        if (!input.exists() || input.isDirectory()
            || ! isEntryValid(input)) { // If name corresponds to directory, return null as it can not be opened
            return null;
        }
        FileInputStream fis = new FileInputStream(input);
        try {
            BufferedInputStream bis = new BufferedInputStream(fis);
            return bis;
        } catch (Throwable tx) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable thr) {
                    throw new IOException("Error closing FileInputStream after error opening BufferedInputStream for entry " + name, thr);
                }
            }
            throw new IOException("Error opening BufferedInputStream for entry " + name, tx);
        }
    }

    private File getEntryFile(String name) {
        name = name.replace('/', File.separatorChar);
        return new File(archive, name);
    }

    /**
     * Returns the entry size for a given entry name or 0 if not known
     *
     * @param name the entry name
     * @return the entry size
     */
    @Override
    public long getEntrySize(String name) {
        name = name.replace('/', File.separatorChar);
        File input = new File(archive, name);
        if (!input.exists() || ! isEntryValid(input)) {
            return 0;
        }
        return input.length();
    }

    /**
     * @return the manifest information for this abstract archive
     */
    @Override
    public Manifest getManifest() throws IOException {
        InputStream is = null;
        try {
            is = getEntry(JarFile.MANIFEST_NAME);
            if (is!=null) {
                Manifest m = new Manifest(is);
                return m;
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return null;
    }

    /**
     * Returns the URI used to create or open the underlyong archive
     *
     * @return the URI for this archive.
     */
    @Override
    public URI getURI() {
        return uri;
    }

    /**
     * rename the archive
     *
     * @param name the archive name
     */
    @Override
    public boolean renameTo(String name) {
        return FileUtils.renameFile(archive, new File(name));
    }
    
    /**
     * Reports whether the entry is valid, in the sense that if this
     * archive has been created during this execution then the entry
     * requested was created later than the archive itself.
     * <p>
     * It is possible (for example, on Windows) for GlassFish to want to create
     * a new archive in a directory that already exists and contains stale
     * "left-over" files from a previous deployment, for example.  This method
     * causes the FileArchive implementation to hide any files that 
     * reside in the directory for an archive that was created during this VM 
     * execution but were not explicitly added to the archive using putNextEntry.
     * 
     * @param entry file to check
     * @return
     */
    private boolean isEntryValid(final File entry) {
        return isEntryValid(entry, true, deplLogger);
    }

    private boolean isEntryValid(final File entry, final boolean isLogging) {
        return isEntryValid(entry, isLogging, deplLogger);
    }

    private boolean isEntryValid(final File entry, final boolean isLogging, final Logger logger) {
        return staleFileManager().isEntryValid(entry, isLogging, logger);
    }

    private StaleFileManager myStaleFileManager() {
       /*
        * If the FileArchive has been opened or created then its
        * staleFileManager has been set.
        */
       if ( ! isOpenedOrCreated) {
           throw new IllegalStateException();
       }
       return staleFileManager;
    }
    
    private StaleFileManager staleFileManager() {
        ReadableArchive parent = getParentArchive();
        if (parent == null) {
            return myStaleFileManager();
        }
        if (parent instanceof FileArchive) {
            return ((FileArchive) parent).staleFileManager();
        } else {
            return null;
        }
    }

    /**
     * Reports whether the entry is valid, in the sense that the entry is
     * more recent than the archive itself.
     * @param entryName name of the entry to check
     * @return
     */
    private boolean isEntryValid(final String entryName, final Logger logger) {
        return isEntryValid(getEntryFile(entryName), true, logger);
    }
    
    /**
     * utility method for deleting a directory and all its content
     */
    private boolean deleteDir(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new FileNotFoundException(directory.getPath());
        }

        boolean allDeletesSucceeded = true;
        // delete contents

        /*
        *Do not recursively delete the contents if the current directory
        *is a symbolic link.
        */
        
        /*
         * Fix for bug Glassfish-21261 , method safeIsRealDirectory(File) might return false in case if the currently directory 
         * has a symbolic link in its hierarchy and the currently directory itself might not be a symbolic link.
         */
        if (!Files.isSymbolicLink(directory.toPath())) {
            File[] entries = directory.listFiles();
            for (int i=0;i<entries.length;i++) {
                if (entries[i].isDirectory()) {
                    allDeletesSucceeded &= deleteDir(entries[i]);
                } else {
                    if ( ! entries[i].equals(StaleFileManager.Util.markerFile(archive))) {
                        final boolean fileDeleteOK = FileUtils.deleteFileWithWaitLoop(entries[i]);
                        if (fileDeleteOK) {
                            myStaleFileManager().recordDeletedEntry(entries[i]);
                        }
                        allDeletesSucceeded &= fileDeleteOK;
                    }
                }
            }
        }
        // delete self (the directory or the symbolic link)
        return (allDeletesSucceeded && FileUtils.deleteFileWithWaitLoop(directory));
    } 
    
    /**
     * utility method for getting contents of directory and 
     * sub directories
     */

    private void getListOfFiles(File directory, List<String> files, List embeddedArchives) {
        getListOfFiles(directory, files, embeddedArchives, deplLogger);
    }

    /**
     * Adds the files in the specified directory to the collection of files
     * already assembled.  Excludes the contents of embedded archives in the current archive which
     * appear in the file tree anchored at the given directory.
     * @param directory the directory to scan for files
     * @param files collection of files already assembled to which this directory's files are to be added
     * @param embeddedArchives collection of embedded archives in the current archive
     * @param logger logger to which to report inability to get the list of files from the directory
     */
    void getListOfFiles(File directory, List<String> files, List embeddedArchives, final Logger logger) {
        if(archive == null || directory == null || !directory.isDirectory())
            return;
        final File[] fileList = directory.listFiles();
        if (fileList == null) {
            deplLogger.log(Level.WARNING,
                           FILE_LIST_FAILURE,
                           directory.getAbsolutePath());
             return;
        }
        for (File aList : fileList) {
            String fileName = aList.getAbsolutePath().substring(archive.getAbsolutePath().length() + 1);
            fileName = fileName.replace(File.separatorChar, '/');
            if (!aList.isDirectory()) {
                if (!fileName.equals(JarFile.MANIFEST_NAME) && isEntryValid(fileName, logger)) {
                    files.add(fileName);
                }
            } else if (isEntryValid(fileName, logger)) {
                files.add(fileName); // Add entry corresponding to the directory also to the list
                if (embeddedArchives != null) {
                    if (!embeddedArchives.contains(fileName)) {
                        getListOfFiles(aList, files, null, logger);
                    }
                } else {
                    getListOfFiles(aList, files, null, logger);
                }
            }
        }
    }          

    /** @return true if this archive abstraction supports overwriting of elements
     *
     */
    public boolean supportsElementsOverwriting() {
        return true;
    }
    
    /** delete an entry in the archive
     * @param name the entry name
     * @return true if the entry was successfully deleted
     *
     */
    public boolean deleteEntry(String name) {
        return deleteEntry(name, true);
    }

    private boolean deleteEntry(String name, final boolean isLogging) {
        name = name.replace('/', File.separatorChar);
        File input = new File(archive, name);
        if (!input.exists() || ! isEntryValid(input, isLogging)) {
            return false;
        }
        final boolean result = input.delete();
        myStaleFileManager().recordDeletedEntry(input);
        return result;
    }

    /**
     * Closes the current entry
     */
    @Override
    public void closeEntry() throws IOException {
        if (os!=null) {
            os.flush();
            os.close();
            os = null;
        }
    }
    
    /**
     * @returns an @see java.io.OutputStream for a new entry in this
     * current abstract archive.
     * @param name the entry name
     */    
    @Override
    public OutputStream putNextEntry(String name) throws java.io.IOException {
        name = name.replace('/', File.separatorChar);
        
        File newFile = new File(archive, name);
        if (newFile.exists()) {
            if (!deleteEntry(name, false /* isLogging */) && uri != null) {
                deplLogger.log(Level.FINE, 
                        "Could not delete file {0} in FileArchive {1} during putNextEntry; continuing", 
                        new Object[]{name, uri.toASCIIString()});
            }
        }
        // if the entry name contains directory structure, we need
        // to create those directories first.
        if (name.lastIndexOf(File.separatorChar)!=-1) {
            String dirs = name.substring(0, name.lastIndexOf(File.separatorChar));    
            File dirsFile = new File(archive, dirs);
            if (!dirsFile.exists() && !dirsFile.mkdirs()) {
              throw new IOException("Unable to create directory for " + dirsFile.getAbsolutePath());
            }
        }
        staleFileManager().recordValidEntry(newFile);
        os = new BufferedOutputStream(new FileOutputStream(newFile));
        return os;   
    }

    /**
     * Returns the name portion of the archive's URI.
     * <p>
     * For FileArhive the name is all of the path that follows
     * the last slash (ignoring a slash at the end of the path).  
     * <p>
     * Here are some example archive names for the specified FileArchive paths:
     * <ul>
     * <li>/a/b/c/d/ -> d
     * <li>/a/b/c/d  -> d
     * <li>/a/b/c.jar -> c.jar
     * </ul>
     * @return the name of the archive
     * 
     */
    @Override
    public String getName() {
        return Util.getURIName(getURI());
    }

    /**
     * API which FileArchive methods should use for dealing with the StaleFileManager
     * implementation.
     */
    public static interface StaleFileManager {

        /**
         * Returns whether the specified file is valid - that is, is dated
         * after the archive was created.
         * @param f the file to check
         * @param isLogging whether to log a warning about the check of the entry
         * @return true if the file is valid; false otherwise
         */
        boolean isEntryValid(File f, boolean isLogging);

        boolean isEntryValid(File f, boolean isLogging, Logger logger);

        /**
         * Returns whether the specified file is for the hidden timestamp file
         * which FileArchive uses internally.
         * @param f the file to check
         * @return true if the File is the hidden timestamp file; false otherwise
         */
        boolean isEntryMarkerFile(File f);

        void recordValidEntry(File f);

        void recordDeletedEntry(File f);

        void flush();

        public class Util {

            private final static String MARKER_FILE_PATH = ".glassfishStaleFiles";
            
            private static File markerFile(final File archive) {
                return new File(archive, MARKER_FILE_PATH);
            }

            /**
             * Creates a marker file in the archive directory - if it still
             * exists and contains any stale files.
             * @param archive the File for the archive to mark
             */
            public static void markDeletedArchive(final Archive archive) {
                if ( ! (archive instanceof FileArchive)) {
                    return;
                }
                
                final File archiveFile = new File(archive.getURI());
                markDeletedArchive(archiveFile);
            }

            /**
             * Creates a marker file in the archive directory - if it still
             * exists and contains any stale files.
             * @param archive the File for the archive to mark
             */
            public static void markDeletedArchive(final File archiveFile) {
                if ( ! archiveFile.exists()) {
                    return;
                }

                final Iterator<File>staleFileIt = findFiles(archiveFile);
                if ( ! staleFileIt.hasNext()) {
                    return;
                }

                final URI archiveURI = archiveFile.toURI();
                PrintStream ps = null;
                try {
                    ps = new PrintStream(markerFile(archiveFile));
                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
                for ( ; staleFileIt.hasNext(); ) {
                    final URI relativeURI = archiveURI.relativize(staleFileIt.next().toURI());
                    ps.println(relativeURI);
                    deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager recording left-over file {0}", relativeURI);
                }
                ps.close();
            }

            /**
             * Returns an Iterator over the files contained in the directory tree
             * anchored at the given directory, excluding any stale file
             * marker file.
             * <p>
             * For efficiency, this implementation avoids creating a list of
             * all the files in the directory tree all at once.  It traverses
             * each directory as it encounters it.
             * @param dir root of the directory tree to be traversed
             * @return Iterator over the contained files
             */
            private static Iterator<File> findFiles(final File dir) {
                return new Iterator<File>() {

                    private final List<File> fileList;
                    private final ListIterator<File> fileListIt;

                    {
                        fileList = new ArrayList<File>(Arrays.asList(dir.listFiles(
                                new MarkerExcluderFileFilter())));
                        fileListIt = fileList.listIterator();
                    }


                    @Override
                    public boolean hasNext() {
                        return fileListIt.hasNext();
                    }

                    @Override
                    public File next() {

                        final File result = fileListIt.next();
                        if (result.isDirectory()) {
                            for (File f : result.listFiles(
                                    new MarkerExcluderFileFilter())) {
                                fileListIt.add(f);
                                /*
                                 * Back up so the next invocation of this method
                                 * will return the just-added entry.
                                 */
                                fileListIt.previous();
                            }
                        }
                        return result;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }

                };
            }

            private static final class MarkerExcluderFileFilter implements FileFilter {

                @Override
                public boolean accept(File pathname) {
                    return ! pathname.getName().equals(MARKER_FILE_PATH);
                }
            }

            /**
             * Factory method for a StaleFileManager.
             * <p>
             * Callers should invoke this method only after they have finished with
             * the FileArchive and have tried to delete it.  If the directory
             * for the archive remains then it contains one or more stale files
             * that could not be deleted, and the factory method returns a
             * instance that tracks the stale files.  If the directory no longer
             * exists then the delete succeeded, there are
             * @param archive the directory to contain the archive
             * @return StaleFileManager for the FileArchive to use
             */
            public static StaleFileManager getInstance(final File archive) throws IOException {
                if (archive.exists()) {
                    return new StaleFileManagerImpl(archive);
                } else {
                    return new StaleFileManagerImplNoop();
                }
            }
        }
    }

    /**
     * Acts as a stale file manager but does no real work.
     * <p>
     * Used as a stale file manager for an archive that was successfully
     * deleted and therefore contains no stale files.
     */
    private static class StaleFileManagerImplNoop implements StaleFileManager {

        @Override
        public boolean isEntryValid(File f, boolean isLogging) {
            return true;
        }

        @Override
        public boolean isEntryValid(File f, boolean isLogging, Logger logger) {
            return true;
        }

        @Override
        public boolean isEntryMarkerFile(File f) {
            return false;
        }

        @Override
        public void recordValidEntry(File f) {
        }

        @Override
        public void recordDeletedEntry(File f) {
        }

        @Override
        public void flush() {
        }
    }

    /**
     * Implements stale file manager that might contain stale files.
     */
    private static class StaleFileManagerImpl implements StaleFileManager {

        private final static String LINE_SEP = System.getProperty("line.separator");
        private final File archiveFile;
        private final URI archiveURI;
        private final Collection<String> staleEntryNames;
        private final File markerFile;
        
        private StaleFileManagerImpl(final File archive) throws FileNotFoundException, IOException {
            archiveFile = archive;
            archiveURI = archive.toURI();
            markerFile = StaleFileManager.Util.markerFile(archive);
            staleEntryNames = readStaleEntryNames(markerFile);
        }

        /**
         * Reads entry names of stale files from the marker file, if it exists.
         * @param markerFile the marker file to be read
         * @return Collection of stale entry names.
         * @throws FileNotFoundException if the marker file existed initially but vanished before it could be opened
         * @throws IOException in case of errors reading the marker file
         */
        private static Collection<String> readStaleEntryNames(final File markerFile) throws FileNotFoundException, IOException {
            final Collection<String> result = new ArrayList<String>();
            if ( ! markerFile.exists()) {
                return result;
            }
            LineNumberReader reader = null;
            try {
                reader = new LineNumberReader(new FileReader(markerFile));

                // Avoid some work if logging is coarser than FINE.
                final boolean isShowEntriesToBeSkipped = deplLogger.isLoggable(DEBUG_LEVEL);
                final StringBuffer entriesToSkip = isShowEntriesToBeSkipped ? new StringBuffer() : null;
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(line);
                    if (isShowEntriesToBeSkipped) {
                        entriesToSkip.append(line).append(LINE_SEP);
                    }
                }
                if (isShowEntriesToBeSkipped) {
                    deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager will skip following file(s): {0}{1}",
                            new Object[] {LINE_SEP, entriesToSkip.toString()});
                }
                return result;
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }

        @Override
        public boolean isEntryValid(final File f, final boolean isLogging) {
            return isEntryValid(f, isLogging, deplLogger);
        }


        @Override
        public boolean isEntryValid(final File f, final boolean isLogging, final Logger logger) {
            final boolean result = ( ! isEntryMarkerFile(f)) && 
                    ( ! staleEntryNames.contains(archiveURI.relativize(f.toURI()).getPath()));
            if ( ! result && ! isEntryMarkerFile(f) && isLogging) {
                deplLogger.log(Level.WARNING,
                               STALE_FILES_SKIPPED,
                               new Object[] {archiveURI.relativize(f.toURI()).toASCIIString(), archiveFile.getAbsolutePath()});
            }
            return result;
        }

        @Override
        public boolean isEntryMarkerFile(File f) {
            return markerFile.equals(f);
        }

        /**
         * Records that the specified file is valid.
         * <p>
         * If the file had previously been marked as stale, it will no longer be
         * considered stale.
         * @param f the File which is now valid
         */
        @Override
        public void recordValidEntry(File f) {
            if (updateStaleEntry(f, "FileArchive.StaleFileManager marking formerly stale entry {0} as active")) {
                /*
                 * Process not only the file itself but the directories from the
                 * file to the owning archive, since the directories are now
                 * implicitly valid as well.
                 */
                do {
                    f = f.getParentFile();
                    updateStaleEntry(f, "FileArchive.StaleFileManager marking formerly stale ancestor {0} as active");
                } while ( ! f.equals(archiveFile));
                flush();
            }
        }

        @Override
        public void recordDeletedEntry(File f) {
            if (updateStaleEntry(f, "FileArchive.StaleFileManager recording deletion of entry {0}")) {
                /*
                 * If there are no other stale files in the same directory as
                 * the file just deleted, then remove the directory from
                 * the stale files collection and check its ancestors.
                 */
                do {
                    if (isStaleEntryInDir(f.getParentFile())) {
                        return;
                    }
                    updateStaleEntry(f, "FileArchive.StaleFileManager recording that formerly stale directory {0} is no longer stale");
                    f = f.getParentFile();
                } while ( ! f.equals(archiveFile));

                flush();
            }
        }

        private boolean isStaleEntryInDir(final File dir) {
            final String dirURIPath = archiveURI.relativize(dir.toURI()).getPath();
            for (String staleEntryName : staleEntryNames) {
                if (staleEntryName.startsWith(dirURIPath) && ! staleEntryName.equals(dirURIPath)) {
                    deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager.isStaleEntryInDir found remaining stale entry {0} in {1}",
                            new Object[] {staleEntryName, dir.getAbsolutePath()});
                    return true;
                }
            }
            return false;
        }

        private boolean updateStaleEntry(File f, final String msg) {
            if (staleEntryNames.isEmpty()) {
                deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager.updateStaleEntry finds staleEntryNames is empty; skipping");
                return false;
            }

            final String entryName = archiveURI.relativize(f.toURI()).toASCIIString();
            final boolean wasStale = staleEntryNames.remove(entryName);
            if (wasStale) {
                deplLogger.log(DEBUG_LEVEL, msg,
                        entryName);
            } else {
                deplLogger.log(DEBUG_LEVEL, "updateStaleEntry did not find {0} in the stale entries {1}",
                        new Object[] {entryName, staleEntryNames.toString()});
            }
            return wasStale;
        }

        @Override
        public void flush() {
            if (staleEntryNames.isEmpty()) {
                deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager.flush deleting marker file; no more stale entries");
                final File marker = Util.markerFile(archiveFile);
                if ( ! marker.exists() || marker.delete()) {
                    return;
                }
                /*
                 * Couldn't delete the marker file, so try to write out an empty one
                 * so its old contents will not confuse the stale file manager.
                 */
                deplLogger.log(Level.FINE, "FileArchive.StatleFileManager.flush could not delete marker file {0}; continuing by writing out an empty marker file", 
                        marker.getAbsolutePath());
            }
            PrintStream ps = null;
            try {
                ps = new PrintStream(Util.markerFile(archiveFile));
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            for (String staleEntryName : staleEntryNames) {
                ps.println(staleEntryName);
            }
            ps.close();
            deplLogger.log(DEBUG_LEVEL, "FileArchive.StaleFileManager.flush rewrote on-disk file {0} containing {1}",
                    new Object[] {markerFile.getAbsolutePath(), staleEntryNames.toString()});
        }


    }
}
