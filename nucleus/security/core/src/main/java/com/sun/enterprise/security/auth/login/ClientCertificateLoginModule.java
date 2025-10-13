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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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

import static com.sun.enterprise.security.auth.login.LoginContextDriver.CERT_REALMNAME;
import static java.util.logging.Level.FINE;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import javax.security.auth.x500.X500Principal;

import com.sun.enterprise.security.auth.realm.certificate.OID;

import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.UserNameAndPassword;
import org.glassfish.security.common.UserPrincipal;


import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;
import com.sun.enterprise.security.integration.AppClientSSL;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * <p>
 * This LoginModule authenticates users with X509 certificates.
 *
 * <p>
 * If testUser successfully authenticates itself, a <code>UserPrincipal</code> with the testUser's
 * username is added to the Subject.
 *
 * <p>
 * This LoginModule recognizes the debug option. If set to true in the login Configuration, debug
 * messages will be output to the output stream, System.out.
 *
 * @author Harpreet Singh (harpreet.singh@sun.com)
 */
public class ClientCertificateLoginModule implements LoginModule {

    private final static Logger _logger = SecurityLoggerInfo.getLogger();
    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ClientCertificateLoginModule.class);

    private static KeyStore keyStore;

    // Initial state
    private Subject subject;
    private CallbackHandler callbackHandler;

    // Configurable option
    private boolean debug;

    // The authentication status
    private boolean succeeded;
    private boolean commitSucceeded;

    private String alias;
    private X509Certificate certificate;

    private UserPrincipal userPrincipal;

    private AppClientSSL ssl;
    private SSLUtils sslUtils;

    /**
     * Initialize this <code>LoginModule</code>.
     *
     * <p>
     *
     * @param subject the <code>Subject</code> to be authenticated.
     * <p>
     *
     * @param callbackHandler a <code>CallbackHandler</code> for communicating with the end user
     * (prompting for usernames and passwords, for example).
     * <p>
     *
     * @param sharedState shared <code>LoginModule</code> state. (unused)
     * <p>
     *
     * @param options options specified in the login <code>Configuration</code> for this particular
     * <code>LoginModule</code>. Only used for the "debug" option.
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        // Initialize any configured options
        debug = "true".equalsIgnoreCase((String) options.get("debug"));
        sslUtils = Globals.getDefaultHabitat().getService(SSLUtils.class);
    }

    /**
     * Authenticate the user by prompting for a username and password.
     *
     * <p>
     *
     * @return true in all cases since this <code>LoginModule</code> should not be ignored.
     *
     * @exception LoginException if this <code>LoginModule</code> is unable to perform the
     * authentication.
     */
    @Override
    public boolean login() throws LoginException {

        // Prompt for a username and password
        if (callbackHandler == null) {
            throw new LoginException("Error: no CallbackHandler available " + "to garner authentication information from the user");
        }

        try {
            String[] certificateNames = new String[keyStore.size()];
            String[] aliasNames = new String[keyStore.size()];

            Enumeration<String> aliases = keyStore.aliases();
            for (int i = 0; i < keyStore.size(); i++) {
                aliasNames[i] = aliases.nextElement();
                certificateNames[i] = ((X509Certificate) keyStore.getCertificate(aliasNames[i]))
                    .getSubjectX500Principal().getName(X500Principal.RFC2253, OID.getOIDMap());
            }

            Callback[] callbacks = new Callback[] {createChoiceCallback(certificateNames)};

            callbackHandler.handle(callbacks);

            int[] selectedIndexes = ((ChoiceCallback) callbacks[0]).getSelectedIndexes();

            if (selectedIndexes == null) {
                throw new LoginException("No certificate selected!");
            } else if (selectedIndexes[0] == -1) {
                throw new LoginException("Incorrect keystore password");
            }

            if (debug) {
                _logger.fine(() -> "[ClientCertificateLoginModule] user entered certificates: "
                    + Arrays.stream(selectedIndexes).mapToObj(i -> aliasNames[i]).collect(Collectors.toList()));
            }

            // The authenticate method previously picked out the wrong alias.
            // Since we allow only 1 choice the first element in idx
            // idx[0] should have the selected index.
            alias = aliasNames[selectedIndexes[0]];
            certificate = (X509Certificate) keyStore.getCertificate(alias);

            // The authenticate should always return a true.
            if (debug) {
                _logger.fine("\t\t[ClientCertificateLoginModule] authentication succeeded");
            }
            succeeded = true;
            return true;
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(
                "Error: " + uce.getCallback() + " not available to garner authentication information from the user");
        } catch (Exception e) {
            throw new LoginException(e.toString());
        }
    }

    /**
     * <p>
     * This method is called if the LoginContext's overall authentication succeeded (the relevant
     * REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules succeeded).
     *
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private
     * state saved by the <code>login</code> method), then this method associates a
     * <code>UserPrincipal</code> with the <code>Subject</code> located in the <code>LoginModule</code>.
     * If this LoginModule's own authentication attempted failed, then this method removes any state
     * that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the commit fails.
     *
     * @return true if this LoginModule's own login and commit attempts succeeded, or false otherwise.
     */
    @Override
    public boolean commit() throws LoginException {
        if (succeeded == false) {
            return false;
        }

        // Add a Principal (authenticated identity) to the Subject
        // Assume the user we authenticated is the UserPrincipal
        userPrincipal = new UserNameAndPassword(alias);
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }

        if (debug) {
            if (_logger.isLoggable(FINE)) {
                _logger.log(FINE, "\t\t[ClientCertificateLoginModule] " + "added UserPrincipal to Subject");
            }
        }

        ssl = new AppClientSSL();
        ssl.setCertNickname(this.alias);
        sslUtils.setAppclientSsl(ssl);

        X509Certificate[] certChain = new X509Certificate[1];
        certChain[0] = certificate;
        X509CertificateCredential pc = new X509CertificateCredential(certChain, alias, CERT_REALMNAME);
        if (!subject.getPrivateCredentials().contains(pc)) {
            subject.getPrivateCredentials().add(pc);
        }

        commitSucceeded = true;
        return true;
    }

    /**
     * <p>
     * This method is called if the LoginContext's overall authentication failed. (the relevant
     * REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules did not succeed).
     *
     * <p>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private
     * state saved by the <code>login</code> and <code>commit</code> methods), then this method cleans
     * up any state that was originally saved.
     *
     * <p>
     *
     * @exception LoginException if the abort fails.
     *
     * @return false if this LoginModule's own login and/or commit attempts failed, and true otherwise.
     */
    @Override
    public boolean abort() throws LoginException {
        if (succeeded == false) {
            return false;
        }

        if (succeeded == true && commitSucceeded == false) {
            // Login succeeded but overall authentication failed
            succeeded = false;
            alias = null;
            userPrincipal = null;
        } else {
            // Overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }

        return true;
    }

    /**
     * Logout the user.
     *
     * <p>
     * This method removes the <code>UserPrincipal</code> that was added by the <code>commit</code>
     * method.
     *
     * <p>
     *
     * @exception LoginException if the logout fails.
     *
     * @return true in all cases since this <code>LoginModule</code> should not be ignored.
     */
    @Override
    public boolean logout() throws LoginException {
        // Unset the alias
        ssl = null;
        sslUtils.setAppclientSsl(ssl);
        subject.getPrincipals().remove(userPrincipal);
        succeeded = false;
        commitSucceeded = false;
        alias = null;
        userPrincipal = null;

        return true;
    }

    public static void setKeyStore(KeyStore keyStore) {
        ClientCertificateLoginModule.keyStore = keyStore;
    }

    private Callback createChoiceCallback(String[] choices) {
        return new ChoiceCallback(localStrings.getLocalString("login.certificate", "Choose from list of certificates: "), choices, 0, false);
    }
}
