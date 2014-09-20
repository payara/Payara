/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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
import com.sun.corba.ee.spi.folb.SocketInfo;
import com.sun.jndi.cosnaming.IiopUrl;
import com.sun.logging.LogDomains;
import org.glassfish.internal.api.ORBLocator;
import org.glassfish.logging.annotation.LogMessageInfo;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.api.naming.NamingClusterInfo.*;
import static org.glassfish.enterprise.iiop.impl.NamingClusterInfoImpl.NO_ENDPOINT_SELECTED;
import static org.glassfish.enterprise.iiop.impl.NamingClusterInfoImpl.logger;

/**
 * The list of endpoints are randomized the very first time.
 *  This happens only once( when called from the static block
 * of SerialInitContextFactory class).
 * Simple RoundRobin is a special case of Weighted Round Robin where the
 * weight per endpoint is equal.With the dynamic reconfiguration 
 * implementation, the endpoints list willhave the following structure:
 * - server_identifier (a stringified name for the machine)
 * - weight- list of SocketInfo {type (type = CLEAR_TEXT or SSL) + 
 *         IP address + port }
 * The above structure supports multi-homed machines 
 * i.e. one machinehosting multiple IP addresses.
 * The RoundRobinPolicy class can be the class that is also implementing
 * the Listener interface for listening to events generated whenever there
 * is a change in the cluster shape. The listener/event design is still
 * under construction.This list of endpoints will have to be created during 
 * bootstrapping(i.e. when the client first starts up.) This list will comprise
 * of theendpoints specified by the user in "com.sun.appserv.iiop.endpoints"
 * property. We can assume a default weight for these endpoints (e.g 10).
 * This list will be used to make the first lookup call. During the first 
 * lookup call, the actual list of endpoints will beprovided back. 
 * Then on, whenever there is any change in the clustershape, 
 * the listener will get the updated list of endpoints from theserver.
 * The implementation for choosing the endpoint from the list of endpoints
 * is as follows:Let's assume 4 endpoints:A(wt=10), B(wt=30), C(wt=40), 
 * D(wt=20). 
 * Using the Random API, generate a random number between 1 and10+30+40+20.
 * Let's assume that the above list is randomized. Based on the weights, we
 * have intervals as follows:
 * 1-----10 (A's weight)
 * 11----40 (A's weight + B's weight)
 * 41----80 (A's weight + B's weight + C's weight)
 * 81----100(A's weight + B's weight + C's weight + C's weight)
 * Here's the psuedo code for deciding where to send the request:
 * if (random_number between 1 & 10) {send request to A;}
 * else if (random_number between 11 & 40) {send request to B;}
 * else if (random_number between 41 & 80) {send request to C;}
 * else if (random_number between 81 & 100) {send request to D;}
 * For simple Round Robin, we can assume the same weight for all endpointsand 
 * perform the above.
 * @author Sheetal Vartak
 **/

public class RoundRobinPolicy {
    @LogMessageInfo(message = "Could not find an endpoint to send request to.")
    public static final String COULD_NOT_FIND_ENDPOINT = "AS-ORB-00004";

    @LogMessageInfo(message = "Unknown host: {0} Exception thrown : {1}")
    public static final String UNKNOWN_HOST = "AS-ORB-00005";

    @LogMessageInfo(message = "No Endpoints selected in com.sun.appserv.iiop.endpoints property. Using JNDI Provider URL {0} instead")
    public static final String NO_ENDPOINTS_SELECTED_PROVIDER = "AS-ORB-00006";

    @LogMessageInfo(message = "Exception : {0} thrown for bad provider URL String: {1}")
    public static final String PROVIDER_EXCEPTION = "AS-ORB-00007";

    // Each SocketInfo.type() must either start with SSL, or be CLEAR_TEXT
    private static final String SSL = "SSL" ;
    private static final String CLEAR_TEXT = "CLEAR_TEXT" ;

