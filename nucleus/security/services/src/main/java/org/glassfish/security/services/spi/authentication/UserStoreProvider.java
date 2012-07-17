/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.security.services.spi.authentication;

import java.util.Set;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.glassfish.security.services.api.common.Attributes;

/*
 * Provider interface for authentication service.
 */

public interface UserStoreProvider {
	
	/*
	 * Represents a user entry in the store
	 */
	public interface UserEntry {
		public String getName();
		public String getDN();
		public String getUid();
		public String getStoreId();
		public Set<GroupEntry> getGroups();
		public Attributes getAttributes();
	}
	
	/*
	 * Represents a group entry in the store
	 */
	public interface GroupEntry {
		public String getName();
		public String getDN();
		public String getUid();
		public String getStoreId();
		public Set<String> getMembers();
	}
	
	/*
	 * Page-able Result Set
	 */
	public interface ResultSet<T> {
		public boolean hasNext();
		public T getNext();
		public void close();
	}

	/**
	 * Get the unique store ID for this user store.  This value must be unique across all
	 * stores configured into the system or which might be propogated into the system via
	 * SSO, etc.  If this USP aggregates multiple underlying stores, the user IDs returned
	 * by the provider must be sufficient to uniquely identify users across all of the
	 * underlying stores.
	 * 
	 * @return The store ID for this USP.
	 */
	public String getStoreId();
	
	/**
	 * Determine if authentication is supported and enabled by this USP.
	 * 
	 * @return True or false.
	 */
	public boolean isAuthenticationEnabled();
	
	/**
	 * Determine if user lookup is supported and enabled by this USP.
	 * 
	 * @return True or false.
	 */
	public boolean isUserLookupEnabled();
	
	/**
	 * Determine if user update (CRUD operations) is supported and enabled by this USP.
	 * 
	 * @return True or false.
	 */
	public boolean isUserUpdateEnabled();

	/**
	 * Authenticate using credentials supplied in the given CallbackHandler.  All USPs must support
	 * at least NameCallback and PasswordCallback.  The only other callback type expected to be commonly
	 * used is X509Certificate, but it's possible to imagine, e.g., KerberosToken or PasswordDigest.
	 * 
	 * @param cbh
	 * @param isGetGroups Whether or not to return the user's groups.
	 * @param attributeNames Names of attributes to return, or null for no attributes.
	 * @return If successful, a UserEntry representing the authenticated user, otherwise throws an exception.
	 * @throws LoginException
	 */
	public UserEntry authenticate(CallbackHandler cbh, boolean isGetGroups, Set<String> attributeNames) throws LoginException;
	
	/*
	 * User Lookup
	 */
	
	/**
	 * Lookup users by name.  Since name is not necessarily unique, more than one entry may be returned.
	 * Group membership and selected attributes can also be requested, but requesting these may be inefficient
	 * if more than one user is matched.
	 * 
	 * @param name The user name to searech for.
	 * @param isGetGroups Whether or not to return users' groups.
	 * @param attributeNames Names of attributes to return, or null for no attributes.
	 * @return The Set of UserEntrys found.
	 * 
	 * @throws UserStoreException
	 */
	public ResultSet<UserEntry> lookupUsersByName(String name, boolean isGetGroups, Set<String> attributeNames) throws UserStoreException;
	
	/**
	 * Lookup a user by unique ID.  Returns the corresponding UserEntry if found.
	 * Group membership and selected attributes can also be requested.
	 * 
	 * @param uid
	 * @param isGetGroups Whether or not to return users' groups.
	 * @param attributeNames Names of attributes to return, or null for no attributes.
	 * @return The UserEntry (if found).
	 * 
	 * @throws UserStoreException
	 */
	public UserEntry lookupUserByUid(String uid, boolean isGetGroups, Set<String> attributeNames) throws UserStoreException;
	
	/*
	 * Group Lookup
	 */

