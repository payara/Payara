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
// Portions Copyright 2026 Payara Foundation and/or affiliates

package org.glassfish.cluster.ssh.sftp;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.SftpClientFactory;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SFTP client wrapper around Apache MINA SSHD's {@link SftpClient}.
 *
 * <p>Manages the lifecycle of the underlying SshClient and ClientSession,
 * closing both when this client is closed.
 */
public class SFTPClient implements AutoCloseable {

    private final SshClient sshClient;
    private final ClientSession session;
    private final SftpClient delegate;

    public SFTPClient(SshClient sshClient, ClientSession session) throws IOException {
        this.sshClient = sshClient;
        this.session = session;
        this.delegate = SftpClientFactory.instance().createSftpClient(session);
    }

    /**
     * Creates an SFTP subsystem over an existing session.
     * This client does NOT own the session lifecycle — the session will not be
     * closed when this client is closed. Use this when the session is managed
     * by an {@link SSHConnection}.
     */
    public SFTPClient(ClientSession session) throws IOException {
        this.sshClient = null;
        this.session = null;
        this.delegate = SftpClientFactory.instance().createSftpClient(session);
    }

    /**
     * Closes the SFTP connection and releases all underlying resources.
     */
    @Override
    public void close() {
        try {
            delegate.close();
        } catch (IOException ignored) {
        }
        if (session != null) {
            try {
                session.close();
            } catch (IOException ignored) {
            }
        }
        if (sshClient != null) {
            try {
                sshClient.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Normalizes a remote path for use with Windows OpenSSH SFTP.
     *
     * <ul>
     *   <li>Backslashes are converted to forward slashes.
     *   <li>{@code C:path} (drive-relative, typically caused by the Linux shell
     *       stripping the backslash from {@code C:\path}) is promoted to the
     *       absolute {@code C:/path} so the SFTP server sees the correct location.
     * </ul>
     */
    public static String normalizePath(String path) {
        path = path.replace('\\', '/');
        // "C:path" → "C:/path": a Windows drive-relative path caused by the Linux
        // shell stripping the backslash from "C:\path".  Only applied when the
        // first character is a letter (Windows drive letter convention) so that
        // Unix paths are never affected.
        if (path.length() >= 2
                && Character.isLetter(path.charAt(0))
                && path.charAt(1) == ':'
                && (path.length() == 2 || path.charAt(2) != '/')) {
            path = path.substring(0, 2) + "/" + path.substring(2);
        }
        return path;
    }

    /**
     * Returns {@code true} if the remote path exists.
     */
    public boolean exists(String path) throws IOException {
        return _stat(normalizePath(path)) != null;
    }

    /**
     * Stat that returns {@code null} when the path does not exist instead of
     * throwing an exception.
     */
    public SftpClient.Attributes _stat(String path) throws IOException {
        try {
            return delegate.stat(normalizePath(path));
        } catch (SftpException e) {
            if (e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_FILE
                    || e.getStatus() == SftpConstants.SSH_FX_NO_SUCH_PATH) {
                return null;
            }
            throw e;
        }
    }

    /**
     * Creates every missing directory component in {@code path} with
     * {@code posixPermission}.
     */
    public void mkdirs(String path, int posixPermission) throws IOException {
        path = normalizePath(path);
        SftpClient.Attributes atts = _stat(path);
        if (atts != null && atts.isDirectory()) {
            return;
        }

        int idx = path.lastIndexOf('/');
        if (idx > 0) {
            mkdirs(path.substring(0, idx), posixPermission);
        }

        try {
            delegate.mkdir(path);
            SftpClient.Attributes perms = new SftpClient.Attributes();
            perms.setPermissions(posixPermission);
            delegate.setStat(path, perms);
        } catch (IOException e) {
            throw new IOException("Failed to mkdir " + path, e);
        }
    }

    /**
     * Opens an {@link OutputStream} for the given remote path, creating the
     * file if it does not exist (truncates if it already exists).
     */
    public OutputStream writeToFile(String path) throws IOException {
        return delegate.write(normalizePath(path));
    }

    /**
     * Opens an {@link InputStream} for the given remote file.
     */
    public InputStream read(String file) throws IOException {
        return delegate.read(normalizePath(file));
    }

    /**
     * Changes the POSIX permission bits of a remote path.
     */
    public void chmod(String path, int permissions) throws IOException {
        path = normalizePath(path);
        SftpClient.Attributes attrs = new SftpClient.Attributes();
        attrs.setPermissions(permissions);
        delegate.setStat(path, attrs);
    }

    public List<SftpClient.DirEntry> ls(String dir) throws IOException {
        dir = normalizePath(dir);
        List<SftpClient.DirEntry> result = new ArrayList<>();
        for (SftpClient.DirEntry entry : delegate.readDir(dir)) {
            result.add(entry);
        }
        return result;
    }

    public void rm(String path) throws IOException {
        delegate.remove(normalizePath(path));
    }

    public void rmdir(String path) throws IOException {
        delegate.rmdir(normalizePath(path));
    }

    public SftpClient.Attributes lstat(String path) throws IOException {
        return delegate.lstat(normalizePath(path));
    }

    public void setStat(String path, SftpClient.Attributes attrs) throws IOException {
        delegate.setStat(normalizePath(path), attrs);
    }
}
