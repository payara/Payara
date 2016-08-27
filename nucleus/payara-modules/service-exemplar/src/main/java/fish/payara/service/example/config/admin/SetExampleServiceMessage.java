/*
 * Copyright (c) 2016 Payara Foundation. All rights reserved.
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
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import fish.payara.service.example.ExampleService;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Exemplar command for configuration updates.
 * 
 * Example command that updates the configuration in the domain.xml
 * Execution of the command will happen on all targeted instances and ALWAYS on the DAS.
 * If the service is a ConfigListener it will be triggered to reconfigure itself from the config change on all targeted instances
 * the service must be a ConfigListener and register itself for the config node it is interested in
 * @author steve
 */
@Service(name = "set-example-service-message") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE}) // this means the command can run on any node so will run on all nodes
@RestEndpoints({  // creates a REST endpoint needed for integration with the admin interface
    @RestEndpoint(configBean = ExampleServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "set-example-service-message",
            description = "Sets the Service Configuration")
})
public class SetExampleServiceMessage implements AdminCommand {
    
    // a useful service to get at the configuration based on the target parameter
    @Inject
    private Target targetUtil;
       
    @Inject
    ServiceLocator habitat;
    
    @Inject
    ServerEnvironment server;
    
    @Inject
    ExampleService service;
    
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
        }
        
        // Below is example code you can use to also manipulate the service directly
        // NOTE: if the Service is a ConfigListener then it will also be notified directly if
        // the config changes therefore this is not always necessary
        // As this command is always executed on the DAS you can check whether the DAS
        // was targetted explicitly via the following code
        if (server.isDas()) {
            // this command is executing on the DAS now check whether you are explicitly the target
            // you would need to do this if you now want to manipulate the service based on the command parameters
            if (targetUtil.getConfig(target).isDas()) { // this command was also targetted at the DAS
                service.doSomethingDirectly("Set message command was targetted at the DAS");
                // as below you can now directly manipulate the service as needed
            }           
        } else {  // if you are not the DAS then impoicitly this command was targeted to this instance
            service.doSomethingDirectly("Set config command targeted at the instance");
            // you can now directly manipulate the service remember though 
            // if it is a config listener it has already been notified of the config change
            // however if it is not a config listener you can manipulate the service now
        }
        
    }
}
