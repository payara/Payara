/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.virtualization.config.MachineConfig;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.AbstractMachine;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.ListenableFutureImpl;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;

/**
 * Abstraction for a local machine to create cluster of instances in native mode
 *
 * @author  Yamini K B
 */
public class LocalMachine extends AbstractMachine implements PostConstruct {

    final Map<String, LocalVirtualMachine> vms = new HashMap<String, LocalVirtualMachine>();

    @Inject
    Virtualizations virtualizations;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    @Inject
    com.sun.enterprise.config.serverbeans.Domain domainConfig;
    
    public static LocalMachine from(ServiceLocator injector,  LocalServerPool group, MachineConfig config) {
        LocalMachine localMachine = new LocalMachine(group, config);
		injector.inject(localMachine);
		
		return localMachine;
    }

    protected  LocalMachine(LocalServerPool group, MachineConfig config) {
        super(group, config);
    }

    @Override
    public void postConstruct() {
        setState(isUp()? State.READY: State.SUSPENDED);
        super.postConstruct();
    }

    @Override
    public StoragePool addStoragePool(String name, long capacity) throws VirtException {
        return null;
    }

    public String description() {
        StringBuilder sb = new StringBuilder();
        sb.append("Machine ").append(getName());
        try {
        // TODO: create proper description
        } catch (Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception caught :" + e,e);
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    @Override
    public Collection<? extends VirtualMachine> getVMs() throws VirtException {
        if (domainConfig.getClusters() != null) {
            for (Cluster cluster : domainConfig.getClusters().getCluster()) {
                for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                    if (vmc.getServerPool().getName().equals(config.getName())) {
                        LocalVirtualMachine vm = new LocalVirtualMachine(vmc, ((LocalServerPool)serverPool), this);
                        vms.put(vmc.getName(), vm);
                    }
                }
            }
        }
        return vms.values();
    }

    @Override
    public Map<String, ? extends org.glassfish.virtualization.spi.StoragePool> getStoragePools() throws VirtException {
        return null;
    }

    @Override
    public VirtualMachine byName(String name) throws VirtException {
        if (!vms.containsKey(name)) {
            try {
                getVMs();
            } catch (VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of vms ", e);
            }
        }
        return vms.get(name);
    }

    public void sleep() throws IOException, InterruptedException  {
        throw new IOException("Impossible to put myself to sleep");
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> create(
                final TemplateInstance template,
                final VirtualCluster cluster,
                final EventSource<AllocationPhase> source)

            throws VirtException, IOException {
        final String vmName = cluster.getConfig().getName() + cluster.allocateToken();

        VirtualMachineConfig vmConfig = VirtualMachineConfig.Utils.create(
                vmName,
                template.getConfig(),
                serverPool.getConfig(),
                cluster.getConfig());

        LocalVirtualMachine vm = new LocalVirtualMachine(vmConfig, (LocalServerPool)serverPool, (Machine)this);
        // this needs to be improved.
        vm.setProperty(VirtualMachine.PropertyName.INSTALL_DIR,
                ((LocalServerPool)serverPool).getServerPoolFactory().getServerContext().getInstallRoot().getParentFile().getAbsolutePath());
        cluster.add(vm);
        vms.put(vmName, vm);
        CountDownLatch latch = new CountDownLatch(1);

        PhasedFuture<AllocationPhase, VirtualMachine> future =
                new ListenableFutureImpl<AllocationPhase, VirtualMachine>(latch, vm, source);
        template.getCustomizer().customize(cluster, vm);
        vm.start();
        latch.countDown();
        return future;
    }
}