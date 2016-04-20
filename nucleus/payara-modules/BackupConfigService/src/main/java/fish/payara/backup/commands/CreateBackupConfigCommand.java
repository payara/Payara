/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.backup.commands;

import fish.payara.backup.service.BackupConfigConfiguration;
import java.beans.PropertyVetoException;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Daniel
 */
@Service(name="create-backup-config")
@PerLookup
public class CreateBackupConfigCommand implements AdminCommand{

    @Inject
    BackupConfigConfiguration config;
    
    @Param(name="test-param", optional=false, shortName="l")
    private String testparam;
            
    @Param(name="daniel", optional=true)
    private String param2;
            
    @Override
    public void execute(AdminCommandContext context) {
        
        try {
            ConfigSupport.apply(new SingleConfigCode<BackupConfigConfiguration>(){
                public Object run(BackupConfigConfiguration configProxy)
                    throws PropertyVetoException, TransactionFailure{
                    if (testparam != null){
                        configProxy.setMinutes(testparam);
                    }
                    return null;
                }
            },config);
        }catch (TransactionFailure ex){
            System.out.println("The transaction has failed "+ ex);
        }
        
        
        System.out.println(testparam);
        System.out.println(param2);
    }
    
    
}
