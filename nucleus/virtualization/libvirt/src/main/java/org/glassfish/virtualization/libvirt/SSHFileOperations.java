/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.virtualization.libvirt;

import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.virtualization.spi.FileOperations;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.util.RuntimeContext;

import com.trilead.ssh2.SFTPv3FileAttributes;
import com.trilead.ssh2.SFTPv3DirectoryEntry;
import com.trilead.ssh2.SCPClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link FileOperations} implementation over SSH
 *
 * @author Jerome Dochez
 */
final class SSHFileOperations implements FileOperations {

    final SSHLauncher sshLauncher;
    final Machine machine;
    final ThreadLocal<ClientTuple> sftpClientThreadLocal = new ThreadLocal<ClientTuple>();

    private static final class ClientTuple {
        final SFTPClient sftpClient;
        final SCPClient  scpClient;

        private ClientTuple(SFTPClient sftpClient, SCPClient scpClient) {
            this.sftpClient = sftpClient;
            this.scpClient = scpClient;
        }
    }

    SSHFileOperations(Machine machine, SSHLauncher sshLauncher) throws IOException {
        this.sshLauncher = sshLauncher;
        this.machine = machine;
    }

    synchronized void close() throws IOException {

        getSFTPClient().close();
    }

    private synchronized SFTPClient getSFTPClient() throws IOException {

        ClientTuple clients = sftpClientThreadLocal.get();
        if (clients==null) {
            clients = init();
            sftpClientThreadLocal.set(clients);
        }

        return clients.sftpClient;
    }

    private synchronized SCPClient getSCPClient() throws IOException {

        ClientTuple clients = sftpClientThreadLocal.get();
        if (clients==null) {
            clients = init();
            sftpClientThreadLocal.set(clients);
        }

        return clients.scpClient;
    }

    private ClientTuple init() throws  IOException {
        File home = new File(System.getProperty("user.home"));
        String keyFile = new File(home,".ssh/id_dsa").getAbsolutePath();
        sshLauncher.init(machine.getUser().getName(), machine.getIpAddress(), 22, null, keyFile, null, Logger.getAnonymousLogger());
        return new ClientTuple(sshLauncher.getSFTPClient(), sshLauncher.getSCPClient());
    }

    @Override
    public boolean mkdir(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        if (!sftpClient.exists(path)) {
            sftpClient.mkdirs(path, 0755);
            return true;
        }
        return false;
    }

    @Override
    public boolean delete(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        if (sftpClient.exists(path)) {

            SFTPv3FileAttributes atts = sftpClient._stat(path);
            if (atts!=null && atts.isDirectory()) {
                System.out.println("Going to delete directory " + path);
                if (path.contains("cached")) {
                    Thread.dumpStack();
                }
                deleteDirectory(sftpClient,path);
            } else {
                sftpClient.rm(path);
            }
            return true;
        }
        return false;
    }

    /**
     * Recursively delete the passed directory
     * @param path directory path
     * @return true if successful
     * @throws IOException
     */
    private boolean deleteDirectory(SFTPClient sftpClient, String path) throws IOException {

        for (String fileName : ls(path)) {
            String filePath = path + File.separatorChar + fileName;
            SFTPv3FileAttributes atts = sftpClient._stat(filePath);
            if (atts!=null && atts.isDirectory()) {
                deleteDirectory(sftpClient, filePath);
            } else {
                sftpClient.rm(filePath);
            }
        }
        sftpClient.rmdir(path);
        return true;
    }

    @Override
    public synchronized boolean mv(String source, String dest) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        if (exists(dest)) {
            delete(dest);
        }
        sftpClient.mv(source, dest);
        return true;
    }

    public synchronized long length(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        return sftpClient.lstat(path).size;
    }

    @Override
    public synchronized boolean exists(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        return sftpClient.exists(path);
    }

    public synchronized Date mod(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        long mTimeinSeconds = sftpClient._stat(path).mtime;
        return new Date(mTimeinSeconds*1000); // in milliseconds.
    }


    @Override
    public void copy(File source, File destination) throws IOException {

        SFTPClient sftpClient = getSFTPClient();
        mkdirs(destination);

        String destPath = destination + "/" + source.getName();
        if (sftpClient.exists(destPath)) {
            sftpClient.rm(destPath);
        }
        getSCPClient().put(source.getAbsolutePath(), destination.getPath());
    }

    @Override
    public void localCopy(String source, String destDir) throws IOException {

        SFTPClient sftpClient = getSFTPClient();

        try {
            if (!sftpClient.exists(destDir)) {
                // not installed
                sftpClient.mkdirs(destDir, 0755);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            RuntimeContext.logger.info("Remote cp file " + source + " on " + machine.getName());
            sshLauncher.runCommand("cp " + source + " " + destDir, baos);
        } catch (IOException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot copy file on " + machine.getName(),e);
            throw e;
        } catch(InterruptedException e) {
            throw new IOException(e);
        }

    }

    @Override
    public List<String> ls(String path) throws IOException {

        SFTPClient sftpClient = getSFTPClient();

        Vector<SFTPv3DirectoryEntry> vector = (Vector<SFTPv3DirectoryEntry>) sftpClient.ls(path);
        List<String> filePaths = new ArrayList<String>();
        for (SFTPv3DirectoryEntry element : vector) {
            if (!(element.filename.equals(".") || element.filename.equals("..")))
                filePaths.add(element.filename);
        }
        return filePaths;
    }

    public synchronized void mkdirs(File path) throws IOException {
        SFTPClient sftpClient = getSFTPClient();
        if (path.getParentFile()!=null) {
            mkdirs(path.getParentFile());
        }
        if (!sftpClient.exists(path.getPath())) {
            sftpClient.mkdirs(path.getPath(), 0755);
        }
    }
}
