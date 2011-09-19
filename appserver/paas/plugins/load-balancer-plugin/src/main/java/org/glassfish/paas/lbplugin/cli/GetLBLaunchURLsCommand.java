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
package org.glassfish.paas.lbplugin.cli;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.deployment.common.DeploymentProperties;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PerLookup;
import com.sun.enterprise.config.serverbeans.*;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.paas.lbplugin.Constants;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

@org.jvnet.hk2.annotations.Service(name = "_get-lb-launch-urls")
@ExecuteOn(value = {RuntimeType.DAS})
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.GET, path = "_get-lb-launch-urls", description = "Get Urls for launch the application via LB")
})
public class GetLBLaunchURLsCommand implements AdminCommand {

    @Param(name = "appname", primary = true)
    private String appName = null;

    @Inject
    Domain domain;

    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        String contextRoot = getContextRoot(appName);

        ApplicationScopedService appScopedService =
                getService(ServiceType.LOAD_BALANCER.name(), appName);
        if(appScopedService == null){
            return;
        }
        ActionReport.MessagePart part = report.getTopMessagePart();
        part.setMessage("A");
        int j = 0;
        ActionReport.MessagePart childPart = part.addChild();
        childPart.setMessage(Integer.toString(j++));
        childPart.addProperty(DeploymentProperties.PROTOCOL,
                Constants.HTTP_PROTOCOL);
        childPart.addProperty(DeploymentProperties.HOST,
                appScopedService.getPropertyValue(Constants.IP_ADDRESS_PROP_NAME));
        childPart.addProperty(DeploymentProperties.PORT,
                Constants.DEFAULT_HTTP_PORT);
        childPart.addProperty(DeploymentProperties.CONTEXT_PATH,
                contextRoot);
    }

    private ApplicationScopedService getService(String serviceType, String appName) {
        Services services = getServices();
        for (Service service : services.getServices()) {
            if (service instanceof ApplicationScopedService) {
                ApplicationScopedService appScopedService =
                        (ApplicationScopedService) service;
                if (appName.equals(appScopedService.getApplicationName())
                        && serviceType.equalsIgnoreCase(appScopedService.getType())) {
                    return appScopedService;
                }
            }
        }
        return null;
    }

    private Services getServices() {
        Services services = domain.getExtensionByType(Services.class);
        if (services == null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Domain>() {

                    public Object run(Domain param) throws PropertyVetoException, TransactionFailure {
                        Services services = param.createChild(Services.class);
                        param.getExtensions().add(services);
                        return services;
                    }
                }, domain) == null) {
                    LBPluginLogger.getLogger().log(Level.SEVERE, "Unable to create 'services' config");
                }
            } catch (TransactionFailure transactionFailure) {
                LBPluginLogger.getLogger().log(Level.SEVERE, "Unable to create 'services' config due to : " + transactionFailure.getMessage());
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        }

        services = domain.getExtensionByType(Services.class);
        return services;
    }

    private String getContextRoot(String appName) {
        Application application = domain.getApplications().getApplication(appName);
        String contextRoot = application.getContextRoot();
        // non standalone war cases
        if (contextRoot == null) {
            contextRoot = "";
        }
        return contextRoot;
    }
}
