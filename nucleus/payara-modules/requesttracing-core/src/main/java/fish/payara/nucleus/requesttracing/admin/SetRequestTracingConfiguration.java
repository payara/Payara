/*
 * Copyright (c) 2016-2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.admin;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
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

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.internal.notification.NotifierUtils;
import fish.payara.internal.notification.TimeUtil;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;

/**
 * Admin command to enable/disable all request tracing services defined in
 * domain.xml.
 *
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CONFIG})
@Service(name = "set-requesttracing-configuration")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set-requesttracing-configuration")
@RestEndpoints({
    @RestEndpoint(configBean = RequestTracingServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            description = "Configures the Request Tracing Service")
})
public class SetRequestTracingConfiguration implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(SetRequestTracingConfiguration.class);
    private static final LocalStringsImpl STRINGS = new LocalStringsImpl(SetRequestTracingConfiguration.class);

    @Inject
    private ServiceLocator serviceLocator;

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

    @Param(name = "sampleRate", optional = true)
    private String sampleRate;

    @Param(name = "adaptiveSamplingEnabled", optional = true)
    private Boolean adaptiveSamplingEnabled;

    @Param(name = "adaptiveSamplingTargetCount", optional = true)
    private String adaptiveSamplingTargetCount;

    @Param(name = "adaptiveSamplingTimeValue", optional = true)
    private Integer adaptiveSamplingTimeValue;

    @Param(name = "adaptiveSamplingTimeUnit", optional = true)
    private String adaptiveSamplingTimeUnit;

    @Param(name = "applicationsOnlyEnabled", optional = true)
    private Boolean applicationsOnlyEnabled;

    @Param(name = "thresholdValue", optional = true)
    private String thresholdValue;

    @Param(name = "thresholdUnit", optional = true)
    private String thresholdUnit;

    @Param(name = "sampleRateFirstEnabled", optional = true)
    private Boolean sampleRateFirstEnabled;

    @Param(name = "traceStoreSize", optional = true)
    private Integer traceStoreSize;

    @Param(name = "traceStoreTimeout", optional = true)
    private String traceStoreTimeout;

    @Param(name = "reservoirSamplingEnabled", optional = true)
    private Boolean reservoirSamplingEnabled;

    @Param(name = "historicTraceStoreEnabled", optional = true)
    private Boolean historicTraceStoreEnabled;

    @Param(name = "historicTraceStoreSize", optional = true)
    private Integer historicTraceStoreSize;

    @Param(name = "historicTraceStoreTimeout", optional = true)
    private String historicTraceStoreTimeout;

    @Param(name = "enableNotifiers", alias = "enable-notifiers", optional = true)
    private List<String> enableNotifiers;

    @Param(name = "disableNotifiers", alias = "disable-notifiers", optional = true)
    private List<String> disableNotifiers;

    @Param(name = "setNotifiers", alias = "set-notifiers", optional = true)
    private List<String> setNotifiers;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        Config config = targetUtil.getConfig(target);
        final RequestTracingServiceConfiguration requestTracingServiceConfiguration = config.getExtensionByType(RequestTracingServiceConfiguration.class);

        if (requestTracingServiceConfiguration != null) {
            final Set<String> notifierNames = NotifierUtils.getNotifierNames(serviceLocator);
            try {
                ConfigSupport.apply(new SingleConfigCode<RequestTracingServiceConfiguration>() {
                    @Override
                    public Object run(final RequestTracingServiceConfiguration proxy) throws
                            PropertyVetoException, TransactionFailure {
                        boolean warn = false;
                        if (enabled != null) {
                            proxy.enabled(enabled.toString());
                        }
                        
                        if (sampleRate != null) {
                            proxy.setSampleRate(sampleRate);
                        }
                        if (adaptiveSamplingEnabled != null) {
                            proxy.setAdaptiveSamplingEnabled(adaptiveSamplingEnabled.toString());
                        }
                        if (adaptiveSamplingTargetCount != null) {
                            proxy.setAdaptiveSamplingTargetCount(adaptiveSamplingTargetCount);
                        }
                        if (adaptiveSamplingTimeValue != null) {
                            proxy.setAdaptiveSamplingTimeValue(adaptiveSamplingTimeValue.toString());
                        }
                        if (adaptiveSamplingTimeUnit != null) {
                            proxy.setAdaptiveSamplingTimeUnit(adaptiveSamplingTimeUnit);
                        }
                        
                        if (applicationsOnlyEnabled != null) {
                            proxy.setApplicationsOnlyEnabled(applicationsOnlyEnabled.toString());
                        }
                        if (thresholdValue != null) {
                            proxy.setThresholdValue(thresholdValue);
                        }
                        if (thresholdUnit != null) {
                            proxy.setThresholdUnit(thresholdUnit);
                        }
                        if (sampleRateFirstEnabled != null) {
                            proxy.setSampleRateFirstEnabled(sampleRateFirstEnabled.toString());
                        }
                        
                        if (traceStoreSize != null) {
                            warn = !traceStoreSize.toString().equals(proxy.getTraceStoreSize());
                            proxy.setTraceStoreSize(traceStoreSize.toString());
                        }
                        if (traceStoreTimeout != null) {
                            warn = !traceStoreTimeout.equals(proxy.getTraceStoreTimeout());
                            proxy.setTraceStoreTimeout(traceStoreTimeout);
                        }
                        if (reservoirSamplingEnabled != null) {
                            proxy.setReservoirSamplingEnabled(reservoirSamplingEnabled.toString());
                        }
                        
                        if (historicTraceStoreEnabled != null) {
                            proxy.setHistoricTraceStoreEnabled(historicTraceStoreEnabled.toString());
                        }
                        if (historicTraceStoreSize != null) {
                            warn = !historicTraceStoreSize.toString().equals(proxy.getHistoricTraceStoreSize());
                            proxy.setHistoricTraceStoreSize(historicTraceStoreSize.toString());
                        }
                        if (historicTraceStoreTimeout != null) {
                            warn = !historicTraceStoreTimeout.equals(proxy.getHistoricTraceStoreTimeout());
                            proxy.setHistoricTraceStoreTimeout(historicTraceStoreTimeout);
                        }

                        List<String> notifiers = proxy.getNotifierList();
                        if (enableNotifiers != null) {
                            for (String notifier : enableNotifiers) {
                                if (notifierNames.contains(notifier)) {
                                    if (!notifiers.contains(notifier)) {
                                        notifiers.add(notifier);
                                    }
                                } else {
                                    throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                                }
                            }
                        }
                        if (disableNotifiers != null) {
                            for (String notifier : disableNotifiers) {
                                if (notifierNames.contains(notifier)) {
                                    notifiers.remove(notifier);
                                } else {
                                    throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                                }
                            }
                        }
                        if (setNotifiers != null) {
                            notifiers.clear();
                            for (String notifier : setNotifiers) {
                                if (notifierNames.contains(notifier)) {
                                    if (!notifiers.contains(notifier)) {
                                        notifiers.add(notifier);
                                    }
                                } else {
                                    throw new PropertyVetoException("Unrecognised notifier " + notifier,
                                            new PropertyChangeEvent(proxy, "notifiers", notifiers, notifiers));
                                }
                            }
                        }

                        actionReport.setActionExitCode(ExitCode.SUCCESS);
                        if (warn) {
                            actionReport.setMessage(STRINGS.get("requesttracing.configure.store.size.warning"));
                        }
                        return proxy;
                    }
                }, requestTracingServiceConfiguration);
            } catch (TransactionFailure ex) {
                actionReport.failure(logger, ex.getCause().getMessage());
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
        
        if (sampleRate != null) {
            service.getExecutionOptions().setSampleRate(Double.parseDouble(sampleRate));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.samplerate.success",
                    "Request Tracing Sample Rate Value is set to {0}.", sampleRate) + "\n");
        }
        if (adaptiveSamplingEnabled != null) {
            service.getExecutionOptions().setAdaptiveSamplingEnabled(adaptiveSamplingEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.adaptivesampling.enabled.success",
                    "Request Tracing Adaptive Sampling Enabled Value is set to {0}.", adaptiveSamplingEnabled) + "\n");
        }
        if (adaptiveSamplingTargetCount != null) {
            service.getExecutionOptions().setAdaptiveSamplingTargetCount(Integer.valueOf(adaptiveSamplingTargetCount));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.adaptivesampling.targetcount.success",
                    "Request Tracing Adaptive Sampling Target Count is set to {0}.", adaptiveSamplingTargetCount) + "\n");
        }
        if (adaptiveSamplingTimeValue != null) {
            service.getExecutionOptions().setAdaptiveSamplingTimeValue(adaptiveSamplingTimeValue);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.adaptivesampling.timevalue.success",
                    "Request Tracing Adaptive Sampling Time Value is set to {0}.", adaptiveSamplingTimeValue) + "\n");
        }
        if (adaptiveSamplingTimeUnit != null) {
            service.getExecutionOptions().setAdaptiveSamplingTimeUnit(TimeUnit.valueOf(adaptiveSamplingTimeUnit));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.adaptivesampling.timeunit.success",
                    "Request Tracing Adaptive Sampling Time Unit is set to {0}.", adaptiveSamplingTimeUnit) + "\n");
        }
        
        if (applicationsOnlyEnabled != null) {
            service.getExecutionOptions().setApplicationsOnlyEnabled(applicationsOnlyEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.applicationsonly.success",
                    "Request Tracing Service Applications Only filter is set to {0}.", applicationsOnlyEnabled) + "\n");
        }
        if (thresholdValue != null) {
            service.getExecutionOptions().setThresholdValue(Long.valueOf(thresholdValue));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdvalue.success",
                    "Request Tracing Service Threshold Value is set to {0}.", thresholdValue) + "\n");
        }
        if (thresholdUnit != null) {
            service.getExecutionOptions().setThresholdUnit(TimeUnit.valueOf(thresholdUnit));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.thresholdunit.success",
                    "Request Tracing Service Threshold Unit is set to {0}.", thresholdUnit) + "\n");
        }
        if (sampleRateFirstEnabled != null) {
            service.getExecutionOptions().setSampleRateFirstEnabled(sampleRateFirstEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.sampleratefirst.success",
                    "Request Tracing Service Sample Rate First Enabled Value is set to {0}.", sampleRateFirstEnabled) + "\n");
        }
        
        if (traceStoreSize != null) {
            service.getExecutionOptions().setTraceStoreSize(traceStoreSize);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.store.size.success",
                    "Request Tracing Historic Trace Store Size is set to {0}.", traceStoreSize) + "\n");
        }
        if (traceStoreTimeout != null) {
            service.getExecutionOptions().setTraceStoreTimeout(TimeUtil.setStoreTimeLimit(traceStoreTimeout));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.store.timeout.success",
                    "Request Tracing Store Timeout is set to {0}.", traceStoreTimeout) + "\n");
        }
        if (reservoirSamplingEnabled != null) {
            service.getExecutionOptions().setReservoirSamplingEnabled(reservoirSamplingEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.reservoirsampling.enabled.success",
                    "Request Tracing Service Reservoir Sampling Enabled Value is set to {0}.", reservoirSamplingEnabled) + "\n");
        }
        
        if (historicTraceStoreEnabled != null) {
            service.getExecutionOptions().setHistoricTraceStoreEnabled(historicTraceStoreEnabled);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historictrace.status.success",
                    "Request Tracing Historic Trace status is set to {0}.", historicTraceStoreEnabled) + "\n");
        }
        if (historicTraceStoreSize != null) {
            service.getExecutionOptions().setHistoricTraceStoreSize(historicTraceStoreSize);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historictrace.storesize.success",
                    "Request Tracing Historic Trace Store Size is set to {0}.", historicTraceStoreSize) + "\n");
        }
        if (historicTraceStoreTimeout != null) {
            service.getExecutionOptions().setHistoricTraceStoreTimeout(TimeUtil.setStoreTimeLimit(historicTraceStoreTimeout));
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.historictrace.timeout.success",
                    "Request Tracing Historic Trace Store Timeout is set to {0}.", historicTraceStoreTimeout) + "\n");
        }

        Set<String> notifiers = service.getExecutionOptions().getEnabledNotifiers();
        if (enableNotifiers != null) {
            enableNotifiers.forEach(notifiers::add);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.notifier.enable.success",
                    "Request Tracing Notifiers {0} have been enabled.", Arrays.toString(enableNotifiers.toArray())) + "\n");
        }
        if (disableNotifiers != null) {
            disableNotifiers.forEach(notifiers::remove);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.notifier.disable.success",
                    "Request Tracing Notifiers {0} have been disabled.", Arrays.toString(disableNotifiers.toArray())) + "\n");
        }
        if (setNotifiers != null) {
            notifiers.clear();
            setNotifiers.forEach(notifiers::add);
            actionReport.appendMessage(strings.getLocalString("requesttracing.configure.notifier.enable.success",
                    "Request Tracing Notifiers {0} have been enabled.", Arrays.toString(setNotifiers.toArray())) + "\n");
        }

        service.bootstrapRequestTracingService();
    }
}
