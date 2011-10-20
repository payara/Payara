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
package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.process.WindowsCredentials;
import com.sun.enterprise.universal.process.WindowsException;
import com.sun.enterprise.universal.process.WindowsRemoteScripter;
import com.sun.enterprise.util.io.WindowsRemoteFile;
import com.sun.enterprise.util.io.WindowsRemoteFileCopyProgress;
import com.sun.enterprise.util.io.WindowsRemoteFileSystem;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * @author Byron Nevins
 */
@Service(name = "install-node-dcom")
@Scoped(PerLookup.class)
public class InstallNodeDcomCommand extends InstallNodeBaseCommand {
    @Param(name = "windowsuser", shortName = "w", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "windowsdomain", shortName = "d", optional = true, defaultValue = "")
    private String windowsDomain;

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
            copyToHostsInternal(zipFile, binDirFiles, getInstallDir());
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
     * @throws WindowsException
     */
    @Override
    final void precopy() throws CommandException {
        if (getForce())
            return;

        try {
            for (String host : hosts) {
                String remotePassword = getDCOMPassword(host);
                WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, getRemoteUser(), remotePassword);
                WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, getInstallDir());

                if (remoteInstallDir.exists())
                    throw new CommandException(Strings.get("install.dir.exists", getInstallDir()));
            }
        }
        catch (WindowsException ex) {
            throw new CommandException(ex);
        }
    }

    private void copyToHostsInternal(File zipFile, ArrayList<String> binDirFiles, String windowsInstallDir)
            throws CommandException, WindowsException {
        final String zipFileName = "glassfish_install.zip";
        final String unpackScriptName = "unpack.bat";

        for (String host : hosts) {
            String remotePassword = getDCOMPassword(host);
            WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, getRemoteUser(), remotePassword);
            WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, windowsInstallDir);
            remoteInstallDir.mkdirs(getForce());
            WindowsRemoteFile remoteZip = new WindowsRemoteFile(remoteInstallDir, zipFileName);
            WindowsRemoteFile unpackScript = new WindowsRemoteFile(remoteInstallDir, unpackScriptName);
            //createUnpackScript
            System.out.printf("Copying %d bytes", zipFile.length());
            remoteZip.copyFrom(zipFile, new WindowsRemoteFileCopyProgress() {
                @Override
                public void callback(long numcopied, long numtotal) {
                    //final int percent = (int)((double)numcopied / (double)numtotal * 100.0);
                    System.out.print(".");
                }
            });
            System.out.println("");
            String fullZipFileName = SmartFile.sanitize(windowsInstallDir + "/" + zipFileName);
            String fullUnpackScriptPath = SmartFile.sanitize(windowsInstallDir + "/" + unpackScriptName);
            unpackScript.copyFrom(makeScriptString(windowsInstallDir, zipFileName));
            logger.fine("WROTE FILE TO REMOTE SYSTEM: " + fullZipFileName + " and " + fullUnpackScriptPath);
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

        logger.fine("Output from Windows Unpacker:\n" + out);
    }
}
