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
package org.glassfish.security.services.impl.authorization;

import java.net.URI;
import java.security.AccessController;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.security.services.api.authorization.AzAttributeResolver;
import org.glassfish.security.services.api.authorization.AzResource;
import org.glassfish.security.services.api.authorization.AzSubject;
import org.glassfish.security.services.api.authorization.RoleMappingService;
import org.glassfish.security.services.common.PrivilegedLookup;
import org.glassfish.security.services.common.Secure;
import org.glassfish.security.services.config.SecurityConfiguration;
import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.impl.ServiceFactory;
import org.glassfish.security.services.impl.ServiceLogging;
import org.glassfish.security.services.spi.authorization.RoleMappingProvider;

import com.sun.enterprise.config.serverbeans.Domain;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * <code>RoleMappingServiceImpl</code> implements
 * <code>{@link org.glassfish.security.services.api.authorization.RoleMappingService}</code>
 * by delegating role mapping decisions to configured
 * <code>{@link org.glassfish.security.services.spi.RoleMappingProvider}</code>
 * instances.
 */
@Service
@Singleton
@Secure(accessPermissionName="security/service/rolemapper")
public final class RoleMappingServiceImpl implements RoleMappingService, PostConstruct {
	private static final Level DEBUG_LEVEL = Level.FINER;
	private static final Logger logger =
			Logger.getLogger(ServiceLogging.SEC_SVCS_LOGGER,ServiceLogging.SHARED_LOGMESSAGE_RESOURCE);
	private static LocalStringManagerImpl localStrings =
			new LocalStringManagerImpl(RoleMappingServiceImpl.class);

	@Inject
	private Domain domain;

	@Inject
	private ServiceLocator serviceLocator;

	// Role Mapping Service Configuration Information
	private org.glassfish.security.services.config.RoleMappingService config;
	private RoleMappingProvider provider;

	// Service lifecycle
	enum InitializationState {
		NOT_INITIALIZED,
		SUCCESS_INIT,
		FAILED_INIT
	}
	private volatile InitializationState initialized = InitializationState.NOT_INITIALIZED;
	private volatile String reasonInitFailed = 
			localStrings.getLocalString("service.role.not_config","The Role Mapping Service was not configured properly.");

	final InitializationState getInitializationState() {
		return initialized;
	}

	final String getReasonInitializationFailed() {
		return reasonInitFailed;
	}

	final void checkServiceAvailability() {
		if (InitializationState.SUCCESS_INIT != getInitializationState()) {
			throw new IllegalStateException(
					localStrings.getLocalString("service.role.not_avail","The Role Mapping Service is not available.")
					+ getReasonInitializationFailed());
		}
	}

	private final List<AzAttributeResolver> attributeResolvers =
			Collections.synchronizedList(new java.util.ArrayList<AzAttributeResolver>());

	private boolean isDebug() {
		return logger.isLoggable(DEBUG_LEVEL);
	}

	// Helpers
	private AzSubject makeAzSubject(final Subject subject) {
		AzSubject azs = new AzSubjectImpl(subject);
		return azs;
	}

	private AzResource makeAzResource(final URI resource) {
		AzResource azr = new AzResourceImpl(resource);
		return azr;
	}

	/**
	 * Initialize the Role Mapping service with the configured role mapping provider.
	 */
	@Override
	public void initialize(SecurityConfiguration securityServiceConfiguration) {
		if (InitializationState.NOT_INITIALIZED != initialized) {
			return;
		}
		try {
			// Get the Role Mapping Service configuration
			config = (org.glassfish.security.services.config.RoleMappingService) securityServiceConfiguration;
			if (config != null) {
				// Get the role mapping provider configuration
				// Consider only one provider for now and take the first provider found!
				List<SecurityProvider> providersConfig = config.getSecurityProviders();
				SecurityProvider roleProviderConfig = null;
				if (providersConfig != null) roleProviderConfig = providersConfig.get(0);
				if (roleProviderConfig != null) {
					// Get the provider
					String providerName = roleProviderConfig.getName();
					if (isDebug()) {
						logger.log(DEBUG_LEVEL, "Attempting to get Role Mapping Provider \"{0}\".", providerName );
					}
					provider = AccessController.doPrivileged(
					        new PrivilegedLookup<RoleMappingProvider>(serviceLocator, RoleMappingProvider.class, providerName) );    
							
					if (provider == null) {
						throw new IllegalStateException(localStrings.getLocalString("service.role.not_provider",
								"Role Mapping Provider {0} not found.", providerName));
					}

					// Initialize the provider
					provider.initialize(roleProviderConfig);

					// Service setup complete
					initialized = InitializationState.SUCCESS_INIT;
					reasonInitFailed = null;

					// Log initialized
					logger.log(Level.INFO, ROLEMAPSVC_INITIALIZED);
				}
			}
		} catch (Exception e) {
			String eMsg = e.getMessage();
			String eClass = e.getClass().getName();
			reasonInitFailed = localStrings.getLocalString("service.role.init_failed",
					"Role Mapping Service initialization failed, exception {0}, message {1}", eClass, eMsg);
			logger.log(Level.WARNING, ROLEMAPSVC_INIT_FAILED, new Object[] {eClass, eMsg});
			throw new RuntimeException(reasonInitFailed, e);
		} finally {
			if (InitializationState.SUCCESS_INIT != initialized) {
				initialized = InitializationState.FAILED_INIT;
			}
		}
	}

