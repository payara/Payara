/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014 C2B2 Consulting Limited. All rights reserved.

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
package fish.payara.cdi.jsr107;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;

/**
 * A CDI Extension for accessing Hazelcast JSR107 artifacts
 * @author steve
 */
public class PayaraHazelcastCDIExtension  implements Extension {
    
    
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<JSR107Producer> at = bm.createAnnotatedType(JSR107Producer.class);
        bbd.addAnnotatedType(at);    
        
        // Add interceptors and interceptor bindings for our internal CDI interceptors
        bbd.addInterceptorBinding(CachePut.class);
        bbd.addInterceptorBinding(CacheRemove.class);
        bbd.addInterceptorBinding(CacheRemoveAll.class);
        bbd.addInterceptorBinding(CacheResult.class);
        AnnotatedType<CachePutInterceptor> cpat = bm.createAnnotatedType(CachePutInterceptor.class);
        bbd.addAnnotatedType(cpat);
        AnnotatedType<CacheResultInterceptor> crat = bm.createAnnotatedType(CacheResultInterceptor.class);
        bbd.addAnnotatedType(crat);
        AnnotatedType<CacheRemoveInterceptor> crmat = bm.createAnnotatedType(CacheRemoveInterceptor.class);
        bbd.addAnnotatedType(crmat);
        AnnotatedType<CacheRemoveAllInterceptor> crmaat = bm.createAnnotatedType(CacheRemoveAllInterceptor.class);
        bbd.addAnnotatedType(crmaat);
    }

    
}
