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

package com.sun.enterprise.security.jacc.provider;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.MBeanPermission;
import javax.security.jacc.EJBRoleRefPermission;
import javax.security.jacc.PolicyConfiguration;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;
import javax.security.jacc.WebResourcePermission;
import javax.security.jacc.WebRoleRefPermission;
import javax.security.jacc.WebUserDataPermission;

/**
 *
 * @author monzillo
 *
 * The methods of this interface are used by containers to
 * create policy statements in a Policy provider.
 * An object that implements the PolicyConfiguration interface provides the
 * policy statement configuration interface for a corresponding policy context
 * within the corresponding Policy provider.
 *
 */
public class SimplePolicyConfiguration implements PolicyConfiguration {

    public static final int OPEN_STATE = 0;
    public static final int INSERVICE_STATE = 2;
    public static final int DELETED_STATE = 3;
    private static final Permission setPolicyPermission =
            new java.security.SecurityPermission("setPolicy");
    private String id;
    private int state = OPEN_STATE;

    // Excluded permissions
    private PermissionCollection excludedPermissions = null;
    // Unchecked permissions
    private PermissionCollection uncheckedPermissions = null;
    // roleTbale mapps permissions and principals to roles.
    private ArrayList<Role> roleTable = null;
    // Lock on this PolicyConfiguration onject
    private ReentrantReadWriteLock pcLock = new ReentrantReadWriteLock(true);
    private Lock pcrLock = pcLock.readLock();
    private Lock pcwLock = pcLock.writeLock();

    static {
        // register a role mapper 
        try {
            String className = System.getProperty(JACCRoleMapper.CLASS_NAME);
            if (className != null ||
                    !PolicyContext.getHandlerKeys().contains(JACCRoleMapper.HANDLER_KEY)) {
                if (className == null) {
                    String packageName = SimplePolicyConfiguration.class.getPackage().getName();
                    className = packageName +"."+ "GlassfishRoleMapper";
                }

                ClassLoader loader = Thread.currentThread().getContextClassLoader();
                Class clazz = loader.loadClass(className);
                final Constructor ctx = clazz.getConstructor(new Class[]{Logger.class});

                PolicyContext.registerHandler(
                        JACCRoleMapper.HANDLER_KEY,
                        new PolicyContextHandler() {

                            public Object getContext(String key, Object data)
                                    throws PolicyContextException {
                                if (key.equals(JACCRoleMapper.HANDLER_KEY)) {
                                    try {
                                        return ctx.newInstance(new Object[]{SharedState.getLogger()});
                                    } catch (Throwable t) {
                                        throw new PolicyContextException(t);
                                    }
                                }
                                return null;
                            }

                            public String[] getKeys()
                                    throws PolicyContextException {
                                return new String[]{JACCRoleMapper.HANDLER_KEY};
                            }

                            public boolean supports(String key)
                                    throws PolicyContextException {
                                return key.equals(JACCRoleMapper.HANDLER_KEY);
                            }
                        },
                        false);
            }
        } catch (Throwable t) {
            SharedState.getLogger().log(Level.SEVERE, "RoleMapper.registration.failed", t);
            throw new RuntimeException(t);
        }
    }

