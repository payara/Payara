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
// Portions Copyright [2016-2024] [Payara Foundation and/or its affiliates]
// Portions Copyright [2024] Contributors to the Eclipse Foundation
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.enterprise.security.acl;

import static com.sun.enterprise.security.common.AppservAccessController.privileged;
import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.stream.Collectors.toSet;

import java.io.Serializable;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;

import com.sun.enterprise.security.auth.login.DistinguishedPrincipalCredential;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.deployment.common.SecurityRoleMapper;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.Role;

import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.security.common.AppservAccessController;
import com.sun.logging.LogDomains;
import org.glassfish.security.common.UserNameAndPassword;
import org.glassfish.security.common.UserPrincipal;

import static com.sun.enterprise.util.Utility.isEmpty;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

/**
 * This class maintains a mapping of users and groups to application specific roles.
 *
 * <p>
 * Using this class the mapping information can be maintained and queried at a later time.
 *
 * @author Harpreet Singh
 */
public class RoleMapper implements Serializable, SecurityRoleMapper {

    private static final long serialVersionUID = -4455830942007736853L;
    private static final Logger _logger = LogDomains.getLogger(RoleMapper.class, SECURITY_LOGGER);

    private String appName;

    // Default mapper to emulate Servlet default p2r mapping semantics
    private String defaultPrincipalToRoleMappingClassName;
    private final DefaultRoleToSubjectMapping defaultRoleToSubjectMapping = new DefaultRoleToSubjectMapping();

    private final Map<String, Subject> roleToSubject = new HashMap<>();
    /*
     * the following 2 Maps are a copy of roleToSubject. This is added as a support for deployment. Should think of
     * optimizing this.
     */
    private final Map<String, Set<Principal>> roleToPrincipal = new HashMap<>();
    private final Map<String, Set<Group>> roleToGroup = new HashMap<>();

    /* The following objects are used to detect conflicts during deployment */
    /*
     * .....Mapping of module (or application) that is presently calling assignRole(). It is set by the startMappingFor()
     * method. After all the subjects have been assigned, stopMappingFor() is called and then the mappings can be checked
     * against those previously assigned.
     */
    private Mapping currentMapping;
    // These override roles mapped in submodules.
    private Set<Role> topLevelRoles;

    // Used to identify the application level mapping file
    private static final String TOP_LEVEL = "sun-application.xml mapping file";

    // Used to log a warning only one time
    private boolean conflictLogged;

    // Store roles that have a conflict so they are not re-mapped
    private Set<Role> conflictedRoles;

    /* End conflict detection objects */
    private Boolean appDefaultMapping;

    private transient SecurityService securityService = null;

