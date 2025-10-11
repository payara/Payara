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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.jacc;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

import java.lang.reflect.Method;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.EJBRoleRefPermission;
import jakarta.security.jacc.PolicyConfiguration;
import jakarta.security.jacc.PolicyContextException;

import org.glassfish.security.common.Role;

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.RoleReference;

/**
 * This class is used for translating security constrains from <code>ejb-jar.xml</code> and corresponding
 * annotations into JACC permissions, and writing this to the pluggable {@link PolicyConfiguration} (which is
 * EE standard permission repository).
 * 
 * @author Harpreet Singh, monzillo
 * @author Arjan Tijms (refactoring)
 *
 */
public class JaccEJBConstraintsTranslator {
    
    private static final Logger _logger = Logger.getLogger(SECURITY_LOGGER);
    
    private JaccEJBConstraintsTranslator() {
        
    }
    
    /**
     * Translate the security constraints presents in the given <code>EjbDescriptor</code> to JACC permissions
     * and store those in the given <code>PolicyConfiguration</code>.
     * 
     * @param ejbDescriptor the source of the security constraints
     * @param policyConfiguration the target of the security permissions
     * @throws PolicyContextException
     */
    public static void translateConstraintsToPermissions(EjbDescriptor ejbDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        createEJBMethodPermissions(ejbDescriptor, policyConfiguration);
        createEJBRoleRefPermissions(ejbDescriptor, policyConfiguration);
    }
    
