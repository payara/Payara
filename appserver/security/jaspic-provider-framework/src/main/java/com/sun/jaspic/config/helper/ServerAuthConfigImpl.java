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
// Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
package com.sun.jaspic.config.helper;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static jakarta.security.auth.message.AuthStatus.SEND_FAILURE;
import static jakarta.security.auth.message.AuthStatus.SEND_SUCCESS;
import static jakarta.security.auth.message.AuthStatus.SUCCESS;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;
import jakarta.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author Ron Monzillo
 */
public class ServerAuthConfigImpl extends BaseAuthConfigImpl implements ServerAuthConfig {

    private final static AuthStatus[] validateRequestSuccessValues = { SUCCESS, SEND_SUCCESS };
    private final static AuthStatus[] secureResponseSuccessValues = { SEND_SUCCESS };

    private Map<String, Map<Integer, ServerAuthContext>> contextMap;
    private BaseAuthContextImpl authContextHelperHelper;

    protected ServerAuthConfigImpl(String loggerName, EpochCarrier providerEpoch, BaseAuthContextImpl authContextHelper,
            MessagePolicyDelegate policyDelegate, String layer, String appContext, CallbackHandler cbh) throws AuthException {

        super(loggerName, providerEpoch, policyDelegate, layer, appContext, cbh);

        this.authContextHelperHelper = authContextHelper;
        this.policyDelegate = policyDelegate;
    }

    @Override
    protected void initializeContextMap() {
        contextMap = new HashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <M> M createAuthContext(String authContextID, Map<String, Object> properties) throws AuthException {

        if (!authContextHelperHelper.isProtected(new ServerAuthModule[0], authContextID)) {
            return null;
        }

        // Need to coordinate calls to CallerPrincipalCallback; especially optional
        // modules that might reset the result of a required module
        return (M) new ServerAuthContext() {

            ServerAuthModule[] module = init();

            ServerAuthModule[] init() throws AuthException {
                ServerAuthModule[] serverAuthModules;

                try {
                    serverAuthModules = authContextHelperHelper.getModules(new ServerAuthModule[0], authContextID);
                } catch (AuthException ae) {
                    logIfLevel(SEVERE, ae, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                            "unable to load server auth modules");
                    throw ae;
                }

                MessagePolicy requestPolicy = policyDelegate.getRequestPolicy(authContextID, properties);
                MessagePolicy responsePolicy = policyDelegate.getResponsePolicy(authContextID, properties);

                boolean noModules = true;
                for (int i = 0; i < serverAuthModules.length; i++) {
                    if (serverAuthModules[i] != null) {
                        if (isLoggable(FINE)) {
                            logIfLevel(FINE, null, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                    "initializing module");
                        }
                        noModules = false;
                        checkMessageTypes(serverAuthModules[i].getSupportedMessageTypes());

                        serverAuthModules[i].initialize(
                                requestPolicy, responsePolicy,
                                callbackHandler, authContextHelperHelper.getInitProperties(i, properties));
                    }
                }

                if (noModules) {
                    logIfLevel(WARNING, null, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                            "contains no Auth Modules");
                }

                return serverAuthModules;
            }

            @Override
            public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];

                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling vaidateRequest on module");
                    }

                    status[i] = module[i].validateRequest(messageInfo, clientSubject, serviceSubject);

                    if (authContextHelperHelper.exitContext(validateRequestSuccessValues, i, status[i])) {
                        return authContextHelperHelper.getReturnStatus(validateRequestSuccessValues, SEND_FAILURE, status, i);
                    }
                }

                return authContextHelperHelper.getReturnStatus(validateRequestSuccessValues, SEND_FAILURE, status, status.length - 1);
            }

            @Override
            public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];

                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling secureResponse on module");
                    }

                    status[i] = module[i].secureResponse(messageInfo, serviceSubject);

                    if (authContextHelperHelper.exitContext(secureResponseSuccessValues, i, status[i])) {
                        return authContextHelperHelper.getReturnStatus(secureResponseSuccessValues, SEND_FAILURE, status, i);
                    }
                }

                return authContextHelperHelper.getReturnStatus(secureResponseSuccessValues, SEND_FAILURE, status, status.length - 1);
            }

            @Override
            public void cleanSubject(MessageInfo arg0, Subject arg1) throws AuthException {
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(Level.FINE)) {
                        logIfLevel(Level.FINE, null, "ServerAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling cleanSubject on module");
                    }

                    module[i].cleanSubject(arg0, arg1);
                }
            }
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public ServerAuthContext getAuthContext(String authContextID, Subject subject, @SuppressWarnings("rawtypes") Map properties)
            throws AuthException {
        return super.getContext(contextMap, authContextID, subject, properties);
    }

    @Override
    public boolean isProtected() {
        return !authContextHelperHelper.returnsNullContexts() || policyDelegate.isProtected();
    }

}
