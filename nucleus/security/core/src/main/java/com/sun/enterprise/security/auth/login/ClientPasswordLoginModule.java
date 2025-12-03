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

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINEST;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.glassfish.security.common.UserNameAndPassword;
import org.glassfish.security.common.UserPrincipal;


import com.sun.enterprise.security.SecurityLoggerInfo;
import com.sun.enterprise.security.UsernamePasswordStore;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * <p>
 * This client LoginModule obtains username/password credentials from TLS, a static variable, system properties or by asking the user for it
 * interactively.
 *
 * <p>
 * The obtained credentials are then merely stored into the subject, meaning this login module doesn't actually authenticate anything. It only
 * moves credentials from a credential source to the subject. This is then used by for instance ProgrammaticLogin to store that subject into
 * a security context. IIOP (Remote EJB code) can then fetch the credentials again from this security context and transfer them to a remote
 * server, where actual authentication takes place.
 *
 * @author Harpreet Singh (harpreet.singh@sun.com)
 */
public class ClientPasswordLoginModule implements LoginModule {

    private static final Logger _logger = SecurityLoggerInfo.getLogger();
    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ClientPasswordLoginModule.class);

    public static final String LOGIN_NAME = "j2eelogin.name";
    public static final String LOGIN_PASSWORD = "j2eelogin.password";
    private static final String DEFAULT_REALMNAME = "default";

    // Initial state
    private Subject subject;
    private CallbackHandler callbackHandler;

    // Username and password, aka "the credentials", as obtained from one of the supported sources
    private String username;
    private char[] password;

    // The authentication status
    private boolean succeeded;
    private boolean commitSucceeded;

    // The principal set when authentication succeeds. We don't really know why this is an instance variable.
    private UserPrincipal userPrincipal;


    /**
     * Initialize this <code>LoginModule</code>.
     *
     * <p>
     *
     * @param subject the <code>Subject</code> in which the credentials will be stored if obtained successfully.
     * <p>
     *
     * @param callbackHandler a <code>CallbackHandler</code> for communicating with the end user
     * (prompting for usernames and passwords, for example).
     * <p>
     *
     * @param sharedState shared <code>LoginModule</code> state (unused)
     * <p>
     *
     * @param options options specified in the login <code>Configuration</code> for this particular
     * <code>LoginModule</code> (unused)
     */
    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String,?> sharedState, Map<String,?> options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    /**
     * Attempt to obtain non-null credentials from various sources. If non-null one are obtained, store them in the Subject.
     *
     * <p>
     *
     * @return true in all cases since this <code>LoginModule</code> should not be ignored.
     *
     * @exception FailedLoginException if the authentication fails.
     * <p>
     *
     * @exception LoginException if this <code>LoginModule</code> is unable to perform the
     * "authentication".
     */
    @Override
    public boolean login() throws LoginException {
        callbackHandlerNonNull();

        // Try to get the username and password from the exchange mechanism, which is either TLS or an global variable
        String incomingUsername = UsernamePasswordStore.getUsername();
        char[] incomingPassword = UsernamePasswordStore.getPassword();

        // Try to get the username and password from system properties if not provided by UsernamePasswordStore

        boolean doSet = false;
        if (incomingUsername == null) {
            incomingUsername = System.getProperty(LOGIN_NAME);
            doSet = true;
        }
        if (incomingPassword == null) {
            if (System.getProperty(LOGIN_PASSWORD) != null) {
                incomingPassword = System.getProperty(LOGIN_PASSWORD).toCharArray();
            }
            doSet = true;
        }

        if (doSet) {
            UsernamePasswordStore.set(incomingUsername, incomingPassword);
        }

        if (incomingUsername != null && incomingPassword != null) {

            // Credentials have been provided. All we need to do is store them

            username = incomingUsername;
            password = copyPassword(incomingPassword);
        } else {

            // Credentials have not been provided. Ask the user for them.


            Callback[] callbacks = new Callback[] { createNameCallback(), createPasswordCallback() };

            try {
                callbackHandler.handle(callbacks);

                username = ((NameCallback) callbacks[0]).getName();
                usernameNonNull();

                char[] tmpPassword = ((PasswordCallback) callbacks[1]).getPassword();
                if (tmpPassword == null) {
                    // treat a NULL password as an empty password
                    tmpPassword = new char[0];
                }
                password = copyPassword(tmpPassword);

                ((PasswordCallback) callbacks[1]).clearPassword();

            } catch (IOException ioe) {
                throw new LoginException(ioe.toString());
            } catch (UnsupportedCallbackException uce) {
                throw new LoginException(
                        localStrings.getLocalString(
                                "login.callback",
                                "Error: Callback not available to garner authentication information from user(CallbackName):") +
                        uce.getCallback().toString());
            }
        }

        // By default - the client side login module will always say that the login successful.
        // The actual login will take place on the server side.

        _logger.log(FINEST, "\t\t[ClientPasswordLoginModule] " + "authentication succeeded");
        succeeded = true;

        return true;
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

        // 1. Add a Principal (authenticated identity) to the Subject

        // Assume the user we authenticated is the UserPrincipal
        userPrincipal = new UserNameAndPassword(username);
        if (!subject.getPrincipals().contains(userPrincipal)) {
            subject.getPrincipals().add(userPrincipal);
        }

        _logger.log(FINE, "\t\t[ClientPasswordLoginModule] " + "added UserPrincipal to Subject");

        String realm = DEFAULT_REALMNAME;

        // 2. Add a PasswordCredential (containing the same username as the Principal) to the Subject

        PasswordCredential passwordCredential = new PasswordCredential(username, password, realm);
        if (!subject.getPrivateCredentials().contains(passwordCredential)) {
            subject.getPrivateCredentials().add(passwordCredential);
        }

        // 3. In any case, clean out state
        username = null;
        for (int i = 0; i < password.length; i++) {
            password[i] = ' ';
        }
        password = null;
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
            // login succeeded but overall authentication failed
            succeeded = false;
            username = null;
            if (password != null) {
                for (int i = 0; i < password.length; i++) {
                    password[i] = ' ';
                }
                password = null;
            }
            userPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded,
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

        subject.getPrincipals().remove(userPrincipal);
        succeeded = commitSucceeded;
        username = null;
        if (password != null) {
            for (int i = 0; i < password.length; i++) {
                password[i] = ' ';
            }
            password = null;
        }
        userPrincipal = null;
        return true;
    }

    private void callbackHandlerNonNull() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException(
                localStrings.getLocalString(
                        "login.nocallback",
                        "Error: no CallbackHandler available to garner authentication information from the user"));
        }
    }

    private void usernameNonNull() throws LoginException {
        if (username == null) {
            throw new LoginException(localStrings.getLocalString("login.nousername", "No user specified"));
        }
    }

    private static char[] copyPassword(char[] incomingPassword) {
        char[]copy = new char[incomingPassword.length];
        System.arraycopy(incomingPassword, 0, copy, 0, incomingPassword.length);
        return copy;
    }

    private Callback createNameCallback() {
        NameCallback nameCallback = new NameCallback(localStrings.getLocalString("login.username", "ClientPasswordModule username"));

        String defaultUname = System.getProperty("user.name");
        if (defaultUname != null) {
            nameCallback = new NameCallback(localStrings.getLocalString("login.username", "ClientPasswordModule username"), defaultUname);
        }

        return nameCallback;
    }

    private Callback createPasswordCallback() {
        return new PasswordCallback(localStrings.getLocalString("login.password", "ClientPasswordModule password: "), false);
    }
}
