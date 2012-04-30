package org.glassfish.elasticity.engine.util;

import org.glassfish.elasticity.api.RootElementFinder;
import org.glassfish.elasticity.config.serverbeans.Elastic;
import org.glassfish.elasticity.config.serverbeans.ElasticAlert;
import org.glassfish.elasticity.config.serverbeans.ElasticAlerts;
import org.glassfish.elasticity.config.serverbeans.MetricGatherers;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.api.TenantScoped;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.glassfish.paas.tenantmanager.impl.TenantManagerEx;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import javax.inject.Inject;
import org.jvnet.hk2.component.Habitat;


/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/24/12
 */
@Scoped(PerLookup.class)
@Service(name="CPAS")
public class ElasticCpasParentFinder implements RootElementFinder{

  @Inject
  TenantManager tenantManager;

  Tenant tenant;

    @Override
    public ElasticAlerts getAlertsParent(String tenantid) {
         tenantManager.setCurrentTenant(tenantid);
        tenant = tenantManager.get(Tenant.class);
        TenantServices ts = tenant.getServices();
         Elastic    elastic =  (Elastic)ts.getServiceByType(Elastic.class);
         ElasticAlerts ea = elastic.getElasticAlerts();

        return ea;
    }

    @Override
    public ElasticAlert addAlertElement(ElasticAlerts elasticAlerts)throws TransactionFailure
    {


        try {
            tenantManager.executeUpdate(new SingleConfigCode<ElasticAlerts>() {
                @Override
                public Object run(ElasticAlerts eAlerts) throws TransactionFailure {

                    ElasticAlert alert = eAlerts.createChild(ElasticAlert.class);
                    alert.setName(("alert1"));
                    alert.setSchedule("10s");
                    alert.setType("jvm_memory");
                    eAlerts.getElasticAlert().add(alert);
                    return eAlerts;
                }
            }, elasticAlerts);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return elasticAlerts.getElasticAlert("alert1");
    }
}
