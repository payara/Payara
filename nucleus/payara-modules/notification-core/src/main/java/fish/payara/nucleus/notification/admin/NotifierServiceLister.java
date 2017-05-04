/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


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
            
            Properties extrasProps = new Properties();
            ArrayList<String> names = new ArrayList<String>();
            for (ServiceHandle serviceHandle : allServiceHandles) {
                sb.append("\t" + serviceHandle.getActiveDescriptor().getName() + "\n");
                names.add(serviceHandle.getActiveDescriptor().getName());
            }
            extrasProps.put("avaliableServices", names);
            report.setMessage(sb.toString());
            report.setExtraProperties(extrasProps);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}