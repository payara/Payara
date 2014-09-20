/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.io;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.OS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


public class FileUtils {
    final static Logger _utillogger = CULoggerInfo.getLogger();
    private final static LocalStringsImpl messages = new LocalStringsImpl(FileUtils.class);

    /**
     * The method, java.io.File.getParentFile() does not necessarily do what
     * you would think it does.  What it really does is to simply chop off the
     * final element in the path and return what is left-over.  E.g.
     * if the file is /foo/.  then the "parent" that is returned is /foo
     * which is probably not what you expected.
     * This method really returns the parent directory - or null if there is none.
     * @param f
     * @return
     */
    public static File getParentFile(File f) {
        if (f == null)
            return null;

        return SmartFile.sanitize(f).getParentFile();
    }

    /**
     * Wrapper for File.mkdirs
     * This version will return true if the directory exists when the method returns.
     * Unlike File.mkdirs which returns false if the directory already exists.
     * @param f The file pointing to the directory to be created
     * @return
     */
    public static boolean mkdirsMaybe(File f) {
        return f != null && (f.isDirectory() || f.mkdirs());
    }

    /**
     * Wrapper for File.delete
     * This version will return true if the file does not exist when the method returns.
     * Unlike File.delete which returns false if the file does not exist.
     * @param f The file to be deleted
     * @return
     */
    public static boolean deleteFileMaybe(File f) {
        return f != null && (!f.exists() || f.delete());
    }

    /*
    * Wrapper for File.listFiles
    * Guaranteed to return an array in all cases.
    * File.listFiles() returns either null or an empty array.  This is annoying and results in harder
    * than neccessry to read code -- i.e. there are 3 results possible:
    * an array with files in it
    * an empty array
    * a null
    */
    public static File[] listFiles(File f) {
        try {
            File[] files = f.listFiles();

            if(files != null)
            return files;
        }
        catch(Exception e) {
            // fall through
        }

        return new File[0];
    }

    public static File[] listFiles(File f, FileFilter ff) {
        try {
            File[] files = f.listFiles(ff);

            if(files != null)
            return files;
        }
        catch(Exception e) {
            // fall through
        }

        return new File[0];
    }

