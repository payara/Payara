/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;


/**
 * Admin command to enable/disable all health check services defined in domain.xml.
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "healthcheck-configure",
            description = "Enables/Disables Health Check Service")
})
public class HealthCheckConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckConfigurer.class);

    @Inject
    HealthCheckService service;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Override
    public void execute(AdminCommandContext context) {
        
        final ActionReport actionReport = context.getActionReport();
        service.setEnabled(enabled);
        service.bootstrapHealthCheck();
        actionReport.appendMessage(strings.getLocalString("healthcheck.configure.status.success",
                "Health Check Service status is set to {0}.", enabled));
    }
}
