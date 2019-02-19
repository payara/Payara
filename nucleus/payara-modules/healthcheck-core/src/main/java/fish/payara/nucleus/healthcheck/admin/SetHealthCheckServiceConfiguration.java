/*
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.admin;

import java.beans.PropertyVetoException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.validation.constraints.Max;
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
import org.jvnet.hk2.config.types.Property;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.LocalStringManagerImpl;

import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.HealthCheckExecutionOptions;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.CheckerConfigurationType;
import fish.payara.nucleus.healthcheck.configuration.CheckerType;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.healthcheck.preliminary.BaseThresholdHealthCheck;

/**
 * Service to configure individual specific {@link BaseHealthCheck} service directly using dynamic or their
 * {@link Checker} configuration.
 * 
 * Depending on the concrete {@link Checker} type the are more parameters that have an effect.
 * 
 * @author Jan Bernitt
 * @since 5.191
 */
@Service(name = "set-healthcheck-service-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure.service")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-service-configuration",
            description = "Enables/Disables Health Check Service Specified With Name")
})
public class SetHealthCheckServiceConfiguration implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(SetHealthCheckServiceConfiguration.class);

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Inject
    private Logger logger;

    @Inject
    private HealthCheckService healthCheckService;

    @Inject
    private ServerEnvironment server;

    // target params:

    @Param(name = "target", optional = true, defaultValue = "server-config")
    private String target;
    private Config targetConfig;

    @Param(name = "service")
    private String serviceName;
    private CheckerType serviceType;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    private boolean dynamic;

    // general properties params:

    @Param(name = "enabled", optional = false)
    private boolean enabled;

    @Param(name = "time", optional = true)
    @Min(value = 1, message = "Time period must be 1 or more")
    private String time;

    @Param(name = "time-unit", optional = true,
            acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String timeUnit;

    // hogging threads properties params:

    @Param(name = "hogging-threads-threshold", optional = true)
    @Min(value = 0, message = "Hogging threads threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Hogging threads threshold is a percentage so must be less than 100")
    private String hogginThreadsThreshold;

    @Min(value = 1, message = "Hogging threads retry count must be 1 or more")
    @Param(name = "hogging-threads-retry-count", optional = true)
    private String hogginThreadsRetryCount;

    // stuck threads property params:

    @Param(name = "stuck-threads-threshold", optional = true)
    @Min(value = 1, message = "Threshold length must be 1 or more")
    private String stuckThreadsThreshold;

    @Param(name = "stuck-threads-threshold-unit", optional = true,
            acceptableValues = "DAYS,HOURS,MICROSECONDS,MILLISECONDS,MINUTES,NANOSECONDS,SECONDS")
    private String stuckThreadsThresholdUnit;

    // threshold properties params:

    @Param(name = "threshold-critical", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_CRITICAL)
    @Min(value = 0, message = "Critical threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Critical threshold is a percentage so must be less than 100")
    private String thresholdCritical;

    @Param(name = "threshold-warning", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_WARNING)
    @Min(value = 0, message = "Warning threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Wanring threshold is a percentage so must be less than 100")
    private String thresholdWarning;

    @Param(name = "threshold-good", optional = true, defaultValue = HealthCheckConstants.THRESHOLD_DEFAULTVAL_GOOD)
    @Min(value = 0, message = "Good threshold is a percentage so must be greater than zero")
    @Max(value = 100, message ="Good threshold is a percentage so must be less than 100")
    private String thresholdGood;

    private ActionReport report;

    @Override
    public void execute(AdminCommandContext context) {
        report = context.getActionReport();
        if (report.getExtraProperties() == null) {
            report.setExtraProperties(new Properties());
        }

        targetConfig = targetUtil.getConfig(target);

        serviceType = parseServiceType(serviceName);

        if (serviceType == null) {
            StringBuilder values = new StringBuilder();
            for (CheckerType type : CheckerType.values()) {
                if (values.length() > 0) {
                    values.append(", ");
                }
                values.append(type.name().toLowerCase().replace('_', '-'));
            }
            report.setMessage("No such service: " + serviceName + ".\nChoose one of: " + values
                    + ".\nThe name can also be given in short form consisting only of the first letters of each word.");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        // update the service to unify the way it is printed later on
        serviceName = serviceType.name().toLowerCase().replace('_', '-');

        BaseHealthCheck<?, ?> service = getService();
        if (service == null) {
            report.appendMessage(strings.getLocalString("healthcheck.service.configure.status.error",
                    "Service with name {0} could not be found.", serviceName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        updateServiceConfiguration(service);
    }

    /**
     * Gives the {@link CheckerType} for its name case insensitive. Underscores in the enum name should be given as
     * dash.
     *
     * The name can also be given in short form consisting only of the first letters of each word.
     */
    private static CheckerType parseServiceType(String serviceName) {
        if (serviceName.length() < 4) {
            for (CheckerType type : CheckerType.values()) {
                String shortName = "";
                for (String word : type.name().split("_")) {
                    shortName += word.charAt(0);
                }
                if (shortName.equals(serviceName.toUpperCase())) {
                    return type;
                }
            }
        } else {
            try {
                return CheckerType.valueOf(serviceName.toUpperCase().replace('-', '_'));
            } catch (IllegalArgumentException ex) {
                // fall through
            }
        }
        return null;
    }

    private BaseHealthCheck<?, ?> getService() {
        for (BaseHealthCheck<?, ?> service : habitat.getAllServices(BaseHealthCheck.class)) {
            if (service.getCheckerType().getAnnotation(CheckerConfigurationType.class).type() == serviceType)
                return service;
        }
        return null;
    }

    private <O extends HealthCheckExecutionOptions, C extends Checker> void updateServiceConfiguration(BaseHealthCheck<O, C> service) {
        HealthCheckServiceConfiguration config = targetConfig.getExtensionByType(HealthCheckServiceConfiguration.class);
        final Class<C> checkerType = service.getCheckerType();
        C checker = config.getCheckerByType(checkerType);
        try {
            if (checker == null) {
                ConfigSupport.apply(new SingleConfigCode<HealthCheckServiceConfiguration>() {
                    @Override
                    public Object run(HealthCheckServiceConfiguration configProxy)
                            throws PropertyVetoException, TransactionFailure {
                                Checker newChecker = configProxy.createChild(checkerType);
                                configProxy.getCheckerList().add(newChecker);
                                updateProperties(newChecker, checkerType);
                                return configProxy;
                            }
                }, config);
            } else {
                ConfigSupport.apply(new SingleConfigCode<C>() {
                    @Override
                    public Object run(C proxy) throws PropertyVetoException, TransactionFailure {
                        return updateProperties(proxy, checkerType);
                    }
                }, checker);
            }
            if (ThresholdDiagnosticsChecker.class.isAssignableFrom(checkerType)) {
                ThresholdDiagnosticsChecker thresholdDiagnosisConfig = (ThresholdDiagnosticsChecker) config
                        .getCheckerByType(checkerType); // get updated checker
                updateProperty(thresholdDiagnosisConfig, HealthCheckConstants.THRESHOLD_CRITICAL, thresholdCritical);
                updateProperty(thresholdDiagnosisConfig, HealthCheckConstants.THRESHOLD_WARNING, thresholdWarning);
                updateProperty(thresholdDiagnosisConfig, HealthCheckConstants.THRESHOLD_GOOD, thresholdGood);
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
        catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            report.setMessage(ex.getCause().getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        if (dynamic && (!server.isDas() || targetConfig.isDas())) {
            configureDynamically(service, config.getCheckerByType(checkerType));
        }
    }

    private <C extends Checker, O extends HealthCheckExecutionOptions> void configureDynamically(
            BaseHealthCheck<O, C> service, C config) {
        if (service.getOptions() == null) {
            service.setOptions(service.constructOptions(config));
        }
        healthCheckService.registerCheck(config.getName(), service);
        healthCheckService.reboot();
        if (service instanceof BaseThresholdHealthCheck) {
            configureDynamically((BaseThresholdHealthCheck<?, ?>) service);
        }
    }

    private void configureDynamically(BaseThresholdHealthCheck<?, ?> service) {
        if (thresholdCritical != null) {
            service.getOptions().setThresholdCritical(Integer.valueOf(thresholdCritical));
            report.appendMessage(strings.getLocalString(
                    "healthcheck.service.configure.threshold.critical.success",
                    "Critical threshold for {0} service is set with value {1}.", serviceName, thresholdCritical));
            report.appendMessage("\n");
        }
        if (thresholdWarning != null) {
            service.getOptions().setThresholdWarning(Integer.valueOf(thresholdWarning));
            report.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.warning.success",
                    "Warning threshold for {0} service is set with value {1}.", serviceName, thresholdWarning));
            report.appendMessage("\n");
        }
        if (thresholdGood != null) {
            service.getOptions().setThresholdGood(Integer.valueOf(thresholdGood));
            report.appendMessage(strings.getLocalString("healthcheck.service.configure.threshold.good.success",
                    "Good threshold for {0} service is set with value {1}.", serviceName, thresholdGood));
            report.appendMessage("\n");
        }
    }

    <C extends Checker> Checker updateProperties(Checker config, Class<C> type) throws PropertyVetoException {
        String enabled = updateProperty("enabled", config.getEnabled(), String.valueOf(this.enabled));
        if (enabled != null) {
            config.setEnabled(enabled);
        }
        String time = updateProperty("time", config.getTime(), this.time);
        if (time != null) {
            config.setTime(time);
        }
        String unit = updateProperty("time-unit", config.getUnit(), timeUnit);
        if (unit != null) {
            config.setUnit(unit);
        }
        if (HoggingThreadsChecker.class.isAssignableFrom(type)) {
            HoggingThreadsChecker hoggingThreadsConfig = (HoggingThreadsChecker) config;
            String hoggingThreshold = updateProperty("hogging-threads-threshold",
                    hoggingThreadsConfig.getThresholdPercentage(), hogginThreadsThreshold);
            if (hoggingThreshold != null) {
                hoggingThreadsConfig.setThresholdPercentage(hoggingThreshold);
            }
            String retryCount = updateProperty("hogging-threads-retry-count", hoggingThreadsConfig.getRetryCount(),
                    hogginThreadsRetryCount);
            if (retryCount != null) {
                hoggingThreadsConfig.setRetryCount(retryCount);
            }
        }
        if (StuckThreadsChecker.class.isAssignableFrom(type)) {
            StuckThreadsChecker stuckThreadsConfig = (StuckThreadsChecker) config;
            String stuckThreshold = updateProperty("stuck-threads-threshold", stuckThreadsConfig.getThreshold(),
                    stuckThreadsThreshold);
            if (stuckThreshold != null) {
                stuckThreadsConfig.setThreshold(stuckThreshold);
            }
            String stuckThresholdUnit = updateProperty("stuck-threads-threshold-unit",
                    stuckThreadsConfig.getThresholdTimeUnit(), stuckThreadsThresholdUnit);
            if (stuckThresholdUnit != null) {
                stuckThreadsConfig.setThresholdTimeUnit(stuckThresholdUnit);
            }
        }
        return config;
    }

    /**
     * Updates the named property in the given config with the given value. If the property does not exist it is
     * created.
     */
    private void updateProperty(ThresholdDiagnosticsChecker config, final String name, final String value)
            throws TransactionFailure {
        Property prop = config.getProperty(name);
        if (prop == null) {
            ConfigSupport.apply(new SingleConfigCode<ThresholdDiagnosticsChecker>() {
                @Override
                public Object run(ThresholdDiagnosticsChecker configProxy)
                        throws PropertyVetoException, TransactionFailure {
                            Property newProp = configProxy.createChild(Property.class);
                            updateProperty(newProp, name, value);
                            configProxy.getProperty().add(newProp);
                            return configProxy;
                        }
            }, config);
        } else {
            ConfigSupport.apply(new SingleConfigCode<Property>() {
                @Override
                public Object run(Property propertyProxy) throws PropertyVetoException, TransactionFailure {
                    updateProperty(propertyProxy, name, value);
                    return propertyProxy;
                }
            }, prop);
        }
    }

    void updateProperty(Property prop, String name, String value) throws PropertyVetoException {
        String from = prop.getValue();
        if (value != null && !value.equals(from)) {
            report.appendMessage(serviceName + "." + name +" was " + from + " set to " + value + "\n");
            prop.setName(name);
            prop.setValue(value);
        }
    }

    private String updateProperty(String name, String from, String to) {
        if (to != null && !to.equals(from)) {
            report.appendMessage(serviceName + "." + name +" was " + from + " set to " + to + "\n");
            return to;
        }
        return null;
    }

}
