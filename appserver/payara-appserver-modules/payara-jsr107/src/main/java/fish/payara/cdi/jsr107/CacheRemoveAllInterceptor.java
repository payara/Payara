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
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 *
 * @author steve
 */ 
@CacheRemoveAll
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_AFTER)
public class CacheRemoveAllInterceptor extends AbstractJSR107Interceptor {
    
    @AroundInvoke
    public Object cacheRemoveAll(InvocationContext ctx) throws Throwable {
        
        if (!isEnabled()) {
            return ctx.proceed();
        }
        
        CacheRemoveAll annotation = ctx.getMethod().getAnnotation(CacheRemoveAll.class);
        PayaraCacheKeyInvocationContext<CacheRemoveAll> pctx = new PayaraCacheKeyInvocationContext<>(ctx, annotation);

        if (!annotation.afterInvocation()) {
            doRemoveAll(pctx);
        }

        Object result = null;
        try {
            result = ctx.proceed();
        } catch (Throwable e) {
            if (annotation.afterInvocation()) {
                if (shouldIEvict(annotation.evictFor(), annotation.noEvictFor(), e)) {
                    doRemoveAll(pctx);
                }
            }
            throw e;
        }

        if (annotation.afterInvocation()) {
            doRemoveAll(pctx);
        }

        return result;
    }

    private void doRemoveAll(PayaraCacheKeyInvocationContext<CacheRemoveAll> pctx) {
        CacheResolverFactory resolverF = pctx.getFactory();
        CacheResolver cacheResolver = resolverF.getCacheResolver(pctx);
        Cache cache = cacheResolver.resolveCache(pctx);
        cache.removeAll();
    }
}

