package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Inject;
import org.glassfish.api.admin.*;
import org.glassfish.elasticity.config.serverbeans.AlertConfig;
import org.glassfish.elasticity.config.serverbeans.Alerts;
import org.glassfish.elasticity.config.serverbeans.ElasticService;
import org.glassfish.elasticity.config.serverbeans.ElasticServices;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/14/11
 */
@Service(name = "list-alerts")
@I18n("list.alerts")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({ @RestEndpoint(configBean = Alerts.class, opType = RestEndpoint.OpType.GET, path = "list-alerts", description = "List alerts") })
public class ListAlertsCommand implements AdminCommand {
    @Inject(optional = true)
    ElasticServices elasticServices;

   @Param(name="service")
   String servicename;

    private static final String EOL = "\n";

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        if (elasticServices == null)   {
            //elasticity element doesn't exist
            String msg = Strings.get("elasticity.not.found", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        ElasticService elasticService= elasticServices.getElasticService(servicename);
        if (elasticService == null) {
            //service doesn't exist
            String msg = Strings.get("noSuchService", servicename);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }
        Alerts alerts = elasticService.getAlerts();

        StringBuilder sb = new StringBuilder();
        boolean firstName =true;
        for (AlertConfig alertList: alerts.getAlert())  {
            if ( firstName)
                firstName = false;
             else
                sb.append(EOL);
            sb.append(alertList.getName());
        }
        report.setMessage(sb.toString());
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

}
