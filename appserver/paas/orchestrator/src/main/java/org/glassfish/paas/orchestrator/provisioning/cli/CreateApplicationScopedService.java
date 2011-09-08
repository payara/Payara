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

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;

@org.jvnet.hk2.annotations.Service(name = "_create-application-scoped-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
public class CreateApplicationScopedService implements AdminCommand {

    @Param(name = "servicetype", optional = false)
    private String serviceType;

    @Param(name = "property", optional = true, separator = ':')
    private Properties properties;

    @Param(name = "template", optional = false)
    private String template;

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Param(name = "appname", optional = false)
    private String appName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

/*
        Applications applications = domain.getApplications();
        if (applications != null) {
            Application app = applications.getApplication(appName);
            if (app != null) {
                Services services = app.getExtensionByType(Services.class);
                if (services == null) {
                    try {
                        if (ConfigSupport.apply(new SingleConfigCode<Application>() {
                            public Object run(Application param) throws PropertyVetoException, TransactionFailure {
                                Services services = param.createChild(Services.class);
                                param.getExtensions().add(services);
                                return services;
                            }
                        }, app) == null) {
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            report.setMessage("Unable to create service");
                            return;
                        }
                    } catch (TransactionFailure transactionFailure) {
                        //TODO log
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Unable to create external service due to : " + transactionFailure.getMessage());
                        return;
                    }
                }

                services = app.getExtensionByType(Services.class);
                for (org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
                    if (service.getServiceName().equals(serviceName)) {
                        //TODO log
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Service by name [" + serviceName + "] already exists");
                        return;
                    }
                }

                try {
                    if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                        public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                            org.glassfish.paas.orchestrator.config.Service service =
                                    param.createChild(org.glassfish.paas.orchestrator.config.Service.class);
                            service.setType(serviceType);
                            service.setServiceName(serviceName);
                            if (properties != null) {
                                for (Map.Entry e : properties.entrySet()) {
                                    Property prop = service.createChild(Property.class);
                                    prop.setName((String) e.getKey());
                                    prop.setValue((String) e.getValue());
                                    service.getProperty().add(prop);
                                }
                            }
                            param.getServices().add(service);
                            return service;
                        }
                    }, services) == null) {
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Unable to create service");
                        return;
                    } else {
                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return;
                    }
                } catch (TransactionFailure transactionFailure) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("Unable to create service due to : " + transactionFailure.getMessage());
                    return;
                }
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed in the server");
                return;
            }
        } else {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such application [" + appName + "] is deployed in the server");
            return;
        }*/

        Services services = serviceUtil.getServices();

        for (Service service : services.getServices()) {
            if (service.getServiceName().equals(serviceName)) {
                if (service instanceof ApplicationScopedService) {
                    ApplicationScopedService appScopedService = (ApplicationScopedService) service;
                    if (appName.equals(appScopedService.getApplicationName())) {
                        //TODO log
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("Application scoped service by name [" + serviceName + "] " +
                                "already exists for application ["+appName+"]");
                        return;
                    }
                }
            }
        }

            try {
                if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                    public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                        ApplicationScopedService appScopedService = param.createChild(ApplicationScopedService.class);
                        appScopedService.setType(serviceType);
                        appScopedService.setServiceName(serviceName);
                        appScopedService.setApplicationName(appName);
                        appScopedService.setTemplate(template);
                        if (properties != null) {
                            for (Map.Entry e : properties.entrySet()) {
                                Property prop = appScopedService.createChild(Property.class);
                                prop.setName((String) e.getKey());
                                prop.setValue((String) e.getValue());
                                appScopedService.getProperty().add(prop);
                            }
                        }
                        param.getServices().add(appScopedService);
                        return appScopedService;
                    }
                }, services) == null) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("Unable to create application scoped service");
                    return;
                } else {
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return;
                }
            } catch (TransactionFailure transactionFailure) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Unable to create application scoped service due to : " + transactionFailure.getMessage());
                return;
            }
        }
    }
