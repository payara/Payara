package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import com.sun.enterprise.util.StringUtils;
import fish.payara.nucleus.notification.configuration.LogNotifier;
import fish.payara.nucleus.notification.configuration.Notifier;
import fish.payara.nucleus.notification.service.BaseNotifierService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
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

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to list the names of all available health check services
 *
 * @author mertcaliskan
 */

@Service(name = "get-requesttracing-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("get.requesttracing.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.GET,
                path = "get-requesttracing-configuration",
                description = "List Request Tracing Configuration")
})
public class GetRequestTracingConfiguration  implements AdminCommand {

    final static String notifiersHeaders[] = {"Name", "Service Name", "Enabled"};

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
        ActionReport notifiersActionReport = mainActionReport.addSubActionsReport();
        ColumnFormatter notifiersColumnFormatter = new ColumnFormatter(notifiersHeaders);

        RequestTracingServiceConfiguration configuration = config.getExtensionByType(RequestTracingServiceConfiguration.class);
        List<ServiceHandle<BaseNotifierService>> allNotifierHandles = habitat.getAllServiceHandles(BaseNotifierService.class);

        mainActionReport.appendMessage("Request Tracing Service is enabled?: " + configuration.getEnabled() + "\n");
        mainActionReport.appendMessage("Request Tracing Threshold Value: " + configuration.getThresholdValue() + " " + configuration.getThresholdUnit() + "\n");
        mainActionReport.appendMessage("Below are the list of notifier details listed by name.");
        mainActionReport.appendMessage(StringUtils.EOL);

        for (ServiceHandle<BaseNotifierService> notifierHandle : allNotifierHandles) {
            Notifier notifier = configuration.getNotifierByType(notifierHandle.getService().getNotifierType());

            if (notifier instanceof LogNotifier) {
                LogNotifier logNotifier = (LogNotifier) notifier;

                Object values[] = new Object[3];
                values[0] = notifierHandle.getService().getType().toString();
                values[1] = notifierHandle.getActiveDescriptor().getName();
                values[2] = logNotifier.getEnabled();
                notifiersColumnFormatter.addRow(values);
            }
        }
        if (!notifiersColumnFormatter.getContent().isEmpty()) {
            notifiersActionReport.setMessage(notifiersColumnFormatter.toString());
            notifiersActionReport.appendMessage(StringUtils.EOL);
        }

        mainActionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
