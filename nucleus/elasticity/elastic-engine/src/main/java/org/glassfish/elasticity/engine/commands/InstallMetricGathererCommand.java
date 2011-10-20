package org.glassfish.elasticity.engine.commands;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.config.serverbeans.*;
import org.glassfish.elasticity.engine.container.ElasticServiceManager;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import java.beans.PropertyVetoException;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 10/19/11
 */
@Service(name="install-metric-gatherer")
@I18n("install.metric.gatherers")
@Scoped(PerLookup.class)
public class InstallMetricGathererCommand implements AdminCommand{

   @Inject
   Domain domain;

   @Inject
   ElasticServices elasticServices;

  @Param(name="name", primary = true)
   String name;

  @Param(name="service")
  String servicename;

    @Param(name="collection-rate", optional = true)
    int collectionRate;

    @Param(name="auto-start", defaultValue = "false", optional = true)
    boolean autoStart;

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
        // make sure the name of the metric gatherer is unique within this service
        if (elasticService.getMetricGatherers().getMetricGatherer(name) != null) {
            String msg = Strings.get("metricGathererNameExists", name);
            logger.warning(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(msg);
            return;
        }

        // find the metrics gatherer and then add it - should be an hk2 service(?)
        try {
            createMetricGathererElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.create.metric.gatherer " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }


    }

        public void createMetricGathererElement(final String alertName) throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode() {
            @Override
            public Object run(ConfigBeanProxy param) throws PropertyVetoException, TransactionFailure {
                // get the transaction
                Transaction t = Transaction.getTransaction(param);
                if (t != null) {
                    ElasticService elasticService = elasticServices.getElasticService(servicename);
                    if (elasticService != null) {
                        ElasticService writeableService = t.enroll(elasticService);
                        MetricGatherers writeableMGs = elasticService.getMetricGatherers();
                        if (writeableMGs == null)
                            writeableMGs = writeableService.createChild(MetricGatherers.class);
                        else
                            writeableMGs = t.enroll(writeableMGs);

                        MetricGatherer writeableGatherer = writeableMGs.createChild(MetricGatherer.class);
                        if (name != null)
                            writeableGatherer.setName(name);
                        if (autoStart)
                            writeableGatherer.setAutoStart(autoStart);
                        if (collectionRate > 0)
                            writeableGatherer.setCollectionRate(collectionRate);

                        writeableMGs.getMetricGatherer().add(writeableGatherer);
                        writeableService.setMetricGatherers(writeableMGs);
                    }
                }
                return Boolean.TRUE;
            }

        }, domain);
    }
}
