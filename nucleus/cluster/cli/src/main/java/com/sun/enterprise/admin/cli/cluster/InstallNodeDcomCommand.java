/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.cli.cluster;

import java.util.logging.Level;
import com.sun.enterprise.util.cluster.windows.io.RemoteFileCopyProgress;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFile;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFileSystem;
import com.sun.enterprise.util.cluster.windows.process.WindowsCredentials;
import com.sun.enterprise.util.cluster.windows.process.WindowsException;
import com.sun.enterprise.util.cluster.windows.process.WindowsRemoteScripter;
import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * @author Byron Nevins
 */
@Service(name = "install-node-dcom")
@PerLookup
public class InstallNodeDcomCommand extends InstallNodeBaseCommand {
    @Param(name = "windowsuser", shortName = "w", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "windowsdomain", shortName = "d", optional = true, defaultValue = "")
    private String windowsDomain;
    private final List<HostAndPassword> passwords = new ArrayList<HostAndPassword>();
    private String remoteInstallDirString;

    /**
     * DCOM won't work right on localhost.  Luckily it makes no sense to do that in
     * in a real, non-test scenario anyway.
     * @throws CommandException
     */
    @Override
    protected void validate() throws CommandException {
        super.validate();

        for (String host : hosts) {
            if (NetUtils.isThisHostLocal(host))
                throw new CommandException(Strings.get("install.node.nolocal", host));
        }
    }

    @Override
    final String getRawRemoteUser() {
        return user;
    }

    @Override
    int getRawRemotePort() {
        return 135; // DCOM port
    }

    @Override
    String getSshKeyFile() {
        return null;  // null -- not an empty string!
    }

    @Override
    void copyToHosts(File zipFile, ArrayList<String> binDirFiles) throws CommandException {
        try {
            copyToHostsInternal(zipFile, binDirFiles);
        }
        catch (CommandException ex) {
            throw ex;
        }
        catch (WindowsException ex) {
            throw new CommandException(ex);
        }
    }

    /**
     * bnevins: This is exclusively a "user-performance" enhancement.
     * We are forcing the failure
     * to happen before the very very slow zipfile creation.
     * FAIL FAST principle
     * This adds a bit of extra overhead to the command...
     * Note that allowing multiple hosts makes things MUCH more complicated.
     * @throws WindowsException
     */
    @Override
    final void precopy() throws CommandException {
        remoteInstallDirString = getInstallDir().replace('/', '\\');

        try {
            for (String host : hosts) {
                String remotePassword = getWindowsPassword(host);
                passwords.add(new HostAndPassword(host, remotePassword));

                if (!getForce()) {
                    WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, getRemoteUser(), remotePassword);
                    WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, remoteInstallDirString);

                    if (remoteInstallDir.exists())
                        throw new CommandException(Strings.get("install.dir.exists", remoteInstallDir));
                }
            }
        }
        catch (WindowsException ex) {
            throw new CommandException(ex);
        }
    }

    private void copyToHostsInternal(File zipFile, ArrayList<String> binDirFiles)
            throws CommandException, WindowsException {
        final String zipFileName = "glassfish_install.zip";
        final String unpackScriptName = "unpack.bat";

        for (String host : hosts) {
            String remotePassword = getPassword(host);
            WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, getRemoteUser(), remotePassword);
            WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, remoteInstallDirString);
            remoteInstallDir.mkdirs(getForce());
            WindowsRemoteFile remoteZip = new WindowsRemoteFile(remoteInstallDir, zipFileName);
            WindowsRemoteFile unpackScript = new WindowsRemoteFile(remoteInstallDir, unpackScriptName);
            //createUnpackScript
            System.out.printf("Copying %d bytes", zipFile.length());
            remoteZip.copyFrom(zipFile, new RemoteFileCopyProgress() {
                @Override
                public void callback(long numcopied, long numtotal) {
                    //final int percent = (int)((double)numcopied / (double)numtotal * 100.0);
                    System.out.print(".");
                }
                @Override
                public int getChunkSize() {
                    return 1048576;
                }
            });
            System.out.println("");
            String fullZipFileName = remoteInstallDirString + "\\" + zipFileName;
            String fullUnpackScriptPath = remoteInstallDirString + "\\" + unpackScriptName;
            unpackScript.copyFrom(makeScriptString(remoteInstallDirString, zipFileName));
            if (logger.isLoggable(Level.FINE))
                logger.fine("WROTE FILE TO REMOTE SYSTEM: " + fullZipFileName +
                            " and " + fullUnpackScriptPath);
            unpackOnHosts(host, remotePassword, fullUnpackScriptPath.replace('/', '\\'));
        }
    }

    private String makeScriptString(String windowsInstallDir, String zipFileName) {
        // first line is drive designator to make sure we are on the right drive.  E.g. "C:"
        StringBuilder scriptString = new StringBuilder(windowsInstallDir.substring(0, 2));
        scriptString.append("\r\n").append("cd \"").append(windowsInstallDir).append("\"\r\n").
                append("jar xvf ").append(zipFileName).append("\r\n");

        return scriptString.toString();
    }

    private void unpackOnHosts(String host, String remotePassword,
            String unpackScript) throws WindowsException, CommandException {
        String domain = windowsDomain;

        if (!ok(domain))
            domain = host;

        WindowsCredentials bonafides = new WindowsCredentials(host, domain, getRemoteUser(), remotePassword);
        WindowsRemoteScripter scripter = new WindowsRemoteScripter(bonafides);
        String out = scripter.run(unpackScript);

        if (out == null || out.length() < 50)
            throw new CommandException(Strings.get("dcom.error.unpacking", unpackScript, out));

        if (logger.isLoggable(Level.FINE))
            logger.fine("Output from Windows Unpacker:\n" + out);
    }

    private String getPassword(String host) {
        if (!ok(host))
            return null;

        for (HostAndPassword hap : passwords) {
            if (host.equals(hap.host))
                return hap.password;
        }

        return null;
    }

    private static class HostAndPassword {
        private final String host;
        private final String password;

        public HostAndPassword(String host, String password) {
            this.host = host;
            this.password = password;
        }
    }
}
