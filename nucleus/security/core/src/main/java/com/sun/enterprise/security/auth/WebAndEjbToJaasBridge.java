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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth;

import static com.sun.enterprise.security.SecurityLoggerInfo.auditAtnRefusedError;
import static com.sun.enterprise.security.SecurityLoggerInfo.certLoginBadRealmError;
import static com.sun.enterprise.security.SecurityLoggerInfo.invalidOperationForRealmError;
import static com.sun.enterprise.security.SecurityLoggerInfo.noSuchUserInRealmError;
import static com.sun.enterprise.security.SecurityLoggerInfo.unknownCredentialError;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.auditAuthenticate;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getJaasContext;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.getValidRealm;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.throwLoginException;
import static com.sun.enterprise.security.auth.login.LoginContextDriver.tryJaasLogin;
import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;
import static java.util.logging.Level.WARNING;

import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.x500.X500Principal;

import org.glassfish.security.common.Group;
import org.glassfish.security.common.PrincipalImpl;

import com.sun.enterprise.common.iiop.security.AnonCredential;
import com.sun.enterprise.common.iiop.security.GSSUPName;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.auth.login.DigestCredentials;
import com.sun.enterprise.security.auth.login.FileLoginModule;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.login.common.ServerLoginCallbackHandler;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;
import com.sun.enterprise.security.auth.realm.InvalidOperationException;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.certificate.CertificateRealm;
import com.sun.enterprise.security.auth.realm.certificate.OID;
import com.sun.enterprise.security.auth.realm.file.FileRealm;

/**
 * This class contains a collection of methods that are used by the Web and EJB containers
 * to interact with the JAAS based LoginModules and set the current (per thread) security context.
 * The WebContainer uses these for the native Servlet authentication, which is distinct from the newer
 * JASPIC Servlet Container Profile authentication.
 *
 * <p>
 * Note that the JAAS system determines which LoginModule is ultimately being called, for instance the
 * {@link FileLoginModule}.
 * Actual LoginModules in Payara are each paired with a Payara Realm, for instance the {@link FileLoginModule}
 * is paired with the {@link FileRealm}. The LoginModule typically does very little else than directly delegating
 * to its peer Realm.
 *
 * <p>
 * Also note that with few exceptions neither the LoginModule nor the Realm set the current security context, but only
 * validate credentials and, if valid, return zero or more roles. The methods in this class set the security context if
 * the JAAS credential validation succeeds.
 *
 * <p>
 * All LoginModules used by Payara have the convention that* credentials are passed in via a {@link Subject} instance
 * (instead of the usual {@link CallbackHandler}). The validation outcome is a boolean, but is being passed via an exception.
 * No exception means success, while an exception means no success. If the LoginModule/Realm returned any roles they will
 * put into the same Subject instance that was used to pass the credentials in.
 *
 * @author Harpreet Singh (hsingh@eng.sun.com)
 * @author Jyri Virkki
 * @author Arjan Tijms (refactoring)
 *
 */
public final class WebAndEjbToJaasBridge {

    private static final Logger LOGGER = SecurityLoggerInfo.getLogger();

    private WebAndEjbToJaasBridge() {
        // No instances of this class
    }


    /**
     * This method is just a convenience wrapper for <i>login(Subject, Class)</i> method. It will
     * construct a PasswordCredential class.
     *
     * @param username
     * @param password
     * @param realmName the name of the realm to login into, if realmName is null, we login into
     *            the default realm
     */
    public static void login(String username, char[] password, String realmName) {
        Subject subject = new Subject();
        privileged(() -> subject.getPrivateCredentials().add(new PasswordCredential(username, password, getValidRealm(realmName))));

        login(subject, PasswordCredential.class);
    }

