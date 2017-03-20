/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016-2017 Payara Foundation. All rights reserved.

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
package fish.payara.cdi.jsr107.implementation;

import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.lang.annotation.Annotation;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.MutableConfiguration;

/**
 *
 * @author steve
 */
public class PayaraCacheResolverFactory implements CacheResolverFactory, CacheResolver {
    
    private CacheManager cacheManager;

    public PayaraCacheResolverFactory() {
        // TBD resolve scoped Caching Providers via JNDI eg. java:comp and java:app
        cacheManager = HazelcastCore.getCore().getCachingProvider().getCacheManager();
    }
    
    @Override
    public CacheResolver getCacheResolver(CacheMethodDetails<? extends Annotation> cmd) {
        String cacheName = cmd.getCacheName();
        Cache cache = cacheManager.getCache(cacheName);
        if ((cache == null)) {
            cache = cacheManager.createCache(cacheName, new MutableConfiguration<Object, Object>());
        }
        return new PayaraCacheResolver(cache);
    }

    @Override
    public CacheResolver getExceptionCacheResolver(CacheMethodDetails<CacheResult> cmd) {
        CacheResult result = cmd.getCacheAnnotation();
        String cacheName = result.exceptionCacheName();
        Cache cache = cacheManager.getCache(cacheName);
        if ((cache == null)) {
            cache = cacheManager.createCache(cacheName, new MutableConfiguration<Object, Object>());
        }
        return new PayaraCacheResolver(cache);
    }

    @Override
    public <K, V> Cache<K, V> resolveCache(CacheInvocationContext<? extends Annotation> cic) {
        Cache<K,V> cache = cacheManager.getCache(cic.getCacheName());
        if ((cache == null)) {
            cacheManager.createCache(cic.getCacheName(),new MutableConfiguration<>());
            cache = cacheManager.getCache(cic.getCacheName());
        }
        return cache;
    }
    
}
