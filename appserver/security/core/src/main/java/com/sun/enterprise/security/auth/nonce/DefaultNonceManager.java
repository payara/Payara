/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.auth.nonce;

import com.sun.logging.LogDomains;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ashutoshshahi
 */
public class DefaultNonceManager extends NonceManager{
    
    private static final boolean USE_DAEMON_THREAD = true;
    private static final Timer nonceCleanupTimer = new Timer(USE_DAEMON_THREAD);
    
    // Nonce Cache
    private NonceCache nonceCache = null;
    
    private static final Logger logger = 
        LogDomains.getLogger(DefaultNonceManager.class,LogDomains.SECURITY_LOGGER);
    
    public DefaultNonceManager() {
    }

    @Override
    public boolean validateNonce(Nonce nonce) throws NonceException {
        // set created to current time
        TimeZone utc = TimeZone.getTimeZone("UTC");
        String created = new GregorianCalendar(utc).getTime().toString();
        return validateNonce(nonce, created);
    }

    @Override
    public boolean validateNonce(Nonce nonce, String created) throws NonceException {
        if ((nonceCache == null) || ((nonceCache != null) && nonceCache.wasCanceled())) {
            initNonceCache(getMaxNonceAge());
        }
        //  check if the reclaimer Task is scheduled or not
        if (!nonceCache.isScheduled()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,
                        "About to Store a new Nonce, but Reclaimer not Scheduled, so scheduling one" + nonceCache);
            }
            setNonceCacheCleanup();
        }
        return nonceCache.validateAndCacheNonce(nonce, created);
    }
    
    public boolean hasNonce(Nonce nonce){
        if ((nonceCache == null) || ((nonceCache != null) && nonceCache.wasCanceled())) {
            initNonceCache(getMaxNonceAge());
        }
        return nonceCache.hasNonce(nonce);
    }
            
    private synchronized void setNonceCacheCleanup() {

        if (!nonceCache.isScheduled()) {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Scheduling Nonce Reclaimer task...... for " + this + ":" + nonceCache);
            }
            nonceCleanupTimer.schedule(
                    nonceCache,
                    nonceCache.getMaxNonceAge(), // run it the first time after
                    nonceCache.getMaxNonceAge()); //repeat every
            nonceCache.scheduled(true);
        }
    }
    
    private synchronized void initNonceCache(long maxNonceAge) {

        if (nonceCache == null) {
            nonceCache = new NonceCache(maxNonceAge);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Creating NonceCache for first time....." + nonceCache);
            }
        } else if (nonceCache.wasCanceled()) {
            nonceCache = new NonceCache(maxNonceAge);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Re-creating NonceCache because it was canceled....." + nonceCache);
            }
        }
    }

}
