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
// Portions Copyright [2018-2019] Payara Foundation and/or affiliates
package com.sun.enterprise.security.auth.realm;

import java.util.Enumeration;

import com.sun.enterprise.security.util.IASSecurityException;

/**
 * This class contains all abstract methods of a Payara Realm.
 *
 * @see Realm
 *
 * @author Arjan Tijms
 */
public abstract class AbstractRealm {

    // ---[ General methods ]------------------------------------------------

    /**
     * Returns a short (preferably less than fifteen characters) description of the kind of
     * authentication which is supported by this realm.
     *
     * @return description of the kind of authentication that is directly supported by this realm.
     */
    public abstract String getAuthType();

    /**
     * Returns names of all the users in this particular realm.
     *
     * @return enumeration of user names (strings)
     * @throws BadRealmException if realm data structures are bad
     */
    public abstract Enumeration<String> getUserNames() throws BadRealmException;

    /**
     * Returns the information recorded about a particular named user.
     *
     * @param name name of the user whose information is desired
     *
     * @return the user object
     * @throws NoSuchUserException if the user doesn't exist
     * @throws BadRealmException if realm data structures are bad
     */
    public abstract User getUser(String name) throws NoSuchUserException, BadRealmException;

    /**
     * Returns names of all the groups in this particular realm.
     *
     * @return enumeration of group names (strings)
     * @throws BadRealmException if realm data structures are bad
     */
    public abstract Enumeration<String> getGroupNames() throws BadRealmException;


    /**
     * Returns the name of all the groups that this user belongs to
     *
     * @param username name of the user in this realm whose group listing is needed.
     * @return enumeration of group names (strings)
     * @throws InvalidOperationException thrown if the realm does not support this operation
     * @throws NoSuchUserException
     */
    public abstract Enumeration<String> getGroupNames(String username) throws InvalidOperationException, NoSuchUserException;



    // ---[ User management methods ]------------------------------------------------


    /**
     * @return true if the realm implementation support User Management (add, update, remove user)
     */
    public abstract boolean supportsUserManagement();

    /**
     * Adds new user to file realm. User cannot exist already.
     *
     * @param name User name.
     * @param password Cleartext password for the user.
     * @param groupList List of groups to which user belongs.
     *
     * @throws BadRealmException If there are problems adding user.
     * @throws IASSecurityException
     *
     */
    public abstract void addUser(String name, char[] password, String[] groupList) throws BadRealmException, IASSecurityException;

    /**
     * Update data for an existing user. User must exist.
     *
     * @param name Current name of the user to update.
     * @param newName New name to give this user. It can be the same as the original name. Otherwise it
     * must be a new user name which does not already exist as a user.
     * @param password Cleartext password for the user. If non-null the user password is changed to this
     * value. If null, the original password is retained.
     * @param groups Array of groups to which user belongs.
     *
     * @throws BadRealmException If there are problems adding user.
     * @throws NoSuchUserException If user does not exist.
     * @throws IASSecurityException
     *
     */
    public abstract void updateUser(String name, String newName, char[] password, String[] groups) throws NoSuchUserException, BadRealmException, IASSecurityException;

    /**
     * Remove user from file realm. User must exist.
     *
     * @param name User name.
     *
     * @throws NoSuchUserException If user does not exist.
     * @throws BadRealmException
     *
     */
    public abstract void removeUser(String name) throws NoSuchUserException, BadRealmException;

    /**
     * Persist the realm data to permanent storage
     *
     * @throws com.sun.enterprise.security.auth.realm.BadRealmException
     */
    public abstract void persist() throws BadRealmException;

    /**
     * Refreshes the realm data so that new users/groups are visible.
     *
     * @throws BadRealmException if realm data structures are bad
     */
    public abstract void refresh() throws BadRealmException;

}
