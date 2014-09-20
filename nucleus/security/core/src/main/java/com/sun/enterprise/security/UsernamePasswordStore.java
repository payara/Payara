/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security;

import com.sun.enterprise.security.common.ClientSecurityContext;
import java.util.logging.*;
import com.sun.logging.*;
import java.util.Arrays;


/**
 * This class is used to share information between either of the following scenarios
 * 1. Different points of execution of a single thread
 * 2. Different threads that wish to share the username and password information
 * 
 * Which of the above two condition is applicable depends upon the system property key
 *        "com.sun.appserv.iiopclient.perthreadauth";
 * When set to true, scenario #1 above applies and the username/password 
 * information is not shared between threads.
 * When set to false, scenario #2 above applies and the username/password
 * information stored by one thread is global and visible to all threads.
 */
public final class UsernamePasswordStore {
    
    private static final Logger _logger = SecurityLoggerInfo.getLogger();

    private static final boolean isPerThreadAuth = 
            Boolean.getBoolean(ClientSecurityContext.IIOP_CLIENT_PER_THREAD_FLAG);

    private static ThreadLocal localUpc =
        isPerThreadAuth ? new ThreadLocal() : null;
    private static UsernamePasswordStore sharedUpc;

    private final String username;
    private final char[] password;

    /**
     * This creates a new UsernamePasswordStore object.
     * The constructor is marked as private.
     *
     * @param username
     * @param password
     */
    private UsernamePasswordStore(String username, char[] password) {
        //Copy the password to another reference before storing it to the
        //instance field.
        char[] passwordCopy = (password == null) ? null : Arrays.copyOf(password, password.length);
	this.password = passwordCopy;
        this.username = username;
        
    }

    /**
     * This method returns a UsernamePasswordStore, that is
     * either thread-local or global depending on the system property
     * IIOP_PER_THREAD_CLIENT_FLAG. 
     * This method is marked as private.
     *
     * @return The current UsernamePasswordStore
     */
    private static UsernamePasswordStore get() {
        if (isPerThreadAuth) {
            return (UsernamePasswordStore) localUpc.get();
        } else {
            synchronized (UsernamePasswordStore.class) {
                return sharedUpc;
            }
        }
    }

    /**
     * This method sets the username and password as thread-local or global variable
     *
     * @param username 
     * @param password
     */
    public static void set(String username, char[] password) {
        if (isPerThreadAuth) {
            localUpc.set(new UsernamePasswordStore(username, password));
        } else {
            synchronized (UsernamePasswordStore.class) {
                sharedUpc = new UsernamePasswordStore(username, password);
            }
        }
    } 

    /**
     * Clears the username and password, that might have been previously stored,
     * either globally or locally to each thread.
     */
    public static void reset() {
        if (isPerThreadAuth) {
            localUpc.set(null);
        } else {
            synchronized (UsernamePasswordStore.class) {
                sharedUpc = null;
            }
        }
    }
    
    /**
     * Clears the username and password only is they were stored locally
     * to each thread
     */
    public static void resetThreadLocalOnly() {
        if (isPerThreadAuth) {
            localUpc.set(null);
        }
    }

    /**
     * Returns the username, that was previously stored.
     *
     * @return The username set previously or null if not set
     */
    public static String getUsername() {
        UsernamePasswordStore ups = UsernamePasswordStore.get();
        if( ups != null )
            return ups.username;
        else 
            return null;
    }

    
    /**
     * Returns the password, that was previously stored.
     *
     * @return The password set previously or null if not set
     */
    public static char[] getPassword() {
        UsernamePasswordStore ups = UsernamePasswordStore.get();
        if( ups != null ) {
             //Copy the password to another reference before returning it
            char[] passwordCopy = (ups.password == null) ? null : Arrays.copyOf(ups.password, ups.password.length);
            return passwordCopy;
        }
        else 
            return null;
    }

}







