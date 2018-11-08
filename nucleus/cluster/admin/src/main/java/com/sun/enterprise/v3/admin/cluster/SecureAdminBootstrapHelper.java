/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.util.cluster.RemoteType;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.util.cluster.windows.process.WindowsException;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFile;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFileSystem;
import com.trilead.ssh2.SFTPv3FileAttributes;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.cluster.ssh.sftp.SFTPClient;
import org.glassfish.cluster.ssh.util.DcomInfo;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Bootstraps the secure admin-related files, either over ssh (copying files from the
 * current runtime environment to the remote system via secure ftp) or locally
 * (using more straightforward file-copying).
 *
 * @author Tim Quinn
 */
public abstract class SecureAdminBootstrapHelper {
    private static final String DOMAIN_XML_PATH = "config/domain.xml";
    private static final String[] SECURE_ADMIN_FILE_REL_URIS_TO_COPY = new String[]{
        DOMAIN_XML_PATH,
        "config/keystore.jks",
        "config/cacerts.jks"
    };
    private static final String[] SECURE_ADMIN_FILE_DIRS_TO_CREATE = new String[]{
        "config"
    };

    /**
     * Creates a new helper for delivering files needed for secure admin to
     * the remote instance.
     *
     * @param habitat hk2 habitat
     * @param DASInstanceDir directory of the local instance - source for the required files
     * @param remoteNodeDir directory of the remote node on the remote system
     * @param instance name of the instance on the remote node to bootstrap
     * @param node Node from the domain configuration for the target node
     * @param logger Logger to use
     * @return the remote helper
     * @throws BootstrapException
     */
    public static SecureAdminBootstrapHelper getRemoteHelper(
            final ServiceLocator habitat,
            final File DASInstanceDir,
            final String remoteNodeDir,
            final String instance,
            final Node node,
            final Logger logger) throws BootstrapException {

        RemoteType type = null;

        try {
            // this also handles the case where node is null
            type = RemoteType.valueOf(node.getType());
        }
        catch (Exception e) {
            throw new IllegalArgumentException(
                    Strings.get("internal.error", "unknown type"));
        }

        switch (type) {
            case SSH:
                return new SSHHelper(
                        habitat,
                        DASInstanceDir,
                        remoteNodeDir,
                        instance,
                        node,
                        logger);
            case DCOM:
                return new DCOMHelper(
                        habitat,
                        DASInstanceDir,
                        remoteNodeDir,
                        instance,
                        node,
                        logger);
            default:
                throw new IllegalArgumentException(
                        Strings.get("internal.error", "A new type must have "
                        + "been added --> unknown type: " + type.toString()));
        }
    }

    /**
     * Creates a new helper for delivering files needed for secure admin to
     * the local instance (local meaning on the same node as the DAS).
     *
     * @param existingInstanceDir directory of an existing instance (typically the DAS) from where the files can be copied
     * @param newInstanceDir directory of the new instance to where the files will be copied
     *
     * @return the local helper
     */
    public static SecureAdminBootstrapHelper getLocalHelper(
            final File existingInstanceDir,
            final File newInstanceDir) {
        return new LocalHelper(existingInstanceDir, newInstanceDir);
    }

    /**
     * Cleans up any allocated resources.
     */
    protected abstract void mkdirs(String dirURI) throws IOException;

    protected abstract void close();

    /**
     * Copies the bootstrap files from their origin to their destination.
     * <p>
     * Concrete subclasses implement this differently, depending on exactly
     * how they actually transfer the files.
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected abstract void copyBootstrapFiles() throws FileNotFoundException, IOException;

    /**
     * Adjusts the date on the new instance's domain.xml so it looks older
     * than the original one on the DAS.
     * <p>
     * We have copied the domain.xml and a small number of other files to the
     * instance, but not all the files needed for the instance to be fully
     * sync-ed.  The sync logic decides if an instance is up-to-date by comparing
     * the timestamp of the instance's domain.xml with that of the DAS.  If
     * those timestamps match then the sync logic judges the
     * instance to be up-to-date.  When we copy the DAS domain.xml to the instance
     * to deliver the secure admin configuration (so the start-local-instance
     * command and the instance will know how to connect to the DAS) it is left
     * with the same timestamp as the DAS copy.  To make sure sync works when
     * start-local-instance runs, we backdate the instance's copy of domain.xml.
     *
     * @throws com.sun.enterprise.v3.admin.cluster.SecureAdminBootstrapHelper.BootstrapException
     */
    protected abstract void backdateInstanceDomainXML() throws BootstrapException;

    /**
     * Bootstraps the instance for remote admin.
     *
     * @throws BootstrapException
     */
    public void bootstrapInstance() throws BootstrapException {
        try {
            mkdirs();
            copyBootstrapFiles();
            backdateInstanceDomainXML();
        }
        catch (Exception ex) {
            throw new BootstrapException(ex);
        }
    }

    private void mkdirs() throws IOException {
        for (String dirPath : SECURE_ADMIN_FILE_DIRS_TO_CREATE) {
            mkdirs(dirPath);
        }
    }

