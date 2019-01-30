package fish.payara.nucleus.healthcheck.admin;

import java.beans.PropertyVetoException;
import java.util.Properties;
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
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
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
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;

import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.HealthCheckServiceConfiguration;
import fish.payara.nucleus.notification.TimeUtil;
import fish.payara.nucleus.notification.configuration.NotificationServiceConfiguration;
import fish.payara.nucleus.notification.configuration.NotifierConfiguration;
import fish.payara.nucleus.notification.log.LogNotifierConfiguration;

@Service(name = "set-healthcheck-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("healthcheck.configure")
@ExecuteOn({RuntimeType.DAS,RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = HealthCheckServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-healthcheck-configuration",
            description = "Enables/Disables Health Check Service")
})
public class SetHealthCheckConfiguration implements AdminCommand {

    @Inject
    ServerEnvironment server;

    @Inject
    protected Logger logger;

    @Inject
    HealthCheckService service;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "enabled")
    private boolean enabled;

    @Param(name = "historicalTraceEnabled", optional = true)
    private Boolean historicalTraceEnabled;

    @Param(name = "historicalTraceStoreSize", optional = true, defaultValue = "20")
    @Min(value = 1, message = "Store size must be greater than 0")
    private int historicalTraceStoreSize;

    @Param(name = "historicalTraceStoreTimeout", optional = true)
    private String historicalTraceStoreTimeout;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = initActionReport(context);

        Config targetConfig = targetUtil.getConfig(target);
        final HealthCheckServiceConfiguration config = targetConfig.getExtensionByType(HealthCheckServiceConfiguration.class);
        if (config != null) {
            updateConfig(actionReport, config);
        }
        if (dynamic && (!server.isDas() || targetUtil.getConfig(target).isDas())) {
            configureDynamically();
        }
        enableLogNotifier(context);
    }

    private static ActionReport initActionReport(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        if (report.getExtraProperties() == null) {
            report.setExtraProperties(new Properties());
        }
        return report;
    }

    private void updateConfig(final ActionReport actionReport, final HealthCheckServiceConfiguration config) {
        try {
            ConfigSupport.apply(proxy -> {
                    proxy.enabled(String.valueOf(enabled));
                    proxy.setHistoricalTraceStoreSize(String.valueOf(historicalTraceStoreSize));
                    if (historicalTraceEnabled != null) {
                        proxy.setHistoricalTraceEnabled(historicalTraceEnabled.toString());
                    }
                    if (historicalTraceStoreTimeout != null) {
                        proxy.setHistoricalTraceStoreTimeout(historicalTraceStoreTimeout.toString());
                    }
                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return proxy;
                }, config);
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Exception during command ", ex);
            actionReport.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

    private void enableLogNotifier(AdminCommandContext context) {
        CommandRunner runner = serviceLocator.getService(CommandRunner.class);
        ActionReport subReport = context.getActionReport().addSubActionsReport();

        CommandRunner.CommandInvocation inv = runner.getCommandInvocation("healthcheck-log-notifier-configure", subReport, context.getSubject());

        ParameterMap params = new ParameterMap();
        params.add("dynamic", String.valueOf(dynamic));
        params.add("target", target);
        params.add("enabled", String.valueOf(enabled));
        Config config = targetUtil.getConfig(target);
        if (config == null) {
            subReport.setMessage("No such config named: " + target);
            subReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        String noisy = "true";
        NotificationServiceConfiguration configuration = config.getExtensionByType(NotificationServiceConfiguration.class);
        NotifierConfiguration notifierConfiguration = configuration.getNotifierConfigurationByType(LogNotifierConfiguration.class);
        noisy = notifierConfiguration.getNoisy();
        params.add("noisy", noisy);
        inv.parameters(params);
        inv.execute();
        // swallow the offline warning as it is not a problem
        if (subReport.hasWarnings()) {
            subReport.setMessage("");
        }
    }

    private void configureDynamically() {
        service.setEnabled(enabled);
        service.setHistoricalTraceStoreSize(historicalTraceStoreSize);
        if (historicalTraceEnabled != null) {
            service.setHistoricalTraceEnabled(historicalTraceEnabled);
        }
        if (historicalTraceStoreTimeout != null) {
            service.setHistoricalTraceStoreTimeout(TimeUtil.setStoreTimeLimit(this.historicalTraceStoreTimeout));
        }
    }
}
