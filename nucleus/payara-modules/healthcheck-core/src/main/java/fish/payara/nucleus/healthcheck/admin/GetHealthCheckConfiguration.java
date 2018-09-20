/*
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.CheckerConfigurationType;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.configuration.MicroProfileHealthCheckerConfiguration;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import fish.payara.nucleus.healthcheck.configuration.StuckThreadsChecker;
import java.util.HashMap;

import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.configuration.NotifierConfigurationType;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.types.Property;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.ConfigView;

/**
 * @author mertcaliskan
 */
@Service(name = "get-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.healthcheck.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG})
@RestEndpoints({
        @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
                opType = RestEndpoint.OpType.GET,
                path = "get-healthcheck-configuration",
                description = "List HealthCheck Configuration")
})
public class GetHealthCheckConfiguration implements AdminCommand, HealthCheckConstants {

    final static String baseHeaders[] = {"Name", "Enabled", "Time", "Unit"};
    final static String hoggingThreadsHeaders[] = {"Name", "Enabled", "Time", "Unit", "Threshold Percentage",
            "Retry Count"};
    final static String thresholdDiagnosticsHeaders[] = {"Name", "Enabled", "Time", "Unit", "Critical Threshold",
            "Warning Threshold", "Good Threshold"};
    final static String stuckThreadsHeaders[] = {"Name", "Enabled", "Time", "Unit", "Threshold Time", "Threshold Unit"};
    final static String MPHealthCheckHeaders[] = {"Name", "Enabled", "Time", "Unit", "Timeout"};
    final static String notifierHeaders[] = {"Name", "Notifier Enabled"};
    
    private final String garbageCollectorPropertyName = "garbageCollector";
    private final String cpuUsagePropertyName = "cpuUsage";
    private final String connectionPoolPropertyName = "connectionPool";
    private final String heapMemoryUsagePropertyName = "heapMemoryUsage";
    private final String machineMemoryUsagePropertyName = "machineMemoryUsage";
    private final String hoggingThreadsPropertyName = "hoggingThreads";
    private final String stuckThreadsPropertyName = "stuckThreads";
    private final String mpHealthcheckPropertyName = "mpHealth";
    
    @Inject
    ServiceLocator habitat;

    @Inject
    private Target targetUtil;

    @Param(name = "target", optional = true, defaultValue = "server")
    private String target;

