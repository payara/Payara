/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.util.ColumnFormatter;
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

import jakarta.inject.Inject;
import java.util.List;

/**
 * Admin command to list the names of all available health check services
 *
 * @author mertcaliskan
 * @deprecated replaced by {@link ListHealthCheckServices}
 */
@Deprecated
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
    final static String[] serviceHeaders = {"Name", "Description"};

    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {

        ColumnFormatter serviceListerColumnFormatter = new ColumnFormatter(serviceHeaders);

        final ActionReport report = context.getActionReport();
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);

        if (allServiceHandles.isEmpty()) {
            report.appendMessage(strings.getLocalString("healthcheck.list.services.warning",
                    "No registered health check service found."));
            report.setActionExitCode(ActionReport.ExitCode.WARNING);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(strings.getLocalString("healthcheck.list.services.availability.info",
                    "Available Health Check Services") + ":\n");
            for (ServiceHandle<BaseHealthCheck> serviceHandle : allServiceHandles) {

                Object values[] = new Object[2];
                values[0] = serviceHandle.getActiveDescriptor().getName();
                values[1] = serviceHandle.getService().resolveDescription();
                serviceListerColumnFormatter.addRow(values);
            }

            sb.append(serviceListerColumnFormatter.toString());
            report.setMessage(sb.toString());
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}