    /**
     * This method converts the dd in two phases. Phase 1: gets a map representing the methodPermission elements exactly as
     * they occured for the ejb in the dd. The map is keyed by method-permission element and each method-permission is
     * mapped to a list of method elements representing the method elements of the method permision element. Each method
     * element is converted to a corresponding EJBMethodPermission and added, based on its associated method-permission, to
     * the policy configuration object. phase 2: configures additional EJBMethodPermission policy statements for the purpose
     * of optimizing Permissions.implies matching by the policy provider. This phase also configures unchecked policy
     * statements for any uncovered methods. This method gets the list of method descriptors for the ejb from the
     * EjbDescriptor object. For each method descriptor, it will get a list of MethodPermission objects that signify the
     * method permissions for the Method and convert each to a corresponding EJBMethodPermission to be added to the policy
     * configuration object.
     *
     * @param ejbDescriptor the ejb descriptor for this EJB.
     * @param policyConfiguration, the policy configuration
     */
    private static void createEJBMethodPermissions(EjbDescriptor ejbDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        String ejbName = ejbDescriptor.getName();

        Permissions uncheckedPermissions = null;
        Permissions excludedPermissions = null;
        Map<String, Permissions> perRolePermissions = null;


        // Phase 1
        
        Map<MethodPermission, List<MethodDescriptor>> methodPermissions = ejbDescriptor.getMethodPermissionsFromDD();
        if (methodPermissions != null) {
            
            for (Entry<MethodPermission, List<MethodDescriptor>> permissionEntry : methodPermissions.entrySet()) {

                MethodPermission methodPermission = permissionEntry.getKey();
                
                for (MethodDescriptor methodDescriptor : permissionEntry.getValue()) {
                    String methodName = methodDescriptor.getName().equals("*") ? null : methodDescriptor.getName();
                    String methodInterface = methodDescriptor.getEjbClassSymbol();
                    String methodParams[] = methodDescriptor.getStyle() == 3 ? methodDescriptor.getParameterClassNames() : null;

                    EJBMethodPermission ejbMethodPermission = new EJBMethodPermission(ejbName, methodName, methodInterface, methodParams);
                    
                    perRolePermissions = addToRolePermissions(perRolePermissions, methodPermission, ejbMethodPermission);
                    uncheckedPermissions = addToUncheckedPermissions(uncheckedPermissions, methodPermission, ejbMethodPermission);
                    excludedPermissions = addToExcludedPermissions(excludedPermissions, methodPermission, ejbMethodPermission);
                }
            }
        }

        // Phase 2 - configures additional permissions:
        // * To optimize performance of Permissions.implies
        // * To cause any uncovered methods to be unchecked

        for (MethodDescriptor methodDescriptor : ejbDescriptor.getMethodDescriptors()) {
            Method method = methodDescriptor.getMethod(ejbDescriptor);
            if (method == null) {
                continue;
            }

            String methodInterface = methodDescriptor.getEjbClassSymbol();
            if (methodInterface == null || methodInterface.equals("")) {
                _logger.log(SEVERE, "method_descriptor_not_defined",
                        new Object[] { ejbName, methodDescriptor.getName(), methodDescriptor.getParameterClassNames() });

                continue;
            }

            EJBMethodPermission ejbMethodPermission = new EJBMethodPermission(ejbName, methodInterface, method);

            for (MethodPermission methodPermission : ejbDescriptor.getMethodPermissionsFor(methodDescriptor)) {
                perRolePermissions = addToRolePermissions(perRolePermissions, methodPermission, ejbMethodPermission);
                uncheckedPermissions = addToUncheckedPermissions(uncheckedPermissions, methodPermission, ejbMethodPermission);
                excludedPermissions = addToExcludedPermissions(excludedPermissions, methodPermission, ejbMethodPermission);
            }
        }

        if (uncheckedPermissions != null) {
            policyConfiguration.addToUncheckedPolicy(uncheckedPermissions);
        }
        
        if (excludedPermissions != null) {
            policyConfiguration.addToExcludedPolicy(excludedPermissions);
        }
        
        if (perRolePermissions != null) {
            for (Entry<String, Permissions> entry : perRolePermissions.entrySet()) {
                policyConfiguration.addToRole(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * This method converts ejb role references to jacc permission objects and adds them to the policy configuration object
     * It gets the list of role references from the ejb descriptor. For each such role reference, create a
     * EJBRoleRefPermission and add it to the PolicyConfiguration object.
     *
     * @param ejbDescriptor the ejb descriptor
     * @param pcid, the policy context identifier
     */
    private static void createEJBRoleRefPermissions(EjbDescriptor ejbDescriptor, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        
        List<Role> ejbScopedRoleNames = new ArrayList<Role>();
        Collection<Role> allRoles = ejbDescriptor.getEjbBundleDescriptor().getRoles();
        
        Role anyAuthUserRole = new Role("**");
        boolean rolesetContainsAnyAuthUserRole = allRoles.contains(anyAuthUserRole);
        
        // Name of EJB being processed in this call
        String ejbName = ejbDescriptor.getName();
        
        writeOutPermissionsForRoleRefRoles(ejbDescriptor.getRoleReferences(), ejbScopedRoleNames, ejbName, policyConfiguration);
        
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,
                    "JACC: Converting role-ref: Going through the list of roles not present in RoleRef elements and creating EJBRoleRefPermissions ");
        }
        
        // For every role in the application for which there is no mapping (role reference) defined for this EJB
        // we insert a 1:1 role mapping. E.g global role "foo" maps to an identical named role "foo" in the scope of EJB 
        // "MyEJB"
        //
        // Note this is the most common situation as mapping roles per EJB is quite rare in practice
        writeOutPermissionsForNonRoleRefRoles(allRoles, ejbScopedRoleNames, ejbName, policyConfiguration);
        
        /**
         * JACC MR8 add EJBRoleRefPermission for the any authenticated user role '**'
         */
        if ((!ejbScopedRoleNames.contains(anyAuthUserRole)) && !rolesetContainsAnyAuthUserRole) {
            addAnyAuthenticatedUserRoleRef(policyConfiguration, ejbName);
        }
    }
    
    private static void writeOutPermissionsForRoleRefRoles(Collection<RoleReference> roleReferences, List<Role> ejbScopedRoleNames, String ejbName, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (RoleReference roleReference : roleReferences) {
            
            // The name of a role, local (scoped) to a single EJB
            String ejbScopedRoleName = roleReference.getRoleName();
            ejbScopedRoleNames.add(new Role(ejbScopedRoleName));
            
            // The name of the global role to which the local EJB scoped role links (is mapped)
            String globalRoleName = roleReference.getSecurityRoleLink().getName();
            
            // Write the role reference to the target policy configuration
            policyConfiguration.addToRole(globalRoleName, new EJBRoleRefPermission(ejbName, ejbScopedRoleName));

            if (_logger.isLoggable(FINE)) {
                _logger.fine(
                    "JACC: Converting role-ref -> " + roleReference.toString() + 
                    " to permission with name(" + ejbName + ")" + 
                    " and actions (" + ejbScopedRoleName + ")" + 
                    " mapped to role (" + globalRoleName + ")");
            }
        }
    }
    
    
    private static void writeOutPermissionsForNonRoleRefRoles(Collection<Role> allRoles, Collection<Role> ejbScopedRoleNames, String ejbName, PolicyConfiguration policyConfiguration) throws PolicyContextException {
        for (Role role : allRoles) {
            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC: Converting role-ref: Looking at Role =  " + role.getName());
            }
            
            if (!ejbScopedRoleNames.contains(role)) {
                String roleName = role.getName();
                policyConfiguration.addToRole(roleName, new EJBRoleRefPermission(ejbName, roleName));
                
                if (_logger.isLoggable(FINE)) {
                    _logger.fine(
                        "JACC: Converting role-ref: Role =  " + role.getName() + 
                        " is added as a permission with name(" + ejbName + ")" + 
                        " and actions (" + roleName + ")" + 
                        " mapped to role (" + roleName + ")");
                }
            }
        }
    }
    
    /**
     * JACC MR8 add WebRoleRefPermission for the any authenticated user role '**'
     */
    private static void addAnyAuthenticatedUserRoleRef(PolicyConfiguration policyConfiguration, String ejbName) throws PolicyContextException {
        String rolename = "**";
        policyConfiguration.addToRole(rolename, new EJBRoleRefPermission(ejbName, rolename));
        
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(
                "JACC: Converting role-ref: Adding any authenticated user role-ref " + 
                " to permission with name(" + ejbName + ")" + 
                " and actions (" + rolename + ")" + 
                " mapped to role (" + rolename + ")");
        }
    }
    
    
    
