/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.microprofile.SetSecureMicroprofileConfigurationCommand;
import fish.payara.microprofile.metrics.impl.MetricsServiceImpl;

import java.util.logging.Logger;
import jakarta.inject.Inject;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import static org.glassfish.api.admin.RestEndpoint.OpType.POST;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.Target;
import org.glassfish.internal.config.UnprocessedConfigListener;
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
@ExecuteOn({RuntimeType.DAS})
@TargetType({DAS, DEPLOYMENT_GROUP, STANDALONE_INSTANCE, CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = MetricsServiceConfiguration.class,
            opType = POST,
            path = "set-metrics-configuration",
            description = "Sets the Metrics Configuration")
})
public class SetMetricsConfigurationCommand extends SetSecureMicroprofileConfigurationCommand {

    private static final Logger LOGGER = Logger.getLogger(SetMetricsConfigurationCommand.class.getName());

    @Inject
    private Target targetUtil;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Deprecated
    @Param(name = "secureMetrics", optional = true)
    private Boolean secure;

    @Param(name = "dynamic", optional = true)
    private Boolean dynamic;

    @Param(name = "endpoint", optional = true)
    private String endpoint;

    @Param(name = "virtualServers", optional = true)
    private String virtualServers;

    @Inject
    private Domain domain;

    @Inject
    UnprocessedConfigListener unprocessedListener;

    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();
        Subject subject = context.getSubject();
        Config targetConfig = targetUtil.getConfig(target);
        MetricsServiceConfiguration metricsConfiguration = targetConfig.getExtensionByType(MetricsServiceConfiguration.class);
        MetricsServiceImpl metricsService = Globals.getDefaultBaseServiceLocator().getService(MetricsServiceImpl.class);

        if (Boolean.TRUE.equals(securityEnabled)
                || Boolean.parseBoolean(metricsConfiguration.getSecurityEnabled())) {
            // Create the default user if it doesn't exist
            ActionReport checkUserReport = actionReport.addSubActionsReport();
            ActionReport createUserReport = actionReport.addSubActionsReport();
            if (!defaultMicroprofileUserExists(checkUserReport, subject) && !checkUserReport.hasFailures()) {
                createDefaultMicroprofileUser(createUserReport, subject);
            }
            if (checkUserReport.hasFailures() || createUserReport.hasFailures()) {
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        try {
            ConfigSupport.apply(configProxy -> {
                if (dynamic != null) {
                    configProxy.setDynamic(dynamic.toString());
                }
                if (enabled != null) {
                    configProxy.setEnabled(enabled.toString());
                    if(dynamic != null && dynamic || Boolean.valueOf(metricsConfiguration.getDynamic())) {
                        metricsService.resetMetricsEnabledProperty();
                    }
                }
                if (secure != null) {
                    actionReport.setMessage("--secureMetrics option is deprecated, replaced by --securityEnabled option.");
                    configProxy.setSecureMetrics(secure.toString());
                    if(dynamic != null && dynamic || Boolean.valueOf(metricsConfiguration.getDynamic())) {
                        metricsService.resetMetricsSecureProperty();
                    }
                }
                if (endpoint != null) {
                    configProxy.setEndpoint(endpoint);
                }
                if (virtualServers != null) {
                    configProxy.setVirtualServers(virtualServers);
                }
                if (securityEnabled != null) {
                    configProxy.setSecurityEnabled(securityEnabled.toString());
                }
                if (roles != null) {
                    configProxy.setRoles(roles);
                }

                actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return configProxy;
            }, metricsConfiguration);

            if(metricsConfiguration != null && !Boolean.valueOf(metricsConfiguration.getDynamic())) {
                actionReport.setMessage("Restart server for change to take effect");
            }
        } catch (TransactionFailure ex) {
            actionReport.failure(LOGGER, "Failed to update Metrics configuration", ex);
        }

        // If everything has passed, scrap the subaction reports as we don't want to print them out
        if (!actionReport.hasFailures() && !actionReport.hasWarnings()) {
            actionReport.getSubActionsReport().clear();
        }
    }

}
