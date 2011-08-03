/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.cluster.ssh.sftp;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.SFTPException;
import com.trilead.ssh2.SFTPv3Client;
import com.trilead.ssh2.SFTPv3FileAttributes;
import com.trilead.ssh2.SFTPv3FileHandle;
import com.trilead.ssh2.sftp.ErrorCodes;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import org.glassfish.cluster.ssh.util.SSHUtil;

public class SFTPClient extends SFTPv3Client {

    private Connection connection = null;

    public SFTPClient(Connection conn) throws IOException {
        super(conn);
        this.connection = conn;
        SSHUtil.register(connection);
    }

    /**
     * Close the SFTP connection and free any resources associated with it.
     * close() should be called when you are done using the SFTPClient
     */
    @Override
    public void close() {
        if (connection != null) {
            SSHUtil.unregister(connection);
            connection = null;
        }
        super.close();
    }

    /**
     * Checks if the given path exists.
     */
    public boolean exists(String path) throws IOException {
        return _stat(normalizePath(path))!=null;
    }

    /**
     * Graceful {@link #stat(String)} that returns null if the path doesn't exist.
     */
    public SFTPv3FileAttributes _stat(String path) throws IOException {
        try {
            return stat(normalizePath(path));
        } catch (SFTPException e) {
            int c = e.getServerErrorCode();
            if (c== ErrorCodes.SSH_FX_NO_SUCH_FILE || c==ErrorCodes.SSH_FX_NO_SUCH_PATH)
                return null;
            else
                throw e;
        }
    }

    /**
     * Makes sure that the directory exists, by creating it if necessary.
     */
    public void mkdirs(String path, int posixPermission) throws IOException {
        path =normalizePath(path);
        SFTPv3FileAttributes atts = _stat(path);
        if (atts!=null && atts.isDirectory())
            return;

        int idx = path.lastIndexOf("/");
        if (idx>0)
            mkdirs(path.substring(0,idx), posixPermission);

        try {
            mkdir(path, posixPermission);
        } catch (IOException e) {
            throw new IOException("Failed to mkdir "+path,e);
        }
    }

    /**
     * Creates a new file and writes to it.
     */
    public OutputStream writeToFile(String path) throws IOException {
        path =normalizePath(path);
        final SFTPv3FileHandle h = createFile(path);
        return new OutputStream() {
            private long offset = 0;
            public void write(int b) throws IOException {
                write(new byte[]{(byte)b});
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                SFTPClient.this.write(h,offset,b,off,len);
                offset += len;
            }

            @Override
            public void close() throws IOException {
                closeFile(h);
            }
        };
    }

    public InputStream read(String file) throws IOException {
        file =normalizePath(file);         
        final SFTPv3FileHandle h = openFileRO(file);
        return new InputStream() {
            private long offset = 0;

            public int read() throws IOException {
                byte[] b = new byte[1];
                if(read(b)<0)
                    return -1;
                return b[0];
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int r = SFTPClient.this.read(h,offset,b,off,len);
                if (r<0)    return -1;
                offset += r;
                return r;
            }

            @Override
            public long skip(long n) throws IOException {
                offset += n;
                return n;
            }

            @Override
            public void close() throws IOException {
                closeFile(h);
            }
        };
    }

    public void chmod(String path, int permissions) throws IOException {
        path =normalizePath(path);
        SFTPv3FileAttributes atts = new SFTPv3FileAttributes();
        atts.permissions = permissions;
        setstat(path, atts);
    }

    // Commands run in a shell on Windows need to have forward slashes.
    public static String normalizePath(String path){
        return path.replaceAll("\\\\","/");
    }

}
