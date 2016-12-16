package fish.payara.nucleus.requesttracing.admin;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.ColumnFormatter;
import fish.payara.nucleus.requesttracing.HistoricRequestEventStore;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.requesttracing.configuration.RequestTracingServiceConfiguration;
import fish.payara.nucleus.requesttracing.domain.HistoricRequestEvent;
import fish.payara.nucleus.requesttracing.domain.execoptions.RequestTracingExecutionOptions;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * @author mertcaliskan
 */
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG})
@Service(name = "list-historic-requesttraces")
@CommandLock(CommandLock.LockType.NONE)
@PerLookup
@I18n("requesttracing.configure")
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class,
                opType = RestEndpoint.OpType.POST,
                path = "list-historic-requesttraces",
                description = "List slowest request traces stored historically.")
})
public class HistoricRequestEventRetriever implements AdminCommand {

    private final String headers[] = {"Elapsed Time", "Traced Message"};

    @Inject
    protected Target targetUtil;

    @Param(name = "dynamic", optional = true, defaultValue = "false")
    protected Boolean dynamic;

    @Param(name = "target", optional = true, defaultValue = "server-config")
    protected String target;

    @Param(name = "first", optional = true)
    private Integer first;

    @Inject
    private RequestTracingService service;

    @Inject
    private HistoricRequestEventStore eventStore;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport actionReport = context.getActionReport();

        RequestTracingExecutionOptions executionOptions = service.getExecutionOptions();
        if (!executionOptions.isHistoricalTraceEnabled()) {
            actionReport.setMessage("Historical Trace is not enabled!");
            actionReport.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        else {
            if (first == null) {
                first = executionOptions.getHistoricalTraceStoreSize();
            }
            HistoricRequestEvent[] traces = eventStore.getTraces(first);

            ColumnFormatter columnFormatter = new ColumnFormatter(headers);
            for (HistoricRequestEvent historicRequestEvent : traces) {
                Object values[] = new Object[2];
                values[0] = historicRequestEvent.getElapsedTime();
                values[1] = historicRequestEvent.getMessage();
                columnFormatter.addRow(values);
            }
            actionReport.setMessage(columnFormatter.toString());
            actionReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        }
    }
}
