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

package org.glassfish.virtualization.commands;

import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.config.TemplateIndex;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;

/**
 * hidden command to start the virtual machine when the instance is requested to start.
 *
 * @author Jerome Dochez
 */
@Service
@Supplemental(value = "start-cluster", on= Supplemental.Timing.Before )
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class SupplementalStartClusterCommand implements AdminCommand {

    @Param(optional = false, primary = true)
    private String clusterName;

    @Inject
    VirtualClusters virtualClusters;

    @Inject
    Services services;

    @Override
    public void execute(AdminCommandContext context) {
        try {
            Virtualizations virtualizations = services.forContract(Virtualizations.class).get();
            if (virtualizations==null) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
            VirtualCluster virtualCluster = virtualClusters.byName(clusterName);
            if (virtualCluster==null) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
            VirtualMachineLifecycle vmLifecycle = services.forContract(VirtualMachineLifecycle.class).get();
            Cluster cluster =  virtualCluster.getConfig();
            List<VirtualMachineConfig> vmConfigs = cluster.getExtensionsByType(VirtualMachineConfig.class);
            for (VirtualMachineConfig vmConfig : vmConfigs) {
                if (handleVM(vmConfig)) {
                    VirtualMachine vm = virtualCluster.vmByName(vmConfig.getName());
                    vmLifecycle.start(vm);
                }
            }
        } catch(VirtException e) {

        }
    }

    /**
     * Return true if we should manually start the virtual machine. In particular, we
     * don't automatically JavaEE virtual machines.
     *
     * @param vmConfig the vm configuration
     * @return true if we should start this virtual machine
     */
    private boolean handleVM(VirtualMachineConfig vmConfig) {
        /*
        for (TemplateIndex ti : vmConfig.getTemplate().getIndexes()) {
            if (ti.getType().equals("ServiceType") && ti.getValue().equals("JavaEE")) return false;
        }
        return true;
        */
        return false; // Let the individual service plugins handle vm lifecycle
    }
}
