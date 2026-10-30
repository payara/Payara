/*
 * Copyright (c) 2021, 2023 Eclipse Foundation and/or its affiliates. All rights reserved.
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
// Portions Copyright [2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.authorization;

import static jakarta.servlet.annotation.ServletSecurity.TransportGuarantee.CONFIDENTIAL;
import static jakarta.servlet.annotation.ServletSecurity.TransportGuarantee.NONE;
import static java.util.Collections.list;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.enterprise.deployment.web.WebDescriptor;
import org.glassfish.exousia.constraints.SecurityConstraint;
import org.glassfish.exousia.constraints.WebResourceCollection;
import org.glassfish.exousia.mapping.SecurityRoleRef;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.WebComponentDescriptor;
import com.sun.enterprise.deployment.web.SecurityRoleReference;

import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;

/**
 * This class converts from GlassFish security types to Exousia security types.
 *
 * @author arjan
 */
public class GlassFishToExousiaConverter {


    /**
     * Get the security constraints from the WebBundleDescriptor.
     *
     * @param webBundleDescriptor the WebBundleDescriptor.
     * @return the security constraints.
     */
    public static List<SecurityConstraint> getConstraintsFromBundle(WebBundleDescriptor webBundleDescriptor) {
        return webBundleDescriptor.getSecurityConstraintsSet().stream().map(GlassFishToExousiaConverter::toExousia)
                .collect(Collectors.toList());
    }


    private static SecurityConstraint toExousia(com.sun.enterprise.deployment.web.SecurityConstraint gfConstraint) {
        List<WebResourceCollection> resources = gfConstraint.getWebResourceCollections().stream()
                .map(GlassFishToExousiaConverter::toWebResourceCollection).collect(Collectors.toList());
        TransportGuarantee transportGuarantee = "confidential".equalsIgnoreCase(transportGuarantee(gfConstraint))
                ? CONFIDENTIAL
                : NONE;
        return new SecurityConstraint(resources, securityRoles(gfConstraint), transportGuarantee);
    }


    private static WebResourceCollection toWebResourceCollection(
            com.sun.enterprise.deployment.web.WebResourceCollection gfCollection) {
        WebResourceCollection resourceCollection = new WebResourceCollection(
                gfCollection.getUrlPatterns(),
                gfCollection.getHttpMethods(),
                gfCollection.getHttpMethodOmissions());
        return resourceCollection;
    }


    static Set<String> securityRoles(com.sun.enterprise.deployment.web.SecurityConstraint gfSecurityConstraint) {
        if (gfSecurityConstraint.getAuthorizationConstraint() == null) {
            return null;
        }
        return
                list(gfSecurityConstraint.getAuthorizationConstraint().getSecurityRoles())
                        .stream()
                        .map(WebDescriptor::getName)
                        .collect(toSet());
    }

    static String transportGuarantee(com.sun.enterprise.deployment.web.SecurityConstraint gfSecurityConstraint) {
        if (gfSecurityConstraint.getUserDataConstraint() == null) {
            return null;
        }
        return gfSecurityConstraint.getUserDataConstraint().getTransportGuarantee();
    }


    /**
     * Get the security role refs from the WebBundleDescriptor.
     *
     * @param webBundleDescriptor the WebBundleDescriptor.
     * @return the security role refs.
     */
    public static Map<String, List<SecurityRoleRef>> getSecurityRoleRefsFromBundle(WebBundleDescriptor webBundleDescriptor) {
        Map<String, List<SecurityRoleRef>> exousiaRoleRefsPerServlet = new HashMap<>();
        for (WebComponentDescriptor webComponent : webBundleDescriptor.getWebComponentDescriptors()) {
            List<SecurityRoleRef> exousiaSecurityRoleRefs = new ArrayList<>();
            for (SecurityRoleReference glassFishSecurityRoleRef : webComponent.getSecurityRoleReferenceSet()) {
                SecurityRoleRef roleRef = new SecurityRoleRef(
                        glassFishSecurityRoleRef.getRoleName(),
                        glassFishSecurityRoleRef.getSecurityRoleLink().getName()
                );
                exousiaSecurityRoleRefs.add(roleRef);
            }
            exousiaRoleRefsPerServlet.put(webComponent.getCanonicalName(), exousiaSecurityRoleRefs);
        }
        return exousiaRoleRefsPerServlet;
    }
}