    @Override
    public void execute(AdminCommandContext context) {

        Config config = targetUtil.getConfig(target);
        if (config == null) {
            context.getActionReport().setMessage("No such config named: " + target);
            context.getActionReport().setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        ActionReport mainActionReport = context.getActionReport();
        ActionReport baseActionReport = mainActionReport.addSubActionsReport(); // subReport(0)
        ActionReport hoggingThreadsActionReport = mainActionReport.addSubActionsReport(); // subReport(1)
        ActionReport thresholdDiagnosticsActionReport = mainActionReport.addSubActionsReport(); // subReport(2) 
        ActionReport stuckThreadsActionReport = mainActionReport.addSubActionsReport(); //subReport(3)
        ActionReport mpHealthcheckCheckerActionReport = mainActionReport.addSubActionsReport(); //subReport(4)

        ColumnFormatter baseColumnFormatter = new ColumnFormatter(baseHeaders);
        ColumnFormatter hoggingThreadsColumnFormatter = new ColumnFormatter(hoggingThreadsHeaders);
        ColumnFormatter stuckThreadsColumnFormatter = new ColumnFormatter(stuckThreadsHeaders);
        ColumnFormatter thresholdDiagnosticsColumnFormatter = new ColumnFormatter(thresholdDiagnosticsHeaders);
        ColumnFormatter mpHealthCheckColumnFormatter = new ColumnFormatter(MPHealthCheckHeaders);
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(notifierHeaders);

        HealthCheckServiceConfiguration configuration = config.getExtensionByType(HealthCheckServiceConfiguration.class);
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);
        List<ServiceHandle<BaseNotifierService>> allNotifierServiceHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        mainActionReport.appendMessage("Health Check Service Configuration is enabled?: " + configuration.getEnabled() + "\n");
        
        if (Boolean.parseBoolean(configuration.getEnabled())) {
            mainActionReport.appendMessage("Historical Tracing Enabled?: " + configuration.getHistoricalTraceEnabled() 
                    + "\n");
            if (Boolean.parseBoolean(configuration.getHistoricalTraceEnabled())) {
                mainActionReport.appendMessage("Historical Tracing Store Size: " 
                        + configuration.getHistoricalTraceStoreSize() + "\n");
            }

            if (!Strings.isNullOrEmpty(configuration.getHistoricalTraceStoreTimeout())) {
                mainActionReport.appendMessage("Health Check Historical Tracing Store Timeout in Seconds: "
                        + configuration.getHistoricalTraceStoreTimeout() + "\n");
            }
        }

        // Create the extraProps map for the general healthcheck configuration
        Properties mainExtraProps = new Properties();
        Map<String, Object> mainExtraPropsMap = new HashMap<>();

        mainExtraPropsMap.put("enabled", configuration.getEnabled());
        mainExtraPropsMap.put("historicalTraceEnabled", configuration.getHistoricalTraceEnabled());
        mainExtraPropsMap.put("historicalTraceStoreSize", configuration.getHistoricalTraceStoreSize());
        mainExtraPropsMap.put("historicalTraceStoreTimeout", configuration.getHistoricalTraceStoreTimeout());

        mainExtraProps.put("healthcheckConfiguration", mainExtraPropsMap);
        mainActionReport.setExtraProperties(mainExtraProps);

        if (!configuration.getNotifierList().isEmpty()) {
            List<Class<Notifier>> notifierClassList = Lists.transform(configuration.getNotifierList(), new Function<Notifier, Class<Notifier>>() {
                @Override
                public Class<Notifier> apply(Notifier input) {
                    return resolveNotifierClass(input);
                }
            });

            Properties extraProps = new Properties();
            for (ServiceHandle<BaseNotifierService> serviceHandle : allNotifierServiceHandles) {
                Notifier notifier = configuration.getNotifierByType(serviceHandle.getService().getNotifierType());
                if (notifier != null) {
                    ConfigView view = ConfigSupport.getImpl(notifier);
                    NotifierConfigurationType annotation = view.getProxyType().getAnnotation(NotifierConfigurationType.class);

                    if (notifierClassList.contains(view.<Notifier>getProxyType())) {
                        Object values[] = new Object[2];
                        values[0] = annotation.type();
                        values[1] = notifier.getEnabled();
                        notifiersColumnFormatter.addRow(values);

                        Map<String, Object> map = new HashMap<>(2);
                        map.put("notifierName", values[0]);
                        map.put("notifierEnabled", values[1]);

                        extraProps.put("notifierList" + annotation.type(), map);
                    }
                }
            }
            mainActionReport.getExtraProperties().putAll(extraProps);
            mainActionReport.appendMessage(notifiersColumnFormatter.toString());
            mainActionReport.appendMessage(StringUtils.EOL);
        }

        mainActionReport.appendMessage("Below are the list of configuration details of each checker listed by its name.");
        mainActionReport.appendMessage(StringUtils.EOL);

        Properties baseExtraProps = new Properties();
        Properties hoggingThreadsExtraProps = new Properties();
        Properties stuckThreadsExtrasProps = new Properties();
        Properties thresholdDiagnosticsExtraProps = new Properties();
        Properties mpHealthcheckExtrasProps = new Properties();
        
        for (ServiceHandle<BaseHealthCheck> serviceHandle : allServiceHandles) {
            Checker checker = configuration.getCheckerByType(serviceHandle.getService().getCheckerType());

            if (checker instanceof HoggingThreadsChecker) {
                HoggingThreadsChecker hoggingThreadsChecker = (HoggingThreadsChecker) checker;

                Object values[] = new Object[6];
                values[0] = hoggingThreadsChecker.getName();
                values[1] = hoggingThreadsChecker.getEnabled();
                values[2] = hoggingThreadsChecker.getTime();
                values[3] = hoggingThreadsChecker.getUnit();
                values[4] = hoggingThreadsChecker.getThresholdPercentage();
                values[5] = hoggingThreadsChecker.getRetryCount();
                hoggingThreadsColumnFormatter.addRow(values);
                
                // Create the extra props map for a hogging thread checker
                addHoggingThreadsCheckerExtraProps(hoggingThreadsExtraProps, hoggingThreadsChecker);
            } else if (checker instanceof ThresholdDiagnosticsChecker) {
                ThresholdDiagnosticsChecker thresholdDiagnosticsChecker = (ThresholdDiagnosticsChecker) checker;

                Object values[] = new Object[7];
                values[0] = thresholdDiagnosticsChecker.getName();
                values[1] = thresholdDiagnosticsChecker.getEnabled();
                values[2] = thresholdDiagnosticsChecker.getTime();
                values[3] = thresholdDiagnosticsChecker.getUnit();
                Property thresholdCriticalProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_CRITICAL);
                values[4] = thresholdCriticalProperty != null ? thresholdCriticalProperty.getValue() : "-";
                Property thresholdWarningProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_WARNING);
                values[5] = thresholdWarningProperty != null ? thresholdWarningProperty.getValue() : "-";
                Property thresholdGoodProperty = thresholdDiagnosticsChecker.getProperty(THRESHOLD_GOOD);
                values[6] = thresholdGoodProperty != null ? thresholdGoodProperty.getValue() : "-";
                thresholdDiagnosticsColumnFormatter.addRow(values);
                
                // Create the extra props map for a checker with thresholds
                addThresholdDiagnosticsCheckerExtraProps(thresholdDiagnosticsExtraProps, 
                        thresholdDiagnosticsChecker);
            } else if (checker instanceof StuckThreadsChecker) {
                StuckThreadsChecker stuckThreadsChecker = (StuckThreadsChecker) checker;
                
                Object values[] = new Object[6];
                values[0] = stuckThreadsChecker.getName();
                values[1] = stuckThreadsChecker.getEnabled();
                values[2] = stuckThreadsChecker.getTime();
                values[3] = stuckThreadsChecker.getUnit();
                values[4] = stuckThreadsChecker.getThreshold();
                values[5] = stuckThreadsChecker.getThresholdTimeUnit();
                stuckThreadsColumnFormatter.addRow(values);
                
                addStuckThreadsCheckerExtrasProps(stuckThreadsExtrasProps, stuckThreadsChecker);
                
            } else if (checker instanceof MicroProfileHealthCheckerConfiguration) {
                MicroProfileHealthCheckerConfiguration mpHealthcheckChecker = (MicroProfileHealthCheckerConfiguration) checker;
                
                Object[] values = new Object[5];
                values[0] = mpHealthcheckChecker.getName();
                values[1] = mpHealthcheckChecker.getEnabled();
                values[2] = mpHealthcheckChecker.getTime();
                values[3] = mpHealthcheckChecker.getUnit();
                values[4] = mpHealthcheckChecker.getTimeout();
                mpHealthCheckColumnFormatter.addRow(values);
                
                addMPHealthcheckCheckerExtrasProps(mpHealthcheckExtrasProps, mpHealthcheckChecker);
                
            }else if (checker != null) {
                Object values[] = new Object[4];
                values[0] = checker.getName();
                values[1] = checker.getEnabled();
                values[2] = checker.getTime();
                values[3] = checker.getUnit();
                baseColumnFormatter.addRow(values);
                
                // Create the extra props map for a base checker
                addBaseCheckerExtraProps(baseExtraProps, checker);
            }
        }

        if (!baseColumnFormatter.getContent().isEmpty()) {
            baseActionReport.setMessage(baseColumnFormatter.toString());
            baseActionReport.appendMessage(StringUtils.EOL);
        }
        if (!hoggingThreadsColumnFormatter.getContent().isEmpty()) {
            hoggingThreadsActionReport.setMessage(hoggingThreadsColumnFormatter.toString());
            hoggingThreadsActionReport.appendMessage(StringUtils.EOL);
        }
        if (!thresholdDiagnosticsColumnFormatter.getContent().isEmpty()) {
            thresholdDiagnosticsActionReport.setMessage(thresholdDiagnosticsColumnFormatter.toString());
            thresholdDiagnosticsActionReport.appendMessage(StringUtils.EOL);
        }
        if (!stuckThreadsColumnFormatter.getContent().isEmpty()){
            stuckThreadsActionReport.setMessage(stuckThreadsColumnFormatter.toString());
            stuckThreadsActionReport.appendMessage(StringUtils.EOL);
        }
        if (!mpHealthCheckColumnFormatter.getContent().isEmpty()) {
            mpHealthcheckCheckerActionReport.setMessage(mpHealthCheckColumnFormatter.toString());
            mpHealthcheckCheckerActionReport.appendMessage(StringUtils.EOL);
        }
        
        // Populate the extraProps with defaults for any checker that isn't present
        baseExtraProps = checkCheckerPropertyPresence(thresholdDiagnosticsExtraProps, garbageCollectorPropertyName);
        hoggingThreadsExtraProps = checkCheckerPropertyPresence(hoggingThreadsExtraProps, hoggingThreadsPropertyName);
        stuckThreadsExtrasProps = checkCheckerPropertyPresence(stuckThreadsExtrasProps, stuckThreadsPropertyName);
        thresholdDiagnosticsExtraProps = checkCheckerPropertyPresence(thresholdDiagnosticsExtraProps, 
                cpuUsagePropertyName);
        thresholdDiagnosticsExtraProps = checkCheckerPropertyPresence(thresholdDiagnosticsExtraProps, 
                connectionPoolPropertyName);
        thresholdDiagnosticsExtraProps = checkCheckerPropertyPresence(thresholdDiagnosticsExtraProps, 
                heapMemoryUsagePropertyName);
        thresholdDiagnosticsExtraProps = checkCheckerPropertyPresence(thresholdDiagnosticsExtraProps, 
                machineMemoryUsagePropertyName);
        mpHealthcheckExtrasProps = checkCheckerPropertyPresence(mpHealthcheckExtrasProps, mpHealthcheckPropertyName);
        
        // Add the extra props to their respective action reports
        baseActionReport.setExtraProperties(baseExtraProps);
        hoggingThreadsActionReport.setExtraProperties(hoggingThreadsExtraProps);
        thresholdDiagnosticsActionReport.setExtraProperties(thresholdDiagnosticsExtraProps);
        stuckThreadsActionReport.setExtraProperties(stuckThreadsExtrasProps);
        mpHealthcheckCheckerActionReport.setExtraProperties(mpHealthcheckExtrasProps);
        
        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
    private void addHoggingThreadsCheckerExtraProps(Properties hoggingThreadsExtraProps, 
            HoggingThreadsChecker hoggingThreadsChecker) {
        Map<String, Object> extraPropsMap = new HashMap<>(6);       
        
        extraPropsMap.put("checkerName", hoggingThreadsChecker.getName());
        extraPropsMap.put("enabled", hoggingThreadsChecker.getEnabled());
        extraPropsMap.put("time", hoggingThreadsChecker.getTime());
        extraPropsMap.put("unit", hoggingThreadsChecker.getUnit());
        extraPropsMap.put("threshold-percentage", hoggingThreadsChecker.getThresholdPercentage());
        extraPropsMap.put("retry-count", hoggingThreadsChecker.getRetryCount());
        
        hoggingThreadsExtraProps.put(hoggingThreadsPropertyName, extraPropsMap);
    }
    
    private void addStuckThreadsCheckerExtrasProps(Properties stuckThreadsExtrasProps, StuckThreadsChecker stuckThreadsChecker){
        Map<String, Object> extraPropsMap = new HashMap<String, Object>(6);
        
        extraPropsMap.put("checkerName", stuckThreadsChecker.getName());
        extraPropsMap.put("enabled", stuckThreadsChecker.getEnabled());
        extraPropsMap.put("time", stuckThreadsChecker.getTime());
        extraPropsMap.put("unit", stuckThreadsChecker.getUnit());
        extraPropsMap.put("threshold", stuckThreadsChecker.getThreshold());
        extraPropsMap.put("thresholdUnit", stuckThreadsChecker.getThresholdTimeUnit());
        
        stuckThreadsExtrasProps.put(stuckThreadsPropertyName, extraPropsMap);
        
    }
    
    private void addMPHealthcheckCheckerExtrasProps(Properties mpHealthcheckExtrasProps, MicroProfileHealthCheckerConfiguration mpHealthcheckCheck) {
        Map<String, Object> extraPropsMap = new HashMap<String, Object>(5);
        extraPropsMap.put("checkerName", mpHealthcheckCheck.getName());
        extraPropsMap.put("enabled", mpHealthcheckCheck.getEnabled());
        extraPropsMap.put("time", mpHealthcheckCheck.getTime());
        extraPropsMap.put("unit", mpHealthcheckCheck.getUnit());
        extraPropsMap.put("timeout", mpHealthcheckCheck.getTimeout());
        
        mpHealthcheckExtrasProps.put(mpHealthcheckPropertyName, extraPropsMap);
        
    }
    
    private void addThresholdDiagnosticsCheckerExtraProps(Properties thresholdDiagnosticsExtraProps, 
            ThresholdDiagnosticsChecker thresholdDiagnosticsChecker) {
        
        Map<String, Object> extraPropsMap = new HashMap<>(7);       
        
        extraPropsMap.put("checkerName", thresholdDiagnosticsChecker.getName());
        extraPropsMap.put("enabled", thresholdDiagnosticsChecker.getEnabled());
        extraPropsMap.put("time", thresholdDiagnosticsChecker.getTime());
        extraPropsMap.put("unit", thresholdDiagnosticsChecker.getUnit());

        if (thresholdDiagnosticsChecker.getProperty(THRESHOLD_CRITICAL) != null) {
            extraPropsMap.put("thresholdCritical", thresholdDiagnosticsChecker.getProperty(THRESHOLD_CRITICAL).getValue());
        }
        if (thresholdDiagnosticsChecker.getProperty(THRESHOLD_WARNING) != null) {
            extraPropsMap.put("thresholdWarning", thresholdDiagnosticsChecker.getProperty(THRESHOLD_WARNING).getValue());
        }
        if (thresholdDiagnosticsChecker.getProperty(THRESHOLD_GOOD) != null) {
            extraPropsMap.put("thresholdGood", thresholdDiagnosticsChecker.getProperty(THRESHOLD_GOOD).getValue());
        }

        // Get the checker type
        ConfigView view = ConfigSupport.getImpl(thresholdDiagnosticsChecker);
        CheckerConfigurationType annotation = view.getProxyType().getAnnotation(CheckerConfigurationType.class);
        
        // Add the extraPropsMap as a property with a name matching its checker type
        switch (annotation.type()) {
            case CONNECTION_POOL:
                thresholdDiagnosticsExtraProps.put(connectionPoolPropertyName, extraPropsMap);
                break;
            case CPU_USAGE:
                thresholdDiagnosticsExtraProps.put(cpuUsagePropertyName, extraPropsMap);
                break;
            case GARBAGE_COLLECTOR:
                thresholdDiagnosticsExtraProps.put(garbageCollectorPropertyName, extraPropsMap);
                break;
            case HEAP_MEMORY_USAGE:
                thresholdDiagnosticsExtraProps.put(heapMemoryUsagePropertyName, extraPropsMap);
                break;
            case MACHINE_MEMORY_USAGE:
                thresholdDiagnosticsExtraProps.put(machineMemoryUsagePropertyName, extraPropsMap);
                break;
            case STUCK_THREAD:
                thresholdDiagnosticsExtraProps.put(stuckThreadsPropertyName, extraPropsMap);
                break;
                
        }
    }
    
    private void addBaseCheckerExtraProps(Properties baseExtraProps, 
            Checker checker) {
        Map<String, Object> extraPropsMap = new HashMap<>(4);       
        
        extraPropsMap.put("checkerName", checker.getName());
        extraPropsMap.put("enabled", checker.getEnabled());
        extraPropsMap.put("time", checker.getTime());
        extraPropsMap.put("unit", checker.getUnit());
    }
    
    private Properties checkCheckerPropertyPresence(Properties extraProps, String checkerName) {
        // Check that properties have been created for each checker, and create a default if not
        if (!extraProps.containsKey(checkerName)) {
            Map<String, Object> extraPropsMap;
            switch (checkerName) {
                case garbageCollectorPropertyName:
                    extraPropsMap = new HashMap<>(7);
                    extraPropsMap.put("checkerName", DEFAULT_GARBAGE_COLLECTOR_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case hoggingThreadsPropertyName:
                    extraPropsMap = new HashMap<>(6);
                    extraPropsMap.put("checkerName", DEFAULT_HOGGING_THREADS_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case cpuUsagePropertyName:
                    extraPropsMap = new HashMap<>(7);
                    extraPropsMap.put("checkerName", DEFAULT_CPU_USAGE_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case connectionPoolPropertyName:
                    extraPropsMap = new HashMap<>(7);
                    extraPropsMap.put("checkerName", DEFAULT_CONNECTION_POOL_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case heapMemoryUsagePropertyName:
                    extraPropsMap = new HashMap<>(7);
                    extraPropsMap.put("checkerName", DEFAULT_HEAP_MEMORY_USAGE_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case machineMemoryUsagePropertyName:
                    extraPropsMap = new HashMap<>(7);
                    extraPropsMap.put("checkerName", DEFAULT_MACHINE_MEMORY_USAGE_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case stuckThreadsPropertyName:
                    extraPropsMap = new HashMap<>(6);
                    extraPropsMap.put("checkerName", DEFAULT_STUCK_THREAD_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
                case mpHealthcheckPropertyName:
                    extraPropsMap = new HashMap<>(5);
                    extraPropsMap.put("checkerName", DEFAULT_MICROPROFILE_HEALTHCHECK_NAME);
                    extraProps.put(checkerName, populateDefaultValuesMap(extraPropsMap));
                    break;
            }
        }
        
        return extraProps;
    }
    
    private Map<String, Object> populateDefaultValuesMap(Map<String, Object> extraPropsMap) {
        // Common properties
        extraPropsMap.put("enabled", DEFAULT_ENABLED);
        extraPropsMap.put("time", DEFAULT_TIME);
        extraPropsMap.put("unit", DEFAULT_UNIT);
        
        // Add default properties for hogging threads checker, or add default properties for a thresholdDiagnostics checker
        if (extraPropsMap.containsValue(DEFAULT_HOGGING_THREADS_NAME)) {
            extraPropsMap.put("threshold-percentage", DEFAULT_THRESHOLD_PERCENTAGE);
            extraPropsMap.put("retry-count", DEFAULT_RETRY_COUNT);
        } else if (extraPropsMap.containsValue(DEFAULT_STUCK_THREAD_NAME)){
            extraPropsMap.put("threshold",  DEFAULT_TIME);
            extraPropsMap.put("thresholdUnit", DEFAULT_UNIT);
        } else if (extraPropsMap.containsValue(DEFAULT_MICROPROFILE_HEALTHCHECK_NAME)) {
            extraPropsMap.put("timeout", DEFAULT_TIMEOUT);
        } else {
            extraPropsMap.put("thresholdCritical", THRESHOLD_DEFAULTVAL_CRITICAL);
            extraPropsMap.put("thresholdWarning", THRESHOLD_DEFAULTVAL_WARNING);
            extraPropsMap.put("thresholdGood", THRESHOLD_DEFAULTVAL_GOOD);
        }
        
        return extraPropsMap;
    }

    private Class<Notifier> resolveNotifierClass(Notifier input) {
        ConfigView view = ConfigSupport.getImpl(input);
        return view.getProxyType();
    }
}
