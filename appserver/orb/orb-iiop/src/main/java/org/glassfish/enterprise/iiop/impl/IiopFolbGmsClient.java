/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.ee.cms.core.FailureNotificationSignal;
import com.sun.enterprise.ee.cms.core.JoinedAndReadyNotificationSignal;
import com.sun.enterprise.ee.cms.core.PlannedShutdownSignal;
import com.sun.enterprise.ee.cms.core.Signal;
import com.sun.enterprise.ee.cms.core.SignalAcquireException;
import com.sun.enterprise.ee.cms.core.SignalReleaseException;
import com.sun.logging.LogDomains;

import com.sun.corba.ee.spi.orb.ORB ;
import com.sun.corba.ee.spi.folb.ClusterInstanceInfo;
import com.sun.corba.ee.spi.folb.GroupInfoService;
import com.sun.corba.ee.impl.folb.GroupInfoServiceBase;
import com.sun.corba.ee.spi.folb.GroupInfoServiceObserver;
import com.sun.corba.ee.spi.folb.SocketInfo;
import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.ee.cms.core.CallBack;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import org.glassfish.config.support.GlassFishConfigBean;
import org.glassfish.config.support.PropertyResolver;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.ORBPackage.InvalidName;

// REVISIT impl
//import com.sun.corba.ee.impl.folb.ServerGroupManager;

/**
 * @author Harold Carr
 */
public class IiopFolbGmsClient implements CallBack {
    private static final Logger _logger =
       LogDomains.getLogger(IiopFolbGmsClient.class,
           LogDomains.CORBA_LOGGER);

    private ServiceLocator services;

    private Domain domain ;

    private Server myServer ;

    private Nodes nodes ;

    private GMSAdapterService gmsAdapterService ;

    private GMSAdapter gmsAdapter ;

    private Map<String, ClusterInstanceInfo> currentMembers;

    private GroupInfoService gis;