    /**
     * Creates a new instance of SimplePolicyConfiguration
     */
    protected SimplePolicyConfiguration(String contextID) {
        id = contextID;
    }

// Public Policy Configuration Interfaces Start here
    /**
     * This method returns this object's policy context identifier.
     * @return this object's policy context identifier.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the getContextID method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public String getContextID()
            throws javax.security.jacc.PolicyContextException {
        return id;
    }

    /**
     * Used to add permissions to a named role in this PolicyConfiguration.
     * If the named Role does not exist in the PolicyConfiguration, it is
     * created as a result of the call to this function.
     * <P>
     * It is the job of the Policy provider to ensure that all the permissions
     * added to a role are granted to principals "mapped to the role".
     * <P>
     * @param roleName the name of the Role to which the permissions are
     * to be added.
     * <P>
     * @param permissions the collection of permissions to be added
     * to the role. The collection may be either a homogenous or
     * heterogenous collection.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToRole method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToRole(String roleName, PermissionCollection permissions)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (roleName != null && permissions != null) {
                Role role = getRole(roleName);
                role.addPermissions(permissions);
            }
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to add a single permission to a named role in this
     * PolicyConfiguration.
     * If the named Role does not exist in the PolicyConfiguration, it is
     * created as a result of the call to this function.
     * <P>
     * It is the job of the Policy provider to ensure that all the permissions
     * added to a role are granted to principals "mapped to the role".
     * <P>
     * @param roleName the name of the Role to which the permission is
     * to be added.
     * <P>
     * @param permission the permission to be added
     * to the role.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToRole method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToRole(String roleName, Permission permission)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (roleName != null && permission != null) {
                Role role = getRole(roleName);
                role.addPermission(permission);
            }
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to add unchecked policy statements to this PolicyConfiguration.
     * <P>
     * @param permissions the collection of permissions to be added
     * as unchecked policy statements. The collection may be either
     * a homogenous or heterogenous collection.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToUncheckedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToUncheckedPolicy(PermissionCollection permissions)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permissions != null) {
                for (Enumeration e = permissions.elements();
                        e.hasMoreElements();) {
                    this.getUncheckedPermissions().add(
                            (Permission) e.nextElement());
                }

            }
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to add a single unchecked policy statement to this
     * PolicyConfiguration.
     * <P>
     * @param permission the permission to be added
     * to the unchecked policy statements.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToUncheckedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToUncheckedPolicy(Permission permission)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permission != null) {
                getUncheckedPermissions().add(permission);
            }

        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to add excluded policy statements to this PolicyConfiguration.
     * <P>
     * @param permissions the collection of permissions to be added
     * to the excluded policy statements. The collection may be either
     * a homogenous or heterogenous collection.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToExcludedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToExcludedPolicy(PermissionCollection permissions)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permissions != null) {
                for (Enumeration e = permissions.elements();
                        e.hasMoreElements();) {
                    this.getExcludedPermissions().add(
                            (Permission) e.nextElement());
                }

            }
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to add a single excluded policy statement to this
     * PolicyConfiguration.
     * <P>
     * @param permission the permission to be added
     * to the excluded policy statements.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the addToExcludedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void addToExcludedPolicy(Permission permission)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (permission != null) {
                getExcludedPermissions().add(permission);
            }

        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to remove a role and all its permissions from this
     * PolicyConfiguration.
     * <P>
     * @param roleName the name of the Role to remove from this
     * PolicyConfiguration.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the removeRole method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void removeRole(String roleName)
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            if (roleName != null && roleTable != null) {
                if (!roleTable.remove(new Role(roleName))) {
		    if (roleName.equals("*")) {
			roleTable.clear();
			roleTable = null;
		    }
		} else if (roleTable.isEmpty()) {
		    roleTable = null;
		}
            }

        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to remove any unchecked policy statements from this
     * PolicyConfiguration.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the removeUncheckedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void removeUncheckedPolicy()
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            uncheckedPermissions =
                    null;
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Used to remove any excluded policy statements from this
     * PolicyConfiguration.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the removeExcludedPolicy method signature.
     * The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void removeExcludedPolicy()
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        pcwLock.lock();
        try {
            assertStateIsOpen();
            excludedPermissions = null;
        } finally {
            pcwLock.unlock();
        }

    }

    /**
     * Creates a relationship between this configuration and another
     * such that they share the same principal-to-role mappings.
     * PolicyConfigurations are linked to apply a common principal-to-role
     * mapping to multiple seperately manageable PolicyConfigurations,
     * as is required when an application is composed of multiple
     * modules.
     * <P>
     * Note that the policy statements which comprise a role, or comprise
     * the excluded or unchecked policy collections in a PolicyConfiguration
     * are unaffected by the configuration being linked to another.
     * <P>
     * @param link a reference to a different PolicyConfiguration than this
     * PolicyConfiguration.
     * <P>
     * The relationship formed by this method is symetric, transitive
     * and idempotent. If the argument PolicyConfiguration does not have a
     * different Policy context identifier than this PolicyConfiguration
     * no relationship is formed, and an exception, as described below, is
     * thrown.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" or "inService" when this
     * method is called.
     *
     * @throws java.lang.IllegalArgumentException
     * if called with an argument PolicyConfiguration whose Policy context
     * is equivalent to that of this PolicyConfiguration.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the linkConfiguration method signature. The exception
     * thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void linkConfiguration(PolicyConfiguration link)
            throws javax.security.jacc.PolicyContextException {

        checkSetPolicyPermission();
        pcrLock.lock();
        try {
            assertStateIsOpen();
        } finally {
            pcrLock.unlock();
        }
        /* if at this point, a simultaneous attempt is made to delete or commit 
         * this pc, we could end up with a corrupted link table. Niether event
         * is likely, but we should try to properly serialize those events.
         */

