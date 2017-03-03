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

import java.net.URI;
import java.util.Properties;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import org.glassfish.internal.api.ServerContext;

/**
 * Proxy all applicable factory calls
 * so Java EE context is propagated from within Hazelcast threads
 *
 * @author lprimak
 */
public class CacheManagerProxy implements CacheManager {
    public CacheManagerProxy(CacheManager cacheManager, ServerContext serverContext) {
        this.delegate = cacheManager;
        this.serverContext = serverContext;
    }

    @Override
    public CachingProvider getCachingProvider() {
        return delegate.getCachingProvider();
    }

    @Override
    public URI getURI() {
        return delegate.getURI();
    }

    @Override
    public ClassLoader getClassLoader() {
        return delegate.getClassLoader();
    }

    @Override
    public Properties getProperties() {
        return delegate.getProperties();
    }

    @Override
    public <K, V, C extends Configuration<K, V>> Cache<K, V> createCache(String string, C config) throws IllegalArgumentException {
        Cache<K, V> cache;
        if(config instanceof CompleteConfiguration) {
            CompleteConfiguration<K,V> cfg = new CompleteConfigurationProxy<>((CompleteConfiguration<K, V>)config, serverContext);
            cache = delegate.createCache(string, cfg);
        } else {
            cache = delegate.createCache(string, config);
        }

        return new CacheProxy<>(cache, serverContext);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String string, Class<K> type, Class<V> type1) {
        return delegate.getCache(string, type, type1);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String string) {
        return delegate.getCache(string);
    }

    @Override
    public Iterable<String> getCacheNames() {
        return delegate.getCacheNames();
    }

    @Override
    public void destroyCache(String string) {
        delegate.destroyCache(string);
    }

    @Override
    public void enableManagement(String string, boolean bln) {
        delegate.enableManagement(string, bln);
    }

    @Override
    public void enableStatistics(String string, boolean bln) {
        delegate.enableStatistics(string, bln);
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


    private final CacheManager delegate;
    private final ServerContext serverContext;
}
