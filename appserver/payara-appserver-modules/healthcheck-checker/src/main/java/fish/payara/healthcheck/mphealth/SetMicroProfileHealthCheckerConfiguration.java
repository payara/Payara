/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.healthcheck.mphealth;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileHealthCheckerConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.validation.constraints.Min;
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
 * Configure the Microprofile Healthcheck Checker
 * @author jonathan coustick
 * @since 5.184
 */
@Service(name = "set-healthcheck-microprofile-health-checker-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.mphealthecheck.configure")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "healthcheck-mp-configure",
            description = "Configures the Microprofile Healthcheck Checker")
})
public class SetMicroProfileHealthCheckerConfiguration implements AdminCommand {
    
    private static final Logger LOGGER = Logger.getLogger("HEALTHCHECK-MP");
    
    @Inject
    ServiceLocator habitat;

    @Inject
    protected Target targetUtil;
    
    @Inject
    HealthCheckService healthCheckService;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "unit", optional = true, acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String unit;
    
    @Param(name = "checkerName", optional = true)
    private String checkerName;
    
    @Param(name = "timeout", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String timeout;
    
    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Inject
    ServerEnvironment server;
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
                
        Config config = targetUtil.getConfig(target);
        MicroProfileHealthChecker service = habitat.getService(MicroProfileHealthChecker.class);
        if (service == null) {
            actionReport.appendMessage("Microprofile Healthcheck Checker Service could not be found");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        try {
            HealthCheckServiceConfiguration healthCheckServiceConfiguration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
            MicroProfileHealthCheckerConfiguration healthCheckerConfiguration = 
                    healthCheckServiceConfiguration.getCheckerByType(MicroProfileHealthCheckerConfiguration.class);
            if (healthCheckerConfiguration == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(final HealthCheckServiceConfiguration healthCheckServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        MicroProfileHealthCheckerConfiguration checkerProxy = 
                                healthCheckServiceConfigurationProxy.createChild(MicroProfileHealthCheckerConfiguration.class);
                        applyValues(checkerProxy);
                        healthCheckServiceConfigurationProxy.getCheckerList().add(checkerProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return healthCheckServiceConfigurationProxy;
                    }
                }, healthCheckServiceConfiguration);
            } else {
                ConfigSupport.apply(new SingleConfigCode<MicroProfileHealthCheckerConfiguration>() {
                    @Override
                    public Object run(final MicroProfileHealthCheckerConfiguration hoggingThreadConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        applyValues(hoggingThreadConfigurationProxy);
                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return hoggingThreadConfigurationProxy;
                    }
                }, healthCheckerConfiguration);
            }
            
            if (dynamic) {
                if (server.isDas()) {
                    if (targetUtil.getConfig(target).isDas()) {
                        MicroProfileHealthCheckerConfiguration checkerByType = 
                                healthCheckServiceConfiguration.getCheckerByType(MicroProfileHealthCheckerConfiguration.class);
                        service.setOptions(service.constructOptions(checkerByType));
                        healthCheckService.registerCheck(checkerByType.getName(), service);
                        healthCheckService.reboot();
                    }
                } else {
                    // it implicitly targetted to us as we are not the DAS
                    // restart the service
                    MicroProfileHealthCheckerConfiguration checkerByType = 
                            healthCheckServiceConfiguration.getCheckerByType(MicroProfileHealthCheckerConfiguration.class);
                    service.setOptions(service.constructOptions(healthCheckerConfiguration));
                    healthCheckService.registerCheck(checkerByType.getName(), service);
                    healthCheckService.reboot();
                }
            }

        } catch (TransactionFailure ex) {
            LOGGER.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
    
    
    private void applyValues(MicroProfileHealthCheckerConfiguration checkerProxy) throws PropertyVetoException {
        if (enabled != null) {
            checkerProxy.setEnabled(enabled.toString());
        }
        
        if (checkerName != null) {
            checkerProxy.setName(checkerName);
        }
        
        if (time != null) {
            checkerProxy.setTime(time);
        }
        
        if (unit != null) {
            checkerProxy.setUnit(unit);
        }
        
        if (timeout != null) {
            checkerProxy.setTimeout(timeout);
        }
        
    }
}
