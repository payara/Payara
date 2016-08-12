/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.healthcheck.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import fish.payara.nucleus.healthcheck.HealthCheckService;
import fish.payara.nucleus.healthcheck.configuration.Checker;
import fish.payara.nucleus.healthcheck.preliminary.BaseHealthCheck;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
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
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Susan Rai
 */
@Service(name = "__enable-healthcheck-configure-service-on-instance")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("__enable-healthcheck-configure-service-on-instance")
@ExecuteOn(RuntimeType.INSTANCE)
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.GET,
            path = "__enable-healthcheck-configure-service-on-instance",
            description = "Enables Healthcheck Configure Service on Instance")
})
public class EnableHealthCheckServiceConfigurerOnInstance implements AdminCommand {

    final private static LocalStringManagerImpl strings = new LocalStringManagerImpl(HealthCheckServiceConfigurer.class);

    @Inject
    BaseHealthCheck service;

    @Inject
    HealthCheckService healthCheckService;

    @Inject
    Checker checker;

    @Param(name = "target", optional = true, defaultValue = "server")
    protected String target;

    @Param(name = "enabled", optional = false)
    private Boolean enabled;

    @Param(name = "time", optional = true)
    private String time;

    @Param(name = "unit", optional = true)
    private String unit;

    @Param(name = "serviceName", optional = false)
    private String serviceName;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();
        Properties extraProperties = actionReport.getExtraProperties();
        if (extraProperties == null) {
            extraProperties = new Properties();
            actionReport.setExtraProperties(extraProperties);
        }
        if (service.getOptions() == null) {
            service.setOptions(service.constructOptions(checker));
            healthCheckService.registerCheck(checker.getName(), service);
        }

        if (enabled != null) {
            service.getOptions().setEnabled(enabled);
        }
        if (time != null) {
            service.getOptions().setTime(Long.valueOf(time));
        }
        if (unit != null) {
            service.getOptions().setUnit(TimeUnit.valueOf(unit));
        }

        actionReport.appendMessage(strings.getLocalString("healthcheck.service.configure.status.success",
                "Service status for {0} is set to {1}.", serviceName, enabled));

        healthCheckService.shutdownHealthCheck();
        healthCheckService.bootstrapHealthCheck();
    }
}
