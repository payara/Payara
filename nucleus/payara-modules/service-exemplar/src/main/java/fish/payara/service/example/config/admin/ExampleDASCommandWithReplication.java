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

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.service.example.config.ExampleServiceConfiguration;
import java.util.List;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * This is an Exemplar asadmin command that needs to first run on the DAS to do 
 * something and then runs an additional command on a number of instances.
 * 
 * The use case is you need to perhaps update something on the DAS or in the DAS config
 * then do some work on a remote instance. 
 *
 * @author steve
 */
@Service(name = "example-das-command") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run
@ExecuteOn(RuntimeType.DAS) // this means the command can run only on the DAS
@RestEndpoints({  // creates a REST endpoint needed for integration with the admin interface
    @RestEndpoint(configBean = ExampleServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "example-das-command",
            description = "Runs a command on the DAS first")
})
public class ExampleDASCommandWithReplication implements AdminCommand {
    
    @Param
    String message;
    
    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        
        // first we work in the DAS in this example just log something
        Logger.getLogger(this.getClass().getCanonicalName()).info("This is on the DAS I am going to send message to all nodes " + message);
       
        // work out the servers you want to call. 
        // The domain config object has methods to find out what servers are available
        List<String> domainServers = domain.getAllTargets();
        
        // build your command parameters
        ParameterMap params = new ParameterMap();
        params.add("message", message);
        ClusterOperationUtil.replicateCommand("example-instance-command", FailurePolicy.Ignore, FailurePolicy.Ignore, FailurePolicy.Error, domainServers, context, params, habitat);

    }
    
}
