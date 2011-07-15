/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

/* CVS information
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/server/MBeanServerRequestHandler.java,v 1.4 2005/12/25 04:26:36 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:36 $
 */


package com.sun.enterprise.admin.jmx.remote.server;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.Iterator;
import java.util.logging.Logger;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.MBeanServerResponseMessage;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.security.auth.Subject;
/* BEGIN -- S1WS_MOD */
import javax.servlet.ServletConfig;

import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
/* END -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.protocol.Version;
/* BEGIN -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.server.notification.ServerNotificationManager;
/* END -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.server.callers.MethodCallers;
import com.sun.enterprise.admin.jmx.remote.server.callers.MBeanServerConnectionMethodCaller;

import com.sun.enterprise.admin.jmx.remote.internal.Shifter;
/* BEGIN -- S1WS_MOD */

/* END -- S1WS_MOD */

/** Handles the request that is received from the servlet. Responsible for
 * producing a guaranteed MBeanServerResponseMessage. Delegates the requests
 * to the System MBean Server. Currently it searches for the MBeanServer after
 * finding the list of MBeanServers.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public class MBeanServerRequestHandler {
    
    /** Creates a new instance of MBeanServerResponseHandler */
    
    final private Set callers;

    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

    private static Version sv;
    static {
            try {
                    sv = (Version)Class.forName(Version.CLASS_NAME).newInstance(); 
            }
            catch(Exception e) {
                    throw new RuntimeException(e);
            }
    }
	
    final private static ServerVersionMatcher matcher = ServerVersionMatcher.getMatcher();
/* BEGIN -- S1WS_MOD */
    private ServerNotificationManager notifyMgr = null;

//    public MBeanServerRequestHandler() {
    public MBeanServerRequestHandler(ServletConfig cfg) {
        MBeanServerConnection mbsc = getMBeanServerConnection(cfg);
        notifyMgr = new ServerNotificationManager(mbsc);
        notifyMgr.setBufSiz(cfg);
//        callers	= MethodCallers.callers(getMBeanServerConnection());
        callers	= MethodCallers.callers(mbsc, notifyMgr);
/* END -- S1WS_MOD */
		logger.finer("Server Jmx Connector Version: " + sv.toString());
    }

/* BEGIN -- S1WS_MOD */
    public ServerNotificationManager getNotificationManager() {
        return notifyMgr;
    }
    
//    private MBeanServerConnection getMBeanServerConnection() {
    private MBeanServerConnection getMBeanServerConnection(ServletConfig cfg) {
		//first get through reflection.
        String factoryClass = cfg.getInitParameter(DefaultConfiguration.MBEANSERVER_FACTORY_PROPERTY_NAME);
//		final MBeanServerConnection mbsc = introspectMBS();
		final MBeanServerConnection mbsc = introspectMBS(factoryClass);
/* END -- S1WS_MOD */
		if (mbsc != null) {
			return mbsc;
		}
        final java.util.ArrayList servers = MBeanServerFactory.findMBeanServer(null);
        final MBeanServer systemMBS = (MBeanServer)servers.get(0);
        return ((MBeanServerConnection)systemMBS);
    }
    
    public MBeanServerResponseMessage handle(MBeanServerRequestMessage request0) {
		assert (request0.getParams().length >= 1) : "Invalid Object Array"; //has to have at least one elem
        MBeanServerResponseMessage response = null;
		final Version cv = (Version)request0.getParams()[0];
		logger.finer("Client Version = " + cv.toString());
		if (! isCompatible(cv)) {
			response = incompatibleVersionMessage(request0);
			return ( response );
		}
		//should come here iff the version is compatible.
		final MBeanServerRequestMessage request = removeVersion(request0);
        boolean handled = false;
        final Iterator iter = callers.iterator();
        while (iter.hasNext()) {
            final MBeanServerConnectionMethodCaller caller =
            (MBeanServerConnectionMethodCaller)iter.next();
            if (caller.canCall(request)) {
                response = caller.call(request);
                handled = true;
                break;
            }
        }
        assert handled : "The request is not handled -- catastrophe";
        return ( response );
    }
	
	/** Returns the instance of S1AS MBeanServer. This information could well
	 * come from deployment descriptor, but for now I am hardcoding it -- 08/04/03.
	 * Returns a null if none could be found.
	 * @return		MBeanServerConnection instance or null
	 */