    /**
     * Implements the helper functionality for a remote instance.
     */
    private abstract static class RemoteHelper extends SecureAdminBootstrapHelper {
        final Logger logger;
        final File dasInstanceDir;
        final String instance;
        final String remoteNodeDir;
        final String remoteInstanceDir;

        RemoteHelper(
                final ServiceLocator habitat,
                final File dasInstanceDir,
                String remoteNodeDir,
                final String instance,
                final Node node,
                final Logger logger) throws BootstrapException {
            this.dasInstanceDir = dasInstanceDir;
            this.instance = instance;
            this.logger = logger;

            this.remoteNodeDir = remoteNodeDirUnixStyle(node, remoteNodeDir);
            remoteInstanceDir = remoteInstanceDir(this.remoteNodeDir);
        }

        abstract void writeToFile(final String path, final InputStream content) throws IOException;

        abstract void setLastModified(final String path, final long when) throws IOException;

        String ensureTrailingSlash(final String path) {
            if (!path.endsWith("/")) {
                return path + "/";
            }
            else {
                return path;
            }
        }

        String remoteNodeDirUnixStyle(final Node node, final String remoteNodeDir) {
            /*
             * Use the node dir if it was specified when the node was created.
             * Otherwise derive it: ${remote-install-dir}/glassfish/${node-name}
             */
            String result;
            if (remoteNodeDir != null) {
                result = remoteNodeDir;
            }
            else {
                result = new StringBuilder(ensureTrailingSlash(node.getInstallDirUnixStyle())).append("glassfish/nodes/").append(node.getName()).toString();
            }

            return ensureTrailingSlash(result.replaceAll("\\\\", "/"));
        }

        String remoteInstanceDir(final String remoteNodeDirPath) {
            final StringBuilder remoteInstancePath = new StringBuilder(remoteNodeDirPath);
            if (!remoteNodeDirPath.endsWith("/")) {
                remoteInstancePath.append("/");
            }
            remoteInstancePath.append(instance).append("/");
            return remoteInstancePath.toString().replaceAll("\\\\", "/");
        }

