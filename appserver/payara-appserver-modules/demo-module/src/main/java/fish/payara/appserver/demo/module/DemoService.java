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

package fish.payara.appserver.demo.module;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.StartupRunLevel;

/**
 *
 * @author srai
 */
//For glasshfish to recognize classes and add componet, you will need to use @Service annoinations 
@Service(name = "Demo-Service")
//Since the service would need to run as the service starts I will use the @RunLevel annotation
@RunLevel(StartupRunLevel.VAL)

/*Each class designated as a Service needs to be bound to a contract. Service we 
 need to listen to the service startup and shutdown event it will implement the event listener 
 interface and be bound to the events contract through injection (@Inject)*/
public class DemoService implements EventListener {

    @Inject
    Events events;

    private static final Logger logger = Logger.getLogger(DemoService.class.getName());

    /*You can instruct a service to perform certain actions as it is constructed and destoryed 
     using the @PostConstruct and @PreDestory interfaces @PostConstruct. They can be used excute
     actions and methods after the service is initialized or just before the components is removed
     from the system respectively. Both methods don't take any parameter*/
    @PostConstruct
    /*Prints statements to work as the service starts and stops we would need to us register method 
     events inside the @PostConstruct method*/
    public void postconstruct() {
        /* service will print "GoodBye" when the service shuts down, "Starting up" as it beigns to start up
         and "Payara Ready" when it is done with starting */
        events.register(this);
        sayHello();
    }

    @PreDestroy
    public void predestroy() {

    }

    //The EventListener interface require us to @Override the event method
    @Override
    //Filling it with print statements to give updates on the startup and shutdown events
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            System.out.println("GoodBye");
        } else if (event.is(EventTypes.SERVER_STARTUP)) {
            System.out.println("Starting up");
        } else if (event.is(EventTypes.SERVER_READY)) {
            System.out.println("Payara Ready");
        }
    }

    private void sayHello() {
        System.out.println("Hello world");
    }

    // To implement a service you will need to package it and add it to a distribution 
}