/* BEGIN -- S1WS_MOD */
//	private MBeanServerConnection introspectMBS() {
	private MBeanServerConnection introspectMBS(String factoryClass) {
/* END -- S1WS_MOD */
		MBeanServerConnection mbsc = null;
/* BEGIN -- S1WS_MOD */
		//final String FACTORY_CLASS = "com.sun.enterprise.admin.common.MBeanServerFactory";
		String FACTORY_CLASS = factoryClass;
        if (factoryClass == null || factoryClass.trim().length() == 0)
            FACTORY_CLASS = System.getProperty(DefaultConfiguration.MBEANSERVER_FACTORY_PROPERTY_NAME);
/* END -- S1WS_MOD */
		final String FACTORY_METHOD = "getMBeanServer";
/* BEGIN -- S1WS_MOD */
        if (FACTORY_CLASS == null || FACTORY_CLASS.trim().length() == 0)
            return null;
/* END -- S1WS_MOD */
		try {
			logger.finer("Introspecting the MBeanServerConnection");
			final Class c = Class.forName(FACTORY_CLASS); //loaded by the same CL
			final Method m = c.getMethod(FACTORY_METHOD, null);
			final Object r = m.invoke(c, null);
			assert (r instanceof MBeanServer) : "Reflection does not return the correct type";
			mbsc = (MBeanServerConnection)r;
			logger.finer("Introspected the MBeanServerConnection successfully!!");
		}
		catch (Throwable t) {
			logger.throwing(this.getClass().getName(), "introspectMBS", t);
		}
		return ( mbsc );
	}
	
	private MBeanServerRequestMessage removeVersion(MBeanServerRequestMessage from) {
		final int id = from.getMethodId();
		final Subject s = from.getDelegationSubject();
		final Shifter sh = new Shifter(from.getParams());
		sh.shiftLeft();
		final Object[] np = sh.state();
		return ( new MBeanServerRequestMessage(id, np, s) );
	}
	private boolean isCompatible(Version cv) {
		return ( matcher.match(cv, sv) );
	}
	
	private MBeanServerResponseMessage incompatibleVersionMessage(MBeanServerRequestMessage r) {
		//should come in this method only in case of incompatible versions
		final int id				= r.getMethodId();
		final boolean isException	= true;
		final Version cv = (Version)r.getParams()[0];
		Exception e = null;
		assert (! isCompatible(cv)) : "No message for compatible versions";
		
		if (! matcher.majorCompatible(cv, sv)) {
			e = majorVersionIncompatible(cv, sv);
		}
		else if (! matcher.minorCompatible(cv, sv)) {
			e = minorVersionIncompatible(cv, sv);
		}
		else if (! matcher.upgradeCompatible(cv, sv)) {
			e = upgradeIncompatible(cv, sv);
		}
		assert (e != null) : "Either minor/major version or upgrade data have to fail the match";
		
		return ( new MBeanServerResponseMessage(id, e, isException) );
	}
	
	private Exception majorVersionIncompatible(Version cv, Version sv) {
		//i18n
		final StringBuffer sb = new StringBuffer();
		sb.append("The major versions don't match: ").
		append("Client Major Version = " + cv.getMajorVersion()).
		append("Server Major Version = " + sv.getMajorVersion()).
		append(" Upgrade the software accordingly");
		
		return ( new RuntimeException(sb.toString()) );
	}
	private Exception minorVersionIncompatible(Version cv, Version sv) {
		//i18n
		final StringBuffer sb = new StringBuffer();
		sb.append("The minor versions don't match: ").
		append("Client Minor Version = " + cv.getMinorVersion()).
		append("Server Minor Version = " + sv.getMinorVersion()).
		append(" Upgrade the software accordingly");
		
		return ( new RuntimeException(sb.toString()) );
	}
	private Exception upgradeIncompatible(Version cv, Version sv) {
		//i18n
		final StringBuffer sb = new StringBuffer();
		sb.append("The upgrade data in versions does not match: ").
		append("Client Upgrade Data = " + cv.toString()).
		append("Server Upgrade Data = " + sv.toString()).
		append(" Upgrade the software accordingly");
		
		return ( new RuntimeException(sb.toString()) );
	}
}
