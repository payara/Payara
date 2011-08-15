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

package amxtest;

import java.util.Properties;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import java.net.MalformedURLException;
import java.io.IOException;

import javax.management.ObjectName;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.MBeanServerDelegateMBean;
import javax.management.MBeanServerNotification;
import javax.management.MBeanServerConnection;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.glassfish.admin.amx.base.*;
import org.glassfish.admin.amx.core.*;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.config.AMXConfigProxy;
import org.glassfish.admin.amx.util.TimingDelta;
import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.jmx.JMXUtil;


import org.glassfish.external.amx.AMXGlassfish;

/** Demonstration AMX java client */
public final class Demo {
    final String  mAdminUser;
    final String  mAdminPassword;
    final String  mHost;
    final int     mPort;
    final boolean mDebug;
    private final MBeanServerConnection mMBeanServerConnection;

    private final ProxyFactory mProxyFactory;
    private final DomainRoot   mDomainRoot;
    
    private final MBeansListener  mListener;
    
    private final boolean   mPrintln;

    private final Query mQueryMgr;

    /**
        Get a connection to the server, and setup various high-level MBean proxies.
     */
    Demo(
        final boolean emitMessages,
        final String host,
        final String port,
        final String user,
        final String password ) throws Exception
    {
        mPrintln = emitMessages;
        
        mDebug = true;
        mHost = host;
        mPort = Integer.parseInt(port);
        mAdminUser     = user;
        mAdminPassword = password;
        
        mMBeanServerConnection = _getMBeanServerConnection(mHost, mPort);
        
        mProxyFactory = ProxyFactory.getInstance(mMBeanServerConnection);
        mDomainRoot   = mProxyFactory.getDomainRootProxy();
        mQueryMgr     = mDomainRoot.getQueryMgr();
            
        mListener = new MBeansListener(mMBeanServerConnection, mDomainRoot.objectName().getDomain());
        mListener.startListening();
        
        println( "Demo: " + host + ":" + port + " as {" + user + "," + password + "}" );
    }
    
    private static String getArg(final String name, final String defaultValue, final String[] args )
    {
        String value = defaultValue;
        for( final String arg : args )
        {
            if ( arg == null ) continue;
            
            if ( arg.startsWith(name) || arg.startsWith("--" + name) )
            {
                final int idx = arg.indexOf("=");
                value = arg.substring(idx+1);
                break;
            }
        }
        return value;
    }
    
    /**
        Run the demo.
     */
    public static void runDemo( final boolean emitMessages, final String[] args )
    {
        final String host = getArg("host", "localhost", args);
        final String port = getArg("port", "8686", args);
        final String user = getArg("user", "admin", args);
        final String password = getArg("password", "changeit", args);
        
        try
        {
            final Demo demo = new Demo(emitMessages, host, port, user, password);
            
            demo.run();
        }
        catch( final Throwable t )
        {
            t.printStackTrace();
        }
    }
    
    public static void main( final String[] args )
    {
        runDemo( true, args );
    }
    
    private void run()
    {
        final Set<AMXProxy>  amxMBeans = mQueryMgr.queryAll();
        println( "AMX MBeans: " + amxMBeans.size() );
        println( "AMX config MBeans: " + getAllConfig().size() );
        
    }    
            
    /** get all AMX MBeans that were found when the test started
    Caller should use the QueryMgr if a fresh set is needed */
    protected Set<AMXProxy> getAllAMX()
    {
        final Set<AMXProxy> allAMX = mQueryMgr.queryAll();
        assert allAMX.size() >= 30;
        return allAMX;  
    }

    /** get all AMX MBeans with the specified interface */
    protected <T> Set<T> getAll(final Class<T> intf)
    {
        return getAll(getAllAMX(), intf);
    }

    /** Filter all AMXProxy with the specified interface */
    protected <T> Set<T> getAll(final Set<AMXProxy> all, final Class<T> intf)
    {
        final Set<T> result = new HashSet<T>();
        for (final AMXProxy amx : all)
        {
            if (intf.isAssignableFrom(amx.getClass()))
            {
                result.add(intf.cast(amx));
            }
        }
        return result;
    }
    
