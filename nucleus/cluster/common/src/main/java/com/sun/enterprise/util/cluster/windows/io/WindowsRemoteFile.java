/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.util.cluster.windows.io;

import com.sun.enterprise.util.cluster.windows.process.WindowsCredentials;
import com.sun.enterprise.util.cluster.windows.process.WindowsException;
import java.io.*;
import java.io.FileOutputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import jcifs.smb.SmbFile;

/**
 * @author Byron Nevins
 */
public final class WindowsRemoteFile {
    private SmbFile smbFile;
    private WindowsRemoteFileSystem wrfs;
    private String smbPath;

    public WindowsRemoteFile(WindowsRemoteFile parent, String path)
            throws WindowsException {
        try {
            wrfs = parent.wrfs;
            smbPath = parent.smbPath + removeLeadingAndTrailingSlashes(path) + "/";
            smbFile = new SmbFile(smbPath, wrfs.getAuthorization());
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    public WindowsRemoteFile(WindowsCredentials bonafides, String path)
            throws WindowsException {
        this(new WindowsRemoteFileSystem(bonafides), path);
    }

    public WindowsRemoteFile(WindowsRemoteFileSystem wrfs, String path)
            throws WindowsException {
        try {
            if (wrfs == null || path == null || path.isEmpty())
                throw new NullPointerException();

            if (path.indexOf(":") < 0)
                throw new IllegalArgumentException("Non-absolute path.  No colon in the path");

            this.wrfs = wrfs;
            //  this.isDir = isDir;

            // replace backslashes with forward slashes
            // replace drive designator(e:) with the default admin share for the drive (e$)

            path = path.replace('\\', '/').replace(':', '$');

            StringBuilder sb = new StringBuilder("smb://");
            sb.append(wrfs.getHost()).append("/").append(path);

            if (!path.endsWith("/"))
                sb.append('/');

            smbPath = sb.toString();
            //SmbFile remoteRoot = new SmbFile("smb://" + name + "/" + path.replace('\\', '/').replace(':', '$')+"/",createSmbAuth());

            smbFile = new SmbFile(smbPath, wrfs.getAuthorization());
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    /**
     * THis returns 3 states:
     * 1. FIle system can't be accessed or found
     * 2. it exists
     * 3. it doesn't exist
     * @return
     * @throws WindowsException
     */
    public final boolean exists() throws WindowsException {
        try {
            return smbFile.exists();
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    public final String[] list() throws WindowsException {
        try {
            return smbFile.list();
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    public final void createNewFile() throws WindowsException {
        try {
            smbFile.createNewFile();
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    public final void copyTo(WindowsRemoteFile wf) throws WindowsException {
        try {
            smbFile.copyTo(wf.smbFile);
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    /**
     * Copy the remote Windows file to the given File
     *
     * @param f The File that will be created or overwritten with the contents of
     * this Windows Remote File.
     * @throws WindowsException
     * @since 3.1.2
     */
    public final void copyTo(final File file) throws WindowsException {
        copyTo(file, null);
    }

    /**
     * Copy the remote Windows file to the given File
     *
     * @param f The File that will be created or overwritten with the contents of
     * this Windows Remote File.
     * @param progress The optional callback object that gets called for each
     * chunk of data that gets copied over the wire.  Setting it to null is OK.
     * @throws WindowsException
     * @since 3.1.2
     */
    public final void copyTo(final File file, final RemoteFileCopyProgress progress) throws WindowsException {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            final long filelength = smbFile.length();
            bis = new BufferedInputStream(smbFile.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));

            byte[] buf = new byte[getChunkSize(progress, filelength)];
            int numBytes = 0;
            long totalBytesCopied = 0;

            while ((numBytes = bis.read(buf)) >= 0) {
                bos.write(buf, 0, numBytes);
                totalBytesCopied += numBytes;

                // It's OK to send in a null Progress object
                if (progress != null)
                    progress.callback(totalBytesCopied, filelength);
            }
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
        finally {
            if (bis != null)
                try {
                    bis.close();
                }
                catch (Exception e) {
                    // this is SO messy!
                }
            if (bos != null)
                try {
                    bos.close();
                }
                catch (Exception e) {
                    // this is SO messy!
                }
        }
    }

    public final void delete() throws WindowsException {
        try {
            smbFile.delete();
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    public final void mkdirs() throws WindowsException {
        mkdirs(false);
    }

    public final void mkdirs(boolean force) throws WindowsException {
        try {
            if (exists()) {
                if (force)
                    delete();
                else
                    throw new WindowsException(Strings.get("dir.already.exists", getPath()));
            }
            smbFile.mkdirs();
        }
        catch (WindowsException we) {
            throw we;
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }
    }

    /**
     * Copies from sin to this WindowsRemoteFile
     * @param sin the opened stream.  It will automatically be closed here.
     * @throws WindowsException if any errors.
     */
    public final void copyFrom(final BufferedInputStream sin) throws WindowsException {
        copyFrom(sin, null, -1);
    }

    /**
     * If desired -- make this public sometime in the future.  For now there is no
     * reason to clog up the public namespace with it...
     */
    private final void copyFrom(final BufferedInputStream sin,
            final RemoteFileCopyProgress progress, final long filelength)
            throws WindowsException {
        OutputStream sout = null;

        if (sin == null)
            throw new NullPointerException("copyFrom stream arg is null");

        try {
            if (!exists())
                createNewFile();

            sout = new BufferedOutputStream(smbFile.getOutputStream());
            byte[] buf = new byte[getChunkSize(progress, filelength)];
            int numBytes = 0;
            long totalBytesCopied = 0;

            while ((numBytes = sin.read(buf)) >= 0) {
                sout.write(buf, 0, numBytes);
                totalBytesCopied += numBytes;

                // It's OK to send in a null Progress object
                if (progress != null)
                    progress.callback(totalBytesCopied, filelength);
            }
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
        finally {
            try {
                sin.close();
            }
            catch (Exception e) {
                // nothing can be done!
            }
            try {
                sout.close();
            }
            catch (Exception e) {
                // nothing can be done!
            }
        }
    }

    public final void copyFrom(File from, RemoteFileCopyProgress progress)
            throws WindowsException {

        try {
            if (from == null || !from.isFile())
                throw new IllegalArgumentException("copyFrom file arg is bad: " + from);

            long filesize = from.length();
            BufferedInputStream sin = new BufferedInputStream(new FileInputStream(from));
            copyFrom(sin, progress, filesize);
        }
        catch (WindowsException e) {
            throw e;
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    /*
     * Use this for tiny files -- like scripts that are created on-the-fly from a String
     */
    public final void copyFrom(Collection<String> from) throws WindowsException {
        if (from == null || from.isEmpty())
            throw new IllegalArgumentException("copyFrom String-array arg is empty");

        StringBuilder sb = new StringBuilder();

        for (String s : from) {
            // since we will write with a writer -- the \n will get translated correctly
            sb.append(s).append('\n');
        }
        copyFrom(sb.toString());
    }
    /*
     * Use this for tiny files -- like scripts that are created on-the-fly from a String
     */

    public final void copyFrom(String from) throws WindowsException {
        try {
            if (from == null || from.isEmpty())
                throw new IllegalArgumentException("copyFrom String arg is empty");

            if (!exists())
                createNewFile();

            PrintWriter pw = new PrintWriter(new BufferedOutputStream(smbFile.getOutputStream()));
            pw.print(from);

            try {
                pw.close();
            }
            catch (Exception e) {
                // nothing can be done!
            }
        }
        catch (Exception e) {
            throw new WindowsException(e);
        }
    }

    public final void setLastModified(long when) throws WindowsException {
        // time is the usual -- msec from 1/1/1970
        // Shows you just how huge a long is.  THe number of milliseconds from (probably)
        // before you were born fits easily into a long!
        try {
            smbFile.setLastModified(when);
        }
        catch (Exception se) {
            throw new WindowsException(se);
        }

    }
    // note that the path is ALWAYS appended with one and only one slash!!
    // THis is important for smb calls...

    public final String getPath() {
        return smbPath;
    }

    private String removeLeadingAndTrailingSlashes(String path) {
        while (path.startsWith("/") || path.startsWith("\\"))
            path = path.substring(1);

        while (path.endsWith("/") || path.endsWith("\\"))
            path = path.substring(0, path.length() - 1);

        return path;
    }

    private int getChunkSize(RemoteFileCopyProgress progress, long filelength) {
        int chunksize = progress == null ? 1048576 : progress.getChunkSize();

        // be careful!  filelength is a long!!!
        if(filelength < Integer.MAX_VALUE && chunksize > (int)filelength && filelength > 0)
            return (int)filelength;

        return chunksize;
    }
}
