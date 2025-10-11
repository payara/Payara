/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.security.jacc.context;

import java.util.logging.Logger;

import jakarta.security.jacc.PolicyContextException;
import jakarta.security.jacc.PolicyContextHandler;

import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.EJB_ARGUMENTS;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.ENTERPRISE_BEAN;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.REUSE;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.SOAP_MESSAGE;
import static com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl.SUBJECT;
import static java.util.logging.Level.SEVERE;
import static jakarta.security.jacc.PolicyContext.registerHandler;

public class PolicyContextRegistration {
    private static final Logger LOG = Logger.getLogger(PolicyContextRegistration.class.getName());

    /**
     * This method registers the policy handlers, which provide objects JACC Providers
     * and other code can use.
     *
     * <p>
     * Note, in a full EE environment with CDI, only the JACC unique SUBJECT is typically
     * really useful.
     */
    public static void registerPolicyHandlers() {
        LOG.finest("registerPolicyHandlers()");
        try {
            PolicyContextHandler policyContextHandler = PolicyContextHandlerImpl.getInstance();

            registerHandler(ENTERPRISE_BEAN, policyContextHandler, true);
            registerHandler(SUBJECT, policyContextHandler, true);
            registerHandler(EJB_ARGUMENTS, policyContextHandler, true);
            registerHandler(SOAP_MESSAGE, policyContextHandler, true);
            registerHandler(HTTP_SERVLET_REQUEST, policyContextHandler, true);
            registerHandler(REUSE, policyContextHandler, true);
            LOG.config("Policy handlers successfully registered.");
        } catch (PolicyContextException ex) {
            LOG.log(SEVERE, "Failed to register policy handlers!", ex);
        }
    }

}
