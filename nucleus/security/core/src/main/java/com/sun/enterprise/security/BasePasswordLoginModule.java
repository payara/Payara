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
package com.sun.enterprise.security;

import static com.sun.enterprise.security.SecurityLoggerInfo.noPwdCredentialProvidedError;
import static com.sun.enterprise.security.SecurityLoggerInfo.privateSubjectCredentialsError;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.security.Principal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.glassfish.security.common.UserNameAndPassword;
import org.glassfish.security.common.UserPrincipal;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.Group;


import com.sun.enterprise.security.auth.login.LoginCallbackHandler;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * Abstract base class for password-based login modules.
 *
 * <P>
 * Most login modules receive a username and password from the client (possibly through HTTP BASIC auth, or FORM, or
 * other mechanism) and then make (or delegate) an authentication decision based on this data. This class provides
 * common methods for such password-based login modules.
 *
 * <P>
 * Subclasses need to implement the authenticateUser() method and later call commitUserAuthentication().
 *
 */
public abstract class BasePasswordLoginModule implements LoginModule {
    
    protected static final Logger LOGGER = SecurityLoggerInfo.getLogger();
    protected static final StringManager sm = StringManager.getManager(LoginCallbackHandler.class);

    // The _subject, _sharedState and _options satisfy LoginModule and are
    // shared across sub-classes
    protected Subject _subject;
    protected Map _sharedState;
    protected Map _options;
    protected String _username;
    protected String _password;
    protected char[] _passwd;
    protected Realm _currentRealm;

    // the authentication status
    protected boolean _succeeded;
    protected boolean _commitSucceeded;
    protected UserPrincipal _userPrincipal;
    protected String[] _groupsList;

