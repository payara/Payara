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

import fish.payara.cdi.jsr107.impl.PayaraCacheKeyInvocationContext;
import javax.annotation.Priority;
import javax.cache.Cache;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.GeneratedCacheKey;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author steve
 */ 
@CacheResult
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class CacheResultInterceptor extends AbstractJSR107Interceptor {
    
    @AroundInvoke
    public Object cacheResult(InvocationContext ctx) throws Throwable {
        
        // get my annotation
        CacheResult annotation = ctx.getMethod().getAnnotation(CacheResult.class);
        PayaraCacheKeyInvocationContext<CacheResult> pctx = new PayaraCacheKeyInvocationContext<>(ctx, annotation);
        CacheResolverFactory resolverF = pctx.getFactory();
        CacheResolver cacheResolver = resolverF.getCacheResolver(pctx);
        Cache cache = cacheResolver.resolveCache(pctx);
        boolean cacheExceptions = (annotation.exceptionCacheName() != null && annotation.exceptionCacheName().length() > 0);
        
        CacheKeyGenerator generator = pctx.getGenerator();
        GeneratedCacheKey key = generator.generateCacheKey(pctx);
        if (!annotation.skipGet()) {
            Object result = cache.get(key);
            if (result != null) {
                return result;
            } else {
                // check exception cache
                if (cacheExceptions) {
                    Cache exceptionCache = resolverF.getExceptionCacheResolver(pctx).resolveCache(pctx);
                    Throwable t = (Throwable) exceptionCache.get(key);
                    if (t != null) {
                        throw t;
                    }
                }
            }
        }
        
        // call the method
                Object result = null;
        try {
            result = ctx.proceed();
            cache.put(key, result);
        } catch (Throwable e) {
            boolean cacheException = false;
            if (cacheExceptions) {
                Cache exceptionCache = resolverF.getExceptionCacheResolver(pctx).resolveCache(pctx);
                if (shouldICache(annotation.cachedExceptions(), annotation.nonCachedExceptions(), e, true)) {
                    exceptionCache.put(key, e);
                }
            }
            throw e;
        }
        return result;
    }
}