    RoleMapper(String appName) {
        this.appName = appName;
        securityService = Globals.getDefaultHabitat().getService(SecurityService.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
        defaultPrincipalToRoleMappingClassName = getDefaultP2RMappingClassName();
    }

    /**
     * Copy constructor. This is called from the JSR88 implementation. This is not stored into the internal rolemapper maps.
     */
    public RoleMapper(RoleMapper other) {
        this.appName = other.getName();
        for (Iterator<String> it = other.getRoles(); it.hasNext();) {
            String role = it.next();

            // Recover groups
            Enumeration<Group> groups = other.getGroupsAssignedTo(new Role(role));
            Set<Group> groupsToRole = new HashSet<>();
            for (; groups.hasMoreElements();) {
                Group gp = groups.nextElement();
                groupsToRole.add(new Group(gp.getName()));
                addRoleToPrincipal(gp, role);
            }
            this.roleToGroup.put(role, groupsToRole);

            // Recover principles
            Enumeration<Principal> users = other.getUsersAssignedTo(new Role(role));
            Set<Principal> usersToRole = new HashSet<>();
            while (users.hasMoreElements()) {
                UserPrincipal principal = (UserPrincipal) users.nextElement();
                usersToRole.add(new UserNameAndPassword(principal.getName()));
                addRoleToPrincipal(principal, role);
            }

            this.roleToPrincipal.put(role, usersToRole);
        }
    }

    private boolean getAppDefaultRoleMapping() {
        if (appDefaultMapping != null) {
            return appDefaultMapping;
        }

        appDefaultMapping = false;
        if (securityService != null) {
            appDefaultMapping = Boolean.parseBoolean(securityService.getActivateDefaultPrincipalToRoleMapping());
            if (appDefaultMapping) {
                // if set explicitly in the security service allow default mapping
                return appDefaultMapping;
            }
        }

        ApplicationRegistry appRegistry = Globals.getDefaultHabitat().getService(ApplicationRegistry.class);
        ApplicationInfo appInfo = appRegistry.get(appName);
        if (appInfo == null) {
            return appDefaultMapping;
        }

        Application app = appInfo.getMetaData(Application.class);
        BundleDescriptor bd = app.getModuleByUri(appName);
        appDefaultMapping = bd == null ? app.isDefaultGroupPrincipalMapping()
                : app.getModuleByUri(appName).isDefaultGroupPrincipalMapping();
        return appDefaultMapping;
    }

    /**
     * @return The application/module name for this RoleMapper
     */
    @Override
    public String getName() {
        return appName;
    }

    /**
     * @param name The application/module name
     */
    @Override
    public void setName(String name) {
        this.appName = name;
    }

    // @return true or false depending on activation of
    // the mapping via domain.xml or property in the glassfish descriptor
    boolean isDefaultRTSMActivated() {
        return (defaultPrincipalToRoleMappingClassName != null) && getAppDefaultRoleMapping();
    }

    /**
     * Returns the RoleToSubjectMapping for the RoleMapping
     *
     * @return Map of role->subject mapping
     */
    @Override
    public Map<String, Subject> getRoleToSubjectMapping() {
        // This causes the last currentMapping information to be added
        checkAndAddMappings();

        assert roleToSubject != null;
        if (roleToSubject.isEmpty() && isDefaultRTSMActivated()) {
            return defaultRoleToSubjectMapping;
        }

        return roleToSubject;
    }

    @Override
    public Map<String, Set<String>> getGroupToRolesMapping() {
        Map<String, Set<String>> groupToRoles = new HashMap<>();

        for (Map.Entry<String, Subject> roleToSubject : getRoleToSubjectMapping().entrySet()) {
            for (String group : getGroups(roleToSubject.getValue())) {
                groupToRoles.computeIfAbsent(group, g -> new HashSet<>())
                            .add(roleToSubject.getKey());
            }
        }

        return groupToRoles;
    }

    @Override
    public Map<String, Set<String>> getCallerToRolesMapping() {
        Map<String, Set<String>> callerToRoles = new HashMap<>();

        for (Map.Entry<String, Subject> roleToSubject : getRoleToSubjectMapping().entrySet()) {
            for (String callerName : getCallerPrincipalNames(roleToSubject.getValue())) {
                callerToRoles.computeIfAbsent(callerName, g -> new HashSet<>())
                             .add(roleToSubject.getKey());
            }
        }

        return callerToRoles;
    }

    /**
     * Assigns a Principal to the specified role. This method delegates work to internalAssignRole() after checking for
     * conflicts. RootDeploymentDescriptor added as a fix for: https://glassfish.dev.java.net/issues/show_bug.cgi?id=2475
     *
     * The first time this is called, a new Mapping object is created to store the role mapping information. When called
     * again from a different module, the old mapping information is checked and stored and a new Mapping object is created.
     *
     * @param principal The principal that needs to be assigned to the role.
     * @param role The Role the principal is being assigned to.
     * @param rootDeploymentDescriptor The descriptor of the module containing the role mapping
     */
    @Override
    public void assignRole(Principal principal, Role role, RootDeploymentDescriptor rootDeploymentDescriptor) {
        assert rootDeploymentDescriptor != null;
        String callingModuleID = getModuleID(rootDeploymentDescriptor);

        if (currentMapping == null) {
            currentMapping = new Mapping(callingModuleID);
        } else if (!callingModuleID.equals(currentMapping.owner)) {
            checkAndAddMappings();
            currentMapping = new Mapping(callingModuleID);
        }

        // When using the top level mapping
        if (callingModuleID.equals(TOP_LEVEL) && topLevelRoles == null) {
            topLevelRoles = new HashSet<>();
        }

        // Store principal and role temporarily until stopMappingFor called
        currentMapping.addMapping(principal, role);
    }

    /**
     * Remove the given role-principal mapping
     *
     * @param role, Role object
     * @param principal, the principal
     */
    @Override
    public void unassignPrincipalFromRole(Role role, final Principal principal) {
        String roleName = role.getName();
        final Subject subject = roleToSubject.get(roleName);
        if (subject != null) {
            AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public java.lang.Object run() {
                    subject.getPrincipals().remove(principal);
                    return null;
                }
            });
            roleToSubject.put(roleName, subject);
        }

