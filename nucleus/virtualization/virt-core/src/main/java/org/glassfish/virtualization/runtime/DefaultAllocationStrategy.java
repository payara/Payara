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

package org.glassfish.virtualization.runtime;

import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.spi.EventSource;
import org.glassfish.virtualization.util.EventSourceImpl;
import org.glassfish.virtualization.util.RuntimeContext;

import java.util.*;
import java.util.logging.Level;

/**
 * Default simple implementation of the {@link AllocationStrategy} interface
 * @author Jerome Dochez
 */
public class DefaultAllocationStrategy implements AllocationStrategy {

    final Map<ServerPool, Integer> allocationMap = new HashMap<ServerPool, Integer>();

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine>
                allocate(Collection<ServerPool> serverPools, AllocationConstraints constraints, List<Listener<AllocationPhase>> listeners)
        throws VirtException {

        EventSource<AllocationPhase> source = new EventSourceImpl<AllocationPhase>();
        if (listeners!=null) {
            for (Listener<AllocationPhase> listener : listeners) {
                source.addListener(listener, null);
            }
        }
        // populate the allocation map if necessary.
        for (ServerPool serverPool : serverPools) {
            if (!allocationMap.containsKey(serverPool)) {
                allocationMap.put(serverPool, 0);
            }
        }

        // re-order the serverPool list by ratio of utilization so we try each serverPool
        // in order
        List<ServerPool> potentialServerPools = new ArrayList<ServerPool>();
        potentialServerPools.addAll(allocationMap.keySet());

        Collections.sort(potentialServerPools, new Comparator<ServerPool>() {
            @Override
            public int compare(ServerPool o1, ServerPool o2) {
                return allocationMap.get(o1).compareTo(allocationMap.get(o2));
            }
        });

        // now try to allocate in the usage order
        while (!potentialServerPools.isEmpty()) {
            ServerPool serverPool = potentialServerPools.iterator().next();
            try {
                PhasedFuture<AllocationPhase, VirtualMachine> vm;
                if (serverPool instanceof PhysicalServerPool) {
                    ServerPoolAllocationStrategy serverPoolStrategy = getServerPoolStrategy(serverPool);
                    if (serverPoolStrategy==null) {
                        serverPoolStrategy = new DefaultServerPoolAllocationStrategy((PhysicalServerPool) serverPool);
                    }
                    vm = serverPoolStrategy.allocate(constraints, source);
                } else {
                    vm = serverPool.allocate(constraints.getTemplate(), constraints.getTargetCluster(), source);
                }
                // record the allocation
                if (vm!=null) {
                    allocationMap.put(serverPool, allocationMap.get(serverPool)+1);
                    return vm;
                } else {
                    potentialServerPools.remove(serverPool);
                }
            } catch(VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Cannot allocate virtual machine in server pool " + serverPool, e);
                potentialServerPools.remove(serverPool); // loop back and try to allocate in the next serverPool
                throw e;
            }
        }
        throw new VirtException("Cannot allocate a virtual machine in any of the configured server pools");
    }

    @Override
    public ServerPoolAllocationStrategy getServerPoolStrategy(ServerPool group) {
        return null;
    }
}
