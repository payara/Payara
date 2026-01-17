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
// Portions Copyright [2018-2024] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.security.auth.login;

import static com.sun.enterprise.security.SecurityLoggerInfo.auditAtnRefusedError;
import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static com.sun.enterprise.security.common.SecurityConstants.ALL;
import static com.sun.enterprise.security.common.SecurityConstants.CERTIFICATE;
import static com.sun.enterprise.security.common.SecurityConstants.CLIENT_JAAS_CERTIFICATE;
import static com.sun.enterprise.security.common.SecurityConstants.CLIENT_JAAS_PASSWORD;
import static com.sun.enterprise.security.common.SecurityConstants.USERNAME_PASSWORD;
import static com.sun.enterprise.util.Utility.isEmpty;
import static java.util.logging.Level.*;

import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;

import javax.security.auth.x500.X500Principal;
import org.glassfish.internal.api.Globals;

import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.audit.AuditManager;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.login.common.ServerLoginCallbackHandler;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.common.ClientSecurityContext;
import org.glassfish.security.common.Group;

/**
 *
 * This class is invoked implicitly by the server to log in the user information that was sent on
 * the wire by the client. Clients will use the <i>doClientLogin</i> method to simulate
 * authentication to the server.
 *
 * @author Harpreet Singh (hsingh@eng.sun.com)
 * @author Jyri Virkki
 *
 */
public class LoginContextDriver {

    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();

    public static final ServerLoginCallbackHandler dummyCallback = new ServerLoginCallbackHandler();
    public static final String CERT_REALMNAME = "certificate";

    private static volatile AuditManager AUDIT_MANAGER;

    /**
     * This class cannot be instantiated
     *
     */
    private LoginContextDriver() {
    }


    // ############################   CLIENT   ######################################

    /**
     * Perform "login" on the client side. The login consists of putting the credentials in a place
     * where the IIOP/Remote EJB code can find it, and sent it over to the remote server where the
     * actual authentication takes place.
     *
     * @param int type whether it is <i> username_password</i> or <i> certificate </i> based login.
     * @param CallbackHandler the callback handler to gather user information.
     * @exception LoginException the exception thrown by the callback handler.
     */
    public static Subject doClientLogin(int type, CallbackHandler handler) throws LoginException {

        // The subject will actually be filled in with a credential as required by the csiv2 layer in the LoginModule.
        Subject subject = new Subject();

        switch (type) {
            case USERNAME_PASSWORD:
                // Store a PasswordCredential with the username and password obtained from UsernamePasswordStore into the
                // passed in subject.
                //
                // This assumes com.sun.enterprise.security.auth.login.ClientPasswordLoginModule is configured.
                //
                // ClientPasswordLoginModule will add a PasswordCredential with the username and password
                // obtained from primarily UsernamePasswordStore into the subject. The handler is used
                // when UsernamePasswordStore does not contain the username and password.
                privileged(() -> addCredentialToSubject(CLIENT_JAAS_PASSWORD, subject, handler));
                
                // Read PasswordCredential from Subject and put it in the security context
                postClientAuth(subject, PasswordCredential.class);
                
                return subject;
            case CERTIFICATE:
                privileged(() -> addCredentialToSubject(CLIENT_JAAS_CERTIFICATE, subject, handler));
                
                postClientAuth(subject, X509CertificateCredential.class);
                
                return subject;
            case ALL:
                privileged(() -> addCredentialToSubject(CLIENT_JAAS_PASSWORD, subject, handler));
                postClientAuth(subject, PasswordCredential.class);
                
                privileged(() -> addCredentialToSubject(CLIENT_JAAS_PASSWORD, subject, handler));
                postClientAuth(subject, X509CertificateCredential.class);
                
                return subject;
            default:
                privileged(() -> addCredentialToSubject(CLIENT_JAAS_PASSWORD, subject, handler));
                postClientAuth(subject, PasswordCredential.class);
                
                return subject;
        }
    }
    
    /**
     * Perform logout on the client side.
     *
     * @exception LoginException
     */
    public static void doClientLogout() throws LoginException {
        unsetClientSecurityContext();
    }





    // ############################   Private methods ######################################



    public static void validateJaasLogin(String username, String jaasCtx, String realm, Subject subject) {
        try {
            tryJaasLogin(jaasCtx, subject);
        } catch (Exception e) {
            LOGGER.log(INFO, auditAtnRefusedError, username);

            auditAuthenticate(username, realm, false);

            throwLoginException(e);
        }

        auditAuthenticate(username, realm, true);
    }

    public static void tryJaasLogin(String jaasCtx, Subject subject) throws javax.security.auth.login.LoginException  {
        // A dummyCallback is used to satisfy JAAS but it is never used.
        // The name/password info is already contained in the Subject's credentials
        LoginContext loginContext = new LoginContext(jaasCtx, subject, dummyCallback);
        loginContext.login();
    }

