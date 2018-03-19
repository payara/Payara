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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.web.integration;

import java.util.HashMap;
import java.util.Map;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.security.WebSecurityDeployerProbeProvider;
import com.sun.enterprise.security.authorize.PolicyContextHandlerImpl;
import com.sun.enterprise.security.factory.SecurityManagerFactory;
import org.glassfish.internal.api.ServerContext;
import java.util.logging.*;
import static com.sun.logging.LogDomains.SECURITY_LOGGER;

import java.security.Principal;
import java.util.ArrayList;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.security.jacc.PolicyContextHandler;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

/**
 * @author JeanFrancois Arcand
 * @author Harpreet Singh
 */
@Service
@Singleton
public class WebSecurityManagerFactory extends SecurityManagerFactory {

    private static Logger logger = Logger.getLogger(SECURITY_LOGGER);
    private WebSecurityDeployerProbeProvider probeProvider = new WebSecurityDeployerProbeProvider();

    public WebSecurityManagerFactory() {
        registerPolicyHandlers();
    }

    final PolicyContextHandlerImpl pcHandlerImpl = (PolicyContextHandlerImpl) PolicyContextHandlerImpl.getInstance();

    final Map ADMIN_PRINCIPAL = new HashMap();
    final Map ADMIN_GROUP = new HashMap();

    public Principal getAdminPrincipal(String username, String realmName) {
        return (Principal) ADMIN_PRINCIPAL.get(realmName + username);
    }

    public Principal getAdminGroup(String group, String realmName) {
        return (Principal) ADMIN_GROUP.get(realmName + group);
    }

    private static void registerPolicyHandlers() {
        try {
            PolicyContextHandler pch = PolicyContextHandlerImpl.getInstance();
            PolicyContext.registerHandler(PolicyContextHandlerImpl.ENTERPRISE_BEAN, pch, true);
            PolicyContext.registerHandler(PolicyContextHandlerImpl.SUBJECT, pch, true);
            PolicyContext.registerHandler(PolicyContextHandlerImpl.EJB_ARGUMENTS, pch, true);
            PolicyContext.registerHandler(PolicyContextHandlerImpl.SOAP_MESSAGE, pch, true);
            PolicyContext.registerHandler(PolicyContextHandlerImpl.HTTP_SERVLET_REQUEST, pch, true);
            PolicyContext.registerHandler(PolicyContextHandlerImpl.REUSE, pch, true);
        } catch (PolicyContextException ex) {
            Logger.getLogger(WebSecurityManagerFactory.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // stores the context ids to appnames for standalone web apps
    private Map<String, ArrayList<String>> CONTEXT_IDS = new HashMap<String, ArrayList<String>>();
    private Map<String, Map<String, WebSecurityManager>> SECURITY_MANAGERS = new HashMap<String, Map<String, WebSecurityManager>>();

    public WebSecurityManager getManager(String ctxId, String name, boolean remove) {
        return getManager(SECURITY_MANAGERS, ctxId, name, remove);
    }

    public <T> ArrayList<WebSecurityManager> getManagers(String ctxId, boolean remove) {
        return getManagers(SECURITY_MANAGERS, ctxId, remove);
    }

    public <T> ArrayList<WebSecurityManager> getManagersForApp(String appName, boolean remove) {
        return getManagersForApp(SECURITY_MANAGERS, CONTEXT_IDS, appName, remove);
    }

    public <T> String[] getContextsForApp(String appName, boolean remove) {
        return getContextsForApp(CONTEXT_IDS, appName, remove);
    }

    public <T> void addManagerToApp(String ctxId, String name, String appName, WebSecurityManager manager) {
        addManagerToApp(SECURITY_MANAGERS, CONTEXT_IDS, ctxId, name, appName, manager);
    }

    public WebSecurityManager createManager(WebBundleDescriptor wbd, boolean register, ServerContext context) {
        String ctxId = WebSecurityManager.getContextID(wbd);
        WebSecurityManager manager = null;
        if (register) {
            manager = getManager(ctxId, null, false);
        }

        if (manager == null || !register) {
            try {
                probeProvider.securityManagerCreationStartedEvent(wbd.getModuleID());
                manager = new WebSecurityManager(wbd, context, this, register);
                probeProvider.securityManagerCreationEndedEvent(wbd.getModuleID());
                if (register) {

                    String appName = wbd.getApplication().getRegistrationName();
                    addManagerToApp(ctxId, null, appName, manager);
                    probeProvider.securityManagerCreationEvent(ctxId);
                }
            } catch (javax.security.jacc.PolicyContextException e) {
                logger.log(Level.FINE, "[Web-Security] FATAL Exception. Unable to create WebSecurityManager: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return manager;
    }
}
