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
import org.glassfish.api.Param;
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
@Service(name = "set-asadmin-recorder-configuration")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("set.asadmin.recorder.configuration")
@ExecuteOn(value = {RuntimeType.DAS})
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-asadmin-recorder-configuration",
            description = "Sets the configuration for the asadmin command "
                    + "recorder service")
})
public class SetAsadminRecorderConfiguration implements AdminCommand {
    @Inject
    AsadminRecorderConfiguration asadminRecorderConfiguration;
    
    @Param(name = "enabled", optional = true)
    private Boolean enabled;
    
    @Param(name = "outputLocation", optional = true)
    private String outputLocation;
    
    @Param(name = "filterCommands", optional = true)
    private Boolean filterCommands;
    
    @Param(name = "filteredCommands", optional = true)
    private String filteredCommands;
    
    @Override
    public void execute(AdminCommandContext context) {
        try {
            ConfigSupport.apply(new 
                    SingleConfigCode<AsadminRecorderConfiguration>() {
                public Object run(AsadminRecorderConfiguration 
                        asadminRecorderConfigurationProxy) 
                        throws PropertyVetoException, TransactionFailure {
                    
                    if (enabled != null) {
                        asadminRecorderConfigurationProxy.setEnabled(enabled);
                    }
                    
                    if (filterCommands != null) {
                        asadminRecorderConfigurationProxy.
                                setFilterCommands(filterCommands);
                    }
                        
                    if (outputLocation != null) {
                        if (!outputLocation.endsWith(".txt")) {
                            outputLocation += ".txt";
                        }
                        asadminRecorderConfigurationProxy.
                                setOutputLocation(outputLocation);
                    }
                    
                    if (filteredCommands != null) {
                        asadminRecorderConfigurationProxy.
                                setFilteredCommands(filteredCommands);
                    }
                    
                    return null;
                }
            }, asadminRecorderConfiguration);          
        } catch (TransactionFailure ex) {
            Logger.getLogger(SetAsadminRecorderConfiguration.class.getName()).
                    log(Level.SEVERE, null, ex);
        }   
    }
}