    public static void addCredentialToSubject(String name, Subject subject, CallbackHandler handler) {
        try {
            LoginContext lg = new LoginContext(name, subject, handler);
            lg.login();
        } catch (javax.security.auth.login.LoginException e) {
            throw (LoginException) new LoginException(e.toString()).initCause(e);
        }
    }

    public static Subject getValidSubject(Subject subject) {
        if (subject == null) {
            return new Subject();
        }

        return subject;
    }

    public static String getValidRealm(String realm) {
        if (realm == null || !Realm.isValidRealm(realm)) {
            return Realm.getDefaultRealm();
        }

        return realm;
    }

    public static String getJaasContext(String realm) {
        try {
            return Realm.getInstance(realm).getJAASContext();
        } catch (Exception ex) {
            if (ex instanceof LoginException) {
                throw (LoginException) ex;
            }

            throw (LoginException) new LoginException(ex.toString()).initCause(ex);
        }
    }

    public static Realm getRealmInstance(String realmName) throws NoSuchRealmException {
        String validRealmName = realmName;

        if (realmName == null || "".equals(realmName)) {
            validRealmName = Realm.getDefaultRealm();
        }

        return Realm.getInstance(validRealmName);
    }

    public static void throwLoginException(Exception exception) {
        throwLoginException(exception, e -> "Login failed: " + e.getMessage());
    }

    public static void throwLoginException(Exception e, Function<Exception, String> exceptionStringFn) {
        if (e instanceof LoginException) {
            throw (LoginException) e;
        } else {
            throw new LoginException(exceptionStringFn.apply(e), e);
        }
    }

    /**
     * Extract the relevant username and realm information from the subject and sets the correct state
     * in the security context. The relevant information is set into the Thread Local Storage. The IIOP
     * code (for remote EJB) knows where to find this, and uses that data to sent it over the wire
     * to the remote server where the actual authentication takes place.
     *
     *
     * @param Subject the subject returned by the JAAS login.
     * @param Class the class of the credential object stored in the subject
     *
     */
    private static void postClientAuth(Subject subject, Class<?> clazz) {
        if (LOGGER.isLoggable(FINEST)) {
            LOGGER.log(FINEST, "LoginContextDriver post login subject :{0}", subject);
        }

        Iterator<?> credentialsIterator = privileged(() -> subject.getPrivateCredentials(clazz)).iterator();

        while (credentialsIterator.hasNext()) {
            Object credential = credentialsIterator.next();

            if (credential instanceof PasswordCredential) {
                PasswordCredential passwordCredential = (PasswordCredential) credential;
                String user = passwordCredential.getUser();

                if (LOGGER.isLoggable(FINEST)) {
                    LOGGER.log(FINEST, "In LoginContextDriver user-pass login:{0} realm :{1}", new Object[]{user, passwordCredential.getRealm()});
                }

                setClientSecurityContext(user, subject);

                return;
            } else if (credential instanceof X509CertificateCredential) {
                X509CertificateCredential certificateCredential = (X509CertificateCredential) credential;
                String user = certificateCredential.getAlias();

                if (LOGGER.isLoggable(FINEST)) {
                    LOGGER.log(FINEST, "In LoginContextDriver cert-login::{0} realm :{1}", new Object[]{user, certificateCredential.getRealm()});
                }

                setClientSecurityContext(user, subject);

                return;
            }
        }
    }

    /**
     * Sets the security context on the appclient side. It sets the relevant information into the TLS
     *
     * @param String username is the user who authenticated
     * @param Subject is the subject representation of the user
     * @param Credentials the credentials that the server associated with it
     */
    private static void setClientSecurityContext(String username, Subject subject) {
        ClientSecurityContext.setCurrent(new ClientSecurityContext(username, subject));
    }

    /**
     * Unsets the current appclient security context on the Thread Local Storage
     */
    private static void unsetClientSecurityContext() {
        ClientSecurityContext.setCurrent(null);
    }

    private static AuditManager getAuditManager() {
        if (AUDIT_MANAGER != null) {
            return AUDIT_MANAGER;
        }
        return _getAuditManager();
    }

    private static synchronized AuditManager _getAuditManager() {
        if (AUDIT_MANAGER == null) {
            SecurityServicesUtil secServUtil = Globals.get(SecurityServicesUtil.class);
            AUDIT_MANAGER = secServUtil.getAuditManager();
        }
        return AUDIT_MANAGER;
    }

    public static void auditAuthenticate(String username, String realm, boolean success) {
        if (getAuditManager().isAuditOn()) {
            getAuditManager().authentication(username, realm, success);
        }
    }

