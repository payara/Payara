/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2024] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.ee.web.integration;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.ee.SecurityUtil;
import com.sun.enterprise.security.factory.SecurityManagerFactory;
import com.sun.enterprise.security.ee.authorization.WebAuthorizationManagerService;

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.security.jacc.PolicyContextException;

import org.glassfish.internal.api.ServerContext;
import org.glassfish.security.common.Group;
import org.glassfish.security.common.UserPrincipal;
import org.jvnet.hk2.annotations.Service;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;

/**
 * @author JeanFrancois Arcand
 * @author Harpreet Singh
 */
@Service
@Singleton
public class WebSecurityManagerFactory extends SecurityManagerFactory {

    private static final Logger logger = Logger.getLogger(SECURITY_LOGGER);

    private final WebSecurityDeployerProbeProvider probeProvider = new WebSecurityDeployerProbeProvider();

    private final Map<String, UserPrincipal> adminPrincipals = new ConcurrentHashMap<>();
    private final Map<String, Group> adminGroups = new ConcurrentHashMap<>();

    // Stores the Context IDs to application names for standalone web applications
    private final Map<String, List<String>> CONTEXT_IDS = new HashMap<>();
    private final Map<String, Map<String, WebAuthorizationManagerService>> SECURITY_MANAGERS = new HashMap<>();

    public WebAuthorizationManagerService createManager(WebBundleDescriptor webBundleDescriptor, boolean register, ServerContext context) {
        String contextId = SecurityUtil.getContextID(webBundleDescriptor);

        WebAuthorizationManagerService manager = null;
        if (register) {
            manager = getManager(contextId, false);
        }

        if (manager == null || !register) {
            try {
                // Create a new WebAuthorizationManagerService for this context
                probeProvider.securityManagerCreationStartedEvent(webBundleDescriptor.getModuleID());

                // As "side-effect" of constructing the manager, the web constraints in the web bundle
                // descriptor will be translated to permissions and loaded into a JACC policy configuration
                manager = new WebAuthorizationManagerService(webBundleDescriptor, register);

                probeProvider.securityManagerCreationEndedEvent(webBundleDescriptor.getModuleID());

                if (register) {
                    addManagerToApp(contextId, null, webBundleDescriptor.getApplication().getRegistrationName(), manager);
                    probeProvider.securityManagerCreationEvent(contextId);
                }
            } catch (PolicyContextException e) {
                throw new IllegalStateException("Unable to create WebSecurityManager", e);
            }
        }

        return manager;
    }

    public <T> void addManagerToApp(String ctxId, String name, String appName, WebAuthorizationManagerService manager) {
        addManagerToApp(SECURITY_MANAGERS, CONTEXT_IDS, ctxId, name, appName, manager);
    }

    public WebAuthorizationManagerService getManager(String ctxId, String name, boolean remove) {
        return getManager(SECURITY_MANAGERS, ctxId, name, remove);
    }

    public WebAuthorizationManagerService getManager(String contextId) {
        return getManager(SECURITY_MANAGERS, contextId, null, false);
    }

    public WebAuthorizationManagerService getManager(String contextId, boolean remove) {
        return getManager(SECURITY_MANAGERS, contextId, null, remove);
    }

    public <T> ArrayList<WebAuthorizationManagerService> getManagers(String contextId, boolean remove) {
        return getManagers(SECURITY_MANAGERS, contextId, remove);
    }
    
    public <T> ArrayList<WebAuthorizationManagerService> getManagersForApp(String appName, boolean remove) {
        return getManagersForApp(SECURITY_MANAGERS, CONTEXT_IDS, appName, remove);
    }

    public <T> String[] getContextsForApp(String appName, boolean remove) {
        return getContextsForApp(CONTEXT_IDS, appName, remove);
    }

    public UserPrincipal getAdminPrincipal(String username, String realmName) {
        // FIXME: can be hacked: "ab+cd" = "a+bcd"
        return adminPrincipals.get(realmName + username);
    }

    public void putAdminPrincipal(String realmName, UserPrincipal principal) {
        // FIXME: can be hacked: "ab+cd" = "a+bcd"
        adminPrincipals.put(realmName + principal.getName(), principal);
    }

    public Group getAdminGroup(String group, String realmName) {
        // FIXME: can be hacked: "ab+cd" = "a+bcd"
        return adminGroups.get(realmName + group);
    }

    public void putAdminGroup(String group, String realmName, Group principal) {
        // FIXME: can be hacked: "ab+cd" = "a+bcd"
        adminGroups.put(realmName + group, principal);
    }
    
}
