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


import com.sun.enterprise.admin.util.ColumnFormatter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.SharedService;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.Collection;


/**
 * @author Jagadish Ramu
 */
@org.jvnet.hk2.annotations.Service(name = "_list-glassfish-services")
@Scoped(PerLookup.class)
public class ListGlassFishServices implements AdminCommand {

    @Inject
    private ProvisionerUtil registryService;

    @Inject
    private GlassFishServiceUtil serviceUtil;

    @Param(name = "servicename", defaultValue = "*", optional = true, primary = true)
    private String serviceName;

    @Param(name="appname", optional=true)
    private String appName;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        Collection<Service> glassFishServices = new ArrayList<Service>();
        try {
            if (serviceName.equals("*")) {
                Services services = serviceUtil.getServices();
                for(Service service : services.getServices()){
                    if(service.getType().equals("ClusterInstance") || service.getType().equals("StandaloneInstance") || service.getType().equals("Cluster")){
                        if(appName != null){
                            if(service instanceof ApplicationScopedService){
                                if(appName.equals(((ApplicationScopedService)service).getApplicationName())){
                                    glassFishServices.add(service);
                                }
                            }
                        }else{
                            glassFishServices.add(service);
                        }
                    }
                }
            } else if (serviceName.endsWith("*")) {
                String wildCardString = serviceName.substring(0, serviceName.lastIndexOf("*"));
                Services services = serviceUtil.getServices();
                for(Service service : services.getServices()){
                    if(serviceName.startsWith(wildCardString)){
                        if(service.getType().equals("ClusterInstance") || service.getType().equals("StandaloneInstance") || service.getType().equals("Cluster")){
                            if(appName != null){
                                if(service instanceof ApplicationScopedService){
                                    if(appName.equals(((ApplicationScopedService)service).getApplicationName())){
                                        glassFishServices.add(service);
                                    }
                                }
                            }else{
                                glassFishServices.add(service);
                            }
                        }
                    }
                }
            } else if (serviceName != null) {
                String wildCardString = serviceName;
                Services services = serviceUtil.getServices();
                for(Service service : services.getServices()){
                    if(serviceName.startsWith(wildCardString)){
                        if(service.getType().equals("ClusterInstance") || service.getType().equals("StandaloneInstance") || service.getType().equals("Cluster")){
                            if(appName != null){
                                if(service instanceof ApplicationScopedService){
                                    if(appName.equals(((ApplicationScopedService)service).getApplicationName())){
                                        glassFishServices.add(service);
                                    }
                                }
                            }else{
                                glassFishServices.add(service);
                            }
                        }
                    }
                }

            }

            if (glassFishServices.size() > 0) {

                String headings[] = {"CLOUD_NAME", "IP_ADDRESS", "INSTANCE_ID", "SERVER_TYPE", "STATE"};
                ColumnFormatter cf = new ColumnFormatter(headings);

                boolean foundRows = false;
                for(Service service : glassFishServices) {
                    foundRows = true;
                    String cloudName = service.getServiceName();
                    String ipAddress = service.getPropertyValue("ip-address");
                    if(ipAddress == null){
                        ipAddress = "-";
                    }
                    String instanceID = service.getPropertyValue("instance-id");
                    if(instanceID == null){
                        instanceID = "-";
                    }
                    String serverType = service.getType();

                    String state = "-";
                    if(service instanceof ApplicationScopedService){
                        state = ((ApplicationScopedService)service).getState();
                    }else if(service instanceof SharedService){
                        state = ((SharedService)service).getState();
                    }

                    cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state});
                }
                if (foundRows) {
                    report.setMessage(cf.toString());
                } else {
                    report.setMessage("Nothing to list.");
                }
            } else {
                report.setMessage("Nothing to list.");
            }


            ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
            report.setActionExitCode(ec);
        } catch (Exception e) {
            report.setMessage("Failed to list GlassFish services");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
