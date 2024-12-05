/*
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.security.ee.web.integration;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.runtime.common.PrincipalNameDescriptor;
import com.sun.enterprise.deployment.runtime.common.SecurityRoleMapping;
import com.sun.enterprise.deployment.runtime.common.wls.SecurityRoleAssignment;
import com.sun.enterprise.deployment.runtime.web.SunWebApp;
import com.sun.enterprise.deployment.web.LoginConfiguration;


import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.ee.authorization.WebAuthorizationManagerService;
import jakarta.security.jacc.PolicyContextException;

import java.util.List;

import java.util.logging.Logger;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.UserNameAndPassword;


import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.FINE;
import static org.glassfish.api.web.Constants.ADMIN_VS;

/**

 * <p>
 * The GlassFish authorization service is a thin wrapper over the Exousia authorization service that implements
 * Jakarta Authorization.
 *
 * @author Jean-Francois Arcand
 * @author Harpreet Singh.
 */
public class WebSecurityManager {
    private static final Logger logger = Logger.getLogger(SECURITY_LOGGER);
    
    private final WebAuthorizationManagerService authorizationService;

    private final WebSecurityManagerFactory webSecurityManagerFactory;
    private final String contextId;

    WebSecurityManager(WebBundleDescriptor webBundleDescriptor, ServerContext serverContext, WebSecurityManagerFactory webSecurityManagerFactory, boolean register) throws PolicyContextException {
        // Sets-up Jakarta Authorization
        this.authorizationService = new WebAuthorizationManagerService(webBundleDescriptor, register);

        this.webSecurityManagerFactory = webSecurityManagerFactory;
        this.contextId = SecurityUtil.getContextID(webBundleDescriptor);

        if (ADMIN_VS.equals(getVirtualServers(webBundleDescriptor, serverContext))) {
            handleAdminVirtualServer(webBundleDescriptor, webSecurityManagerFactory);
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "[Web-Security] Context id (id under which  WEB component in application will be created) = {0}", contextId);
            logger.log(FINE, "[Web-Security] Codebase (module id for web component) {0}", removeSpaces(contextId));
        }
    }

    /**
     * @return the authorizationService
     */
    public WebAuthorizationManagerService getAuthorizationService() {
        return authorizationService;
    }
    
    /**
     * Analogous to destroy, except does not remove links from Policy Context, and does not remove context_id from role mapper
     * factory. Used to support Policy Changes that occur via ServletContextListener.
     *
     * @throws PolicyContextException
     */
    public void release() throws PolicyContextException {
        authorizationService.release();
        webSecurityManagerFactory.getManager(contextId, true);
    }

    public void destroy() throws PolicyContextException {
        authorizationService.destroy();
        webSecurityManagerFactory.getManager(contextId, true);
    }



    // ### Private methods ###

    private void handleAdminVirtualServer(WebBundleDescriptor webBundleDescriptor, WebSecurityManagerFactory webSecurityManagerFactory) {
        LoginConfiguration loginConfiguration = webBundleDescriptor.getLoginConfiguration();
        if (loginConfiguration == null) {
            return;
        }

        String realmName = loginConfiguration.getRealmName();
        SunWebApp sunDescriptor = webBundleDescriptor.getSunDescriptor();
        if (sunDescriptor == null) {
            return;
        }

        SecurityRoleMapping[] sunRoleMappings = sunDescriptor.getSecurityRoleMapping();
        if (sunRoleMappings != null) {
            for (SecurityRoleMapping roleMapping : sunRoleMappings) {
                for (PrincipalNameDescriptor principal : roleMapping.getPrincipalNames()) {
                    // We keep just a name here
                    webSecurityManagerFactory.putAdminPrincipal(realmName,
                        new UserNameAndPassword(principal.getName()));
                }
                for (String group : roleMapping.getGroupNames()) {
                    webSecurityManagerFactory.putAdminGroup(group, realmName, new Group(group));
                }
            }
        }

        SecurityRoleAssignment[] sunRoleAssignments = sunDescriptor.getSecurityRoleAssignments();
        if (sunRoleAssignments != null) {
            for (SecurityRoleAssignment roleAssignment : sunRoleAssignments) {
                List<String> principals = roleAssignment.getPrincipalNames();
                if (roleAssignment.isExternallyDefined()) {
                    webSecurityManagerFactory.putAdminGroup(roleAssignment.getRoleName(), realmName,
                        new Group(roleAssignment.getRoleName()));
                    continue;
                }
                for (String principal : principals) {
                    webSecurityManagerFactory.putAdminPrincipal(realmName, new UserNameAndPassword(principal));
                }
            }
        }
    }

    /**
     * Virtual servers are maintained in the reference contained in Server element. First, we need to find the server and then get
     * the virtual server from the correct reference
     *
     * @param applicationName Name of the app to get vs
     *
     * @return virtual servers as a string (separated by space or comma)
     */
    private String getVirtualServers(WebBundleDescriptor webBundleDescriptor, ServerContext serverContext) {
        String applicationName = webBundleDescriptor.getApplication().getRegistrationName();
        Server server = serverContext.getDefaultServices().getService(Server.class);

        for (ApplicationRef appplicationRef : server.getApplicationRef()) {
            if (appplicationRef.getRef().equals(applicationName)) {
                return appplicationRef.getVirtualServers();
            }
        }

        return null;
    }

    private static String removeSpaces(String withSpaces) {
        return withSpaces.replace(' ', '_');
    }

}