    private static java.util.Random rand = new java.util.Random();

    private List<ClusterInstanceInfo> endpointsList = 
	 new LinkedList<ClusterInstanceInfo>();

    private int totalWeight = 0;

    private List<String> resolvedEndpoints ;

    private static final int default_weight = 10;

    //called during bootstrapping
    public RoundRobinPolicy(List<String> list) {
	setClusterInstanceInfoFromString(list);
    }

    // Copy list, changing any type that does not start with SSL to CLEAR_TEXT.
    private List<SocketInfo> filterSocketInfos( List<SocketInfo> sis ) {
        final List<SocketInfo> result = new ArrayList<SocketInfo>() ;
        for (SocketInfo si : sis) {
            final String newType = si.type().startsWith(SSL) ?
                si.type() : CLEAR_TEXT ;
            final SocketInfo siCopy = new SocketInfo( newType,
                si.host(), si.port() ) ;
            result.add( siCopy ) ;
        }
        return result ;
    }

    private boolean isWeighted() {
        String policy = System.getProperty(LOAD_BALANCING_PROPERTY, IC_BASED);
        return policy.equals(IC_BASED_WEIGHTED);
    }

    private List<ClusterInstanceInfo> filterClusterInfo(
        List<ClusterInstanceInfo> info ) {

        boolean isw = isWeighted() ;
        ArrayList<ClusterInstanceInfo> newList =
            new ArrayList<ClusterInstanceInfo>() ;
        totalWeight = 0 ;

	for (ClusterInstanceInfo clinfo : info) {
            final int newWeight = isw ? clinfo.weight() : default_weight ;

            final List<SocketInfo> newEndpoints =
                filterSocketInfos( clinfo.endpoints() ) ;
            final ClusterInstanceInfo newClinfo = new ClusterInstanceInfo(
                clinfo.name(), newWeight, newEndpoints ) ;
            newList.add( newClinfo ) ;

	    totalWeight += newWeight ;
        }

        return newList ;
    }

    private boolean containsMatchingAddress( List<ClusterInstanceInfo> list,
        String host, int port ) {

        for (ClusterInstanceInfo info : list) {
             for (SocketInfo si : info.endpoints()) {
                 if (si.type().equals( CLEAR_TEXT )) {
                     if (si.host().equals( host) && si.port() == port) {
                         return true ;
                     }
                 }
            }
        }

        return false ;
    }

    // Add those elements of the second list that do not contain a clear
    // text address that appears in the first list.
    private List<ClusterInstanceInfo> merge( List<ClusterInstanceInfo> first,
        List<ClusterInstanceInfo> second ) {

        List<ClusterInstanceInfo> result = new ArrayList<ClusterInstanceInfo>() ;
        result.addAll( first ) ;
        for (ClusterInstanceInfo info : second) {
            for (SocketInfo si : info.endpoints()) {
                if (!containsMatchingAddress(first, si.host(), si.port() )) {
                    result.add( info ) ;
                }
            }
        }
        return result ;
    }

    private List<ClusterInstanceInfo> fromHostPortStrings( List<String> list ) {
        List<ClusterInstanceInfo> result =
            new LinkedList <ClusterInstanceInfo> ();

        for( String elem : list) {
            ClusterInstanceInfo info = makeClusterInstanceInfo( elem,
                default_weight ) ;
            result.add( info );
        }

        return result ;
    }


