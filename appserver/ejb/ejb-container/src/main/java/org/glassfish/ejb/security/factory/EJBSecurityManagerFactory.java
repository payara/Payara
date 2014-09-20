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

/*
 * EJBSecurityManagerFactory.java
 *
 * Created on June 9, 2003, 5:42 PM
 */

package org.glassfish.ejb.security.factory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.glassfish.ejb.security.application.EjbSecurityProbeProvider;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

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

    private static Logger _logger = null;

    static {
        _logger = LogDomains.getLogger(EJBSecurityManagerFactory.class, LogDomains.SECURITY_LOGGER);
    }

    @Inject
    InvocationManager invMgr;
     

    @Inject
    AppServerAuditManager auditManager;

    private EjbSecurityProbeProvider probeProvider = new EjbSecurityProbeProvider();
    /**
     * Creates a new instance of EJBSecurityManagerFactory
     */
    public EJBSecurityManagerFactory() {
    }

    /*
    public SecurityManager getSecurityManager(String contextId) {
        if (_poolHas(contextId)) {
            return (SecurityManager) _poolGet(contextId);
        }
        return null;
    }

    public EJBSecurityManager createSecurityManager(Descriptor descriptor) {
        EJBSecurityManager ejbSM = null;
        String contextId = null;
        String appName = null;
        try {

            if (descriptor == null || !(descriptor instanceof EjbDescriptor)) {
                throw new IllegalArgumentException("Illegal Deployment Descriptor Information.");
            }
            EjbDescriptor ejbdes = (EjbDescriptor) descriptor;
            ejbSM = new EJBSecurityManager(ejbdes, invMgr);

            // if the descriptor is not a EjbDescriptor the EJBSM will 
            // throw an exception. So the following will always work.
            appName = ejbdes.getApplication().getRegistrationName();
            contextId = EJBSecurityManager.getContextID(ejbdes);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "[EJB-Security] EJB Security:Creating EJBSecurityManager for contextId = "
                                + contextId);
            }

        } catch (Exception e) {
            _logger.log(Level.FINE,
                    "[EJB-Security] FATAl Exception. Unable to create EJBSecurityManager: "
                            + e.getMessage());
            throw new RuntimeException(e);
        }

        synchronized (CONTEXT_ID) {
            List lst = (List) CONTEXT_ID.get(appName);
            if (lst == null) {
                lst = new ArrayList();
                CONTEXT_ID.put(appName, lst);
            }
            if (!lst.contains(contextId)) {
                lst.add(contextId);
            }
        }

        _poolPut(contextId, ejbSM);
        return ejbSM;
    }

    public String[] getAndRemoveContextIdForEjbAppName(String appName) {
        synchronized (CONTEXT_ID) {
            List contextId = (List) CONTEXT_ID.get(appName);
            if (contextId == null) {
                return null;
            }
            String[] rvalue = new String[contextId.size()];
            rvalue = (String[]) contextId.toArray(rvalue);

            CONTEXT_ID.remove(appName);
            return rvalue;
        }
    }*/
     // stores the context ids to appnames for apps
    private Map<String, ArrayList<String>> CONTEXT_IDS =
            new HashMap<String, ArrayList<String>>();
    private Map<String, Map<String, EJBSecurityManager>> SECURITY_MANAGERS =
            new HashMap<String, Map<String, EJBSecurityManager>>();

    public <T> EJBSecurityManager getManager(String ctxId, String name, boolean remove) {
        return getManager(SECURITY_MANAGERS, ctxId, name, remove);
    }

    public  <T> ArrayList<EJBSecurityManager> 
            getManagers(String ctxId, boolean remove) {
        return getManagers(SECURITY_MANAGERS, ctxId, remove);
    }

    public  <T> ArrayList<EJBSecurityManager> 
            getManagersForApp(String appName, boolean remove) {
        return getManagersForApp(SECURITY_MANAGERS, CONTEXT_IDS, appName, remove);
    }

    public <T> String[] getContextsForApp(String appName, boolean remove) {
        return getContextsForApp(CONTEXT_IDS, appName, remove);
    }

    public <T> void addManagerToApp(String ctxId, String name,
            String appName, EJBSecurityManager manager) {
        addManagerToApp(SECURITY_MANAGERS, CONTEXT_IDS, ctxId, name, appName, manager);
    }

    public EJBSecurityManager createManager(EjbDescriptor ejbDesc,
            boolean register) {
        String ctxId = EJBSecurityManager.getContextID(ejbDesc);
        String ejbName = ejbDesc.getName();
        EJBSecurityManager manager = null;
        if (register) {
            manager = getManager(ctxId, ejbName, false);
        }
        if (manager == null || !register) {
            try {
                probeProvider.securityManagerCreationStartedEvent(ejbName);
                manager = new EJBSecurityManager(ejbDesc, this.invMgr, this);
                probeProvider.securityManagerCreationEndedEvent(ejbName);
                if (register) {
                          
                    String appName = ejbDesc.getApplication().getRegistrationName();
                    addManagerToApp(ctxId, ejbName, appName, manager);
                    probeProvider.securityManagerCreationEvent(ejbName);
                }
            } catch (Exception ex) {
                _logger.log(Level.FINE, "[EJB-Security] FATAL Exception. Unable to create EJBSecurityManager: " + ex.getMessage());
                throw new RuntimeException(ex);
            }
        }
        return manager;
    }
    
    public final AppServerAuditManager getAuditManager() {
        return this.auditManager;
    }
}
