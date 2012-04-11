package org.glassfish.elasticity.metrics.commands;

import org.glassfish.elasticity.api.ElasticEngine;
import org.glassfish.elasticity.config.serverbeans.*;

import java.beans.PropertyVetoException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.*;
import java.util.logging.Logger;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/10/12
 */
@Service(name = "create-jvm-alert")
@I18n("create.jvm.alert")
@Scoped(PerLookup.class)
@ExecuteOn({RuntimeType.DAS})
@RestEndpoints({ @RestEndpoint(configBean = AlertConfig.class, opType = OpType.POST, path = "create-jvm-alert", description = "Create JVM alert") })
public class CreateJVMAlertCommand implements AdminCommand{

  @Inject
    TenantManager tenantManager;

  @Inject
  ElasticEngine elasticEngine;

  @Param(name="name", primary = true)
   String name;

  @Param(name="environment")
  String envname;

  @Param(name="schedule", optional = true)
  String schedule;

  @Param(name="sampleinterval", optional = true)
  int sampleInterval;

  @Param(name="enabled", defaultValue = "true", optional = true)
  boolean enabled;

   String serviceName = "gf-service-";

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        tenantManager.setCurrentTenant("t1");
        tenant = tenantManager.get(Tenant.class);
         System.out.println("tenant " +tenant.getName());
        TenantServices ts = tenant.getServices();
        elastic =  (Elastic)ts.getServiceByType (Elastic.class);
        if (elastic == null) {
            System.out.println("Elastic needs to be created");
            return;
        }
        serviceName = serviceName+envname;

        try {
            createAlertElement(name);
        } catch(TransactionFailure e) {
            logger.warning("failed.to.create.alert " + name);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        }

        //notify elastic container to run this alert
        ElasticAlerts ea = elastic.getElasticAlerts();
//        AlertConfig alert  = elasticServices.getElasticService(serviceName).getAlerts().getAlert(name);
//        elasticEngine.getElasticEnvironment(envname).addAlert(ea);
    }

    public void createAlertElement(final String alertName) throws TransactionFailure {
         TenantServices ts = tenant.getServices();
        elastic =  (Elastic)ts.getServiceByType (Elastic.class);
        ConfigSupport.apply(new SingleConfigCode<Elastic>() {
            @Override
            public Object run(Elastic elastic) throws PropertyVetoException, TransactionFailure {
                    ElasticAlerts alert = elastic.createChild(ElasticAlerts.class);
                    if (name != null)
                        alert.setName(name);
                    if (schedule != null)
                        alert.setSchedule(schedule);
                    if (sampleInterval != 0)
                        alert.setSampleInterval(sampleInterval);
                    if (!enabled)
                        alert.setEnabled(enabled);
                    alert.setType("jvm_memory");
                    elastic.setElasticAlerts(alert);
                    return elastic;
            }

        }, elastic);
    }
}
