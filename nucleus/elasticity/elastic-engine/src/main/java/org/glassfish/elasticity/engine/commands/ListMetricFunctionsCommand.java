package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.glassfish.elasticity.api.MetricFunction;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/14/11
 */
@Service(name="list-metric-functions")
@I18n("list.metric.functions")
@Scoped(PerLookup.class)
public class ListMetricFunctionsCommand implements AdminCommand{

    @Inject
     MetricFunction[] metricFunctions;

    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        // Look for the Metric Functions and list them
        StringBuilder sb = new StringBuilder();
        boolean firstName =true;

        for(MetricFunction mf: metricFunctions)    {
            String metricFunctionName = mf.getClass().getAnnotation(Service.class).toString();
            if ( firstName)
                firstName = false;
             else
                sb.append(EOL);
             int nameIndex = metricFunctionName.indexOf("name=") + 5;
            int endNameIndex = metricFunctionName.indexOf(",");
            sb.append(metricFunctionName.substring(nameIndex, endNameIndex));

        }
        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }


}
