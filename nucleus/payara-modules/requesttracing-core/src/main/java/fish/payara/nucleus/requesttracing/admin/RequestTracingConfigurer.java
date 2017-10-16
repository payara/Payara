/*
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
package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.TimeUtil;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;
import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Admin command to enable/disable all request tracing services defined in
 * domain.xml.
 *
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "requesttracing-configure")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("requesttracing.configure")
@RestEndpoints({
    @RestEndpoint(configBean = RequestTracingServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "requesttracing-configure",
            description = "Enables/Disables Request Tracing Service")
})
public class RequestTracingConfigurer implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(RequestTracingConfigurer.class);

    @Inject
    ServerEnvironment server;

    @Inject
    RequestTracingService service;

    @Inject
    protected Logger logger;

    @Inject
    protected Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private Boolean dynamic;

    @Param(name = "sampleChance", optional = true)
    private Integer sampleChance;

    @Param(name = "thresholdUnit", optional = true)
    private String unit;

    @Param(name = "thresholdValue", optional = true)
    private String value;

    @Param(name = "historicalTraceEnabled", optional = true)
    private Boolean historicalTraceEnabled;

    @Param(name = "historicalTraceStoreSize", optional = true)
    private Integer historicalTraceStoreSize;

    @Param(name = "historicalTraceStoreTimeout", optional = true)
    private String historicalTraceStoreTimeout;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (!validate(actionReport)) {
            return;
        }

        Config config = targetUtil.getConfig(target);
        final RequestTracingServiceConfiguration requestTracingServiceConfiguration = config.getExtensionByType(RequestTracingServiceConfiguration.class);

        if (requestTracingServiceConfiguration != null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<RequestTracingServiceConfiguration>() {
                    @Override
                    public Object run(final RequestTracingServiceConfiguration requestTracingServiceConfigurationProxy) throws
                            PropertyVetoException, TransactionFailure {
                        if (enabled != null) {
                            requestTracingServiceConfigurationProxy.enabled(enabled.toString());
                        }
                        if (sampleChance != null) {
                            requestTracingServiceConfigurationProxy.setSampleChance(sampleChance.toString());
                        }
                        if (unit != null) {
                            requestTracingServiceConfigurationProxy.setThresholdUnit(unit);
                        }
                        if (value != null) {
                            requestTracingServiceConfigurationProxy.setThresholdValue(value);
                        }
                        if (historicalTraceEnabled != null) {
                            requestTracingServiceConfigurationProxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                        }
                        if (historicalTraceStoreSize != null) {
                            requestTracingServiceConfigurationProxy.setHistoricalTraceStoreSize(historicalTraceStoreSize.toString());
                        }
                        if (historicalTraceStoreTimeout != null) {
                            requestTracingServiceConfigurationProxy.setHistoricalTraceStoreTimeout(historicalTraceStoreTimeout.toString());
                        }

                        actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                        return requestTracingServiceConfigurationProxy;
                    }
                }, requestTracingServiceConfiguration);
            } catch (TransactionFailure ex) {
                logger.log(Level.WARNING, "Exception during command ", ex);
                actionReport.setMessage(ex.getCause().getMessage());
                actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (dynamic) {
            if (server.isDas()) {
                if (targetUtil.getConfig(target).isDas()) {
                    configureDynamically(actionReport);
                }
            } else {
                configureDynamically(actionReport);
            }
        }
    }

    private void configureDynamically(ActionReport actionReport) {
        service.getExecutionOptions().setEnabled(enabled);
        if (sampleChance != null) {
            service.getExecutionOptions().setSampleChance(Integer.valueOf(sampleChance));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.samplechance.success",
                    "Request Tracing Service Sample Chance is set to {0}.", sampleChance) + "\n");
        }
        if (value != null) {
            service.getExecutionOptions().setThresholdValue(Long.valueOf(value));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdvalue.success",
                    "Request Tracing Service Threshold Value is set to {0}.", value) + "\n");
        }
        if (unit != null) {
            service.getExecutionOptions().setThresholdUnit(TimeUnit.valueOf(unit));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdunit.success",
                    "Request Tracing Service Threshold Unit is set to {0}.", unit) + "\n");
        }

        if (historicalTraceEnabled != null) {
            service.getExecutionOptions().setHistoricalTraceEnabled(historicalTraceEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historicaltrace.status.success",
                    "Request Tracing Historical Trace status is set to {0}.", historicalTraceEnabled) + "\n");
        }

        if (historicalTraceStoreSize != null) {
            service.getExecutionOptions().setHistoricalTraceStoreSize(historicalTraceStoreSize);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historicaltrace.storesize.success",
                    "Request Tracing Historical Trace Store Size is set to {0}.", historicalTraceStoreSize) + "\n");
        }

        if (historicalTraceStoreTimeout != null) {
            service.getExecutionOptions().setHistoricalTraceTimeout(TimeUtil.setStoreTimeLimit(historicalTraceStoreTimeout));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historicaltrace.timeout.success",
                    "Request Tracing Historical Trace Store Timeout is set to {0}.", historicalTraceStoreTimeout) + "\n");
        }

        service.bootstrapRequestTracingService();
    }

    private boolean validate(ActionReport actionReport) {
        boolean result = false;
        if (value != null) {
            try {
                int thresholdValue = Integer.parseInt(value);
                if (thresholdValue <= 0 || thresholdValue > Short.MAX_VALUE * 2) {
                    actionReport.failure(logger, "Threshold Value must be greater than zero or less than " + Short.MAX_VALUE * 2 + 1);
                    return result;
                }
            } catch (NumberFormatException nfe) {
                actionReport.failure(logger, "Threshold Value is not a valid integer", nfe);
                return result;
            }

        }

        if (unit != null) {
            try {
                if (!unit.equals("NANOSECONDS")
                        && !unit.equals("MICROSECONDS")
                        && !unit.equals("MILLISECONDS")
                        && !unit.equals("SECONDS")
                        && !unit.equals("MINUTES")
                        && !unit.equals("HOURS")
                        && !unit.equals("DAYS")) {
                    actionReport.failure(logger, unit + " is an invalid time unit");
                    return result;
                }
            } catch (IllegalArgumentException iaf) {
                actionReport.failure(logger, unit + " is an invalid time unit", iaf);
                return result;
            }
        }

        return true;
    }
}
