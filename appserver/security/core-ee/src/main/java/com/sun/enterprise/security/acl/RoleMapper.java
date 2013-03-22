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

package com.sun.enterprise.security.acl;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.*;
import javax.security.auth.Subject;

import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.security.common.Role;
import org.glassfish.security.common.PrincipalImpl;
import org.glassfish.deployment.common.SecurityRoleMapper;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.logging.*;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.api.Globals;
import org.glassfish.security.common.Group;


/** This Object maintains  a mapping of users and groups to application
 * specific Roles.
 * Using this object this mapping information could be maintained and
 * queried at a later time. This is a complete rewrite of the previous
 * RoleMapper for JACC related changes.
 * @author Harpreet Singh
 */
public class RoleMapper implements Serializable, SecurityRoleMapper {

    //private static Map ROLEMAPPER = new HashMap();
    private static final long serialVersionUID = -4455830942007736853L;
    private static final String DEFAULT_ROLE_NAME = "ANYONE";
    private  Role defaultRole = null;
    private  String defaultRoleName = null;
    private  String appName;
    private final Map<String, Subject> roleToSubject =
            new HashMap<String, Subject>();

    // default mapper to emulate Servlet default p2r mapping semantics
    private String defaultP2RMappingClassName = null;
    private DefaultRoleToSubjectMapping defaultRTSM =
            new DefaultRoleToSubjectMapping();
    /* the following 2 Maps are a copy of roleToSubject.
     * This is added as a support for deployment.
     * Should think of optimizing this.
     */
    private final Map<String, Set<Principal>> roleToPrincipal =
            new HashMap<String, Set<Principal>>();
    private final Map<String, Set<Group>> roleToGroup =
            new HashMap<String, Set<Group>>();
    /* The following objects are used to detect conflicts during deployment */
    /* .....Mapping of module (or application) that is presently calling
     * assignRole(). It is set by the startMappingFor() method.
     * After all the subjects have been assigned, stopMappingFor()
     * is called and then the mappings can be checked against
     * those previously assigned.
     */
    private Mapping currentMapping;
    // These override roles mapped in submodules.
    private Set<Role> topLevelRoles;

    // used to identify the application level mapping file
    private static final String TOP_LEVEL = "sun-application.xml mapping file";
    // used to log a warning only one time
    private boolean conflictLogged = false;
    // store roles that have a conflict so they are not re-mapped
    private Set<Role> conflictedRoles;
    /* End conflict detection objects */
    private static final Logger _logger =
            LogDomains.getLogger(RoleMapper.class, LogDomains.SECURITY_LOGGER);

    private transient SecurityService secService = null;
    
    RoleMapper(String appName) {
        this.appName = appName;
        secService = Globals.getDefaultHabitat().getService(SecurityService.class,
                ServerEnvironment.DEFAULT_INSTANCE_NAME);
        defaultP2RMappingClassName = getDefaultP2RMappingClassName();
        postConstruct();
    }
   
