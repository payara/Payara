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
package fish.payara.cdi.micro;

import fish.payara.micro.services.CDIEventListener;
import fish.payara.micro.services.PayaraClusteredCDIEvent;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 *
 * @author steve
 */
@Singleton
public class InboundEventPublisher implements CDIEventListener {
    
    @Inject
    Event<PayaraClusteredCDIEvent> clusterEvent;

    @Override
    public void eventReceived(PayaraClusteredCDIEvent event) {
        clusterEvent.fire(event);
    }
    
    
            
    
}
