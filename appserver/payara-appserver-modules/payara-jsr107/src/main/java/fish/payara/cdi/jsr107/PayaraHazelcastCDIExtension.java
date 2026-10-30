/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.cdi.jsr107;

import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

/**
 * A CDI Extension for accessing Hazelcast JSR107 artifacts
 *
 * @author steve
 */
public class PayaraHazelcastCDIExtension  implements Extension {
    
    
    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        AnnotatedType<JSR107Producer> at = bm.createAnnotatedType(JSR107Producer.class);
        bbd.addAnnotatedType(at, JSR107Producer.class.getName());
        
        // Add interceptors and interceptor bindings for our internal CDI interceptors
        bbd.addInterceptorBinding(CachePut.class);
        bbd.addInterceptorBinding(CacheRemove.class);
        bbd.addInterceptorBinding(CacheRemoveAll.class);
        bbd.addInterceptorBinding(CacheResult.class);
        AnnotatedType<CachePutInterceptor> cpat = bm.createAnnotatedType(CachePutInterceptor.class);
        bbd.addAnnotatedType(cpat, CachePutInterceptor.class.getName());
        AnnotatedType<CacheResultInterceptor> crat = bm.createAnnotatedType(CacheResultInterceptor.class);
        bbd.addAnnotatedType(crat, CacheResultInterceptor.class.getName());
        AnnotatedType<CacheRemoveInterceptor> crmat = bm.createAnnotatedType(CacheRemoveInterceptor.class);
        bbd.addAnnotatedType(crmat, CacheRemoveInterceptor.class.getName());
        AnnotatedType<CacheRemoveAllInterceptor> crmaat = bm.createAnnotatedType(CacheRemoveAllInterceptor.class);
        bbd.addAnnotatedType(crmaat, CacheRemoveAllInterceptor.class.getName());
    }

    
}
