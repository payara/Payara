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

import java.util.List;

import org.glassfish.security.services.config.SecurityConfigurations;
import org.glassfish.security.services.config.SecurityConfiguration;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * The base security service factory class.
 */
public class ServiceFactory {
	/**
	 * Get the security service configuration for the specified service type.
	 * 
	 * Attempt to obtain the service configuration marked as default
	 * otherwise use the first configured service instance.
	 * 
	 * @param domain The current Domain configuration object
	 * @param type The type of the security service configuration
	 * 
	 * @return null when no service configurations are found
	 */
	public static <T extends SecurityConfiguration> T getSecurityServiceConfiguration(Domain domain, Class<T> type) {
		T config = null;

		// Look for security service configurations
		SecurityConfigurations secConfigs = domain.getExtensionByType(SecurityConfigurations.class);
		if (secConfigs != null) {
			// Look for the service configuration marked default
			config = secConfigs.getDefaultSecurityServiceByType(type);
			if (config == null) {
				// Obtain the first service configuration listed
				List<T> configs = secConfigs.getSecurityServicesByType(type);
				if (!configs.isEmpty())
					config = configs.get(0);
			}
		}

		// Return the service configuration
		return config;
	}
}
