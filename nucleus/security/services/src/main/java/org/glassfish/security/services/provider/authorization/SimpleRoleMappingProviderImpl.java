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
package org.glassfish.security.services.provider.authorization;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.security.services.api.authorization.AuthorizationAdminConstants;
import org.glassfish.security.services.api.authorization.AzAttributeResolver;
import org.glassfish.security.services.api.authorization.AzEnvironment;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.authorization.RoleMappingService;
import org.glassfish.security.services.common.Secure;
import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.impl.ServiceLogging;
import org.glassfish.security.services.spi.authorization.RoleMappingProvider;

import org.glassfish.logging.annotation.LogMessageInfo;

@Service (name="simpleRoleMapping")
@Secure(accessPermissionName="security/service/rolemapper/provider/simple")
@PerLookup
public class SimpleRoleMappingProviderImpl implements RoleMappingProvider {
	private static final Level DEBUG_LEVEL = Level.FINER;
	private static final Logger _logger =
			Logger.getLogger(ServiceLogging.SEC_PROV_LOGGER,ServiceLogging.SHARED_LOGMESSAGE_RESOURCE);

	private static final String ADMIN = "Admin";

	private RoleMappingProviderConfig cfg; 
	private boolean deployable;
	private String version;
	private Map<String, ?> options;

	private boolean isDebug() {
		return _logger.isLoggable(DEBUG_LEVEL);
	}

	private boolean isAdminResource(AzResource resource) {
		return "admin".equals(resource.getUri().getScheme());
	}

	private boolean containsAdminGroup(AzSubject subject) {
		// Only checking for principal name
		for (Principal p : subject.getSubject().getPrincipals()) {
			if (AuthorizationAdminConstants.ADMIN_GROUP.equals(p.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void initialize(SecurityProvider providerConfig) {
		cfg = (RoleMappingProviderConfig)providerConfig.getSecurityProviderConfig().get(0);        
		deployable = cfg.getSupportRoleDeploy();
		version = cfg.getVersion();
		options = cfg.getProviderOptions();
		if (isDebug()) {
			_logger.log(DEBUG_LEVEL, "provider deploy:  " + deployable);
			_logger.log(DEBUG_LEVEL, "provider version: " + version);
			_logger.log(DEBUG_LEVEL, "provider options: " + options);
		}
	}

	@Override
	public boolean isUserInRole(String appContext, AzSubject subject, AzResource resource, String role, AzEnvironment environment, List<AzAttributeResolver> resolvers) {
		boolean result = false;
		if (isDebug()) _logger.log(DEBUG_LEVEL, "isUserInRole() - " + role);

		if (!isAdminResource(resource)) {
			// Log a warning if the resource is not correct
			final String resourceName = resource.getUri() == null ? "null" : resource.getUri().toASCIIString();
			_logger.log(Level.WARNING, ROLEPROV_BAD_RESOURCE, resourceName);
			_logger.log(Level.WARNING, "IllegalArgumentException", new IllegalArgumentException(resourceName));
		}

		// Only support for admin role 
		if (ADMIN.equals(role)) {
			result = containsAdminGroup(subject);
		}

		if (isDebug()) _logger.log(DEBUG_LEVEL, "isUserInRole() - returning " + result);
		return result;
	}

	@Override
	public RoleMappingService.RoleDeploymentContext findOrCreateDeploymentContext(String appContext) {
		// Not Supported
		return null;
	}

	@LogMessageInfo(
			message = "Role Mapping Provider supplied an invalid resource: {0}",
			level = "WARNING")
	private static final String ROLEPROV_BAD_RESOURCE = "SEC-PROV-00150";
}
