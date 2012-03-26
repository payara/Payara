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

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.hk2.Factory;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

/**
 * hidden command to start the virtual machine when the instance is requested to start.
 *
 * @author Jerome Dochez
 */
@Service
@Supplemental(value = "start-instance", on= Supplemental.Timing.Before )
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class SupplementalStartInstance implements AdminCommand {

    @Param(name="instance_name", primary = true)
    String instanceName;

    @Inject
    IAAS groups;

    @Inject
    Factory<VirtualMachineLifecycle> vmLifecycle;

    @Inject(optional = true)
    Virtualizations virtualizations;

    @Param(name="_vmStartup", optional=true, defaultValue = "true")
    private String vmStartup;

    @Override
    public void execute(AdminCommandContext context) {

        if (!Boolean.valueOf(vmStartup) || instanceName.indexOf("_")==-1 || groups==null) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
        }

        if (virtualizations==null || instanceName.indexOf("_")==-1) {
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
            return;
        }
        final String groupName = instanceName.substring(0, instanceName.indexOf("_"));
        final String vmName = instanceName.substring(instanceName.lastIndexOf("_")+1, instanceName.length()-"Instance".length());

        ServerPool group = groups.byName(groupName);
        try {
            VirtualMachine vm = group.vmByName(vmName);
            vmLifecycle.get().start(vm);
        } catch (VirtException e) {
            RuntimeContext.logger.warning(e.getMessage());
        }
        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        //String nodeName = instanceName.substring(0, instanceName.length() - "Instance".length());
        //rtContext.executeAdminCommand(context.getActionReport(), "ping-node-ssh", nodeName);
    }
}
