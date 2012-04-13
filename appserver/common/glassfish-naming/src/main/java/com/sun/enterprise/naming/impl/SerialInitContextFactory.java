/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.logging.LogDomains;
import org.glassfish.api.naming.NamingClusterInfo;
import org.glassfish.hk2.Services;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ORBLocator;
import org.omg.CORBA.ORB;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.naming.spi.NamingManager;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.glassfish.api.naming.NamingClusterInfo.*;

public class SerialInitContextFactory implements InitialContextFactory {
    private static volatile boolean initialized = false ;

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

    private static Services defaultServices = null ;

    static void setDefaultHost(String host) {
        defaultHost = host;
    }

    static void setDefaultPort(String port) {
        defaultPort = port;
    }

    static void setDefaultServices(Services h) {
        defaultServices = h;

    }

    static Services getDefaultServices() {
        return defaultServices;
    }

    private final Services services;


    private boolean propertyIsSet( Hashtable env, String pname ) {
        String value = getEnvSysProperty( env, pname ) ;
        return value != null && !value.isEmpty() ;
    }

    private String getEnvSysProperty( Hashtable env, String pname ) {
        String value = (String)env.get( pname ) ;
        if (value == null) {
            value = System.getProperty( pname ) ;
        }
        return value ;
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
        Services temp = defaultServices;
        if (temp == null) {
            temp = Globals.getDefaultServices() ;
        }
        if (temp == null) {
            // May need to initialize hk2 component model in standalone client
            temp = Globals.getStaticHabitat() ;
        }
        services = temp ;
    }

    private ORB getORB() {
        if (services != null) {
            ORBLocator orbLocator = services.forContract(ORBLocator.class).get() ;
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
        boolean useLB = propertyIsSet(myEnv, IIOP_ENDPOINTS_PROPERTY)
            || propertyIsSet(myEnv, LOAD_BALANCING_PROPERTY) ;
        NamingClusterInfo namingClusterInfo = null;

        if (useLB && !initialized) {
            synchronized( SerialInitContextFactory.class ) {
                if (!initialized) {
                    namingClusterInfo = services.forContract(NamingClusterInfo.class).get();
                    namingClusterInfo.initGroupInfoService(myEnv, defaultHost, defaultPort, getORB(), services);
                    membershipChangeForced = true ;
                    initialized = true ;
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
                if(namingClusterInfo == null) {
                    namingClusterInfo = services.forContract(NamingClusterInfo.class).get();
                }
                if (myEnv.containsKey( IIOP_ENDPOINTS_PROPERTY ) ||
                    myEnv.containsKey( LOAD_BALANCING_PROPERTY )) {
                    synchronized( SerialInitContextFactory.class ) {
                        namingClusterInfo.setClusterInstanceInfo(myEnv, defaultHost, defaultPort, membershipChangeForced);
                    }
                }

                List<String> rrList = namingClusterInfo.getNextRotation();
                fineLog( "getInitialContext: RoundRobinPolicy list = {0}", rrList);
                myEnv.put(IIOP_URL_PROPERTY, getCorbalocURL(rrList));
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
        SerialContext serialContext = new SerialContext(env, services);
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
