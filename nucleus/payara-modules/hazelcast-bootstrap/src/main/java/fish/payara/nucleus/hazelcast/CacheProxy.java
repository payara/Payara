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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import org.glassfish.internal.api.ServerContext;

/**
 * proxy the cache so we can set up invocation context for
 * the Hazelcast thread
 *
 * @author lprimak
 * @param <K> key
 * @param <V> value
 */
public class CacheProxy<K, V> implements Cache<K, V> {
    public CacheProxy(Cache<K, V> delegate, ServerContext serverContext) {
        this.delegate = delegate;
        this.serverContext = serverContext;
    }

    @Override
    public V get(K k) {
        return delegate.get(k);
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> set) {
        return delegate.getAll(set);
    }

    @Override
    public boolean containsKey(K k) {
        return delegate.containsKey(k);
    }

    private static class CPLProxy implements CompletionListener {
        public CPLProxy(CompletionListener delegate, ServerContext serverContext) {
            this.delegate = delegate;
            this.ctxUtil = new JavaEEContextUtil(serverContext);
        }

        @Override
        public void onCompletion() {
            Context ctx = null;
            try {
                ctx = ctxUtil.preInvoke();
                delegate.onCompletion();
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }

        @Override
        public void onException(Exception excptn) {
            Context ctx = null;
            try {
                ctx = ctxUtil.preInvoke();
                delegate.onException(excptn);
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }

        private final CompletionListener delegate;
        private final JavaEEContextUtil ctxUtil;
    }

    @Override
    public void loadAll(Set<? extends K> set, boolean bln, CompletionListener cl) {
        if(!(cl instanceof CPLProxy)) {
            cl = new CPLProxy(cl, serverContext);
        }
        delegate.loadAll(set, bln, cl);
    }

    @Override
    public void put(K k, V v) {
        delegate.put(k, v);
    }

    @Override
    public V getAndPut(K k, V v) {
        return delegate.getAndPut(k, v);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate.putAll(map);
    }

    @Override
    public boolean putIfAbsent(K k, V v) {
        return delegate.putIfAbsent(k, v);
    }

    @Override
    public boolean remove(K k) {
        return delegate.remove(k);
    }

    @Override
    public boolean remove(K k, V v) {
        return delegate.remove(k, v);
    }

    @Override
    public V getAndRemove(K k) {
        return delegate.getAndRemove(k);
    }

    @Override
    public boolean replace(K k, V v, V v1) {
        return delegate.replace(k, v, v1);
    }

    @Override
    public boolean replace(K k, V v) {
        return delegate.replace(k, v);
    }

    @Override
    public V getAndReplace(K k, V v) {
        return delegate.getAndReplace(k, v);
    }

    @Override
    public void removeAll(Set<? extends K> set) {
        delegate.removeAll(set);
    }

    @Override
    public void removeAll() {
        delegate.removeAll();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> type) {
        return delegate.getConfiguration(type);
    }

    @Override
    public <T> T invoke(K k, EntryProcessor<K, V, T> ep, Object... os) throws EntryProcessorException {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, serverContext);
        }
        return delegate.invoke(k, ep, os);
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> ep, Object... os) {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, serverContext);
        }
        return delegate.invokeAll(set, ep, os);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public CacheManager getCacheManager() {
        return delegate.getCacheManager();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean isClosed() {
        return delegate.isClosed();
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        return delegate.unwrap(type);
    }

    // +++ TODO create entry listner proxy
    private static class CELCProxy<K, V> implements CacheEntryListenerConfiguration<K, V> {
        public CELCProxy(CacheEntryListenerConfiguration<K, V> delegate, ServerContext serverContext) {
            this.delegate = delegate;
            this.ctxUtil = new JavaEEContextUtil(serverContext);
        }

        @Override
        public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
            Context ctx = null;
            try {
                ctxUtil.preInvoke();
                return delegate.getCacheEntryListenerFactory();
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }

        @Override
        public boolean isOldValueRequired() {
            Context ctx = null;
            try {
                ctxUtil.preInvoke();
                return delegate.isOldValueRequired();
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }

        @Override
        public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryEventFilterFactory() {
            Context ctx = null;
            try {
                ctxUtil.preInvoke();
                return delegate.getCacheEntryEventFilterFactory();
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }

        @Override
        public boolean isSynchronous() {
            Context ctx = null;
            try {
                ctxUtil.preInvoke();
                return delegate.isSynchronous();
            }
            finally {
                ctxUtil.postInvoke(ctx);
            }
        }


        private final CacheEntryListenerConfiguration<K, V> delegate;
        private final JavaEEContextUtil ctxUtil;

        private static final long serialVersionUID = 1L;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> celc) {
        if(!(celc instanceof CELCProxy)) {
            celc = new CELCProxy<>(celc, serverContext);
        }
        delegate.registerCacheEntryListener(celc);
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> celc) {
        delegate.deregisterCacheEntryListener(celc);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return delegate.iterator();
    }

    private final Cache<K, V> delegate;
    private final ServerContext serverContext;
}