        SharedState.link(id, link.getContextID());
    }

    /**
     * Causes all policy statements to be deleted from this PolicyConfiguration
     * and sets its internal state such that calling any method, other than
     * delete, getContextID, or inService on the PolicyConfiguration will
     * be rejected and cause an UnsupportedOperationException to be thrown.
     * <P>
     * This operation has no affect on any linked PolicyConfigurations
     * other than removing any links involving the deleted PolicyConfiguration.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the delete method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void delete() throws javax.security.jacc.PolicyContextException {

        checkSetPolicyPermission();
        SharedState.removeLinks(id);

        /* final state will be unlinked and deleted
         */

        pcwLock.lock();
        try {
            removePolicy();
        } finally {
            try {
                setState(DELETED_STATE);
            } finally {
                pcwLock.unlock();
            }
        }

    }

    /**
     * This method is used to set to "inService" the state of the policy context
     * whose interface is this PolicyConfiguration Object. Only those policy
     * contexts whose state is "inService" will be included in the policy
     * contexts processed by the Policy.refresh method. A policy context whose
     * state is "inService" may be returned to the "owpen" state by calling the
     * getPolicyConfiguration method of the PolicyConfiguration factory
     * with the policy context identifier of the policy context.
     * <P>
     * When the state of a policy context is "inService", calling any method
     * other than commit, delete, getContextID, or inService on its
     * PolicyConfiguration Object will cause an UnsupportedOperationException
     * to be thrown.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws java.lang.UnsupportedOperationException
     * if the state of the policy context whose interface is this
     * PolicyConfiguration Object is "deleted" when this
     * method is called.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the commit method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public void commit()
            throws javax.security.jacc.PolicyContextException {
        checkSetPolicyPermission();
        boolean initRoles = false;
        pcwLock.lock();
        try {
            if (stateIs(DELETED_STATE)) {
                String msg = "pc.invalid_op_for_state_delete";
                throw new UnsupportedOperationException(msg);
            }

            if (stateIs(OPEN_STATE)) {
                if (roleTable != null) {
                    initRoles = true;
                }
                setState(INSERVICE_STATE);
            }

        } finally {
            pcwLock.unlock();
        }

        if (initRoles) {
            commitRoleMapping();
        }
    }

    /**
     * This method is used to determine if the policy context whose interface is
     * this PolicyConfiguration Object is in the "inService" state.
     *
     * @return true if the state of the associated policy context is
     * "inService"; false otherwise.
     *
     * @throws java.lang.SecurityException
     * if called by an AccessControlContext that has not been
     * granted the "setPolicy" SecurityPermission.
     *
     * @throws javax.security.jacc.PolicyContextException
     * if the implementation throws a checked exception that has not been
     * accounted for by the inService method signature. The exception thrown
     * by the implementation class will be encapsulated (during construction)
     * in the thrown PolicyContextException.
     */
    public boolean inService()
            throws javax.security.jacc.PolicyContextException {
        pcrLock.lock();
        try {
            return stateIs(INSERVICE_STATE);
        } finally {
            pcrLock.unlock();
        }

    }

    // Internal Policy Configuration interfaces start here
    protected static SimplePolicyConfiguration getPolicyConfig(
            String pcid, boolean remove) throws PolicyContextException {

        SimplePolicyConfiguration pc = SharedState.getConfig(pcid, remove);
        pc.pcwLock.lock();
        try {
            if (remove) {
                pc.removePolicy();
            }
            pc.setState(OPEN_STATE);
        } finally {
            pc.pcwLock.unlock();
        }

        return pc;
    }

    protected static boolean inService(String pcid)
            throws javax.security.jacc.PolicyContextException {
        SimplePolicyConfiguration pc = SharedState.lookupConfig(pcid);
        if (pc == null) {
            return false;
        }
        return pc.inService();
    }

    protected static void checkSetPolicyPermission() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(setPolicyPermission);
        }

    }

    private void setState(int stateValue) {
        this.state = stateValue;
    }

    private boolean stateIs(int stateValue) {
        return this.state == stateValue;
    }

    private void assertStateIsOpen() throws UnsupportedOperationException {
        if (!stateIs(OPEN_STATE)) {
            String msg = "Operation invoked on closed or deleted PolicyConfiguration.";
            throw new UnsupportedOperationException(msg);
        }
    }

    private void assertStateIsInService() throws UnsupportedOperationException {
        if (!stateIs(INSERVICE_STATE)) {
            String msg = "Operation invoked on open or deleted PolicyConfiguration.";
            throw new UnsupportedOperationException(msg);
        }
    }

    private PermissionCollection getUncheckedPermissions() {
        if (uncheckedPermissions == null) {
            uncheckedPermissions = new Permissions();
        }
        return uncheckedPermissions;
    }

    private PermissionCollection getExcludedPermissions() {
        if (excludedPermissions == null) {
            excludedPermissions = new Permissions();
        }
        return excludedPermissions;
    }

    private Role getRole(String roleName) {
        int index = -1;
        Role rvalue = new Role(roleName);

        if (roleTable == null) {
            roleTable = new ArrayList<Role>();
        } else {
            index = roleTable.indexOf(rvalue);
        }

        if (index < 0) {
            roleTable.add(rvalue);
        } else {
            rvalue = roleTable.get(index);
        }

        return rvalue;
    }

    private void removePolicy() {
        excludedPermissions = null;
        uncheckedPermissions = null;
        roleTable = null;
    }

    /**
     * Adds the principal-2-role mapping to the roles in the roleTable
     * @throws javax.security.jacc.PolicyContextException
     */
    private void commitRoleMapping() throws PolicyContextException {
        JACCRoleMapper roleMapper = null;
        try {
            /**
             * NB: when running with a security manager, this method will 
             * call policy, to check if the policy module is authorized
             * to invoke the policy context handler.
             */
            roleMapper = (JACCRoleMapper) PolicyContext.getContext(JACCRoleMapper.HANDLER_KEY);

            if (roleMapper == null) {
                throw new PolicyContextException("RoleMapper.lookup.null");
            }
        } catch (Throwable t) {
            SharedState.getLogger().log(Level.SEVERE, "RoleMapper.lookup.failed", t);
            if (t instanceof PolicyContextException) {
                throw (PolicyContextException) t;
            } else {
                throw new PolicyContextException(t);
            }
        }

        pcwLock.lock();
        try {
            if (roleTable != null) {
                for (Role role : roleTable) {
                    role.setPrincipals(roleMapper.getPrincipalsInRole(id, role.getName()));
                }
                /**
                 * JACC MR8 add handling for the any authenticated user role '**'
                 */
                Role rvalue = new Role("**");
                int index = roleTable.indexOf(rvalue);
                if (index != -1) {
                    rvalue = roleTable.get(index);
                    rvalue.determineAnyAuthenticatedUserRole();
                }
            }
        } finally {
            pcwLock.unlock();
        }
    }

