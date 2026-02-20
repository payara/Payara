/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) 2017-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.stuck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.admin.HealthCheckServiceConfigurer;
import fish.payara.nucleus.healthcheck.admin.SetHealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import fish.payara.nucleus.healthcheck.stuck.StuckThreadsHealthCheck;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.validation.constraints.Min;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
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
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * @since 4.1.2.173
 * @author jonathan coustick
 * @deprecated Replaced by {@link SetHealthCheckServiceConfiguration}
 */
@Deprecated
@Service(name = "healthcheck-stuckthreads-configure")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.stuckthreads.configure")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-stuckthreads-configure",
            description = "Configures the Stuck Threads Checker")
})
public class StuckThreadsConfigurer implements AdminCommand {

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
    private Boolean dynamic;
    
    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "unit", optional = true, acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String unit;
    
    @Param(name="threshold", optional=true)
    @Min(value = 1, message = "Threshold length must be 1 or more")
    private String threshold;
    
    @Param(name = "thresholdUnit", optional = true, acceptableValues = "DAYS,HOURS,MILLISECONDS,MINUTES,SECONDS")
    private String thresholdUnit;

    @Param(name = "checkerName", optional = true)
    private String checkerName;
    
    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;
    
    @Inject
    ServerEnvironment server;
    
    
    @Override
    public void execute(AdminCommandContext context) {
        Config config = targetUtil.getConfig(target);
        StuckThreadsHealthCheck service = habitat.getService(StuckThreadsHealthCheck.class);
        final ActionReport actionReport = context.getActionReport();
        if (service == null) {
            actionReport.appendMessage(strings.getLocalString("healthcheck.stuckthreads.configure.status.error",
                    "Stuck Threads Checker Service could not be found"));
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        try {
            HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
            StuckThreadsChecker stuckThreadConfiguration = healthCheckServiceConfiguration.getCheckerByType(StuckThreadsChecker.class);
            if (stuckThreadConfiguration == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        StuckThreadsChecker checkerProxy = healthCheckServiceConfigurationProxy.createChild(StuckThreadsChecker.class);
                        applyValues(checkerProxy);
                        healthCheckServiceConfigurationProxy.getCheckerList().add(checkerProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }
                }, healthCheckServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<StuckThreadsChecker>() {
                    @Override
                    public Object run(final StuckThreadsChecker hoggingThreadConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        applyValues(hoggingThreadConfigurationProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return hoggingThreadConfigurationProxy;
                    }
                }, stuckThreadConfiguration);
            }
            
            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        StuckThreadsChecker checkerByType = healthCheckServiceConfiguration.getCheckerByType(StuckThreadsChecker.class);
                        service.setOptions(service.constructOptions(checkerByType));
                        healthCheckService.registerCheck(checkerByType.getName(), service);
                        healthCheckService.reboot();
                    }
                } else {
                    // it implicitly targetted to us as we are not the DAS
                    // restart the service
                    StuckThreadsChecker checkerByType = healthCheckServiceConfiguration.getCheckerByType(StuckThreadsChecker.class);
                    service.setOptions(service.constructOptions(stuckThreadConfiguration));
                    healthCheckService.registerCheck(checkerByType.getName(), service);
                    healthCheckService.reboot();
                }
            }

        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

    }
    
    private void applyValues(StuckThreadsChecker checkerProxy) throws PropertyVetoException {
        if (enabled != null) {
            checkerProxy.setEnabled(enabled.toString());
        }
        
        // Take priority over deprecated parameter
        if (checkerName != null) {
            checkerProxy.setName(checkerName);
        }
        
        if (time != null) {
            checkerProxy.setTime(time);
        }
        
        if (unit != null) {
            checkerProxy.setUnit(unit);
        }
        
        if (threshold != null){
            checkerProxy.setThreshold(threshold);
        }
        
        if (thresholdUnit != null){
            checkerProxy.setThresholdTimeUnit(thresholdUnit);
        }
    }
    
}
