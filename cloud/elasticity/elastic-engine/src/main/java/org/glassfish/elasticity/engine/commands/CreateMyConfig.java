package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.admin.AdminCommand;
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

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        tm.setCurrentTenant("t1");
        tenant = tm.get(Tenant.class);
         System.out.println("tenant" +tenant.getName());
//        elastic =  (Elastic)tenant.getTenantServices (Elastic.class);

        try {

            createESElement();
         } catch(TransactionFailure e) {
            e.printStackTrace();
        }
        }

        public void createESElement() throws TransactionFailure {

        try {
            ConfigSupport.apply(new SingleConfigCode() {
                @Override
                public Object run(ConfigBeanProxy param) throws TransactionFailure {
                    
                    Transaction t = Transaction.getTransaction(param);
                    TenantServices tenantServices =((Tenant)param).getServices();

                    t.enroll(tenantServices);

                     Elastic es = tenantServices.createChild(Elastic.class);
                    tenantServices.getTenantServices().add(es);
                    return es;
                }
            }, tenant);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
