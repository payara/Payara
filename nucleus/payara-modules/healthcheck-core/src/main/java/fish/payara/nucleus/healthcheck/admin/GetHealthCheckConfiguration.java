/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.


 Copyright (c) 2016 Payara Foundation. All rights reserved.


 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.healthcheck.HealthCheckConstants;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.configuration.CheckerConfigurationType;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.healthcheck.configuration.HoggingThreadsChecker;
import fish.payara.nucleus.healthcheck.configuration.ThresholdDiagnosticsChecker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.HashMap;
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

        ColumnFormatter baseColumnFormatter = new ColumnFormatter(baseHeaders);
        ColumnFormatter hoggingThreadsColumnFormatter = new ColumnFormatter(hoggingThreadsHeaders);
        ColumnFormatter thresholdDiagnosticsColumnFormatter = new ColumnFormatter(thresholdDiagnosticsHeaders);

        HealthCheckServiceConfiguration configuration = config.getExtensionByType(HealthCheckServiceConfiguration
                .class);
        List<ServiceHandle<BaseHealthCheck>> allServiceHandles = habitat.getAllServiceHandles(BaseHealthCheck.class);

        mainActionReport.appendMessage("Health Check Service Configuration is enabled?: " + configuration.getEnabled() + "\n");
        
        if (Boolean.parseBoolean(configuration.getEnabled())) {
            mainActionReport.appendMessage("Historical Tracing Enabled?: " + configuration.getHistoricalTraceEnabled() 
                    + "\n");
            if (Boolean.parseBoolean(configuration.getHistoricalTraceEnabled())) {
                mainActionReport.appendMessage("Historical Tracing Store Size: " 
                        + configuration.getHistoricalTraceStoreSize() + "\n");
            }
        }
        
        // Create the extraProps map for the general healthcheck configuration
        Properties mainExtraProps = new Properties();
        Map<String, Object> mainExtraPropsMap = new HashMap<>();
        
        mainExtraPropsMap.put("enabled", configuration.getEnabled());
        mainExtraPropsMap.put("historicalTraceEnabled", configuration.getHistoricalTraceEnabled());
        mainExtraPropsMap.put("historicalTraceStoreSize", configuration.getHistoricalTraceStoreSize());
        
        mainExtraProps.put("healthcheckConfiguration", mainExtraPropsMap);
        mainActionReport.setExtraProperties(mainExtraProps);

        mainActionReport.appendMessage("Below are the list of configuration details of each checker listed by its name.");
        mainActionReport.appendMessage(StringUtils.EOL);

        Properties baseExtraProps = new Properties();
        Properties hoggingThreadsExtraProps = new Properties();
        Properties thresholdDiagnosticsExtraProps = new Properties();
        
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
            }
            else if (checker instanceof ThresholdDiagnosticsChecker) {
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
            }
            else if (checker != null) {
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
        
        // Check that properties have been created for each checker, and create a default if not
        if (!baseExtraProps.containsKey("garbageCollector")) {
            Map<String, Object> extraPropsMap = new HashMap<>(4);
            extraPropsMap.put("name", DEFAULT_GARBAGE_COLLECTOR_NAME);
            baseExtraProps.put("garbageCollector", populateDefaultValuesMap(extraPropsMap));
        }
        
        if (!hoggingThreadsExtraProps.containsKey("hoggingThreads")) {
            Map<String, Object> extraPropsMap = new HashMap<>(6);
            extraPropsMap.put("name", DEFAULT_HOGGING_THREADS_NAME);
            hoggingThreadsExtraProps.put("hoggingThreads", populateDefaultValuesMap(extraPropsMap));
        }
        
        if (!thresholdDiagnosticsExtraProps.containsKey("cpuUsage")) {
            Map<String, Object> extraPropsMap = new HashMap<>(7);
            extraPropsMap.put("name", DEFAULT_CPU_USAGE_NAME);
            thresholdDiagnosticsExtraProps.put("cpuUsage", populateDefaultValuesMap(extraPropsMap));
        }
        
        if (!thresholdDiagnosticsExtraProps.containsKey("connectionPool")) {
            Map<String, Object> extraPropsMap = new HashMap<>(7);
            extraPropsMap.put("name", DEFAULT_CONNECTION_POOL_NAME);
            thresholdDiagnosticsExtraProps.put("connectionPool", populateDefaultValuesMap(extraPropsMap));
        }
        
        if (!thresholdDiagnosticsExtraProps.containsKey("heapMemoryUsage")) {
            Map<String, Object> extraPropsMap = new HashMap<>(7);
            extraPropsMap.put("name", DEFAULT_HEAP_MEMORY_USAGE_NAME);
            thresholdDiagnosticsExtraProps.put("heapMemoryUsage", populateDefaultValuesMap(extraPropsMap));
        }
        
        if (!thresholdDiagnosticsExtraProps.containsKey("machineMemoryUsage")) {
            Map<String, Object> extraPropsMap = new HashMap<>(7);
            extraPropsMap.put("name", DEFAULT_MACHINE_MEMORY_USAGE_NAME);
            thresholdDiagnosticsExtraProps.put("machineMemoryUsage", populateDefaultValuesMap(extraPropsMap));
        }
        
        // Add the extra props to their respective action reports
        baseActionReport.setExtraProperties(baseExtraProps);
        hoggingThreadsActionReport.setExtraProperties(hoggingThreadsExtraProps);
        thresholdDiagnosticsActionReport.setExtraProperties(thresholdDiagnosticsExtraProps);

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
    private void addHoggingThreadsCheckerExtraProps(Properties hoggingThreadsExtraProps, 
            HoggingThreadsChecker hoggingThreadsChecker) {
        Map<String, Object> extraPropsMap = new HashMap<>(6);       
        
        extraPropsMap.put("name", hoggingThreadsChecker.getName());
        extraPropsMap.put("enabled", hoggingThreadsChecker.getEnabled());
        extraPropsMap.put("time", hoggingThreadsChecker.getTime());
        extraPropsMap.put("unit", hoggingThreadsChecker.getUnit());
        extraPropsMap.put("threshold-percentage", hoggingThreadsChecker.getThresholdPercentage());
        extraPropsMap.put("retry-count", hoggingThreadsChecker.getRetryCount());
        
        hoggingThreadsExtraProps.put("hoggingThreads", extraPropsMap);
    }
    
    private void addThresholdDiagnosticsCheckerExtraProps(Properties thresholdDiagnosticsExtraProps, 
            ThresholdDiagnosticsChecker thresholdDiagnosticsChecker) {
        
        Map<String, Object> extraPropsMap = new HashMap<>(7);       
        
        extraPropsMap.put("name", thresholdDiagnosticsChecker.getName());
        extraPropsMap.put("enabled", thresholdDiagnosticsChecker.getEnabled());
        extraPropsMap.put("time", thresholdDiagnosticsChecker.getTime());
        extraPropsMap.put("unit", thresholdDiagnosticsChecker.getUnit());
        extraPropsMap.put("thresholdCritical", thresholdDiagnosticsChecker.getProperty(THRESHOLD_CRITICAL).getValue());
        extraPropsMap.put("thresholdWarning", thresholdDiagnosticsChecker.getProperty(THRESHOLD_WARNING).getValue());
        extraPropsMap.put("thresholdGood", thresholdDiagnosticsChecker.getProperty(THRESHOLD_GOOD).getValue());

        // Get the checker type
        ConfigView view = ConfigSupport.getImpl(thresholdDiagnosticsChecker);
        CheckerConfigurationType annotation = view.getProxyType().getAnnotation(CheckerConfigurationType.class);
        
        // Add the extraPropsMap as a property with a name matching its checker type
        switch (annotation.type()) {
            case CONNECTION_POOL:
                thresholdDiagnosticsExtraProps.put("connectionPool", extraPropsMap);
                break;
            case CPU_USAGE:
                thresholdDiagnosticsExtraProps.put("cpuUsage", extraPropsMap);
                break;
            case HEAP_MEMORY_USAGE:
                thresholdDiagnosticsExtraProps.put("heapMemoryUsage", extraPropsMap);
                break;
            case MACHINE_MEMORY_USAGE:
                thresholdDiagnosticsExtraProps.put("machineMemoryUsage", extraPropsMap);
                break;
        }
    }
    
    private void addBaseCheckerExtraProps(Properties baseExtraProps, 
            Checker checker) {
        Map<String, Object> extraPropsMap = new HashMap<>(4);       
        
        extraPropsMap.put("name", checker.getName());
        extraPropsMap.put("enabled", checker.getEnabled());
        extraPropsMap.put("time", checker.getTime());
        extraPropsMap.put("unit", checker.getUnit());
        
        // Get the checker type
        ConfigView view = ConfigSupport.getImpl(checker);
        CheckerConfigurationType annotation = view.getProxyType().getAnnotation(CheckerConfigurationType.class);
        
        // Add the extraPropsMap as a property with a name matching its checker type
        switch (annotation.type()) {
            case GARBAGE_COLLECTOR:
                baseExtraProps.put("garbageCollector", extraPropsMap);
                break;
        }
    }
    
    private Map<String, Object> populateDefaultValuesMap(Map<String, Object> extraPropsMap) {
        // Common properties
        extraPropsMap.put("enabled", DEFAULT_ENABLED);
        extraPropsMap.put("time", DEFAULT_TIME);
        extraPropsMap.put("unit", DEFAULT_UNIT);
        
        // Add default properties for hogging threads checker, or add default properties for a thresholdDiagnostics checker
        // if the checker type isn't a hogging thread or garbage collector checker (the only current base checker)
        if (extraPropsMap.containsValue(DEFAULT_HOGGING_THREADS_NAME)) {
            extraPropsMap.put("threshold-percentage", DEFAULT_THRESHOLD_PERCENTAGE);
            extraPropsMap.put("retry-count", DEFAULT_RETRY_COUNT);
        } else if (!extraPropsMap.containsValue(DEFAULT_GARBAGE_COLLECTOR_NAME)) {
            extraPropsMap.put("thresholdCritical", THRESHOLD_DEFAULTVAL_CRITICAL);
            extraPropsMap.put("thresholdWarning", THRESHOLD_DEFAULTVAL_WARNING);
            extraPropsMap.put("thresholdGood", THRESHOLD_DEFAULTVAL_GOOD);
        }
        
        return extraPropsMap;
    }
}
