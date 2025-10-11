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
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.web.integration;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.factory.SecurityManagerFactory;
import com.sun.enterprise.security.jacc.JaccWebAuthorizationManager;
import com.sun.enterprise.security.jacc.context.PolicyContextHandlerImpl;
import com.sun.enterprise.security.jacc.context.PolicyContextRegistration;

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
import org.jvnet.hk2.annotations.Service;

import static com.sun.logging.LogDomains.SECURITY_LOGGER;
import static java.util.logging.Level.CONFIG;

/**
 * @author JeanFrancois Arcand
 * @author Harpreet Singh
 */
@Service
@Singleton
public class WebSecurityManagerFactory extends SecurityManagerFactory {

    private static final Logger logger = Logger.getLogger(SECURITY_LOGGER);

    private final WebSecurityDeployerProbeProvider probeProvider = new WebSecurityDeployerProbeProvider();

    public final PolicyContextHandlerImpl pcHandlerImpl = (PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance();

    public final Map<String, Principal> adminPrincipalsPerApp = new ConcurrentHashMap<>();
    public final Map<String, Principal> adminGroupsPerApp = new ConcurrentHashMap<>();

    // Stores the Context IDs to application names for standalone web applications
    private final Map<String, List<String>> CONTEXT_IDS = new HashMap<>();
    private final Map<String, Map<String, JaccWebAuthorizationManager>> SECURITY_MANAGERS = new HashMap<>();

    public WebSecurityManagerFactory() {
        // Registers the JACC policy handlers, which provide objects JACC Providers and other code can use
        PolicyContextRegistration.registerPolicyHandlers();
    }

    public JaccWebAuthorizationManager createManager(WebBundleDescriptor webBundleDescriptor, boolean register, ServerContext context) {
        String contextId = JaccWebAuthorizationManager.getContextID(webBundleDescriptor);

        JaccWebAuthorizationManager manager = null;
        if (register) {
            manager = getManager(contextId, null, false);
        }

        if (manager == null || !register) {
            try {
                // Create a new JaccWebAuthorizationManager for this context
                probeProvider.securityManagerCreationStartedEvent(webBundleDescriptor.getModuleID());

                // As "side-effect" of constructing the manager, the web constraints in the web bundle
                // descriptor will be translated to permissions and loaded into a JACC policy configuration
                manager = new JaccWebAuthorizationManager(webBundleDescriptor, context, this, register);

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

    public JaccWebAuthorizationManager getManager(String ctxId, String name, boolean remove) {
        return getManager(SECURITY_MANAGERS, ctxId, name, remove);
    }

    public <T> ArrayList<JaccWebAuthorizationManager> getManagers(String ctxId, boolean remove) {
        return getManagers(SECURITY_MANAGERS, ctxId, remove);
    }

    public <T> List<JaccWebAuthorizationManager> getManagersForApp(String appName, boolean remove) {
        return getManagersForApp(SECURITY_MANAGERS, CONTEXT_IDS, appName, remove);
    }

    public <T> String[] getContextsForApp(String appName, boolean remove) {
        return getContextsForApp(CONTEXT_IDS, appName, remove);
    }

    public <T> void addManagerToApp(String contextId, String name, String appName, JaccWebAuthorizationManager manager) {
        addManagerToApp(SECURITY_MANAGERS, CONTEXT_IDS, contextId, name, appName, manager);
    }


    // ### PrincipalGroupFactoryImpl backing

    public void addAdminPrincipal(String username, String realmName, Principal principal) {
        adminPrincipalsPerApp.put(realmName + username, principal);
    }

    public void addAdminGroup(String group, String realmName, Principal principal) {
        adminGroupsPerApp.put(realmName + group, principal);
    }

    public Principal getAdminPrincipal(String username, String realmName) {
        return adminPrincipalsPerApp.get(realmName + username);
    }

    public Principal getAdminGroup(String group, String realmName) {
        return adminGroupsPerApp.get(realmName + group);
    }

}
