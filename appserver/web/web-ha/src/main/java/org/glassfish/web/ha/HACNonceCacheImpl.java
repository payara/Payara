/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.web.ha;

import static com.sun.enterprise.security.CNonceCacheFactory.CLUSTER_NAME_PROP;
import static com.sun.enterprise.security.CNonceCacheFactory.INSTANCE_NAME_PROP;
import static java.util.logging.Level.WARNING;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.security.common.CNonceCache;
import org.glassfish.security.common.NonceInfo;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.auth.digest.impl.CNonceCacheImpl;

/**
 *
 * @author vbkumarjayanti
 */
@Service(name = "HA-CNonceCache")
@PerLookup
public class HACNonceCacheImpl implements CNonceCache {
    
    private static final Logger logger = LogFacade.getLogger();
    private static final String BS_TYPE_REPLICATED = "replicated";

    @Inject
    private ServiceLocator services;

    private CNonceCacheImpl localStore;
    private BackingStore<String, NonceInfo> backingStore;
    private String storeName;
    private Map<String, String> props;


    /**
     * @param cnonceCacheSize the cnonceCacheSize to set
     */
    @Override
    public void setCnonceCacheSize(long cnonceCacheSize) {
        localStore.setCnonceCacheSize(cnonceCacheSize);
    }

    /**
     * @param nonceValidity the nonceValidity to set
     */
    @Override
    public void setNonceValidity(long nonceValidity) {
        localStore.setNonceValidity(nonceValidity);
    }

    /**
     * @return the cnonceCacheSize
     */
    @Override
    public long getCnonceCacheSize() {
        return localStore.getCnonceCacheSize();
    }

    /**
     * @return the nonceValidity
     */
    @Override
    public long getNonceValidity() {
        return localStore.getNonceValidity();
    }

    @Override
    public int size() {
        return localStore.size();
    }

    @Override
    public boolean isEmpty() {
        return localStore.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return localStore.containsKey((String) o);
    }

    @Override
    public boolean containsValue(Object o) {
        return localStore.containsValue((NonceInfo) o);
    }

    @Override
    public NonceInfo get(Object o) {
        NonceInfo ret = localStore.get((String) o);
        if (ret == null && backingStore != null) {
            try {
                return backingStore.load((String) o, null);
            } catch (BackingStoreException ex) {
                // TODO exception message
                logger.log(WARNING, null, ex);
            }
        }
        
        return ret;
    }

    @Override
    public NonceInfo put(String key, NonceInfo value) {
        NonceInfo ret = localStore.put(key, value);
        if (backingStore == null) {
            return ret;
        }
        
        try {
            if (removeEldestEntry(null)) {
                backingStore.remove(localStore.getEldestCNonce());
            }
            backingStore.save(key, value, true);
        } catch (BackingStoreException ex) {
            // TODO: EX message
            logger.log(WARNING, null, ex);
        }

        return ret;
    }

    @Override
    public NonceInfo remove(Object o) {
        NonceInfo ret = localStore.remove((String) o);
        if (backingStore == null) {
            return ret;
        }
        
        try {
            backingStore.remove((String) o);
        } catch (BackingStoreException ex) {
            // TODO: EX message
            logger.log(WARNING, null, ex);
        }
        
        return ret;
    }

    // we do not need to support the below operation
    @Override
    public void putAll(Map<? extends String, ? extends NonceInfo> map) {
        throw new UnsupportedOperationException("putAll : Not  Supported");
    }

    @Override
    public void clear() {
        if (backingStore != null) {
            for (String key : keySet()) {
                try {
                    backingStore.remove(key);
                } catch (BackingStoreException ex) {
                    // TODO: EX message
                    logger.log(WARNING, null, ex);
                }
            }
        }
        
        localStore.clear();
    }

    @Override
    public Set<String> keySet() {
        return localStore.keySet();
    }

    @Override
    public Collection<NonceInfo> values() {
        return localStore.values();
    }

    @Override
    public Set<Entry<String, NonceInfo>> entrySet() {
        return localStore.entrySet();
    }

    protected boolean removeEldestEntry(Map.Entry<String, NonceInfo> eldest) {
        // This is called from a sync so keep it simple
        if (size() > getCnonceCacheSize()) {
            return true;
        }
        
        return false;
    }

    public void postConstruct() {
        localStore = new CNonceCacheImpl();
        try {
            BackingStoreConfiguration<String, NonceInfo> bsConfig = new BackingStoreConfiguration<String, NonceInfo>();
            
            bsConfig.setClusterName(props.get(CLUSTER_NAME_PROP))
                    .setInstanceName(props.get(INSTANCE_NAME_PROP))
                    .setStoreName(storeName).setKeyClazz(String.class)
                    .setValueClazz(NonceInfo.class);
            
            backingStore = services.getService(BackingStoreFactory.class, BS_TYPE_REPLICATED)
                                   .createBackingStore(bsConfig);
            
        } catch (BackingStoreException ex) {
            logger.log(WARNING, null, ex);
        }
    }

    @Override
    public void init(long size, String name, long validity, Map<String, String> props) {
        this.storeName = name;
        this.props = props;
        postConstruct();
        localStore.setCnonceCacheSize(size);
        localStore.setNonceValidity(validity);

    }

    @Override
    public void destroy() {
        clear();
        
        try {
            if (backingStore != null) {
                backingStore.destroy();
            }
        } catch (Exception ex) {
        }
    }

}
