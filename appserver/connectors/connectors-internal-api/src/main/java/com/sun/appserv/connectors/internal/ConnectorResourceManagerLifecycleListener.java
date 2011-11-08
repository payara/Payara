/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.appserv.connectors.internal;

import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorDescriptorProxy;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.logging.LogDomains;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.resources.listener.ResourceManagerLifecycleListener;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Inhabitant;

import javax.naming.NamingException;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;


@Service
public class ConnectorResourceManagerLifecycleListener implements ResourceManagerLifecycleListener {

    @Inject
    private GlassfishNamingManager namingMgr;

    @Inject
    private Habitat habitat;

    @Inject
    private Domain domain;

    @Inject
    private Applications applications;

    @Inject
    private Habitat connectorRuntimeHabitat;

    private ConnectorRuntime runtime;

    private static final Logger logger =
            LogDomains.getLogger(ConnectorRuntime.class, LogDomains.RESOURCE_BUNDLE);

    private void bindConnectorDescriptors() {
        for(String rarName : ConnectorConstants.systemRarNames){
            bindConnectorDescriptorProxies(rarName);
        }
    }

    private void bindConnectorDescriptorProxies(String rarName) {
        //these proxies are needed as appclient container may lookup descriptors
        String jndiName = ConnectorsUtil.getReservePrefixedJNDINameForDescriptor(rarName);
        ConnectorDescriptorProxy proxy = habitat.getComponent(ConnectorDescriptorProxy.class);
        proxy.setJndiName(jndiName);
        proxy.setRarName(rarName);
        try {
            namingMgr.publishObject(jndiName, proxy, true);
        } catch (NamingException e) {
            Object[] params = new Object[]{rarName, e};
            logger.log(Level.WARNING, "resources.resource-manager.connector-descriptor.bind.failure", params);
        }
    }

    private ConnectorRuntime getConnectorRuntime() {
        if(runtime == null){
            runtime = connectorRuntimeHabitat.getComponent(ConnectorRuntime.class, null);
        }
        return runtime;
    }

    /**
     * Check whether connector-runtime is initialized.
     * @return boolean representing connector-runtime initialization status.
     */
    public boolean isConnectorRuntimeInitialized() {
        Collection<Inhabitant<? extends ConnectorRuntime>> inhabitants =
                connectorRuntimeHabitat.getInhabitants(ConnectorRuntime.class);
        for(Inhabitant inhabitant : inhabitants){
            // there will be only one implementation of connector-runtime
            return inhabitant.isActive();
        }
        return true; // to be safe
    }

    public void resourceManagerLifecycleEvent(EVENT event){
        if(EVENT.STARTUP.equals(event)){
            resourceManagerStarted();
        }else if(EVENT.SHUTDOWN.equals(event)){
            resourceManagerShutdown();
        }
    }

    public void resourceManagerStarted() {
        bindConnectorDescriptors();
    }

    public void resourceManagerShutdown() {
        if (isConnectorRuntimeInitialized()) {
            ConnectorRuntime cr = getConnectorRuntime();
            if (cr != null) {
                // clean up will take care of any system RA resources, pools
                // (including pools via datasource-definition)
                cr.cleanUpResourcesAndShutdownAllActiveRAs();
            }
        } else {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("ConnectorRuntime not initialized, hence skipping " +
                    "resource-adapters shutdown, resources, pools cleanup");
            }
        }
    }
}