// Public Policy Enforcement Interfaces Start here
    /**
     * Evaluates the global policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed for code from the specified
     * code source.
     *
     * @param codeSource the CodeSource associated with the caller.
     * This encapsulates the original location of the code (where the code
     * came from) and the public key(s) of its signer.
     *
     * @return the set of permissions allowed for code from <i>codesource</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     */
    public static PermissionCollection getPermissions(
            PermissionCollection basePerms, CodeSource codesource)
            throws PolicyContextException {
        SimplePolicyConfiguration pc = SharedState.getActiveConfig();
        return pc == null ? basePerms : pc.getPermissions(basePerms, (PermissionCollection) null, new Principal[0]);
    }

    /**
     * Evaluates the policy and returns a
     * PermissionCollection object specifying the set of
     * permissions allowed given the characteristics of the
     * protection domain.
     *
     * @param domain the ProtectionDomain associated with the caller.
     *
     * @return the set of permissions allowed for the <i>domain</i>
     * according to the policy.The returned set of permissions must be
     * a new mutable instance and it must support heterogeneous
     * Permission types.
     *
     * @see java.security.ProtectionDomain
     * @see java.security.SecureClassLoader
     * @since 1.4
     */
    public static PermissionCollection getPermissions(
            PermissionCollection basePerms, ProtectionDomain domain)
            throws PolicyContextException {
        SimplePolicyConfiguration pc = SharedState.getActiveConfig();
        return pc == null ? basePerms : pc.getPermissions(basePerms, domain.getPermissions(), domain.getPrincipals());
    }

    /**
     * Evaluates the policy to determine whether the permissions is granted to
     * the ProtectionDomain.
     *
     * @param domain the ProtectionDomain to test
     * @param permission the Permission object to be tested for implication.
     *
     * @return integer -1 if excluded, 0 if not implied, 1 if implied
     * granted to this ProtectionDomain.
     *
     */
    public static int implies(ProtectionDomain domain, Permission p)
            throws PolicyContextException {
        SimplePolicyConfiguration pc = SharedState.getActiveConfig();
        return (pc == null ? 0 : pc.doImplies(domain, p));
    }

