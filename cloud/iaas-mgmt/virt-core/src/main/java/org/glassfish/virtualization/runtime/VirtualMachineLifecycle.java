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

package org.glassfish.virtualization.runtime;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.Disk;
import org.glassfish.virtualization.spi.FileOperations;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.MachineOperations;
import org.glassfish.virtualization.spi.PhysicalServerPool;
import org.glassfish.virtualization.spi.ServerPool;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.spi.VirtualMachineInfo;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Service to register virtual machine lifecycle tokens.
 * @author Jerome Dochez
 */
@Service
public class VirtualMachineLifecycle {

    final TemplateRepository templateRepository;
    final Map<String, CountDownLatch> inStartup = new HashMap<String, CountDownLatch>();
    final Domain domain;
    @Inject
    @Optional
    private Virtualizations virtualizations;

    @Inject
    Disk custDisk;

    @Inject
    AuthTokenManager authTokenManager;

    @Inject
    public VirtualMachineLifecycle(TemplateRepository templateRepository, Domain domain) {
        this.templateRepository = templateRepository;
        this.domain = domain;
    }

    public synchronized CountDownLatch inStartup(String name) {
        CountDownLatch latch = new CountDownLatch(1);
        inStartup.put(name, latch);
        return latch;
    }

    public synchronized CountDownLatch getStartupLatch(String name) {
        return inStartup.remove(name);
    }

    /**
     * Returns the number of virtual machine in startup mode (between
     * the virtual machine start and the register-virtual-machine or
     * register-startup calls.
     *
     * @return the number of virtual machines in startup.
     */
    public int vmInStartup() {
        return inStartup.values().size();
    }

    public void start(VirtualMachine vm) throws VirtException {
        VirtualMachineInfo vmInfo = vm.getInfo();
        if (Machine.State.SUSPENDED.equals(vmInfo.getState()) ||
                Machine.State.SUSPENDING.equals(vmInfo.getState())) {
            final String vmName = vm.getName();
            final Machine machine = vm.getMachine();
            ServerPool group = vm.getServerPool();
            File machineDisks = RuntimeContext.absolutize(
                    new File(virtualizations.getDisksLocation(), group.getName()));
            machineDisks = new File(machineDisks, machine.getName());
            File custDir = new File(machineDisks, vm.getName() + "cust");
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

                    machine.execute(new MachineOperations<Object>() {
                        @Override
                        public Object run(FileOperations fileOperations) throws IOException {
                            fileOperations.delete(machine.getConfig().getDisksLocation() + "/" + vmName + "cust.iso");
                            fileOperations.copy(custISOFile, new File(machine.getConfig().getDisksLocation()));
                            return null;
                        }
                    });
                }
                CountDownLatch latch = inStartup(vmName);
                vm.start();
                try {
                    latch.await(90, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    custDisk.umount();
                } catch (IOException exe) {
                    //
                }
            }
        }
        // we do not call the customizer from here since we don't know the
        // virtual machine address, etc...
        // so we wait for the register-startup or register-virtual-machine calls
    }

    public void stop(VirtualMachine vm) throws VirtException {
        TemplateInstance ti = getTemplateInstance(vm);
        if (ti.getCustomizer()!=null) {
            ti.getCustomizer().stop(vm);
        }
        vm.stop();
    }

    public void delete(VirtualMachine vm) throws VirtException {
        TemplateInstance ti = getTemplateInstance(vm);
        if (ti.getCustomizer()!=null) {
            ti.getCustomizer().stop(vm);
            ti.getCustomizer().clean(vm);
        }
        vm.delete();
    }

    private TemplateInstance getTemplateInstance(VirtualMachine vm) {
        for (Cluster cluster : domain.getClusters().getCluster()) {
            for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                if (vmc.getName().equals(vm.getName())) {
                    return templateRepository.byName(vmc.getTemplate().getName());
                }
            }
        }
        throw new RuntimeException("Cannot find registered virtual machine " + vm.getName());
    }
}
