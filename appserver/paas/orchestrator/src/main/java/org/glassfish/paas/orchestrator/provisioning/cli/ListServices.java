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

package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.admin.util.ColumnFormatter;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.List;

@org.jvnet.hk2.annotations.Service(name = "list-services")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
public class ListServices implements AdminCommand {

    @Param(name = "appname", optional = true)
    private String appName;

    @Param(name="type", optional=true)
    private String type;

    @Param(name="scope", optional=true, acceptableValues = SCOPE_EXTERNAL+","+SCOPE_SHARED+","+SCOPE_APPLICATION)
    private String scope;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    public static final String SCOPE_EXTERNAL = "external";
    public static final String SCOPE_SHARED = "shared";
    public static final String SCOPE_APPLICATION = "application";

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Services services = serviceUtil.getServices();
        List<Service> matchedServices = new ArrayList<Service>();

        if (appName != null) {
            //TODO will "target" of application play a role here ? AFAIK, no.
            if (domain.getApplications().getApplication(appName) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed");
                return;
            }

            for (Service service : services.getServices()) {
                if (service instanceof ApplicationScopedService) {
                    if (appName.equals(((ApplicationScopedService) service).getApplicationName())) {
                        if (type != null) {
                            if (service.getType().equals(type)) {
                                if(scope != null){
                                    if(scope.equals(getServiceScope(service))){
                                        matchedServices.add(service);
                                    }
                                }else{
                                    matchedServices.add(service);
                                }
                            }
                        } else {
                            if(scope != null){
                                if(scope.equals(getServiceScope(service))){
                                    matchedServices.add(service);
                                }
                            }else{
                                matchedServices.add(service);
                            }
                        }
                    }
                }
            }

            for (ServiceRef serviceRef : services.getServiceRefs()) {
                if (appName.equals(serviceRef.getApplicationName())) {
                    for (Service service : services.getServices()) {
                        if (service.getServiceName().equals(serviceRef.getServiceName())) {
                            if(type != null){
                                if(service.getType().equals(type)){
                                    if(scope != null){
                                        if(scope.equals(getServiceScope(service))){
                                            matchedServices.add(service);
                                        }
                                    }else{
                                        matchedServices.add(service);
                                        break;
                                    }
                                }
                            }else{
                                if(scope != null){
                                    if(scope.equals(getServiceScope(service))){
                                        matchedServices.add(service);
                                        break;
                                    }else{
                                        matchedServices.add(service);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }else{
            if (scope != null && scope.equals(SCOPE_APPLICATION)) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Need application name in order to list application-scoped services");
                return;
            }

            for (Service service : services.getServices()) {
                if(type != null){
                    if(service.getType().equals(type)){
                        if(scope != null){
                            if(scope.equals(getServiceScope(service))){
                                matchedServices.add(service);
                            }
                        }else{
                            matchedServices.add(service);
                        }
                    }
                }else{
                    if(scope != null){
                        if(scope.equals(getServiceScope(service))){
                            matchedServices.add(service);
                        }
                    }else{
                        matchedServices.add(service);
                    }
                }
            }
        }
        if (matchedServices.size() > 0) {

            String headings[] = new String[]{"SERVICE_NAME", "IP_ADDRESS", "VM_ID", "SERVER_TYPE", "STATE", "SCOPE"};
            if(type != null){
                if(scope != null){
                    headings = new String[]{"SERVICE_NAME", "IP_ADDRESS", "VM_ID", "STATE"};
                }else{
                    headings = new String[]{"SERVICE_NAME", "IP_ADDRESS", "VM_ID", "STATE", "SCOPE"};
                }
            }else{
                if(scope != null){
                    headings = new String[]{"SERVICE_NAME", "IP_ADDRESS", "VM_ID", "SERVER_TYPE", "STATE"};
                }
            }
            ColumnFormatter cf = new ColumnFormatter(headings);

            boolean foundRows = false;
            for (Service service : matchedServices) {
                foundRows = true;
                String cloudName = service.getServiceName();
                String ipAddress = service.getPropertyValue("ip-address");
                if (ipAddress == null) {
                    ipAddress = "-";
                }
                String instanceID = service.getPropertyValue("instance-id");
                if (instanceID == null) {
                    instanceID = "-";
                }
                String serverType = service.getType();

                String serviceType = null;
                String state = "-";
                if (service instanceof ApplicationScopedService) {
                    state = ((ApplicationScopedService) service).getState();
                    serviceType = SCOPE_APPLICATION;
                } else if (service instanceof SharedService) {
                    state = ((SharedService) service).getState();
                    serviceType = SCOPE_SHARED;
                } else if (service instanceof ExternalService) {
                    state = "-";
                    serviceType = SCOPE_EXTERNAL;
                }

                if(type == null){
                    if(scope == null){
                        cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state, serviceType});
                    }else{
                        if(serviceType.equals(scope)){
                            cf.addRow(new Object[]{cloudName, ipAddress, instanceID, serverType, state});
                        }
                    }
                }else{
                    if(scope == null){
                        cf.addRow(new Object[]{cloudName, ipAddress, instanceID, state, serviceType});
                    }else{
                        if(serviceType.equals(scope)){
                            cf.addRow(new Object[]{cloudName, ipAddress, instanceID, state});
                        }
                    }
                }
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
    }

    private String getServiceScope(Service service){
        String scope = null;
        if(service instanceof ApplicationScopedService){
            scope = SCOPE_APPLICATION;
        }else if(service instanceof SharedService){
            scope = SCOPE_SHARED;
        }else if(service instanceof ExternalService){
            scope = SCOPE_EXTERNAL;
        }
        return scope;
    }
}