    /**
     * Initialize this login module.
     *
     * @param subject - the Subject to be authenticated.
     * @param callbackHandler - a CallbackHandler for obtaining the subject username and password.
     * @param sharedState - state shared with other configured LoginModules.
     * @param options - options specified in the login Configuration for this particular LoginModule.
     *
     */
    @Override
    public final void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        _subject = subject;
        _sharedState = sharedState;
        _options = options;
        
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "Login module initialized: " + getClass());
        }
    }

    /**
     * Perform login.
     *
     * <P>
     * The callback handler is used to obtain authentication info for the subject and a login is attempted. This
     * PasswordLoginModule expects to find a PasswordCredential in the private credentials of the Subject. If not present
     * the login fails. The callback handler is ignored as it is not really relevant on the server side. Finally, the
     * authenticateUser() method is invoked.
     *
     * @returns true if login succeeds, otherwise an exception is thrown.
     * @throws LoginException Thrown if login failed, or on other problems.
     *
     */
    @Override
    public final boolean login() throws LoginException {
        // Extract the username and password
        extractCredentials();

        // Delegate the actual authentication to subclass.
        authenticateUser();
        LOGGER.fine("JAAS login complete.");
        
        return true;
    }

    /**
     * Commit the authentication.
     *
     * <P>
     * Commit is called after all necessary login modules have succeeded. It adds (if not present) a PrincipalImpl principal
     * and a LocalCredentials public credential to the Subject.
     *
     * @throws LoginException If commit fails.
     *
     */
    @Override
    public boolean commit() throws LoginException {
        if (!_succeeded) {
            return false;
        }

        // Add a Principal (authenticated identity) to the Subject
        // Assume the user we authenticated is the UserPrincipal [RI]
        String realmName = _currentRealm.getName();
        PrincipalGroupFactory factory = Globals.getDefaultHabitat().getService(PrincipalGroupFactory.class);
        if (factory != null) {
            _userPrincipal = factory.getPrincipalInstance(getUsername(), realmName);
        } else {
            _userPrincipal = new UserNameAndPassword(getUsername());
        }

        Set<Principal> principals = _subject.getPrincipals();
        if (!principals.contains(_userPrincipal)) {
            principals.add(_userPrincipal);
        }
        
        // populate the group in the subject and clean out the slate at the same time
        
        for (int i = 0; i < _groupsList.length; i++) {
            if (_groupsList[i] != null) {
                Group group;
                if (factory != null) {
                    group = factory.getGroupInstance(_groupsList[i], realmName);
                } else {
                    group = new Group(_groupsList[i]);
                }

                if (!principals.contains(group)) {
                    principals.add(group);
                }

                // cleaning the slate
                _groupsList[i] = null;
            }
        }

        // In any case, clean out state.
        _groupsList = null;
        setUsername(null);
        setPassword(null);
        setPasswordChar(null);
        _commitSucceeded = true;
        
        LOGGER.fine("JAAS authentication committed.");
        
        return true;
    }

    /**
     * Abort the authentication process.
     *
     */
    @Override
    public final boolean abort() throws LoginException {
        LOGGER.fine("JAAS authentication aborted.");

        if (!_succeeded) {
            return false;
        }
        
        if (!_commitSucceeded) {
            // login succeeded but overall authentication failed
            _succeeded = false;
            setUsername(null);
            setPassword(null);
            setPasswordChar(null);
            _userPrincipal = null;
            for (int i = 0; i < _groupsList.length; i++) {
                _groupsList[i] = null;
            }
            _groupsList = null;
        } else {
            // Overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        
        return true;
    }

    /**
     * Log out the subject.
     *
     */
    @Override
    public final boolean logout() throws LoginException {
        if (LOGGER.isLoggable(FINE)) {
            LOGGER.log(FINE, "JAAS logout for: " + _subject);
        }

        _subject.getPrincipals().clear();
        _subject.getPublicCredentials().clear();
        _subject.getPrivateCredentials().clear();

        _succeeded = false;
        _commitSucceeded = false;
        setUsername(null);
        setPassword(null);
        _userPrincipal = null;
        
        if (_groupsList != null) {
            for (int i = 0; i < _groupsList.length; i++) {
                _groupsList[i] = null;
            }
            _groupsList = null;
        }
        
        return true;
    }

    /**
     *
     * <P>
     * This is a convenience method which can be used by subclasses
     *
     * <P>
     * Note that this method is called after the authentication has succeeded. If authentication failed do not call this
     * method.
     *
     * Global instance field succeeded is set to true by this method.
     *
     * @param groups String array of group memberships for user (could be empty).
     */
    public final void commitUserAuthentication(final String[] groups) {
        // Copy the groups into a new array before storing it in the instance
        String[] groupsListCopy = (groups == null) ? null : Arrays.copyOf(groups, groups.length);

        _groupsList = groupsListCopy;
        _succeeded = true;
    }

    /**
     * @return the subject being authenticated. use case: A custom login module could overwrite commit() method, and call
     * getSubject() to get subject being authenticated inside its commit(). Custom principal then can be added to subject.
     * By doing this,custom principal will be stored in calling thread's security context and participate in following
     * Appserver's authorization.
     *
     */
    public Subject getSubject() {
        return _subject;
    }

    /**
     * Method to extract container-provided username and password
     *
     * @throws javax.security.auth.login.LoginException
     */
    public final void extractCredentials() throws LoginException {
        if (_subject == null) {
            String msg = sm.getString("pwdlm.noinfo");
            LOGGER.log(SEVERE, msg);
            throw new LoginException(msg);
        }

        PasswordCredential passwordCredential = null;

        try {
            Iterator<Object> privateCredentials = _subject.getPrivateCredentials().iterator();
            while (privateCredentials.hasNext() && passwordCredential == null) {
                Object privateCredential = privateCredentials.next();
                if (privateCredential instanceof PasswordCredential) {
                    passwordCredential = (PasswordCredential) privateCredential;
                }
            }
        } catch (Exception e) {
            LOGGER.log(WARNING, privateSubjectCredentialsError, e.toString());
        }

        if (passwordCredential == null) {
            LOGGER.log(SEVERE, noPwdCredentialProvidedError);
            throw new LoginException(sm.getString("pwdlm.nocreds"));
        }

        // Need to obtain the requested realm to get parameters.

        String realm = null;
        try {
            realm = passwordCredential.getRealm();
            _currentRealm = Realm.getInstance(realm);

        } catch (Exception e) {
            String msg = sm.getString("pwdlm.norealm", realm);
            LOGGER.log(Level.SEVERE, msg);
            throw new LoginException(msg);
        }

        // Get username and password data from credential (ignore callback)

        setUsername(passwordCredential.getUser());
        setPasswordChar(passwordCredential.getPassword());
        setPassword(new String(passwordCredential.getPassword()));
    }

    /**
     * Perform authentication decision.
     *
     * Method returns silently on success and returns a LoginException on failure.
     *
     * @throws LoginException on authentication failure.
     *
     */
    protected abstract void authenticateUser() throws LoginException;

    public void setLoginModuleForAuthentication(LoginModule userDefinedLoginModule) {
    }

    /**
     * @return the username sent by container - is made available to the custom login module using the protected _username
     * field. Use Case: A custom login module could use the username to validate against a realm of users
     */

    public String getUsername() {
        return _username;
    }

    /**
     * Used for setting the username obtained from the container internally, to be made available to the custom login module
     * implementation
     *
     * @param username
     */
    private void setUsername(String username) {
        this._username = username;
    }

    /**
     * Deprecated - password is preferred to be a char[]
     */
    public String getPassword() {
        return _password;
    }

    /**
     * Deprecated - password is preferred to be a char[]
     */
    private void setPassword(String password) {
        this._password = password;
    }

    /**
     * @return the password sent by container - is made available to the custom login module using the protected _password
     * field. Use Case: A custom login module could use the password to validate against a custom realm of usernames and
     * passwords Password is preferred to be a char[] instead of a string
     */

    public char[] getPasswordChar() {
        return Arrays.copyOf(_passwd, _passwd.length);
    }

    /**
     * Used for setting the password obtained from the container internally, to be made available to the custom login module
     * implementation Password is preferred to be a char[] instead of a string
     *
     * @param password
     */
    private void setPasswordChar(char[] password) {
        this._passwd = password;
    }

    /**
     * @return the currentRealm - for backward compatibility
     */
    public Realm getCurrentRealm() {
        return _currentRealm;
    }

    /**
     * @return the succeeded state - for backward compatibility
     */
    public boolean isSucceeded() {
        return _succeeded;
    }

    /**
     * @return the commitsucceeded state - for backward compatibility
     */
    public boolean isCommitSucceeded() {
        return _commitSucceeded;
    }

    /**
     * @return the UserPrincipal - for backward compatibility
     */
    public UserPrincipal getUserPrincipal() {
        return _userPrincipal;
    }

    /**
     * @return the groupList - for backward compatibility
     */
    public String[] getGroupsList() {
        return Arrays.copyOf(_groupsList, _groupsList.length);
    }

    public <T> T getRealm(Class<T> realmClazz, String exceptionMsgKey) throws LoginException {
        if (!realmClazz.isAssignableFrom(_currentRealm.getClass())) {
            throw new LoginException(sm.getString(exceptionMsgKey));
        }

        return realmClazz.cast(_currentRealm);
    }
}
