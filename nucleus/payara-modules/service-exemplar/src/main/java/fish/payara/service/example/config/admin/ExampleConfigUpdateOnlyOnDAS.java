/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
  */
package fish.payara.service.example.config.admin;

import com.sun.enterprise.config.serverbeans.Config;
import fish.payara.service.example.config.ExampleServiceConfiguration;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *  Example command that updates the correct configuration but runs ONLY on the DAS
 *  You can do this style of command if you only want to update the configuration
 *  and you don't care about updating the service dynamically in other running instances
 * 
 *  NOTE: If the service is a ConfigListener it will still be notified on the DAS that
 *  the configuration has changed
 * @author steve
 */
@Service(name = "example-das-command") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run it is mandatory for admin commands
@ExecuteOn(value=RuntimeType.DAS) // this means the command can ONLY run on the DAS
@TargetType(value = {CommandTarget.CONFIG}) // says the target can only be a named configuration
@RestEndpoints({  // creates a REST endpoint needed for integration with the admin interface
    @RestEndpoint(configBean = ExampleServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "example-das-command",
            description = "Updates the configuration only on the DAS but for any configuration")
})
public class ExampleConfigUpdateOnlyOnDAS implements AdminCommand {

    @Inject
    private Target targetUtil;
    
    @Param(optional=false)
    String message;
    
    @Param(optional=true, defaultValue = "server-config")
    String target;    
    
    @Override
    public void execute(AdminCommandContext context) {
        // obtain the correct configuration
        Config configVal = targetUtil.getConfig(target);
        ExampleServiceConfiguration serviceConfig = configVal.getExtensionByType(ExampleServiceConfiguration.class);
        if (serviceConfig != null) {
            try {
                // to perform a transaction on the domain.xml you need to use this construct
                // see https://github.com/hk2-project/hk2/blob/master/hk2-configuration/persistence/hk2-xml-dom/hk2-config/src/main/java/org/jvnet/hk2/config/ConfigSupport.java
                ConfigSupport.apply(new SingleConfigCode<ExampleServiceConfiguration>(){
                    @Override
                    public Object run(ExampleServiceConfiguration config) {
                        config.setMessage(message);
                        return null;
                    }
                }, serviceConfig);
            } catch (TransactionFailure ex) {
                // set failure
                context.getActionReport().failure(Logger.getLogger(SetExampleServiceMessage.class.getName()), "Failed to update message", ex);
            }
        } else {
            context.getActionReport().failure(Logger.getLogger(this.getClass().getCanonicalName()), "No configuration with name " + target);
        }
    }
    
}
