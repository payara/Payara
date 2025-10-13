/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.digest.impl;

import jakarta.inject.Provider;

import org.glassfish.security.common.CNonceCache;
import org.glassfish.security.common.NonceInfo;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.AppCNonceCacheMap;
import com.sun.enterprise.security.CNonceCacheFactory;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.digest.impl.NestedDigestAlgoParamImpl;

public class CNonceValidator {

    private WebBundleDescriptor webDescriptor;
    private Provider<AppCNonceCacheMap> appCNonceCacheMapProvider;
    private Provider<CNonceCacheFactory> cNonceCacheFactoryProvider;

    private CNonceCacheFactory cNonceCacheFactory;
    private CNonceCache cnonces;
    private AppCNonceCacheMap haCNonceCacheMap;
    
    public CNonceValidator(WebBundleDescriptor webDescriptor, Provider<AppCNonceCacheMap> appCNonceCacheMapProvider, Provider<CNonceCacheFactory> cNonceCacheFactoryProvider) {
        this.webDescriptor = webDescriptor;
        this.appCNonceCacheMapProvider = appCNonceCacheMapProvider;
        this.cNonceCacheFactoryProvider = cNonceCacheFactoryProvider;
    }
    
    public DigestAlgorithmParameter[] validateCnonce(DigestAlgorithmParameter[] parameters) {
        if (cnonces == null) {
           init();
        }

        String cnonce = null;
        String nc = null;

        // Get cnonce and nc (nonce count) from the digest parameters
        for (DigestAlgorithmParameter digestParameter : parameters) {
            if (digestParameter instanceof NestedDigestAlgoParamImpl) {
                for (DigestAlgorithmParameter nestedDigestParameter : getNestedParams(digestParameter)) {
                    
                    if (isCnonce(nestedDigestParameter)) {
                        cnonce = new String(nestedDigestParameter.getValue());
                    } else if (isNc(nestedDigestParameter)) {
                        nc = new String(nestedDigestParameter.getValue());
                    }
                    
                    if (cnonce != null && nc != null) {
                        break;
                    }
                }
                
                if (cnonce != null && nc != null) {
                    break;
                }
            }
            
            if (isCnonce(digestParameter)) {
                cnonce = new String(digestParameter.getValue());
            } else if (isNc(digestParameter)) {
                nc = new String(digestParameter.getValue());
            }
        }
        
        long currentTime = System.currentTimeMillis();
        long count = getHexCount(nc);

        // Throws exception if validation fails
        NonceInfo info = getValidatedNonceInfo(cnonce, count);

        info.setCount(count);
        info.setTimestamp(currentTime);
        
        synchronized (cnonces) {
            cnonces.put(cnonce, info);
        }
        
        return parameters;
    }
    
    private void init() {
        String appName = webDescriptor.getApplication().getAppName();

        synchronized (this) {
            if (haCNonceCacheMap == null) {
                haCNonceCacheMap = appCNonceCacheMapProvider.get();
            }

            if (haCNonceCacheMap != null) {
                // get the initialized HA CNonceCache
                cnonces = haCNonceCacheMap.get(appName);
            }

            if (cnonces == null) {
                if (cNonceCacheFactory == null) {
                    cNonceCacheFactory = cNonceCacheFactoryProvider.get();
                }

                // create a Non-HA CNonce Cache
                cnonces = cNonceCacheFactory.createCNonceCache(webDescriptor.getApplication().getAppName(), null, null, null);
            }
        }
    }
    
    private NonceInfo getValidatedNonceInfo(String cnonce, long count) {
        NonceInfo info;
        synchronized (cnonces) {
            info = cnonces.get(cnonce);
        }

        if (info == null) {
            return new NonceInfo();
        }
        
        if (count <= info.getCount()) {
            throw new RuntimeException("Invalid Request : Possible Replay Attack detected ?");
        }
        
        return info;
    }
    
    private DigestAlgorithmParameter[] getNestedParams(DigestAlgorithmParameter digestParameter) {
        NestedDigestAlgoParamImpl nestedParameter = (NestedDigestAlgoParamImpl) digestParameter;
        
        return (DigestAlgorithmParameter[]) nestedParameter.getNestedParams();
    }
    
    private boolean isCnonce(DigestAlgorithmParameter digestParameter) {
        return "cnonce".equals(digestParameter.getName());
    }
    
    private boolean isNc(DigestAlgorithmParameter digestParameter) {
        return "nc".equals(digestParameter.getName());
    }
    
    private long getHexCount(String nc) {
        try {
            return Long.parseLong(nc, 16);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException(nfe);
        }
    }

}
