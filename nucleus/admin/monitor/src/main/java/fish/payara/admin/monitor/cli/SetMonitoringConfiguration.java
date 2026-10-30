/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.admin.monitor.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;
import static org.glassfish.api.ActionReport.ExitCode.WARNING;

/**
 * @author Susan Rai
 */
@Service(name = "set-monitoring-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.monitoring.configuration")
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-monitoring-configuration",
            description = "This command can be used to configure all the monitoring related configuration")
})
public class SetMonitoringConfiguration implements AdminCommand {

    private static final Logger logger = Logger.getLogger(SetMonitoringConfiguration.class.getName());

    @Inject
    private Target targetUtil;

    @Param(optional = true, alias = "enabled")
    private Boolean monitoringServiceEnabled;

    @Param(optional = true, alias = "mbeanEnabled")
    private Boolean mbeansEnabled;

    @Param(optional = true)
    private Boolean dTraceEnabled;

    @Param(optional = true)
    private Boolean amxEnabled;

    @Param(optional = true)
    private Boolean jmxLogEnabled;

    @Param(optional = true)
    private String jmxLogFrequency;

    @Param(optional = true, acceptableValues = "NANOSECONDS,MILLISECONDS,SECONDS,MINUTES,HOURS,DAYS")
    private String jmxLogFrequencyUnit;

    @Param(name = "addattribute", optional = true, multiple = true, alias = "addproperty")
    private List<String> attributesToAdd;

    @Param(name = "delattribute", optional = true, multiple = true, alias = "delproperty")
    private List<String> attributesToRemove;

    @Param(optional = true)
    private Boolean restMonitoringEnabled;

    @Param(optional = true)
    private String restMonitoringApplicationName;

    @Param(optional = true)
    private String restMonitoringContextRoot;

    @Param(optional = true)
    private Boolean restMonitoringSecurityEnabled;

    @Param(optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Inject
    private CommandRunner commandRunner;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport actionReport = context.getActionReport();

        // obtain the correct configuration
        Config config = targetUtil.getConfig(target);
        MonitoringService monitorServiceConfig = config.getExtensionByType(MonitoringService.class);

        if (monitorServiceConfig != null) {
            try {
                ConfigSupport.apply((monitorServiceConfigProxy) -> {
                    if (monitoringServiceEnabled != null) {
                        monitorServiceConfigProxy.setMonitoringEnabled(String.valueOf(monitoringServiceEnabled));
                    }
                    if (mbeansEnabled != null) {
                        monitorServiceConfigProxy.setMbeanEnabled(String.valueOf(mbeansEnabled));
                    }
                    if (dTraceEnabled != null) {
                        monitorServiceConfigProxy.setMbeanEnabled(String.valueOf(dTraceEnabled));
                    }

                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return monitorServiceConfigProxy;
                }, monitorServiceConfig);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        CommandInvocation commandInvocation = commandRunner.getCommandInvocation("set-jmx-monitoring-configuration", actionReport, context.getSubject());
        ParameterMap commandParameters = new ParameterMap();

        if (jmxLogEnabled != null) {
            commandParameters.add("enabled", jmxLogEnabled.toString());
        }
        if (jmxLogFrequency != null) {
            commandParameters.add("logfrequency", jmxLogFrequency);
        }
        if (jmxLogFrequencyUnit != null) {
            commandParameters.add("logfrequencyunit", jmxLogFrequencyUnit);
        }
        if (attributesToAdd != null) {
            for (String attributeToAdd : attributesToAdd) {
                commandParameters.add("addattribute", attributeToAdd);
            }
        }
        if (attributesToRemove != null) {
            for (String attributeToRemove : attributesToRemove) {
                commandParameters.add("delattribute", attributeToRemove);
            }
        }
        if (dynamic) {
            commandParameters.add("dynamic", dynamic.toString());
        }
        commandParameters.add("target", target);
        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        if (actionReport.getActionExitCode() != SUCCESS && actionReport.getActionExitCode() != WARNING) {
            // Command couldn't be execute
            failureMessage(actionReport);
            return;
        }

        if (amxEnabled != null) {
            commandInvocation = commandRunner.getCommandInvocation("set-amx-enabled", actionReport, context.getSubject());
            commandParameters = new ParameterMap();
            commandParameters.add("enabled", amxEnabled.toString());
            commandParameters.add("dynamic", dynamic.toString());
            commandParameters.add("target", target);
            commandInvocation.parameters(commandParameters);
            commandInvocation.execute();

            if (actionReport.getActionExitCode() != SUCCESS && actionReport.getActionExitCode() != WARNING) {
                // Command couldn't be execute
                failureMessage(actionReport);
                return;
            }
        }

        commandInvocation = commandRunner.getCommandInvocation("set-rest-monitoring-configuration", actionReport, context.getSubject());
        commandParameters = new ParameterMap();

        if (restMonitoringEnabled != null) {
            commandParameters.add("enabled", restMonitoringEnabled.toString());
        }
        if (restMonitoringContextRoot != null) {
            commandParameters.add("contextroot", restMonitoringContextRoot);
        }
        if (restMonitoringApplicationName != null) {
            commandParameters.add("applicationname", restMonitoringApplicationName);
        }
        if (restMonitoringSecurityEnabled != null) {
            commandParameters.add("securityenabled", restMonitoringSecurityEnabled.toString());
        }

        commandParameters.add("target", target);
        commandInvocation.parameters(commandParameters);
        commandInvocation.execute();

        if (actionReport.getActionExitCode() != SUCCESS && actionReport.getActionExitCode() != WARNING) {
            // Command couldn't be execute
            failureMessage(actionReport);
            return;
        }

        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void failureMessage(ActionReport actionReport) {
        String message = "Failed to execute the command";
        logger.warning(message);
        actionReport.setActionExitCode(FAILURE);
        actionReport.setMessage(message);
    }

}
