/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

/* Byron Nevins, April 2000
 * ZipFile -- A utility class for exploding archive (zip) files.
 */
package com.sun.enterprise.util.zip;

import com.sun.enterprise.util.CULoggerInfo;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

///////////////////////////////////////////////////////////////////////////////
public class ZipFile {

    public ZipFile(String zipFilename, String explodeDirName) throws ZipFileException {
        this(new File(zipFilename), new File(explodeDirName));
    }

    ///////////////////////////////////////////////////////////////////////////
    public ZipFile(InputStream inStream, String anExplodeDirName) throws ZipFileException {
        this(new BufferedInputStream(inStream, BUFFER_SIZE), new File(anExplodeDirName));
    }

    ///////////////////////////////////////////////////////////////////////////
    public ZipFile(File zipFile, File anExplodeDir) throws ZipFileException {
        checkZipFile(zipFile);
        BufferedInputStream bis = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE);
            ctor(bis, anExplodeDir);
            this.zipFile = zipFile;
        } catch (Throwable e) {
            if (bis != null) {
                try {
                    bis.close();
                } catch (Throwable thr) {
                    throw new ZipFileException(thr);
                }
            }
            throw new ZipFileException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public ZipFile(InputStream inStream, File anExplodeDir) throws ZipFileException {
        ctor(inStream, anExplodeDir);
    }

    /**
     * Explodes files as usual, and then explodes every jar file found. All
     * explosions are copied relative the same root directory.<p>It does a
     * case-sensitive check for files that end with ".jar" will comment out for
     * later possbile use public ArrayList explodeAll() throws ZipFileException
     * { return doExplode(this); }
     */
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ArrayList<String> explode() throws ZipFileException {
        files = new ArrayList<String>();
        ZipInputStream zin = null;
        BufferedOutputStream bos = null;

        try {
            zin = zipStream; // new ZipInputStream(new FileInputStream(zipFile));
            ZipEntry ze;

            while ((ze = zin.getNextEntry()) != null) {
                String filename = ze.getName();

                /*
                 if(isManifest(filename))
                 {
                 continue;	// don't bother with manifest file...
                 }
                 */

                File fullpath;

                if (isDirectory(filename)) {
                    // just a directory -- make it and move on...
                    fullpath = new File(explodeDir, filename.substring(0, filename.length() - 1));
                    if (!fullpath.mkdirs()) {
                        throw new IOException("Cannot create directory: " + fullpath);
                    }
                    continue;
                }

                fullpath = new File(explodeDir, filename);

                File newDir = fullpath.getParentFile();

                if (!newDir.mkdirs()) {
                    _utillogger.log(Level.FINE, "Cannot create directory {0}", newDir);
                }

                if (fullpath.delete()) {	// wipe-out pre-existing files
                                    /*
                     * Report that a file is being overwritten.
                     */
                    if (_utillogger.isLoggable(Level.FINE)) {
                        _utillogger.log(Level.FINE, "File {0} is being overwritten during expansion of {1}", new Object[]{fullpath.getAbsolutePath(), zipFile != null ? ("file " + zipFile.getAbsolutePath()) : "stream"});
                    }
                }

                File f = new File(explodeDir, filename);

                if (f.isDirectory()) {
                    continue; // e.g. if we asked to write to a directory instead of a file...
                }

                bos = new BufferedOutputStream(getOutputStream(f), BUFFER_SIZE);

                int totalBytes = 0;

                for (int numBytes = zin.read(buffer); numBytes > 0; numBytes = zin.read(buffer)) {
                    bos.write(buffer, 0, numBytes);
                    totalBytes += numBytes;
                }
                bos.close();
                bos = null;
                files.add(filename);
            }
        } catch (IOException e) {
            throw new ZipFileException(e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                }
            }
            try {
                if (zin != null) {
                    zin.close();
                }
            } catch (IOException e) {
                throw new ZipFileException("Got an exception while trying to close Jar input stream: " + e);//NOI18N
            }
        }
        return files;
    }

    /**
     * Extracts the named jar file from the ear.
     *
     * @param jarEntryName name of the jar file
     * @param earFile application archive
     * @param jarFile locaton of the jar file where the jar entry will be
     * extracted
     *
     * @return the named jar file from the ear
     *
     * @exception ZipFileException if an error while extracting the jar
     */
    public static void extractJar(String jarEntryName, JarFile earFile,
            File jarFile) throws ZipFileException {

        try {
            File parent = jarFile.getParentFile();
            if (!parent.exists()) {
                if (!parent.mkdirs()) {
                    throw new ZipFileException("Cannot create directory: " + parent);
                }
            }

            ZipEntry jarEntry = earFile.getEntry(jarEntryName);
            if (jarEntry == null) {
                throw new ZipFileException(jarEntryName
                        + " not found in " + earFile.getName());
            }

            InputStream is = earFile.getInputStream(jarEntry);
            FileOutputStream fos = new FileOutputStream(jarFile);
            // the FileUtils.copy will buffer the streams, 
            // so we won't buffer them here.
            FileUtils.copy(is, fos, jarEntry.getSize());
        } catch (IOException e) {
            throw new ZipFileException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    public ArrayList getFileList() {
        return files;
    }

    /**
     * *********************************************************************
     * /******************************** Private ******************************
	/**********************************************************************
     */
    /* Apparently this method is never called. Don't know the history
     * about this method so commenting it out as opposed to removing it
     * for now.
     private static ArrayList<String> doExplode(ZipFile zf) throws ZipFileException
     {
     ArrayList<String> finalList = new ArrayList<String>(50);
     ArrayList<ZipFile> zipFileList = new ArrayList<ZipFile>();
     ArrayList tmpList = null;
     ZipFile tmpZf = null;
     Iterator itr = null;
     String fileName = null;

     zipFileList.add(zf);
     while (zipFileList.size() > 0)
     {
     // get "last" jar to explode
     tmpZf = zipFileList.remove(zipFileList.size() - 1);
     tmpList = tmpZf.explode();

     // traverse list of files
     itr = tmpList.iterator();
     while (itr.hasNext())
     {
     fileName = (String)itr.next();
     if ( ! fileName.endsWith(".jar") )
     {
     // add non-jar file to finalList
     finalList.add(fileName);
     }
     else
     {
     // create ZipFile and add to zipFileList
     File f = new File(tmpZf.explodeDir, fileName);
     ZipFile newZf = new ZipFile(f, tmpZf.explodeDir);
     zipFileList.add(newZf);
     }

     if (tmpZf != zf)  // don't remove first ZipFile
     {
     tmpZf.explodeDir.delete();
     }
     }
     }
     return finalList;
     }
     */
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void ctor(InputStream inStream, File anExplodeDir) throws ZipFileException {
        insist(anExplodeDir != null);
        explodeDir = anExplodeDir;

        try {
            zipStream = new ZipInputStream(inStream);
            checkExplodeDir();
        } catch (Throwable t) {
            if (zipStream != null) {
                try {
                    zipStream.close();
                } catch (Throwable thr) {
                }
            }
            throw new ZipFileException(t.toString());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    private boolean isDirectory(String s) {
        char c = s.charAt(s.length() - 1);

        return c == '/' || c == '\\';
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    private void checkZipFile(File zipFile) throws ZipFileException {
        insist(zipFile != null);

        String zipFileName = zipFile.getPath();

        insist(zipFile.exists(), "zipFile (" + zipFileName + ") doesn't exist");//NOI18N
        insist(!zipFile.isDirectory(), "zipFile (" + zipFileName + ") is actually a directory!");//NOI18N
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    private void checkExplodeDir() throws ZipFileException {
        String explodeDirName = explodeDir.getPath();

        // just in case...
        insist(explodeDir.mkdirs(), "Unable to create target directory: " + explodeDirName);

        insist(explodeDir.exists(), "Target Directory doesn't exist: " + explodeDirName);//NOI18N
        insist(explodeDir.isDirectory(), "Target Directory isn't a directory: " + explodeDirName);//NOI18N
        insist(explodeDir.canWrite(), "Can't write to Target Directory: " + explodeDirName);//NOI18N
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    private FileOutputStream getOutputStream(File f) throws ZipFileException {
        try {
            return new FileOutputStream(f);
        } catch (FileNotFoundException e) {
            throw new ZipFileException("filename: " + f.getPath() + "  " + e);
        } catch (IOException e) {
            throw new ZipFileException(e);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    ////////////                                                   //////////////////////////
    ////////////    Internal Error-Checking Stuff                  //////////////////////////
    ////////////                                                   //////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    private static void insist(boolean b) throws ZipFileException {
        if (!b) {
            throw new ZipFileException();
        } 
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    private static void insist(boolean b, String mesg) throws ZipFileException {
        if (!b) {
            throw new ZipFileException(mesg);
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////
    private static final int BUFFER_SIZE = 0x10000; //64k
    private File explodeDir = null;
    private ArrayList<String> files = null;
    private static final String specialDir = "META-INF/";//NOI18N
    private byte[] buffer = new byte[BUFFER_SIZE];
    private ZipInputStream zipStream = null;
    private static final Logger _utillogger = CULoggerInfo.getLogger();
    private File zipFile = null;
}

////////////////  
