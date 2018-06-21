/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.jmx.monitoring.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.admin.amx.config.AMXConfiguration;
import fish.payara.jmx.monitoring.JMXMonitoringService;
import fish.payara.jmx.monitoring.configuration.MonitoredAttribute;
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

/**
 * Asadmin command to set the JMX monitoring service's configuration.
 * 
 * @since 5.183
 * @author savage
 */
@Service(name = "set-jmx-monitoring-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.jmx.monitoring.configuration")
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-jmx-monitoring-configuration",
            description = "Set JMX Monitoring Configuration")
})
public class SetJMXMonitoringConfiguration implements AdminCommand {

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Inject
    JMXMonitoringService monitoringService;

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    /**
     *
     * @deprecated Since 5.182. Use set-amx-enabled command instead.
     */
    @Deprecated
    @Param(name = "amx", optional = true)
    private Boolean amx;

    @Param(name = "logfrequency", optional = true)
    private String logfrequency;

    @Param(name = "logfrequencyunit", optional = true, acceptableValues = "NANOSECONDS,MILLISECONDS,SECONDS,MINUTES,HOURS,DAYS")
    private String logfrequencyunit;

    @Param(name = "addattribute", optional = true, multiple = true, alias = "addproperty")
    private List<String> attributesToAdd;

    @Param(name = "delattribute", optional = true, multiple = true, alias = "delproperty")
    private List<String> attributesToRemove;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Inject
    protected Logger logger;

    private MonitoringServiceConfiguration monitoringConfig;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Config config = targetUtil.getConfig(target);

        final JMXMonitoringService service = serviceLocator.getService(JMXMonitoringService.class);
        if (service == null) {
            actionReport.appendMessage("Could not find a monitoring service.");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        monitoringConfig = config.getExtensionByType(MonitoringServiceConfiguration.class);
        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringServiceConfiguration>() {
                @Override
                public Object run(final MonitoringServiceConfiguration monitoringConfigProxy) throws PropertyVetoException, TransactionFailure {
                    updateConfiguration(monitoringConfigProxy);
                    updateAttributes(monitoringConfigProxy, actionReport);
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return monitoringConfigProxy;
                }
            }, monitoringConfig);

