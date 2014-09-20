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

/*
 * Listener.java
 * $Id: Listener.java,v 1.5 2006/11/08 20:55:16 harpreet Exp $
 * $Date: 2006/11/08 20:55:16 $
 * $Revision: 1.5 $
 */

package com.sun.enterprise.admin.monitor.callflow;

/**
 * This interface exposes the call flow Listener API.
 * 
 * This interface is implemented by listeners that are registered with the
 * call flow agent, in order to receive the call flow trap point notifications.
 *
 * Note 1: There are no ordering guaratees for the various notifications.
 * 
 * Note 2: A listener implementation must be stateless. This is allow the
 * listener to be accessed concurrently by multiple threads, and yet avoid
 * synchronization overhead associated with protected access to shared state,
 * in a multi-threaded environment.
 *
 * Note 3: It is also imperative that the listener implementation is
 * light-weight, and avoids time consuming operations such as disk access,
 * logging, synchronization locks, et cetera. This will ensure that the
 * listener does not negatively impact the performance of the
 * application thread.
 *
 * @author Ram Jeyaraman
 * @date October 11, 2005
 */
public interface Listener {

    /**
     * This notification indicates that a request is being started.
     *
     * Allowed request types are:
     * 
     * 1. Remote HTTP Web request.
     * 2. Remote EJB request.
     * 3. MDB request.
     * 4. Timer EJB.
     *
     * @param requestType Type of the request.
     *
     * @param callerIPAddress Client host IP address of caller.
     */      
    public void requestStart(
            final String requestId,  final RequestType requestType,
            final String callerIPAddress, final String remoteUser);
    
    /**
     * This notification indicates that a request is about to complete.
     */
    public void requestEnd(final String requestId);

    /**
     * This notification indicates that an EJB method is about to be invoked.
     * 
     * This parameters provide information such as method name, component
     * name, component type, application name, module name, caller principal.
     */
    public void ejbMethodStart(
            final String requestId, final String methodName,
            final String applicationName, final String moduleName,
            final String componentName, final ComponentType componentType,
            final String callerPrincipal, final String transactionId);
    
    /**
     * This notification indicates that an EJB method has completed. The
     * parameters provide information on the outcome of the invocation
     * such as exception, if any.
     */    
    public void ejbMethodEnd(final String requestId, final Throwable exception);
    
    /**
     * This notification indicates that a web method is about to be invoked.
     * 
     * This parameters provide information such as method name, component
     * name, component type, application name, module name, caller principal,
     * and caller IP address.
     */
    public void webMethodStart(
            final String requestId, final String methodName,
            final String applicationName, final String moduleName,
            final String componentName, final ComponentType componentType,
            final String callerPrincipal);
    
    /**
     * This notification indicates that a web method has completed. The
     * parameters provide information on the outcome of the invocation
     * such as exception, if any.
     */    
    public void webMethodEnd(final String requestId, final Throwable exception);
    
    /**
     * This notification indicates that an EntityManager method is about to be invoked.
     * 
     * This parameters provide information such as method name, component
     * name, component type, application name, module name, caller principal.
     */    
    public void entityManagerMethodStart(
            final String requestId, final EntityManagerMethod entityManagerMethod, 
            final String applicationName, final String moduleName, 
            final String componentName, final ComponentType componentType, 
            final String callerPrincipal);

    /**
     * This notification indicates that an EntityManager method has completed. 
     */
    public void entityManagerMethodEnd(String requestId);

    /**
     * This notification indicates that an EntityManager Query method is about to be invoked.
     * 
     * This parameters provide information such as method name, component
     * name, component type, application name, module name, caller principal.
     */
    public void entityManagerQueryStart(
            final String requestId, final EntityManagerQueryMethod queryMethod, 
            final String applicationName, final String moduleName, 
            final String componentName, final ComponentType componentType, 
            final String callerPrincipal);

    /**
     * This notification indicates that an EntityManager Query method has completed. 
     */  
    public void entityManagerQueryEnd(final String requestId);

}
