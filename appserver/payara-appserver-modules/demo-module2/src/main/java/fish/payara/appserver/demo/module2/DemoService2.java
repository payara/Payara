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
package fish.payara.appserver.demo.module2;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;


/**
 *
 * @author srai
 */
@Service
@RunLevel(StartupRunLevel.VAL)
public class DemoService2 implements EventListener {

    private static final Logger logger = Logger.getLogger(DemoService2.class.getName());

    @Inject
    private Events events;

    @Inject
    DemoServiceConfiguration config;

    @PostConstruct
    public void postconstruct() {
        logger.info(config.getHelloWorldMessage());
        events.register(this);
    }

    @Override
    public void event(EventListener.Event event) {
        logger.log(Level.INFO, "Got event {0} type {1}", new Object[]{event.name(), event.type()});
    }

}
