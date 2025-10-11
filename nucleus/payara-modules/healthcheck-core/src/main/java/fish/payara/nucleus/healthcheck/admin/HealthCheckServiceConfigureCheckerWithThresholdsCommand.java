/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *     Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

/**
 * Helper command that combines the HealthCheckServiceConfigurer and HealthCheckServiceThresholdsConfigurer commands.
 * @since 4.1.1.171
 * @author Andrew Pielage
 * @deprecated Replaced by {@link SetHealthCheckServiceConfiguration}
 */
@Deprecated
@Service(name = "healthcheck-service-configure-checker-with-thresholds")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.service.configure.checker.with.thresholds")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE,
        CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-service-configure-checker-with-thresholds",
            description = "Configures a Heakthcheck Service Checker with Thresholds")
})
public class HealthCheckServiceConfigureCheckerWithThresholdsCommand implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceConfigurer.class);

    @Inject
    ServiceLocator habitat;

    @Inject
    protected Target targetUtil;

    @Inject
    protected Logger logger;

    @Inject
    HealthCheckService healthCheckService;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;
    
    @Param(name = "addToMicroProfileHealth", optional = true, defaultValue = "false")
    private Boolean addToMicroProfileHealth;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "unit", optional = true,
            acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String unit;
    
    @Param(name = "checkerName", optional = true)
    private String checkerName;
    
    @Param(name = "serviceName", optional = false, 
            acceptableValues = "healthcheck-cpu,healthcheck-gc,healthcheck-cpool,healthcheck-heap,healthcheck-threads,"
                    + "healthcheck-machinemem, healthcheck-stuck")
    private String serviceName;

    @Param(name = "thresholdCritical", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdCritical;

    @Param(name = "thresholdWarning", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdWarning;

    @Param(name = "thresholdGood", optional = true)
    @Min(value = 0, message = "Threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Threshold is a percentage so must be less than 100")
    private String thresholdGood;
    
    @Inject
    ServerEnvironment server;
    
    @Inject
    CommandRunner commandRunner;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport mainActionReport = context.getActionReport();
        final ActionReport checkerConfigureReport = mainActionReport.addSubActionsReport();
        
        // Configure the checker with the provided parameters
        CommandRunner.CommandInvocation checkerConfigureInvocation = commandRunner.getCommandInvocation(
                "healthcheck-configure-service", checkerConfigureReport, context.getSubject());
        
        ParameterMap checkerConfigureParameters = new ParameterMap();
        checkerConfigureParameters.add("enabled", enabled.toString());
        checkerConfigureParameters.add("dynamic", dynamic.toString());
        checkerConfigureParameters.add("addToMicroProfileHealth", addToMicroProfileHealth.toString());
        checkerConfigureParameters.add("time", time);
        checkerConfigureParameters.add("unit", unit);
        checkerConfigureParameters.add("checkerName", checkerName);
        checkerConfigureParameters.add("serviceName", serviceName);
        checkerConfigureParameters.add("target", target);
        
        checkerConfigureInvocation.parameters(checkerConfigureParameters);
        checkerConfigureInvocation.execute();
        
        // Only carry on if the first command succeeds
        if (checkerConfigureReport.hasFailures()) {
            mainActionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            checkerConfigureReport.setMessage("Failed to configure checker");
        } else {
            final ActionReport thresholdConfigureReport = mainActionReport.addSubActionsReport();
            
            // Configure the checker thresholds
            CommandRunner.CommandInvocation thresholdConfigureInvocation = commandRunner.getCommandInvocation(
                "healthcheck-configure-service-threshold", thresholdConfigureReport, context.getSubject());
        
            ParameterMap thresholdConfigureParameters = new ParameterMap();
            thresholdConfigureParameters.add("dynamic", dynamic.toString());
            thresholdConfigureParameters.add("serviceName", serviceName);
            thresholdConfigureParameters.add("target", target);
            thresholdConfigureParameters.add("thresholdCritical", thresholdCritical);
            thresholdConfigureParameters.add("thresholdWarning", thresholdWarning);
            thresholdConfigureParameters.add("thresholdGood", thresholdGood);
            
            thresholdConfigureInvocation.parameters(thresholdConfigureParameters);
            thresholdConfigureInvocation.execute();
            
            // Check the command result to determine if the whole command succeeds or fails
            if (thresholdConfigureReport.hasFailures()) {
                mainActionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                checkerConfigureReport.setMessage("Failed to configure thresholds");
            }
        }
    }
}
