/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.asadmin.recorder.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.asadmin.recorder.AsadminRecorderConfiguration;
import java.beans.PropertyVetoException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author Andrew Pielage
 */
@Service(name = "enable-asadmin-recorder")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("enable.asadmin.recorder")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "enable-asadmin-recorder",
            description = "Enables the asadmin command recorder service")
})
public class EnableAsadminRecorder implements AdminCommand {
    @Inject
    AsadminRecorderConfiguration asadminRecorderConfiguration;
    
    @Override
    public void execute(AdminCommandContext context) {
        try {
            ConfigSupport.apply(new 
                    SingleConfigCode<AsadminRecorderConfiguration>() {
                public Object run(AsadminRecorderConfiguration 
                        asadminRecorderConfigurationProxy) 
                        throws PropertyVetoException, TransactionFailure {
                    
                    if (asadminRecorderConfiguration.isEnabled()) {
                        Logger.getLogger(EnableAsadminRecorder.class.getName())
                                .log(Level.INFO, 
                                        "Asadmin Recorder already enabled");                       
                    } else {
                        asadminRecorderConfigurationProxy.setEnabled(true);
                        Logger.getLogger(EnableAsadminRecorder.class.getName())
                                .log(Level.INFO, 
                                        "Asadmin Recorder enabled");
                    }
                    
                    return null;
                }
            }, asadminRecorderConfiguration);          
        } catch (TransactionFailure ex) {
            Logger.getLogger(EnableAsadminRecorder.class.getName()).
                    log(Level.SEVERE, null, ex);
        }
    }
    
}
