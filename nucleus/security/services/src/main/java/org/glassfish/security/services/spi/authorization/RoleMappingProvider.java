/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.spi.authorization;

import java.util.List;

import org.glassfish.security.services.api.authorization.AzAttributeResolver;
import org.glassfish.security.services.api.authorization.AzEnvironment;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.authorization.RoleMappingService;
import org.glassfish.security.services.spi.SecurityProvider;

import org.jvnet.hk2.annotations.Contract;

/**
 * <code>RoleMappingProvider</code> instances are used by the
 * <code>{@link org.glassfish.security.services.api.authorization.RoleMappingService}</code>
 * to evaluate role policy conditions.
 * 
 * The security provider is part of a plug-in mechanism which allows decisions
 * to be handled by a configured implementation.
 */
@Contract
public interface RoleMappingProvider extends SecurityProvider {

	/**
	 * Determine whether the user (<code>AzSubject</code>) has the indicated role
	 * for a given resource (<code>AzResource</code>) and application context.
	 *
	 * @param appContext The application context for the query (can be null).
	 * @param subject The target <code>Subject</code>.
	 * @param resource The <code>URI</code> resource for the query.
	 * @param role The target role.
	 * @param environment The attributes collection representing the environment.
	 * @param attributeResolvers The ordered list of attribute resolvers.
	 *
	 * @see {@link org.glassfish.security.services.api.authorization.RoleMappingService#isUserInRole(String, AzSubject, AzResource, String)}
	 */
	public boolean isUserInRole(String appContext,
			AzSubject subject,
			AzResource resource,
			String role,
			AzEnvironment environment,
			List<AzAttributeResolver> attributeResolvers);

	/**
	 * Find an existing <code>RoleDeploymentContext</code>, or create a new one if one does not
	 * already exist for the specified application context. 
	 *
	 * @param appContext The application context for which the <code>RoleDeploymentContext</code> is desired.
	 * 
	 * @see {@link org.glassfish.security.services.api.authorization.RoleMappingService#findOrCreateDeploymentContext(String)}
	 */
	public RoleMappingService.RoleDeploymentContext findOrCreateDeploymentContext(String appContext);
}