    private  synchronized void initDefaultRole() {  
//        if (!SecurityServicesUtil.getInstance().isServer()) {
//            //do nothing if this is not an EJB or Web Container
//            return;
//        }
        if (defaultRole == null) {
            defaultRoleName = DEFAULT_ROLE_NAME;
            try {
                assert (secService != null);
                defaultRoleName = secService.getAnonymousRole();
            } catch (Exception e) {
                _logger.log(Level.WARNING,
                        "java_security.anonymous_role_reading_exception",
                        e);
            }

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Default role is: " + defaultRoleName);
            }
            defaultRole = new Role(defaultRoleName);
        }
    }

    /**
     * @return The application/module name for this RoleMapper
     */
    public String getName() {
        return appName;
    }

    /**
     * @param name The application/module name
     */
    public void setName(String name) {
        this.appName = name;
    }

    /**
     * @param principal A principal that corresponds to the role
     * @param role A role corresponding to this principal
     */
    private void addRoleToPrincipal(final Principal principal, String role) {
        assert roleToSubject != null;
        Subject subject = roleToSubject.get(role);
        final Subject sub = (subject == null) ? new Subject() : subject;
        AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {

            public java.lang.Object run() {
                sub.getPrincipals().add(principal);
                return null;
            }
        });
        roleToSubject.put(role, sub);
    }

    /**
     * Remove the given role-principal mapping
     * @param role, Role object
     * @param principal, the principal
     */
    public void unassignPrincipalFromRole(Role role, Principal principal) {
        assert roleToSubject != null;
        String mrole = role.getName();
        final Subject sub = roleToSubject.get(mrole);
        final Principal p = principal;
        if (sub != null) {
            AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {

                public java.lang.Object run() {
                    sub.getPrincipals().remove(p);
                    return null;
                }
            });
            roleToSubject.put(mrole, sub);
        }
        if (principal instanceof Group) {
            Set<Group> groups = roleToGroup.get(mrole);
            if (groups != null) {
                groups.remove((Group) principal);
                roleToGroup.put(mrole, groups);
            }
        } else {
            Set<Principal> principals = roleToPrincipal.get(mrole);
            if (principals != null) {
                principals.remove(principal);
                roleToPrincipal.put(mrole, principals);
            }
        }
    }

    // @return true or false depending on activation of
    // the mapping via domain.xml.
    boolean isDefaultRTSMActivated() {
        return (defaultP2RMappingClassName != null);
    }

    /**
     * Returns the RoleToSubjectMapping for the RoleMapping
     * @return Map of role->subject mapping
     */
    public Map<String, Subject> getRoleToSubjectMapping() {
        // this causes the last currentMapping information to be added
        checkAndAddMappings();
        assert roleToSubject != null;
        if (roleToSubject.isEmpty() && isDefaultRTSMActivated()) {
            return defaultRTSM;
        }
        return roleToSubject;
    }

    // The method that does the work for assignRole().
    private void internalAssignRole(Principal p, Role r) {
        String role = r.getName();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SECURITY:RoleMapper Assigning Role " + role +
                    " to  " + p.getName());
        }
        addRoleToPrincipal(p, role);
        if (p instanceof Group) {
            Set<Group> groups = roleToGroup.get(role);
            if (groups == null) {
                groups = new HashSet<Group>();
            }
            groups.add((Group) p);
            roleToGroup.put(role, groups);
        } else {
            Set<Principal> principals = roleToPrincipal.get(role);
            if (principals == null) {
                principals = new HashSet<Principal>();
            }
            principals.add(p);
            roleToPrincipal.put(role, principals);
        }
    }

    /**
     * Assigns a Principal to the specified role. This method delegates
     * work to internalAssignRole() after checking for conflicts.
     * RootDeploymentDescriptor added as a fix for:
     * https://glassfish.dev.java.net/issues/show_bug.cgi?id=2475
     *
     * The first time this is called, a new Mapping object is created
     * to store the role mapping information. When called again from
     * a different module, the old mapping information is checked and
     * stored and a new Mapping object is created.
     *
     * @param p The principal that needs to be assigned to the role.
     * @param r The Role the principal is being assigned to.
     * @param rdd The descriptor of the module containing the role mapping
     */
    public void assignRole(Principal p, Role r, RootDeploymentDescriptor rdd) {
        assert rdd != null;
        String callingModuleID = getModuleID(rdd);

        if (currentMapping == null) {
            currentMapping = new Mapping(callingModuleID);
        } else if (!callingModuleID.equals(currentMapping.owner)) {
            checkAndAddMappings();
            currentMapping = new Mapping(callingModuleID);
        }

        // when using the top level mapping
        if (callingModuleID.equals(TOP_LEVEL) &&
                topLevelRoles == null) {
            topLevelRoles = new HashSet<Role>();
        }

        // store principal and role temporarily until stopMappingFor called
        currentMapping.addMapping(p, r);
    }

    /**
     * Returns an enumeration of roles for this rolemapper.
     */
    public Iterator<String> getRoles() {
        assert roleToSubject != null;
        return roleToSubject.keySet().iterator(); // All the roles
    }

    /**
     * Returns an enumeration of Groups assigned to the given role
     * @param The Role to which the groups are assigned to.
     */
    public Enumeration<Group> getGroupsAssignedTo(Role r) {
        assert roleToGroup != null;
        Set<Group> s = roleToGroup.get(r.getName());
        if (s == null) {
            return Collections.enumeration(Collections.EMPTY_SET);
        }
        return Collections.enumeration(s);
    }

    /**
     * Returns an enumeration of Principals assigned to the given role
     * @param The Role to which the principals are assigned to.
     */
    public Enumeration<Principal> getUsersAssignedTo(Role r) {
        assert roleToPrincipal != null;
        Set<Principal> s = roleToPrincipal.get(r.getName());
        if (s == null) {
            return Collections.enumeration(Collections.EMPTY_SET);
        }
        return Collections.enumeration(s);
    }

    public void unassignRole(Role r) {
        if (r != null) {
            String role = r.getName();
            roleToSubject.remove(role);
            roleToPrincipal.remove(role);
            roleToGroup.remove(role);
        }
    }

    /**
     * @return String. String representation of the RoleToPrincipal Mapping
     */
    public String toString() {

        StringBuilder s = new StringBuilder("RoleMapper:");
        for (Iterator<String> e = this.getRoles(); e.hasNext();) {
            String r = e.next();
            s.append("\n\tRole (").append(r).append(") has Principals(");
            Subject sub = roleToSubject.get(r);
            Iterator<Principal> it = sub.getPrincipals().iterator();
            for (; it.hasNext();) {
                Principal p = it.next();
                s.append(p.getName()).append(" ");
            }
            s.append(")");
        }
        if (_logger.isLoggable(Level.FINER)) {
            _logger.log(Level.FINER, s.toString());
        }
        return s.toString();
    }

    /** Copy constructor. This is called from the JSR88 implementation.
     * This is not stored into the internal rolemapper maps.
     */
    public RoleMapper(RoleMapper r) {
        this.appName = r.getName();
        for (Iterator<String> it = r.getRoles(); it.hasNext();) {
            String role = it.next();
            // recover groups
            Enumeration<Group> groups = r.getGroupsAssignedTo(new Role(role));
            Set<Group> groupsToRole = new HashSet<Group>();
            for (; groups.hasMoreElements();) {
                Group gp = groups.nextElement();
                groupsToRole.add(new Group(gp.getName()));
                addRoleToPrincipal(gp, role);
            }
            this.roleToGroup.put(role, groupsToRole);

            // recover principles
            Enumeration<Principal> users = r.getUsersAssignedTo(new Role(role));
            Set<Principal> usersToRole = new HashSet<Principal>();
            for (; users.hasMoreElements();) {
                PrincipalImpl gp = (PrincipalImpl) users.nextElement();
                usersToRole.add(new PrincipalImpl(gp.getName()));
                addRoleToPrincipal(gp, role);
            }
            this.roleToPrincipal.put(role, usersToRole);
        }
    }

    /**
     * @returns the class name used for default Principal to role mapping
     *          return null if default P2R mapping is not supported.
     */
     private String getDefaultP2RMappingClassName() {
        String className = null;
         try {
             if (secService != null && Boolean.parseBoolean(secService.getActivateDefaultPrincipalToRoleMapping())) {
                 className = secService.getMappedPrincipalClass();
                 if (className == null || "".equals(className)) {
                     className = Group.class.getName();
                 }
             }

             if (className == null) {
                 return null;
             }
             Class<?> clazz = Class.forName(className);
             Class<?>[] argClasses = new Class<?>[]{String.class};
             Object[] arg = new Object[]{"anystring"};
             Constructor<?> c = clazz.getConstructor(argClasses);
             //To avoid a failure later make sure we can instantiate now
             Principal principal = (Principal) c.newInstance(arg);
             return className;
         } catch (Exception e) {
            _logger.log(Level.SEVERE, "pc.getDefaultP2RMappingClass: " + e);
            return null;
        }
    }

    /*
     * Only web/ejb BundleDescriptor and Application descriptor objects
     * are used for role mapping currently. If other subtypes of
     * RootDeploymentDescriptor are used in the future, they should
     * be added here.
     */
    private String getModuleID(RootDeploymentDescriptor rdd) {
        //V3: Can we use this : return  rdd.getModuleID();
    /*V3:Comment
        if (rdd instanceof Application) {
        return TOP_LEVEL;
        } else if (rdd instanceof BundleDescriptor) {
        return ((BundleDescriptor) rdd).getModuleDescriptor().getArchiveUri();
        } else {
        // cannot happen unless glassfish code is changed
        throw new AssertionError(rdd.getClass() +
        " is not a known descriptor type");
        }*/
        if (rdd.isApplication()) {
            return TOP_LEVEL;
        } else if (rdd.getModuleDescriptor() != null) {
            return rdd.getModuleDescriptor().getArchiveUri();
        } else {
            // cannot happen unless glassfish code is changed
            throw new AssertionError(rdd.getClass() +
                    " is not a known descriptor type");
        }

    }

    /*
     * For each role in the current mapping:
     *
     * First check that the role does not already exist in the
     * top-level mapping. If it does, then the top-level role mapping
     * overrides the current one and we do not need to check if they
     * conflict. Just continue with the next role.
     *
     * If the current mapping is from the top-level file, then
     * check to see if the role has already been mapped. If so,
     * do not need to check for conflicts. Simply override and
     * assign the role.
     * 
     * If the above cases do not apply, check for conflicts
     * with roles already set. If there is a conflict, it is
     * between two submodules, so the role should be unmapped
     * in the existing role mappings.
     *
     */
    private void checkAndAddMappings() {
        if (currentMapping == null) {
            return;
        }

        for (Role r : currentMapping.getRoles()) {

            if (topLevelRoles != null && topLevelRoles.contains(r)) {
                logConflictWarning();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Role " + r +
                            " from module " + currentMapping.owner +
                            " is being overridden by top-level mapping.");
                }

                continue;
            }

            if (currentMapping.owner.equals(TOP_LEVEL)) {
                topLevelRoles.add(r);
                if (roleToSubject.keySet().contains(r.getName())) {
                    logConflictWarning();
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "Role " + r +
                                " from top-level mapping descriptor is " +
                                "overriding existing role in sub module.");
                    }

                    unassignRole(r);
                }

            } else if (roleConflicts(r, currentMapping.getPrincipals(r))) {
                // detail message already logged
                logConflictWarning();
                unassignRole(r);
                continue;
            }

            // no problems, so assign role
            for (Principal p : currentMapping.getPrincipals(r)) {
                internalAssignRole(p, r);
            }

        }

        // clear current mapping
        currentMapping = null;
    }


    private boolean roleConflicts(Role r, Set<Principal> ps) {

        // check to see if there has been a previous conflict
        if (conflictedRoles != null && conflictedRoles.contains(r)) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "Role " + r + " from module " + currentMapping.owner +
                        " has already had a conflict with other modules.");
            }

            return true;
        }