    // collect role permisisions in table of collections
    private static Map<String, Permissions> addToRolePermissions(Map<String, Permissions> perRolePermissions, MethodPermission methodPermission, EJBMethodPermission ejbMethodPermission) {
        if (methodPermission.isRoleBased()) {
            if (perRolePermissions == null) {
                perRolePermissions = new HashMap<>();
            }
            
            String roleName = methodPermission.getRole().getName();
            
            perRolePermissions.computeIfAbsent(roleName, e -> new Permissions())
                              .add(ejbMethodPermission);
            
            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->(" + ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() + ")protected by role -> " + roleName);
            }
        }
        
        return perRolePermissions;
    }

    // collect unchecked permissions in collection
    private static Permissions addToUncheckedPermissions(Permissions permissions, MethodPermission methodPermission, EJBMethodPermission ejbMethodPermission) {
        if (methodPermission.isUnchecked()) {
            if (permissions == null) {
                permissions = new Permissions();
            }
            
            permissions.add(ejbMethodPermission);
            
            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->(" + ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() + ") is (unchecked)");
            }
        }
        return permissions;
    }

    // collect excluded permissions in collection
    private static Permissions addToExcludedPermissions(Permissions permissions, MethodPermission methodPermission, EJBMethodPermission ejbMethodPermission) {
        if (methodPermission.isExcluded()) {
            if (permissions == null) {
                permissions = new Permissions();
            }
            
            permissions.add(ejbMethodPermission);
            
            if (_logger.isLoggable(FINE)) {
                _logger.fine("JACC DD conversion: EJBMethodPermission ->(" + ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() + ") is (excluded)");
            }
        }
        
        return permissions;
    }

}
