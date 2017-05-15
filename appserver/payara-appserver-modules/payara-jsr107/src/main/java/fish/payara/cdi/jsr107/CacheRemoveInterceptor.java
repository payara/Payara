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
package fish.payara.cdi.jsr107;

import fish.payara.cdi.jsr107.implementation.PayaraCacheKeyInvocationContext;
import javax.annotation.Priority;
import javax.cache.Cache;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheRemove;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.GeneratedCacheKey;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author steve
 */
@CacheRemove
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class CacheRemoveInterceptor extends AbstractJSR107Interceptor {

    @AroundInvoke
    public Object cacheRemove(InvocationContext ctx) throws Throwable {
        
        if (!isEnabled()) {
            return ctx.proceed();
        }
        
        
        CacheRemove annotation = ctx.getMethod().getAnnotation(CacheRemove.class);
        PayaraCacheKeyInvocationContext<CacheRemove> pctx = new PayaraCacheKeyInvocationContext<>(ctx, annotation);

        if (!annotation.afterInvocation()) {
            doRemove(pctx);
        }

        Object result = null;
        try {
            result = ctx.proceed();
        } catch (Throwable e) {
            if (annotation.afterInvocation()) {
                if (shouldIEvict(annotation.evictFor(), annotation.noEvictFor(), e)) {
                    doRemove(pctx);
                }
            }
            throw e;
        }

        if (annotation.afterInvocation()) {
            doRemove(pctx);
        }

        return result;
    }

    private void doRemove(PayaraCacheKeyInvocationContext<CacheRemove> pctx) {
        CacheKeyGenerator generator = pctx.getGenerator();
        CacheResolverFactory resolverF = pctx.getFactory();
        CacheResolver cacheResolver = resolverF.getCacheResolver(pctx);
        Cache cache = cacheResolver.resolveCache(pctx);
        GeneratedCacheKey key = generator.generateCacheKey(pctx);
        cache.remove(key);
    }
}
