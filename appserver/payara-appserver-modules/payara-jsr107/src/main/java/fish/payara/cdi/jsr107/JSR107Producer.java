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

import com.hazelcast.core.HazelcastInstance;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.lang.annotation.Annotation;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;


/**
 *
 * @author steve
 */
public class JSR107Producer {
    
    public JSR107Producer() {
        hazelcastCore = HazelcastCore.getCore();
    }
    
    HazelcastCore hazelcastCore;
    
    @Dependent
    @Produces
    CachingProvider getCachingProvider() {
        // TBD look for scoped Caching Providers in JNDI
        return hazelcastCore.getCachingProvider();
    }
    
    @Dependent
    @Produces
    CacheManager getCacheManager(InjectionPoint point) {
        CacheManager result = null;
        Set<Annotation> qualifiers = point.getQualifiers();
        for (Annotation qualifier : qualifiers) {
            if (qualifier instanceof TCCLCacheManager) {
                return hazelcastCore.getCachingProvider().getCacheManager(hazelcastCore.getCachingProvider().getDefaultURI(), Thread.currentThread().getContextClassLoader());
            }
        }
        return hazelcastCore.getCachingProvider().getCacheManager();
    }
    
    @Dependent
    @Produces
    HazelcastInstance getHazelcast() {
        return hazelcastCore.getInstance();
    }
    
    @Produces
    @NamedCache
    public Cache<Object,Object> createCache(InjectionPoint ip) {
        Cache<Object,Object> result = null;
        Set<Annotation> qualifiers = ip.getQualifiers();
        NamedCache ncqualifier = null;
        for (Annotation qualifier : qualifiers) {
            if (qualifier instanceof NamedCache) {
                ncqualifier = NamedCache.class.cast(qualifier);
            }
        }
        if (ncqualifier != null) {
            String cacheName = ncqualifier.name();
            Class keyClass = ncqualifier.keyClass();
            Class valueClass = ncqualifier.valueClass();
            
            MutableConfiguration<Object,Object> config = new MutableConfiguration<>();
            config.setTypes(keyClass, valueClass);
            config.setManagementEnabled(ncqualifier.managementEnabled());
            config.setStatisticsEnabled(ncqualifier.statisticsEnabled());
            CacheManager manager = getCacheManager(ip);
            result = manager.createCache(cacheName, config);
        }
        return result;
    }

    
}