	/**
	 * Determine the user's role by converting arguments into security authorization data types.
	 * 
	 * @see <code>{@link org.glassfish.security.services.api.authorization.RoleMappingService}</code>
	 */
	@Override
	public boolean isUserInRole(String appContext, Subject subject, URI resource, String role) {
		// Validate inputs
		if (subject == null) throw new IllegalArgumentException(
				localStrings.getLocalString("service.subject_null", "The supplied Subject is null."));
		if (resource == null) throw new IllegalArgumentException(
				localStrings.getLocalString("service.resource_null", "The supplied Resource is null."));

		// Convert arguments
		return isUserInRole(appContext, makeAzSubject(subject), makeAzResource(resource), role);
	}

	/**
	 * Determine if the user's is in the specified role.
	 * 
	 * @see <code>{@link org.glassfish.security.services.api.authorization.RoleMappingService}</code>
	 */
	@Override
	public boolean isUserInRole(String appContext, AzSubject subject, AzResource resource, String role) {
		boolean result = false;

		// Validate inputs
		if (subject == null) throw new IllegalArgumentException(
				localStrings.getLocalString("service.subject_null", "The supplied Subject is null."));
		if (resource == null) throw new IllegalArgumentException(
				localStrings.getLocalString("service.resource_null", "The supplied Resource is null."));

		// Make sure provider and config have been setup...
		checkServiceAvailability();

		// Call provider - AzEnvironment and AzAttributeResolver are placeholders
		result = provider.isUserInRole(appContext, subject, resource, role, new AzEnvironmentImpl(), attributeResolvers);

		// Display and return results
		if (isDebug()) {
			logger.log(DEBUG_LEVEL, "Role Mapping Service result {0}"
					+ " for role {1} with resource {2} using subject {3} in context {4}.",
					new String[]{ Boolean.toString(result), role,
						resource.toString(), subject.toString(), appContext});
		}
		return result;
	}

	/**
	 * Find an existing <code>RoleDeploymentContext</code>, or create a new one if one does not
	 * already exist for the specified application context.
	 * 
	 * @see <code>{@link org.glassfish.security.services.api.authorization.RoleMappingService}</code>
	 */
	@Override
	public RoleMappingService.RoleDeploymentContext findOrCreateDeploymentContext(String appContext) {
		checkServiceAvailability();
		return provider.findOrCreateDeploymentContext(appContext);
	}

	/**
	 * Handle lookup of role mapping service configuration and initialization.
	 * If no service or provider is configured the service run-time will throw exceptions.
	 * 
	 * Addresses alternate configuration handling until adopt @Proxiable support. 
	 */
	@Override
	public void postConstruct() {
		org.glassfish.security.services.config.RoleMappingService roleConfiguration =
				ServiceFactory.getSecurityServiceConfiguration(
						domain, org.glassfish.security.services.config.RoleMappingService.class);
		initialize(roleConfiguration);
	}

	//
	// Log Messages
	//

	@LogMessageInfo(
			message = "Role Mapping Service has successfully initialized.",
			level = "INFO")
	private static final String ROLEMAPSVC_INITIALIZED = "SEC-SVCS-00150";

	@LogMessageInfo(
			message = "Role Mapping Service initialization failed, exception {0}, message {1}",
			level = "WARNING")
	private static final String ROLEMAPSVC_INIT_FAILED = "SEC-SVCS-00151";
}
