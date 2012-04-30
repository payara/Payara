package org.glassfish.elasticity.metrics.commands;

import org.glassfish.elasticity.api.ElasticEngine;
import org.glassfish.elasticity.api.RootElementFinder;
import org.glassfish.elasticity.config.serverbeans.*;
import org.glassfish.elasticity.engine.util.ElasticCpasParentFinder;

import java.beans.PropertyVetoException;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.Services;
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

//  import javax.inject.*;

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

   @Inject
   Services services;

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

  @Param(name="tenantid")
  String tenantid;

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        tenantManager.setCurrentTenant(tenantid);
        /*
        tenant = tenantManager.get(Tenant.class);
        TenantServices ts = tenant.getServices();
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
        if (elastic != null) {
            System.out.println("Elastic element already exists");
            return;
        }
        */
        RootElementFinder elasticParentFinder = services.forContract(RootElementFinder.class).named("CPAS").get();
        ElasticAlerts elasticAlerts = elasticParentFinder.getAlertsParent(tenantid );
        ElasticAlert alert = null;
        try {
             alert = elasticParentFinder.addAlertElement(elasticAlerts);

//            createESElement();
         } catch(TransactionFailure e) {
            e.printStackTrace();
        }
        /*
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
         ElasticAlerts ea = elastic.getElasticAlerts();
        ElasticAlert alert = ea.getElasticAlert(name);
 */
        elasticEngine.getElasticEnvironment(envname).addAlert(alert);
        }

  private static class MyCode implements  SingleConfigCode<ElasticAlerts> {
                @Override
                public Object run(final ElasticAlerts eAlerts) throws TransactionFailure {

                    ElasticAlert alert = eAlerts.createChild(ElasticAlert.class);
                    alert.setName(("alert1"));
                    alert.setSchedule("10s");
                    alert.setType("jvm_memory");
                    eAlerts.getElasticAlert().add(alert);
                    return eAlerts;
                }
        }
}
