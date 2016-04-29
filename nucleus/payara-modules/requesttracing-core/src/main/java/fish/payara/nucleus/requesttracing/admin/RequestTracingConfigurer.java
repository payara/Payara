/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Admin command to enable/disable all request tracing services defined in domain.xml.
 *
 * @author mertcaliskan
 */
@Service(name = "requesttracing-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("requesttracing.configure")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "requesttracing-configure",
            description = "Enables/Disables Request Tracing Service")
})
public class RequestTracingConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(RequestTracingConfigurer.class);

    @Inject
    RequestTracingService service;

    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        Config config = targetUtil.getConfig(target);
        final RequestTracingServiceConfiguration requestTracingServiceConfiguration = config.getExtensionByType(RequestTracingServiceConfiguration.class);

        if (requestTracingServiceConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<RequestTracingServiceConfiguration>() {
                    @Override
                    public Object run(final RequestTracingServiceConfiguration requestTracingServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (enabled != null) {
                            requestTracingServiceConfigurationProxy.enabled(enabled.toString());
                        }
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return requestTracingServiceConfigurationProxy;
                    }
                }, requestTracingServiceConfiguration);
            }
            catch(TransactionFailure ex){
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (dynamic) {
            enableOnTarget(actionReport, context, enabled);
        }
    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        service.getExecutionOptions().setEnabled(enabled);
        actionReport.appendMessage(strings.getLocalString("requesttracing.configure.status.success",
                "Request Tracing Service status is set to {0}.", enabled));
    }
}