/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.impl;

import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.common.AppservAccessController;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.PrincipalImpl;

import org.glassfish.security.services.api.authentication.AuthenticationService;
import org.glassfish.security.services.config.LoginModuleConfig;
import org.glassfish.security.services.config.SecurityProvider;
import org.glassfish.security.services.config.SecurityProviderConfig;

/**
 * The Authentication Service Implementation.
 * 
 * Use JAAS LoginContext with the LoginModule(s) specified by the service configuration.
 */
@Service
@Singleton
public class AuthenticationServiceImpl implements AuthenticationService, PostConstruct {
	@Inject
	private Domain domain;

	// Authentication Service Configuration Information
	private String name = null;
	private String realmName = null;
	private Configuration configuration = null;
	private boolean usePasswordCredential = false;
	private org.glassfish.security.services.config.AuthenticationService config = null;

	/**
	 * Initialize the Authentication Service configuration.
	 * 
	 * Create the JAAS Configuration using the specified LoginModule configurations
	 */
	@Override
	public void initialize(org.glassfish.security.services.config.SecurityService serviceConfiguration) {
        config = (org.glassfish.security.services.config.AuthenticationService) serviceConfiguration;
        if (config == null)
        	return;

        // JAAS LoginContext Name
        name = config.getName();

        // Determine if handling Realm password credential
        usePasswordCredential = config.getUsePasswordCredential();

        // Build JAAS Configuration based on the individual LoginModuleConfig settings
        List<SecurityProvider> providers = config.getSecurityProviders();
        if (providers != null) {
    		ArrayList<AppConfigurationEntry> lmEntries = new ArrayList<AppConfigurationEntry>();
        	for (SecurityProvider provider : providers) {

        		// If the provider is a LoginModule look for the LoginModuleConfig
        		if ("LoginModule".equalsIgnoreCase(provider.getType())) {
        			List<SecurityProviderConfig> providerConfig = provider.getSecurityProviderConfig();
        			if ((providerConfig != null) && (!providerConfig.isEmpty())) {

        				// Create the JAAS AppConfigurationEntry from the LoginModule settings
        				LoginModuleConfig lmConfig = (LoginModuleConfig) providerConfig.get(0);
        				Map<String, ?> lmOptions = lmConfig.getModuleOptions();
        				lmEntries.add(new AppConfigurationEntry(lmConfig.getModuleClass(),
        						getLoginModuleControlFlag(lmConfig.getControlFlag()),lmOptions));

        				// Obtain the Realm name for password credential from the LoginModule options
        				// Use the first LoginModule with auth-realm (i.e. unable to stack Realms)
        				if (usePasswordCredential && (realmName == null)) {
        					String authRealm = (String) lmOptions.get("auth-realm");
        					if ((authRealm != null) && (!authRealm.isEmpty()))
    							realmName = authRealm;
        				}
        			}
        		}
        	}
        	if (!lmEntries.isEmpty())
        		configuration = new AuthenticationJaasConfiguration(name,lmEntries);
        }
    }

