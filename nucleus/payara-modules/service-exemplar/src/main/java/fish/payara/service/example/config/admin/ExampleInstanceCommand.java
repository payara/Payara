/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.service.example.config.admin;

import fish.payara.service.example.ExampleService;
import fish.payara.service.example.config.ExampleServiceConfiguration;
import jakarta.inject.Inject;
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
import org.jvnet.hk2.annotations.Service;

/**
 * Example Command that does not operate on the configuration but wants to 
 * interact directly with the service running in an instance. 
 * 
 * From the ExecuteOn below this command will ONLY run on targeted instances
 * The command will NOT run on the DAS unless targeted explicitly.
 * 
 * If a target parameter is specified this command will be executed on all instances
 * that meet the target specification i.e. if a config all instances referencing the config
 * if a cluster all instances in the cluster if an instance name that specific instance.
 * 
 * @author steve
 */
@Service(name = "example-instance-command") // the name of the service is the asadmin command name
@PerLookup // this means one instance is created every time the command is run it is mandatory for admin commands
@ExecuteOn(value=RuntimeType.INSTANCE) // this means the command can run on any node
@TargetType(value = {CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE})
@RestEndpoints({  // creates a REST endpoint needed for integration with the admin interface
    @RestEndpoint(configBean = ExampleServiceConfiguration.class,
            opType = RestEndpoint.OpType.POST, // must be POST as it is doing an update
            path = "example-instance-command",
            description = "Interacts directly on a node with the Example Service")
})
public class ExampleInstanceCommand implements AdminCommand {
    
    @Param (optional = true, defaultValue = "server") // if no target is specified it will be the DAS
    String target;
    
    @Param
    String message;
    
    // inject the service I want to operate on
    @Inject
    ExampleService service;

    @Override
    public void execute(AdminCommandContext context) {
        service.doSomethingDirectly(message);
        context.getActionReport().appendMessage("Sent " + message + " directly to the service on target " + target);
    }
    
}
