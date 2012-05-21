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

package org.glassfish.virtualization.commands;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Deletes a virtual cluster (temporary, will be probably retroffited into normal delete-cluster command).
 */
@Service(name="delete-virtual-cluster")
@Scoped(PerLookup.class)
public class DeleteVirtualCluster implements AdminCommand {

    @Param(primary=true)
    String name;

    @Inject
    Domain domain;

    @Inject
    RuntimeContext rtContext;

    @Inject
    IAAS iaas;

    @Inject
    VirtualClusters virtualClusters;

    @Inject
    TemplateRepository templateRepository;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    @Override

    public void execute(final AdminCommandContext context) {

        Cluster cluster = domain.getClusterNamed(name);
        if (cluster==null) {
            context.getActionReport().failure(RuntimeContext.logger, "No cluster named " + name);
            return;
        }
        if (!cluster.isVirtual()) {
            context.getActionReport().failure(RuntimeContext.logger, "Cluster is not a virtual cluster");
            return;
        }
        List<Future> deletions = new ArrayList<Future>();
        try {
            VirtualCluster virtualCluster = virtualClusters.byName(name);
            if (virtualCluster==null) {
                context.getActionReport().failure(RuntimeContext.logger, "Cannot find cluster named " + name);
                return;
            }
            List<VirtualMachineConfig> vmConfigs = new ArrayList<VirtualMachineConfig>(
                    cluster.getExtensionsByType(VirtualMachineConfig.class));


            Map<String, VirtualMachine> vms = new HashMap<String, VirtualMachine>();
            for (VirtualMachine vm : virtualCluster.getVMs()) {
                vms.put(vm.getName(), vm);
            }

            if (vms.size()>0) {
                ExecutorService executorService = Executors.newFixedThreadPool(vms.size());
                for (VirtualMachineConfig vmConfig : vmConfigs) {
                    final VirtualMachine vm = vms.get(vmConfig.getName());
                    deletions.add(executorService.submit(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            try {
                                vmLifecycle.delete(vm);
                            } catch (Exception e) {
                                RuntimeContext.logger.log(Level.SEVERE, "Exception while shutting down virtual machine "
                                        + vm.getName(), e);
                            }
                            return null;
                        }
                    }));
                }
                // wait for deletions to be done.
                for (Future<?> future : deletions) {
                    try {
                        future.get(100, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        RuntimeContext.logger.log(Level.WARNING, "Cannot delete remote virtual machine...", e);
                    }
                }
                executorService.shutdown();
            }
            virtualClusters.remove(virtualCluster);

        } catch (VirtException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        // finally delete the cluster.
        rtContext.executeAdminCommand(context.getActionReport(), "delete-cluster", name);

        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        context.getActionReport().setMessage("Cluster " + name + " deleted successfully");
    }
}
