/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.notification.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.nucleus.notification.domain.execoptions.NotifierConfigurationExecutionOptionsFactory;
import java.util.Properties;
import javax.inject.Inject;
import static javax.management.Query.value;
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
import org.jvnet.hk2.annotations.Service;

/**
 * Admin command to set notification services configuration
 *
 * @author Susan Rai
 */
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "set-notification")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("set.notification")
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-notification",
            description = "Set notification Services Configuration")
})
public class SetNotification implements AdminCommand {

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "notifierDynamic", optional = true, defaultValue = "false")
    protected Boolean notifierDynamic;

    @Param(name = "notifierEnabled", optional = false)
    private Boolean notifierEnabled;

    @Param(name = "notifierName", optional = true, defaultValue = "service-log")
    private String notifierName;

    @Inject
    ServiceLocator serviceLocator;

    CommandRunner.CommandInvocation inv;

    @Override
    public void execute(AdminCommandContext context) {
        final AdminCommandContext theContext = context;
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }

        if (dynamic || enabled) {
            if (dynamic) {
                notifierDynamic = true;
            } else {
                notifierDynamic = false;
            }
            if (enabled) {
                notifierEnabled = true;
            } else {
                notifierEnabled = false;
            }
            enableNotificationConfigureOnTarget(actionReport, theContext, enabled);
        }

        if (notifierDynamic || notifierEnabled) {
            enableNotificationNotifierConfigurerOnTarget(actionReport, theContext, notifierEnabled);
        }
    }

    private void enableNotificationConfigureOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        if (target.equals("server-config")) {
            inv = runner.getCommandInvocation("notification-configure-das", subReport, context.getSubject());
        } else {
            inv = runner.getCommandInvocation("notification-configure", subReport, context.getSubject());
        }

        ParameterMap params = new ParameterMap();
        params.add("enabled", enabled.toString());
        params.add("target", target);
        params.add("dynamic", dynamic.toString());
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void enableNotificationNotifierConfigurerOnTarget(ActionReport actionReport, AdminCommandContext context, Boolean enabled) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        if (target.equals("server-config")) {
            inv = runner.getCommandInvocation("notification-configure-notifier-das", subReport, context.getSubject());
        } else {
            inv = runner.getCommandInvocation("notification-configure-notifier", subReport, context.getSubject());
        }

        ParameterMap params = new ParameterMap();
        params.add("dynamic", notifierDynamic.toString());
        params.add("target", target);
        params.add("notifierName", notifierName);
        params.add("notifierEnabled", enabled.toString());
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }
}
