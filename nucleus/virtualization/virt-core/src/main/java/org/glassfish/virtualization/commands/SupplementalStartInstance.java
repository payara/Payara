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

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.hk2.Factory;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.os.Disk;
import org.glassfish.virtualization.os.FileOperations;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    @Inject
    Disk custDisk;

    @Inject
    AuthTokenManager authTokenManager;

    @Override
    public void execute(AdminCommandContext context) {

        if (virtualizations==null || instanceName.indexOf("_")==-1) {
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
            return;
        }
        final String groupName = instanceName.substring(0, instanceName.indexOf("_"));
        final String machineName = instanceName.substring(instanceName.indexOf("_")+1, instanceName.lastIndexOf("_"));
        final String vmName = instanceName.substring(instanceName.lastIndexOf("_")+1, instanceName.length()-"Instance".length());

        ServerPool group = groups.byName(groupName);
        try {
            VirtualMachine vm = group.vmByName(vmName);
            VirtualMachineInfo vmInfo = vm.getInfo();
            if (Machine.State.RESUMING.equals(vmInfo.getState()) || Machine.State.READY.equals(vmInfo.getState())) {
                context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }
        } catch (VirtException e) {
            RuntimeContext.logger.warning(e.getMessage());
        }

        File machineDisks = RuntimeContext.absolutize(new File(virtualizations.getDisksLocation(), group.getName()));
        machineDisks = new File(machineDisks, machineName);
        File custDir = new File(machineDisks, vmName + "cust");
        File custFile = new File(custDir, "customization");

        try {
            if (custFile.exists() && group instanceof PhysicalServerPool) {

                // only if ISO customization is present (ie not for JRVE)
                // this needs to be moved to virtualization specific code.

                Properties customizedProperties = new Properties();

                // read existing properties
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(custFile);
                    customizedProperties.load(fileReader);
                } finally {
                    fileReader.close();
                }

                // we create 3 tokens as the starting VM may invoke 3 commands, one to change IP address
                // one to notify of startup, one for the start-local-instance
                customizedProperties.put("AuthToken", authTokenManager.createToken());
                customizedProperties.put("AuthToken2", authTokenManager.createToken());
                customizedProperties.put("StartToken", authTokenManager.createToken());

                // write them out
                FileWriter fileWriter = null;
                try {
                    fileWriter = new FileWriter(new File(custDir, "customization"));
                    customizedProperties.store(fileWriter, "Customization properties for virtual machine" + vmName);
                } finally {
                    fileWriter.close();
                }

                final File custISOFile = new File(machineDisks, vmName + "cust.iso");
                custDisk.createISOFromDirectory(custDir, custISOFile);

                PhysicalServerPool physicalGroup = (PhysicalServerPool) group;
                final Machine machine = physicalGroup.byName(machineName);
                machine.execute(new MachineOperations<Object>() {
                    @Override
                    public Object run(FileOperations fileOperations) throws IOException {
                        fileOperations.delete(machine.getConfig().getDisksLocation() + "/" + vmName + "cust.iso");
                        fileOperations.copy(custISOFile, new File(machine.getConfig().getDisksLocation()));
                        return null;
                    }
                });
            }

            VirtualMachine vm = group.vmByName(vmName);

            CountDownLatch latch = vmLifecycle.get().inStartup(vm.getName());
            vmLifecycle.get().start(vm);
            try {
                latch.await(90, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                context.getActionReport().failure(RuntimeContext.logger, "Virtual machine " + vmName +
                    " took too long to register its startup");
                return;
            }

        } catch(Exception e) {
            e.printStackTrace();
            context.getActionReport().failure(RuntimeContext.logger, e.getMessage(), e);
            try {
                custDisk.umount();
            } catch(IOException exe) {
                //
            }
            return;
        }
        context.getActionReport().setActionExitCode(ActionReport.ExitCode.SUCCESS);
        //String nodeName = instanceName.substring(0, instanceName.length() - "Instance".length());
        //rtContext.executeAdminCommand(context.getActionReport(), "ping-node-ssh", nodeName);
    }
}
