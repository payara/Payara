/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.metrics.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.appserver.microprofile.service.admin.SetMicroProfileConfigurationCommand;
import fish.payara.microprofile.metrics.MetricsService;
import fish.payara.microprofile.metrics.service.MetricsAuthModule;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CLUSTERED_INSTANCE;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * AsAdmin command to set metrics configuration
 * 
 * @author Gaurav Gupta
 */
@Service(name = "set-metrics-configuration")
@PerLookup
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({DAS, DEPLOYMENT_GROUP, STANDALONE_INSTANCE, CLUSTER, CLUSTERED_INSTANCE, CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = MetricsServiceConfiguration.class,
            opType = POST,
            path = "set-metrics-configuration",
            description = "Sets the Metrics Configuration")
})
public class SetMetricsConfigurationCommand extends SetMicroProfileConfigurationCommand {

    private static final Logger LOGGER = Logger.getLogger(SetMetricsConfigurationCommand.class.getName());
    
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Subject subject = context.getSubject();

        Config targetConfig = targetUtil.getConfig(target);
        MetricsServiceConfiguration metricsConfiguration = targetConfig.getExtensionByType(MetricsServiceConfiguration.class);
        MetricsService metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsService.class);

        // If the required message security provider is not present, create it
        if (!messageSecurityProviderExists(actionReport.addSubActionsReport(), 
                context.getSubject(), MetricsAuthModule.class)) {
            createRequiredMessageSecurityProvider(
                    actionReport.addSubActionsReport(),
                    subject,
                    MetricsAuthModule.class
            );
        }
        
        // Create the default user if it doesn't exist
        if (!defaultMicroProfileUserExists(actionReport.addSubActionsReport(), subject)) {
            createDefaultMicroProfileUser(actionReport.addSubActionsReport(), subject);
        }

        try {
            ConfigSupport.apply(configProxy -> {
                boolean restart = false;
                if (dynamic != null) {
                    configProxy.setDynamic(dynamic.toString());
                }
                if (enabled != null) {
                    configProxy.setEnabled(enabled.toString());
                    if ((dynamic != null && dynamic)
                            || Boolean.valueOf(metricsConfiguration.getDynamic())) {
                        metricsService.setSecurityEnabled(enabled);
                    } else {
                        restart = true;
                    }
                }
                if (securityEnabled != null) {
                    configProxy.setSecurityEnabled(securityEnabled.toString());
                    if ((dynamic != null && dynamic)
                            || Boolean.valueOf(metricsConfiguration.getDynamic())) {
                        metricsService.setMetricsSecure(securityEnabled);
                    } else {
                        restart = true;
                    }
                }
                if (endpoint != null) {
                    configProxy.setEndpoint(endpoint);
                    restart = true;
                }
                if (virtualServers != null) {
                    configProxy.setVirtualServers(virtualServers);
                    restart = true;
                }
                if (applicationName != null) {
                    configProxy.setApplicationName(applicationName);
                    restart = true;
                }

                if (restart) {
                    actionReport.setMessage("Restart server for change to take effect");
                }
                return configProxy;
            }, metricsConfiguration);
        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update Metrics configuration", ex);
        }
        
        // If everything has passed, scrap the subaction reports as we don't want to print them out
        if (!actionReport.hasFailures() || !actionReport.hasWarnings()) {
            actionReport.getSubActionsReport().clear();
        }
    }

}
