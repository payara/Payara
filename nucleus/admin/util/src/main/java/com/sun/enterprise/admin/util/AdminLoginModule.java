/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util;

import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.logging.LogDomains;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.glassfish.common.util.admin.AuthTokenManager;
import org.glassfish.common.util.admin.AdminAuthCallback;
import org.glassfish.internal.api.LocalPassword;

/**
 * Handles the non-username/password ways an admin user can authenticate.
 * <p>
 * As specified by the LoginModule contract, the login method creates lists 
 * of principals or credentials to be added to the Subject during commit.  Only
 * if commit is invoked does the module actually add them to the Subject.
 * 
 * @author tjquinn
 */
public class AdminLoginModule implements LoginModule {

    private static final Logger logger = LogDomains.getLogger(AdminLoginModule.class,
            LogDomains.ADMIN_LOGGER);
    
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> sharedState;
    private Map<String, ?> options;
    
    private String authRealm = null;
    
    private List<Principal> principalsToAdd = new ArrayList<Principal>();
    private List<Object> privateCredentialsToAdd = new ArrayList<Object>();
    private List<Object> publicCredentialsToAdd = new ArrayList<Object>();
    
    private final NameCallback usernameCallback = new NameCallback("username");
    private final AdminPasswordCallback passwordCallback = new AdminPasswordCallback("password", false);
    private final AdminIndicatorCallback adminIndicatorCallback = new AdminIndicatorCallback();
    private final TokenCallback tokenCallback = new TokenCallback();
    private final RemoteHostCallback remoteHostCallback = new RemoteHostCallback();
    private final PrincipalCallback principalCallback = new PrincipalCallback();
    
    private final Callback[] staticCallbacks = new Callback[] {
            usernameCallback,
            passwordCallback,
            adminIndicatorCallback,
            tokenCallback,
            remoteHostCallback,
            principalCallback
        };
    
    private Callback[] callbacks;
    
    private Callback[] dynamicCallbacks = null;
    
   
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        if (callbackHandler instanceof AdminCallbackHandler) {
            initDynamicCallbacks();
        }
        this.sharedState = sharedState;
        this.options = options;
        authRealm = (String) options.get("auth-realm");
    }

    private void initDynamicCallbacks() {
        dynamicCallbacks = ((AdminCallbackHandler) callbackHandler).dynamicCallbacks();
        callbacks = Arrays.copyOf(staticCallbacks, staticCallbacks.length + dynamicCallbacks.length);
        System.arraycopy(dynamicCallbacks, 0, callbacks, staticCallbacks.length, dynamicCallbacks.length);
    }
    
    @Override
    public boolean login() throws LoginException {
        /*
         * Without a callback handler we cannot find out what we need about
         * the incoming request.
         */
        if (callbackHandler == null) {
            throw new LoginException(Strings.get("secure.admin.noCallbackHandler"));
        }
        try {
            callbackHandler.handle(callbacks);
        } catch (Exception ex) {
            final LoginException lex = new LoginException();
            lex.initCause(ex);
            throw lex;
        }
        
        /*
         * Verifying the admin indicator will throw an exception if the
         * admin indicator was provided but did not match what we expect.
         */
        verifyAdminIndicator();
        
        /*
         * Make sure this login module has some way of authenticating this user.  
         * Otherwise we don't need it to be invoked during commit or logout.
         */
        final boolean result = localPassword() | token() | clientCert() | anyDynamicCallback() ;
        return result;
    }

    private boolean anyDynamicCallback() {
        boolean result = false;
        for (Callback cb : dynamicCallbacks) {
            if (cb instanceof AdminAuthCallback.RequestBasedCallback) {
                final AdminAuthCallback.RequestBasedCallback tbcb = (AdminAuthCallback.RequestBasedCallback) cb;
                final Subject s = tbcb.getSubject();
                if (s != null) {
                    result = true;
                    updateFromSubject(s);
                }
            }
        }
        return result;
    }
    
    private boolean localPassword() {
        final boolean result = passwordCallback.isLocalPassword();
        if (result) {
            /*
             * Create a new private credential if it's the local password.
             */
            final PasswordCredential pwCred = new PasswordCredential(
                    usernameCallback.getName(), 
                    passwordCallback.getPassword(), 
                    authRealm);
            privateCredentialsToAdd.add(pwCred);
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "AdminLoginModule detected local password {0}", new String(passwordCallback.getPassword()));
            }
        }
        return result;
    }
    
    private boolean token() {
        final Subject s = tokenCallback.consumeToken();
        if (s == null) {
            return false;
        }
        /*
         * The token manager knows which Subject was effective when the token
         * was created.  We add those to the lists we'll add if this module's
         * commit is invoked.
         */
        updateFromSubject(s);
        return true;
    }
    
    private void updateFromSubject(final Subject s) {
        principalsToAdd.addAll(s.getPrincipals());
        privateCredentialsToAdd.addAll(s.getPrivateCredentials());
        publicCredentialsToAdd.addAll(s.getPublicCredentials());
    }
    
    private boolean clientCert() {
        final Principal p = principalCallback.getPrincipal();
        if (p == null) {
            return false;
        }
        principalsToAdd.add(p);
        return true;
    }
    
    private void verifyAdminIndicator() throws LoginException {
        adminIndicatorCallback.verify();
    }
    
    @Override
    public boolean commit() throws LoginException {
        subject.getPrincipals().addAll(principalsToAdd);
        subject.getPrivateCredentials().addAll(privateCredentialsToAdd);
        subject.getPublicCredentials().addAll(publicCredentialsToAdd);
        
        return true;
    }
    
