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

import fish.payara.nucleus.hazelcast.JavaEEContextUtil.Context;
import fish.payara.nucleus.hazelcast.JavaEEContextUtil.RequestContext;
import java.util.Collection;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.integration.CacheLoader;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriter;
import javax.cache.integration.CacheWriterException;
import org.glassfish.internal.api.ServerContext;

/**
 * Proxy all applicable factory calls
 * so Java EE context is propagated from within Hazelcast threads
 *
 * @author lprimak
 */
class CompleteConfigurationProxy<K, V> extends MutableConfiguration<K, V>{
    public CompleteConfigurationProxy(CompleteConfiguration<K, V> config, ServerContext serverContext) {
        super(config);
        ctxUtil = new JavaEEContextUtil(serverContext);
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

    private Factory<CacheLoader<K, V>> proxyLoader(final Factory<CacheLoader<K, V>> fact) {
        return new Factory<CacheLoader<K, V>>() {
            @Override
            public CacheLoader<K, V> create() {
                Context ctx = null;
                try {
                    ctx = ctxUtil.preInvoke();
                    final CacheLoader<K, V> loader = fact.create();
                    return new CacheLoaderImpl(loader);
                }
                finally {
                    ctxUtil.postInvoke(ctx);
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
                        context = ctxUtil.preInvokeRequestContext();
                        return loader.load(k);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
                    }
                }

                @Override
                public Map<K, V> loadAll(Iterable<? extends K> itrbl) throws CacheLoaderException {
                    RequestContext context = null;
                    try {
                        context = ctxUtil.preInvokeRequestContext();
                        return loader.loadAll(itrbl);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
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
                    ctx = ctxUtil.preInvoke();
                    @SuppressWarnings("unchecked")
                    final CacheWriter<K, V> delegate = (CacheWriter<K, V>) fact.create();
                    return new CacheWriterImpl(delegate);
                }
                finally {
                    ctxUtil.postInvoke(ctx);
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
                        context = ctxUtil.preInvokeRequestContext();
                        delegate.write(entry);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
                    }
                }

                @Override
                public void writeAll(Collection<Cache.Entry<? extends K, ? extends V>> clctn) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = ctxUtil.preInvokeRequestContext();
                        delegate.writeAll(clctn);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
                    }
                }

                @Override
                public void delete(Object o) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = ctxUtil.preInvokeRequestContext();
                        delegate.delete(o);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
                    }
                }

                @Override
                public void deleteAll(Collection<?> clctn) throws CacheWriterException {
                    RequestContext context = null;
                    try {
                        context = ctxUtil.preInvokeRequestContext();
                        delegate.deleteAll(clctn);
                    }
                    finally {
                        ctxUtil.postInvokeRequestContext(context);
                    }
                }

                private final CacheWriter<K, V> delegate;
            }

            private static final long serialVersionUID = 1L;
        };
    }

    private final JavaEEContextUtil ctxUtil;
    private static final long serialVersionUID = 1L;
}
