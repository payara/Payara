package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.elasticity.api.MetricGatherer;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
//import org.glassfish.elasticity.config.serverbeans.ElasticService;
//import org.glassfish.elasticity.config.serverbeans.ElasticServices;
//import org.glassfish.elasticity.config.serverbeans.MetricGatherer;
//import org.glassfish.elasticity.config.serverbeans.MetricGatherers;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/27/11
 */
@Service(name="list-metric-gatherers")
@I18n("list.metric.gatherers")
@Scoped(PerLookup.class)
public class ListMetricGatherersCommand  implements AdminCommand {

    @Inject
    org.glassfish.elasticity.api.MetricGatherer[] metricGatherers;

    @Param(name="service")
    String servicename;

    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        // Look for the Metric Gatherer services and list them
        // Eventually want to list if they are running, for now they are
        StringBuilder sb = new StringBuilder();
        boolean firstName =true;

        for (MetricGatherer mg : metricGatherers) {
            String metricName = mg.getClass().getAnnotation(Service.class).toString();
            if ( firstName)
                firstName = false;
             else
                sb.append(EOL);
             int nameIndex = metricName.indexOf("name=") + 5;
            int endNameIndex = metricName.indexOf(",");
            sb.append(metricName.substring(nameIndex, endNameIndex));

        }

        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        }
}