	@Override
	public Subject login(String username, char[] password, Subject subject)
			throws LoginException {
		// Use the supplied Subject or create a new Subject
		Subject _subject = subject;
		if (_subject == null)
			_subject = new Subject();

		try {
			// Unable to login without a JAAS Configuration instance
			// TODO - Dynamic configuration handling?
			if (configuration == null) {
				throw new UnsupportedOperationException(
						"JAAS Configuration setup incomplete, unable to perform login");
			}

			// Setup the PasswordCredential for the Realm LoginModules when required
			if (usePasswordCredential) {
				final Subject s = _subject;
				final PasswordCredential pc = new PasswordCredential(username, password, realmName);
				AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						s.getPrivateCredentials().add(pc);
						return null;
					}
				});
			}

			// Perform the login via the JAAS LoginContext
			CallbackHandler handler = new AuthenticationCallbackHandler(username, password);
			LoginContext context = new LoginContext(name, _subject, handler, configuration);
			context.login();
		} catch (Exception exc) {
			// TODO - Address Audit/Log/Debug handling options
			if (exc instanceof LoginException)
				throw (LoginException) exc;

			throw (LoginException) new LoginException(
					"AuthenticationService: "+ exc.getMessage()).initCause(exc);
		}

		// Return the Subject that was logged in
		return _subject;
	}

	@Override
	public Subject impersonate(String user, String[] groups, Subject subject, boolean virtual)
			throws LoginException {
		// Use the supplied Subject or create a new Subject
		Subject _subject = subject;
		if (_subject == null)
			_subject = new Subject();

		try {
			// TODO - Add support for virtual = false
			if (!virtual) {
				throw new UnsupportedOperationException(
						"Use of non-virtual parameter is not supported");
			}
			
			// Build the Subject
			if ((user != null) && (!user.isEmpty())) {
				final Subject s = _subject;
				final String userName = user;
				final String[] groupNames = groups;
				AppservAccessController.doPrivileged(new PrivilegedAction<Object>() {
					public Object run() {
						Set<Principal> principals = s.getPrincipals();
						principals.add(new PrincipalImpl(userName));
						if (groupNames != null) {
							for (int i = 0; i < groupNames.length; i++)
								principals.add(new Group(groupNames[i]));
						}
						return null;
					}
				});
			}
		} catch (Exception exc) {
			// TODO - Address Audit/Log/Debug handling options
			throw (LoginException) new LoginException(
					"AuthenticationService: "+ exc.getMessage()).initCause(exc);
		}

		// Return the impersonated Subject
		return _subject;
	}

	/**
	 * Handle lookup of authentication service configuration and initialization.
	 * If no service is configured the service run-time will throw exceptions.
	 * 
	 * Addresses alternate configuration handling until adopt @Proxiable support. 
	 */
	@Override
	public void postConstruct() {
		// TODO - Dynamic configuration changes?
		initialize(AuthenticationServiceFactory.getAuthenticationServiceConfiguration(domain));
	}

	/**
	 * Convert the String setting to the JAAS LoginModuleControlFlag.
	 * An unknown or null flag value is treated as LoginModuleControlFlag.REQUIRED.
	 */
	private LoginModuleControlFlag getLoginModuleControlFlag(String controlFlag) {
		LoginModuleControlFlag flag = LoginModuleControlFlag.REQUIRED;
		// TODO - Handle invalid control flag?
		if (controlFlag != null) {
			if ("required".equalsIgnoreCase(controlFlag))
				return flag;
			// Check additional flag types
			if ("sufficient".equalsIgnoreCase(controlFlag))
				flag = LoginModuleControlFlag.SUFFICIENT;
			else if ("optional".equalsIgnoreCase(controlFlag))
				flag = LoginModuleControlFlag.OPTIONAL;
			else if ("requisite".equalsIgnoreCase(controlFlag))
				flag = LoginModuleControlFlag.REQUISITE;
		}
		return flag;
	}

	/**
	 * The Authentication Service CallbackHandler implementation.
	 *  
	 * Facilitates use of the standard JAAS NameCallback and PasswordCallback.
	 */
	private class AuthenticationCallbackHandler implements CallbackHandler {
		private String user;
		private char[] pass;

		public AuthenticationCallbackHandler(String username, char[] password) {
			user = username;
			pass = password;
		}

		@Override
		public void handle(Callback[] callbacks) throws UnsupportedCallbackException {
			for (int i = 0; i < callbacks.length; i++) {
				if (callbacks[i] instanceof NameCallback)
					((NameCallback) callbacks[i]).setName(user);
				else if (callbacks[i] instanceof PasswordCallback)
					((PasswordCallback) callbacks[i]).setPassword(pass);
				else
					throw new UnsupportedCallbackException(callbacks[i],
							"AuthenticationCallbackHandler: Unrecognized Callback");
			}
		}
	}

	/**
	 * The Authentication Service JAAS Configuration implementation.
	 *  
	 * Facilitates the use of JAAS LoginContext with Authentication Service LoginModule configuration.
	 */
	private class AuthenticationJaasConfiguration extends Configuration {
		private String configurationName;
		private AppConfigurationEntry[] lmEntries;

		/**
		 * Create the JAAS Configuration instance used by the Authentication Service.
		 */
		private AuthenticationJaasConfiguration(String name, ArrayList<AppConfigurationEntry> entries) {
			configurationName = name;
			lmEntries = entries.toArray(new AppConfigurationEntry[entries.size()]);
		}

		/**
		 * Get the LoginModule(s) specified by the Authentication Service configuration.
		 */
		@Override
		public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
			if (configurationName.equals(name))
				return lmEntries;
			else
				return null;
		}
	}
}
