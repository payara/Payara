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

package com.sun.jaspic.config.servlet;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;
import com.sun.jaspic.config.helper.AuthContextHelper;
import com.sun.jaspic.config.jaas.JAASAuthConfigProvider;
import java.util.Map;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.AuthConfigFactory;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Ron Monzillo
 */
public class JAASServletAuthConfigProvider extends JAASAuthConfigProvider {

    private static final String HTTP_SERVLET_LAYER = "HttpServlet";
    private static final String MANDATORY_KEY = "javax.security.auth.message.MessagePolicy.isMandatory";
    private static final String MANDATORY_AUTH_CONTEXT_ID = "mandatory";
    private static final String OPTIONAL_AUTH_CONTEXT_ID = "optional";
    private static final Class[] moduleTypes = new Class[] {ServerAuthModule.class};
    private static final Class[] messageTypes = new Class[] {HttpServletRequest.class, HttpServletResponse.class};
    final static MessagePolicy mandatoryPolicy =
            new MessagePolicy(new MessagePolicy.TargetPolicy[]{
                new MessagePolicy.TargetPolicy((MessagePolicy.Target[]) null,
                new MessagePolicy.ProtectionPolicy() {

                    public String getID() {
                        return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
                    }
                })}, true);
    final static MessagePolicy optionalPolicy =
            new MessagePolicy(new MessagePolicy.TargetPolicy[]{
                new MessagePolicy.TargetPolicy((MessagePolicy.Target[]) null,
                new MessagePolicy.ProtectionPolicy() {

                    public String getID() {
                        return MessagePolicy.ProtectionPolicy.AUTHENTICATE_SENDER;
                    }
                })}, false);

    public JAASServletAuthConfigProvider(Map properties, AuthConfigFactory factory) {
        super(properties, factory);
    }

    public MessagePolicyDelegate getMessagePolicyDelegate(String appContext) throws AuthException {

        return new MessagePolicyDelegate() {

            public MessagePolicy getRequestPolicy(String authContextID, Map properties) {
                MessagePolicy rvalue;
                if (MANDATORY_AUTH_CONTEXT_ID.equals(authContextID)) {
                    rvalue = mandatoryPolicy;
                } else {
                    rvalue = optionalPolicy;
                }
                return rvalue;
            }

            public MessagePolicy getResponsePolicy(String authContextID, Map properties) {
                return null;
            }

            public Class[] getMessageTypes() {
                return messageTypes;
            }

            public String getAuthContextID(MessageInfo messageInfo) {
                String rvalue;
                if (messageInfo.getMap().containsKey(MANDATORY_KEY)) {
                    rvalue = MANDATORY_AUTH_CONTEXT_ID;
                } else {
                    rvalue = OPTIONAL_AUTH_CONTEXT_ID;
                }
                return rvalue;
            }

            public boolean isProtected() {
                return true;
            }

        };
    }

    @Override
    protected Class[] getModuleTypes() {
        return moduleTypes;
    }

    @Override
    protected String getLayer() {
        return HTTP_SERVLET_LAYER;
    }

    @Override
    public AuthContextHelper getAuthContextHelper(String appContext, boolean returnNullContexts)
            throws AuthException {
        // overrides returnNullContexts to false (as required by Servlet Profile)
        return super.getAuthContextHelper(appContext,false);
    }
}
