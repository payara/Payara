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
// Portions Copyright [2018-2019] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security;

import java.util.Enumeration;

import com.sun.enterprise.security.auth.realm.BadRealmException;
import com.sun.enterprise.security.auth.realm.NoSuchUserException;
import com.sun.enterprise.security.auth.realm.Realm;
import com.sun.enterprise.security.auth.realm.User;
import com.sun.enterprise.security.util.IASSecurityException;
import com.sun.enterprise.util.i18n.StringManager;

/**
 * Parent class for Payara Realm classes.
 *
 * <P>
 * This class provides default implementation for most of the abstract methods in
 * com.sun.enterprise.security.auth.realm.Realm. Since most of these abstract methods are not supported by Realms there
 * is no need for the subclasses to implement them. The default implementations provided here generally throw an
 * exception if invoked.
 *
 * @author Harpreet Singh
 */
public abstract class BaseRealm extends Realm {

    protected static final StringManager sm = StringManager.getManager(Realm.class);
    private static final String NOT_SUPPORTED = sm.getString("iasrealm.notsupported");

    /**
     * Returns names of all the users in this particular realm.
     *
     * <P>
     * This method always throws a BadRealmException since by default this operation is not supported. Subclasses which
     * support this method can override.
     *
     * @return enumeration of user names (strings)
     * @exception com.sun.enterprise.security.auth.realm.BadRealmException if realm data structures are bad
     *
     */
    @Override
    public Enumeration<String> getUserNames() throws BadRealmException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Returns the information recorded about a particular named user.
     *
     * <P>
     * This method always throws a BadRealmException since by default this operation is not supported. Subclasses which
     * support this method can override.
     *
     * @param name name of the user whose information is desired
     * @return the user object
     * @exception com.sun.enterprise.security.auth.realm.NoSuchUserException if the user doesn't exist
     * @exception com.sun.enterprise.security.auth.realm.BadRealmException if realm data structures are bad
     *
     */
    @Override
    public User getUser(String name) throws NoSuchUserException, BadRealmException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Returns names of all the groups in this particular realm.
     *
     * <P>
     * This method always throws a BadRealmException since by default this operation is not supported. Subclasses which
     * support this method can override.
     *
     * @return enumeration of group names (strings)
     * @exception com.sun.enterprise.security.auth.realm.BadRealmException if realm data structures are bad
     *
     */
    @Override
    public Enumeration<String> getGroupNames() throws BadRealmException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * <P>
     * This method always throws a BadRealmException since by default this operation is not supported. Subclasses which
     * support this method can override.
     *
     * @exception com.sun.enterprise.security.auth.realm.BadRealmException if realm data structures are bad
     *
     */
    @Override
    public void refresh() throws BadRealmException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Adds new user to file realm. User cannot exist already.
     *
     * @param name User name.
     * @param password Cleartext password for the user.
     * @param groupList List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     *
     */
    @Override
    public void addUser(String name, char[] password, String[] groupList) throws BadRealmException, IASSecurityException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Adds new user to file realm. User cannot exist already.
     *
     */
    public void addUser(String name, String password, String[] groupList) throws BadRealmException, IASSecurityException {
        addUser(name, password.toCharArray(), groupList);

    }

    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    @Override
    public void removeUser(String name) throws NoSuchUserException, BadRealmException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * Update data for an existing user. User must exist.
     *
     */
    public void updateUser(String name, String newName, String password, String[] groups)
            throws NoSuchUserException, BadRealmException, IASSecurityException {
        updateUser(name, newName, (password == null) ? null : password.toCharArray(), groups);
    }

    /**
     * Update data for an existing user. User must exist.
     *
     * @param name Current name of the user to update.
     * @param newName New name to give this user. It can be the same as the original name. Otherwise it must be a new user
     * name which does not already exist as a user.
     * @param password Cleartext password for the user. If non-null the user password is changed to this value. If null, the
     * original password is retained.
     * @param groups List of groups to which user belongs.
     * @throws BadRealmException If there are problems adding user.
     * @throws NoSuchUserException If user does not exist.
     *
     */
    @Override
    public void updateUser(String name, String newName, char[] password, String[] groups) throws NoSuchUserException, BadRealmException, IASSecurityException {
        throw new BadRealmException(NOT_SUPPORTED);
    }

    /**
     * @return true if the realm implementation support User Management (add,remove,update user)
     */
    @Override
    public boolean supportsUserManagement() {
        return false;
    }

    /**
     * Persist the realm data to permanent storage
     *
     * @throws com.sun.enterprise.security.auth.realm.BadRealmException
     */
    @Override
    public void persist() throws BadRealmException {
        // NOOP for realms that do not support UserManagement
    }
}
