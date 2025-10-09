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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static jakarta.security.auth.message.AuthStatus.SEND_FAILURE;
import static jakarta.security.auth.message.AuthStatus.SEND_SUCCESS;
import static jakarta.security.auth.message.AuthStatus.SUCCESS;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ClientAuthContext;
import jakarta.security.auth.message.module.ClientAuthModule;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;

/**
 *
 * @author Ron Monzillo
 */
public class ClientAuthConfigImpl extends BaseAuthConfigImpl implements ClientAuthConfig {

    private final static AuthStatus[] validateResponseSuccessValues = { SUCCESS };
    private final static AuthStatus[] secureResponseSuccessValues = { SEND_SUCCESS };

    private Map<String, Map<Integer, ClientAuthContext>> contextMap;
    private BaseAuthContextImpl authContextHelper;

    protected ClientAuthConfigImpl(String loggerName, EpochCarrier providerEpoch, BaseAuthContextImpl acHelper,
            MessagePolicyDelegate mpDelegate, String layer, String appContext, CallbackHandler cbh) throws AuthException {
        super(loggerName, providerEpoch, mpDelegate, layer, appContext, cbh);

        this.authContextHelper = acHelper;
    }

    @Override
    protected void initializeContextMap() {
        contextMap = new HashMap<>();
    }

    protected void refreshContextHelper() {
        authContextHelper.refresh();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <M> M createAuthContext(String authContextID, Map<String, Object> properties) throws AuthException {

        if (!authContextHelper.isProtected(new ClientAuthModule[0], authContextID)) {
            return null;
        }

        ClientAuthContext context = new ClientAuthContext() {

            ClientAuthModule[] module = init();

            ClientAuthModule[] init() throws AuthException {

                ClientAuthModule[] clientModules;
                try {
                    clientModules = authContextHelper.getModules(new ClientAuthModule[0], authContextID);
                } catch (AuthException ae) {
                    logIfLevel(SEVERE, ae, "ClientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                            "unable to load client auth modules");
                    throw ae;
                }

                MessagePolicy requestPolicy = policyDelegate.getRequestPolicy(authContextID, properties);
                MessagePolicy responsePolicy = policyDelegate.getResponsePolicy(authContextID, properties);

                boolean noModules = true;
                for (int i = 0; i < clientModules.length; i++) {
                    if (clientModules[i] != null) {
                        if (isLoggable(FINE)) {
                            logIfLevel(FINE, null, "ClientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                    "initializing module");
                        }

                        noModules = false;
                        checkMessageTypes(clientModules[i].getSupportedMessageTypes());

                        clientModules[i].initialize(requestPolicy, responsePolicy, callbackHandler,
                                authContextHelper.getInitProperties(i, properties));
                    }
                }

                if (noModules) {
                    logIfLevel(WARNING, null, "CLientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                            "contains no Auth Modules");
                }

                return clientModules;
            }

            @Override
            public AuthStatus validateResponse(MessageInfo arg0, Subject arg1, Subject arg2) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];

                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ClientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling vaidateResponse on module");
                    }

                    status[i] = module[i].validateResponse(arg0, arg1, arg2);

                    if (authContextHelper.exitContext(validateResponseSuccessValues, i, status[i])) {
                        return authContextHelper.getReturnStatus(validateResponseSuccessValues, SEND_FAILURE, status, i);
                    }
                }

                return authContextHelper.getReturnStatus(validateResponseSuccessValues, SEND_FAILURE, status, status.length - 1);
            }

            @Override
            public AuthStatus secureRequest(MessageInfo arg0, Subject arg1) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ClientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling secureResponse on module");
                    }

                    status[i] = module[i].secureRequest(arg0, arg1);

                    if (authContextHelper.exitContext(secureResponseSuccessValues, i, status[i])) {
                        return authContextHelper.getReturnStatus(secureResponseSuccessValues, AuthStatus.SEND_FAILURE, status, i);
                    }
                }
                return authContextHelper.getReturnStatus(secureResponseSuccessValues, AuthStatus.SEND_FAILURE, status, status.length - 1);
            }

            @Override
            public void cleanSubject(MessageInfo arg0, Subject arg1) throws AuthException {
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }

                    if (isLoggable(FINE)) {
                        logIfLevel(FINE, null, "ClientAuthContext: ", authContextID, "of AppContext: ", getAppContext(),
                                "calling cleanSubject on module");
                    }

                    module[i].cleanSubject(arg0, arg1);
                }
            }
        };

        return (M) context;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ClientAuthContext getAuthContext(String authContextID, Subject subject, @SuppressWarnings("rawtypes") Map properties)
            throws AuthException {
        return super.getContext(contextMap, authContextID, subject, properties);
    }

    @Override
    public boolean isProtected() {
        return !authContextHelper.returnsNullContexts() || policyDelegate.isProtected();
    }
}