    public static File[] listFiles(File f, FilenameFilter fnf) {
        try {
            File[] files = f.listFiles(fnf);

            if(files != null)
            return files;
        }
        catch(Exception e) {
            // fall through
        }

        return new File[0];
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean safeIsDirectory(File f) {
        if (f == null || !f.exists() || !f.isDirectory())
            return false;

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean safeIsRealDirectory(String s) {
        return safeIsRealDirectory(new File(s));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean safeIsRealDirectory(File f) {
        if (safeIsDirectory(f) == false)
            return false;

        // these 2 values while be different for symbolic links
        String canonical = safeGetCanonicalPath(f);
        String absolute = f.getAbsolutePath();

        if (canonical.equals(absolute))
            return true;

        /* Bug 4715043 -- WHOA -- Bug Obscura!!
           * In Windows, if you create the File object with, say, "d:/foo", then the
           * absolute path will be "d:\foo" and the canonical path will be "D:\foo"
           * and they won't match!!!
           **/
        if (OS.isWindows() && canonical.equalsIgnoreCase(absolute))
            return true;

        return false;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean safeIsDirectory(String s) {
        return safeIsDirectory(new File(s));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String safeGetCanonicalPath(File f) {
        if (f == null)
            return null;

        try {
            return f.getCanonicalPath();
        }
        catch (IOException e) {
            return f.getAbsolutePath();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static File safeGetCanonicalFile(File f) {
        if (f == null)
            return null;

        try {
            return f.getCanonicalFile();
        }
        catch (IOException e) {
            return f.getAbsoluteFile();
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean hasExtension(String filename, String ext) {
        if (filename == null || filename.length() <= 0)
            return false;

        return filename.endsWith(ext);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean hasExtension(File f, String ext) {
        if (f == null || !f.exists())
            return false;

        return f.getName().endsWith(ext);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean hasExtensionIgnoreCase(String filename, String ext) {
        if (filename == null || filename.length() <= 0)
            return false;

        return filename.toLowerCase(Locale.ENGLISH).endsWith(ext.toLowerCase(Locale.ENGLISH));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean hasExtensionIgnoreCase(File f, String ext) {
        if (f == null || !f.exists())
            return false;

        return f.getName().toLowerCase(Locale.ENGLISH).endsWith(ext.toLowerCase(Locale.ENGLISH));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean isLegalFilename(String filename) {
        if (!isValidString(filename))
            return false;

        for (int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++)
            if (filename.indexOf(ILLEGAL_FILENAME_CHARS[i]) >= 0)
                return false;

        return true;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean isFriendlyFilename(String filename) {
        if (!isValidString(filename))
            return false;

        if (filename.indexOf(BLANK) >= 0 || filename.indexOf(DOT) >= 0)
            return false;

        return isLegalFilename(filename);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String makeLegalFilename(String filename) {
        if (isLegalFilename(filename))
            return filename;

        // let's use "__" to replace "/" and "\" (on Windows) so less chance
        // to collide with the actual name when reverting
        filename = filename.replaceAll("[/" + Pattern.quote("\\") + "]", "__");

        for (int i = 0; i < ILLEGAL_FILENAME_CHARS.length; i++)
            filename = filename.replace(ILLEGAL_FILENAME_CHARS[i], REPLACEMENT_CHAR);

        return filename;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String makeLegalNoBlankFileName(String filename)
    {
        return  makeLegalFilename(filename).replace(
            BLANK, REPLACEMENT_CHAR);
    }


    ///////////////////////////////////////////////////////////////////////////
    public static String makeFriendlyFilename(String filename) {
        if (isFriendlyFilename(filename))
            return filename;

        String ret = makeLegalFilename(filename).replace(BLANK, REPLACEMENT_CHAR);
        ret = ret.replace(DOT, REPLACEMENT_CHAR);
        return ret;
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String makeFriendlyFilenameNoExtension(String filename) {
        int index = filename.lastIndexOf('.');

        if (index > 0)
            filename = filename.substring(0, index);

        return (makeFriendlyFilename(filename));
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String makeFriendlyFilenameExtension(String filename) {
        if (filename == null) {
            return null;
        }

        filename = makeLegalNoBlankFileName(filename);

        String extension = "";
        if (filename.endsWith(".ear")) {
            filename = filename.substring(0, filename.indexOf(".ear"));
            extension = "_ear";
        } else if (filename.endsWith(".war")) {
            filename = filename.substring(0, filename.indexOf(".war"));
            extension = "_war";
        } else if (filename.endsWith(".jar")) {
            filename = filename.substring(0, filename.indexOf(".jar"));
            extension = "_jar";
        } else if (filename.endsWith(".rar")) {
            filename = filename.substring(0, filename.indexOf(".rar"));
            extension = "_rar";
        }
        return filename + extension;
    }

    public static String revertFriendlyFilenameExtension(String filename) {
        if (filename == null ||
                !(filename.endsWith("_ear") || filename.endsWith("_war") ||
                        filename.endsWith("_jar") || filename.endsWith("_rar"))) {
            return filename;
        }

        String extension = "";
        if (filename.endsWith("_ear")) {
            filename = filename.substring(0, filename.indexOf("_ear"));
            extension = ".ear";
        } else if (filename.endsWith("_war")) {
            filename = filename.substring(0, filename.indexOf("_war"));
            extension = ".war";
        } else if (filename.endsWith("_jar")) {
            filename = filename.substring(0, filename.indexOf("_jar"));
            extension = ".jar";
        } else if (filename.endsWith("_rar")) {
            filename = filename.substring(0, filename.indexOf("_rar"));
            extension = ".rar";
        }
        return filename + extension;
    }

    public static String revertFriendlyFilename(String filename) {

        //first, revert the file extension
        String name = revertFriendlyFilenameExtension(filename);

        //then, revert the rest of the string
        return name.replaceAll("__", "/");
    }

    /////////////////////////////////////////////////////////

    public static void liquidate(File parent) {
        whack(parent);
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean isJar(String filename)
    {
        return hasExtension(filename, ".jar");
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean isZip(String filename)
    {
        return hasExtensionIgnoreCase(filename, ".zip");
    }

    ///////////////////////////////////////////////////////////////////////////

    public static boolean isJar(File f)
    {
        return hasExtension(f, ".jar");
    }

    ///////////////////////////////////////////////////////////////////////////
    public static boolean isZip(File f)
    {
        return hasExtensionIgnoreCase(f, ".zip");
    }

    /**
     * Deletes a directory and its contents.
     * <p/>
     * If this method encounters a symbolic link in the subtree below "parent"
     * then it deletes the link but not any of the files pointed to by the link.
     * Note that whack will delete files if a symbolic link appears in the
     * path above the specified parent directory in the path.
     *
     * @param parent the File at the top of the subtree to delete
     * @return success or failure of deleting the directory
     */
    public static boolean whack(File parent) {
        return whack(parent, null);
    }

    /**
     * Deletes a directory and its contents.
     * <p/>
     * If this method encounters a symbolic link in the subtree below "parent"
     * then it deletes the link but not any of the files pointed to by the link.
     * Note that whack will delete files if a symbolic link appears in the
     * path above the specified parent directory in the path.
     *
     * @param parent the File at the top of the subtree to delete
     * @return success or failure of deleting the directory
     */
    public static boolean whack(File parent, Collection<File> undeletedFiles) {
        try {
            /*
            *Resolve any links up-stream from this parent directory and
            *then whack the resulting resolved directory.
            */
            return whackResolvedDirectory(parent.getCanonicalFile(), undeletedFiles);
        } catch (IOException ioe) {
            _utillogger.log(Level.SEVERE, CULoggerInfo.exceptionIO, ioe);
            return false;
        }
    }

    /**
     * Deletes a directory and its contents.
     * <p/>
     * The whackResolvedDirectory method is invoked with a File argument
     * in which any upstream file system links have already been resolved.
     * This method will treate Any file passed in that does not have the same
     * absolute and canonical path - as evaluated in safeIsRealDirectory -
     * as a link and will delete the link without deleting any files in the
     * linked directory.
     *
     * @param parent the File at the top of the subtree to delete
     * @return success or failure of deleting the directory
     */
    private static boolean whackResolvedDirectory(File parent, Collection<File> undeletedFiles) {
        /*
        *Do not recursively delete the contents if the current parent
        *is a symbolic link.
        */
        if (safeIsRealDirectory(parent)) {
            File[] kids = listFiles(parent);

            for (int i = 0; i < kids.length; i++) {
                File f = kids[i];

                if (f.isDirectory())
                    whackResolvedDirectory(f, undeletedFiles);
                else if (!deleteFile(f) && undeletedFiles != null) {
                    undeletedFiles.add(f);
                }

            }
        }

        /*
        *Delete the directory or symbolic link.
        */
        return deleteFile(parent);
    }

    /**
     * Delete a file.  If impossible to delete then try to delete it when the JVM exits.
     * E.g. when Windows is using a jar in the current JVM -- you can not delete the jar until
     * the JVM dies.
     * @param f file to delete
     */
    public static void deleteFileNowOrLater(File f) {
        if(!deleteFile(f))
            f.deleteOnExit();
    }

    /**
     * Delete a file.  Will retry every ten milliseconds for five seconds, doing a
     * gc after each second.
     *
     * @param f file to delete
     * @return boolean indicating success or failure of the deletion atttempt; returns true if file is absent
     */
    public static boolean deleteFileWithWaitLoop(File f) {
        return internalDeleteFile(f, true);

    }

    /**
     * Delete a file.  If on Windows and the delete fails, run the gc and retry the deletion.
     *
     * @param f file to delete
     * @return boolean indicating success or failure of the deletion atttempt; returns true if file is absent
     */
    public static boolean deleteFile(File f) {
        return internalDeleteFile(f, false);
    }

    /**
     * Delete a file.  If on Windows and the delete fails, run the gc and retry the deletion.
     *
     * @param f file to delete
     * @return boolean indicating success or failure of the deletion atttempt; returns true if file is absent
     */
    private static boolean internalDeleteFile(File f, boolean doWaitLoop) {
        /*
        *The operation succeeds immediately if the file is deleted
        *successfully.  On systems that support symbolic links, the file
        *will be reported as non-existent if the file is a sym link to a
        *non-existent directory.  In that case invoke delete to remove
        *the link before checking for existence, since File.exists on
        *a symlink checks for the existence of the linked-to directory or
        *file rather than of the link itself.
        */
        if (!doWaitLoop) {
            if (f.delete()) {
                return true;
            }
        }
        else {
            DeleteFileWork work = new DeleteFileWork(f);

            doWithRetry(work);

            if (work.workComplete()) {
                return true;
            }
        }

        boolean log = _utillogger.isLoggable(FILE_OPERATION_LOG_LEVEL);
        String filePath = f.getAbsolutePath();

        /*
        *The deletion failed.  This could be simply because the file
        *does not exist.  In that case, log an appropriate message and
        *return.
        */
        if (!f.exists()) {
            if (log) {
                _utillogger.log(Level.FINE, CULoggerInfo.deleteFailedAbsent, filePath);
            }
            return true;
        } else {
            /*
            *The delete failed and the file exists.  Log a message if that
            *level is enabled and return false to indicate the failure.
            */
            if (log) {
                _utillogger.log(FILE_OPERATION_LOG_LEVEL, CULoggerInfo.deleteFailed, filePath);
            }
            return false;
        }
    }

    /**
     * Opens a stream to the specified output file, retrying if necessary.
     *
     * @param out the output File for which a stream is needed
     * @return the FileOutputStream
     * @throws IOException for any errors opening the stream
     */
    public static FileOutputStream openFileOutputStream(File out) throws IOException {
        FileOutputStreamWork work = new FileOutputStreamWork(out);
        int retries = doWithRetry(work);
        if (retries > 0) {
            _utillogger.log(Level.FINE, "Retrying " + retries + " times");
        }
        if (work.workComplete()) {
            return work.getStream();
        } else {
            IOException ioe = new IOException();
            ioe.initCause(work.getLastError());
            throw ioe;
        }
    }

    /**
    * Return a set of all the files (File objects) under the directory specified, with
    * relative pathnames filtered with the filename filter (can be null for all files).
    */
    public static Set<File> getAllFilesUnder(File directory, FilenameFilter filenameFilter) throws IOException {
	if (!directory.exists() || !directory.isDirectory()) {
	    throw new IOException("Problem with: " + directory + ". You must supply a directory that exists");
	}
        return getAllFilesUnder(directory, filenameFilter, true);
    }

    public static Set<File> getAllFilesUnder(File directory, FilenameFilter filenameFilter, boolean relativize) throws IOException {
        Set<File> allFiles = new TreeSet<File>();
        File relativizingDir = relativize ? directory : null;
        recursiveGetFilesUnder( relativizingDir, directory, filenameFilter,
                                allFiles, false );
        return allFiles;
    }

    public static Set<File> getAllFilesAndDirectoriesUnder(File directory) throws IOException {
	if (!directory.exists() || !directory.isDirectory()) {
	    throw new IOException("Problem with: " + directory + ". You must supply a directory that exists");
	}
	Set<File> allFiles = new TreeSet<File>();
	recursiveGetFilesUnder(directory, directory, null, allFiles, true);
	return allFiles;
    }

    // relativizingRoot can be null, in which case no relativizing is
    // performed.
    private static void recursiveGetFilesUnder(File relativizingRoot, File directory, FilenameFilter filenameFilter, Set<File> set, boolean returnDirectories) {
	File[] files = listFiles(directory, filenameFilter);
	for (int i = 0; i < files.length; i++) {
	    if (files[i].isDirectory()) {
		recursiveGetFilesUnder(relativizingRoot, files[i], filenameFilter, set, returnDirectories);
		if (returnDirectories) {
                    if( relativizingRoot != null ) {
                        set.add(relativize(relativizingRoot, files[i]));
                    } else {
                        set.add(files[i]);
                    }
		}
	    } else {
                if( relativizingRoot != null ) {
                    set.add(relativize(relativizingRoot, files[i]));
                } else {
                    set.add(files[i]);
                }
	    }
    	}
    }

    /**
     * Given a directory and a fully-qualified file somewhere
     * under that directory, return the portion of the child
     * that is relative to the parent.
     */
    public static File relativize(File parent, File child) {
	String baseDir         = parent.getAbsolutePath();
	String baseDirAndChild = child.getAbsolutePath();

        String relative = baseDirAndChild.substring(baseDir.length(),
                                                    baseDirAndChild.length());

        // Strip off any extraneous file separator character.
        if( relative.startsWith(File.separator) ) {
            relative = relative.substring(1);
        }

	return new File(relative);
    }


    /**
     * Executes the supplied work object until the work is done or the max.
     * retry count is reached.
     *
     * @param work the RetriableWork implementation to be run
     * @return the number of retries performed; 0 indicates the work succeeded without having to retry
     */
    private static int doWithRetry(RetriableWork work) {
        int retries = 0;

        /*
        *Try the work the first time.  Ideally this will work.
        */
        work.run();

        /*
        *If the work failed and this is Windows - on which running gc may
        *unlock the locked file - then begin the retries.
        */
        if (!work.workComplete() && OS.isWindows()) {
            _utillogger.log(FILE_OPERATION_LOG_LEVEL, CULoggerInfo.performGC);
            while (!work.workComplete() && retries++ < FILE_OPERATION_MAX_RETRIES) {
                try {
                    Thread.sleep(FILE_OPERATION_SLEEP_DELAY_MS);
                } catch (InterruptedException ex) {
                }
                System.gc();
                work.run();
            }
        }
        return retries;
    }

    /**
     * Creates a String listing the absolute paths of files, separated by
     * the platform's line separator.
     *
     * @param files the Collection of File objects to be listed
     * @return String containing the absolute paths of the files with the line separator between them
     */
    public static String formatFileCollection(Collection<File> files) {
        StringBuilder sb = new StringBuilder();
        String lineSep = System.getProperty("line.separator");
        String prefix = "";
        for (File f : files) {
            sb.append(prefix).append(f.getAbsolutePath());
            prefix = lineSep;
        }
        return sb.toString();
    }
    ///////////////////////////////////////////////////////////////////////////

    public static File getDirectory(File f) {
        String filename = f.getAbsolutePath();
        return new File((new File(filename)).getParent());
    }

    ///////////////////////////////////////////////////////////////////////////

    public static File createTempFile(File directory) {
        File f = null;

        try {
            f = File.createTempFile(TMPFILENAME, "jar", directory);
        }
        catch (IOException ioe) {
            _utillogger.log(Level.SEVERE, CULoggerInfo.exceptionIO, ioe);
        }

        f.deleteOnExit(); // just in case
        return f;
    }

    /**
     * Returns an array of abstract pathnames that matches with the given
     * file extension. If the given abstract pathname does not denote a
     * directory, then this method returns null. If there is no matching
     * file under the given directory and its sub directories,
     * it returns null;
     *
     * @param dirName dir name under which search will begin
     * @param ext     file extension to look for
     * @return an array of abstract pathnames that matches with the extension
     */
    public static File[] listAllFiles(File dirName, String ext) {
        File[] target = null;
        List<File> list = searchDir(dirName, ext);

        if ((list != null) && (list.size() > 0)) {
            target = new File[list.size()];
            target = (File[]) list.toArray(target);
        }

        return target;
    }

    /**
     * Returns a list of abstract pathnames that matches with the given
     * file extension. If the given abstract pathname does not denote a
     * directory, then this method returns null. If there is no matching
     * file under the given directory and its sub directories, it returns
     * an empty list.
     *
     * @param dirName dir name under which search will begin
     * @param ext     file extension to look for
     * @return a list of abstract pathnames of type java.io.File
     *         that matches with the given extension
     */
    public static List<File> searchDir(File dirName, String ext) {
        List<File> targetList = null;

        if (dirName.isDirectory()) {
            targetList = new ArrayList<File>();

            File[] list = listFiles(dirName);

            for (int i = 0; i < list.length; i++) {
                if (list[i].isDirectory()) {
                    targetList.addAll(searchDir(list[i], ext));
                } else {
                    String name = list[i].toString();
                    if (hasExtension(name, ext)) {
                        targetList.add(list[i]);
                    }
                }
            }
        }

        return targetList;
    }

    /**
     * Copies a file.
     *
     * @param from Name of file to copy
     * @param to   Name of new file
     * @throws IOException if an error while copying the content
     */
    public static void copy(String from, String to) throws IOException {
        //if(!StringUtils.ok(from) || !StringUtils.ok(to))
        if (from == null || to == null)
            throw new IllegalArgumentException("null or empty filename argument");

        File fin = new File(from);
        File fout = new File(to);

        copy(fin, fout);
    }

    /**
     * Copies a file.
     *
     * @param fin  File to copy
     * @param fout New file
     * @throws IOException if an error while copying the content
     */
    public static void copy(File fin, File fout) throws IOException {
        if (safeIsDirectory(fin)) {
            copyTree(fin, fout);
            return;
        }

        if (!fin.exists())
            throw new IllegalArgumentException("File source doesn't exist");

            if(!mkdirsMaybe(fout.getParentFile()))
                throw new RuntimeException("Can't create parent dir of output file: " + fout);

        copyFile(fin, fout);
    }

    /**
     * Copies the entire tree to a new location.
     *
     * @param din  File pointing at root of tree to copy
     * @param dout File pointing at root of new tree
     * @throws IOException if an error while copying the content
     */
    public static void copyTree(File din, File dout)
            throws IOException {
        if (!safeIsDirectory(din))
            throw new IllegalArgumentException("Source isn't a directory");

        if(!mkdirsMaybe(dout))
            throw new IllegalArgumentException("Can't create destination directory");

        FileListerRelative flr = new FileListerRelative(din);
        String[] files = flr.getFiles();

        for (int i = 0; i < files.length; i++) {
            File fin = new File(din, files[i]);
            File fout = new File(dout, files[i]);

            copy(fin, fout);
        }
    }


    /**
     * Returns a String with uniform slashes such that all the
     * occurances of '\\' are replaced with '/'.
     * In other words, the returned string will have all forward slashes.
     * Accepts non-null strings only.
     *
     * @param inputStr non null String
     * @return a String which <code> does not contain `\\` character </code>
     */
    public static String makeForwardSlashes(String inputStr) {
        if (inputStr == null)
            throw new IllegalArgumentException("null String FileUtils.makeForwardSlashes");
        return (inputStr.replace('\\', '/'));
    }

    /**
     * Given a string (typically a path), quote the string such that spaces
     * are protected from interpretation by a Unix or Windows command shell.
     * Note that this method does not handle quoting for all styles of special
     * characters. Just for the basic case of strings with spaces.
     *
     * @param s input string
     * @return a String which is quoted to protect spaces
     */
    public static String quoteString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("null string");
        }

        if (!s.contains("\'")) {
            return("\'" + s + "\'");
        } else if(!s.contains("\"")) {
            return("\"" + s + "\"");
        } else {
            // Contains a single quote and a double quote. Use backslash
            // On Unix. Double quotes on Windows. This method does not claim
            // to support this case well if at all
            if (OS.isWindows()) {
                return("\"" + s + "\"");
            } else {
                return(s.replaceAll("\040", "\134 "));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    public static String getIllegalFilenameCharacters() {
        return ILLEGAL_FILENAME_STRING;
    }

    ///////////////////////////////////////////////////////////////////////////

    static boolean isValidString(String s) {
        return ((s != null) && (s.length() != 0));
    }


    /**
     * This method is used to copy a given file to another file
     * using the buffer sixe specified
     *
     * @param fin  the source file
     * @param fout the destination file
     */
    public static void copyFile(File fin, File fout) throws IOException {

        InputStream inStream = new BufferedInputStream(new FileInputStream(fin));
        FileOutputStream fos = FileUtils.openFileOutputStream(fout);
        copy(inStream, fos, fin.length());
    }


    public static void copy(InputStream in, FileOutputStream out, long size) throws IOException {

        try {
            copyWithoutClose(in, out, size);
        } finally {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        }
    }

    public static void copyWithoutClose(InputStream in, FileOutputStream out, long size) throws IOException {

        ReadableByteChannel inChannel = Channels.newChannel(in);
        FileChannel outChannel = out.getChannel();
        outChannel.transferFrom(inChannel, 0, size);

    }

    public static void copy(InputStream in, OutputStream os, long size) throws IOException {
        if (os instanceof FileOutputStream) {
            copy(in, (FileOutputStream) os, size);
        } else {
            ReadableByteChannel inChannel = Channels.newChannel(in);
            WritableByteChannel outChannel = Channels.newChannel(os);
            if (size==0) {

                ByteBuffer byteBuffer = ByteBuffer.allocate(10240);
                int read;
                do {
                    read = inChannel.read(byteBuffer);
                    if (read>0) {
                        byteBuffer.limit(byteBuffer.position());
                        byteBuffer.rewind();
                        outChannel.write(byteBuffer);
                        byteBuffer.clear();
                    }
                } while (read!=-1);
            } else {
                ByteBuffer byteBuffer;
                try{
                    byteBuffer = ByteBuffer.allocate(Long.valueOf(size).intValue());
                }catch(Throwable err){
                    throw new IOException(messages.get("allocate.more.than.java.heap.space", err));
                }
                inChannel.read(byteBuffer);
                byteBuffer.rewind();
                outChannel.write(byteBuffer);
            }
        }
    }

        /**
         *Rename, running gc on Windows if needed to try to force open streams to close.
         *@param fromFile to be renamed
         *@param toFile name for the renamed file
         *@return boolean result of the rename attempt
         */
        public static boolean renameFile(File fromFile, File toFile) {
            boolean log = _utillogger.isLoggable(FILE_OPERATION_LOG_LEVEL) || _utillogger.isLoggable(Level.FINE);

            RenameFileWork renameWork = new RenameFileWork(fromFile, toFile);
            int retries = doWithRetry(renameWork);
            boolean result = renameWork.workComplete();

            String fromFilePath = null;
            String toFilePath = null;
            if (log || ! result) {
                fromFilePath = fromFile.getAbsolutePath();
                toFilePath = toFile.getAbsolutePath();
            }

            /*
             *If the rename worked, then write an appropriate log message if the
             *logging level allows.
             */
            if (result) {
                if (log) {
                    /*
                     *If the rename worked without retries, then log a FINE message.
                     *If retries were needed then use the configured
                     *FILE_OPERATION_LOG_LEVEL.
                     */
                    if (retries == 0) {
                        if (_utillogger.isLoggable(Level.FINE)) {
                            _utillogger.log(Level.FINE, CULoggerInfo.renameInitialSuccess,
                                    new Object [] {fromFilePath, toFilePath});
                        }
                    } else {
                        _utillogger.log(FILE_OPERATION_LOG_LEVEL, CULoggerInfo.retryRenameSuccess,
                                new Object [] {fromFilePath, toFilePath, Integer.valueOf(retries)});
                    }
                }
            } else {
                /*
                 *The rename has failed.  Write a warning message.
                 */
                _utillogger.log(Level.WARNING, CULoggerInfo.retryRenameFailure,
                        new Object [] {fromFilePath, toFilePath, Integer.valueOf(retries) });
            }
            return result;
        }

    /** Appends the given line at the end of given text file. If the given
     * file does not exist, an attempt is made to create it.
     * Note that this method can handle only text files.
     * @param fileName name of the text file that needs to be appended to
     * @param line the line to append to
     * @throws RuntimeException in case of any error - that makes it callable
     * from a code not within try-catch. Note that NPE will be thrown if either
     * argument is null.
     * Note that this method is not tested with String containing characters
     * with 2 bytes.
     */
    public static void appendText(String fileName, String line) throws
        RuntimeException    {
        RandomAccessFile file = null;
        try {
            final String MODE = "rw";
            file = new RandomAccessFile(fileName, MODE);
            file.seek(file.getFilePointer() + file.length());
            file.writeBytes(line);
        }
        catch(Exception e) {
            throw new RuntimeException("FileUtils.appendText()", e);
        }
        finally {
            try {
                if (file != null)
                    file.close();
            }
            catch(Exception e){}
        }
    }
    public static void appendText(String fileName, StringBuffer buffer)
    throws IOException, FileNotFoundException
    {
        appendText(fileName, buffer.toString());
    }
    ///////////////////////////////////////////////////////////////////////////

    /** A utility routine to read a <b> text file </b> efficiently and return
     * the contents as a String. Sometimes while reading log files of spawned
     * processes this kind of facility is handy. Instead of opening files, coding
     * FileReaders etc. this method could be employed. It is expected that the
     * file to be read is <code> small </code>.
     * @param fileName String representing absolute path of the file
     * @return String representing the contents of the file, empty String for an empty file
     * @throws java.io.IOException if there is an i/o error.
     * @throws java.io.FileNotFoundException if the file could not be found
     */
    public static String readSmallFile(final String fileName)
            throws IOException, FileNotFoundException {
        return (readSmallFile(new File(fileName)) );
    }

    public static String readSmallFile(final File file)
            throws IOException {
        final BufferedReader bf = new BufferedReader(new FileReader(file));
        final StringBuilder sb = new StringBuilder(); //preferred over StringBuffer, no need to synchronize
        String line = null;
        try {
            while ( (line = bf.readLine()) != null ) {
                sb.append(line);
                sb.append(System.getProperty("line.separator"));
            }
        }
        finally {
            try {
                bf.close();
            }
            catch (Exception e) {}
        }
        return ( sb.toString() );
    }

    /**
     * If the path dir/file does not exist, look for it in the classpath. If found
     * in classpath, create dir/file.
     *
     * @param file - path to look for
     * @param dir - directory where the path file should exist
     * @return the File representing dir/file. If that does not exist, return null.
     * @throws IOException
     */

    public static File getManagedFile(String file, File dir) throws IOException {
        File f = new File(dir, file);
        if (f.exists())
           return f;
        InputStream is = null, bis = null;
        OutputStream os = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(file);
            if (is == null)
                return null;
            bis = new BufferedInputStream(is);

            if(!mkdirsMaybe(f.getParentFile()))
                throw new RuntimeException("Can't create parent dir of output file: " + f);

            os = new BufferedOutputStream(FileUtils.openFileOutputStream(f));
            byte buf[] = new byte[10240];
            int len = 0;
            while ((len =bis.read(buf)) > 0) {
               os.write(buf, 0, len);
            }
            return f;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException ex) {}

            if (bis != null)
                try {
                    bis.close();
                } catch (IOException ex) {}

            if (is != null)
                try {
                    is.close();
                } catch (IOException ex) {}
        }
    }

    /**
     * Write the String to a file.  Then make the file readable and writable.
     * If the file already exists it will be truncated and the contents replaced
     * with the String argument.
     * @param s The String to write to the file
     * @param f The file to write the String to
     * @throws IOException if any errors
     */
    public static void writeStringToFile(String s, File f) throws IOException {
        Writer writer = null;

        try {
            writer = new PrintWriter(f);
            writer.write(s);
        }
        finally {
            if(writer != null) {
                try {
                    writer.close();
                }
                catch(Exception e) {
                    //ignore
                }
                f.setReadable(true);
                f.setWritable(true);
            }
        }
    }
/**
 * Find files matching the regular expression in the given directory
 * @param dir the directory to search
 * @param regexp the regular expression pattern
 * @return either an array of matching File objects or an empty array.  Guaranteed
 * to never return null
 */
    public static File[] findFilesInDir(File dir, final String regexp) {
        try {
            File[] matches = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.matches(regexp);
                }
            });
            if (matches != null)
                return matches;
        }
        catch (Exception e) {
            // fall through
        }
        return new File[0];
    }

    /**
     * Read in the given resourceName as a resource, and convert to a String
     *
     * @param resourceName
     * @return the contents of the resource as a String or null if absent
     */
    public static String resourceToString(String resourceName) {
        byte[] bytes = resourceToBytes(resourceName);

        return bytes == null ? null : new String(bytes);
    }

    /**
     * Read in the given resourceName as a resource, and convert to a byte array
     *
     * @param resourceName
     * @return the contents of the resource as a byte array or null if absent
     */
    public static byte[] resourceToBytes(String resourceName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream is = cl.getResourceAsStream(resourceName);

        if (is == null)
            return null;

        try {
            is = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;

            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            is.close();
            return baos.toByteArray();
        }
        catch (Exception e) {
                try {
                    is.close();
                }
                catch (Exception ex) {
                    // ignore...
                }
            return null;
        }
    }

    /**
     * Represents a unit of work that should be retried, if needed, until it
     * succeeds or the configured retry limit is reached.
     * <p/>
     * The <code>run</code> method required by the Runnable interface is invoked
     * to perform the work.
     */
    private interface RetriableWork extends Runnable {

        /**
         * Returns whether the work to be done by this instance of RetriableWork
         * has been completed successfully.
         * <p/>
         * This method may be invoked multiple times and so should not have
         * side effects.
         *
         * @return whether the work has been successfully completed
         */
        public boolean workComplete();
    }

    /**
     *Retriable work for renaming a file.
     */
    private static class RenameFileWork implements RetriableWork {

        private File originalFile;
        private File newFile;
        private boolean renameResult = false;

        public RenameFileWork(File originalFile, File newFile) {
            this.originalFile = originalFile;
            this.newFile = newFile;
        }

        public boolean workComplete() {
            return renameResult;
        }

        public void run() {
            renameResult = originalFile.renameTo(newFile);
        }
    }

    /**
     * Retriable work for opening a FileOutputStream.
     */
    private static class FileOutputStreamWork implements RetriableWork {

        private FileOutputStream fos = null;
        private Throwable lastError = null;
        private File out;

        public FileOutputStreamWork(File out) {
            this.out = out;
        }

        public boolean workComplete() {
            return fos != null;
        }

        public void run() {
            try {
                fos = new FileOutputStream(out);
                lastError = null;
            } catch (IOException ioe) {
                lastError = ioe;
            }
        }

        public FileOutputStream getStream() {
            return fos;
        }

        public Throwable getLastError() {
            return lastError;
        }
    }

    /**
     * Retriable work for deleting a file
     */
    private static class DeleteFileWork implements RetriableWork {

        private final File deleteMe;
        private boolean complete = false;

        private DeleteFileWork(File deleteMe) {
            this.deleteMe = deleteMe;
        }

        @Override
        public void run() {
            if (complete) return;

            if (deleteMe.delete()) complete = true;
        }

        @Override
        public boolean workComplete() {
            return complete;
        }


    }

    ///////////////////////////////////////////////////////////////////////////

    private final static char[] ILLEGAL_FILENAME_CHARS = {'/', '\\', ':', '*', '?', '"', '<', '>', '|'};
    private final static String ILLEGAL_FILENAME_STRING = "\\/:*?\"<>|";
    private final static char REPLACEMENT_CHAR = '_';
    private final static char BLANK = ' ';
    private final static char DOT = '.';
    private static String TMPFILENAME = "scratch";
    /*
    *The following property names are private, unsupported, and unpublished.
    */
    private static final int FILE_OPERATION_MAX_RETRIES = Integer.getInteger("com.sun.appserv.winFileLockRetryLimit", 5).intValue();
        private static final int FILE_OPERATION_SLEEP_DELAY_MS = Integer.getInteger("com.sun.appserv.winFileLockRetryDelay", 1000).intValue();
        private static final Level FILE_OPERATION_LOG_LEVEL = Level.FINE;
}
