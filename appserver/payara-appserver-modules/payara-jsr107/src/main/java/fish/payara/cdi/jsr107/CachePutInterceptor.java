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
import javax.cache.annotation.CachePut;
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
@CachePut
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class CachePutInterceptor extends AbstractJSR107Interceptor {

    @AroundInvoke
    public Object cachePut(InvocationContext ctx) throws Throwable {

        CachePut annotation = ctx.getMethod().getAnnotation(CachePut.class);
        PayaraCacheKeyInvocationContext<CachePut> pctx = new PayaraCacheKeyInvocationContext<>(ctx, annotation);
        if (!annotation.afterInvocation()) {
            doPut(pctx);
        }

        Object result = null;
        try {
            result = ctx.proceed();
        } catch (Throwable e) {
            if (annotation.afterInvocation()) {
                if (shouldICache(annotation.cacheFor(), annotation.noCacheFor(), e, false)) {
                    doPut(pctx);
                }
            }
            throw e;
        }

        if (annotation.afterInvocation()) {
            doPut(pctx);
        }

        return result;
    }

    private void doPut(PayaraCacheKeyInvocationContext<CachePut> pctx) {
        CacheKeyGenerator generator = pctx.getGenerator();
        CacheResolverFactory resolverF = pctx.getFactory();
        CacheResolver cacheResolver = resolverF.getCacheResolver(pctx);
        Cache cache = cacheResolver.resolveCache(pctx);
        GeneratedCacheKey key = generator.generateCacheKey(pctx);
        Object value = pctx.getValueParameter().getValue();
        cache.put(key, value);
    }

}
