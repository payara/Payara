package org.glassfish.elasticity.metrics.commands;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.elasticity.config.serverbeans.Elastic;
import org.glassfish.elasticity.config.serverbeans.ElasticAlert;
import org.glassfish.elasticity.config.serverbeans.ElasticAlerts;
import org.glassfish.paas.tenantmanager.api.TenantManager;
//import javax.inject.*;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import org.glassfish.paas.tenantmanager.entity.TenantServices;
import org.jvnet.hk2.annotations.*;
import org.glassfish.hk2.Services;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.logging.Logger;


/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: 4/26/12
 * Time: 9:50 AM
 * To change this template use File | Settings | File Templates.
 */
@Scoped(PerLookup.class)
@Service (name="create-elastic-element")
public class CreateElasticElementCommand implements AdminCommand{

    @Inject
    TenantManager tenantManager;

    @Param(name="tenantid")
    String tenantid;

    Tenant tenant;
    Elastic elastic=null;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        Logger logger= context.logger;

        tenantManager.setCurrentTenant(tenantid);

        tenant = tenantManager.get(Tenant.class);
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
    }
    public void createESElement() throws TransactionFailure {

        TenantServices services = tenant.getServices();
        try {
            tenantManager.executeUpdate(new SingleConfigCode<TenantServices>() {
                @Override
                public Object run(TenantServices tenantServices) throws TransactionFailure {

                    Elastic es = tenantServices.createChild(Elastic.class);

                    ElasticAlerts alerts=es.createChild((ElasticAlerts.class));
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
