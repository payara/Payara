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
package fish.payara.cdi.jsr107;

import fish.payara.cdi.jsr107.impl.NamedCache;
import com.hazelcast.core.HazelcastInstance;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.logging.Logger;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.annotation.CacheDefaults;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 *
 * @author steve
 */
public class JSR107Producer {
    
    private static final Logger logger = Logger.getLogger(JSR107Producer.class.getName());
    
    public JSR107Producer() {
        hazelcastCore = HazelcastCore.getCore();
    }
    
    HazelcastCore hazelcastCore;
    
    @Dependent
    @Produces
    CachingProvider getCachingProvider() {
        // TBD look for scoped Caching Providers in JNDI
        if (!hazelcastCore.isEnabled()) {
            logger.warning("Unable to inject CachingProvider as Hazelcast is Disabled");
            return null;
        }
        return hazelcastCore.getCachingProvider();
    }
    
    @Dependent
    @Produces
    CacheManager getCacheManager(InjectionPoint point) {
        if (!hazelcastCore.isEnabled()) {
            logger.warning("Unable to inject CacheManager as Hazelcast is Disabled");
            return null;
        }
        return hazelcastCore.getCachingProvider().getCacheManager();
    }
    
    @Dependent
    @Produces
    HazelcastInstance getHazelcast() {
        if (!hazelcastCore.isEnabled()) {
            logger.warning("Unable to inject HazelcastInstance as Hazelcast is Disabled");
            return null;
        }
        return hazelcastCore.getInstance();
    }
    
    
    /**
     * Produces a Cache for injection. If the @NamedCache annotation is present the
     * cache will be created based on the values of the annotation field
     * 
     * Otherwise the cache will be created with the cache name being equal
     * to the fully qualified name of the class into which it is injected
     * @param ip
     * @return 
     */
    @Produces
    public <K,V> Cache<K, V> createCache(InjectionPoint ip) {
        Cache<K, V> result;
        if (!hazelcastCore.isEnabled()) {
            logger.warning("Unable to inject Cache as Hazelcast is Disabled");
            return null;
        }

        //determine the cache name first start with the default name
        String cacheName = ip.getMember().getDeclaringClass().getCanonicalName();
        NamedCache ncqualifier = ip.getAnnotated().getAnnotation(NamedCache.class);      
        CacheManager manager = getCacheManager(ip);
        
        
        if (ncqualifier != null) {  // configure the cache based on the annotation
            String qualifierName = ncqualifier.cacheName();
            if (!"".equals(cacheName)) {
                cacheName = qualifierName;
            }
            Class keyClass = ncqualifier.keyClass();
            Class valueClass = ncqualifier.valueClass();           
            result = manager.getCache(cacheName, keyClass, valueClass);
            if (result == null) {
                MutableConfiguration<K, V> config = new MutableConfiguration<>();
                config.setTypes(keyClass, valueClass);
                
                // determine the expiry policy
                Class expiryPolicyFactoryClass = ncqualifier.expiryPolicyFactoryClass();
                if (!"Object".equals(expiryPolicyFactoryClass.getSimpleName())) {
                    Factory factory = FactoryBuilder.factoryOf(expiryPolicyFactoryClass);
                    config.setExpiryPolicyFactory(factory);
                }
                
                // determine the cache writer if any
                Class writerFactoryClass = ncqualifier.cacheWriterFactoryClass();
                if (!"Object".equals(writerFactoryClass.getSimpleName())) {
                    Factory factory = FactoryBuilder.factoryOf(writerFactoryClass);
                    config.setCacheWriterFactory(factory);
                }  
                config.setWriteThrough(ncqualifier.writeThrough());
                
                // determine the cache loader if any
                Class loaderFactoryClass = ncqualifier.cacheLoaderFactoryClass();
                if (!"Object".equals(loaderFactoryClass.getSimpleName())) {
                    Factory factory = FactoryBuilder.factoryOf(loaderFactoryClass);
                    config.setCacheLoaderFactory(factory);
                } 
                config.setReadThrough(ncqualifier.readThrough());
                
                config.setManagementEnabled(ncqualifier.managementEnabled());
                config.setStatisticsEnabled(ncqualifier.statisticsEnabled());                
                result = manager.createCache(cacheName, config);                
            }
        } else {  // configure a "raw" cache
            Bean<?> bean = ip.getBean();
            if (bean != null) {
                Class<?> beanClass = bean.getBeanClass();
                CacheDefaults defaults = beanClass.getAnnotation(CacheDefaults.class);
                if (defaults != null) {
                    String cacheNameFromAnnotation = defaults.cacheName();
                    if (!"".equals(cacheNameFromAnnotation)) {
                        cacheName = cacheNameFromAnnotation;
                    }
                }
            }
            result = manager.getCache(cacheName);
            if (result == null) {
                MutableConfiguration<K, V> config = new MutableConfiguration<>();
                config.setManagementEnabled(true);
                config.setStatisticsEnabled(true);
                result = manager.createCache(cacheName, config);
            }
        }
        return result;
    }
    
}