    /**
     * This method performs the login on the server side.
     *
     * <P>
     * This method is the main login method for Payara. It is called with a Subject and the type (class)
     * of credential which should be checked. The Subject must contain a credential of the specified
     * type or login will fail.
     *
     * <P>
     * While the implementation has been cleaned up, the login process still consists of a number of
     * special cases which are treated separately at the realm level. In the future tighter JAAS
     * integration could clean some of this up.
     *
     * <P>
     * The following credential types are recognized at this time:
     *
     * <ul>
     *   <li>PasswordCredential - This is the general case for all login methods which rely on the client
     *       providing a name and password. It can be used with any realms/JAAS login modules which expect
     *       such data (e.g. file realm, LDAP realm, UNIX realm)
     *   <li>X509CertificateCredential - Special case for SSL client auth. Here authentication has already
     *       been done by the SSL subsystem so this login only creates a security context based on the
     *       certificate data.
     *   <li>AnonCredential - Unauthenticated session, set anonymous security context.
     *   <li>GSSUPName - Retrieve user and realm and set security context.
     *   <li>X500Name - Retrieve user and realm and set security context.
     * </ul>
     *
     * @param subject the subject of the client
     * @param credentialClass the class of the credential packaged in the subject.
     *
     * @throws LoginException when login fails
     */
    public static void login(Subject subject, Class<?> credentialClass) {
        LOGGER.finest(() -> "Processing login with credentials of type: " + credentialClass);

        if (credentialClass.equals(PasswordCredential.class)) {
            doPasswordLogin(subject);

        } else if (credentialClass.equals(X509CertificateCredential.class)) {
            doX509CertificateLogin(subject);

        } else if (credentialClass.equals(AnonCredential.class)) {
            doAnonymousLogin();

        } else if (credentialClass.equals(GSSUPName.class)) {
            doGSSUPLogin(subject);

        } else if (credentialClass.equals(X500Principal.class)) {
            doX500Login(subject, null);

        } else {
            LOGGER.log(INFO, unknownCredentialError, credentialClass.toString());
            throw new LoginException("Unknown credential type, cannot login.");
        }
    }

    public static void doX500Login(Subject subject, String appModuleID) {
        doX500Login(subject, CertificateRealm.AUTH_TYPE, appModuleID);
    }

    /**
     * A special case login for X500Name credentials.This is invoked for
     * certificate login because the containers extract the X.500 name from the
     * X.509 certificate before calling into this class.
     *
     * @param subject
     * @param realmName
     * @param appModuleID
     * @throws LoginException when login fails
     *
     */
    public static void doX500Login(Subject subject, String realmName, String appModuleID) {
        LOGGER.finest(() -> String.format("doX500Login(subject=%s, realmName=%s, appModuleID=%s)",
            subject, realmName, appModuleID));

        String user = null;
        try {
            X500Principal x500principal = getPublicCredentials(subject, X500Principal.class);
            if (x500principal == null) {
                // Should never happen
                return;
            }

            user = x500principal.getName(X500Principal.RFC2253, OID.getOIDMap());

            // In the RI-inherited implementation this directly creates
            // some credentials and sets the security context.
            //
            // This means that the certificate realm does not get an opportunity to
            // process the request. While the realm will not do any authentication
            // (already done by this point) it can choose to adjust the groups or principal
            // name or other variables of the security context.
            //
            // Of course, bug 4646134 needs to be kept in mind at all times, even though time has
            // forgotten what 4646134 was.

            Realm realm = Realm.getInstance(realmName);

            if (realm instanceof CertificateRealm) { // Should always be true

                CertificateRealm certRealm = (CertificateRealm) realm;
                String jaasCtx = certRealm.getJAASContext();
                if (jaasCtx != null) {
                    // The subject has the certificate Credential.
                    new LoginContext(
                            jaasCtx, subject,
                            new ServerLoginCallbackHandler(user, null, appModuleID))
                        .login();
                }

                // The name that the cert realm decided to set as the caller principal name
                user = certRealm.authenticate(subject, x500principal);

                auditAuthenticate(user, realmName, true);
            } else {
                // Should never come here
                LOGGER.warning(certLoginBadRealmError);
                setSecurityContext(user, subject, realmName);
            }

            if (LOGGER.isLoggable(FINE)) {
                LOGGER.log(FINE, "X.500 name login succeeded for : {0}", user);
            }
        } catch (LoginException le) {
            auditAuthenticate(user, realmName, false);
            throw le;
        } catch (Exception ex) {
            throw (LoginException) new LoginException(ex.toString()).initCause(ex);
        }
    }

