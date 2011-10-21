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
import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.libvirt.jna.Domain;
import org.glassfish.virtualization.libvirt.jna.DomainInfo;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.AbstractVirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Representation of a virtual machine in the libvirt world.
 *
 * @author Jerome Dochez
 */
public class LibVirtVirtualMachine extends AbstractVirtualMachine {

    final private Machine owner;
    final private Domain domain;
    final private List<StorageVol> storageVols;
    final private String name;
    private String address;

    protected LibVirtVirtualMachine(VirtualMachineConfig config, VirtUser user, Machine owner, Domain domain, List<StorageVol> storageVols)
            throws VirtException {
        super(config, user);
        this.domain = domain;
        this.owner = owner;
        this.name = domain.getName();
        this.storageVols = new ArrayList<StorageVol>(storageVols);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void start() throws VirtException {
        domain.create();
    }

    public void stop() throws VirtException {

        if (DomainInfo.DomainState.VIR_DOMAIN_RUNNING.equals(domain.getInfo().getState()))
            domain.destroy();
    }

    public void resume() throws VirtException {

        domain.resume();
    }

    public void suspend() throws VirtException {
        domain.suspend();
    }

    public String getName() {
       return name;
    }

    public void delete() throws VirtException {

        try {
            stop();
        } catch(VirtException e) {
            e.printStackTrace();
            // ignore any shutdown failure
        }
        for (StorageVol volume : storageVols) {
            String volumeName = volume.getName();
            try {
                volume.delete();
            } catch (VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE,
                        "Error while deleting the " + volumeName + " virtual machine disk", e);
            }
            RuntimeContext.logger.log(Level.INFO,
                    getName() + " disk " + volume.getName() + " deleted successfully");
        }
        domain.undefine();
    }

    @Override
    public VirtualMachineInfo getInfo() {
        return new VirtualMachineInfo() {

            @Override
            public int nbVirtCpu() throws VirtException {
                return domain.getInfo().nrVirtCpu;
            }

            @Override
            public long memory() throws VirtException {
                return domain.getInfo().memory.longValue();
            }

            @Override
            public long cpuTime() throws VirtException {
                return domain.getInfo().cpuTime;
            }

            @Override
            public long maxMemory() throws VirtException {
                return domain.getInfo().maxMem.longValue();
            }

            @Override
            public Machine.State getState() throws VirtException {
                try {
                    DomainInfo.DomainState state = DomainInfo.DomainState.values()[domain.getInfo().state];
                    if (DomainInfo.DomainState.VIR_DOMAIN_RUNNING.equals(state)
                            || DomainInfo.DomainState.VIR_DOMAIN_BLOCKED.equals(state)) {
                        return Machine.State.READY;
                    } else {
                        if (DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN.equals(state)) {
                            return Machine.State.SUSPENDING;
                        } else {
                            return Machine.State.SUSPENDED;
                        }
                    }
                } catch(VirtException e) {
                    throw new VirtException(e);
                }

            }

            private final Map<MemoryListener, ScheduledFuture> listeners =
                    new HashMap<MemoryListener, ScheduledFuture>();

            @Override
            public void registerMemoryListener(final MemoryListener ml, long period, TimeUnit unit) {
                final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                final VirtualMachine owner = LibVirtVirtualMachine.this;

                listeners.put(ml,
                        executor.schedule(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ml.notified(owner, memory(), cpuTime());
                                } catch (VirtException e) {
                                    RuntimeContext.logger.log(Level.FINE, "Exception while notifying of vm load ", e);
                                }
                            }
                        }, period, unit)
                );
            }

            @Override
            public void unregisterMemoryListener(final MemoryListener ml) {

                listeners.get(ml).cancel(false);
            }
        };
    }

    @Override
    public ServerPool getServerPool() {
        return owner.getServerPool();
    }

    @Override
    public Machine getMachine() {
        return owner;
    }

    @Override
    public String executeOn(String[] args) throws IOException, InterruptedException {
        SSHLauncher sshLauncher = new SSHLauncher();
        File home = new File(System.getProperty("user.home"));
        String keyFile = new File(home,".ssh/id_dsa").getAbsolutePath();
        sshLauncher.init(getUser().getName(), address, 22, null, keyFile, null, Logger.getAnonymousLogger());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg);
            stringBuilder.append(" ");
        }
        sshLauncher.runCommand(stringBuilder.toString().trim(), baos);
        return baos.toString();

    }
}
