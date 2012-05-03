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

package org.glassfish.virtualization;


import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.runtime.DefaultAllocationStrategy;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.config.ServerPoolConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


/**
 * Service the looks up the machines in the configured groups.
 *
 *
 */
@Service
public class IAASImpl implements IAAS, ConfigListener {

    private final Habitat services;
    private final Map<String, Virtualization> virtConfigs = new ConcurrentHashMap<String, Virtualization>();
    private final Map<String, ServerPool> groups = new ConcurrentHashMap<String, ServerPool>();
    private final Map<String, VirtualCluster> virtualClusterMap = new ConcurrentHashMap<String, VirtualCluster>();

    @Override
    public Iterator<ServerPool> iterator() {
        return groups.values().iterator();
    }

    @Override
    public ServerPool byName(String groupName) {
        return groups.get(groupName);
    }

    @Inject
    public IAASImpl(@Optional Virtualizations virtualizations,
                    Transactions transactions,
                    ServerEnvironment env,
                    final Habitat services) {

        this.services = services;
        // first executeAndWait the fping command to populate our arp table.
        transactions.addListenerForType(Virtualization.class, this);
        if (virtualizations==null) {
            transactions.addListenerForType(Virtualizations.class, new ConfigListener() {
                @Override
                public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                    Virtualizations virts = services.forContract(Virtualizations.class).get();
                    for (Virtualization virt : virts.getVirtualizations()) {
                        processVirtualization(virt);
                    }
                    Dom.unwrap(virts).addListener(IAASImpl.this);
                    return null;
                }
            });
        } else {
            Dom.unwrap(virtualizations).addListener(this);
        }
        if (virtualizations==null || env.isInstance() ) return;

        for (Virtualization virt : virtualizations.getVirtualizations()) {
            processVirtualization(virt);
        }
    }

    private void processVirtualization(Virtualization virtualization) {

        if (virtConfigs.containsKey(virtualization.getName())) return;
        virtConfigs.put(virtualization.getName(), virtualization);
        Dom.unwrap(virtualization).addListener(new VirtualizationListener(virtualization));
        for (ServerPoolConfig groupConfig : virtualization.getServerPools()) {
            try {
                ServerPool group = addServerPool(groupConfig);
                System.out.println("I have a serverPool " + group.getName());
                if (group instanceof PhysicalServerPool) {
                    for (Machine machine : ((PhysicalServerPool) group).machines()) {
                        System.out.println("LibVirtMachine  " + machine.getName() + " is at " + machine.getIpAddress() + " state is " + machine.getState());
                        if (machine.getState().equals(Machine.State.READY)) {
                            try {
                                System.out.println(machine.toString());
                            } catch (Exception e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    private ServerPool addServerPool(ServerPoolConfig serverPoolConfig) {
        if (groups.containsKey(serverPoolConfig.getName())) {
            return groups.get(serverPoolConfig.getName());
        }
        ServerPoolFactory spf = services.forContract(ServerPoolFactory.class).named(
                serverPoolConfig.getVirtualization().getType()).get();

        if (spf==null) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot find an implementation of the " +
                ServerPoolFactory.class.getName() + " named " + serverPoolConfig.getVirtualization().getType());
            throw new RuntimeException("Cannot find the plugin implementation for virtualization " +
                serverPoolConfig.getVirtualization().getType());
        }
        ServerPool serverPool = spf.build(serverPoolConfig);
        groups.put(serverPoolConfig.getName(), serverPool);
        return serverPool;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        Virtualizations virtualizations = services.forContract(Virtualizations.class).get();
        Set<String> existingVirts = new HashSet<String>(virtConfigs.keySet());
        for (Virtualization virt : virtualizations.getVirtualizations()) {
            processVirtualization(virt);
            existingVirts.remove(virt.getName());
        }
        TemplateRepository templateRepository = services.forContract(TemplateRepository.class).get();
        for (String virtName : existingVirts) {
            System.out.println("Deleted virtualization : " + virtName);
            // this need to be improved by moving it to the Virtualization class.
            for (ServerPoolConfig serverPool : virtConfigs.get(virtName).getServerPools()) {
                groups.remove(serverPool.getName());
            }
            for (Template template : virtConfigs.get(virtName).getTemplates()) {
                templateRepository.delete(template);
            }
            virtConfigs.remove(virtName);
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

    private class VirtualizationListener implements ConfigListener {
        final Virtualization target;

        private VirtualizationListener(Virtualization target) {
            this.target = target;
        }

        @Override
        public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
            for (ServerPoolConfig serverPoolConfig : target.getServerPools()) {
                addServerPool(serverPoolConfig);
            }
            return null;
        }
    }
}
