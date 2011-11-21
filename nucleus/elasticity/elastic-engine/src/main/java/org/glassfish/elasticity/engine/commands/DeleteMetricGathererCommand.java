package org.glassfish.elasticity.engine.commands;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;
import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import org.glassfish.elasticity.config.serverbeans.*;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 11/18/11
 */
@Service(name="delete-metric-gatherer")
@I18n("delete.metric.gatherer")
@Scoped(PerLookup.class)
public class DeleteMetricGathererCommand implements AdminCommand{

     @Inject
     Domain domain;

    @Inject
     ElasticServices elasticServices;

    @Param(name="name", primary = true)
     String name;

    @Param(name="service")
    String servicename;

     @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        if (elasticServices == null)   {
            //service doesn't exist
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
        if (elasticService.getMetricGatherers().getMetricGatherer(name) != null) {
             try {
                 deleteMetricGathererElement(name);
            } catch(TransactionFailure e) {
                logger.warning("failed.to.delete.metric.gatherer " + name);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(e.getMessage());
             }
         } else {
            logger.warning("failed.to.delete.metric.gatherer " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("metric.gatherer.not.found"+ name);

        }
    }
         public void deleteMetricGathererElement(final String name) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t != null) {
                    ElasticService elasticService = elasticServices.getElasticService(servicename);
                    if (elasticService != null) {
                        MetricGatherers writeableMG = elasticService.getMetricGatherers();
                        if (writeableMG != null) {
                            writeableMG = t.enroll(writeableMG);
                            MetricGatherer mg = writeableMG.getMetricGatherer(name);
                            writeableMG.getMetricGatherer().remove(mg);
                        }
                        //nothing to delete

                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
