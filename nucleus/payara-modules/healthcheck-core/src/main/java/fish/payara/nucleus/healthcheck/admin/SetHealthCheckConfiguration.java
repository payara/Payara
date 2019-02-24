/*
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.validation.constraints.Min;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.TimeUtil;
import fish.payara.nucleus.notification.configuration.NotifierType;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;

/**
 * Service to configure the {@link HealthCheckServiceConfiguration} of the {@link Target}.
 *
 * Updating the service configuration also updates the {@link LogNotifierConfiguration} accordingly.
 *
 * @author Jan Bernitt
 * @since 5.191
 */
@Service(name = "set-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-configuration",
            description = "Enables/Disables Health Check Service")
})
public class SetHealthCheckConfiguration implements AdminCommand {

    @Inject
    ServerEnvironment server;

    @Inject
    private Logger logger;

    @Inject
    HealthCheckService healthCheck;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    private Target targetUtil;
    private Config targetConfig;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    private String target;

    @Param(name = "enabled")
    private boolean enabled;

    @Param(name = "historical-trace-enabled", optional = true)
    private Boolean historicalTraceEnabled;

    @Param(name = "historical-trace-store-size", optional = true, defaultValue = "20")
    @Min(value = 1, message = "Store size must be greater than 0")
    private int historicalTraceStoreSize;

    @Param(name = "historical-trace-store-timeout", optional = true)
    private String historicalTraceStoreTimeout;

    @Override
    public void execute(AdminCommandContext context) {
        targetConfig = targetUtil.getConfig(target);
        HealthCheckServiceConfiguration config = targetConfig.getExtensionByType(HealthCheckServiceConfiguration.class);
        if (config != null) {
            updateConfig(config, context);
        }
        if (dynamic && (!server.isDas() || targetConfig.isDas())) {
            configureDynamically();
        }
        updateLogNotifier(context);
    }

    private static ActionReport initActionReport(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        if (report.getExtraProperties() == null) {
            report.setExtraProperties(new Properties());
        }
        return report;
    }

    private void updateConfig(HealthCheckServiceConfiguration config, AdminCommandContext context) {
        final ActionReport report = initActionReport(context);
        try {
            ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                @Override
                public Object run(HealthCheckServiceConfiguration configProxy)
                        throws PropertyVetoException, TransactionFailure {
                                configProxy.enabled(String.valueOf(enabled));
                                configProxy.setHistoricalTraceStoreSize(String.valueOf(historicalTraceStoreSize));
                                if (historicalTraceEnabled != null) {
                                    configProxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                                }
                                if (historicalTraceStoreTimeout != null) {
                                    configProxy.setHistoricalTraceStoreTimeout(historicalTraceStoreTimeout.toString());
                                }
                                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                return configProxy;
                            }
            }, config);
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            report.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

    private void updateLogNotifier(AdminCommandContext context) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation inv = runner.getCommandInvocation("set-healthcheck-service-notifier-configuration", subReport, context.getSubject());
        ParameterMap params = new ParameterMap();
        params.add("target", target);
        params.add("dynamic", String.valueOf(dynamic));
        params.add("enabled", String.valueOf(enabled));
        params.add("notifier", NotifierType.LOG.name().toLowerCase());
        // noisy will default to the notifier's config when not set so we do not set it as this is what we want
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void configureDynamically() {
        healthCheck.setEnabled(enabled);
        healthCheck.setHistoricalTraceStoreSize(historicalTraceStoreSize);
        if (historicalTraceEnabled != null) {
            healthCheck.setHistoricalTraceEnabled(historicalTraceEnabled);
        }
        if (historicalTraceStoreTimeout != null) {
            healthCheck.setHistoricalTraceStoreTimeout(TimeUtil.setStoreTimeLimit(this.historicalTraceStoreTimeout));
        }
    }
}