    //will be called after dynamic reconfig
    // used in GroupInfoServiceObserverImpl
    synchronized final void setClusterInstanceInfo(List<ClusterInstanceInfo> list) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "setClusterInstanceInfo: list={0}", list);
        }

        List<ClusterInstanceInfo> filtered = filterClusterInfo(list);
        List<ClusterInstanceInfo> resolved = fromHostPortStrings(resolvedEndpoints);

        endpointsList = merge(filtered, resolved);
    }

    // Note: regard any addresses supplied here as a permanent part of the
    // cluster.
    synchronized final void setClusterInstanceInfoFromString(List<String> list) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "setClusterInstanceInfoFromString: list={0}", list);
        }

        List<String> newList = list;
        if (newList.isEmpty()) {
            newList = getEndpointForProviderURL(
                    System.getProperty(ORBLocator.JNDI_PROVIDER_URL_PROPERTY));
        }

        //randomize the list before adding it to linked list
        if (!newList.isEmpty()) {
            List<String> newList2 = randomize(newList);
            resolvedEndpoints = new ArrayList<String>(newList2);
            endpointsList = fromHostPortStrings(newList2);
            // Put in a default for total weight; any update will correct this.
            totalWeight = 10 * endpointsList.size();
        } else {
            logger.log(Level.FINE, "no.endpoints");
        }
    }   

    /**
     * during bootstrapping, weight is assumed "10" for all endpoints
     * then on, whenever server sends updates list,
     * create the list again here with right weights
     */
    private ClusterInstanceInfo makeClusterInstanceInfo(String str, 
        int weight) {

    //support IPV6 literal address
    String[] host_port = new String[2];
    int i = str.lastIndexOf(':');
    host_port[0] = str.substring(0,i);
    host_port[1] = str.substring(i+1);

	String server_identifier = ""; //for bootstrapping, can be ""
	String type = CLEAR_TEXT; //will be clear_text for bootstrapping
	SocketInfo socketInfo = new SocketInfo(
            type, host_port[0], Integer.parseInt( host_port[1]) );
        List<SocketInfo> sil = new ArrayList<SocketInfo>(1) ;
        sil.add( socketInfo ) ;

        return new ClusterInstanceInfo(server_identifier, weight, sil);
    }

    /*
     * This method checks for other ways of specifying endpoints
     * namely JNDI provider url 
     * orb host:port is used only if even env passed into 
     * getInitialContext is empty. This check is performed in 
     * SerialInitContextFactory.getInitialContext()
     */
    public List<String> getEndpointForProviderURL(String providerURLString) {
        if (providerURLString != null) {
            try {
                final IiopUrl providerURL = new IiopUrl(providerURLString);
                final List<String> newList = getAddressPortList(providerURL);
                logger.log(Level.WARNING, NO_ENDPOINTS_SELECTED_PROVIDER, providerURLString);
                return newList;
            } catch (MalformedURLException me) {
                logger.log(Level.WARNING, PROVIDER_EXCEPTION, new Object[]{me, providerURLString});
            }
        }
        return new ArrayList<String>();
    }
    
    /**
     * randomize the list.  Note: this empties its argument.
     */
    private List<String> randomize( List<String> list ) {
        List<String> result = new ArrayList<String>( list.size() ) ;
        while (!list.isEmpty()) {
            int random = rand.nextInt( list.size() ) ;
            String elem = list.remove( random ) ;
            result.add( elem ) ;
        }

        if(logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Randomized list {0}", result);
        }
        return result ;
    }

    /*
     * get a new shape of the endpoints
     * For e.g. if list contains A,B,C
     * if the logic below chooses B as the endpoint to send the req to
     * then return B,C,A.
     * logic used is as described in Class description comments
     */
    public synchronized List<String> getNextRotation() {
	int lowerLimit = 0; //lowerLimit
	int random = 0;
	//make sure that the random # is not 0
	//Random API gives a number between 0 and sumOfAllWeights
	//But our range intervals are from 1-upperLimit, 
	//11-upperLimit and so
	//on. Hence we dont want random # to be 0.
        // fineLog( "RoundRobinPolicy.getNextRotation -> sumOfAllWeights = {0}",
            // totalWeight);
	while( random == 0) {
	    random = rand.nextInt(totalWeight);
	    if (random != 0) {
		break;
	    }
	}
        // fineLog( "getNextRotation : random # = {0} sum of all weights = {1}",
            // new Object[]{random, totalWeight});
	int i = 0;
	for (ClusterInstanceInfo endpoint : endpointsList) {
	    int upperLimit = lowerLimit + endpoint.weight();
            // fineLog( "upperLimit = {0}", upperLimit);
	    if (random > lowerLimit && random <= upperLimit) {
		List<ClusterInstanceInfo> instanceInfo = 
		    new LinkedList<ClusterInstanceInfo>();
		
		//add the sublist at index 0 
		instanceInfo.addAll(0, 
                    endpointsList.subList(i, endpointsList.size()));

		//add the remaining list
		instanceInfo.addAll(endpointsList.subList(0, i));

                endpointsList = instanceInfo ;

		if(logger.isLoggable(Level.FINE)) {
		    logger.log(Level.FINE, "getNextRotation: result={0}", instanceInfo);
        }
		
		return convertIntoCorbaloc(instanceInfo);
	    }
	    lowerLimit = upperLimit;
	    // fineLog( "lowerLimit = {0}", lowerLimit);
	    i++;    
	}
	logger.log(Level.WARNING, COULD_NOT_FIND_ENDPOINT);
	return new ArrayList<String>() ;
    }
    
    private List<String> convertIntoCorbaloc(List<ClusterInstanceInfo> list) {
	List<String> host_port = new ArrayList<String>();
	for (ClusterInstanceInfo endpoint : list) {
	    List<SocketInfo> sinfos = endpoint.endpoints();
            for (SocketInfo si : sinfos ) {
                // XXX this needs to be revised if we ever do a secure
                // bootstrap protocol for the initial corbaloc URL resolution
                if (si.type().equals( CLEAR_TEXT )) {
                    String element = si.host().trim() + ":" + si.port() ;
                    if (!host_port.contains( element )) {
                        host_port.add( element ) ;
                    }
                }
            }
	}
	return host_port ;
    }

    /**
     * following methods (over-loaded) for getting all IP addresses
     * corresponding to a particular host.
     * (multi-homed hosts).
     */
    
    private List<String> getAddressPortList(IiopUrl iiopUrl) {
        // Pull out the host name and port
        IiopUrl.Address iiopUrlAddress = 
                (IiopUrl.Address)(iiopUrl.getAddresses().elementAt(0));
        String host = iiopUrlAddress.host;
        int portNumber = iiopUrlAddress.port;
        String port = Integer.toString(portNumber);
        // We return a list of <IP ADDRESS>:<PORT> values
        return getAddressPortList(host, port);        
    }
    
    public List<String> getAddressPortList(String host, String port) {
        // Get the ip addresses corresponding to the host.
        // XXX this currently does NOT support IPv6.
        try {
            InetAddress [] addresses = InetAddress.getAllByName(host);
            List<InetAddress> addrs = new ArrayList<InetAddress>() ;
            for (InetAddress addr : addresses) {
                if (addr instanceof Inet4Address || addr instanceof Inet6Address) {
                    addrs.add( addr ) ;
                }
            }

            List<String> ret = new ArrayList<String>() ;
            for (InetAddress addr : addrs) {
                ret.add( addr.getHostAddress() + ":" + port ) ;
            }

            // We return a list of <IP ADDRESS>:<PORT> values
            return ret;
        } catch (UnknownHostException ukhe) {
            logger.log(Level.WARNING, UNKNOWN_HOST, new Object[]{host, ukhe});
            return new ArrayList<String>() ;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "RoundRobinPolicy[") ;
        boolean first = true ;
        for (ClusterInstanceInfo endpoint : endpointsList ) {
            if (first) {
                first = false ;
            } else {
                sb.append( ' ' ) ;
            }

            sb.append( endpoint.toString() ) ;
        }
        sb.append( ']' ) ;
        return sb.toString() ;
    }

    public synchronized List<String> getHostPortList() {
        return resolvedEndpoints ;
    }
}
