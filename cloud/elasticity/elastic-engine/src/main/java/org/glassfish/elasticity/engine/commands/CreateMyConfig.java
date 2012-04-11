package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.elasticity.api.ElasticEngine;
import org.glassfish.elasticity.config.serverbeans.ElasticAlerts;
import org.glassfish.paas.tenantmanager.api.TenantManager;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import javax.inject.Inject;

import org.glassfish.paas.tenantmanager.entity.TenantService;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.component.*;
import org.glassfish.elasticity.config.serverbeans.Elastic;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.paas.tenantmanager.impl.TenantManagerEx;
import org.jvnet.hk2.component.Habitat;

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/8/12
 * Time: 8:43 PM
 * To change this template use File | Settings | File Templates.
 */
@Service(name="create-elastic")
@Scoped(PerLookup.class)
public class CreateMyConfig implements AdminCommand{

    @Inject
    TenantManager tm;

   @Inject
   ElasticEngine elasticEngine;

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        tm.setCurrentTenant("t1");
        tenant = tm.get(Tenant.class);
         System.out.println("tenant " +tenant.getName());
        TenantServices ts = tenant.getServices();
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
        if (elastic != null) {
            System.out.println("Elastic element already exists");
            return;
        }
        try {

            createESElement();
         } catch(TransactionFailure e) {
            e.printStackTrace();
        }
        elastic =  (Elastic)ts.getServiceByType(Elastic.class);
         ElasticAlerts ea = elastic.getElasticAlerts();

        elasticEngine.getElasticEnvironment("SimpleSessionDemo").addAlert(ea);
        }

        public void createESElement() throws TransactionFailure {

        TenantServices services = tenant.getServices();
        try {
            ConfigSupport.apply(new SingleConfigCode<TenantServices>() {
                @Override
                public Object run(TenantServices tenantServices) throws TransactionFailure {
                    
                    Elastic es = tenantServices.createChild(Elastic.class);

                    ElasticAlerts alerts=es.createChild((ElasticAlerts.class));
                    alerts.setName(("alert1"));
                    alerts.setSchedule("10s");
                    alerts.setType("jvm_memory");
                    es.setElasticAlerts(alerts);
                    tenantServices.getTenantServices().add(es);
                    return tenantServices;
                }
            }, services);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
