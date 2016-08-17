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

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Exemplar command for configuration updates for a service that wishes to be dynamic.
 * i.e. it can be notified of config changes and reconfigure itself based on the config change
 * 
 * Example command that updates the configuration in the domain.xml
 * The command then replicates itself to all nodes referenced by the changed config
 * The execution on the remote nodes will then trigger the Service to reconfigure itself
 * the service must be a ConfigListener and register itself for the config node it is interested in
 * @author steve
 */
@Service(name = "set-example-service-message") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn(RuntimeType.INSTANCE) // this means the command can run on any node
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
    
    @Param(optional=false)
    String message;
    
    @Param(optional=true, defaultValue = "server-config")
    String config;
    
    @Override
    public void execute(AdminCommandContext context) {

        // obtain the correct configuration
        Config configVal = targetUtil.getConfig(config);
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
        
        // replicate this command, if we are the DAS, 
        // to all servers/clusters referenced by config and this will trigger the config listeners remotely      
        if (targetUtil.isThisDAS() && !"server-config".equals(config)) {
            ParameterMap params = new ParameterMap();
            params.add("config", config);
            params.add("message", message);
            ClusterOperationUtil.replicateCommand("set-example-service-message", FailurePolicy.Ignore, FailurePolicy.Ignore, FailurePolicy.Error, targetUtil.getInstances(config), context, params, habitat);
        }
    }
}
