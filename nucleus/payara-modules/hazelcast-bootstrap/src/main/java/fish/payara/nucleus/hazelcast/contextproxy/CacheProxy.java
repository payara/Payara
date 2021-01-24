/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.nucleus.hazelcast.contextproxy;

import org.glassfish.internal.api.JavaEEContextUtil;
import org.glassfish.internal.api.JavaEEContextUtil.Context;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

/**
 * proxy the cache so we can set up invocation context for
 * the Hazelcast thread
 *
 * @author lprimak
 * @param <K> key
 * @param <V> value
 */
public class CacheProxy<K, V> implements Cache<K, V> {

    private static class CPLProxy implements CompletionListener {
        @Override
        public void onCompletion() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                delegate.onCompletion();
            }
        }

        @Override
        public void onException(Exception excptn) {
            try (Context ctx = ctxUtilInst.pushContext()) {
                delegate.onException(excptn);
            }
        }

        public CPLProxy(CompletionListener delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final CompletionListener delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;
    }

    @Override
    public void loadAll(Set<? extends K> set, boolean bln, CompletionListener cl) {
        if(!(cl instanceof CPLProxy)) {
            cl = new CPLProxy(cl, ctxUtilInst);
        }
        delegate.loadAll(set, bln, cl);
    }

    @Override
    public <T> T invoke(K k, EntryProcessor<K, V, T> ep, Object... os) throws EntryProcessorException {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, ctxUtilInst);
        }
        return delegate.invoke(k, ep, os);
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> ep, Object... os) {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, ctxUtilInst);
        }
        return delegate.invokeAll(set, ep, os);
    }

    private static class CELProxy<K, V> implements CacheEntryCreatedListener<K, V>, CacheEntryExpiredListener<K, V>,
            CacheEntryRemovedListener<K, V>, CacheEntryUpdatedListener<K, V> {
        @Override
        public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryCreatedListener<K, V> listener = (CacheEntryCreatedListener<K, V>)delegate;
            try (Context ctx = ctxUtilInst.pushRequestContext()) {
                listener.onCreated(itrbl);
            }
        }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryExpiredListener<K, V> listener = (CacheEntryExpiredListener<K, V>)delegate;
            try (Context ctx = ctxUtilInst.pushRequestContext()) {
                listener.onExpired(itrbl);
            }
        }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryRemovedListener<K, V> listener = (CacheEntryRemovedListener<K, V>)delegate;
            try (Context ctx = ctxUtilInst.pushRequestContext()) {
                listener.onRemoved(itrbl);
            }
        }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryUpdatedListener<K, V> listener = (CacheEntryUpdatedListener<K, V>)delegate;
            try (Context ctx = ctxUtilInst.pushRequestContext()) {
                listener.onUpdated(itrbl);
            }
        }

        public CELProxy(CacheEntryListener<K, V> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final CacheEntryListener<K, V> delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;
    }

    private static class CELFProxy<K, V> implements Factory<CacheEntryListener<? super K, ? super V>> {
        @Override
        public CacheEntryListener<? super K, ? super V> create() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return new CELProxy<>(delegate.create(), ctxUtilInst);
            }
        }

        public CELFProxy(Factory<CacheEntryListener<? super K, ? super V>> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final Factory<CacheEntryListener<? super K, ? super V>> delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;
        private static final long serialVersionUID = 1L;
    }

    private static class CEEVProxy<K, V> implements CacheEntryEventFilter<K, V> {
        @Override
        public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> cee) throws CacheEntryListenerException {
            try (Context ctx = ctxUtilInst.pushRequestContext()) {
                return delegate.evaluate(cee);
            }
        }

        public CEEVProxy(CacheEntryEventFilter<K, V> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final CacheEntryEventFilter<K, V> delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;
    }

    private static class CEEVFProxy<K, V> implements Factory<CacheEntryEventFilter<? super K, ? super V>> {
        @Override
        public CacheEntryEventFilter<? super K, ? super V> create() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return new CEEVProxy<>(delegate.create(), ctxUtilInst);
            }
        }

        public CEEVFProxy(Factory<CacheEntryEventFilter<? super K, ? super V>> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final Factory<CacheEntryEventFilter<? super K, ? super V>> delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;
        private static final long serialVersionUID = 1L;
    }

    private static class CELCProxy<K, V> implements CacheEntryListenerConfiguration<K, V> {
        @Override
        public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return new CELFProxy<>(delegate.getCacheEntryListenerFactory(), ctxUtilInst);
            }
        }

        @Override
        public boolean isOldValueRequired() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return delegate.isOldValueRequired();
            }
        }

        @Override
        public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryEventFilterFactory() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return new CEEVFProxy<>(delegate.getCacheEntryEventFilterFactory(), ctxUtilInst);
            }
        }

        @Override
        public boolean isSynchronous() {
            try (Context ctx = ctxUtilInst.pushContext()) {
                return delegate.isSynchronous();
            }
        }

        public CELCProxy(CacheEntryListenerConfiguration<K, V> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
            this.delegate = delegate;
            this.ctxUtilInst = ctxUtilInst;
        }

        private final CacheEntryListenerConfiguration<K, V> delegate;
        private final JavaEEContextUtil.Instance ctxUtilInst;

        private static final long serialVersionUID = 1L;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> celc) {
        if(!(celc instanceof CELCProxy)) {
            celc = new CELCProxy<>(celc, ctxUtilInst);
        }
        delegate.registerCacheEntryListener(celc);
    }

    private final Cache<K, V> delegate;
    private final JavaEEContextUtil.Instance ctxUtilInst;

    @Override
    public V get(K key) {
        return delegate.get(key);
    }

    @Override
    public Map<K, V> getAll(Set<? extends K> keys) {
        return delegate.getAll(keys);
    }

    public CacheProxy(Cache<K, V> delegate, JavaEEContextUtil.Instance ctxUtilInst) {
        this.delegate = delegate;
        this.ctxUtilInst = ctxUtilInst;
    }

    @Override
    public boolean containsKey(K key) {
        return delegate.containsKey(key);
    }

    @Override
    public void put(K key, V value) {
        delegate.put(key, value);
    }

    @Override
    public V getAndPut(K key, V value) {
        return delegate.getAndPut(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        delegate.putAll(map);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(K key) {
        return delegate.remove(key);
    }

    @Override
    public boolean remove(K key, V oldValue) {
        return delegate.remove(key, oldValue);
    }

    @Override
    public V getAndRemove(K key) {
        return delegate.getAndRemove(key);
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public boolean replace(K key, V value) {
        return delegate.replace(key, value);
    }

    @Override
    public V getAndReplace(K key, V value) {
        return delegate.getAndReplace(key, value);
    }

    @Override
    public void removeAll(Set<? extends K> keys) {
        delegate.removeAll(keys);
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
    public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
        return delegate.getConfiguration(clazz);
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
    public <T> T unwrap(Class<T> clazz) {
        return delegate.unwrap(clazz);
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {
        delegate.deregisterCacheEntryListener(cacheEntryListenerConfiguration);
    }

    @Override
    public Iterator<Entry<K, V>> iterator() {
        return delegate.iterator();
    }
}
