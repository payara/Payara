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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.servlet;

import static jakarta.security.auth.message.MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;

import java.util.Map;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.MessagePolicy.TargetPolicy;
import jakarta.security.auth.message.config.AuthConfigFactory;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;
import com.sun.jaspic.config.helper.BaseAuthContextImpl;
import com.sun.jaspic.config.jaas.JAASAuthConfigProvider;

/**
 *
 * @author Ron Monzillo
 */
public class JAASServletAuthConfigProvider extends JAASAuthConfigProvider {

    private static final String HTTP_SERVLET_LAYER = "HttpServlet";
    private static final String MANDATORY_KEY = "jakarta.security.auth.message.MessagePolicy.isMandatory";
    private static final String MANDATORY_AUTH_CONTEXT_ID = "mandatory";
    private static final String OPTIONAL_AUTH_CONTEXT_ID = "optional";

    private static final Class<?>[] moduleTypes = new Class[] { ServerAuthModule.class };
    private static final Class<?>[] messageTypes = new Class[] { HttpServletRequest.class, HttpServletResponse.class };

    private static final MessagePolicy mandatoryPolicy = new MessagePolicy(
            new TargetPolicy[] {
                    new TargetPolicy(
                            null,
                            () -> AUTHENTICATE_SENDER)
            },
            true);

    private static final MessagePolicy optionalPolicy = new MessagePolicy(
            new TargetPolicy[] {
                    new TargetPolicy(
                            null,
                            () -> AUTHENTICATE_SENDER)
            },
            false);

    public JAASServletAuthConfigProvider(Map<String, String> properties, AuthConfigFactory factory) {
        super(properties, factory);
    }

    @Override
    public MessagePolicyDelegate getMessagePolicyDelegate(String appContext) throws AuthException {

        return new MessagePolicyDelegate() {

            @Override
            public MessagePolicy getRequestPolicy(String authContextID, Map properties) {
                return MANDATORY_AUTH_CONTEXT_ID.equals(authContextID) ? mandatoryPolicy : optionalPolicy;
            }

            @Override
            public MessagePolicy getResponsePolicy(String authContextID, Map properties) {
                return null;
            }

            @Override
            public Class<?>[] getMessageTypes() {
                return messageTypes;
            }

            @Override
            public String getAuthContextID(MessageInfo messageInfo) {
                return messageInfo.getMap().containsKey(MANDATORY_KEY) ? MANDATORY_AUTH_CONTEXT_ID : OPTIONAL_AUTH_CONTEXT_ID;
            }

            @Override
            public boolean isProtected() {
                return true;
            }

        };
    }

    @Override
    protected Class<?>[] getModuleTypes() {
        return moduleTypes;
    }

    @Override
    protected String getLayer() {
        return HTTP_SERVLET_LAYER;
    }

    @Override
    public BaseAuthContextImpl getAuthContextHelper(String appContext, boolean returnNullContexts) throws AuthException {
        // overrides returnNullContexts to false (as required by Servlet Profile)
        return super.getAuthContextHelper(appContext, false);
    }
}
