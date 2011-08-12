/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import com.sun.corba.ee.spi.folb.GroupInfoService;
import com.sun.logging.LogDomains;
import java.util.ArrayList;
import org.glassfish.api.naming.NamingObjectsProvider;
import org.jvnet.hk2.component.Habitat;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ORBLocator;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;

public class SerialInitContextFactory implements InitialContextFactory {
    public static final String LOAD_BALANCING_PROPERTY =
            "com.sun.appserv.iiop.loadbalancingpolicy";

    public static final String IIOP_ENDPOINTS_PROPERTY =
            "com.sun.appserv.iiop.endpoints";

    public static final String IIOP_URL_PROPERTY =
            "com.sun.appserv.ee.iiop.endpointslist";

    public static final String IC_BASED_WEIGHTED = "ic-based-weighted";

    public static final String IC_BASED = "ic-based";

    public static final String IIOP_URL = "iiop:1.2@";

    public static final String CORBALOC = "corbaloc:";

    private static volatile boolean initialized = false ;

    private static volatile RoundRobinPolicy rrPolicy ;

    private static volatile GroupInfoServiceObserverImpl giso ;

    protected static final Logger _logger = LogDomains.getLogger(
        SerialInitContextFactory.class, LogDomains.JNDI_LOGGER );

    private static void doLog( Level level, String fmt, Object... args )  {
        if (_logger.isLoggable(level)) {
            _logger.log( level, fmt, args ) ;
        }
    }

    private static void fineLog( String fmt, Object... args ) {
        doLog( Level.FINE, fmt, args ) ;
    }

    private static String defaultHost = null ;

    private static String defaultPort = null ;

    private static Habitat defaultHabitat = null ;

    static void setDefaultHost(String host) {
        defaultHost = host;
    }

    static void setDefaultPort(String port) {
        defaultPort = port;
    }

    static void setDefaultHabitat(Habitat h) {
        defaultHabitat = h;

    }

    static Habitat getDefaultHabitat() {
        return defaultHabitat;
    }

    private boolean useLB ;

    private final Habitat habitat ;


    private boolean propertyIsSet( Hashtable env, String pname ) {
        String value = getEnvSysProperty( env, pname ) ;
        return value != null && !value.isEmpty() ;
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

    private List<String> getEndpointList( Hashtable env ) {
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
                list.addAll(
                    rrPolicy.getAddressPortList(host, port) ) ;
                _logger.log(Level.WARNING, "no.endpoints.selected",
                        new Object[] {host, port});
            }
        }

        if (list.isEmpty()) {
            if (defaultHost != null && defaultPort != null) {
                list.add( defaultHost + ":" + defaultPort ) ;
            }
        }

        if (list.isEmpty()) {
            _logger.log(Level.SEVERE, "no.endpoints");
            throw new RuntimeException("Cannot Proceed. No Endpoints specified.");
        }