    /**
     * Performs Digest authentication based on RFC 2617. It
     *
     * @param digestCred DigestCredentials
     */
    public static void login(DigestCredentials digestCred) throws javax.security.auth.login.LoginException {
        Subject subject = new Subject();
        subject.getPrivateCredentials().add(digestCred);

        try {
            tryJaasLogin(getJaasContext(digestCred.getRealmName()), subject);
        } catch (Exception e) {
            LOGGER.log(INFO, auditAtnRefusedError, digestCred.getUserName());
            LOGGER.log(FINEST, "doPasswordLogin fails", e);

            auditAuthenticate(digestCred.getUserName(), digestCred.getRealmName(), false);

            throwLoginException(e);
        }

        setSecurityContext(digestCred.getUserName(), subject, digestCred.getRealmName());
    }


    /**
     * This method is used for logging in a run As principal. It creates a JAAS subject whose credential
     * is to type GSSUPName. This is used primarily for runas
     *
     * @throws LoginException if login fails
     */
    public static void loginPrincipal(String username, String realmName) {
        if (realmName == null || realmName.length() == 0) {
            // No realm provided, assuming default
            realmName = Realm.getDefaultRealm();
        }

        Subject subject = new Subject();
        PrincipalImpl callerPrincipal = new PrincipalImpl(username);
        GSSUPName name = new GSSUPName(username, realmName);

        privileged(() -> {
            subject.getPrincipals().add(callerPrincipal);
            subject.getPublicCredentials().add(name);
        });


        try {
            Enumeration<String> groupNames = Realm.getInstance(realmName).getGroupNames(username);
            Set<Principal> principalSet = subject.getPrincipals();
            while (groupNames.hasMoreElements()) {
                principalSet.add(new Group(groupNames.nextElement()));
            }

        } catch (InvalidOperationException ex) {
            LOGGER.log(WARNING, invalidOperationForRealmError, new Object[] { username, realmName, ex.toString() });
        } catch (NoSuchUserException ex) {
            LOGGER.log(WARNING, noSuchUserInRealmError, new Object[] { username, realmName, ex.toString() });
        } catch (NoSuchRealmException ex) {
            throw (LoginException) new LoginException(ex.toString()).initCause(ex);
        }

        setSecurityContext(username, subject, realmName);
    }

    /**
     * This method logs out the user by clearing the security context.
     *
     * @throws LoginException if logout fails
     */
    public static void logout() {
        unsetSecurityContext();
    }


    // ############################   Private methods ######################################

