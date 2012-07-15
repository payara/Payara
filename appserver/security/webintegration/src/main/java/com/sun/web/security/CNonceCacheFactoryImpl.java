/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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


package com.sun.web.security;

import com.sun.enterprise.security.CNonceCacheFactory;
import org.glassfish.security.common.CNonceCache;
import com.sun.enterprise.config.serverbeans.SecurityService;

import java.util.HashMap;
import java.util.Map;
import org.glassfish.api.admin.ServerEnvironment;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

/**
 *
 * @author vbkumarjayanti
 */
@Service
@Singleton
public class CNonceCacheFactoryImpl implements CNonceCacheFactory, PostConstruct {

    @Inject
    @Named("HA-CNonceCache")
    private Provider<CNonceCache> cHANonceCacheProvider;

    @Inject
    @Named("CNonceCache")
    private Provider<CNonceCache> cNonceCacheProvider;


    @Inject()
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private SecurityService secService;

    /**
     * Maximum number of client nonces to keep in the cache. If not specified,
     * the default value of 1000 is used.
     */
    protected long cnonceCacheSize = 1000;

    /**
     * How long server nonces are valid for in milliseconds. Defaults to 5
     * minutes.
     */
    protected long nonceValidity = 5 * 60 * 1000;


    @Override
    public void postConstruct() {
        String sz = this.secService.getPropertyValue("NONCE_CACHE_SIZE");
        String age = this.secService.getPropertyValue("MAX_NONCE_AGE");
        if (sz != null) {
            this.cnonceCacheSize =  Long.parseLong(sz);
        }
        if (age != null) {
            this.nonceValidity = Long.parseLong(age);
        }
    }

    @Override
    public CNonceCache createCNonceCache(String appName, String clusterName, String instanceName, String storeName) {
        boolean haEnabled = (clusterName != null) && (instanceName != null) && (storeName != null);
        CNonceCache  cache = null;
        Map<String, String> map = new HashMap<String, String>();
        if (haEnabled) {
            cache = cHANonceCacheProvider.get();
            map.put(CLUSTER_NAME_PROP, clusterName);
            map.put(INSTANCE_NAME_PROP, instanceName);
        } else {
            cache = cNonceCacheProvider.get();
        }
        if (cache != null) {
            cache.init(cnonceCacheSize, storeName, nonceValidity, map);
        }
        return cache;
    }

}
