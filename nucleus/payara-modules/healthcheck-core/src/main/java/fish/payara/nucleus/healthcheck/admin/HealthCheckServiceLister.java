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

import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to list the names of all available health check services
 *
 * @author mertcaliskan
 */
@Service(name = "healthcheck-list-services")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.list.services")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "healthcheck-list-services",
                description = "Lists the names of all available health check services")
})
public class HealthCheckServiceLister implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceLister.class);

    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);

        if (allServiceHandles.isEmpty()) {

            report.appendMessage(strings.getLocalString("healthcheck.list.services.warning",
                    "No registered health check service found."));
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(strings.getLocalString("healthcheck.list.services.availability.info",
                    "Available Health Check Services") + ":\n");
            for (ServiceHandle serviceHandle : allServiceHandles) {
                sb.append("\t" + serviceHandle.getActiveDescriptor().getName() + "\n");
            }
            report.setMessage(sb.toString());
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}
