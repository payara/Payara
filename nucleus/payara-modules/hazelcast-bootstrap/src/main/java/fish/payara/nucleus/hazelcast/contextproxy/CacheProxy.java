/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
import org.glassfish.internal.api.JavaEEContextUtil.RequestContext;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;
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
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

/**
 * proxy the cache so we can set up invocation context for
 * the Hazelcast thread
 *
 * @author lprimak
 * @param <K> key
 * @param <V> value
 */
@RequiredArgsConstructor
public class CacheProxy<K, V> implements Cache<K, V> {
    @RequiredArgsConstructor
    private static class CPLProxy implements CompletionListener {
        @Override
        public void onCompletion() {
            Context ctx = null;
            try {
                ctx = ctxUtil.pushContext();
                delegate.onCompletion();
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        @Override
        public void onException(Exception excptn) {
            Context ctx = null;
            try {
                ctx = ctxUtil.pushContext();
                delegate.onException(excptn);
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        private interface Exclusions {
            void onCompletion();
            void onException(Exception excptn);
        }

        private final @Delegate(excludes = Exclusions.class) CompletionListener delegate;
        private final JavaEEContextUtil ctxUtil;
    }

    @Override
    public void loadAll(Set<? extends K> set, boolean bln, CompletionListener cl) {
        if(!(cl instanceof CPLProxy)) {
            cl = new CPLProxy(cl, ctxUtil);
        }
        delegate.loadAll(set, bln, cl);
    }

    @Override
    public <T> T invoke(K k, EntryProcessor<K, V, T> ep, Object... os) throws EntryProcessorException {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, ctxUtil);
        }
        return delegate.invoke(k, ep, os);
    }

    @Override
    public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> ep, Object... os) {
        if(!(ep instanceof EntryProcessorProxy)) {
            ep = new EntryProcessorProxy<>(ep, ctxUtil);
        }
        return delegate.invokeAll(set, ep, os);
    }

    @RequiredArgsConstructor
    private static class CELProxy<K, V> implements CacheEntryCreatedListener<K, V>, CacheEntryExpiredListener<K, V>,
            CacheEntryRemovedListener<K, V>, CacheEntryUpdatedListener<K, V> {
        @Override
        public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryCreatedListener<K, V> listener = (CacheEntryCreatedListener<K, V>)delegate;
            RequestContext ctx = ctxUtil.pushRequestContext();
            try {
                listener.onCreated(itrbl);
            }
            finally {
                ctxUtil.popRequestContext(ctx);
            }
        }

        @Override
        public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryExpiredListener<K, V> listener = (CacheEntryExpiredListener<K, V>)delegate;
            RequestContext ctx = ctxUtil.pushRequestContext();
            try {
                listener.onExpired(itrbl);
            }
            finally {
                ctxUtil.popRequestContext(ctx);
            }
        }

        @Override
        public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryRemovedListener<K, V> listener = (CacheEntryRemovedListener<K, V>)delegate;
            RequestContext ctx = ctxUtil.pushRequestContext();
            try {
                listener.onRemoved(itrbl);
            }
            finally {
                ctxUtil.popRequestContext(ctx);
            }
        }

        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> itrbl) throws CacheEntryListenerException {
            CacheEntryUpdatedListener<K, V> listener = (CacheEntryUpdatedListener<K, V>)delegate;
            RequestContext ctx = ctxUtil.pushRequestContext();
            try {
                listener.onUpdated(itrbl);
            }
            finally {
                ctxUtil.popRequestContext(ctx);
            }
        }

        private final CacheEntryListener<K, V> delegate;
        private final JavaEEContextUtil ctxUtil;
    }

    @RequiredArgsConstructor
    private static class CELFProxy<K, V> implements Factory<CacheEntryListener<? super K, ? super V>> {
        @Override
        public CacheEntryListener<? super K, ? super V> create() {
            Context ctx = ctxUtil.pushContext();
            try {
                return new CELProxy<>(delegate.create(), ctxUtil);
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        private final Factory<CacheEntryListener<? super K, ? super V>> delegate;
        private final JavaEEContextUtil ctxUtil;
        private static final long serialVersionUID = 1L;
    }

    @RequiredArgsConstructor
    private static class CEEVProxy<K, V> implements CacheEntryEventFilter<K, V> {
        @Override
        public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> cee) throws CacheEntryListenerException {
            RequestContext ctx = ctxUtil.pushRequestContext();
            try {
                return delegate.evaluate(cee);
            }
            finally {
                ctxUtil.popRequestContext(ctx);
            }
        }

        private final CacheEntryEventFilter<K, V> delegate;
        private final JavaEEContextUtil ctxUtil;
    }

    @RequiredArgsConstructor
    private static class CEEVFProxy<K, V> implements Factory<CacheEntryEventFilter<? super K, ? super V>> {
        @Override
        public CacheEntryEventFilter<? super K, ? super V> create() {
            Context ctx = ctxUtil.pushContext();
            try {
                return new CEEVProxy<>(delegate.create(), ctxUtil);
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        private final Factory<CacheEntryEventFilter<? super K, ? super V>> delegate;
        private final JavaEEContextUtil ctxUtil;
        private static final long serialVersionUID = 1L;
    }

    @RequiredArgsConstructor
    private static class CELCProxy<K, V> implements CacheEntryListenerConfiguration<K, V> {
        @Override
        public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
            Context ctx = null;
            try {
                ctxUtil.pushContext();
                return new CELFProxy<>(delegate.getCacheEntryListenerFactory(), ctxUtil);
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        @Override
        public boolean isOldValueRequired() {
            Context ctx = null;
            try {
                ctxUtil.pushContext();
                return delegate.isOldValueRequired();
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        @Override
        public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryEventFilterFactory() {
            Context ctx = null;
            try {
                ctxUtil.pushContext();
                return new CEEVFProxy<>(delegate.getCacheEntryEventFilterFactory(), ctxUtil);
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }

        @Override
        public boolean isSynchronous() {
            Context ctx = null;
            try {
                ctxUtil.pushContext();
                return delegate.isSynchronous();
            }
            finally {
                ctxUtil.popContext(ctx);
            }
        }


        private final CacheEntryListenerConfiguration<K, V> delegate;
        private final JavaEEContextUtil ctxUtil;

        private static final long serialVersionUID = 1L;
    }

    @Override
    public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> celc) {
        if(!(celc instanceof CELCProxy)) {
            celc = new CELCProxy<>(celc, ctxUtil);
        }
        delegate.registerCacheEntryListener(celc);
    }

    private interface Exclusions<K, V> {
        void loadAll(Set<? extends K> set, boolean bln, CompletionListener cl);
        <T> T invoke(K k, EntryProcessor<K, V, T> ep, Object... os) throws EntryProcessorException;
        <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> set, EntryProcessor<K, V, T> ep, Object... os);
        void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> celc);
    }

    
    private final @Delegate(excludes = Exclusions.class) Cache<K, V> delegate;
    private final JavaEEContextUtil ctxUtil;
}