    public static void jmacLogin(Subject subject, Principal callerPrincipal, String realmName) throws LoginException {
        if (CertificateRealm.AUTH_TYPE.equals(realmName)) {
            if (callerPrincipal instanceof X500Principal) {
                LoginContextDriver.jmacLogin(subject, (X500Principal) callerPrincipal);
            }
        } else if (!callerPrincipal.equals(SecurityContext.getDefaultCallerPrincipal())) {
            LoginContextDriver.jmacLogin(subject, callerPrincipal.getName(), realmName);
        }
    }

    public static Subject jmacLogin(Subject subject, X500Principal x500Principal) throws LoginException {
        if (subject == null) {
            subject = new Subject();
        }

        String userName = "";
        try {
            userName = x500Principal.getName();
            subject.getPublicCredentials().add(x500Principal);

            CertificateRealm certRealm = (CertificateRealm) Realm.getInstance(CertificateRealm.AUTH_TYPE);
            String jaasCtx = certRealm.getJAASContext();
            if (jaasCtx != null) {
                // The subject has the Certificate Credential.
                new LoginContext(jaasCtx, subject, dummyCallback).login();
            }
            certRealm.authenticate(subject, x500Principal);
        } catch (Exception ex) {
            LOGGER.log(INFO, auditAtnRefusedError, userName);
            if (getAuditManager().isAuditOn()) {
                getAuditManager().authentication(userName, CertificateRealm.AUTH_TYPE, false);
            }

            if (ex instanceof LoginException) {
                throw (LoginException) ex;
            }
            throw new LoginException("Authentication failed.", ex);
        }

        LOGGER.log(FINE, "JMAC cert login succeeded for {0}", userName);

        if (getAuditManager().isAuditOn()) {
            getAuditManager().authentication(userName, CertificateRealm.AUTH_TYPE, true);
        }
        // do not set the security Context

        return subject;
    }

    public static Subject jmacLogin(Subject subject, String userName, String realm) throws LoginException {
        if (subject == null) {
            subject = new Subject();
        }

        try {
            if (isEmpty(realm)) {
                realm = Realm.getDefaultRealm();
            }

            Enumeration<String> groups = Realm.getInstance(realm).getGroupNames(userName);
            if (groups != null) {
                while (groups.hasMoreElements()) {
                    subject.getPrincipals().add(new Group(groups.nextElement()));
                }
            }
        } catch (Exception ex) {
            LOGGER.log(FINE, "Exception when trying to populate groups for CallerPrincipal " + userName, ex);
        }

        return subject;
    }

    /**
     * Performs login for JMAC security. The difference between this method and others is that it just verifies whether the login
     * will succeed in the given realm. It does not set the result of the authentication in the appserver runtime environment A
     * silent return from this method means that the given user succeeding in authenticating with the given password in the given
     * realm
     *
     * @param subject
     * @param username
     * @param password
     * @param realmName the realm to authenticate under
     * @returns Subject on successful authentication
     * @throws LoginException
     */
    public static Subject jmacLogin(Subject subject, String username, char[] password, String realmName) throws LoginException {
        if (realmName == null || !Realm.isValidRealm(realmName)) {
            realmName = Realm.getDefaultRealm();
        }

        if (subject == null) {
            subject = new Subject();
        }

        final PasswordCredential passwordCredential = new PasswordCredential(username, password, realmName);
        subject.getPrivateCredentials().add(passwordCredential);

        String jaasCtx = getJaasCtx(realmName);

        LOGGER.log(FINE, "JMAC login user {0} into realm {1} using JAAS module {2}",
                new Object[] {username, realmName, jaasCtx});

        try {
            // A dummyCallback is used to satisfy JAAS but it is never used.
            // name/pwd info is already contained in Subject's Credential
            new LoginContext(jaasCtx, subject, dummyCallback).login();

        } catch (Exception e) {
            LOGGER.log(INFO, SecurityLoggerInfo.auditAtnRefusedError, username);
            if (getAuditManager().isAuditOn()) {
                getAuditManager().authentication(username, realmName, false);
            }

            if (e instanceof LoginException) {
                throw (LoginException) e;
            }
            throw new LoginException("Login failed: " + e.getMessage(), e);
        }
        if (getAuditManager().isAuditOn()) {
            getAuditManager().authentication(username, realmName, true);
        }
        LOGGER.log(FINE, "jmac Password login succeeded for {0}", username);

        return subject;
        // do not set the security Context
    }

    private static String getJaasCtx(String realm) {
        try {
            return Realm.getInstance(realm).getJAASContext();
        } catch (Exception ex) {
            if (ex instanceof LoginException) {
                throw (LoginException) ex;
            }

            throw (LoginException) new LoginException(ex.toString()).initCause(ex);
        }
    }

}