        return list ;
    }

    private String getCorbalocURL( final List<String> list) {
        final StringBuilder sb = new StringBuilder() ;
        boolean first = true ;
        for (String str : list) {
	    if (first) {
                first = false ;
                sb.append( CORBALOC ) ;
	    } else {
                sb.append( ',' ) ;
	    }

            sb.append( IIOP_URL ) ;
            sb.append( str.trim() ) ;
	}

	// fineLog( "corbaloc url ==> {0}", sb.toString() );

	return sb.toString() ;
    }

    public SerialInitContextFactory() {
        // Issue 14396
        Habitat temp = defaultHabitat ;
        if (temp == null) {
            temp = Globals.getDefaultHabitat() ;
        }
        if (temp == null) {
            // May need to initialize hk2 component model in standalone client
            temp = Globals.getStaticHabitat() ;
        }
        habitat = temp ;
    }

    private ORB getORB() {
        if (habitat != null) {
            ORBLocator orbLocator = habitat.getByContract(ORBLocator.class) ;
            if (orbLocator != null) {
                return orbLocator.getORB() ;
            }
        }

        throw new RuntimeException( "Could not get ORB" ) ;
    }

    /**
     * Create the InitialContext object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public Context getInitialContext(Hashtable env) throws NamingException {
        final Hashtable myEnv = env == null ? new Hashtable() : env ;

        boolean membershipChangeForced = false ;

        fineLog( "getInitialContext: env={0}", env ) ;
        useLB = propertyIsSet(myEnv, IIOP_ENDPOINTS_PROPERTY)
            || propertyIsSet(myEnv, LOAD_BALANCING_PROPERTY) ;

        if (useLB && !initialized) {
            synchronized( SerialInitContextFactory.class ) {
                if (!initialized) {
                    // Always create one rrPolicy to be shared, if needed.
                    final List<String> epList = getEndpointList( myEnv ) ;
                    rrPolicy = new RoundRobinPolicy(epList);

                    GroupInfoService gis = null ;
                    try {
                        gis = (GroupInfoService) (getORB().resolve_initial_references(
                            ORBLocator.FOLB_CLIENT_GROUP_INFO_SERVICE));
                    } catch (InvalidName ex) {
                        doLog(Level.SEVERE,
                            "Exception in SerialInitContextFactory constructor {0}",
                                ex);
                    }

                    giso = new GroupInfoServiceObserverImpl( gis, rrPolicy );

                    gis.addObserver(giso);

                    // fineLog( "getInitialContext: rrPolicy = {0}", rrPolicy );

                    // this should force the initialization of the resources providers
                    if (habitat!=null) {
                        for (NamingObjectsProvider provider :
                            habitat.getAllByContract(NamingObjectsProvider.class)) {
                            // no-op
                        }
                    }

                    // Get the actual content, not just the configured
                    // endpoints.
                    giso.forceMembershipChange();
                    membershipChangeForced = true ;

                    initialized = true ;

                    fineLog( "getInitialContext(initial): rrPolicy = {0}",
                        rrPolicy );
                }
            }
        }

        if (useLB || initialized)  {
            // If myEnv already contains the IIOP_URL, don't get a new one:
            // this getInitialContext call came from an internal
            // new InitialContext call.
            if (!myEnv.containsKey(IIOP_URL_PROPERTY)) {
                Context ctx = SerialContext.getStickyContext() ;
                if (ctx != null) {
                    return ctx ;
                }

                // If the IIOP endpoint list is explicitly set in the env,
                // update rrPolicy to use that information, otherwise just
                // rotate rrPolicy to the next element.
                if (myEnv.containsKey( IIOP_ENDPOINTS_PROPERTY ) ||
                    myEnv.containsKey( LOAD_BALANCING_PROPERTY )) {
                    synchronized( SerialInitContextFactory.class ) {
                        final List<String> list = getEndpointList( myEnv ) ;
                        rrPolicy.setClusterInstanceInfoFromString(list);
                        if (!membershipChangeForced) {
                            giso.forceMembershipChange() ;
                        }
                    }
                }

                List<String> rrList = rrPolicy.getNextRotation();
                fineLog( "getInitialContext: rrPolicy = {0}", rrPolicy );

                String corbalocURL = getCorbalocURL(rrList);

                myEnv.put(IIOP_URL_PROPERTY, corbalocURL);
            }

            myEnv.put(ORBLocator.JNDI_CORBA_ORB_PROPERTY, getORB());
        } else {
            if (defaultHost != null) {
                myEnv.put( ORBLocator.OMG_ORB_INIT_HOST_PROPERTY, defaultHost ) ;
            }

            if (defaultPort != null) {
                myEnv.put( ORBLocator.OMG_ORB_INIT_PORT_PROPERTY, defaultPort ) ;
            }
        }

        return createInitialContext(myEnv);
    }

    private Context createInitialContext(Hashtable env) throws NamingException
    {
        SerialContext serialContext = new SerialContext(env, habitat);
        if (NamingManager.hasInitialContextFactoryBuilder()) {
            // When builder is used, JNDI does not go through
            // URL Context discovery anymore. To address that
            // we install a wrapper that first goes through
            // URL context discovery and then falls back to
            // serialContext.
            return new WrappedSerialContext(env, serialContext);
        } else {
            return serialContext ;
        }
    }
}
