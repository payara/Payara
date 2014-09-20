/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 * ZipFile -- A utility class for exploding jar files that contain EJB(s).  Used *only* in this package by the EJBImporter class
 */
package com.sun.enterprise.util.zip;

import com.sun.enterprise.util.io.FileListerRelative;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class ZipWriter {
    public ZipWriter(String zipFilename, String dirName) throws ZipFileException {
        init(zipFilename, dirName);
        createItemList(null);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public ZipWriter(String zipFilename, String dirName, ZipItem[] theItems) throws ZipFileException {
        items = theItems;
        init(zipFilename, dirName);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public ZipWriter(String zipFilename, String dirName, String[] fileList) throws ZipFileException {
        init(zipFilename, dirName);
        createItemList(fileList);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////
    public ZipWriter(OutputStream outStream, String dirName, String[] fileList) throws ZipFileException {
        init(outStream, dirName);
        createItemList(fileList);
    }

    /**
     * Exclude any files that are under these directories. E.g. suppose you have
     * C:/temp/x1, C:/temp/x2 and C:/temp/x3 and the root is set to c:temp. Then
     * to exclude the contents of the second 2 dirs you would send in a String
     * array with "x2" and "x3"
     *
     * @param dirs an array of top-level directory names
     */
    public void excludeDirs(String[] dirs) {
        if (dirs == null || dirs.length <= 0)
            return;

        // make sure the names all end with "/"
        for (int i = 0; i < dirs.length; i++) {
            if (!dirs[i].endsWith("/"))
                dirs[i] += "/";
        }

        // copy all the items we will retain into list
        List<ZipItem> list = new ArrayList<ZipItem>(items.length);

        for (int i = 0; i < items.length; i++) {
            boolean exclude = false;

            for (int j = 0; j < dirs.length; j++) {
                if (items[i].name.startsWith(dirs[j])) {
                    exclude = true;
                    break;  // don't waste time looking at more dirs!
                }
            }

            if (!exclude) {
                list.add(items[i]);
            }
        }

        // reset items to the pruned list
        if (list.size() != items.length) {
            items = new ZipItem[list.size()];
            items = list.toArray(items);
        }
    }

    /**
     * Exclude any files that are under these directories. In this case if the
     * dir name matches with ANY directory anywhere in the path -- exclude it.
     * E.g. say you don't want to copy /a/b/c/osgi-cache/*.* then simply call
     * this method with one of the dirs equal to "osgi-cache"
     *
     * @param dirs an array of directory names
     * @since 4.0
     */
    public void excludeDirsAnywhere(String[] dirs) {
        if (dirs == null || dirs.length <= 0)
            return;
        // make sure the names all start and end with "/"
        for (int i = 0; i < dirs.length; i++) {
            if (!dirs[i].startsWith("/"))
                dirs[i] = "/" + dirs[i];
            if (!dirs[i].endsWith("/"))
                dirs[i] += "/";
        }

        // copy all the items we will retain into list
        List<ZipItem> list = new ArrayList<ZipItem>(items.length);

        for (int i = 0; i < items.length; i++) {
            boolean exclude = false;
            String item = "/" + items[i].name;

            for (int j = 0; j < dirs.length; j++) {
                if (item.indexOf(dirs[j]) >= 0) {
                    exclude = true;
                    break;  // don't waste time looking at more dirs!
                }
            }

            if (!exclude) {
                list.add(items[i]);
            }

        }
        // reset items to the pruned list
        if (list.size() != items.length) {
            items = new ZipItem[list.size()];
            items = list.toArray(items);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void init(String outFileName, String dirName) throws ZipFileException {
        try {
            // ambiguous overload of the 2 init methods if the first arg is plain null.
            init((OutputStream)null, dirName);
            userOutFile = new File(outFileName);
        }
        catch (Exception e) {
            throw new ZipFileException(e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void close() {
        // close the stream in ALL cases -- even if the caller sent in the already-made stream
        try {
            if (zipStream != null)
                zipStream.close();
        }
        catch (Exception e) {
            // nothing can be done about it.
        }
        zipStream = null;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void init(OutputStream outStream, String dirName) throws ZipFileException {
        try {
            userStream = outStream; // might be null

            if (dirName == null)
                throw new IllegalArgumentException("null dirName");

            //make sure it's really a directory
            File f = new File(dirName);

            if (!f.exists())
                throw new ZipFileException("directory (" + dirName + ") doesn't exist");

            if (!f.isDirectory())
                throw new ZipFileException(dirName + " is not a directory");

            // change the filename to be full-path & UNIX style
            try {
                dirName = f.getCanonicalPath();
            }
            catch (IOException e) {
                dirName = f.getAbsolutePath();
            }

            dirName = dirName.replace('\\', '/');    // all UNIX-style filenames...


            // we need the dirname to end in a '/'
            if (!dirName.endsWith("/"))
                dirName += "/";

            this.dirName = dirName;
        }
        catch (ZipFileException zfe) {
            throw zfe;
        }
        catch (Throwable t) {
            throw new ZipFileException(t);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Does not throw an exception when there is a duplicate zip entry.
     *
     * @throws ZipFileException if an error while creating the archive
     */
    public void safeWrite() throws ZipFileException {
        try {
            setupZipStream();
            for (int i = 0; i < items.length; i++) {
                try {
                    addEntry(items[i]);
                }
                catch (ZipException e) {
                    // ignore - duplicate zip entry
                }
            }
        }
        catch (ZipFileException z) {
            throw z;
        }
        catch (Exception e) {
            throw new ZipFileException(e);
        }
        finally {
            close();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public void write() throws ZipFileException {
        try {
            setupZipStream();
            for (int i = 0; i < items.length; i++) {
                addEntry(items[i]);
            }
        }
        catch (ZipFileException z) {
            throw z;
        }
        catch (Exception e) {
            throw new ZipFileException(e);
        }
        finally {
            close();
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void addEntry(ZipItem item) throws ZipFileException, IOException {
        int totalBytes = 0;
        ZipEntry ze = new ZipEntry(item.name);

        zipStream.putNextEntry(ze);
        if (!item.name.endsWith("/")) {
            FileInputStream in = new FileInputStream(item.file);

            try {
                for (int numBytes = in.read(buffer); numBytes > 0; numBytes = in.read(buffer)) {
                    zipStream.write(buffer, 0, numBytes);
                    totalBytes += numBytes;
                }
            }
            finally {
                if (in != null)
                    try {
                        in.close();
                    }
                    catch (IOException e) {
                    }
            }
        }

        zipStream.closeEntry();
        Logger.getAnonymousLogger().finer("Wrote " + item.name + " to Zip File.  Wrote " + totalBytes + " bytes.");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void createItemList(String[] files) throws ZipFileException {
        try {
            if (files == null) {
                FileListerRelative lister = new FileListerRelative(new File(dirName));
                files = lister.getFiles();
            }

            if (files.length <= 0)
                throw new ZipFileException("No files to add!");

            items = new ZipItem[files.length];

            for (int i = 0; i < files.length; i++) {
                File f = new File(dirName + files[i]);
                items[i] = new ZipItem(f, files[i].replace('\\', '/'));    // just in case...

                // bnevins -- add a trailing "/" to empty directories
                if (f.isDirectory())
                    items[i].name += "/";
            }
        }
        catch (Throwable t) {
            throw new ZipFileException(t);
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    String getDirName() {
        return dirName;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static void usage() {
        System.out.println("usage: java com.elf.util.zip.ZipWriter zip-filename directory-name");
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            usage();
            return;
        }

        try {
            ZipWriter zw = new ZipWriter(args[0], args[1]);
            zw.write();
        }
        catch (ZipFileException e) {
            // nothing to do.
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    private void setupZipStream() throws FileNotFoundException {
        // 2 cases, either user supplied an output filename or an output stream
        if(userStream != null)
            zipStream = new ZipOutputStream(userStream);
        else
            zipStream = new ZipOutputStream(new FileOutputStream(userOutFile));
    }
    /////////////////////////////////////////////////////////////////////////////////////////
    //private                    String            zipFilename        = null;
    private String dirName = null;
    private OutputStream userStream;
    private File userOutFile;
    private ZipOutputStream zipStream;
    private byte[] buffer = new byte[16384];
    private ZipItem[] items = null;

}
