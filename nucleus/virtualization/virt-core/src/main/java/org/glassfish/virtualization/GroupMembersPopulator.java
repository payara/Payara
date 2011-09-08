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

package org.glassfish.virtualization;


import org.glassfish.api.Startup;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.runtime.DefaultAllocationStrategy;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.config.ServerPoolConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Service the looks up the machines in the configured groups.
 *
 *
 */
@Service
public class GroupMembersPopulator implements Startup, IAAS, ConfigListener {

    private final Services services;
    private final Map<String, ServerPool> groups = new HashMap<String, ServerPool>();
    private final Map<String, VirtualCluster> virtualClusterMap = new HashMap<String, VirtualCluster>();

    @Override
    public Lifecycle getLifecycle() {
        return Lifecycle.SERVER;
    }

    @Override
    public Iterator<ServerPool> iterator() {
        return groups.values().iterator();
    }

    @Override
    public ServerPool byName(String groupName) {
        return groups.get(groupName);
    }

    public GroupMembersPopulator(@Inject(optional=true) Virtualizations virtualizations,
                                 @Inject Transactions transactions,
                                 @Inject ServerEnvironment env,
                                 @Inject Services services) {

        this.services = services;
        // first executeAndWait the fping command to populate our arp table.
        transactions.addListenerForType(ServerPoolConfig.class, this);
        if (virtualizations==null || env.isInstance() ) return;

        for (Virtualization virt : virtualizations.getVirtualizations()) {
            for (ServerPoolConfig groupConfig : virt.getServerPools()) {
                try {
                    ServerPool group = addServerPool(groupConfig);
                    System.out.println("I have a serverPool " + group.getName());
                    if (group instanceof PhysicalServerPool) {
                        for (Machine machine : ((PhysicalServerPool) group).machines()) {
                            System.out.println("LibVirtMachine  " + machine.getName() + " is at " +  machine.getIpAddress() + " state is " + machine.getState());
                            if (machine.getState().equals(Machine.State.READY)) {
                                try {
                                    System.out.println(machine.toString());
                                } catch (Exception e) {
                                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                                }
                            }
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

            }
        }
    }

    private ServerPool addServerPool(ServerPoolConfig serverPoolConfig) {
        ServerPool group = services.forContract(ServerPool.class).named(
                serverPoolConfig.getVirtualization().getType()).get();
        group.setConfig(serverPoolConfig);
        synchronized (this) {
            groups.put(serverPoolConfig.getName(), group);
        }
        return group;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        Virtualizations virtualizations = services.forContract(Virtualizations.class).get();
        for (Virtualization virt : virtualizations.getVirtualizations()) {
            for (ServerPoolConfig config : virt.getServerPools()) {
                if (!groups.containsKey(config.getName())) {
                    addServerPool(config);
                }
            }
        }
        return null;
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> allocate(AllocationConstraints order, List<Listener<AllocationPhase>> listeners) throws VirtException {
        return allocate(new DefaultAllocationStrategy(), order, listeners);
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> allocate(AllocationStrategy strategy, AllocationConstraints constraints, List<Listener<AllocationPhase>> listeners) throws VirtException {
        return strategy.allocate(groups.values(), constraints, listeners);
    }
}
