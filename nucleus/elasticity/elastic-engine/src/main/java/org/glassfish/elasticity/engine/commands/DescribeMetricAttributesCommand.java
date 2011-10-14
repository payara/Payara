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
import org.glassfish.elasticity.metric.MetricAttribute;
import org.glassfish.elasticity.metric.MetricNode;
import org.glassfish.elasticity.metric.TabularMetricAttribute;
import org.glassfish.elasticity.util.TabularMetricHolder;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.util.List;
import java.util.logging.Logger;
/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 9/27/11
 * */
 @Service(name="describe-metric-attributes")
@I18n("describe.metric.attributes")
@Scoped(PerLookup.class)
public class DescribeMetricAttributesCommand implements AdminCommand{

    @Inject
    org.glassfish.elasticity.api.MetricGatherer[] metricGatherers;

    @Inject
    Habitat habitat;

    @Param(name="service")
    String servicename;

    @Param(name="metricGatherer", primary = true)
    String metricGatherer;

    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        // Look for the Metric Gatherer services and get the attribute for the
        StringBuilder sb = new StringBuilder();
        boolean firstName =true;

        MetricNode metricNode = habitat.getComponent(MetricNode.class, metricGatherer);

        MetricAttribute[] metricAttribute=metricNode.getAttributes();
        for(int i=0;i < metricAttribute.length; i++ ) {
                    if ( firstName)
                     firstName = false;
                    else
                        sb.append(EOL);
                     sb.append(metricAttribute[i].getName());

            // if this attribute is an instance of TabularMetricAttribute then it has attributes so need to get those too
            if ( metricAttribute[i] instanceof TabularMetricAttribute ) {
                boolean firstAttribute=true;
                String[] columnNames = ((TabularMetricHolder)metricAttribute[i]).getColumnNames();
                for (int k=0; k<columnNames.length;k++){
                    if (firstAttribute)  {
                        sb.append(" (");
                        firstAttribute = false;
                    }
                    sb.append(columnNames[k]+ ", ");

                }
                int lastComa=sb.lastIndexOf(",");
                sb.deleteCharAt(lastComa);
                sb.append(")"+EOL);
            }

        }

        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        }

}