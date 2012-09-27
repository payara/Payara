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

import org.glassfish.security.common.NonceInfo;
import org.glassfish.security.common.CNonceCache;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.catalina.util.StringManager;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;



/**
 *
 * @author vbkumarjayanti
 */
@Service(name = "CNonceCache")
@PerLookup
public final class CNonceCacheImpl extends LinkedHashMap<String, NonceInfo> implements CNonceCache {

    private static final long LOG_SUPPRESS_TIME = 5 * 60 * 1000;

    private long lastLog = 0;

    private static final Logger log = Logger.getLogger(
        CNonceCacheImpl.class.getName());

    private String  eldestCNonce = null;
    private String  storeName = null;
    /**
     * The string manager for this package.
     */
    static final StringManager sm =
            StringManager.getManager("org.apache.catalina.util");

    public CNonceCacheImpl() {

    }

    /**
     * Maximum number of client nonces to keep in the cache. If not specified,
     * the default value of 1000 is used.
     */
    long cnonceCacheSize = 1000;

    /**
     * How long server nonces are valid for in milliseconds. Defaults to 5
     * minutes.
     */
    long nonceValidity = 5 * 60 * 1000;

    @Override
    protected boolean removeEldestEntry(
            Map.Entry<String, NonceInfo> eldest) {
        // This is called from a sync so keep it simple
        long currentTime = System.currentTimeMillis();
        eldestCNonce = eldest.getKey();
        if (size() > getCnonceCacheSize()) {
            if (lastLog < currentTime
                    && currentTime - eldest.getValue().getTimestamp()
                    < getNonceValidity()) {
                // Replay attack is possible
                log.warning(sm.getString(
                        "digestAuthenticator.cacheRemove"));
                lastLog = currentTime + LOG_SUPPRESS_TIME;
            }
            return true;
        }
        return false;
    }


    /**
     * @return the cnonceCacheSize
     */
    @Override
    public long getCnonceCacheSize() {
        return cnonceCacheSize;
    }

    /**
     * @return the nonceValidity
     */
    @Override
    public long getNonceValidity() {
        return nonceValidity;
    }

    /**
     * @return the eldestCNonce
     */
    public String getEldestCNonce() {
        return eldestCNonce;
    }

    @Override
    public void init(long size, String name, long validity, Map<String, String> props) {
        this.storeName = name;
        this.cnonceCacheSize = size;
        this.nonceValidity = validity;
    }

    @Override
    public void setCnonceCacheSize(long cnonceCacheSize) {
        this.cnonceCacheSize = cnonceCacheSize;
    }

    @Override
    public void setNonceValidity(long nonceValidity) {
        this.nonceValidity = nonceValidity;
    }

    @Override
    public void destroy() {
        clear();
    }

}
