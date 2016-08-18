/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates.
 * All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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
                        if (outputLocation.endsWith("/")||outputLocation.endsWith("\\")){
                            outputLocation += "asadmin-commands.txt";
                        }
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
