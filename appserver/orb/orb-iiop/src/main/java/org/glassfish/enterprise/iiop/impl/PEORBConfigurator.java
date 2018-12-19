/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.impl;

// import org.glassfish.pfl.dynamic.copyobject.spi.CopyobjectDefaults ;
import com.sun.corba.ee.spi.copyobject.CopyobjectDefaults ;
import org.glassfish.pfl.dynamic.copyobject.spi.ObjectCopierFactory ;
import com.sun.corba.ee.spi.copyobject.CopierManager;
import com.sun.corba.ee.spi.orb.DataCollector;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.corba.ee.spi.orb.ORBConfigurator;
import com.sun.corba.ee.spi.threadpool.NoSuchWorkQueueException;
import com.sun.corba.ee.spi.threadpool.ThreadPoolManager;
import com.sun.corba.ee.spi.presentation.rmi.InvocationInterceptor;
import com.sun.corba.ee.spi.transport.TransportManager;
import com.sun.corba.ee.spi.transport.Acceptor;
import com.sun.corba.ee.spi.transport.TransportDefault;
import com.sun.logging.LogDomains;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.grizzly.config.dom.Ssl;
import java.util.logging.Logger;
import org.glassfish.enterprise.iiop.api.IIOPConstants;
import org.glassfish.enterprise.iiop.util.S1ASThreadPoolManager;
import org.glassfish.enterprise.iiop.util.IIOPUtils;

import java.nio.channels.SocketChannel;
import java.net.Socket;
import java.util.List;

import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;

import com.sun.corba.ee.impl.naming.cosnaming.TransientNameService;

// TODO import org.omg.CORBA.TSIdentification;

// TODO import com.sun.corba.ee.impl.txpoa.TSIdentificationImpl;

import com.sun.corba.ee.spi.threadpool.ThreadPool;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.nio.channels.SelectableChannel;
import org.glassfish.enterprise.iiop.util.ThreadPoolStats;
import org.glassfish.enterprise.iiop.util.ThreadPoolStatsImpl;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;

public class PEORBConfigurator implements ORBConfigurator {
    private static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(LogDomains.CORBA_LOGGER);

    private static final String SSL = "SSL";
    private static final String SSL_MUTUALAUTH = "SSL_MUTUALAUTH";
    private static final String IIOP_CLEAR_TEXT_CONNECTION =
            "IIOP_CLEAR_TEXT";
    private static final String DEFAULT_ORB_INIT_HOST = "localhost";

    // TODO private static TSIdentification tsIdent;
    private static ORB theORB;
    private static ThreadPoolManager threadpoolMgr = null;
    private static boolean txServiceInitialized = false;

    private Acceptor lazyAcceptor = null;

    static {
        // TODO tsIdent = new TSIdentificationImpl();
    }

    private GlassFishORBHelper getHelper() {
        IIOPUtils iiopUtils = IIOPUtils.getInstance();
        return iiopUtils.getHabitat().getService(
            GlassFishORBHelper.class);
    }

    public void configure(DataCollector dc, ORB orb) {
        try {
            //begin temp fix for bug 6320008
            // this is needed only because we are using transient Name Service
            //this should be removed once we have the persistent Name Service in place
            /*TODO
            orb.setBadServerIdHandler(
            new BadServerIdHandler() {
            public void handle(ObjectKey objectkey) {
            // NO-OP
            }
            }
            );
             */
            //end temp fix for bug 6320008
            if (threadpoolMgr != null) {
                // This will be the case for the Server Side ORB created
                // For client side threadpoolMgr will be null, so we will
                // never come here
              orb.setThreadPoolManager(threadpoolMgr);
            }

            // Do the stats for the threadpool
            
            ThreadPoolManager tpool =  orb.getThreadPoolManager();
            // ORB creates its own threadpool if threadpoolMgr was null above
            ThreadPool thpool=tpool.getDefaultThreadPool();
            String ThreadPoolName = thpool.getName();
            ThreadPoolStats tpStats = new ThreadPoolStatsImpl(
                thpool.getWorkQueue(0).getThreadPool());
            StatsProviderManager.register("orb", PluginPoint.SERVER,
                "thread-pool/orb/threadpool/"+ThreadPoolName, tpStats);
           
            configureCopiers(orb);
            configureCallflowInvocationInterceptor(orb);

            // In the server-case, iiop acceptors need to be set up after the 
            // initial part of the orb creation but before any
            // portable interceptor initialization
            IIOPUtils iiopUtils = IIOPUtils.getInstance();
            if (iiopUtils.getProcessType().isServer()) {
                List<IiopListener> iiop_listener_list = IIOPUtils.getInstance()
                        .getIiopService().getIiopListener() ;
                IiopListener[] iiopListenerBeans =  iiop_listener_list
                        .toArray(new IiopListener [iiop_listener_list.size()]) ;                
                this.createORBListeners(iiopUtils, iiopListenerBeans, orb);
            }
            if (orb.getORBData().environmentIsGFServer()) {
                // Start the transient name service, which publishes NameService
                // in the ORB's local resolver.
                new TransientNameService(orb);
            }
            // Publish the ORB reference back to GlassFishORBHelper, so that
            // subsequent calls from interceptor ORBInitializers can call
            // GlassFishORBHelper.getORB() without problems.  This is
            // especially important for code running in the service initializer
            // thread.
            getHelper().setORB(orb);
        } catch (NoSuchWorkQueueException ex) {
            Logger.getLogger(PEORBConfigurator.class.getName()).log(Level.SEVERE, null, ex);
        }  
        } 

