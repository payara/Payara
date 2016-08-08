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
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.notification.service.BaseNotifierService;
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
 * @author mertcaliskan
 */
@Service(name = "notifier-list-services")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("notifier.list.services")
@ExecuteOn({RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "notifier-list-services",
                description = "Lists the names of all available notifier services")
})
public class NotifierServiceLister implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(NotifierServiceLister.class);

    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        List<ServiceHandle<BaseNotifierService>> allServiceHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        if (allServiceHandles.isEmpty()) {

            report.appendMessage(strings.getLocalString("notifier.list.services.warning",
                    "No registered notifier service found."));
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
        } else {
            StringBuffer sb = new StringBuffer();
            sb.append(strings.getLocalString("notifier.list.services.availability.info",
                    "Available Notifier Services") + ":\n");
            for (ServiceHandle serviceHandle : allServiceHandles) {
                sb.append("\t" + serviceHandle.getActiveDescriptor().getName() + "\n");
            }
            report.setMessage(sb.toString());
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}