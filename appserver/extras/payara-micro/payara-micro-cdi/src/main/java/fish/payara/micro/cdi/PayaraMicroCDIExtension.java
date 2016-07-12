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
package fish.payara.micro.cdi;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * A CDI Extension for integrating with Payara Micro
 *
 * @author steve
 */
public class PayaraMicroCDIExtension implements Extension {

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {

        AnnotatedType<PayaraMicroProducer> at = bm.createAnnotatedType(PayaraMicroProducer.class);
        bbd.addAnnotatedType(at);
        
        AnnotatedType<ClusteredCDIEventBus> at3 = bm.createAnnotatedType(ClusteredCDIEventBus.class);
        bbd.addAnnotatedType(at3);

    }

}
