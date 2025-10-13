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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.auth.login;

import static com.sun.enterprise.security.auth.realm.ldap.LDAPRealm.MODE_FIND_BIND;
import static com.sun.enterprise.security.auth.realm.ldap.LDAPRealm.PARAM_MODE;

import javax.security.auth.login.LoginException;

import com.sun.enterprise.security.BasePasswordLoginModule;
import com.sun.enterprise.security.auth.realm.ldap.LDAPRealm;

/**
 * Payara JAAS LoginModule for an LDAP Realm.
 *
 * <P>
 * Refer to the LDAPRealm documentation for necessary and optional configuration parameters for the Payara LDAP login
 * support.
 *
 * <P>
 * There are various ways in which a user can be authenticated using an LDAP directory. Currently this login module only
 * supports one mode, 'find and bind'. Other modes may be added as schedules permit.
 *
 * <P>
 * Mode: <i>find-bind</i>
 * <ol>
 *   <li>An LDAP search is issued on the directory starting at base-dn with the given search-filter (having substituted
 *   the user name in place of %s). If no entries match this search, login fails and authentication is over.
 *   <li>The DN of the entry which matched the search as the DN of the user in the directory. If the search-filter is
 *   properly set there should always be a single match; if there are multiple matches, the first one found is used.
 *   <li>Next an LDAP bind is attempted using the above DN and the provided password. If this fails, login is considered
 *   to have failed and authentication is over.
 *   <li>Then an LDAP search is issued on the directory starting at group-base-dn with the given group-search-filter
 *   (having substituted %d for the user DN previously found). From the matched entry(ies) all the values of group-target
 *   are taken as group names in which the user has membership. If no entries are found, the group membership is empty.
 * </ol>
 *
 *
 */
public class LDAPLoginModule extends BasePasswordLoginModule {

    /**
     * Perform LDAP authentication. Delegates to LDAPRealm.
     *
     * @throws LoginException If login fails (JAAS login() behavior).
     */
    @Override
    protected void authenticateUser() throws LoginException {
        LDAPRealm ldapRealm = getRealm(LDAPRealm.class, "ldaplm.badrealm");

        // Enforce that password cannot be empty.
        // ldap may grant login on empty password!
        if (getPasswordChar() == null || getPasswordChar().length == 0) {
            throw new LoginException(sm.getString("ldaplm.emptypassword", _username));
        }

        String mode = ldapRealm.getProperty(PARAM_MODE);

        if (!MODE_FIND_BIND.equals(mode)) {
            throw new LoginException(sm.getString("ldaplm.badmode", mode));
        }

        String[] groups = ldapRealm.findAndBind(_username, getPasswordChar());

        commitUserAuthentication(groups);
    }
}
