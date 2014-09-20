/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ee.auth.login;

import com.sun.enterprise.security.PrincipalGroupFactory;
import com.sun.enterprise.security.auth.realm.NoSuchRealmException;
import com.sun.logging.LogDomains;
import java.util.Map;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.security.common.Group;
import com.sun.enterprise.security.auth.digest.api.DigestAlgorithmParameter;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.ee.auth.realm.DigestRealm;
import java.util.Enumeration;


/**
 *
 * @author K.Venugopal@sun.com
 */
public abstract class DigestLoginModule implements LoginModule {

    private Subject subject = null;
    private CallbackHandler handler = null;
    private Map<String, ?> sharedState = null;
    private Map<String, ?> options = null;
    protected static final Logger _logger = LogDomains.getLogger(DigestLoginModule.class, LogDomains.SECURITY_LOGGER);
    protected boolean _succeeded = false;
    protected boolean _commitSucceeded = false;
    protected PrincipalImpl _userPrincipal;
    private DigestCredentials digestCredentials = null;
    private Realm _realm;

    public DigestLoginModule() {
    }

    public final void initialize(Subject subject, CallbackHandler handler, Map<String, ?> sharedState, Map<String, ?> options) {
        this.subject = subject;
        this.handler = handler;
        this.sharedState = sharedState;
        this.options = options;
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Login module initialized: " + this.getClass().toString());
        }
    }

    public final boolean login() throws LoginException {
        Set<Object> creds = this.subject.getPrivateCredentials();
        Iterator<Object> itr = creds.iterator();
        while (itr.hasNext()) {
            Object obj = itr.next();
            if (obj instanceof DigestCredentials) {
                digestCredentials = (DigestCredentials) obj;
                break;
            } else if (obj instanceof com.sun.enterprise.security.auth.login.DigestCredentials) {
                com.sun.enterprise.security.auth.login.DigestCredentials dc = (com.sun.enterprise.security.auth.login.DigestCredentials) obj;
                digestCredentials = new DigestCredentials(dc.getRealmName(), 
                        dc.getUserName(), dc.getParameters());
            }
        }
        if (digestCredentials == null) {
            throw new LoginException();
        }
        DigestAlgorithmParameter [] params = digestCredentials.getParameters();
        String username = digestCredentials.getUserName();
        try {
            _realm = Realm.getInstance(digestCredentials.getRealmName());
        } catch (NoSuchRealmException ex) {
            _logger.log(Level.FINE,"",ex);
            _logger.log(Level.SEVERE,"no.realm",digestCredentials.getRealmName());
            throw new LoginException(ex.getMessage());
        }
            if (_realm instanceof DigestRealm){
                if(((DigestRealm)_realm).validate(username, params)) {
                //change to pass Password Validator
                _succeeded = true;
                }
            }else{
                _logger.log(Level.SEVERE,"digest.realm",digestCredentials.getRealmName());
                throw new LoginException("Realm" + digestCredentials.getRealmName()+ " does not support Digest validation");                
            }

        return _succeeded;
    }

    public final boolean commit() throws LoginException {
        
            if (!_succeeded) {
                _commitSucceeded = false;
                return false;
            }

            PrincipalGroupFactory factory = Globals.getDefaultHabitat().getService(PrincipalGroupFactory.class);
            _userPrincipal = factory.getPrincipalInstance(digestCredentials.getUserName(), digestCredentials.getRealmName());
            java.util.Set principalSet = this.subject.getPrincipals();
            if (!principalSet.contains(_userPrincipal)) {
                principalSet.add(_userPrincipal);
            }
            java.util.Enumeration groupsList = getGroups(digestCredentials.getUserName());
            while (groupsList.hasMoreElements()) {
                java.lang.String value = (java.lang.String) groupsList.nextElement();
                Group g = factory.getGroupInstance(value, digestCredentials.getRealmName());
                if (!principalSet.contains(g)) {
                    principalSet.add(g);
                }
                // cleaning the slate
            }

            return true;
        
	
    }

    public final boolean abort() throws LoginException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "JAAS authentication aborted.");
        }

        if (_succeeded == false) {
            return false;
        } else if (_succeeded == true && _commitSucceeded == false) {
            // login succeeded but overall authentication failed
            _succeeded = false;


          
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    public final boolean logout() throws LoginException {
        subject.getPrincipals().clear();
        subject.getPublicCredentials().clear();
        subject.getPrivateCredentials().clear();

        _succeeded = false;
        _commitSucceeded = false;
       
        return true;
    }

    protected Realm getRealm() {
        return _realm;
    }

    protected abstract Enumeration getGroups(String username);

    
}