    private void fineLog( String fmt, Object... args ) {
        if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE, fmt, args ) ;
        }
    }

    public IiopFolbGmsClient( ServiceLocator services ) {
        fineLog( "IiopFolbGmsClient: constructor: services {0}",
            services ) ;
        this.services = services ;

        gmsAdapterService = services.getService(GMSAdapterService.class);

	try {
            if (gmsAdapterService == null) {
                return ;
            }

            gmsAdapter = gmsAdapterService.getGMSAdapter() ;
            fineLog( "IiopFolbGmsClient: gmsAdapter {0}", gmsAdapter );

            if (gmsAdapter != null) {
                domain = services.getService( Domain.class );
                fineLog( "IiopFolbGmsClient: domain {0}", domain) ;

                Servers servers = services.getService(Servers.class );
                fineLog( "IiopFolbGmsClient: servers {0}", servers );

                nodes = services.getService(Nodes.class);
                fineLog( "IiopFolbGmsClient: nodes {0}", nodes );

                String instanceName = gmsAdapter.getModule().getInstanceName() ;
                fineLog( "IiopFolbGmsClient: instanceName {0}", instanceName );

                myServer = servers.getServer(instanceName) ;
                fineLog( "IiopFolbGmsClient: myServer {0}", myServer );

                gis = new GroupInfoServiceGMSImpl() ;
                fineLog("IiopFolbGmsClient: IIOP GIS created");

                currentMembers = getAllClusterInstanceInfo() ;
                fineLog( "IiopFolbGmsClient: currentMembers = ", currentMembers ) ;

                fineLog( "iiop instance info = " + getIIOPEndpoints() ) ;

                gmsAdapter.registerFailureNotificationListener(this);
                gmsAdapter.registerJoinedAndReadyNotificationListener(this);
                gmsAdapter.registerPlannedShutdownListener(this);

                fineLog( "IiopFolbGmsClient: GMS action factories added");
            } else {
                fineLog( "IiopFolbGmsClient: gmsAdapterService is null") ;
                gis = new GroupInfoServiceNoGMSImpl() ;
            }

	} catch (Throwable t) {
            _logger.log(Level.SEVERE, t.getLocalizedMessage(), t);
	} finally {
            fineLog( "IiopFolbGmsClient: gmsAdapter {0}", gmsAdapter ) ;
	}
    }

    public void setORB( ORB orb ) {
        try {
            orb.register_initial_reference(
                    ORBConstants.FOLB_SERVER_GROUP_INFO_SERVICE,
                    (org.omg.CORBA.Object) gis);
            fineLog( ".initGIS: naming registration complete: {0}", gis);

            // Just for logging
            GroupInfoService gisRef = (GroupInfoService)orb.resolve_initial_references(
                ORBConstants.FOLB_SERVER_GROUP_INFO_SERVICE);
            List<ClusterInstanceInfo> lcii =
                    gisRef.getClusterInstanceInfo(null);
            fineLog( "Results from getClusterInstanceInfo:");
            if (lcii != null) {
                for (ClusterInstanceInfo cii : lcii) {
                    fineLog( cii.toString() );
                }
            }
        } catch (InvalidName e) {
            fineLog( ".initGIS: registering GIS failed: {0}", e);
        }
    }

    public GroupInfoService getGroupInfoService() {
        return gis ;
    }

    public boolean isGMSAvailable() {
        return gmsAdapter != null ;
    }

    ////////////////////////////////////////////////////
    //
    // Action
    //

    @Override
    public void processNotification(final Signal signal) {
        try {
            fineLog( "processNotification: signal {0}", signal ) ;
            signal.acquire();
            handleSignal(signal);
        } catch (SignalAcquireException e) {
            _logger.log(Level.SEVERE, e.getLocalizedMessage());
	} catch (Throwable t) {
	    _logger.log(Level.SEVERE, t.getLocalizedMessage(), t);
        } finally {
	    try {
		signal.release();
	    } catch (SignalReleaseException e) {
		_logger.log(Level.SEVERE, e.getLocalizedMessage());
	    }
	}
    }

    ////////////////////////////////////////////////////
    //
    // Implementation
    //

    private void handleSignal(final Signal signal) 
    {
        fineLog( "IiopFolbGmsClient.handleSignal: signal from: {0}",
            signal.getMemberToken());
        fineLog( "IiopFolbGmsClient.handleSignal: map entryset: {0}",
            signal.getMemberDetails().entrySet());

	if (signal instanceof PlannedShutdownSignal ||
	    signal instanceof FailureNotificationSignal) {

	    removeMember(signal);

	} else if (signal instanceof JoinedAndReadyNotificationSignal) {

	    addMember(signal);

	} else {
	    _logger.log(Level.SEVERE, 
                "IiopFolbGmsClient.handleSignal: unknown signal: {0}",
                signal.toString());
	}
    }

    private void removeMember(final Signal signal)
    {
	String instanceName = signal.getMemberToken();
	try {
            fineLog( "IiopFolbGmsClient.removeMember->: {0}",
                instanceName);

	    synchronized (this) {
		if (currentMembers.get(instanceName) != null) {
		    currentMembers.remove(instanceName);

                    fineLog(
                        "IiopFolbGmsClient.removeMember: {0} removed - notifying listeners",
                        instanceName);

		    gis.notifyObservers();

                    fineLog(
                        "IiopFolbGmsClient.removeMember: {0} - notification complete",
                        instanceName);
		} else {
                    fineLog(
                        "IiopFolbGmsClient.removeMember: {0} not present: no action",
                        instanceName);
		}
	    }
	} finally {
            fineLog( "IiopFolbGmsClient.removeMember<-: {0}", instanceName);
	}
    }

    private void addMember(final Signal signal)
    {
	final String instanceName = signal.getMemberToken();
	try {
            fineLog( "IiopFolbGmsClient.addMember->: {0}", instanceName);

	    synchronized (this) {
		if (currentMembers.get(instanceName) != null) {
                    fineLog( "IiopFolbGmsClient.addMember: {0} already present: no action",
                            instanceName);
		} else {
		    ClusterInstanceInfo clusterInstanceInfo = 
                        getClusterInstanceInfo(instanceName) ;

		    currentMembers.put( clusterInstanceInfo.name(),
                        clusterInstanceInfo);

                    fineLog( "IiopFolbGmsClient.addMember: {0} added - notifying listeners",
                        instanceName);

		    gis.notifyObservers();

                    fineLog( "IiopFolbGmsClient.addMember: {0} - notification complete",
                        instanceName);
		}
	    }	
	} finally {
            fineLog( "IiopFolbGmsClient.addMember<-: {0}", instanceName);
	}
    }

    private int resolvePort( Server server, IiopListener listener ) {
        fineLog( "resolvePort: server {0} listener {1}", server, listener ) ;

        IiopListener ilRaw = GlassFishConfigBean.getRawView( listener ) ;
        fineLog( "resolvePort: ilRaw {0}", ilRaw ) ;

        PropertyResolver pr = new PropertyResolver( domain, server.getName() ) ;
        fineLog( "resolvePort: pr {0}", pr ) ;

        String port = pr.getPropertyValue( ilRaw.getPort() ) ;
        fineLog( "resolvePort: port {0}", port ) ;

        return Integer.parseInt(port) ;
    }

    private ClusterInstanceInfo getClusterInstanceInfo( Server server,
        Config config, boolean assumeInstanceIsRunning ) {
        if (server == null) {
            return null ;
        }

        if (!assumeInstanceIsRunning) {
            if (!server.isRunning()) {
                return null ;
            }
        }

        fineLog( "getClusterInstanceInfo: server {0}, config {1}",
            server, config ) ;

        final String name = server.getName() ;
        fineLog( "getClusterInstanceInfo: name {0}", name ) ;

        final int weight = Integer.parseInt( server.getLbWeight() ) ;
        fineLog( "getClusterInstanceInfo: weight {0}", weight ) ;

        final String nodeName = server.getNodeRef() ;
        String hostName = nodeName ;
        if (nodes != null) {
            Node node = nodes.getNode( nodeName ) ;
            if (node != null) {
                if (node.isLocal()) {
                    try {
                        hostName = InetAddress.getLocalHost().getHostName() ;
                    } catch (UnknownHostException exc) {
                        fineLog( "getClusterInstanceInfo: caught exception for localhost lookup {0}",
                            exc )  ;
                    }
                } else {
                    hostName = node.getNodeHost() ;
                }
            }
        }

        fineLog( "getClusterInstanceInfo: host {0}", hostName ) ;

        final IiopService iservice = config.getExtensionByType(IiopService.class) ;
        fineLog( "getClusterInstanceInfo: iservice {0}", iservice ) ;

        final List<IiopListener> listeners = iservice.getIiopListener() ;
        fineLog( "getClusterInstanceInfo: listeners {0}", listeners ) ;

        final List<SocketInfo> sinfos = new ArrayList<SocketInfo>() ;
        for (IiopListener il : listeners) {
            SocketInfo sinfo = new SocketInfo( il.getId(), hostName,
                resolvePort( server, il ) ) ;
            sinfos.add( sinfo ) ;
        }
        fineLog( "getClusterInstanceInfo: sinfos {0}", sinfos ) ;

        final ClusterInstanceInfo result = new ClusterInstanceInfo( name, weight,
            sinfos ) ;
        fineLog( "getClusterInstanceInfo: result {0}", result ) ;

        return result ;
    }

    private Config getConfigForServer( Server server ) {
        fineLog( "getConfigForServer: server {0}", server ) ;

        String configRef = server.getConfigRef() ;
        fineLog( "getConfigForServer: configRef {0}", configRef ) ;

        Configs configs = services.getService( Configs.class ) ;
        fineLog( "getConfigForServer: configs {0}", configs ) ;

        Config config = configs.getConfigByName(configRef) ;
        fineLog( "getConfigForServer: config {0}", config ) ;

        return config ;
    }

    // For addMember.
    private ClusterInstanceInfo getClusterInstanceInfo( String instanceName) {
        fineLog( "getClusterInstanceInfo: instanceName {0}", instanceName ) ;

        final Servers servers = services.getService( Servers.class );
        fineLog( "getClusterInstanceInfo: servers {0}", servers ) ;

        final Server server = servers.getServer(instanceName) ;
        fineLog( "getClusterInstanceInfo: server {0}", server ) ;

        final Config config = getConfigForServer( server ) ;
        fineLog( "getClusterInstanceInfo: servers {0}", servers ) ;

        // assumeInstanceIsRunning is set to true since this is
        // coming from addMember, because shoal just told us that the instance is up.
        ClusterInstanceInfo result = getClusterInstanceInfo( server, config,
            true ) ;
        fineLog( "getClusterInstanceInfo: result {0}", result ) ;

        return result ;
    }

    private Map<String,ClusterInstanceInfo> getAllClusterInstanceInfo() {
        final Cluster myCluster = myServer.getCluster() ;
        fineLog( "getAllClusterInstanceInfo: myCluster {0}", myCluster ) ;

        final Config myConfig = getConfigForServer( myServer ) ;
        fineLog( "getAllClusterInstanceInfo: myConfig {0}", myConfig ) ;

        final Map<String,ClusterInstanceInfo> result =
            new HashMap<String,ClusterInstanceInfo>() ;

        //When myServer is DAS's situation, myCluster is null.
        //null check is needed.
        if (myCluster != null) {
            for (Server server : myCluster.getInstances()) {
                ClusterInstanceInfo cii = getClusterInstanceInfo(server,
                        myConfig, false);
                if (cii != null) {
                    result.put(server.getName(), cii);
                }
            }
        }

        fineLog( "getAllClusterInstanceInfo: result {0}", result ) ;
        return result ;
    }

    // return host:port,... string for all clear text ports in the cluster
    // instance info.
    public final String getIIOPEndpoints() {
        final Map<String,ClusterInstanceInfo> cinfos = getAllClusterInstanceInfo() ;
        final StringBuilder result = new StringBuilder() ;
        boolean first = true ;
        for (ClusterInstanceInfo cinfo : cinfos.values() ) {
            for (SocketInfo sinfo : cinfo.endpoints()) {
                if (!sinfo.type().startsWith( "SSL" )) {
                    if (first) {
                        first = false ;
                    } else {
                        result.append( ',' ) ;
                    }

                    result.append( sinfo.host() ).append( ':' )
                        .append( sinfo.port() ) ;
                }
            }
        }
        return result.toString() ;
    }

    class GroupInfoServiceGMSImpl extends GroupInfoServiceBase {
        @Override
        public List<ClusterInstanceInfo> internalClusterInstanceInfo( 
            List<String> endpoints) {

            fineLog( "internalClusterInstanceInfo: currentMembers {0}",
                currentMembers ) ;

            if (currentMembers == null) {
                return new ArrayList<ClusterInstanceInfo>() ;
            } else {
                return new ArrayList<ClusterInstanceInfo>(
                    currentMembers.values() ) ;
            }
        }
    }

    class GroupInfoServiceNoGMSImpl extends GroupInfoServiceGMSImpl {
        @Override
        public boolean addObserver(GroupInfoServiceObserver x) {
            throw new RuntimeException("SHOULD NOT BE CALLED");
        }

        @Override
        public void notifyObservers() {
            throw new RuntimeException("SHOULD NOT BE CALLED");
        }

        @Override
        public boolean shouldAddAddressesToNonReferenceFactory(String[] x) {
            throw new RuntimeException("SHOULD NOT BE CALLED");
        }

        @Override
        public boolean shouldAddMembershipLabel(String[] adapterName) {
            throw new RuntimeException("SHOULD NOT BE CALLED");
        }
    }
}

// End of file.
