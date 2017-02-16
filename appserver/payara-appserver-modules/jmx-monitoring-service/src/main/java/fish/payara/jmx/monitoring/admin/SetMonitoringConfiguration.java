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
package fish.payara.jmx.monitoring.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.jmx.monitoring.MonitoringService;
import fish.payara.jmx.monitoring.configuration.MonitoringServiceConfiguration;
import java.beans.PropertyVetoException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
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

/**
 * Asadmin command to set the monitoring service's configuration.
 *
 * @author savage
 */
@Service(name = "set-monitoring-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.monitoring.configuration")
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "set-monitoring-configuration",
            description = "Sets the Monitoring Service Configuration to that specified")
})
public class SetMonitoringConfiguration implements AdminCommand {

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Inject
    MonitoringService monitoringService;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "amx", optional = true)
    private Boolean amx;

    @Param(name = "logfrequency", optional = true)
    private String logfrequency;

    @Param(name = "logfrequencyunit", optional = true)
    private String logfrequencyunit;

    @Param(name = "addproperty", optional = true)
    private String addproperty;

    @Param(name = "delproperty", optional = true)
    private String delproperty;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Config config = targetUtil.getConfig(target);

        final MonitoringService service = serviceLocator.getService(MonitoringService.class);
        if (service == null) {
            actionReport.appendMessage("Could not find a monitoring service.");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }       

        MonitoringServiceConfiguration monitoringConfig = config.getExtensionByType(MonitoringServiceConfiguration.class);
        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringServiceConfiguration>() {
                @Override
                public Object run(final MonitoringServiceConfiguration monitoringConfigProxy) throws PropertyVetoException, TransactionFailure {
                    updateConfiguration(monitoringConfigProxy);
                    updateAttributes(actionReport, monitoringConfigProxy);
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return monitoringConfigProxy;
                }
            }, monitoringConfig);
                   
            if (dynamic) {
                enableOnTarget(actionReport, context, enabled);
            }

        } catch (TransactionFailure ex) {
            Logger.getLogger(SetMonitoringConfiguration.class.getName()).log(Level.WARNING, "Exception during command "
                    + "set-monitoring-configuration: " + ex.getCause().getMessage());
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

    }

    /**
     * Updates the configuration attributes for the MonitoringServiceConfiguration given to it.
     *
     * @param monitoringConfig
     * @throws PropertyVetoException
     */
    private void updateConfiguration(MonitoringServiceConfiguration monitoringConfig) throws PropertyVetoException {
         
        if (null != enabled) {
            monitoringConfig.setEnabled(String.valueOf(enabled));
        } 
        if (null != amx) {
            monitoringConfig.setAmx(String.valueOf(amx));
        } 
        if (null != logfrequency) {
            monitoringConfig.setLogFrequency(String.valueOf(logfrequency));
        } 
        if (null != logfrequencyunit) {
            monitoringConfig.setLogFrequencyUnit(logfrequencyunit);
        }
    }

    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation invocation;

        if (target.equals("server-config")) {
            invocation = runner.getCommandInvocation("__enable-monitoring-service-on-das", subReport, context.getSubject());
        } else {
            invocation = runner.getCommandInvocation("__enable-monitoring-service-on-instance", subReport, context.getSubject());
        }

        // Build the parameters
        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        invocation.parameters(params);
        invocation.execute();
    }

    /**
     * Updates monitoring attributes through adding a new property and/or deleting an existing one.
     *
     * @param actionReport
     * @param monitoringConfig
     * @throws PropertyVetoException
     * @throws TransactionFailure
     */
    private void updateAttributes(ActionReport actionReport, MonitoringServiceConfiguration monitoringConfig) throws PropertyVetoException, TransactionFailure {
        List<Property> attributes = monitoringConfig.getProperty();

        if (null != delproperty) {
            for (Property attribute : attributes) {
                if (attribute.getName().equals(delproperty)) {
                   attributes.remove(attribute); 
                   break;
                }
            }
        }

        if (null != addproperty) {
            String[] addpropertytokens = addproperty.split(" ");
            String name = null, value = null, description = null;
            for (String token : addpropertytokens) {
                String[] param = token.split("=",2);
                switch (param[0]) {
                    case "name":
                        name = param[1];
                        break;
                    case "value":
                        value = param[1];
                        break;
                    case "description":
                        description = param[1];
                        break;
                    default:
                        actionReport.appendMessage(param[0] + " is not a valid property.\n"); 
                        actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
                }
            }

            if (null != name && null != value) {
                Property property = monitoringConfig.createChild(Property.class);
                property.setName(name);
                property.setValue(value);
                
                if (null != description) {
                    property.setDescription(description);
                }
                
                attributes.add(property);
            }
        }         
    }

}
