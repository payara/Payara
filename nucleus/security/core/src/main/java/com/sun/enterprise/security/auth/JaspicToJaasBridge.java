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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth;

import static com.sun.enterprise.security.SecurityLoggerInfo.auditAtnRefusedError;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.auditAuthenticate;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.dummyCallback;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getJaasContext;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getRealmInstance;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getValidRealm;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getValidSubject;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.validateJaasLogin;
import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static java.util.Collections.list;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.INFO;

import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.x500.X500Principal;

import org.glassfish.security.common.Group;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;

import sun.security.x509.X500Name;

/**
 * This class contains a collection of methods used by the JASPIC implementation to interact
 * with the Payara JAAS/Realm system.
 * 
 * <p>
 * For the most part JASPIC does the authentication itself, and the JASPIC runtime code sets the 
 * security context based on that, but in a few cases bridging to JAAS is supported. This is especially
 * the case for JASPIC's PasswordValidationCallback, which is specified to delegate credential validation
 * from JASPIC to the contain/application server's native "identity stores" (realms, login modules, etc).
 * 
 * @author Harpreet Singh (hsingh@eng.sun.com)
 * @author Jyri Virkki
 * @author Arjan Tijms (refactoring)
 *
 */
public class JaspicToJaasBridge {
    
    private static final Logger _logger = SecurityLoggerInfo.getLogger();
    
    /**
     * Performs username/password login validation against a configured JAAS context and realm for JASPIC security.
     *
     * <p>
     * This is used by SAMs that wish to delegate the validation of username/password credentials to a realm installed
     * on the application server (e.g. the LdapRealm). Note that such delegation in pure JASPIC is only defined for
     * the username/password credential.
     *
     * <p>
     * The difference between this method and the ones in {@link WebAndEjbToJaasBridge} is that it just
     * verifies whether the login will succeed in the given realm. It does not set the result of the
     * authentication in the appserver runtime environment A silent return from this method means that
     * the given user succeeding in authenticating with the given password in the given realm
     *
     * @param subject
     * @param username
     * @param password
     * @param realm the realm to authenticate under
     * @returns Subject on successful authentication
     * @throws LoginException
     */
    public static Subject validateUsernamePasswordByJaas(Subject subject, String username, char[] password, String realm) throws LoginException {
        String validRealm = getValidRealm(realm);
        Subject validSubject = getValidSubject(subject);
        PasswordCredential passwordCredential = new PasswordCredential(username, password, validRealm);

        privileged(() -> validSubject.getPrivateCredentials().add(passwordCredential));

        String jaasCtx = getJaasContext(validRealm);

        if (_logger.isLoggable(FINE)) {
            _logger.fine("JASPIC login user [" + username + "] into realm: " + validRealm + " using JAAS module: " + jaasCtx);
        }

        validateJaasLogin(username, jaasCtx, validRealm, validSubject);

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("JASPIC Password login succeeded for : " + username);
        }

        return subject;
    }

    public static Subject jaasX500Login(Subject subject, X500Principal x500Principal) throws LoginException {
        Subject validSubject = getValidSubject(subject);

        String callerPrincipalName = "";
        try {
            callerPrincipalName = x500Principal.getName(X500Principal.RFC1779);

            privileged(() -> validSubject.getPublicCredentials().add(x500Principal));

            CertificateRealm certRealm = (CertificateRealm) Realm.getInstance(CertificateRealm.AUTH_TYPE);
            String jaasCtx = certRealm.getJAASContext();

            if (jaasCtx != null) {
                // The subject has the certificate Credential.
                LoginContext loginContext = new LoginContext(jaasCtx, validSubject, dummyCallback);
                loginContext.login();
            }

            // Sets security context
            certRealm.authenticate(validSubject, x500Principal);
        } catch (Exception ex) {
            _logger.log(INFO, auditAtnRefusedError, callerPrincipalName);

            auditAuthenticate(callerPrincipalName, CertificateRealm.AUTH_TYPE, false);

            if (ex instanceof LoginException) {
                throw (LoginException) ex;
            }

            throw (LoginException) new LoginException(ex.toString()).initCause(ex);
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("jmac cert login succeeded for: " + callerPrincipalName);
        }

        auditAuthenticate(callerPrincipalName, CertificateRealm.AUTH_TYPE, true);

        // do not set the security Context

        return subject;
    }

    public static Subject addRealmGroupsToSubject(Subject subject, String callerPrincipalName, String realmName) throws LoginException {

        Subject validSubject = getValidSubject(subject);

        try {
            Enumeration<String> groupsEnumeration = getRealmInstance(realmName).getGroupNames(callerPrincipalName);

            if (groupsEnumeration != null) {
                privileged(() -> list(groupsEnumeration)
                                    .stream()
                                    .forEach(groupName -> validSubject.getPrincipals().add(new Group(groupName))));
            }
        } catch (Exception ex) {
            if (_logger.isLoggable(FINE)) {
                _logger.log(FINE, "Exception when trying to populate groups for CallerPrincipal " + callerPrincipalName, ex);
            }
        }

        return subject;
    }


}
