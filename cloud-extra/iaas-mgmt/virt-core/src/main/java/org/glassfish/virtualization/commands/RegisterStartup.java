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

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.types.Property;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;

/**
 * Administrative command to register a virtual machine is started and can be interfaced.
 * @author Jerome Dochez
 */
@Service(name="register-startup")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
   @RestEndpoint(configBean = VirtualMachineConfig.class, opType = RestEndpoint.OpType.GET, path = "register-startup", description = "Register startup")
})
public class RegisterStartup implements AdminCommand {

    @Param
    String group;

    @Param
    String machine;

    @Param
    String address;

    @Param
    String cluster;

    @Param(primary = true)
    String virtualMachine;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    @Inject
    IAAS iaas;

    @Inject
    Domain domain;

    @Inject
    VirtualClusters virtualClusters;

    @Inject
    TemplateRepository templateRepository;

    @Override
    public void execute(AdminCommandContext context) {
        RuntimeContext.logger.log(Level.INFO, "Virtual Machine " + virtualMachine + " registering its startup at " + address);
        ServerPool targetGroup = iaas.byName(group);
        if (targetGroup==null) {
            context.getActionReport().failure(RuntimeContext.logger, "Cannot find serverPool " + group);
            return;
        }
        try {
            VirtualMachine vm = targetGroup.vmByName(virtualMachine);
            if (vm!=null) {
                try {
                    vm.setAddress(InetAddress.getByName(address));
                } catch (UnknownHostException e) {
                    RuntimeContext.logger.log(Level.SEVERE,
                            "Unknown host exception while setting the virtual machine address", e);
                    vm.delete();
                    return;
                }
                Cluster clusterConfig = domain.getClusterNamed(cluster);
                VirtualMachineConfig vmConfig = clusterConfig.getExtensionsByTypeAndName(VirtualMachineConfig.class, vm.getName());
                for (Property property : vmConfig.getProperty()) {
                    vm.setProperty(VirtualMachine.PropertyName.valueOf(property.getName()), property.getValue());
                }
                Template template = vmConfig.getTemplate();
                TemplateCustomizer customizer = templateRepository.byName(template.getName()).getCustomizer();
                if (customizer!=null) {
                    customizer.start(vm, false);
                }
                CountDownLatch latch = vmLifecycle.getStartupLatch(vm.getName());
                if (latch!=null) {
                    latch.countDown();
                }
            }
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, e.getMessage(),e);
            context.getActionReport().failure(RuntimeContext.logger, e.getMessage());
            return;
        }
        RuntimeContext.logger.log(Level.INFO, "Virtual Machine " + virtualMachine + " registered its startup");

        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