//    private void processRemoteHost() {
//        if (remoteHostCallback.get() != null) {
//            subject.getPublicCredentials().add(new ClientHostCredential(remoteHostCallback.get()));
//        }
//    }
    
    @Override
    public boolean abort() throws LoginException {
        removeAddedInfo();
        return true;
    }

    @Override
    public boolean logout() throws LoginException {
        removeAddedInfo();
        return true;
    }
    
    private void removeAddedInfo() {
        subject.getPrincipals().removeAll(principalsToAdd);
        subject.getPrivateCredentials().removeAll(privateCredentialsToAdd);
    }
    
    static class PrincipalCallback implements Callback {
        private Principal p;
        
        public void setPrincipal(final Principal p) {
            this.p = p;
        }
        
        public Principal getPrincipal() {
            return p;
        }
    }
    
    static abstract class StringCallback implements Callback {
        private String value;
        
        public void set(final String value) {
            this.value = value;
        }
        
        public String get() {
            return value;
        }
    }
    
    static class TokenCallback implements Callback {
        private AuthTokenManager authTokenManager = null;
        private String token;
        
        public void set(final String token, final AuthTokenManager atm) {
            this.token = token;
            this.authTokenManager = atm;
        }
        
        Subject consumeToken() {
            return (token == null ? null : authTokenManager.findToken(token));
        }
    }
    
    static class AdminIndicatorCallback implements Callback {
        private String expectedAdminIndicator;
        private String providedAdminIndicator;
        private String remoteHost;
        
        public void set(final String actual, final String expected,
                final String remoteHost) {
            this.providedAdminIndicator = actual;
            this.expectedAdminIndicator = expected;
            this.remoteHost = remoteHost;
        }
        
        public void verify() throws LoginException {
            final SpecialAdminIndicatorChecker checker = 
                    new SpecialAdminIndicatorChecker(providedAdminIndicator,
                            expectedAdminIndicator,
                            remoteHost);
            if (checker.result() == SpecialAdminIndicatorChecker.Result.MISMATCHED) {
                throw new LoginException();
            }
        }
    }
    
    static class RemoteHostCallback extends StringCallback{}
    
    static class AdminPasswordCallback extends PasswordCallback {
        
        private LocalPassword localPassword;
        
        AdminPasswordCallback(String prompt, boolean echoOn) {
            super(prompt, echoOn);
        }
        
        void setLocalPassword(final LocalPassword lp) {
            this.localPassword = lp;
        }
        
        private boolean isLocalPassword() {
            return (localPassword == null) ? false : localPassword.isLocalPassword(new String(getPassword()));
        }
    }
    
    /*
     * If the admin client sent the unique domain identifier in a header then
     * that should mean the request came from another GlassFish server in this
     * domain. Make sure that the value, if present, matches the one in 
     * this server's domain config.  If they do not match then reject the 
     * message - it came from a domain other than this server's.
     * 
     * Note that we don't insist that every request have the domain identifier.  For 
     * example, requests from asadmin will not include the domain ID.  But if
     * the domain ID is present in the request it needs to match the 
     * configured ID.
     */
    private static class SpecialAdminIndicatorChecker {
        
        private static enum Result {
            NOT_IN_REQUEST,
            MATCHED,
            MISMATCHED
        }
        
        private final SpecialAdminIndicatorChecker.Result _result;
        
        private SpecialAdminIndicatorChecker(
                final String actualIndicator,
                final String expectedIndicator,
                final String originHost) {
            if (actualIndicator != null) {
                if (actualIndicator.equals(expectedIndicator)) {
                    _result = SpecialAdminIndicatorChecker.Result.MATCHED;
                    logger.fine("Admin request contains expected domain ID");
                } else {
                    final String msg = Strings.get("foreign.domain.ID", 
                            originHost, actualIndicator, expectedIndicator);
                    logger.log(Level.WARNING, msg);
                    _result = SpecialAdminIndicatorChecker.Result.MISMATCHED;
                }
            } else {
                logger.fine("Admin request contains no domain ID; this is OK - continuing");
                _result = SpecialAdminIndicatorChecker.Result.NOT_IN_REQUEST;
            }
        }
        
        private SpecialAdminIndicatorChecker.Result result() {
            return _result;
        }
    }
}
