/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admin.monitor.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.admin.amx.config.AMXConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Susan Rai
 */
@Service(name = "set-monitoring-service-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.monitoring.service.configuration")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-monitoring-service-configuration",
            description = "Set monitoring Service Configuration")
})
public class SetMonitoringServiceConfiguration implements AdminCommand {

    @Param(name = "enabled", optional = true)
    private Boolean enabled;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;

    @Param(name = "mbeansenabled", optional = true, alias = "mbeansEnabled")
    private Boolean mbeansEnabled;

    @Param(name = "amxenabled", optional = true, alias = "amxEnabled")
    private Boolean amxEnabled;

    @Inject
    protected Target targetUtil;

    @Inject
    ServiceLocator serviceLocator;

    @Inject
    protected Logger logger;

    private MonitoringService monitoringService;

    private Config config;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        config = targetUtil.getConfig(target);
        if (config != null) {
            monitoringService = config.getMonitoringService();
        } else {
            actionReport.setMessage("Cound not find target: " + target);
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<MonitoringService>() {
                @Override
                public Object run(final MonitoringService monitoringServiceProxy) throws PropertyVetoException, TransactionFailure {
                    if (enabled != null) {
                        monitoringServiceProxy.setMonitoringEnabled(String.valueOf(enabled));
                    }

                    if (mbeansEnabled != null) {
                        monitoringServiceProxy.setMbeanEnabled(String.valueOf(mbeansEnabled));
                    }

                    if (amxEnabled != null) {
                        AMXConfiguration amxConfiguration = config.getExtensionByType(AMXConfiguration.class);
                        ConfigSupport.apply(new SingleConfigCode<AMXConfiguration>() {
                            @Override
                            public Object run(final AMXConfiguration amxConfigurationProxy) throws PropertyVetoException, TransactionFailure {
                                amxConfigurationProxy.setEnabled((String.valueOf(amxEnabled)));
                                return amxConfigurationProxy;
                            }
                        }, amxConfiguration);
                    }

                    actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return monitoringServiceProxy;
                }
            }, monitoringService);
        } catch (TransactionFailure ex) {
            logger.log(Level.WARNING, "Falied to excute the command " + "set-monitoring-service-configuration: " + ex.getCause().getMessage());
            actionReport.setMessage(ex.getCause().getMessage());
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
    }
}
