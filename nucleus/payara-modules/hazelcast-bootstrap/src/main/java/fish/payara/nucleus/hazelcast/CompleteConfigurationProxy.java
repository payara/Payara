/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2015,2016,2017 Payara Foundation. All rights reserved.

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
package fish.payara.nucleus.hazelcast;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.util.Utility;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import javax.enterprise.inject.spi.CDI;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.ServerContext;
import org.jboss.weld.context.bound.BoundRequestContext;

/**
 * Proxy all applicable factory calls
 * so Java EE context is propagated from within Hazelcast threads
 *
 * @author lprimak
 */
class CompleteConfigurationProxy<K, V> extends MutableConfiguration<K, V>{
    public CompleteConfigurationProxy(CompleteConfiguration<K, V> config, ServerContext serverContext) {
        super(config);
        this.serverContext = serverContext;
        capturedInvocation = serverContext.getInvocationManager().getCurrentInvocation();
        compEnvMgr = serverContext.getDefaultServices().getService(ComponentEnvManager.class);
        init();
    }

    private void init() {
        if(cacheLoaderFactory != null) {
            cacheLoaderFactory = proxyLoader(cacheLoaderFactory);
        }
        if(cacheWriterFactory != null) {
            cacheWriterFactory = proxyWriter(cacheWriterFactory);
        }
    }

    /**
     * pushes Java EE invocation context
     *
     * @return old ClassLoader, or null if no invocation has been created
     */
    private Context preInvoke() {
        ClassLoader oldClassLoader = Utility.getClassLoader();
        InvocationManager invMgr = serverContext.getInvocationManager();
        boolean invocationCreated = false;
        if(invMgr.getCurrentInvocation() == null) {
            invMgr.preInvoke(new ComponentInvocation(capturedInvocation.getComponentId(), ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION,
                    capturedInvocation.getContainer(), capturedInvocation.getAppName(), capturedInvocation.getModuleName()));
            invocationCreated = true;
        }
        JndiNameEnvironment componentEnv = compEnvMgr.getCurrentJndiNameEnvironment();
        if(invocationCreated && componentEnv instanceof BundleDescriptor) {
            BundleDescriptor bd = (BundleDescriptor)componentEnv;
            Utility.setContextClassLoader(bd.getClassLoader());
        }
        return new Context(oldClassLoader, invocationCreated? invMgr.getCurrentInvocation() : null);
    }

    private void postInvoke(Context ctx) {
        if (ctx.invocation != null) {
            serverContext.getInvocationManager().postInvoke(ctx.invocation);
            Utility.setContextClassLoader(ctx.classLoader);
        }
    }

    /**
     * pushes invocation context onto the stack
     * Also creates Request scope
     *
     * @return new context that was created
     */
    private RequestContext preInvokeRequestContext() {
        Context rootCtx = preInvoke();
        BoundRequestContext brc = CDI.current().select(BoundRequestContext.class).get();
        RequestContext context = new RequestContext(rootCtx, brc.isActive()? null : brc, new HashMap<String, Object>());
        if(context.ctx != null) {
            context.ctx.associate(context.storage);
            context.ctx.activate();
        }
        return context;
    }

    /**
     * context to pop from the stack
     *
     * @param context to be popped
     */
    private void postInvokeRequestContext(RequestContext context) {
        if (context.ctx != null) {
            context.ctx.deactivate();
            context.ctx.dissociate(context.storage);
        }
        postInvoke(context.rootCtx);
    }

    private static class Context {
        public Context(ClassLoader classLoader, ComponentInvocation invocation) {
            this.classLoader = classLoader;
            this.invocation = invocation;
        }

        final ClassLoader classLoader;
        final ComponentInvocation invocation;
    }

    private static class RequestContext {
        public RequestContext(Context rootCtx, BoundRequestContext ctx, Map<String, Object> storage) {
            this.rootCtx = rootCtx;
            this.ctx = ctx;
            this.storage = storage;
        }

        final Context rootCtx;
        final BoundRequestContext ctx;
        final Map<String, Object> storage;
    }

    private Factory<CacheLoader<K, V>> proxyLoader(final Factory<CacheLoader<K, V>> fact) {
        return new Factory<CacheLoader<K, V>>() {
            @Override
            public CacheLoader<K, V> create() {
                Context ctx = null;
                try {
                    ctx = preInvoke();
                    final CacheLoader<K, V> loader = fact.create();
                    return new CacheLoaderImpl(loader);
                }
                finally {
                    postInvoke(ctx);
                }
            }
            
            class CacheLoaderImpl implements CacheLoader<K, V> {
                public CacheLoaderImpl(CacheLoader<K, V> loader) {
                    this.loader = loader;
                }

                @Override
                public V load(K k) throws CacheLoaderException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        return loader.load(k);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                @Override
                public Map<K, V> loadAll(Iterable<? extends K> itrbl) throws CacheLoaderException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        return loader.loadAll(itrbl);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                private final CacheLoader<K, V> loader;
            }

            private static final long serialVersionUID = 1L;
        };
    }

    private Factory<CacheWriter<? super K, ? super V>> proxyWriter(final Factory<CacheWriter<? super K, ? super V>> fact) {
        return new Factory<CacheWriter<? super K, ? super V>>() {
            @Override
            public CacheWriter<K, V> create() {
                Context ctx = null;
                try {
                    ctx = preInvoke();
                    @SuppressWarnings("unchecked")
                    final CacheWriter<K, V> delegate = (CacheWriter<K, V>) fact.create();
                    return new CacheWriterImpl(delegate);
                }
                finally {
                    postInvoke(ctx);
                }
            }

            class CacheWriterImpl implements CacheWriter<K, V> {
                public CacheWriterImpl(CacheWriter<K, V> delegate) {
                    this.delegate = delegate;
                }

                @Override
                public void write(Cache.Entry<? extends K, ? extends V> entry) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        delegate.write(entry);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                @Override
                public void writeAll(Collection<Cache.Entry<? extends K, ? extends V>> clctn) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        delegate.writeAll(clctn);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                @Override
                public void delete(Object o) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        delegate.delete(o);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                @Override
                public void deleteAll(Collection<?> clctn) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = preInvokeRequestContext();
                        delegate.deleteAll(clctn);
                    }
                    finally {
                        postInvokeRequestContext(context);
                    }
                }

                private final CacheWriter<K, V> delegate;
            }

            private static final long serialVersionUID = 1L;
        };
    }

    private final ServerContext serverContext;
    private final ComponentInvocation capturedInvocation;
    private final ComponentEnvManager compEnvMgr;

    private static final long serialVersionUID = 1L;
}
