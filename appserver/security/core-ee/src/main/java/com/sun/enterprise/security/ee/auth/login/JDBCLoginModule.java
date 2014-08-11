/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;

import com.sun.enterprise.security.auth.login.PasswordLoginModule;
import com.sun.enterprise.security.auth.login.common.LoginException;
import com.sun.enterprise.security.auth.realm.jdbc.JDBCRealm;

import java.util.Arrays;


/**
 * This class implement a JDBC Login module for Glassfish. The work is derivated from Sun's sample JDBC login module.
 * Enhancement has been done to use latest features.
 * sample setting in server.xml for JDBCLoginModule
 * @author Jean-Baptiste Bugeaud
 */
public class JDBCLoginModule extends PasswordLoginModule {
    /**
     * Perform JDBC authentication. Delegates to JDBCRealm.
     *
     * @throws LoginException If login fails (JAAS login() behavior).
     */    
    protected void authenticate() throws LoginException {
        if (!(_currentRealm instanceof JDBCRealm)) {
            String msg = sm.getString("jdbclm.badrealm");
            throw new LoginException(msg);
        }
        
        final JDBCRealm jdbcRealm = (JDBCRealm)_currentRealm;

        // A JDBC user must have a name not null and non-empty.
        if ( (_username == null) || (_username.length() == 0) ) {
            String msg = sm.getString("jdbclm.nulluser");
            throw new LoginException(msg);
        }
        
        String[] grpList = jdbcRealm.authenticate(_username, getPasswordChar());

        if (grpList == null) {  // JAAS behavior
            String msg = sm.getString("jdbclm.loginfail", _username);
            throw new LoginException(msg);
        }

        if (_logger.isLoggable(Level.FINEST)) {
            _logger.finest("JDBC login succeeded for: " + _username
                + " groups:" + Arrays.toString(grpList));
        }

        commitAuthentication(_username, getPasswordChar(),
                             _currentRealm, grpList);
    }
}