    private static void configureCopiers(ORB orb) {
        CopierManager cpm = orb.getCopierManager();

        ObjectCopierFactory stream = 
            CopyobjectDefaults.makeORBStreamObjectCopierFactory(orb) ;
        ObjectCopierFactory reflect = 
            CopyobjectDefaults.makeReflectObjectCopierFactory(orb) ;
        ObjectCopierFactory fallback = 
            CopyobjectDefaults.makeFallbackObjectCopierFactory( reflect, stream ) ;
        ObjectCopierFactory reference = 
            CopyobjectDefaults.getReferenceObjectCopierFactory() ;

        cpm.registerObjectCopierFactory( fallback, IIOPConstants.PASS_BY_VALUE_ID ) ;
        cpm.registerObjectCopierFactory( reference, IIOPConstants.PASS_BY_REFERENCE_ID ) ;
        cpm.setDefaultId( IIOPConstants.PASS_BY_VALUE_ID ) ;
    }

    // Called from GlassFishORBManager only when the ORB is running on server side
    public static void setThreadPoolManager() {
        threadpoolMgr = S1ASThreadPoolManager.getThreadPoolManager();
    }

    private static void configureCallflowInvocationInterceptor(ORB orb) {
        orb.setInvocationInterceptor(
                new InvocationInterceptor() {
            @Override
                    public void preInvoke() {
                        /*    TODO
                  Agent agent = Switch.getSwitch().getCallFlowAgent();
                  if (agent != null) {
                      agent.startTime(
                          ContainerTypeOrApplicationType.ORB_CONTAINER);
                  }
                  */
                    }

            @Override
                    public void postInvoke() {
                        /*   TODO
                  Agent agent = Switch.getSwitch().getCallFlowAgent();
                  if (agent != null) {
                      agent.endTime();
                  }
                  */
                    }
                }
        );
    }

    private Acceptor addAcceptor( org.omg.CORBA.ORB orb, boolean isLazy, 
        String host, String type, int port ) {

        com.sun.corba.ee.spi.orb.ORB theOrb = (com.sun.corba.ee.spi.orb.ORB) orb;
        TransportManager ctm = theOrb.getTransportManager() ;
        Acceptor acceptor ;
        if (isLazy) {
            acceptor = TransportDefault.makeLazyCorbaAcceptor( 
                theOrb, port, host, type );
        } else {
            acceptor = TransportDefault.makeStandardCorbaAcceptor( 
                theOrb, port, host, type ) ;
        }
        ctm.registerAcceptor( acceptor ) ;
        return acceptor;
    }

    private static final Set<String> ANY_ADDRS = new HashSet<String>(
        Arrays.asList( "0.0.0.0", "::", "::ffff:0.0.0.0" ) ) ;

    private String handleAddrAny( String hostAddr )  {
        if (ANY_ADDRS.contains( hostAddr )) {
            try {
                return java.net.InetAddress.getLocalHost().getHostAddress() ;
            } catch (java.net.UnknownHostException exc) {
                logger.log( Level.WARNING, 
                    "Unknown host exception : Setting host to localhost" ) ;
                return DEFAULT_ORB_INIT_HOST ;
            }
        } else {
            return hostAddr ;
        }
    }

    private void createORBListeners( IIOPUtils iiopUtils, 
        IiopListener[] iiopListenerBeans, org.omg.CORBA.ORB orb ) {

        if (iiopListenerBeans != null) {
            int lazyCount = 0 ;
            for (IiopListener ilb : iiopListenerBeans) {
                boolean securityEnabled = Boolean.valueOf( ilb.getSecurityEnabled() ) ;

                boolean isLazy = Boolean.valueOf( ilb.getLazyInit() ) ;
                if( isLazy ) {
                    lazyCount++;
                }

                if (lazyCount > 1) {
                    throw new IllegalStateException( "Invalid iiop-listener " 
                        + ilb.getId() 
                        + ". Only one iiop-listener can be configured "
                        + "with lazy-init=true");
                }

                int port = Integer.parseInt( ilb.getPort() ) ;
                String host = handleAddrAny( ilb.getAddress() ) ;

                if (!securityEnabled || ilb.getSsl() == null) {
                    Acceptor acceptor = addAcceptor( orb, isLazy, host, 
                            IIOP_CLEAR_TEXT_CONNECTION, port ) ;
                    if( isLazy ) {
                        lazyAcceptor = acceptor;
                    }
                } else {
                    if (isLazy) {
                        throw new IllegalStateException( "Invalid iiop-listener " 
                            + ilb.getId() 
                            + ". Lazy-init not supported for SSL iiop-listeners");
                    }

                    Ssl sslBean = ilb.getSsl() ;
                    assert sslBean != null ;

                    boolean clientAuth = Boolean.valueOf( 
                        sslBean.getClientAuthEnabled() ) ;
                    String type = clientAuth ? SSL_MUTUALAUTH : SSL ;
                    addAcceptor( orb, isLazy, host, type, port ) ;
                }
            }

            if( lazyCount == 1 ) {
                getHelper().setSelectableChannelDelegate(new AcceptorDelegateImpl(
                    lazyAcceptor));
            }
        }
    }

    private static class AcceptorDelegateImpl 
        implements GlassFishORBHelper.SelectableChannelDelegate {

        private Acceptor acceptor;

        AcceptorDelegateImpl(Acceptor lazyAcceptor) {
            acceptor = lazyAcceptor;
        }

        @Override
        public void handleRequest(SelectableChannel channel) {
            SocketChannel sch = (SocketChannel)channel ;
            Socket socket = sch.socket() ;
            acceptor.processSocket( socket ) ;
        }
    }
}