	/**
	 * Get the GroupEntry(s) for the specified group name.
	 * 
	 * @param name The name to search on, may include wildcards (e.g., a*, *b, etc.)
	 * @return ResultSet of the GroupEntries matching the specified name.
	 * 
	 * @throws UserStoreException
	 */
	public ResultSet<GroupEntry> lookupGroupsByName(String name) throws UserStoreException;

	/**
	 * Get the GroupEntry for the specified group.
	 * 
	 * @param uid The UID of the group to return.
	 * @return GroupEntry corresponding to the group UID.
	 * 
	 * @throws UserStoreException
	 */
	public GroupEntry lookupGroupByUid(String uid) throws UserStoreException;
	
	/*
	 * User CRUD
	 */
	
	/**
	 * Create a new user and return the unique ID assigned.
	 * 
	 * @param name Name of the new user entry.
	 * @param pwd Password to set on the new entry.
	 * @param attributes Attributes to set on the entry (or null if none).
	 * @return Returns the UID assigned to the new entry (can be used for subsequent operations)
	 * 
	 * @throws UserStoreException
	 */
	public String /*uid*/ createUser(String name, char[] pwd, Attributes attributes) throws UserStoreException;
	
	/**
	 * Remove the specified user.
	 * 
	 * @param uid UID of the user to remove.
	 * 
	 * @throws UserStoreException
	 */
	public void deleteUser(String uid) throws UserStoreException;
	
	/**
	 * Change the password for the specified user.  If old password is provided, verify before changing.
	 * 
	 * @param uid UID of user whose password should be changed.
	 * @param oldPwd Old password, if verification desired, or null.  If provided, must be valid.
	 * @param newPwd New password to set.
	 * 
	 * @throws UserStoreException
	 */
	public void changePassword(String uid, char[] oldPwd, char[] newPwd) throws UserStoreException;  // setPassword(String uid, char[] pwd)?  password reset?
	
	/**
	 * Add the given attribute values to the user entry.
	 * 
	 * @param uid
	 * @param attributes
	 * @param replace
	 * @throws UserStoreException
	 */
	public void addAttributeValues(String uid, Attributes attributes, boolean replace) throws UserStoreException;
	
	/**
	 * Remove the given attribute values from the user entry.
	 * 
	 * @param uid
	 * @param attributes
	 * @throws UserStoreException
	 */
	public void removeAttributeValues(String uid, Attributes attributes) throws UserStoreException;
	
	/**
	 * Remove the given attributes from the user entry.
	 * 
	 * @param uid
	 * @param attributeNames
	 * @throws UserStoreException
	 */
	public void removeAttributes(String uid, Set<String> attributeNames) throws UserStoreException;
	
	/*
	 * Group CRUD
	 */
	
	/**
	 * Create a new group.
	 * 
	 * @param groupName
	 * @return The UID for the newly created group
	 * @throws UserStoreException
	 */
	public String /*uid*/ createGroup(String groupName) throws UserStoreException;
	
	/**
	 * Delete a group.
	 * 
	 * @param uid UID of group to delete.
	 * @throws UserStoreException
	 */
	public void deleteGroup(String uid) throws UserStoreException;
	
	/**
	 * Add the specified user to the set of groups.
	 * 
	 * @param uid
	 * @param groups
	 * @throws UserStoreException
	 */
	public void addUserToGroups(String uid, Set<String> groups) throws UserStoreException;
	
	/**
	 * Remove the specified user from the set of groups.
	 * 
	 * @param uid
	 * @param groups
	 * @throws UserStoreException
	 */
	public void removeUserFromGroups(String uid, Set<String> groups) throws UserStoreException;
	
	/**
	 * Add the set of users to the specified group.
	 * 
	 * @param uids
	 * @param group
	 * @throws UserStoreException
	 */
	public void addUsersToGroup(Set<String> uids, String group) throws UserStoreException;
	
	/**
	 * Remove the set of users from the specified group.
	 * 
	 * @param uids
	 * @param group
	 * @throws UserStoreException
	 */
	public void removeUsersFromGroup(Set<String> uids, String group) throws UserStoreException;

}
