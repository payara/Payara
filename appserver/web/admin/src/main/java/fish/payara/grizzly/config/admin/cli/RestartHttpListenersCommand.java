/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020-2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.grizzly.config.admin.cli;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.NetworkListeners;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.v3.services.impl.GrizzlyService;

@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@Service(name = "restart-http-listeners")
@PerLookup
@RestEndpoints({
    @RestEndpoint(configBean = NetworkListeners.class,
            opType = RestEndpoint.OpType.POST,
            description = "Restarts all HTTP listeners")
})
public class RestartHttpListenersCommand implements AdminCommand {

    @Inject
    private Domain domain;

    @Inject
    private ServiceLocator locator;

    @Inject
    private Target targetUtil;

    @Inject
    private GrizzlyService service;

    @Param(name = "target", optional = true, primary = true)
    private String target;

    @Param(name = "all", optional = true)
    private Boolean all;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        boolean isAll = all != null && all.booleanValue();
        if (isAll && target == null) {
            ExitCode exitCode = ClusterOperationUtil.replicateCommand("restart-http-listeners", FailurePolicy.Ignore, FailurePolicy.Ignore,
                        FailurePolicy.Error, domain.getAllTargets(), context, new ParameterMap(), locator);
            report.setActionExitCode(exitCode);
        }
        if (report.hasFailures()) {
            return;
        }
        if (target != null && isAll) {
            report.setMessage("--all used together with --target and is ignored.");
            report.setActionExitCode(ExitCode.WARNING);
        }
        if (target == null) {
            target = SystemPropertyConstants.DAS_SERVER_NAME;
        }

        Config config = targetUtil.getConfig(target);
        for (NetworkListener listener : config.getNetworkConfig().getNetworkListeners().getNetworkListener()) {
            try {
                if (!"admin-listener".equals(listener.getName())) {
                    service.restartNetworkListener(listener, 10, TimeUnit.SECONDS);
                }
            } catch (Exception ex) {
                report.setMessage(MessageFormat.format("Failed to restart listener {0}, {1}", listener.getName(),
                        (ex.getMessage() == null ? "No reason given" : ex.getMessage())));
                report.setActionExitCode(ExitCode.FAILURE);
                report.setFailureCause(ex);
                return;
            }
            if (!report.hasFailures() && !report.hasWarnings()) {
                report.setActionExitCode(ExitCode.SUCCESS);
            }
        }
    }

}
