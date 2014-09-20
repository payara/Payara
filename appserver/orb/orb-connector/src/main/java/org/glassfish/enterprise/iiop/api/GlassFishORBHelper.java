/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.api;

import javax.inject.Inject;
import javax.inject.Provider;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.ORB;
import org.omg.PortableInterceptor.ServerRequestInfo;

import org.glassfish.api.admin.ProcessEnvironment;

import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.internal.api.ORBLocator;

import java.util.Properties;
import java.rmi.Remote;
import java.nio.channels.SelectableChannel;

import org.glassfish.hk2.api.PostConstruct;

import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.EventListener;
import com.sun.logging.LogDomains;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class exposes any orb/iiop functionality needed by modules in the app server.
 * This prevents modules from needing any direct dependencies on the orb-iiop module.
 * @author Mahesh Kannan
 *         Date: Jan 17, 2009
 */
@Service
public class GlassFishORBHelper implements PostConstruct, ORBLocator {

    @Inject
    private ServiceLocator services;

    @Inject
    private ProcessEnvironment processEnv;

    private static final Logger _logger =
        LogDomains.getLogger(GlassFishORBHelper.class, LogDomains.CORBA_LOGGER);

    private volatile ORB orb = null ;

    private ProtocolManager protocolManager = null ;

    private ORBLazyServiceInitializer lazyServiceInitializer;

    private SelectableChannelDelegate selectableChannelDelegate;

    @Inject
    Provider<ProtocolManager> protocolManagerProvider;


    @Inject
    Provider<GlassfishNamingManager> glassfishNamingManagerProvider;

    @Inject
    private Provider<Events> eventsProvider;

    //@Inject
    private GlassFishORBFactory orbFactory;

    public void postConstruct() {
        orbFactory = services.getService(GlassFishORBFactory.class);
    }


    public void onShutdown() {
        _logger.log(Level.FINE, ("ORB Shutdown started"));
        orb.destroy();
    }

    public synchronized void setORB( ORB orb ) {
        this.orb = orb ;
        
        if (orb != null) {
            EventListener glassfishEventListener = new org.glassfish.api.event.EventListener() {

                public void event(org.glassfish.api.event.EventListener.Event event) {
                if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                        onShutdown();
                    }
                }
            };
            eventsProvider.get().register(glassfishEventListener);
        }
    }

    /**
     * Get or create the default orb.  This can be called for any process type.  However,
     * protocol manager and CosNaming initialization only take place for the Server.
     */
    public ORB getORB() {
        // Use a volatile double-checked locking idiom here so that we can publish
        // a partly-initialized ORB early, so that lazy init can come into getORB() 
        // and allow an invocation to the transport to complete.
        if (orb == null) {

            synchronized( this ) {
                if (orb == null) {
                    try {
                        final boolean isServer = processEnv.getProcessType().isServer() ;

                        Properties props = new Properties();
                        props.setProperty( GlassFishORBFactory.ENV_IS_SERVER_PROPERTY,
                            Boolean.valueOf( isServer ).toString() ) ;

                        // Create orb and make it visible.  This will allow
                        // loopback calls to getORB() from
                        // portable interceptors activated as a side-effect of the
                        // remaining initialization. If it's a
                        // server, there's a small time window during which the
                        // ProtocolManager won't be available.  Any callbacks that
                        // result from the protocol manager initialization itself
                        // cannot depend on having access to the protocol manager.
                        orb = orbFactory.createORB(props);

                        if (isServer) {
                            if (protocolManager == null) {
                                ProtocolManager tempProtocolManager =
                                                protocolManagerProvider.get();

                                tempProtocolManager.initialize(orb);
                                // Move startup of naming to PEORBConfigurator so it runs
                                // before interceptors.
                                // tempProtocolManager.initializeNaming();
                                tempProtocolManager.initializePOAs();

                                // Now make protocol manager visible.
                                protocolManager = tempProtocolManager;
                                
                                GlassfishNamingManager namingManager =
                                    glassfishNamingManagerProvider.get();

                                Remote remoteSerialProvider =
                                    namingManager.initializeRemoteNamingSupport(orb);

                                protocolManager.initializeRemoteNaming(remoteSerialProvider);
                            }
                        }
                    } catch(Exception e) {
                        orb = null;
                        protocolManager = null;
                        throw new RuntimeException("Orb initialization erorr", e);    
                    }
                }
            }
        }

        return orb;
    }


    public void setSelectableChannelDelegate(SelectableChannelDelegate d) {
        selectableChannelDelegate = d;
    }

    public SelectableChannelDelegate getSelectableChannelDelegate() {
        return this.selectableChannelDelegate;
    }

    public static interface SelectableChannelDelegate {

        public void handleRequest(SelectableChannel channel);

    }
    

    /**
     * Get a protocol manager for creating remote references. ProtocolManager is only
     * available in the server.  Otherwise, this method returns null.
     *
     * If it's the server and the orb hasn't been already created, calling
     * this method has the side effect of creating the orb.
     */
    public ProtocolManager getProtocolManager() {

        if( !processEnv.getProcessType().isServer() ) {
            return null;
        }
        
        synchronized (this) {
            if (protocolManager == null) {
                getORB();
            }
            
            return protocolManager;
        }
    }

    public boolean isORBInitialized() {
	return (orb != null);
    }

    public int getOTSPolicyType() {
        return orbFactory.getOTSPolicyType();    
    }

    public int getCSIv2PolicyType() {
        return orbFactory.getCSIv2PolicyType();    
    }

    public Properties getCSIv2Props() {
        return orbFactory.getCSIv2Props();
    }

    public void setCSIv2Prop(String name, String value) {
        orbFactory.setCSIv2Prop(name, value);
    }

    public int getORBInitialPort() {
        return orbFactory.getORBInitialPort();
    }

    public String getORBHost(ORB orb) {
        return orbFactory.getORBHost(orb);
    }

    public int getORBPort(ORB orb) {
        return orbFactory.getORBPort(orb);
    }
      
    public boolean isEjbCall(ServerRequestInfo sri) {
        return orbFactory.isEjbCall(sri);
    }
      
    
}
