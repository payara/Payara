/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.jwtauth.jaxrs;

import java.lang.reflect.Method;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * This JAX-RS dynamic feature will install filters for JAX-RS resources
 * that check roles or deny all access.
 * 
 * @author Arjan Tijms
 */
@Provider
public class RolesAllowedDynamicFeature implements DynamicFeature {
    
    @Context
    private HttpServletRequest request;
    
    @Context
    private HttpServletResponse response;

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext configuration) {
        Method resourceMethod = resourceInfo.getResourceMethod();

        // ## Method level access
        
        // Deny All (Excluded) resources cannot be accessed by anyone
        if (resourceMethod.isAnnotationPresent(DenyAll.class)) {
            configuration.register(new DenyAllRequestFilter());
            return;
        }
        
        // Permit All (Unchecked) resources are free to be accessed by everyone
        if (resourceMethod.isAnnotationPresent(PermitAll.class)) {
            return;
        }

        // Access is granted via role 
        RolesAllowed rolesAllowed = resourceMethod.getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            configuration.register(new RolesAllowedRequestFilter(request, response, rolesAllowed.value()));
            return;
        }
        
        // ## Class level access

        rolesAllowed = resourceInfo.getResourceClass().getAnnotation(RolesAllowed.class);
        if (rolesAllowed != null) {
            configuration.register(new RolesAllowedRequestFilter(request, response, rolesAllowed.value()));
        }
    }
 
}