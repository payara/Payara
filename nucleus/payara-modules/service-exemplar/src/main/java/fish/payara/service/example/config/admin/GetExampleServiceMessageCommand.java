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

/**
 * This is an exemplar command for displaying to the user the configuration as specified
 * in the domain.xml
 * 
 * In contrast to the equivalent set command this command only needs to run on the DAS
 * and does not need to replicate itself to all nodes.
 * 
 * This class is an example asadmin command for getting the configured message
 * 
 * 
 * @author steve
 */
@Service(name = "get-example-service-message") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn(RuntimeType.DAS) // this means the command can only run on the DAS
@RestEndpoints({  // creates a REST endpoint needed for integration with the admin interface
    @RestEndpoint(configBean = ExampleServiceConfiguration.class,
            opType = RestEndpoint.OpType.GET,
            path = "get-example-service-message",
            description = "Gets the Service Configuration")
})
public class GetExampleServiceMessageCommand implements AdminCommand { // must implement AdminCommand to be an asadmin command
    
    // a useful service to get at the configuration based on the target parameter
    @Inject
    private Target targetUtil;

    // target configuration to retrieve the message from 
    @Param(name = "config", optional = true, defaultValue = "server-config")
    String config;

    /**
     * This method is called when the command is executed
     * @param context 
     */
    @Override
    public void execute(AdminCommandContext context) {
        
        // obtain the correct configuration
        Config configNode = targetUtil.getConfig(config);
        ExampleServiceConfiguration serviceConfig = configNode.getExtensionByType(ExampleServiceConfiguration.class);
        
        // add return message
        context.getActionReport().setMessage("Example Service message is " + serviceConfig.getMessage());
    }
    
}
