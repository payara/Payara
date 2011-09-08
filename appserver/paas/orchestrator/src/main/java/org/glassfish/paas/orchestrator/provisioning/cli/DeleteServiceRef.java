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
import org.glassfish.paas.orchestrator.config.ServiceRef;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;

@org.jvnet.hk2.annotations.Service(name = "_delete-service-ref")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
public class DeleteServiceRef implements AdminCommand {

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Param(optional = false)
    private String appName;

    @Inject
    private ServiceUtil serviceUtil;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

/*
        //old code that assumes that <service-ref> is within <application>

        Applications applications = domain.getApplications();
        if (applications != null) {
            Application app = applications.getApplication(appName);
            if (app != null) {
                Services services = app.getExtensionByType(Services.class);
                if(services != null){
                    boolean foundService = false;
                    for (final ServiceRef serviceRef : services.getServiceRefs()) {
                        if(serviceRef.getServiceName().equals(serviceName)){
                            foundService = true;
                            try {
                                if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                                    public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                                        param.getServiceRefs().remove(serviceRef);
                                        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                        return serviceRef;
                                    }
                                }, services) == null) {
                                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                    report.setMessage("Deleting service-ref [" + serviceName + "] failed " );
                                    return;
                                }
                            } catch (TransactionFailure transactionFailure) {
                                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                report.setMessage("Deleting service-ref [" + serviceName + "] failed : " + transactionFailure.getMessage());
                                return;
                            }
                            break;
                        }
                    }
                    if(!foundService){
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setMessage("No such service-ref [" + serviceName + "] is part of the application ["+appName+"]");
                        return;
                    }

                }else{
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setMessage("No such service-ref [" + serviceName + "] is part of the application ["+appName+"]");
                    return;
                }
            }else{
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such application [" + appName + "] is deployed in the server");
                return;
            }
        }else{
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such application [" + appName + "] is deployed in the server");
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }*/

        Services services = serviceUtil.getServices();
        if (services != null) {
            boolean foundServiceRef = false;
            for (final ServiceRef serviceRef : services.getServiceRefs()) {
                if (serviceRef.getServiceName().equals(serviceName)) {
                    if (appName.equals(serviceRef.getApplicationName())) {
                        foundServiceRef = true;
                        try {
                            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                                public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                                    param.getServiceRefs().remove(serviceRef);
                                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                    return serviceRef;
                                }
                            }, services) == null) {
                                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                report.setMessage("Deleting serviceRef [" + serviceName + "] failed ");
                                return;
                            }
                        } catch (TransactionFailure transactionFailure) {
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            report.setMessage("Deleting serviceRef [" + serviceName + "] failed : " + transactionFailure.getMessage());
                        }
                        break;
                    }
                }
            }
            if (!foundServiceRef) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("No such service-ref [" + serviceName + "] is part of the application [" + appName + "]");
                return;
            }

        } else {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such service-ref [" + serviceName + "] is part of the application [" + appName + "]");
            return;
        }
    }
}
