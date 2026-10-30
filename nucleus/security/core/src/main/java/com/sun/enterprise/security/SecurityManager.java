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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import java.lang.reflect.Method;
import java.security.Principal;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.glassfish.api.invocation.ComponentInvocation;

/**
 * This interface is used by the Container to manage access to EJBs. The container has a reference to an implementation
 * of this interface.
 * 
 * @author Harish Prabandham
 */
public interface SecurityManager {

    /**
     * @param The Invocation object containing the details of the invocation.
     * @return true if the client is allowed to invoke the EJB, false otherwise.
     */
    boolean authorize(ComponentInvocation inv);

    /**
     * @return The Principal of the client who made the current invocation.
     */
    Principal getCallerPrincipal();

    /**
     * @return A boolean true/false depending on whether or not the caller has the specified role.
     * @param The EJB developer specified "logical role".
     */
    boolean isCallerInRole(String role);

    /**
     * This sets up the security context - if not set and does run-as related login if required
     * 
     * @param ComponentInvocation
     */
    void preInvoke(ComponentInvocation inv);

    Object invoke(Object bean, Method beanClassMethod, Object[] methodParameters) throws Throwable;

    /**
     * This method is used by the Invocation Manager to remove the run-as identity information that was set up using the
     * preInvoke
     * 
     * @param ComponentInvocation
     */
    void postInvoke(ComponentInvocation inv);

    /**
     * Call this method to clean up all the bookeeping data-structures in the SM.
     */
    void destroy();

    /**
     * This will return the subject associated with the current call. If the run as subject is in effect. It will return
     * that subject. This is done to support the JACC specification which says if the runas principal is in effect, that
     * principal should be used for making a component call.
     * 
     * @return Subject the current subject. Null if this is not the runas case
     */
    Subject getCurrentSubject();

    /**
     * Purge ThreadLocals held by jakarta.security.jacc.PolicyContext
     */
    void resetPolicyContext();

}