    /**
     * Log in subject with PasswordCredential. This is a generic login which applies to all login
     * mechanisms which process PasswordCredential. In other words, any mechanism which receives an
     * actual username, realm and password set from the client.
     *
     * <P>
     * The realm contained in the credential is checked, and a JAAS LoginContext is created using a
     * context name obtained from the appropriate Realm instance. The applicable JAAS LoginModule is
     * initialized (based on the JAAS login configuration) and login() is invoked on it.
     *
     * <P>
     * RI code makes several assumptions which are retained here:
     *
     * <ul>
     *   <li>The PasswordCredential is stored as a private credential of the subject.
     *   <li>There is only one such credential present (actually, only the first one is relevant if more are present).
     * </ul>
     *
     * @param subject Subject to be authenticated.
     * @throws LoginException Thrown if the login fails.
     *
     */
    private static void doPasswordLogin(Subject subject) {
        PasswordCredential passwordCredential = getPrivateCredentials(subject, PasswordCredential.class);

        String user = passwordCredential.getUser();
        String realm = passwordCredential.getRealm();
        String jaasCtx = getJaasContext(realm);

        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "Logging in user [{0}] into realm: {1} using JAAS module: {2}", new Object[]{user, realm, jaasCtx});
        }

        try {
            tryJaasLogin(jaasCtx, subject);
        } catch (Exception e) {
            LOGGER.log(FINEST, "doPasswordLogin fails", e);

            auditAuthenticate(user, realm, false);
            throwLoginException(e);
        }

        auditAuthenticate(user, realm, true);

        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "Password login succeeded for : {0}", user);
        }

        setSecurityContext(user, subject, realm);

        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "Set security context as user: {0}", user);
        }
    }

    /**
     * A special case login for handling X509CertificateCredential. This does not get triggered based on
     * current RI code. See X500Login.
     *
     * @throws LoginException Thrown if the login fails.
     *
     */
    private static void doX509CertificateLogin(Subject subject) {
        LOGGER.log(FINE, "Processing X509 certificate login.");

        String user = null;
        String realm = CertificateRealm.AUTH_TYPE;

        try {
            user = getPublicCredentials(subject, X509CertificateCredential.class).getAlias();

            if (LOGGER.isLoggable(FINE)) {
                LOGGER.log(FINE, "Set security context as user: {0}", user);
            }

            setSecurityContext(user, subject, realm);
            auditAuthenticate(user, realm, true);

        } catch (LoginException le) {
            auditAuthenticate(user, realm, false);
            throw le;
        }
    }

    /**
     * A special case login for anonymous credentials (no login info).
     *
     * @throws LoginException Thrown if the login fails.
     *
     */
    private static void doAnonymousLogin() {
        // Instance of anonymous credential login with guest
        SecurityContext.setUnauthenticatedContext();
        LOGGER.log(FINE, "Set anonymous security context.");
    }

    /**
     * A special case login for GSSUPName credentials.
     *
     * @throws LoginException Thrown if the login fails.
     */
    private static void doGSSUPLogin(Subject s) {
        LOGGER.fine("Processing GSSUP login.");

        String user = null;
        String realm = Realm.getDefaultRealm();

        try {
            user = getPublicCredentials(s, GSSUPName.class).getUser();

            setSecurityContext(user, s, realm);
            auditAuthenticate(user, realm, true);

            if (LOGGER.isLoggable(FINE)) {
                LOGGER.log(FINE, "GSSUP login succeeded for : {0}", user);
            }
        } catch (LoginException le) {
            auditAuthenticate(user, realm, false);
            throw le;
        }
    }

    /**
     * Retrieve a public credential of the given type (java class) from the subject.
     *
     * <P>
     * This method retains the RI assumption that only the first credential of the given type is used.
     *
     * @throws LoginException Thrown if the login fails.
     */
    private static <T> T getPublicCredentials(Subject subject, Class<T> cls) {
        Set<T> credset = subject.getPublicCredentials(cls);

        Iterator<T> iter = credset.iterator();

        if (!iter.hasNext()) {
            String credentialType = cls.toString();
            LOGGER.log(FINER, () -> "Expected public credentials of type : " + credentialType + " but none found.");
            throw new LoginException("Expected public credential of type: " + credentialType + " but none found.");
        }

        try {
            return privileged(() -> iter.next());
        } catch (Exception exception) {
            // Should never come here
            throwLoginException(exception, e -> "Failed to retrieve public credential: " + e.getMessage());
            return null;
        }

    }

    /**
     * Retrieve a private credential of the given type (java class) from the subject.
     *
     * <P>
     * This method retains the RI assumption that only the first credential of the given type is used.
     *
     * @throws LoginException Thrown if the login fails.
     */
    private static <T> T getPrivateCredentials(Subject subject, Class<T> cls) {
        Iterator<T> iter = privileged(() -> subject.getPrivateCredentials(cls)).iterator();

        if (!iter.hasNext()) {
            String credmsg = cls.toString();
            if (LOGGER.isLoggable(FINER)) {
                LOGGER.log(FINER, "Expected private credential of type: {0} but none found.", credmsg);
            }
            throw new LoginException("Expected private credential of type: " + credmsg + " but none found.");
        }

        // Retrieve only first credential of give type
        try {
            return privileged(() -> iter.next());
        } catch (Exception e) {
            // should never come here
            if (e instanceof LoginException) {
                throw (LoginException) e;
            }
            throw new LoginException("Failed to retrieve private credential: " + e.getMessage(), e);
        }
    }

    /**
     * This method sets the security context on the current Thread Local Storage
     *
     * @param String username is the user who authenticated
     * @param Subject is the subject representation of the user
     * @param Credentials the credentials that the server associated with it
     */
    private static void setSecurityContext(String userName, Subject subject, String realm) {
        SecurityContext.setCurrent(new SecurityContext(userName, subject, realm));
    }

    /**
     * Set the current security context on the Thread Local Storage to null.
     *
     */
    private static void unsetSecurityContext() {
        SecurityContext.setCurrent((SecurityContext) null);
    }
}