// Internal Policy Enforcement Interfaces Start here
    private boolean permIsExcluded(Permission p) {
        boolean isExcluded = false;
        if (hasExcludedPermissions()) {
            if (!getExcludedPermissions().implies(p)) {
                /* 
                 * this loop ensures that the tested perm does not imply
                 * an excluded perm; in which case it must be excluded
                 */
                Enumeration e = excludedPermissions.elements();
                while (e.hasMoreElements()) {
                    Permission excludedPerm = (Permission) e.nextElement();
                    if (p.implies(excludedPerm)) {
                        isExcluded = true;
                        break;
                    }

                }
            } else {
                isExcluded = true;
            }

        }
        return isExcluded;
    }

    /**
     * @param d 
     * @param p
     * @return integer -1 if excluded, 0 if not implied, 1 if implied.
     * @throws javax.security.jacc.PolicyContextException
     */
    private int doImplies(ProtectionDomain d, Permission p)
            throws PolicyContextException {
        pcrLock.lock();
        int rvalue = 0;
        try {
            assertStateIsInService();

            if (permIsExcluded(p)) {
                rvalue = -1;

            } else if (getUncheckedPermissions().implies(p)) {
                rvalue = 1;

            } else if (roleTable != null) {

                Principal principals[] = d.getPrincipals();

                if (principals.length == 0) {
                    rvalue = 0;

                } else {

                    for (Role role : roleTable) {
                        if (role.arePrincipalsInRole(principals) && role.implies(p)) {
                            rvalue = 1;
                            break;
                        }
                        //added as a per role debugging convenience
                        if (rvalue != 1) {
                            rvalue = 0;
                        }
                    }

                    //added as an all role debugging convenience
                    if (rvalue != 1) {
                        rvalue = 0;
                    }
                }
            }
            return rvalue;
        } catch (UnsupportedOperationException uso) {
            throw new PolicyContextException(uso);
        } finally {
            pcrLock.unlock();
        }
    }

    private boolean hasExcludedPermissions() {
        return excludedPermissions == null ||
                !excludedPermissions.elements().hasMoreElements() ? false : true;
    }

    private PermissionCollection getPermissions(PermissionCollection basePerms,
            PermissionCollection domainPerms, Principal[] principals)
            throws PolicyContextException, UnsupportedOperationException {

        pcrLock.lock();
        try {
            assertStateIsInService();
            Permissions c = null;
            boolean hasExcludes = hasExcludedPermissions();

            if (basePerms != null) {
                for (Enumeration e = basePerms.elements();
                        e.hasMoreElements();) {
                    Permission p = (Permission) e.nextElement();
                    if (!hasExcludes || !permIsExcluded(p)) {
                        if (c == null) {
                            c = new Permissions();
                        }

                        c.add(p);
                    }

                }
            }

            if (domainPerms != null) {
                for (Enumeration e = domainPerms.elements();
                        e.hasMoreElements();) {
                    Permission p = (Permission) e.nextElement();
                    if (!hasExcludes || !permIsExcluded(p)) {
                        if (c == null) {
                            c = new Permissions();
                        }

                        c.add(p);
                    }

                }
            }

            for (Enumeration e = getUncheckedPermissions().elements();
                    e.hasMoreElements();) {
                Permission p = (Permission) e.nextElement();
                if (!hasExcludes || !permIsExcluded(p)) {
                    if (c == null) {
                        c = new Permissions();
                    }

                    c.add(p);
                }

            }

            if (principals.length == 0 || roleTable == null) {
                return c;
            }

            for (Role role : roleTable) {
                if (role.arePrincipalsInRole(principals)) {
                    PermissionCollection rc = role.getPermissions();
                    for (Enumeration e = rc.elements(); e.hasMoreElements();) {
                        Permission p = (Permission) e.nextElement();
                        if (!hasExcludes || !permIsExcluded(p)) {
                            if (c == null) {
                                c = new Permissions();
                            }

                            c.add(p);
                        }
                    }
                }
            }

            return c;
        } catch (UnsupportedOperationException uso) {
            throw new PolicyContextException(uso);
        } finally {
            pcrLock.unlock();
        }

    }

    /**
     * Refreshes/reloads all of the inservice policy configurations. 
     * The behavior of this method
     * depends on the implementation. For example, calling <code>refresh</code>
     * on a file-based policy will cause the file to be re-read.
     */
    static void refresh() throws PolicyContextException {
    }

    static void doPrivilegedLog(final Level level,
            final String msg, final Object[] params) {
        final Logger logger = SharedState.getLogger();
        if (logger.isLoggable(level)) {
            if (System.getSecurityManager() == null) {
                logger.log(level, msg, params);
            } else {
                AccessController.doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        logger.log(level, msg, params);
                        return null;
                    }
                });
            }
        }
    }

    static void doPrivilegedLog(final Level level,
            final String msg, final Throwable t) {
        final Logger logger = SharedState.getLogger();
        if (logger.isLoggable(level)) {
            if (System.getSecurityManager() == null) {
                logger.log(level, msg, t);
            } else {
                AccessController.doPrivileged(new PrivilegedAction() {

                    public Object run() {
                        logger.log(level, msg, t);
                        return null;
                    }
                });
            }
        }
    }
    // Internal logging interfaces start here
    static void logGetPermissionsFailure(final Object o, Throwable t) {

        String id = PolicyContext.getContextID();
        doPrivilegedLog(Level.INFO, "getPermissions.failure", new Object[]{id, o});
        doPrivilegedLog(Level.INFO, "getPermissions.failure", t);
    }

    private static boolean permissionShouldBeLogged(Permission p) {
        return !(p instanceof WebResourcePermission) &&
                !(p instanceof WebUserDataPermission) &&
                !(p instanceof MBeanPermission) &&
                !(p instanceof WebRoleRefPermission) &&
                !(p instanceof EJBRoleRefPermission);
    }

    static void logAccessFailure(ProtectionDomain d, Permission p) {

        if (permissionShouldBeLogged(p) || SharedState.getLogger().isLoggable(Level.FINE)) {

            String id = PolicyContext.getContextID();
            doPrivilegedLog(Level.FINE, "Domain.that.failed",
                    new Object[]{id, p, d});
        }

    }

    static void logException(Level l, String msg, Throwable t) {
        String id = PolicyContext.getContextID();
        doPrivilegedLog(l, msg, new Object[]{id});
        doPrivilegedLog(l, msg, t);
    }
}
