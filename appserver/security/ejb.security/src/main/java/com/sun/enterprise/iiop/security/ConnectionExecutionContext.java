/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.iiop.security;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

/**
 * This class that implements ConnectionExecutionContext that gets 
 * stored in Thread Local Storage. If the current thread creates
 * child threads, the context info that is  stored in the current 
 * thread is automatically propogated to the child threads.
 * 
 * Two class methods serve as a convinient way to set/get the 
 * Context information within the current thread.   
 *
 * Thread Local Storage is a concept introduced in JDK1.2. So, it
 * will not work on earlier releases of JDK.
 *
 * @see java.lang.ThreadLocal
 * @see java.lang.InheritableThreadLocal
 * 
 */
public class ConnectionExecutionContext {
    
    public static final String IIOP_CLIENT_PER_THREAD_FLAG =
        "com.sun.appserv.iiopclient.perthreadauth";
    private static final boolean isPerThreadAuth;

    static {
       Boolean b  = (Boolean) AccessController.doPrivileged( new PrivilegedAction(){
            @Override
            public Object run() {
                 return Boolean.valueOf(Boolean.getBoolean(IIOP_CLIENT_PER_THREAD_FLAG));
            }

       });
       isPerThreadAuth = b.booleanValue();
    }

     //private static final InheritableThreadLocal connCurrent= new InheritableThreadLocal();
    private static final ThreadLocal connCurrent= (isPerThreadAuth) ? new ThreadLocal() : new InheritableThreadLocal();

    // XXX: Workaround for non-null connection object ri for local invocation.
    private static final ThreadLocal<Long> ClientThreadID = new ThreadLocal<Long>();

    public static Long readClientThreadID() {
        Long ID = ClientThreadID.get();
        //ClientThreadID.remove();
        return ID;
    }

    public static void setClientThreadID(Long ClientThreadID) {
        ConnectionExecutionContext.ClientThreadID.set(ClientThreadID);
    }

    public static void removeClientThreadID() {
        ClientThreadID.remove();
    }

    /** 
     * This method can be used to add a new hashtable for storing the 
     * Thread specific context information. This method is useful to add a 
     * deserialized Context information that arrived over the wire.
     * @param A hashtable that stores the current thread's context
     * information.
     */
    public static void setContext(Hashtable ctxTable) {
        if (ctxTable != null) {
            connCurrent.set(ctxTable);
        } else {
            connCurrent.set(new Hashtable());
        }
    }

    /**
     * This method returns the hashtable that stores the thread specific
     * Context information.
     * @return The Context object stored in the current TLS. It always 
     * returns a non null value;
     */
    public static Hashtable getContext() {
         if (connCurrent.get() == null) {
             setContext(null); // Create a new one...
         } 
         return (Hashtable) connCurrent.get();
    }
}
