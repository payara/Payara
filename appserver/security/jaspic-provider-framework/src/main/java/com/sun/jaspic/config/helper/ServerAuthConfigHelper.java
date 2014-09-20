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

package com.sun.jaspic.config.helper;

import com.sun.jaspic.config.delegate.MessagePolicyDelegate;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.config.ServerAuthConfig;
import javax.security.auth.message.config.ServerAuthContext;
import javax.security.auth.message.module.ServerAuthModule;

/**
 *
 * @author Ron Monzillo
 */
public class ServerAuthConfigHelper extends AuthConfigHelper implements ServerAuthConfig {

    final static AuthStatus[] vR_SuccessValue = {AuthStatus.SUCCESS, AuthStatus.SEND_SUCCESS};
    final static AuthStatus[] sR_SuccessValue = {AuthStatus.SEND_SUCCESS};
    HashMap<String, HashMap<Integer, ServerAuthContext>> contextMap;
    AuthContextHelper acHelper;

    protected ServerAuthConfigHelper(String loggerName, EpochCarrier providerEpoch,
            AuthContextHelper acHelper, MessagePolicyDelegate mpDelegate,
            String layer, String appContext, CallbackHandler cbh)
            throws AuthException {
        super(loggerName, providerEpoch, mpDelegate, layer, appContext, cbh);
        this.acHelper = acHelper;
        this.mpDelegate = mpDelegate;
    }

    protected void initializeContextMap() {
        contextMap = new HashMap<String, HashMap<Integer, ServerAuthContext>>();
    }

    protected void refreshContextHelper() {
        acHelper.refresh();
    }

    protected ServerAuthContext createAuthContext(final String authContextID,
            final Map properties) throws AuthException {

        if (!acHelper.isProtected(new ServerAuthModule[0], authContextID)) {
            return null;
        }

        // need to coordinate calls to CallerPrincipalCallback; expecially optional
        // modules that might reset the result of a required module
        return new ServerAuthContext() {

            ServerAuthModule[] module = init();

            ServerAuthModule[] init() throws AuthException {
                ServerAuthModule[] m;
                try {
                    m = acHelper.getModules(new ServerAuthModule[0], authContextID);
                } catch (AuthException ae) {
                    logIfLevel(Level.SEVERE, ae,
                            "ServerAuthContext: ", authContextID,
                            "of AppContext: ", getAppContext(),
                            "unable to load server auth modules");
                    throw ae;
                }

                MessagePolicy requestPolicy =
                        mpDelegate.getRequestPolicy(authContextID, properties);
                MessagePolicy responsePolicy =
                        mpDelegate.getResponsePolicy(authContextID, properties);

                boolean noModules = true;
                for (int i = 0; i < m.length; i++) {
                    if (m[i] != null) {
                        if (isLoggable(Level.FINE)) {
                            logIfLevel(Level.FINE, null,
                                    "ServerAuthContext: ", authContextID,
                                    "of AppContext: ", getAppContext(),
                                    "initializing module");
                        }
                        noModules = false;
                        checkMessageTypes(m[i].getSupportedMessageTypes());
                        m[i].initialize(requestPolicy, responsePolicy,
                                cbh, acHelper.getInitProperties(i, properties));
                    }
                }
                if (noModules) {
                    logIfLevel(Level.WARNING, null,
                            "ServerAuthContext: ", authContextID,
                            "of AppContext: ", getAppContext(),
                            "contains no Auth Modules");
                }
                return m;
            }

            public AuthStatus validateRequest(MessageInfo arg0, Subject arg1, Subject arg2) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }
                    if (isLoggable(Level.FINE)) {
                        logIfLevel(Level.FINE, null,
                                "ServerAuthContext: ", authContextID,
                                "of AppContext: ", getAppContext(),
                                "calling vaidateRequest on module");
                    }
                    status[i] = module[i].validateRequest(arg0, arg1, arg2);
                    if (acHelper.exitContext(vR_SuccessValue, i, status[i])) {
                        return acHelper.getReturnStatus(vR_SuccessValue,
                                AuthStatus.SEND_FAILURE, status, i);
                    }
                }
                return acHelper.getReturnStatus(vR_SuccessValue,
                        AuthStatus.SEND_FAILURE, status, status.length - 1);
            }

            public AuthStatus secureResponse(MessageInfo arg0, Subject arg1) throws AuthException {
                AuthStatus[] status = new AuthStatus[module.length];
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }
                    if (isLoggable(Level.FINE)) {
                        logIfLevel(Level.FINE, null,
                                "ServerAuthContext: ", authContextID,
                                "of AppContext: ", getAppContext(),
                                "calling secureResponse on module");
                    }
                    status[i] = module[i].secureResponse(arg0, arg1);
                    if (acHelper.exitContext(sR_SuccessValue, i, status[i])) {
                        return acHelper.getReturnStatus(sR_SuccessValue,
                                AuthStatus.SEND_FAILURE, status, i);
                    }
                }
                return acHelper.getReturnStatus(sR_SuccessValue,
                        AuthStatus.SEND_FAILURE, status, status.length - 1);
            }

            public void cleanSubject(MessageInfo arg0, Subject arg1) throws AuthException {
                for (int i = 0; i < module.length; i++) {
                    if (module[i] == null) {
                        continue;
                    }
                    if (isLoggable(Level.FINE)) {
                        logIfLevel(Level.FINE, null,
                                "ServerAuthContext: ", authContextID,
                                "of AppContext: ", getAppContext(),
                                "calling cleanSubject on module");
                    }
                    module[i].cleanSubject(arg0, arg1);
                }
            }
        };
    }

    public ServerAuthContext getAuthContext(String authContextID,
            Subject subject, final Map properties) throws AuthException {
        return super.getContext(contextMap, authContextID, subject, properties);
    }

    public boolean isProtected() {
        return (!acHelper.returnsNullContexts() || mpDelegate.isProtected());
    }
}
