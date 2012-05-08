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

package org.glassfish.virtualization.util;

import com.trilead.ssh2.SCPClient;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.VirtualMachine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common behaviors of a {@link VirtualMachine} implementation
 * @author Jerome Dochez
 */
public abstract class AbstractVirtualMachine implements VirtualMachine {

    final VirtUser user;
    final Map<PropertyName, String> properties = new HashMap<PropertyName, String>();
    final VirtualMachineConfig config;
    protected SSHLauncher sshLauncher;
    protected static final Logger logger = RuntimeContext.logger;

    protected AbstractVirtualMachine(VirtualMachineConfig config, VirtUser user) {
        this.config = config;
        this.user = user;
    }

    @Override
    public VirtUser getUser() {
        return user;
    }

    @Override
    public void setProperty(PropertyName name, String value) {
        properties.put(name, value);
    }

    @Override
    public String getProperty(PropertyName name) {
        return properties.get(name);
    }

    @Override
    public VirtualMachineConfig getConfig() {
        return config;
    }

    public boolean upload(File localFile, File remoteTargetDirectory) {
        try {
            SCPClient scpClient = getSSHLauncher().getSCPClient();
            scpClient.put(localFile.getAbsolutePath(),
                    remoteTargetDirectory.getAbsolutePath());
            logger.log(Level.INFO, "Successfully uploaded local file {0} to remote location {1} in VM {2}",
                    new Object[]{localFile.getAbsolutePath(), remoteTargetDirectory.getAbsolutePath(), getName()});
            return true;
        } catch (Exception ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, ex.getMessage(), ex);
            return false;
        }
    }

    public boolean download(File remoteFile, File localTargetDirectory) {
        try {
            SCPClient scpClient = getSSHLauncher().getSCPClient();
            scpClient.get(remoteFile.getAbsolutePath(),
                    localTargetDirectory.getAbsolutePath());
            logger.log(Level.INFO, "Successfully downloaded remote file {0} from VM {2} to local directory {1}",
                    new Object[]{remoteFile.getAbsolutePath(), localTargetDirectory.getAbsolutePath(), getName()});
            return true;
        } catch (Exception ex) {
            Logger.getAnonymousLogger().log(Level.WARNING, ex.getMessage(), ex);
            return false;
        }
    }

    protected SSHLauncher getSSHLauncher() {
        if (sshLauncher == null) {
            synchronized (this) {
                if(sshLauncher == null) {
                    sshLauncher = new SSHLauncher();
                    File home = new File(System.getProperty("user.home"));
                    String keyFile = new File(home, ".ssh/id_dsa").getAbsolutePath();
                    sshLauncher.init(getUser().getName(), getAddress().getHostAddress(), 22, null,
                            keyFile, null, Logger.getAnonymousLogger());
                }
            }
        }
        return sshLauncher;
    }

}
