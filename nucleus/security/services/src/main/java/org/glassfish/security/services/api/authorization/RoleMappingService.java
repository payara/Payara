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
package org.glassfish.security.services.api.authorization;

import java.net.URI;
import javax.security.auth.Subject;

import org.glassfish.security.services.api.SecurityService;
import org.jvnet.hk2.annotations.Contract;

/**
 * The <code>RoleMappingService</code> provides functions that determine a user's role.
 */
@Contract
public interface RoleMappingService extends SecurityService {

	/**
	 * Determine whether the user (<code>Subject</code>) has the indicated role
	 * for a given resource (<code>URI</code>) and application context.
	 *
	 * @param appContext The application context for the query (can be null).
	 * @param subject The target <code>Subject</code>.
	 * @param resource The <code>URI</code> resource for the query.
	 * @param role The target role.
	 * @return true if the user has the specified role.
	 *
	 * @throws IllegalArgumentException for a <code>null</code> subject or resource
	 * @throws IllegalStateException if the service was not initialized.
	 */
	public boolean isUserInRole(String appContext, Subject subject, URI resource, String role);

	/**
	 * Determine whether the user (<code>AzSubject</code>) has the indicated role
	 * for a given resource (<code>AzResource</code>) and application context.
	 *
	 * @param appContext The application context for the query (can be null).
	 * @param subject The target <code>{@link org.glassfish.security.services.api.authorization.AzSubject}</code>.
	 * @param resource The <code>{@link org.glassfish.security.services.api.authorization.AzResource}</code> for the query.
	 * @param role The target role.
	 * @return true if the user has the specified role.
	 *
	 * @throws IllegalArgumentException for a <code>null</code> subject or resource
	 * @throws IllegalStateException if the service was not initialized.
	 */
	public boolean isUserInRole(String appContext, AzSubject subject, AzResource resource, String role);

	/**
	 * Find an existing <code>RoleDeploymentContext</code>, or create a new one if one does not
	 * already exist for the specified application context.  The role deployment context will be
	 * returned in an "open" state, and will stay that way until commit() or delete() is called.
	 *
	 * @param appContext The application context for which the <code>RoleDeploymentContext</code> is desired.
	 * @return The resulting <code>RoleDeploymentContext</code> or <code>null</code> if the configured providers
	 * do not support this feature.
	 * 
	 * @throws IllegalStateException if the service was not initialized.
	 */
	public RoleDeploymentContext findOrCreateDeploymentContext(String appContext);

	/**
	 * This interface represents a <code>RoleDeploymentContext</code> as returned by the Role Mapping
	 * Service's findOrCreateDeploymentContext() method.  The <code>RoleDeploymentContext</code> is used
	 * to configure role mapping policy for an application (or server administration) context.
	 * It represents the body of policy that applies to the given context.
	 *
	 * A <code>RoleDeploymentContext</code> is always in one of three states: open, closed/inService,
	 * or deleted.  When returned by the Role Mapping service, a context is in an open state.
	 * Policies can be added or deleted while in the open state, but the context is not
	 * in service.  Upon calling commit(), the context is closed and the policies are place
	 * in service.  Upon calling delete(), the context is taken out of service and the policies
	 * are deleted from the Role Mapping Provider.
	 */
	public interface RoleDeploymentContext {

		public void addMapping(String role, String[] users, String[] groups);

		public void removeMapping(String role, String[] users, String[] groups);

		public void removeRole(String role);

		public void commit();

		public void delete();
	}
}
