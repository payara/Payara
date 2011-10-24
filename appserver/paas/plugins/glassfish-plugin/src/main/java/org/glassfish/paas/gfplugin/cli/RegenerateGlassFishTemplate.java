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
package org.glassfish.paas.gfplugin.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.io.FileUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.iaas.CloudProvisioner;
import org.glassfish.virtualization.runtime.VirtualClusters;
import org.glassfish.virtualization.spi.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author ishan.vishnoi@java.net
 */
@Service(name = "regenerate-glassfish-template")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class RegenerateGlassFishTemplate implements AdminCommand, Runnable {

    @Param(name = "targetdir", optional = false)
    private String targetdir;
    @Param(name = "glassfishlocation", optional = false)
    private String gflocation;
    @Param(name = "waitforcompletion", optional = true, defaultValue = "true")
    private boolean waitforcompletion;
    @Param(name = "servicename", primary = true)
    private String serviceName;
    @Param(name = "virtualcluster", optional = true)
    private String virtualClusterName;
    @Param(name = "templateid", optional = true)
    private String templateId;
    @Param(name = "servicecharacteristics", optional = true, separator = ':')
    public Properties serviceCharacteristics;
    public Properties serviceConfigurations;
    private String clusterName;
    private String instanceName;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;
    @Inject(optional = true) // made it optional for non-virtual scenario to work
    IAAS iaas;
    @Inject(optional = true) // // made it optional for non-virtual scenario to work
    VirtualClusters virtualClusters;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        System.out.println("regenerate-glassfish-template called.");

        clusterName = serviceName.indexOf(".") > -1
                ? serviceName.substring(0, serviceName.indexOf(".")) : serviceName;

        String dasIPAddress = "Obtaining";

        if (waitforcompletion) {
            run();
        } else {
            ServiceUtil.getThreadPool().execute(this);
        }
    }

    public void run() {
        TemplateInstance matchingTemplate = null;
        if (templateRepository != null) {
            if (templateId == null) {
                // search for matching template based on service characteristics
                if (serviceCharacteristics != null) {
                    
                    Set<TemplateCondition> andConditions = new HashSet<TemplateCondition>();
                    andConditions.add(new org.glassfish.virtualization.util.ServiceType(
                            serviceCharacteristics.getProperty("service-type")));

                    for (TemplateInstance ti : templateRepository.all()) {
                        boolean allConditionsSatisfied = true;
                        for (TemplateCondition condition : andConditions) {
                            if (!ti.satisfies(condition)) {
                                allConditionsSatisfied = false;
                                break;
                            }
                        }
                        if (allConditionsSatisfied) {
                            matchingTemplate = ti;
                            break;
                        }
                    }
                    if (matchingTemplate != null) {
                        templateId = matchingTemplate.getConfig().getName();
                    }
                }
            } else {
                for (TemplateInstance ti : templateRepository.all()) {
                    if (ti.getConfig().getName().equals(templateId)) {
                        matchingTemplate = ti;
                        break;
                    }
                }
            }
        }

        if (matchingTemplate != null) {
            try {

                VirtualCluster vCluster = virtualClusters.byName(virtualClusterName);
                int min = 1;
                List<PhasedFuture<AllocationPhase, VirtualMachine>> futures =
                        new ArrayList<PhasedFuture<AllocationPhase, VirtualMachine>>();

                for (int i = 0; i < min; i++) {
                    PhasedFuture<AllocationPhase, VirtualMachine> future =
                            iaas.allocate(new AllocationConstraints(matchingTemplate, vCluster), null);
                    futures.add(future);
                }

                NetworkInterface ni = NetworkInterface.getByName("br0");
                Enumeration<InetAddress> e = ni.getInetAddresses();
                String ipaddress = "";
                while (e.hasMoreElements()) {
                    ipaddress = e.nextElement().toString();
                }

                if (gflocation.contains("http")) {
                    URL gf = new URL(gflocation);
                    FileOutputStream fout = new FileOutputStream(System.getenv("S1AS_HOME") + "/domains/domain1/docroot/glassfish.zip");
                    FileUtils.copy(gf.openStream(), fout, Integer.MAX_VALUE);
                } else {
                    FileUtils.copy(gflocation, System.getenv("S1AS_HOME") + "/domains/domain1/docroot/glassfish.zip");
                }

                for (PhasedFuture<AllocationPhase, VirtualMachine> future : futures) {
                    VirtualMachine vm = future.get();
                    String echo = vm.executeOn(new String[]{"echo \"cloud\" > /home/cloud/p "});
                    String rm = vm.executeOn(new String[]{"rm /home/cloud/glassfish.zip*"});
                    String wget = vm.executeOn(new String[]{"wget http:/" + ipaddress + ":8080/glassfish.zip"});
                    String stopGF = vm.executeOn(new String[]{"/opt/glassfishvm/glassfish3/glassfish/bin/asadmin",
                                "stop-local-instance"});
                    String rm1 = vm.executeOn(new String[]{"rm -rf /opt/glassfishvm/glassfish3"});
                    String unzip = vm.executeOn(new String[]{"unzip -d /opt/glassfishvm/ glassfish.zip"});
                    String rm2 = vm.executeOn(new String[]{"sudo -S rm /etc/opt/glassfishvm/configured_ip < /home/cloud/p"});
                    String umount = vm.executeOn(new String[]{"sudo -S umount /etc/opt/glassfishvm/cust < /home/cloud/p"});
                    String rm3 = vm.executeOn(new String[]{"sudo -S rm -rf /etc/opt/glassfishvm/cust < /home/cloud/p"});
                    String rm4 = vm.executeOn(new String[]{"rm /home/cloud/p"});
                    String rm5 = vm.executeOn(new String[]{"rm /home/cloud/glassfish.zip*"});

                    vm.stop();
                    FileUtils.copy(System.getenv("HOME") + "/virt/disks/glassfish1.img", targetdir);
                    FileUtils.whack(new File(System.getenv("HOME") + "/virt/templates"));
                    FileUtils.whack(new File(System.getenv("HOME") + "/virt/disks"));

                    vm.delete();                   
                }
            } catch (Throwable ex) {
                throw new RuntimeException(ex);
            }
            return;
        }
    }
}