    private static MBeanServerConnection _getMBeanServerConnection(final String host, final int port)
            throws MalformedURLException, IOException
    {
        final long start = System.currentTimeMillis();
        
        final String urlStr = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
        final JMXServiceURL url = new JMXServiceURL(urlStr);

        final JMXConnector jmxConn = JMXConnectorFactory.connect(url);
        //debug( "Demo: connecting to: " + url );
        final MBeanServerConnection conn = jmxConn.getMBeanServerConnection();
        conn.getDomains();	// sanity check
        try
        {
            final ObjectName domainRootObjectName = AMXGlassfish.DEFAULT.bootAMX(conn);
        }
        catch( final Exception e )
        {
            System.err.println( ExceptionUtil.toString(ExceptionUtil.getRootCause(e)) );
        }
        //debug( "Demo: got connection, verified connectivity: " + (System.currentTimeMillis() - start));
        return conn;
    }

    /** Find all AMX MBeans of the specified type */
    protected Set<AMXProxy> findAllContainingType( final String type )
    {
        final Set<AMXProxy> all = mQueryMgr.queryAll();
        final Set<AMXProxy> parentsWith = new HashSet<AMXProxy>();
        for( final AMXProxy amx : all )
        {
            if ( amx.type().equals(type) )
            {
                final AMXProxy parent = amx.parent();
                parentsWith.add(parent);
            }
        }
        return parentsWith;
    }
    
    /** Get all AMX MBeans that are descendendts of the specified MBean */
    protected <T extends AMXProxy> List<T> getAllDescendents( final AMXProxy top, final Class<T> clazz)
    {
        final AMXProxy[]  a = mQueryMgr.queryDescendants( top.objectName() );
        final List<AMXProxy>  list = ListUtil.newListFromArray(a);
        
        return Util.asProxyList( list, clazz );
    }
    
    /** Get all config MBeans */
    List<AMXConfigProxy> getAllConfig()
    {
        return getAllDescendents( mDomainRoot, AMXConfigProxy.class);
    }
    
    /** Get all monitoring MBeans */
    List<AMXProxy> getAllMonitoring()
    {
        return getAllDescendents( mDomainRoot.getMonitoringRoot(), AMXProxy.class);
    }


    /**
        Listen to MBeans for Notifications.
     */
    final class MBeansListener implements NotificationListener
    {
        private final MBeanServerConnection mServer;
        private final String                mDomain;
        
        private final List<ObjectName> mRegistered = Collections.synchronizedList(new ArrayList<ObjectName>());
        private final List<ObjectName> mUnregistered = Collections.synchronizedList(new ArrayList<ObjectName>());
        
        public MBeansListener(final MBeanServerConnection server, final String domain)
        {
            mServer  = server;
            mDomain  = domain;
        }
        public void startListening()
        {
            final ObjectName delegate = JMXUtil.newObjectName( JMXUtil.MBEAN_SERVER_DELEGATE );
            try
            {
                mServer.addNotificationListener( delegate, this, null, null);
            }
            catch( final Exception e )
            {
                throw new RuntimeException(e);
            }
        }
        
        public void handleNotification(
            final Notification notif,
            final Object handback) 
        {
            if ( ! (notif instanceof MBeanServerNotification) )
            {
                return;
            }
            final MBeanServerNotification mbs = (MBeanServerNotification)notif;
            final ObjectName objectName = mbs.getMBeanName();
            if  ( "*".equals(mDomain) || mDomain.equals(objectName.getDomain()) )
            {
                if ( mbs.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION ) )
                {
                   // debug( "Registered: " + objectName );
                    mRegistered.add( objectName );
                }
                else if ( mbs.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION ) )
                {
                   // debug( "Unregistered: " + objectName );
                    mUnregistered.add( objectName );
                }
            }
        }
    }
    
    

    protected void debug(final String s) {
        System.out.println("" + s);
    }
    
    protected void println(final String s) {
        if ( mPrintln ) {
            System.out.println("" + s);
        }
    }
    
    protected void warning(final String s) {
        if ( mPrintln ) {
            System.out.println("" + s);
        }
    }

}












