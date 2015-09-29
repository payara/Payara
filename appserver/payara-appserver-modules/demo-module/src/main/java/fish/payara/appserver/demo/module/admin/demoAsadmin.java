/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.demo.module.admin;

import com.sun.enterprise.config.serverbeans.Domain;
import fish.payara.appserver.demo.module.demoConfig;
import java.beans.PropertyVetoException;
import javax.inject.Inject;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author srai
 */
/*Annotations and interface need for Glasshfish to detect the class as a new 
 admin command */
@Service(name = "demo") //Name of the "asadmin" command, give it a clear name
@PerLookup //Scope
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class,
            opType = RestEndpoint.OpType.POST,
            path = "set-demo-service-configuration",
            description = "Set Demo Configuration")
})

//implement the admin command interface 
public class demoAsadmin implements AdminCommand {

    //Injecting a configuration interface
    @Inject
    demoConfig democonfig;

    /*specifying any parameters for the command. Parameters are annotated and 
     defined with @Param annotations*/
    @Param(name = "frist-name", optional = false) /*Give the parameter a name 
     to identify it and specify if it is optional or not*/

    private String fristname; //Varabile to store it
    /*Define shortname of the parameter using "shortName", so you dont have to 
     specify the whole name*/
    @Param(name = "surname", defaultValue = "Payara", optional = true, shortName = "l")
    private String surname;

    /*if you want to limit the values that can be entered in a parameter, you 
     can specify a comma-separated list with the acceptable values attribute. 
     This can done using "acceptableValues"*/
    @Param(name = "full", optional = true, defaultValue = "flase", acceptableValues = "true,flase")
    private String full;

    /*"AdminCommand" interface requires us to @Override the execute method with
     parameter "". This method is what will be run when the command is executed.
     This contents example method boby */
    @Override
    public void execute(AdminCommandContext context) {
        /* To have our asadmin command write to the domain.xml file. We need to
         create a transaction in the execute method.*/

        try {
            /*calls the apply method of the "ConfigSupport" class with a single
             config code interface as a parameter*/
            ConfigSupport.apply(new SingleConfigCode<demoConfig>() {
                /*override the "run" method, defining a new variable of our 
                 configuration interface*/
                public Object run(demoConfig demoConfigProxy)
                        throws PropertyVetoException, TransactionFailure {
                    /*using the if statment to check whether or not a value has
                     been provided for a parameter. Before using the set method
                     of the attribute in our configuration interface*/
                    if (fristname != null) {
                        demoConfigProxy.setFristname(fristname);
                    }

                    if (surname != null) {
                        demoConfigProxy.setFristname(surname);
                    }
                    return null; //Return null to complete the method
                }
                /*add our injected interface variable as the second parameter of
                 the apply method*/
            }, democonfig);
        } catch (TransactionFailure ex) {
            ex.printStackTrace();
        }

        //prints out statements that change depending on the parameter specified
        if (full.equals("false")) {
            System.out.println("Hello " + democonfig.getFristname() + "! ");
        } else {
            System.out.println("Hello " + democonfig.getFristname() + " " + democonfig.getSurname() + "! ");
        }
    }

}
/*Now we have a asadmin command that will set the name parameter in our domain.xml*/