// if role not previously mapped, no conflict
        if (!roleToSubject.keySet().contains(r.getName())) {
            return false;
        }

// check number of mappings first
        int targetNumPrin = ps.size();
        int actualNum = 0;
        Set<Principal> pSet = roleToPrincipal.get(r.getName());
        Set<Group> gSet = roleToGroup.get(r.getName());
        actualNum +=
                (pSet == null) ? 0 : pSet.size();
        actualNum +=
                (gSet == null) ? 0 : gSet.size();
        if (targetNumPrin != actualNum) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "Module " + currentMapping.owner +
                        " has different number of mappings for role " +
                        r.getName() +
                        " than other mapping files");
            }

            if (conflictedRoles == null) {
                conflictedRoles = new HashSet<Role>();
            }

            conflictedRoles.add(r);
            return true;
        }

// check the principals and groups
        boolean fail = false;
        for (Principal p : ps) {
            if (p instanceof Group) {
                if (gSet != null && !gSet.contains((Group) p)) {
                    fail = true;
                }

            } else if (pSet != null && !pSet.contains(p)) {
                fail = true;
            }

            if (fail) {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                            "Role " + r + " in module " + currentMapping.owner +
                            " is not included in other modules.");
                }

                if (conflictedRoles == null) {
                    conflictedRoles = new HashSet<Role>();
                }

                conflictedRoles.add(r);
                return true;
            }

        }

        // no conflicts
        return false;
    }

    private void logConflictWarning() {
        if (!conflictLogged) {
            _logger.log(Level.WARNING, "java_security.role_mapping_conflict",
                    getName());
            conflictLogged =
                    true;
        }

    }

    /*
     * Used to represent the role mapping of a single
     * descriptor file.
     */
    private static class Mapping implements Serializable {
        private static final long serialVersionUID = 5863982599500877228L;
        private final String owner;
        private final Map<Role, Set<Principal>> roleMap;

        Mapping(String owner) {
            this.owner = owner;
            roleMap = new HashMap<Role, Set<Principal>>();
        }

        void addMapping(Principal p, Role r) {
            Set<Principal> pSet = roleMap.get(r);
            if (pSet == null) {
                pSet = new HashSet<Principal>();
                roleMap.put(r, pSet);
            }
            pSet.add(p);
        }

        Set<Role> getRoles() {
            return roleMap.keySet();
        }

        Set<Principal> getPrincipals(Role r) {
            return roleMap.get(r);
        }
    }

    class DefaultRoleToSubjectMapping extends HashMap<String, Subject> {
        private static final long serialVersionUID = 3074733840327132690L;

        private final HashMap<String, Subject> roleMap = new HashMap<String, Subject>();

        DefaultRoleToSubjectMapping() {
            super();
        }

        Principal getSameNamedPrincipal(String roleName) {
            try {
                Class<?> clazz = Class.forName(defaultP2RMappingClassName);
                Class<?>[] argClasses = new Class<?>[]{String.class};
                Object[] arg = new Object[]{roleName};
                Constructor<?> c = clazz.getConstructor(argClasses);
                Principal principal = (Principal) c.newInstance(arg);
                return principal;
            } catch (Exception e) {
                _logger.log(Level.SEVERE, "rm.getSameNamedPrincipal", new Object[]{roleName, e});
                throw new RuntimeException("Unable to get principal by default p2r mapping");
            }
        }

        // Do not map '**' to a Principal as this represents the any authenticated user role
        public Subject get(Object key) {
            synchronized (roleMap) {
                Subject s = roleMap.get((String)key);
                if ((s == null) && (key instanceof String) && (!"**".equals((String)key))) {
                    final Subject fs = new Subject();
                    final String roleName = (String) key;
                    AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {

                        public java.lang.Object run() {
                            fs.getPrincipals().add(getSameNamedPrincipal(roleName));
                            return null;
                        }
                    });
                    roleMap.put((String)key, fs);
                    s = fs;
                }
                return s;
            }
        }
    }

    private void postConstruct() {
//       initDefaultRole();
    }

}
