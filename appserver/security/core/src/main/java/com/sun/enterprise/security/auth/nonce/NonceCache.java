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

import java.util.Hashtable;
import java.util.TimerTask;
import com.sun.enterprise.security.auth.nonce.NonceManager.NonceException;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author ashutoshshahi
 */
public class NonceCache extends TimerTask{
    
    private static final Logger logger = 
        LogDomains.getLogger(NonceCache.class,LogDomains.SECURITY_LOGGER);
    
    public static final long MAX_NONCE_AGE=900000;
    
     // Nonce Cache
    private Hashtable<Nonce, String> nonceCache = new Hashtable<Nonce, String>();
    private Hashtable<Nonce, String> oldNonceCache = new Hashtable<Nonce, String>();
    
    private long maxNonceAge = MAX_NONCE_AGE;
    
        // flag to indicate if this timertask is scheduled into the Timer queue
    private boolean scheduledFlag = false;
    private boolean canceledFlag = false;
    
    public NonceCache() {}
   
    public NonceCache(long maxNonceAge) {
        this.maxNonceAge = maxNonceAge;
    }

    @Override
    public void run() {
        
        if (nonceCache.size() == 0) {
            cancel();
            if (logger.isLoggable(Level.FINE)){
                logger.log(Level.FINE, "Canceled Timer Task due to inactivity ...for " + this); 
            }
            return;
        }

        if (logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, "Clearing old Nonce values...for " + this);
        }
        
        oldNonceCache.clear();
        Hashtable temp = nonceCache;
        nonceCache = oldNonceCache;
        oldNonceCache = temp;
    }
    
    public boolean validateAndCacheNonce(Nonce nonce, String created) throws NonceException {
        if (nonceCache.containsKey(nonce)|| oldNonceCache.containsKey(nonce)) {
           // logger.log(Level.WARNING,
             //       "Nonce Repeated : Nonce Cache already contains the nonce value :" + nonce);
            //throw new NonceManager.NonceException("Nonce Repeated : Nonce Cache already contains the nonce value :" + nonce);
            return false;
        }
        
        if (logger.isLoggable(Level.FINE)){
            logger.log(Level.FINE, "Storing Nonce Value " + nonce  + " into " + this);
        }
        
        nonceCache.put(nonce, created);
        return true;
    }
    
    public boolean hasNonce(Nonce nonce){
        if (nonceCache.containsKey(nonce)|| oldNonceCache.containsKey(nonce)) {
           return true;
        }
        return false;
    }
    
    public boolean isScheduled() {
        return scheduledFlag;
    }

    public void scheduled(boolean flag) {
        scheduledFlag = flag;
    }

    public boolean wasCanceled() {
        return canceledFlag;
    }
    
    @Override
    public boolean cancel() {
        boolean ret = super.cancel();
        canceledFlag = true;
        oldNonceCache.clear();
        nonceCache.clear();

        return ret;
    }
    
    public long getMaxNonceAge() {
        return maxNonceAge;
    }

}
