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
package org.glassfish.security.services.impl;

import java.security.AccessController;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.security.services.common.PrivilegedLookup;
import org.glassfish.security.services.common.Secure;
import org.glassfish.security.services.common.StateManager;
import org.glassfish.security.services.common.SecurityScope;

import org.glassfish.security.services.api.authentication.AuthenticationService;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * The factory of AuthenticationService instances used by the SecurityScopeContext.
 */
@Singleton
@Secure(accessPermissionName = "security/service/authentication")
public class AuthenticationServiceFactory extends ServiceFactory implements Factory<AuthenticationService> {
    
    @Inject
    private StateManager manager;

    @Inject
    private ServiceLocator serviceLocator;

    @SecurityScope
    @Override
    public AuthenticationService provide() {
        String currentState = manager.getCurrent();

        // Get Service Instance
        AuthenticationService atnService = AccessController.doPrivileged( 
                new PrivilegedLookup<AuthenticationService>(
                        serviceLocator, AuthenticationService.class));

        // Get Service Configuration
        org.glassfish.security.services.config.AuthenticationService atnConfiguration =
            serviceLocator.getService(org.glassfish.security.services.config.AuthenticationService.class,currentState);

        // Initialize Service
        atnService.initialize(atnConfiguration);

        return atnService;
    }

    @Override
    public void dispose(AuthenticationService instance) {
    }

    /**
     * Helper function to obtain the Authentication Service configuration from the Domain.
     */
    public static org.glassfish.security.services.config.AuthenticationService getAuthenticationServiceConfiguration(Domain domain) {
		org.glassfish.security.services.config.AuthenticationService atnConfiguration =
        	ServiceFactory.getSecurityServiceConfiguration(
        			domain, org.glassfish.security.services.config.AuthenticationService.class);
        return atnConfiguration;
	}
}
