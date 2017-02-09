/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.


 Copyright (c) 2016 Payara Foundation. All rights reserved.


 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin command to enable/disable all health check services defined in
 * domain.xml.
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-configure",
            description = "Enables/Disables Health Check Service")
})
public class HealthCheckConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckConfigurer.class);
    
    @Inject
    ServerEnvironment server;

    @Inject
    protected Logger logger;

    @Inject
    HealthCheckService service;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "historicalTraceEnabled", optional = true, defaultValue = "false")
    private Boolean historicalTraceEnabled;

    @Param(name = "historicalTraceStoreSize", optional = true, defaultValue = "20")
    private Integer historicalTraceStoreSize;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config config = targetUtil.getConfig(target);
        final HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
        if (healthCheckServiceConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (enabled != null) {
                            healthCheckServiceConfigurationProxy.enabled(enabled.toString());
                        }
                        if (historicalTraceEnabled != null) {
                            healthCheckServiceConfigurationProxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                        }
                        if (historicalTraceStoreSize != null) {
                            healthCheckServiceConfigurationProxy.setHistoricalTraceStoreSize(historicalTraceStoreSize.toString());
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }

                }, healthCheckServiceConfiguration);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (dynamic) {
            if (server.isDas()) {
                if (targetUtil.getConfig(target).isDas()) {
                    configureDynamically();
                }
            } else {
                // apply as not the DAS so implicitly it is for us
                configureDynamically();
            }
        }
    }

    private void configureDynamically() {
        service.setEnabled(enabled);

        if (historicalTraceEnabled != null) {
            service.setHistoricalTraceEnabled(historicalTraceEnabled);
        }
        if (historicalTraceStoreSize != null) {
            service.setHistoricalTraceStoreSize(historicalTraceStoreSize);
        }
    }
}