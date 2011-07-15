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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 *
 * @author ashutoshshahi
 */
public abstract class NonceManager {
    
    private static Map<String, NonceManager> nonceMgrMap = null;
    private long maxNonceAge;
    
    private static final Logger logger = 
        LogDomains.getLogger(NonceManager.class,LogDomains.SECURITY_LOGGER);
    
    /**
     * 
     * @return the approximate maximum age for which a recieved nonce would be stored by the NonceManager
     */
    public long getMaxNonceAge() {
        return maxNonceAge;
    }
    
    /**
     * Set the approximate maximum age for which a recieved nonce needs to be stored by the NonceManager
     * @param maxNonceAge  
     */
    public void setMaxNonceAge(long maxNonceAge) {
        this.maxNonceAge = maxNonceAge;
    }
    
    /**
     * Exception to be thrown when an Error in processing Recieved Nonces occurs. 
     * A Nonce-replay would also be indicated by a NonceException.
     */
    public static class NonceException extends SecurityException {

        /**
         * Constructor specifying the message string.
         * @param message the exception message string
         */
        public NonceException(String message) {
            super(message);
        }

        /**
         * Constructor specifying the message string and a  nested exception
         * @param message the exception message string
         * @param cause the nested exception as a Throwable
         */
        public NonceException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Constructor specifying a nested exception
         * @param cause the nested exception as a Throwable
         */
        public NonceException(Throwable cause) {
            super(cause);
        }
    
    }
    
    /**
     * 
     * @param nonce the nonce to be validated
     * @return true if the nonce is not a replay
     * @throws com.sun.xml.wss.NonceManager.NonceException  if a replay is detected
     */
    public abstract boolean validateNonce(Nonce nonce) throws NonceException;


    public abstract boolean hasNonce(Nonce nonce);
    
    /**
     * 
     * @param nonce the nonce to be validated
     * @param created the creation time of the nonce as indicated in the UsernameToken
     * @return true if the nonce is not a replay
     * @throws com.sun.xml.wss.NonceManager.NonceException  if a replay is detected
     */
    public abstract boolean validateNonce(Nonce nonce, String created) throws NonceException;
    
    /**
     * 
     * @parem id The unique id for the component that wants to use a NonceManager
     * @param maxNonceAge the approximate maximum age for which a recieved nonce would be stored by the NonceManager
     * @return the singleton instance of the configured NonceManager, calling getInstance with different maxNonceAge 
     * will have no effect and will instead return the same NonceManager which was initialized first.
     */
    public static synchronized NonceManager getInstance(String id ,long maxNonceAge) {
        if(nonceMgrMap == null){
            nonceMgrMap = new HashMap<String, NonceManager>();
        }
        if (nonceMgrMap.get(id) != null) {
            return nonceMgrMap.get(id);
        }
        
        NonceManager nonceMgr =  new DefaultNonceManager();
        nonceMgr.setMaxNonceAge(maxNonceAge);
        nonceMgrMap.put(id, nonceMgr);
        return nonceMgr;
    }
    
    public static synchronized NonceManager getInstance(String className, String id, long maxNonceAge){
        
        try{
            if(nonceMgrMap == null){
                nonceMgrMap = new HashMap<String, NonceManager>();
            }
            if (nonceMgrMap.get(id) != null) {
                return nonceMgrMap.get(id);
            }
            Class nonceMgrClass=null;
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader != null) {
                nonceMgrClass = classLoader.loadClass(className);
            }
            if (nonceMgrClass == null) {
                nonceMgrClass = Class.forName(className);
            }
            NonceManager nonceMgr = (NonceManager)nonceMgrClass.newInstance();
            nonceMgr.setMaxNonceAge(maxNonceAge);
            nonceMgrMap.put(id, nonceMgr);
            return nonceMgr;
        } catch(ClassNotFoundException x){
            throw new RuntimeException(
                    "The NonceManager class: " + className + " specified was not found", x);
        } catch(Exception e){
            throw new RuntimeException(
                    "The NonceManager class: " + className + " could not be instantiated ", e);
        }
    }
    
    /**
     * 
     * @param value takes the delimited property value for id and maxNonceAge. It can be like "id=12345, nonceAge=10000"
     * an example is "id=12345, maxNonceAge=1000000; id = 123, maxNonceAge=10"
     * @return a HshMap
     */
    public static Map getProperties(String value){
        
        HashMap map = new HashMap();
        
        String delim = "(\\s)*;(\\s)*";
        String localDelim = "(\\s)*,(\\s)*";
        String equalDelim = "(\\s)*=(\\s)*";
        
        String id = null;
        String nonceAge = null;
        
        String[] values = value.split(delim);
        for(String s : values){
            String eachNonceProperty[] = s.split(localDelim);
            for(String eachProperty : eachNonceProperty){
                String[] nameValue = eachProperty.split(equalDelim);
                
                if(nameValue[0].equalsIgnoreCase("id")){
                    id = nameValue[1];
                } else if(nameValue[0].equalsIgnoreCase("maxNonceAge")){
                    nonceAge = nameValue[1];
                }
                
            }
            if(id != null && nonceAge != null){
                map.put(id, nonceAge);
            }
        }
        
        return map;
    }

}
