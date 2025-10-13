/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]
package org.glassfish.ejb.security.factory;

import static java.util.logging.Level.FINE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.glassfish.ejb.security.application.EjbSecurityProbeProvider;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.security.ee.audit.AppServerAuditManager;
import com.sun.enterprise.security.factory.SecurityManagerFactory;
import com.sun.logging.LogDomains;

/**
 * EJB Security Manager Factory Implementation
 *
 * @author Harpreet Singh
 */
@Service
@Singleton
public final class EJBSecurityManagerFactory extends SecurityManagerFactory {

    private static Logger _logger = LogDomains.getLogger(EJBSecurityManagerFactory.class, LogDomains.SECURITY_LOGGER, false);

    @Inject
    private InvocationManager invocationManager;

    @Inject
    private AppServerAuditManager auditManager;

    private EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();

    // stores the context ids to appnames for apps
    private Map<String, List<String>> CONTEXT_IDS = new HashMap<>();
    private Map<String, Map<String, EJBSecurityManager>> SECURITY_MANAGERS = new HashMap<>();
    
    public EJBSecurityManager createManager(EjbDescriptor ejbDescriptor, boolean register) {
        String contextId = EJBSecurityManager.getContextID(ejbDescriptor);
        String ejbName = ejbDescriptor.getName();
        EJBSecurityManager manager = null;
        
        if (register) {
            manager = getManager(contextId, ejbName, false);
        }
        
        if (manager == null || !register) {
            try {
                probeProvider.securityManagerCreationStartedEvent(ejbName);
                manager = new EJBSecurityManager(ejbDescriptor, invocationManager, this);
                probeProvider.securityManagerCreationEndedEvent(ejbName);
                
                if (register) {
                    String appName = ejbDescriptor.getApplication().getRegistrationName();
                    addManagerToApp(contextId, ejbName, appName, manager);
                    probeProvider.securityManagerCreationEvent(ejbName);
                }
            } catch (Exception ex) {
                _logger.log(FINE, "[EJB-Security] FATAL Exception. Unable to create EJBSecurityManager: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
        
        return manager;
    }

    public <T> EJBSecurityManager getManager(String ctxId, String name, boolean remove) {
        return getManager(SECURITY_MANAGERS, ctxId, name, remove);
    }

    public <T> List<EJBSecurityManager> getManagers(String ctxId, boolean remove) {
        return getManagers(SECURITY_MANAGERS, ctxId, remove);
    }

    public <T> List<EJBSecurityManager> getManagersForApp(String appName, boolean remove) {
        return getManagersForApp(SECURITY_MANAGERS, CONTEXT_IDS, appName, remove);
    }

    public <T> String[] getContextsForApp(String appName, boolean remove) {
        return getContextsForApp(CONTEXT_IDS, appName, remove);
    }

    public <T> void addManagerToApp(String ctxId, String name, String appName, EJBSecurityManager manager) {
        addManagerToApp(SECURITY_MANAGERS, CONTEXT_IDS, ctxId, name, appName, manager);
    }

    public final AppServerAuditManager getAuditManager() {
        return auditManager;
    }
}
