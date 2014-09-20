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

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.spi.folb.ClusterInstanceInfo;
import org.glassfish.orb.admin.config.IiopListener;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.logging.LogDomains;
import org.glassfish.enterprise.iiop.util.IIOPUtils;

import java.util.List;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * This class is responsible for reading the domain.xml via Config API
 * and producing a list of instances in the form of ClusterInstanceInfo 
 * objects.
 * This class is designed for use by both FailoverIORInterceptor
 * and Java Web Start.
 * @author Sheetal Vartak
 * @date 1/12/05
 */

public class IIOPEndpointsInfo {  

    private static final IIOPUtils iiopUtils = IIOPUtils.getInstance();

    private static Logger _logger = LogDomains.getLogger(IIOPEndpointsInfo.class, LogDomains.CORBA_LOGGER);
    
    private static final String baseMsg	= IIOPEndpointsInfo.class.getName();


    /**
     * TODO implement post V3 FCS
    public static Collection<ServerRef> getServersInCluster() {
    
        return iiopUtils.getServerRefs();
    }

    public static List<IiopListener> getListenersInCluster() {
 

        return iiopUtils.getIiopListeners();
    }

    **/

    /**
     * This method returns a list of SocketInfo objects for a particular 
     * server. This method is the common code called by 
     * getIIOPEndpoints() and getClusterInstanceInfo()
     */
    /*
    public static List<SocketInfo> getSocketInfoForServer(ServerRef serverRef, 
							  IiopListener[] listen) {
      
        List<SocketInfo> listOfSocketInfo =
		    new LinkedList<SocketInfo>();
	String serverName = serverRef.getRef();
	String hostName =
	  getHostNameForServerInstance(serverName);
	if (hostName == null) {
	    hostName = listen[0].getAddress();
	}
	for (int j = 0; j < listen.length; j++) { 
	    String id = listen[j].getId();
	    String port = 
	      getResolvedPort(listen[j], serverName);
	    if (_logger.isLoggable(Level.FINE)) {
	        _logger.log(Level.FINE, 
			    baseMsg + ".getSocketInfoForServer:" +
			    " adding address for "+ 
			    serverName + "/" + id +
			    "/" + hostName + "/" + port);
	    }
	    listOfSocketInfo.add(new SocketInfo(id, hostName, Integer.valueOf(port)));
	}
	return listOfSocketInfo;
    }
    */

    /**
     * This method returns the endpoints in host:port,host1:port1,host2:port2,...
     * format. This is called by Java Web Start
     */
    public static String getIIOPEndpoints() {
        //TODO FIXME
        String endpoints = null;

	    return endpoints;

    }

    /**
     * This method returns a ClusterInstanceInfo list.
     */
    public static List<ClusterInstanceInfo> getClusterInstanceInfo()
    {
        //TODO FIXME
    return null;
    }

    /**
      * The following returns the IIOP listener(s) for all the
      * servers belonging to the current cluster.
      *
      * @author  satish.viswanatham@sun.com
      *
      */
    /*
    public static IiopListener[][] getIIOPEndPointsForCurrentCluster() {
	// For each server instance in a cluster, there are 3 iiop listeners:
	// one for non ssl, one for ssl and third for ssl mutual auth
	
        IiopListener[][] listeners = new IiopListener[serverRefs.length][3];  //SHEETAL can there be multiple SSL or 
                                                                         //SSL_MUTH_AUTH ports? bug 6321813
	for (int i = 0; i < serverRefs.length; i++) {
	    Server server = 
		ServerHelper.getServerByName(configCtx, serverRefs[i].getRef());
	    String configRef = server.getConfigRef();
	    Config config =
		ConfigAPIHelper.getConfigByName(configCtx, configRef);
	    IiopService iiopService = config.getIiopService();
	    listeners[i] = iiopService.getIiopListener();
	}
	return listeners;
    }
    */
    /**
     * Returns ip address from node agent refered from instance
     * or null if Exception
     *
     * @author  sridatta.viswanath@sun.com
     */
    /*
    public static String getHostNameForServerInstance(String serverName) 
    {
        try {
            JMXConnectorConfig info = 
		ServerHelper.getJMXConnectorInfo(configCtx, serverName);
            _logger.log(Level.FINE, 
			baseMsg + ".getHostNameForServerInstance: " +
			"found info: " + info.toString());
	    String host = info.getHost();
            _logger.log(Level.FINE, 
			baseMsg + ".getHostNameForServerInstance: " +
			"found host: " + host);
            return host;
        } catch (Throwable e){
            _logger.log(Level.FINE, 
			baseMsg + ".getHostNameForServerInstance: " +
			"gotException: " + e + " " + e.getMessage() +
			"; returning null");
            return null;
        }
    }
    */
    /**
     * Gets the correct resolved value for the specific instance
     * Without this routine, config context resolves the value
     * to the current running instance
     *
     * @author  sridatta.viswanath@sun.com
     */
    /*
    public static String getResolvedPort(IiopListener l,
					  String server) {
	String rawPort = l.getRawAttributeValue("port");
	PropertyResolver pr = new PropertyResolver(configCtx, server);
	return pr.resolve(rawPort);
    }
    */
}
