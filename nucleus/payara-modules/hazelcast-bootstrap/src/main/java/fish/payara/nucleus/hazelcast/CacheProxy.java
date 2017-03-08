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
package fish.payara.nucleus.hazelcast;

import fish.payara.nucleus.hazelcast.JavaEEContextUtil.Context;
import java.util.Map;
import java.util.Set;
import javax.cache.Cache;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;
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


    // +++ TODO create entry listner proxy
    @RequiredArgsConstructor
    private static class CELCProxy<K, V> implements CacheEntryListenerConfiguration<K, V> {
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
