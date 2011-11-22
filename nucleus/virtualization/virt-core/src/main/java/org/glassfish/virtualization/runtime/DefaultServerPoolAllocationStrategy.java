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
import org.glassfish.virtualization.util.RuntimeContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: 8/4/11
 * Time: 12:51 AM
 * To change this template use File | Settings | File Templates.
 */
class DefaultServerPoolAllocationStrategy implements ServerPoolAllocationStrategy {

    final PhysicalServerPool targetPool;

    DefaultServerPoolAllocationStrategy(PhysicalServerPool group) {
        this.targetPool = group;
    }

    @Override
    public PhysicalServerPool getServerPool() {
        return targetPool;
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> allocate(
            AllocationConstraints constraints, EventSource<AllocationPhase> source)
            throws VirtException {

        List<Machine> potentialMachines = new ArrayList<Machine>();
        for (Machine machine : targetPool.machines()) {
            boolean foundOne=false;
            for (VirtualMachine vm : machine.getVMs()) {
                for (VirtualMachine noColocate : constraints.separateFrom()) {
                    if (noColocate.getName().equals(vm.getName())) {
                        foundOne=true;
                        break;
                    }
                }
            }
            if (!foundOne) {
                potentialMachines.add(machine);
            }

        }
        // now find the lowest use machine, so far make it simple...
        // and do it until we got a machine that allocated our virtual machine
        while (!potentialMachines.isEmpty()) {
            float lowestUsed = Float.MAX_VALUE;
            Machine targetMachine = null;
            for (Machine machine : potentialMachines) {
                float ratio = machine.getVMs().size();
                if (ratio<lowestUsed && machine.isUp()) {
                    lowestUsed = ratio;
                    targetMachine = machine;
                }
            }
            assert(targetMachine!=null);
            try {
                return targetMachine.create(constraints.getTemplate(), constraints.getTargetCluster(), source);
            } catch(IOException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Cannot allocate virtual machine on " + targetMachine);
                potentialMachines.remove(targetMachine); // let's try on the remaining machines...
            } catch(VirtException e) {
                if (RuntimeContext.logger.isLoggable(Level.FINE)) {
                    RuntimeContext.logger.log(Level.FINE, "Cannot allocate virtual machine on " + targetMachine,e);
                } else {
                    RuntimeContext.logger.log(Level.WARNING, "Cannot allocate virtual machine on " + targetMachine + " reason : " + e.getMessage());
                }
                potentialMachines.remove(targetMachine); // let's try on the remaining machines...
            }
        }
        // if we are here we exhausted our machine pool
        throw new VirtException("None of the machine in this serverPool could allocate a virtual machine");
    }
}
