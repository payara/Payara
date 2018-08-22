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
package com.sun.enterprise.security.auth.login;

import static com.sun.enterprise.security.SecurityLoggerInfo.auditAtnRefusedError;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;

import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

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
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.enterprise.security.common.ClientSecurityContext;
import com.sun.enterprise.security.common.SecurityConstants;

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

    private static final Logger _logger = SecurityLoggerInfo.getLogger();

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
     * Perform login on the client side. It just simulates the login on the client side. The method uses
     * the callback handlers and generates correct credential information that will be later sent to the
     * server
     *
     * @param int type whether it is <i> username_password</i> or <i> certificate </i> based login.
     * @param CallbackHandler the callback handler to gather user information.
     * @exception LoginException the exception thrown by the callback handler.
     */
    public static Subject doClientLogin(int type, javax.security.auth.callback.CallbackHandler jaasHandler) throws LoginException {
        final javax.security.auth.callback.CallbackHandler handler = jaasHandler;
        // the subject will actually be filled in with a PasswordCredential
        // required by the csiv2 layer in the LoginModule.
        // we create the dummy credential here and call the
        // set security context. Thus, we have 2 credentials, one each for
        // the csiv2 layer and the other for the RI.
        final Subject subject = new Subject();

        if (type == SecurityConstants.USERNAME_PASSWORD) {
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public java.lang.Object run() {
                    try {
                        LoginContext lg = new LoginContext(SecurityConstants.CLIENT_JAAS_PASSWORD, subject, handler);
                        lg.login();
                    } catch (javax.security.auth.login.LoginException e) {
                        throw (LoginException) new LoginException(e.toString()).initCause(e);
                    }

                    return null;
                }
            });
            postClientAuth(subject, PasswordCredential.class);
            return subject;
        } else if (type == SecurityConstants.CERTIFICATE) {
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public java.lang.Object run() {
                    try {
                        LoginContext lg = new LoginContext(SecurityConstants.CLIENT_JAAS_CERTIFICATE, subject, handler);
                        lg.login();
                    } catch (javax.security.auth.login.LoginException e) {
                        throw (LoginException) new LoginException(e.toString()).initCause(e);
                    }

                    return null;
                }
            });
            postClientAuth(subject, X509CertificateCredential.class);
            return subject;
        } else if (type == SecurityConstants.ALL) {
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public java.lang.Object run() {
                    try {
                        LoginContext lgup = new LoginContext(SecurityConstants.CLIENT_JAAS_PASSWORD, subject, handler);
                        LoginContext lgc = new LoginContext(SecurityConstants.CLIENT_JAAS_CERTIFICATE, subject, handler);
                        lgup.login();
                        postClientAuth(subject, PasswordCredential.class);

                        lgc.login();
                        postClientAuth(subject, X509CertificateCredential.class);
                    } catch (javax.security.auth.login.LoginException e) {
                        throw (LoginException) new LoginException(e.toString()).initCause(e);
                    }

                    return null;
                }
            });
            return subject;
        } else {
            AppservAccessController.doPrivileged(new PrivilegedAction() {
                @Override
                public java.lang.Object run() {
                    try {
                        LoginContext lg = new LoginContext(SecurityConstants.CLIENT_JAAS_PASSWORD, subject, handler);
                        lg.login();
                        postClientAuth(subject, PasswordCredential.class);
                    } catch (javax.security.auth.login.LoginException e) {
                        throw (LoginException) new LoginException(e.toString()).initCause(e);
                    }
                    return null;
                }
            });
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
            _logger.log(INFO, auditAtnRefusedError, username);

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
            throw (LoginException) new LoginException(exceptionStringFn.apply(e)).initCause(e);
        }
    }

    /**
     * Extract the relevant username and realm information from the subject and sets the correct state
     * in the security context. The relevant information is set into the Thread Local Storage from which
     * then is extracted to send over the wire.
     *
     * @param Subject the subject returned by the JAAS login.
     * @param Class the class of the credential object stored in the subject
     *
     */
    private static void postClientAuth(Subject subject, Class<?> clazz) {
        final Class<?> clas = clazz;
        final Subject fs = subject;
        Set credset = (Set) AppservAccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public java.lang.Object run() {
                if (_logger.isLoggable(Level.FINEST)) {
                    _logger.log(Level.FINEST, "LCD post login subject :" + fs);
                }
                return fs.getPrivateCredentials(clas);
            }
        });
        final Iterator iter = credset.iterator();
        while (iter.hasNext()) {
            Object obj = null;
            try {
                obj = AppservAccessController.doPrivileged(new PrivilegedAction() {
                    @Override
                    public java.lang.Object run() {
                        return iter.next();
                    }
                });
            } catch (Exception e) {
                // should never come here
                _logger.log(Level.SEVERE, SecurityLoggerInfo.securityAccessControllerActionError, e);
            }
            if (obj instanceof PasswordCredential) {
                PasswordCredential p = (PasswordCredential) obj;
                String user = p.getUser();
                if (_logger.isLoggable(Level.FINEST)) {
                    String realm = p.getRealm();
                    _logger.log(Level.FINEST, "In LCD user-pass login:" + user + " realm :" + realm);
                }
                setClientSecurityContext(user, fs);
                return;
            } else if (obj instanceof X509CertificateCredential) {
                X509CertificateCredential p = (X509CertificateCredential) obj;
                String user = p.getAlias();
                if (_logger.isLoggable(FINEST)) {
                    _logger.log(FINEST, "In LCD cert-login::" + user + " realm :" + p.getRealm());
                }
                setClientSecurityContext(user, fs);
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

}
