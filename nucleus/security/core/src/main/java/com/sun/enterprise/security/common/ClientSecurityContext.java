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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.common;

import java.security.Principal;

import javax.security.auth.Subject;

import org.glassfish.security.common.PrincipalImpl;

import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.integration.AppServSecurityContext;

/**
 * This class represents the security context on the client side. For usage of the
 * IIOP_CLIENT_PER_THREAD_FLAG flag, see UsernamePasswordStore. When set to false, the volatile
 * field sharedCsc is used to store the context.
 *
 * @see UsernamePasswordStore
 * @author Harpreet Singh
 *
 */
public final class ClientSecurityContext extends AbstractSecurityContext {

    private static final long serialVersionUID = -8079501498521266505L;

    public static final String IIOP_CLIENT_PER_THREAD_FLAG = "com.sun.appserv.iiopclient.perthreadauth";

    // Bug Id: 4787940
    private static final boolean isPerThreadAuth = Boolean.getBoolean(IIOP_CLIENT_PER_THREAD_FLAG);

    // Either the thread local or shared version will be used
    private static ThreadLocal<ClientSecurityContext> localSecurityContext = isPerThreadAuth ? new ThreadLocal<>() : null;
    private static volatile ClientSecurityContext sharedSecurityContext;

    /**
     * This creates a new ClientSecurityContext object.
     *
     * @param username name of the user.
     * @param subject Credentials of the user.
     */
    public ClientSecurityContext(String username, Subject subject) {
        this.callerPrincipal = new PrincipalImpl(username);
        this.subject = subject;
    }

    /**
     * This method gets the SecurityContext stored here. If using a per-thread authentication model, it
     * gets the context from Thread Local Store (TLS) of the current thread. If not using a per-thread
     * authentication model, it gets the singleton context.
     *
     * @return The current Security Context stored here. It returns null if SecurityContext could not be
     * found.
     */
    public static ClientSecurityContext getCurrent() {
        if (isPerThreadAuth) {
            return localSecurityContext.get();
        }

        return sharedSecurityContext;
    }

    /**
     * This method sets the SecurityContext to be stored here.
     *
     * @param clientSecurityContext The Security Context that should be stored.
     */
    public static void setCurrent(ClientSecurityContext clientSecurityContext) {
        if (isPerThreadAuth) {
            localSecurityContext.set(clientSecurityContext);
        } else {
            sharedSecurityContext = clientSecurityContext;
        }
    }

    /**
     * This method returns the caller principal. This information may be redundant since the same
     * information can be inferred by inspecting the Credentials of the caller.
     *
     * @return The caller Principal.
     */
    @Override
    public Principal getCallerPrincipal() {
        return callerPrincipal;
    }

    @Override
    public Subject getSubject() {
        return subject;
    }

    @Override
    public String toString() {
        return "ClientSecurityContext[ " + "Initiator: " + callerPrincipal + "Subject " + subject + " ]";
    }

    // added for CR:6620388
    public static boolean hasEmtpyCredentials(ClientSecurityContext clientSecurityContext) {
        if (clientSecurityContext == null) {
            return true;
        }

        Subject subject = clientSecurityContext.getSubject();
        if (subject == null) {
            return true;
        }

        return subject.getPrincipals().isEmpty();
    }

    @Override
    public AppServSecurityContext newInstance(String userName, Subject subject, String realm) {
        // TODO:V3 ignoring realm in this case
        return new ClientSecurityContext(userName, subject);
    }

    @Override
    public AppServSecurityContext newInstance(String userName, Subject subject) {
        return new ClientSecurityContext(userName, subject);
    }

    @Override
    public void setCurrentSecurityContext(AppServSecurityContext context) {
        if (context instanceof ClientSecurityContext) {
            setCurrent((ClientSecurityContext) context);
            return;
        }

        throw new IllegalArgumentException("Expected ClientSecurityContext, found " + context);
    }

    @Override
    public AppServSecurityContext getCurrentSecurityContext() {
        return getCurrent();
    }

    @Override
    public void setUnauthenticatedSecurityContext() {
        throw new UnsupportedOperationException("Not supported yet in V3.");
    }

    @Override
    public void setSecurityContextWithPrincipal(Principal principal) {
        throw new UnsupportedOperationException("Not supported yet in V3.");
    }

}
