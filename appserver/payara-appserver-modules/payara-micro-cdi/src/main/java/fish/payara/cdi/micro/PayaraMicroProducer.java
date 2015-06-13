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

import fish.payara.micro.PayaraMicro;
import fish.payara.micro.PayaraMicroRuntime;
import fish.payara.micro.services.PayaraClusteredCDIEvent;
import javax.annotation.PostConstruct;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

/**
 *
 * @author steve
 */
public class PayaraMicroProducer {
    
    @Inject
    private InboundEventPublisher publisher;
    
    private final PayaraMicroRuntime runtime;
    
    PayaraMicroProducer() {
        runtime = PayaraMicro.getInstance(false).getRuntime();
    }
    
    @Produces
    PayaraMicroRuntime getRuntime() {
        return runtime;
    }
    
    public void onClusteredMessage(@Observes @Outbound PayaraClusteredCDIEvent event) {
        runtime.publishCDIEvent(event);
    }
    
    @PostConstruct void postcontstruct() {
        runtime.addCDIEventListener(publisher);
    }
    
}