        @Override
        protected void copyBootstrapFiles() throws FileNotFoundException, IOException {
            for (String fileRelativePath : SECURE_ADMIN_FILE_REL_URIS_TO_COPY) {
                InputStream is = null;
                String remoteFilePath = null;
                try {
                    is = new BufferedInputStream(
                            new FileInputStream(
                            new File(dasInstanceDir.toURI().resolve(fileRelativePath))));
                    remoteFilePath = remoteInstanceDir + fileRelativePath;
                    writeToFile(remoteFilePath, is);
                    logger.log(Level.FINE, "Copied bootstrap file to {0}", remoteFilePath);
                }
                catch (Exception ex) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, "Error copying bootstrap file to " + remoteFilePath, ex);
                    }
                    throw new IOException(ex);
                }
                finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }

        /**
         * Returns the specified system time in seconds since 01 Jan 1970.
         *
         * @param milliseconds normal Java time (in milliseconds)
         * @return
         */
        Integer secondsSince_01_Jan_1970(final long milliseconds) {
            return (int) (milliseconds) / 1000;
        }
    }

    private static class SSHHelper extends RemoteHelper {
        final SFTPClient ftpClient;
        final SSHLauncher launcher;

        private SSHHelper(
                final ServiceLocator habitat,
                final File dasInstanceDir,
                String remoteNodeDir,
                final String instance,
                final Node node,
                final Logger logger) throws BootstrapException {
            super(habitat, dasInstanceDir, remoteNodeDir, instance, node, logger);

            launcher = habitat.getService(SSHLauncher.class);
            launcher.init(node, logger);

            try {
                ftpClient = launcher.getSFTPClient();
            }
            catch (IOException ex) {
                throw new BootstrapException(launcher, ex);
            }
        }

        @Override
        protected void mkdirs(String dir) throws IOException {
            String remoteDir = remoteInstanceDir + dir;
            logger.log(Level.FINE, "Trying to create directories for remote path {0}",
                    remoteDir);
            Integer instanceDirPermissions;
            try {
                instanceDirPermissions = ftpClient.lstat(remoteNodeDir).permissions;
            }
            catch (IOException ex) {
                throw new IOException(remoteNodeDir, ex);
            }
            logger.log(Level.FINE, "Creating remote bootstrap directory {0} with permissions {1}", new Object[]{remoteDir, instanceDirPermissions.toString()});
            try {
                ftpClient.mkdirs(remoteDir, instanceDirPermissions);
            }
            catch (IOException ex) {
                throw new IOException(remoteDir, ex);
            }
        }

        @Override
        protected void close() {
            if (ftpClient != null) {
                ftpClient.close();
            }
        }

        @Override
        void writeToFile(final String path, final InputStream content) throws IOException {
            final OutputStream os = new BufferedOutputStream(ftpClient.writeToFile(path));
            int bytesRead;
            final byte[] buffer = new byte[1024];
            try {
                while ((bytesRead = content.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }
            finally {
                os.close();
            }
        }
        /* bnevins -- this method had to be made abstract ONLY because of the
         * annoying special exception constructor that is SSH-specific.
         */

        @Override
        protected void backdateInstanceDomainXML() throws BootstrapException {
            final String remoteDomainXML = remoteInstanceDir + DOMAIN_XML_PATH;
            try {
                setLastModified(remoteDomainXML, 0);
            }
            catch (IOException ex) {
                throw new BootstrapException(launcher, ex);
            }
            logger.log(Level.FINE, "Backdated the instance's copy of domain.xml");
        }

        @Override
        void setLastModified(final String path, final long when) throws IOException {
            /*
             * Times over ssh are expressed as seconds since 01 Jan 1970.
             */
            final SFTPv3FileAttributes attrs = ftpClient.stat(path);
            attrs.mtime = secondsSince_01_Jan_1970(when);
            ftpClient.setstat(path, attrs);
        }
    }

    private static class DCOMHelper extends RemoteHelper {
        final WindowsRemoteFileSystem wrfs;
        final DcomInfo info;

        DCOMHelper(
                final ServiceLocator habitat,
                final File dasInstanceDir,
                String remoteNodeDir,
                final String instance,
                final Node node,
                final Logger logger) throws BootstrapException {
            super(habitat, dasInstanceDir, remoteNodeDir, instance, node, logger);
            try {
                info = new DcomInfo(node);
                wrfs = new WindowsRemoteFileSystem(info.getHost(), info.getUser(), info.getPassword());
            }
            catch (WindowsException ex) {
                throw new BootstrapException(ex);
            }
        }

        @Override
        protected void close() {
            // DCOM doesn't need to do anything...
        }

        @Override
        protected void mkdirs(String subdir) throws IOException {
            String remoteDir = remoteInstanceDir + subdir;
            logger.log(Level.FINE, "Trying to create directories for remote path {0}",
                    remoteDir);
            try {
                WindowsRemoteFile f = new WindowsRemoteFile(wrfs, remoteDir);
                f.mkdirs();

                if (!f.exists())
                    throw new IOException(Strings.get("no.mkdir", f.getPath()));
            }
            catch (WindowsException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        @Override
        void writeToFile(String path, InputStream content) throws IOException {
            try {
                WindowsRemoteFile f = new WindowsRemoteFile(wrfs, path);
                f.copyFrom((BufferedInputStream)content);
            }
            catch (WindowsException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        @Override
        void setLastModified(String path, long when) throws IOException {
            try {
                WindowsRemoteFile f = new WindowsRemoteFile(wrfs, path);
                f.setLastModified(when);
            }
            catch (WindowsException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }

        @Override
        protected void backdateInstanceDomainXML() throws BootstrapException {
            final String remoteDomainXML = remoteInstanceDir + DOMAIN_XML_PATH;
            try {
                setLastModified(remoteDomainXML, 0);
            }
            catch (IOException ex) {
                throw new BootstrapException(ex);
            }
            logger.log(Level.FINE, "Backdated the instance's copy of domain.xml");
        }
    }

    /**
     * Implements the helper for a local instance (one co-located with the DAS).
     */
    private static class LocalHelper extends SecureAdminBootstrapHelper {
        private final URI existingInstanceDirURI;
        private final URI newInstanceDirURI;

        private LocalHelper(final File existingInstanceDir, final File newInstanceDir) {
            this.existingInstanceDirURI = existingInstanceDir.toURI();
            this.newInstanceDirURI = newInstanceDir.toURI();
        }

        @Override
        protected void mkdirs(String dir) {
            final File newDir = new File(newInstanceDirURI.resolve(dir));
            if (!newDir.exists() && !newDir.mkdirs()) {
                throw new RuntimeException(Strings.get("secure.admin.boot.errCreDir", newDir.getAbsolutePath()));
            }
        }

        @Override
        public void copyBootstrapFiles() throws IOException {
            for (String relativePathToFile : SECURE_ADMIN_FILE_REL_URIS_TO_COPY) {
                final File origin = new File(existingInstanceDirURI.resolve(relativePathToFile));
                final File dest = new File(newInstanceDirURI.resolve(relativePathToFile));
                FileUtils.copyFile(origin, dest);
            }
        }

        @Override
        protected void backdateInstanceDomainXML() throws BootstrapException {
            final File newDomainXMLFile = new File(newInstanceDirURI.resolve(DOMAIN_XML_PATH));
            if (!newDomainXMLFile.setLastModified(0)) {
                throw new RuntimeException(Strings.get("secure.admin.boot.errSetLastMod", newDomainXMLFile.getAbsolutePath()));
            }
        }

        @Override
        protected void close() {
            // Nothing to do for local provider
        }
    }

    public static class BootstrapException extends Exception {
        private final transient SSHLauncher launcher;

        public BootstrapException(final SSHLauncher launcher, final Exception ex) {
            super(ex);
            this.launcher = launcher;
        }

        public BootstrapException(final Exception ex) {
            super(ex);
            launcher = null;
        }

        public BootstrapException(final String msg) {
            super(msg);
            launcher = null;
        }

        public String sshSettings() {
            return (launcher != null ? launcher.toString() : "");
        }
    }
}
