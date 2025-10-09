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
package com.sun.enterprise.security.jacc.context;

import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.EJB_ARGUMENTS;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.ENTERPRISE_BEAN;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.REUSE;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.SOAP_MESSAGE;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.SUBJECT;

import jakarta.servlet.http.HttpServletRequest;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.jacc.cache.PermissionCacheFactory;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.internal.api.Globals;
import java.lang.ref.WeakReference;

/**
 * This class implements thread scoped data used for the JACC PolicyContext.
 * 
 * <p>
 * Here the handlers for e.g. HTTP_SERVLET_REQUEST and SUBJECT are essentially implemented.
 * 
 * @author Harry Singh
 * @author Jyri Virkki
 * @author Shing Wai Chan
 *
 */
public class PolicyContextHandlerData {

    private HttpServletRequest httpServletRequest;
    private WeakReference<ComponentInvocation> invocation;
    private PolicyContextDelegate ejbDelegate;

    private PolicyContextHandlerData() {
        ejbDelegate = Globals.getDefaultHabitat().getService(PolicyContextDelegate.class, "EJB");
    }

    public static PolicyContextHandlerData getInstance() {
        return new PolicyContextHandlerData();
    }

    public void setHttpServletRequest(HttpServletRequest httpReq) {
        this.httpServletRequest = httpReq;
    }

    public void setInvocation(ComponentInvocation inv) {
        this.invocation = new WeakReference<>(inv);
    }

    public Object get(String key) {
        if (HTTP_SERVLET_REQUEST.equalsIgnoreCase(key)) {
            return httpServletRequest;
        }

        if (SUBJECT.equalsIgnoreCase(key)) {
            return SecurityContext.getCurrent().getSubject();
        }

        if (REUSE.equalsIgnoreCase(key)) {
            PermissionCacheFactory.resetCaches();
            return Integer.valueOf(0);
        }

        if (invocation == null) {
            return null;
        }

        if (SOAP_MESSAGE.equalsIgnoreCase(key)) {
            return ejbDelegate != null ? ejbDelegate.getSOAPMessage(invocation.get()) : null;
        }

        if (ENTERPRISE_BEAN.equalsIgnoreCase(key)) {
            return ejbDelegate != null ? ejbDelegate.getEnterpriseBean(invocation.get()) : null;
        }

        if (EJB_ARGUMENTS.equalsIgnoreCase(key)) {
            return ejbDelegate != null ? ejbDelegate.getEJbArguments(invocation.get()) : null;
        }

        return null;
    }

    void reset() {
        httpServletRequest = null;
        invocation = null;
        ejbDelegate = null;
    }
}