            if (dynamic) {
                enableOnTarget(actionReport, context, enabled);
            }

        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Falied to excute the command " + "set-jmx-monitoring-configuration: " + ex.getCause().getMessage());
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

    }

    /**
     * Updates the configuration attributes for the
     * MonitoringServiceConfiguration given to it.
     *
     * @param monitoringConfig
     * @throws PropertyVetoException
     */
    private void updateConfiguration(MonitoringServiceConfiguration monitoringConfig) throws PropertyVetoException, TransactionFailure {

        if (null != enabled) {
            monitoringConfig.setEnabled(String.valueOf(enabled));
        }
        if (null != amx) {
            AMXConfiguration amxConfig = serviceLocator.getService(AMXConfiguration.class);
            ConfigSupport.apply(new SingleConfigCode<AMXConfiguration>() {
                @Override
                public Object run(final AMXConfiguration amxConfigProxy) throws PropertyVetoException, TransactionFailure {
                    amxConfigProxy.setEnabled((String.valueOf(amx)));
                    return amxConfigProxy;
                }
            }, amxConfig);
            monitoringConfig.setAmx(null);
        }
        if (null != logfrequency) {
            monitoringConfig.setLogFrequency(String.valueOf(logfrequency));
        }
        if (null != logfrequencyunit) {
            monitoringConfig.setLogFrequencyUnit(logfrequencyunit);
        }
    }

    /**
     *
     * @param actionReport
     * @param context
     * @param enabled
     */
    private void enableOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();
        CommandRunner.CommandInvocation invocation;

        if (target.equals("server-config")) {
            invocation = runner.getCommandInvocation("__enable-jmx-monitoring-service-on-das", subReport, context.getSubject());
        } else {
            invocation = runner.getCommandInvocation("__enable-jmx-monitoring-service-on-instance", subReport, context.getSubject());
        }

        // Build the parameters
        ParameterMap params = new ParameterMap();
        enabled = Boolean.valueOf(monitoringConfig.getEnabled());
        params.add("enabled", enabled.toString());
        params.add("target", target);
        invocation.parameters(params);
        invocation.execute();
    }

    /**
     * Updates monitoring attributes through adding a new property and/or
     * deleting an existing one.
     *
     * @param actionReport
     * @param monitoringConfig
     * @throws PropertyVetoException
     * @throws TransactionFailure
     */
    private void updateAttributes(MonitoringServiceConfiguration monitoringConfig, ActionReport report) throws PropertyVetoException, TransactionFailure {
        List<MonitoredAttribute> attributes = monitoringConfig.getMonitoredAttributes();

        // If there are attributes to be removed
        if (attributesToRemove != null && !attributesToRemove.isEmpty()) {
            // Loop through them
            for (String attributeToRemove : attributesToRemove) {
                // Parse the provided attribute
                MonitoredAttribute monitoredAttribute = parseToMonitoredAttribute(attributeToRemove, monitoringConfig.createChild(MonitoredAttribute.class));
                boolean removed = false;
                // Find the attribute matching the one specified
                for (MonitoredAttribute attribute : attributes) {
                    if (attribute.equals(monitoredAttribute)) {
                        attributes.remove(attribute);
                        report.appendMessage(monitoringService.getLocalStringManager().getLocalString(
                                "jmxmonitoring.configure.attribute.remove",
                                "Attribute 'objectName={0} attributeName={1}' successfully deleted.",
                                monitoredAttribute.getObjectName(), monitoredAttribute.getAttributeName()) + "\n");
                        removed = true;
                        break;
                    }
                }
                if (!removed) {
                    report.appendMessage(monitoringService.getLocalStringManager().getLocalString(
                            "jmxmonitoring.configure.attribute.remove.error",
                            "Attribute 'objectName={0} attributeName={1}' doesn't exist, so was ignored.",
                            monitoredAttribute.getObjectName(), monitoredAttribute.getAttributeName()) + "\n");
                }
            }
        }

        if (attributesToAdd != null && !attributesToAdd.isEmpty()) {
            for (String attributeToAdd : attributesToAdd) {
                MonitoredAttribute monitoredAttribute = parseToMonitoredAttribute(attributeToAdd, monitoringConfig.createChild(MonitoredAttribute.class));
                boolean attributeExists = false;
                for (MonitoredAttribute attribute : attributes) {
                    if (attribute.equals(monitoredAttribute)) {
                        attributeExists = true;
                        report.appendMessage(monitoringService.getLocalStringManager().getLocalString(
                                "jmxmonitoring.configure.attribute.add.error",
                                "Attribute 'objectName={0} attributeName={1}' already exists, so was ignored.",
                                monitoredAttribute.getObjectName(), monitoredAttribute.getAttributeName()) + "\n");
                        break;
                    }
                }
                if (!attributeExists) {
                    attributes.add(monitoredAttribute);
                    report.appendMessage(monitoringService.getLocalStringManager().getLocalString(
                            "jmxmonitoring.configure.attribute.add",
                            "Attribute 'objectName={0} attributeName={1}' successfully added.",
                            monitoredAttribute.getObjectName(), monitoredAttribute.getAttributeName()) + "\n");
                }
            }
        }
    }

    /**
     * Produces a
     * {@link fish.payara.jms.monitoring.configuration.MonitoredBean MonitoredAttribute}
     * from a given string. The string should be in the following format:
     * 'attribute=something objectName=something description=something'.
     *
     * @param input the string to be parsed
     * @param property the property to configure as per the input string.
     * @return a constructed Property.
     * @throws PropertyVetoException if a config listener vetoes a property
     * attribute value.
     */
    private MonitoredAttribute parseToMonitoredAttribute(String input, MonitoredAttribute monitoredAttribute) throws PropertyVetoException {
        // Negative lookbehind - find space characters not preceeded by \
        String[] attributeTokens = input.split("(?<!\\\\) ");
        String attributeName = null;
        String objectName = null;
        String description = null;

        if (attributeTokens.length < 2) {
            throw new IllegalArgumentException(monitoringService.getLocalStringManager().getLocalString(
                    "jmxmonitoring.configure.attributes.too.few",
                    "Too few properties. Required properties are 'objectName' and 'attributeName'."));
        }

        for (String token : attributeTokens) {
            token = token.replaceAll("\\\\", "");
            String[] param = token.split("=", 2);
            if (param.length != 2) {
                throw new IllegalArgumentException(monitoringService.getLocalStringManager().getLocalString(
                        "jmxmonitoring.configure.attributes.too.few",
                        "Too few properties. Required properties are 'objectName' and 'attributeName'."));
            }
            switch (param[0]) {
                case "attributeName":
                case "name":
                    attributeName = (param.length == 2) ? param[1] : null;
                    break;
                case "objectName":
                case "value":
                    objectName = (param.length == 2) ? param[1] : null;
                    break;
                case "description":
                    description = (param.length == 2) ? param[1] : null;
                    break;
                default:
                    throw new IllegalArgumentException(monitoringService.getLocalStringManager().getLocalString(
                            "jmxmonitoring.configure.attributes.unknown",
                            "Unknown property: {0}. Valid properties are: 'objectName', 'attributeName' and 'description'.",
                            param[0]));
            }
        }
        if (attributeName == null || attributeName.isEmpty()) {
            throw new IllegalArgumentException(monitoringService.getLocalStringManager().getLocalString(
                    "jmxmonitoring.configure.attributes.invalid",
                    "Invalid property: {0}.",
                    "attributeName"));
        }
        if (objectName == null || objectName.isEmpty()) {
            throw new IllegalArgumentException(monitoringService.getLocalStringManager().getLocalString(
                    "jmxmonitoring.configure.attributes.invalid",
                    "Invalid property: {0}.",
                    "objectName"));
        }

        monitoredAttribute.setAttributeName(attributeName);
        monitoredAttribute.setObjectName(objectName);

        if (description != null) {
            monitoredAttribute.setDescription(description);
        }
        return monitoredAttribute;
    }

}
