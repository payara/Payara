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

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.spi.folb.GroupInfoService;
import org.glassfish.api.naming.NamingClusterInfo;
import org.glassfish.api.naming.NamingObjectsProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.internal.api.ORBLocator;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LoggerInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Singleton;

/**
 * This class is responsible for setting up naming load-balancing including RoundRobinPolicy.
 */

@Service
@Singleton
public class NamingClusterInfoImpl implements NamingClusterInfo {
    //move up to some class in top orb module
    @LoggerInfo(subsystem = "orb", description = "logger for GlassFish appserver orb modules", publish = true)
    public static final String ORB_LOGGER_NAME = "org.glassfish.orb";

    @LogMessagesResourceBundle
    public static final String ORB_LOGGER_RB = ORB_LOGGER_NAME + ".LogMessages";

    public static final Logger logger = Logger.getLogger(ORB_LOGGER_NAME, ORB_LOGGER_RB);

    @LogMessageInfo(message = "Exception occurred when resolving {0}",
    cause = "org.omg.CORBA.ORBPackage.InvalidName when trying to resolve GroupInfoService",
    action = "Check server.log for details")
    public static final String FAILED_TO_RESOLVE_GROUPINFOSERVICE = "AS-ORB-00001";

    @LogMessageInfo(
    message = "No Endpoints selected in com.sun.appserv.iiop.endpoints property. Using {0}:{1} instead")
    public static final String NO_ENDPOINT_SELECTED = "AS-ORB-00002";

    private RoundRobinPolicy rrPolicy;

    private GroupInfoServiceObserverImpl giso;

    @Override
    public void initGroupInfoService(Hashtable<?, ?> myEnv, String defaultHost, String defaultPort,
                                     ORB orb, ServiceLocator services) {
        // Always create one rrPolicy to be shared, if needed.
        final List<String> epList = getEndpointList(myEnv, defaultHost, defaultPort);
        rrPolicy = new RoundRobinPolicy(epList);

        GroupInfoService gis = null ;
        try {
            gis = (GroupInfoService) (orb.resolve_initial_references(ORBLocator.FOLB_CLIENT_GROUP_INFO_SERVICE));
        } catch (InvalidName ex) {
            logger.log(Level.SEVERE, FAILED_TO_RESOLVE_GROUPINFOSERVICE, ORBLocator.FOLB_CLIENT_GROUP_INFO_SERVICE);
            logger.log(Level.SEVERE, "", ex);
        }

        giso = new GroupInfoServiceObserverImpl( gis, rrPolicy );

        gis.addObserver(giso);

        // fineLog( "getInitialContext: rrPolicy = {0}", rrPolicy );

        // this should force the initialization of the resources providers
        if (services !=null) {
            for (ServiceHandle<?> provider : services.getAllServiceHandles(NamingObjectsProvider.class)) {
                provider.getService();
                // no - op. Do nothing with the provided object
            }
//                        for (NamingObjectsProvider provider :
//                            services.getAllByContract(NamingObjectsProvider.class)) {
//                            // no-op
//                        }
        }

        // Get the actual content, not just the configured
        // endpoints.
        giso.forceMembershipChange();

        if(logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "NamingClusterInfoImpl.initGroupInfoService RoundRobinPolicy {0}", rrPolicy);
    }

    @Override
    public void setClusterInstanceInfo(Hashtable<?, ?> myEnv, String defaultHost, String defaultPort,
                                      boolean membershipChangeForced) {
        final List<String> list = getEndpointList(myEnv, defaultHost, defaultPort) ;
        rrPolicy.setClusterInstanceInfoFromString(list);
        if (!membershipChangeForced) {
            giso.forceMembershipChange() ;
        }
    }

    @Override
    public List<String> getNextRotation() {
        return rrPolicy.getNextRotation();
    }

    private List<String> getEndpointList(Hashtable env, String defaultHost, String defaultPort) {
        final List<String> list = new ArrayList<String>() ;
        final String lbpv = getEnvSysProperty( env, LOAD_BALANCING_PROPERTY);
        final List<String> lbList = splitOnComma(lbpv) ;
        if (lbList.size() > 0) {
            final String first = lbList.remove( 0 ) ;
            if (first.equals(IC_BASED) || first.equals(IC_BASED_WEIGHTED)) {
                // XXX concurrency issue here:  possible race on global
                System.setProperty(LOAD_BALANCING_PROPERTY, first );
            }
        }
        list.addAll( lbList ) ;

        if (list.isEmpty()) {
            final String iepv = getEnvSysProperty( env, IIOP_ENDPOINTS_PROPERTY);
            final List<String> epList = splitOnComma(iepv) ;
            list.addAll( epList ) ;
        }

        if (list.isEmpty()) {
            final String urlValue = (String)env.get(
                    ORBLocator.JNDI_PROVIDER_URL_PROPERTY) ;
            list.addAll( rrPolicy.getEndpointForProviderURL( urlValue ) ) ;
        }

        if (list.isEmpty()) {
            String host = getEnvSysProperty( env,
                    ORBLocator.OMG_ORB_INIT_HOST_PROPERTY) ;
            String port = getEnvSysProperty( env,
                    ORBLocator.OMG_ORB_INIT_PORT_PROPERTY) ;

            if (host != null && port != null) {
                list.addAll(rrPolicy.getAddressPortList(host, port) ) ;
                logger.log(Level.WARNING, NO_ENDPOINT_SELECTED, new Object[]{host, port});
            }
        }

        if (list.isEmpty()) {
            if (defaultHost != null && defaultPort != null) {
                list.add( defaultHost + ":" + defaultPort ) ;
            }
        }

        if (list.isEmpty()) {
            throw new RuntimeException("Cannot Proceed. No Endpoints specified.");
        }

        return list ;
    }

    private List<String> splitOnComma( String arg ) {
        final List<String> result = new ArrayList<String>() ;
        if (arg != null) {
            final String[] splits = arg.split( "," ) ;
            if (splits != null) {
                for (String str : splits) {
                    result.add( str.trim() ) ;
                }
            }
        }

        return result ;
    }

    private String getEnvSysProperty( Hashtable env, String pname ) {
        String value = (String)env.get( pname ) ;
        if (value == null) {
            value = System.getProperty( pname ) ;
        }
        return value ;
    }

}
