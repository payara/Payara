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
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.glassfish.internal.api.ServerContext;

/**
 * Proxy all applicable factory calls
 * so Java EE context is propagated from within Hazelcast threads
 *
 * @author lprimak
 */
@RequiredArgsConstructor
public class CacheManagerProxy implements CacheManager {
    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String string, C config) throws IllegalArgumentException {
        Cache<K, V> cache;
        JavaEEContextUtil ctxUtil = serverContext.getDefaultServices().getService(JavaEEContextUtil.class);
        if(ctxUtil != null && config instanceof CompleteConfiguration) {
            CompleteConfiguration<K, V> cfg = new CompleteConfigurationProxy<>((CompleteConfiguration<K, V>)config, ctxUtil);
            cache = delegate.createCache(string, cfg);
        } else {
            cache = delegate.createCache(string, config);
        }

        return ctxUtil != null? new CacheProxy<>(cache, ctxUtil) : cache;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName) {
        JavaEEContextUtil ctxUtil = serverContext.getDefaultServices().getService(JavaEEContextUtil.class);
        Cache<K, V> cache = delegate.getCache(cacheName);
        return cache != null? new CacheProxy<>(cache, ctxUtil) : null;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        JavaEEContextUtil ctxUtil = serverContext.getDefaultServices().getService(JavaEEContextUtil.class);
        Cache<K, V> cache = delegate.getCache(cacheName, keyType, valueType);
        return cache != null? new CacheProxy<>(cache, ctxUtil) : null;
    }


    private interface Exclusions {
        public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String string, C config) throws IllegalArgumentException;
        public <K, V> Cache<K, V> getCache(String cacheName);
        public <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType);
    }


    private final @Delegate(excludes = Exclusions.class ) CacheManager delegate;
    private final ServerContext serverContext;
}
