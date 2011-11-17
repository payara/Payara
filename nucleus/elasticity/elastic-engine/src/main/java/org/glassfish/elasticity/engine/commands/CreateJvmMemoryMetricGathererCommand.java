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
@Service(name="create-jvm-memory-metric-gatherer")
@I18n("create.jvm-memory.metric.gatherers")
@Scoped(PerLookup.class)
public class CreateJvmMemoryMetricGathererCommand implements AdminCommand{

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

    @Param(name="retain-data", optional = true)
    int retainData;

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
        if (elasticService.getMetricGatherers().getMetricGatherer(name) == null) {
             try {
                 createMetricGathererElement(name);
            } catch(TransactionFailure e) {
                logger.warning("failed.to.create.metric.gatherer " + name);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(e.getMessage());
             }
         } //if already exists then it's an error
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