        if (principal instanceof Group) {
            Set<Group> groups = roleToGroup.get(roleName);
            if (groups != null) {
                groups.remove(principal);
                roleToGroup.put(roleName, groups);
            }
        } else {
            Set<Principal> principals = roleToPrincipal.get(roleName);
            if (principals != null) {
                principals.remove(principal);
                roleToPrincipal.put(roleName, principals);
            }
        }
    }

    /**
     * Returns an enumeration of roles for this rolemapper.
     */
    @Override
    public Iterator<String> getRoles() {
        assert roleToSubject != null;
        return roleToSubject.keySet().iterator(); // All the roles
    }

    /**
     * Returns an enumeration of Groups assigned to the given role
     *
     * @param role The Role to which the groups are assigned to.
     */
    @Override
    public Enumeration<Group> getGroupsAssignedTo(Role role) {
        assert roleToGroup != null;
        Set<Group> groups = roleToGroup.get(role.getName());
        if (groups == null) {
            return Collections.enumeration(Collections.emptySet());
        }

        return Collections.enumeration(groups);
    }

    /**
     * Returns an enumeration of Principals assigned to the given role
     *
     * @param role The Role to which the principals are assigned to.
     */
    @Override
    public Enumeration<Principal> getUsersAssignedTo(Role role) {
        assert roleToPrincipal != null;
        Set<Principal> principals = roleToPrincipal.get(role.getName());
        if (principals == null) {
            return Collections.enumeration(Collections.emptySet());
        }

        return Collections.enumeration(principals);
    }

    @Override
    public void unassignRole(Role role) {
        if (role != null) {
            String roleName = role.getName();
            roleToSubject.remove(roleName);
            roleToPrincipal.remove(roleName);
            roleToGroup.remove(roleName);
        }
    }

    // @return true or false depending on activation of
    // the mapping via domain.xml.
    @Override
    public boolean isDefaultPrincipalToRoleMapping() {
        return defaultPrincipalToRoleMappingClassName != null;
    }

    @Override
    public Set<String> getGroups(Subject subject) {
        return
            subject.getPrincipals()
                   .stream()
                   .filter(e -> e instanceof Group)
                   .map(e -> e.getName())
                   .collect(toSet());
    }

    @Override
    public Principal getCallerPrincipal(Subject subject) {
        return
            subject.getPublicCredentials()
                   .stream()
                   .filter(DistinguishedPrincipalCredential.class::isInstance)
                   .map(Principal.class::cast)
                   .findAny()
                   .orElse(subject.getPrincipals()
                       .stream()
                       .filter(UserPrincipal.class::isInstance)
                       .findAny()
                       .orElse(null));
    }

    /**
     * @return String. String representation of the RoleToPrincipal Mapping
     */
    @Override
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
        if (_logger.isLoggable(FINER)) {
            _logger.log(FINER, s.toString());
        }
        return s.toString();
    }

    /**
     * @param principal A principal that corresponds to the role
     * @param role A role corresponding to this principal
     */
    private void addRoleToPrincipal(Principal principal, String role) {
        privileged(() -> roleToSubject.computeIfAbsent(role, e -> new Subject())
                     .getPrincipals()
                     .add(principal));
    }

    /**
     * @returns the class name used for default Principal to role mapping return null if default P2R mapping is not
     * supported.
     */
    private String getDefaultP2RMappingClassName() {
        String className = null;
        try {
            if (securityService != null && Boolean.parseBoolean(securityService.getActivateDefaultPrincipalToRoleMapping())) {
                className = securityService.getMappedPrincipalClass();
                if (isEmpty(className)) {
                    className = Group.class.getName();
                }
            }

            if (className == null) {
                return null;
            }

            // To avoid a failure later make sure we can instantiate now
            Class.forName(className)
                 .getConstructor(String.class)
                 .newInstance("anystring");

            return className;
        } catch (Exception e) {
            _logger.log(Level.SEVERE, "pc.getDefaultP2RMappingClass: " + className, e);
            return null;
        }
    }

    private Set<String> getCallerPrincipalNames(Subject subject) {
        return
            subject.getPrincipals()
                   .stream()
                   .filter(UserPrincipal.class::isInstance)
                   .map(Principal::getName)
                   .collect(toSet());
    }

    /**
     * For each role in the current mapping:
     *
     * First check that the role does not already exist in the top-level mapping. If it does, then the top-level role
     * mapping overrides the current one and we do not need to check if they conflict. Just continue with the next role.
     *
     * If the current mapping is from the top-level file, then check to see if the role has already been mapped. If so, do
     * not need to check for conflicts. Simply override and assign the role.
     *
     * If the above cases do not apply, check for conflicts with roles already set. If there is a conflict, it is between
     * two submodules, so the role should be unmapped in the existing role mappings.
     *
     */
    private void checkAndAddMappings() {
        if (currentMapping == null) {
            return;
        }

        for (Role role : currentMapping.getRoles()) {

            if (topLevelRoles != null && topLevelRoles.contains(role)) {
                logConflictWarning();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,"Role {0} from module {1} is being overridden by top-level mapping.",
                            new Object[] {role, currentMapping.owner});
                }
                continue;
            }

            if (currentMapping.owner.equals(TOP_LEVEL)) {
                topLevelRoles.add(role);
                if (roleToSubject.keySet().contains(role.getName())) {
                    logConflictWarning();
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE,
                            "Role {0} from top-level mapping descriptor is overriding existing role in sub module.", role);
                    }
                    unassignRole(role);
                }

            } else if (roleConflicts(role, currentMapping.getPrincipals(role))) {
                // Detail message already logged
                logConflictWarning();
                unassignRole(role);
                continue;
            }

            // No problems, so assign role
            for (Principal principal : currentMapping.getPrincipals(role)) {
                internalAssignRole(principal, role);
            }

        }

        // Clear current mapping
        currentMapping = null;
    }

    // The method that does the work for assignRole().
    private void internalAssignRole(Principal principal, Role role) {
        String roleName = role.getName();
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "SECURITY:RoleMapper Assigning Role {0} to {1}", new Object[] {roleName, principal});
        }

        addRoleToPrincipal(principal, roleName);

        if (principal instanceof Group) {
            roleToGroup.computeIfAbsent(roleName, e -> new HashSet<>())
                    .add((Group) principal);

        } else {
            roleToPrincipal.computeIfAbsent(roleName, e -> new HashSet<>())
                    .add(principal);
        }
    }

    /**
     * Only web/ejb BundleDescriptor and Application descriptor objects are used for role mapping currently. If other
     * subtypes of RootDeploymentDescriptor are used in the future, they should be added here.
     */
    private String getModuleID(RootDeploymentDescriptor rootDeploymentDescriptor) {
        // V3: Can we use this : return rdd.getModuleID();

        if (rootDeploymentDescriptor.isApplication()) {
            return TOP_LEVEL;
        }

        if (rootDeploymentDescriptor.getModuleDescriptor() != null) {
            return rootDeploymentDescriptor.getModuleDescriptor().getArchiveUri();
        }

        // Cannot happen unless glassfish code is changed
        throw new AssertionError(rootDeploymentDescriptor.getClass() + " is not a known descriptor type");

    }

    private boolean roleConflicts(Role r, Set<Principal> ps) {
        // check to see if there has been a previous conflict
        if (conflictedRoles != null && conflictedRoles.contains(r)) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(FINE, "Role {0} from module {1} has already had a conflict with other modules.",
                    new Object[] {r, currentMapping.owner});
            }
            return true;
        }

        // If role not previously mapped, no conflict
        if (!roleToSubject.keySet().contains(r.getName())) {
            return false;
        }

        // check number of mappings first
        int targetNumPrin = ps.size();
        int actualNum = 0;
        Set<Principal> pSet = roleToPrincipal.get(r.getName());
        Set<Group> gSet = roleToGroup.get(r.getName());
        actualNum += pSet == null ? 0 : pSet.size();
        actualNum += gSet == null ? 0 : gSet.size();
        if (targetNumPrin != actualNum) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(FINE, "Module {0} has different number of mappings for role {1} than other mapping files",
                        new Object[] {currentMapping.owner, r.getName()});
            }

            if (conflictedRoles == null) {
                conflictedRoles = new HashSet<>();
            }

            conflictedRoles.add(r);
            return true;
        }

        // check the principals and groups
        boolean fail = false;
        for (Principal p : ps) {
            if (p instanceof Group) {
                if (gSet != null && !gSet.contains(p)) {
                    fail = true;
                }

            } else if (pSet != null && !pSet.contains(p)) {
                fail = true;
            }

            if (fail) {
                if (_logger.isLoggable(FINE)) {
                    _logger.log(FINE, "Role {0} in module {1} is not included in other modules.",
                            new Object[] {r, currentMapping.owner});
                }

                if (conflictedRoles == null) {
                    conflictedRoles = new HashSet<>();
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
            _logger.log(WARNING, "Role mapping conflicts found in application {0}. Some roles may not be mapped.", getName());
            conflictLogged = true;
        }
    }

    /**
     * Used to represent the role mapping of a single descriptor file.
     */
    private static class Mapping implements Serializable {
        private static final long serialVersionUID = 5863982599500877228L;

        private final String owner;
        private final Map<Role, Set<Principal>> roleMap;

        Mapping(String owner) {
            this.owner = owner;
            roleMap = new HashMap<>();
        }

        void addMapping(Principal principal, Role role) {
            roleMap.computeIfAbsent(role, e -> new HashSet<>())
                   .add(principal);
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

        private final Map<String, Subject> roleMap = new HashMap<>();

        DefaultRoleToSubjectMapping() {
        }

        // Do not map '**' to a Principal as this represents the any authenticated user role
        @Override
        public Subject get(Object key) {
            synchronized (roleMap) {
                Subject subject = roleMap.get(key);
                if (subject == null && key instanceof String && !"**".equals(key)) {
                    Subject fs = new Subject();
                    String roleName = (String) key;
                    privileged(() -> fs.getPrincipals().add(getSameNamedPrincipal(roleName)));
                    roleMap.put(roleName, fs);
                    subject = fs;
                }

                return subject;
            }
        }

        Principal getSameNamedPrincipal(String roleName) {
            try {
                return (Principal)
                    Class.forName(defaultPrincipalToRoleMappingClassName)
                         .getConstructor(String.class)
                         .newInstance(roleName);
            } catch (Exception e) {
                _logger.log(SEVERE, "rm.getSameNamedPrincipal", new Object[] { roleName, e });
                throw new RuntimeException("Unable to get principal by default p2r mapping");
            }
        }
    }
}
