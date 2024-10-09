/*
 * Copyright (c) 2021, 2024 Contributors to the Eclipse Foundation. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
package org.glassfish.ejb.security.application;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.deployment.RoleReference;
import com.sun.logging.LogDomains;

import jakarta.security.jacc.EJBMethodPermission;
import jakarta.security.jacc.PolicyContextException;

import java.lang.reflect.Method;
import java.security.Permission;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.exousia.mapping.SecurityRoleRef;
import org.glassfish.exousia.permissions.JakartaPermissions;

import static java.util.Collections.list;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

public class PayaraToExousiaConverter {

    private static final Logger _logger = LogDomains.getLogger(EJBSecurityManager.class, LogDomains.EJB_LOGGER);

    /**
     * This method converts the deployment descriptor in two phases.
     *
     * <p>
     * Phase 1: gets a map representing the methodPermission elements exactly as they
     * occured for the ejb in the dd. The map is keyed by method-permission element and each method-permission is mapped to a list of
     * method elements representing the method elements of the method permision element. Each method element is converted to a
     * corresponding EJBMethodPermission and added, based on its associated method-permission, to the returned JakartaPermissions instance.
     *
     * <p>
     * phase 2: configures additional EJBMethodPermission policy statements for the purpose of optimizing Permissions.implies
     * matching by the policy provider. This phase also configures unchecked policy statements for any uncovered methods. This method
     * gets the list of method descriptors for the ejb from the EjbDescriptor object. For each method descriptor, it will get a list
     * of MethodPermission objects that signify the method permissions for the Method and convert each to a corresponding
     * EJBMethodPermission to be added to the returned JakartaPermissions instance.
     *
     * @param ejbDescriptor the ejb descriptor for this EJB.
     */
    public static JakartaPermissions convertEJBMethodPermissions(EjbDescriptor ejbDescriptor) throws PolicyContextException {
        JakartaPermissions jakartaPermissions = new JakartaPermissions();

        String ejbName = ejbDescriptor.getName();

        // phase 1
        Map<MethodPermission, List<MethodDescriptor>> methodPermissionsFromDD = ejbDescriptor.getMethodPermissionsFromDD();

        for (var methodPermissionFromDD : methodPermissionsFromDD.entrySet()) {
            MethodPermission methodPermission = methodPermissionFromDD.getKey();
            for (MethodDescriptor methodDescriptor : methodPermissionFromDD.getValue()) {
                String methodName = methodDescriptor.getName();
                String methodInterface = methodDescriptor.getEjbClassSymbol();
                String methodParameters[] = methodDescriptor.getStyle() == 3
                        ? methodDescriptor.getParameterClassNames()
                        : null;

                EJBMethodPermission ejbMethodPermission = new EJBMethodPermission(ejbName,
                        methodName.equals("*") ? null : methodName, methodInterface, methodParameters);

                if (methodPermission.isExcluded()) {
                    jakartaPermissions.getExcluded().add(ejbMethodPermission);
                } else if (methodPermission.isUnchecked()) {
                    jakartaPermissions.getUnchecked().add(ejbMethodPermission);
                } else if (methodPermission.isRoleBased()) {
                    jakartaPermissions.getPerRole()
                            .computeIfAbsent(methodPermission.getRole().getName(), e -> new Permissions())
                            .add(ejbMethodPermission);
                }
            }
        }

        // phase 2 - configures additional perms:
        //      . to optimize performance of Permissions.implies
        //      . to cause any uncovered methods to be unchecked

        for (MethodDescriptor methodDescriptor : ejbDescriptor.getMethodDescriptors()) {

            Method methodName = methodDescriptor.getMethod(ejbDescriptor);
            String methodInterface = methodDescriptor.getEjbClassSymbol();

            if (methodName == null) {
                continue;
            }

            if (methodInterface == null || methodInterface.isEmpty()) {
                _logger.log(SEVERE, "method_descriptor_not_defined",
                        new Object[] { ejbName, methodDescriptor.getName(), methodDescriptor.getParameterClassNames() });

                continue;
            }

            EJBMethodPermission ejbMethodPermission = new EJBMethodPermission(ejbName, methodInterface, methodName);

            Set<MethodPermission> methodPermissions = ejbDescriptor.getMethodPermissionsFor(methodDescriptor);
            _logger.log(Level.FINEST, "Descriptor: {0}, permissions: {1}",
                    new Object[] {methodDescriptor, methodPermissions});
            for (MethodPermission methodPermission : methodPermissions) {
                if (methodPermission.isExcluded()) {
                    jakartaPermissions.getExcluded().add(ejbMethodPermission);
                } else if (methodPermission.isUnchecked()) {
                    jakartaPermissions.getUnchecked().add(ejbMethodPermission);
                } else if (methodPermission.isRoleBased()) {
                    jakartaPermissions.getPerRole()
                            .computeIfAbsent(methodPermission.getRole().getName(), e -> new Permissions())
                            .add(ejbMethodPermission);
                }
            }
        }

        return jakartaPermissions;
    }

    /**
     * Get the security role refs from the EjbDescriptor.
     *
     * @param ejbDescriptor the EjbDescriptor.
     * @return the security role refs.
     */
    public static Map<String, List<SecurityRoleRef>> getSecurityRoleRefsFromBundle(EjbDescriptor ejbDescriptor) {
        Map<String, List<SecurityRoleRef>> exousiaRoleRefsPerEnterpriseBean = new HashMap<>();

        List<SecurityRoleRef> exousiaSecurityRoleRefs = new ArrayList<>();

        for (RoleReference glassFishSecurityRoleRef : ejbDescriptor.getRoleReferences()) {
            exousiaSecurityRoleRefs.add(new SecurityRoleRef(
                    glassFishSecurityRoleRef.getRoleName(),
                    glassFishSecurityRoleRef.getSecurityRoleLink().getName()));
        }

        exousiaRoleRefsPerEnterpriseBean.put(ejbDescriptor.getName(), exousiaSecurityRoleRefs);

        return exousiaRoleRefsPerEnterpriseBean;
    }

    private static void log(JakartaPermissions jakartaPermissions) {
        for (Permission ejbMethodPermission : list(jakartaPermissions.getExcluded().elements())) {
            _logger.log(FINE, () ->
                    "Jakarta Authorization DD conversion: EJBMethodPermission ->(" +
                            ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() +
                            ") is (excluded)");
        }

        for (Permission ejbMethodPermission : list(jakartaPermissions.getUnchecked().elements())) {
            _logger.log(FINE, () ->
                    "Jakarta Authorization conversion: EJBMethodPermission ->(" +
                            ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() +
                            ") is (unchecked)");
        }

        for (var perMissionsPerRole : jakartaPermissions.getPerRole().entrySet()) {
            String role = perMissionsPerRole.getKey();
            for (Permission ejbMethodPermission : list(perMissionsPerRole.getValue().elements())) {
                _logger.log(FINE, () ->
                        "Jakarta Authorization conversion: EJBMethodPermission ->(" +
                                ejbMethodPermission.getName() + " " + ejbMethodPermission.getActions() +
                                ")protected by role -> " + role);
            }

        }

    }

}
