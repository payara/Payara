package org.glassfish.elasticity.engine.commands;

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.paas.tenantmanager.entity.Tenant;
import javax.inject.Inject;
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

    private  TenantManagerEx tm;
    @Inject
     Habitat habitat;

    Elastic elastic=null;

    Tenant tenant;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        tm = habitat.getComponent(TenantManagerEx.class);
        tm.setCurrentTenant("t1");
        Tenant tenant = tm.get(Tenant.class);

//        elastic =  tenant.getExtensionByType (Elastic.class);
        try {

            createESElement();
         } catch(TransactionFailure e) {
            e.printStackTrace();
        }
        }

        public void createESElement() throws TransactionFailure {

//         TenantServices services = tenant.getServices();
        try {
            ConfigSupport.apply(new SingleConfigCode<Tenant>() {
                @Override
                public Object run(Tenant tenant) throws TransactionFailure {
                    
//                     //Commented out next three lines to fix build issues
                    
//                     Elastic es = tenant.createChild(Elastic.class);
//                    tenant.getExtensions().add(es);
//                    return es;
                    return null;
                }
            }, tenant);
        } catch (TransactionFailure e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
