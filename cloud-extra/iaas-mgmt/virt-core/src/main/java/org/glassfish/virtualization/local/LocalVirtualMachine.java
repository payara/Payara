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
package org.glassfish.virtualization.local;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.ProcessExecutor;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.AbstractVirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;

/**
 * Representation of a virtual machine in native mode.
 *
 * @author Jerome Dochez
 */
public class LocalVirtualMachine extends AbstractVirtualMachine {

    final String vmName;
    final private Machine owner;
    final LocalServerPool pool;

    public static LocalVirtualMachine from(Injector injector, VirtualMachineConfig config, LocalServerPool group, Machine owner) {
        return injector.inject(new LocalVirtualMachine(config, group, owner));
    }

    protected LocalVirtualMachine(VirtualMachineConfig config, LocalServerPool pool, Machine owner) {
        super(config, owner.getUser());
        this.vmName = config.getName();
        this.pool = pool;
        this.owner = owner;
    }

    @Override
    public void setAddress(InetAddress address) {
        //ignore
    }

    @Override
    public InetAddress getAddress() {
        try {
            String name = owner.getConfig().getNetworkName();
            if (name != null) {
                return InetAddress.getByName(name);
            } else {
                return InetAddress.getByName(owner.getIpAddress());
            }
        } catch (UnknownHostException e1) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot create ip address");
            return null;
        }
    }

    @Override
    public void start() throws VirtException {
        for (Cluster cluster : pool.serverPoolFactory.getDomain().getClusters().getCluster()) {
            for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                if (vmc.getName().equals(getName())) {
                    TemplateInstance ti = pool.serverPoolFactory.getTemplateRepository().byName(vmc.getTemplate().getName());
                    if (ti!=null) {
                        ti.getCustomizer().start(this, false);
                    }
                    return;
                }
            }
        }
        throw new RuntimeException("Cannot find registered virtual machine " + getName());

    }

    @Override
    public void stop() throws VirtException {
        // nothing to do ?
    }

    @Override
    public void resume() throws VirtException {
        throw new VirtException("Local processes cannot be resumed.");
    }

    @Override
    public void suspend() throws VirtException {
        throw new VirtException("Local processes cannot be suspended.");
    }

    @Override
    public String getName() {
       return vmName;
    }

    @Override
    public void delete() throws VirtException {
        // nothing to do, the delete-instance took care of it.
    }

    @Override
    public VirtualMachineInfo getInfo() {
        return new VirtualMachineInfo() {

            @Override
            public int nbVirtCpu() throws VirtException {
                return 0;
            }

            @Override
            public long memory() throws VirtException {
                return 0;
            }

            @Override
            public long cpuTime() throws VirtException {
                return 0;
            }

            @Override
            public long maxMemory() throws VirtException {
                return 0;
            }

            @Override
            public Machine.State getState() throws VirtException {
                return Machine.State.READY;
            }

            @Override
            public void registerMemoryListener(final MemoryListener ml, long period, TimeUnit unit) {
                // Not applicable
            }

            @Override
            public void unregisterMemoryListener(final MemoryListener ml) {
                // Not applicable
            }
        };
    }

    @Override
    public ServerPool getServerPool() {
        return pool;
    }

    @Override
    public Machine getMachine() {
        return owner;
    }

    @Override
    public String executeOn(String[] args) throws IOException, InterruptedException {
        ProcessExecutor processExecutor = new ProcessExecutor(args);
        try {
            String[] returnLines = processExecutor.execute(true);
            StringBuilder stringBuffer = new StringBuilder();
            if (returnLines != null) {
                for (String returnLine : returnLines) {
                    stringBuffer.append(returnLine);
                }
            }
            return stringBuffer.toString();
        } catch (ExecException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean upload(File localFile, File remoteTargetDirectory) {
        try {
            File toFile = new File(remoteTargetDirectory, localFile.getName());
            if(!toFile.equals(localFile)) {
                FileUtils.copy(localFile, toFile);
                logger.log(Level.INFO, "Successfully copied file {0} to directory {1}",
                        new Object[]{localFile.getAbsolutePath(), remoteTargetDirectory.getAbsolutePath()});
            }
            return true;
        } catch (Exception e) {
            RuntimeContext.logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean download(File remoteFile, File localTargetDirectory) {
        try {
            FileUtils.copy(remoteFile, new File(localTargetDirectory, remoteFile.getName()));
            logger.log(Level.INFO, "Successfully copied file {0} to directory {2}",
                    new Object[]{remoteFile.getAbsolutePath(), localTargetDirectory.getAbsolutePath()});
            return true;
        } catch (Exception e) {
            RuntimeContext.logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }
    }
}
