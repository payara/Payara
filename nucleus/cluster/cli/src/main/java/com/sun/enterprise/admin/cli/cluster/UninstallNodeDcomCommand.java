/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFile;
import com.sun.enterprise.util.cluster.windows.io.WindowsRemoteFileSystem;
import com.sun.enterprise.util.net.NetUtils;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 *
 * @author Byron Nevins
 */
@Service(name = "uninstall-node-dcom")
@PerLookup
public class UninstallNodeDcomCommand extends UninstallNodeBaseCommand {
    @Param(name = "windowsuser", shortName = "w", optional = true, defaultValue = "${user.name}")
    private String user;
    @Param(name = "windowsdomain", shortName = "d", optional = true, defaultValue = "")
    private String windowsDomain;

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
    final int getRawRemotePort() {
        return 135; // DCOM port
    }

    @Override
    final String getSshKeyFile() {
        return null;  // null -- not an empty string!
    }

    @Override
    final void deleteFromHosts() throws CommandException {
        for (String host : hosts) {
            try {
                String pw = getWindowsPassword(host);
                WindowsRemoteFileSystem wrfs = new WindowsRemoteFileSystem(host, getRemoteUser(), pw);
                WindowsRemoteFile remoteInstallDir = new WindowsRemoteFile(wrfs, getInstallDir());

                if (!remoteInstallDir.exists()) {
                    throw new CommandException(
                            Strings.get("remote.install.dir.already.gone", getInstallDir()));
                }
                remoteInstallDir.delete();

                // make sure it's gone now...
                if (remoteInstallDir.exists()) {
                    throw new CommandException(Strings.get("remote.install.dir.cant.delete", getInstallDir()));
                }
            }
            catch (CommandException ce) {
                throw ce;
            }
            catch (Exception e) {
                throw new CommandException(e);
            }
        }
    }
}
