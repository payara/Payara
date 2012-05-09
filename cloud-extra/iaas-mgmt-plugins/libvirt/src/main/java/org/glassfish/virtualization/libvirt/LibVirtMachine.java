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
package org.glassfish.virtualization.libvirt;

import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import com.trilead.ssh2.SFTPv3FileAttributes;
import com.trilead.ssh2.SFTPv3DirectoryEntry;

import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.*;
import org.glassfish.virtualization.spi.MachineOperations;
import org.glassfish.virtualization.spi.PhysicalServerPool;

import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of a remote physical machine managed by the libvirt interfaces.
 *
 * @author Jerome Dochez
 */
final class LibVirtMachine extends LibVirtLocalMachine {

    // Sa far, IP addresses are static within a single run. we could support changing the IP address eventually.
    final String ipAddress;

    @Inject
    SSHLauncher sshLauncher;

    SSHFileOperations sshFileOperations;
    int references=0;

    public static LibVirtMachine from(Injector injector, LibVirtServerPool group, MachineConfig config, String ipAddress) {
        return injector.inject(new LibVirtMachine(group, config, ipAddress));
    }

    protected  LibVirtMachine(LibVirtServerPool group, MachineConfig config, String ipAddress) {
        super(group, config);
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    @Override
    public <T> T execute(MachineOperations<T> operations) throws IOException {

        SSHFileOperations sshFileOperations;
        try {
             sshFileOperations = connect();
        } catch (IOException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot open connection to machine " + this, e);
            throw e;
        }
        try {
            return operations.run(sshFileOperations);
        } finally {
            disconnect();
        }
    }

    @Override
    public PhysicalServerPool getServerPool() {
        return serverPool;
    }

    public void ping() throws IOException, InterruptedException  {
        SSHLauncher ssl = getSSH();
        ssl.pingConnection();
    }

    public void sleep() throws IOException, InterruptedException  {
        SSHLauncher ssl = getSSH();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ssl.runCommand("sudo pm-suspend",baos );
        System.out.println(baos.toString());
    }

    public boolean isUp() {

        if (State.READY.equals(getState())) return true;
        if (ipAddress==null) return false;

        try {
            ping();
        } catch(Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while pinging " + config.getName() + " : "
                   + e.getMessage());
            RuntimeContext.logger.log(Level.FINE, "Exception while pinging " + config.getName(), e);
            return false;
        }
        // the machine is alive, let's connect to it's virtualization implementation
        try {
            connection();
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot connect to machine " + config.getName() +
                " with the user " + serverPool.getConfig().getUser().getName(), e);
            return false;
        }
        return true;
    }

    public VirtUser getUser() {
        if (config.getUser()!=null) {
            return config.getUser();
        } else {
            return serverPool.getConfig().getUser();
        }
    }

    protected String getUserHome() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            getSSH().runCommand("echo $HOME", baos);
        } catch(Exception e) {
            return "/home" + getUser().getName();
        }
        String userHome = baos.toString();
        // horrible hack to remove trailing \n
        return userHome.substring(0, userHome.length()-1);
    }

    private synchronized SSHFileOperations connect() throws IOException {
        if (sshFileOperations==null) {
            sshFileOperations = new SSHFileOperations(this, sshLauncher);
        }
        references++;
        return sshFileOperations;
    }

    private synchronized void disconnect() throws IOException {
        references--;
        if (references==0) {
            sshFileOperations.close();
            sshFileOperations=null;
        }
    }

    private SSHLauncher getSSH() {
        File home = new File(System.getProperty("user.home"));
        String keyFile = new File(home,".ssh/id_dsa").getAbsolutePath();
        sshLauncher.init(getUser().getName(), ipAddress, 22, null, keyFile, null, Logger.getAnonymousLogger());
        return sshLauncher;
    }
}
