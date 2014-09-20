/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.security.AppservPasswordLoginModule;
import com.sun.enterprise.security.auth.realm.pam.PamRealm;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.login.LoginException;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;
import org.jvnet.libpam.UnixUser;

/**
 * This is the main LoginModule for PAM realm that invokes the calls to libpam4j
 * classes to authenticate the given username and password
 * @author Nithya Subramanian
 */
public class PamLoginModule extends AppservPasswordLoginModule {

    protected void authenticateUser() throws LoginException {

        // A Unix user must have a name not null so check here.
        if ((_username == null) || (_username.length() == 0)) {
            throw new LoginException("Invalid Username");
        }
        UnixUser user = authenticate(_username, _password);

        if (user == null) {  // JAAS behavior
            throw new LoginException("Failed Pam Login for " + _username);
        }
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "PAM login succeeded for: " + _username);
        }
        
        /*
         * Get the groups from the libpam4j UnixUser class that has been 
         * returned after a successful authentication.
         */

        String[] grpList = null;
        Set<String> groupSet = user.getGroups();
        
        if (groupSet != null) {
            grpList = new String[groupSet.size()];
            user.getGroups().toArray(grpList);
        }
        else {
            //Empty group list, create a zero-length group list
             grpList = new String[0];
        }
        commitUserAuthentication(grpList);
    }

    /**
     * Invokes the  authentication call.This class uses the default PAM service
     * - sshd
     * @param username OS User to authenticate.
     * @param password Given password.
     * @returns null if authentication failed,
     * returns the UnixUser object if authentication succeeded.
     *
     */
    private UnixUser authenticate(String username, String password) throws LoginException {
        UnixUser user = null;
        String pamService = null;

        if(_currentRealm instanceof PamRealm) {
            pamService = ((PamRealm)_currentRealm).getPamService();
        }
        else {
            throw new LoginException("pamrealm.invalid_realm");
        }
       
        try {
            user = new PAM(pamService).authenticate(username, password);

        } catch (PAMException e) {
              _logger.log(Level.SEVERE, "pam_exception_authenticate", e);
        }
        return user;
    }
}


