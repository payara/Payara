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

import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Configuration;
import org.testng.annotations.ExpectedExceptions;
import org.testng.annotations.Test;
import org.testng.annotations.*;
import org.testng.Assert;


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

/** The base class for AMX tests
 */
public class AMXTestBase
{
    String mAdminUser;

    String mAdminPassword;

    String mHost;

    int mPort;

    boolean mDebug;

    private volatile MBeanServerConnection mMBeanServerConnection;

    private volatile ProxyFactory mProxyFactory;
    private volatile DomainRoot   mDomainRoot;

    private volatile Query mQueryMgr;

    protected static void debug(final String s)
    {
        System.out.println("" + s);
    }
    
    protected static void println(final String s)
    {
        System.out.println("" + s);
    }
    
    
    protected static void warning(final String s)
    {
        System.out.println("" + s);
    }


    AMXTestBase()
    {
        //debug("################################ AMXTestBase");
    }

    // might need these later: "admin.user", "admin.password"
    @BeforeClass(description = "get setup and connect to the MBeanServer")
    @Parameters(
    {
        "amx.debug", "amx.rmiport"
    })
    void setUpEnvironment(
            final boolean debug,
            final int port)
    {
        // defined in top-level build.xml
        mHost = System.getProperty("http.host");

        mDebug = debug;
        mPort = port;

        try
        {
            setup();
        }
        catch (Exception ex)
        {
            debug("AMXTestBase: Exception in setting up env. = " + ex);
            ex.printStackTrace();
        }
    }

    static final class MBeansListener implements NotificationListener
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
                    //debug( "Registered: " + objectName );
                    mRegistered.add( objectName );
                }
                else if ( mbs.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION ) )
                {
                    //debug( "Unregistered: " + objectName );
                    mUnregistered.add( objectName );
                }
            }
        }
    }
    
    private static MBeansListener sMBeansListener = null;
    protected synchronized void getMBeansListener() throws Exception
    {
        if ( sMBeansListener == null )
        {
            sMBeansListener = new MBeansListener(mMBeanServerConnection, mDomainRoot.objectName().getDomain());
            sMBeansListener.startListening();
        }
    }
    
    private static boolean sEnabledMonitoring = false;
    
    /**
    Subclasses may override if desired.  AMX will have been started
    and initialized already.
     */
    protected void setup()
    {
        //debug("################################ AMXTestBase.setup");

        final TimingDelta timing = new TimingDelta();
        final TimingDelta overall = new TimingDelta();

        try
        {
            mMBeanServerConnection = _getMBeanServerConnection();
            mProxyFactory = ProxyFactory.getInstance(mMBeanServerConnection);

            //debug( "AMXTestBase.setup(): millis to connect: " + timing.elapsedMillis() );
            mDomainRoot = _getDomainRoot(mMBeanServerConnection);
            //debug( "AMXTestBase.setup(): millis to boot AMX: " + timing.elapsedMillis() );
            mQueryMgr = getDomainRootProxy().getQueryMgr();
            //debug( "AMXTestBase.setup(): millis to get QueryMgr: " + timing.elapsedMillis() );
            
            getMBeansListener();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //debug( "AMXTestBase.setup(): total setup millis: " + overall.elapsedMillis() );
    }

    protected final Query getQueryMgr()
    {
        return mQueryMgr;
    }

    /** get all AMX MBeans that were found when the test started
    Caller should use the QueryMgr if a fresh set is needed */
    protected Set<AMXProxy> getAllAMX()
    {
        final Set<AMXProxy> allAMX = getQueryMgr().queryAll();
        assert allAMX.size() >= 30;
        return allAMX;  
    }

    protected <T> Set<T> getAll(final Class<T> intf)
    {
        return getAll(getAllAMX(), intf);
    }

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

    protected final DomainRoot getDomainRootProxy()
    {
        return mDomainRoot;
    }
    protected final Ext getExt()             { return getDomainRootProxy().getExt(); }


    protected ProxyFactory getProxyFactory() { return mProxyFactory; }
    
    protected final DomainRoot _getDomainRoot(final MBeanServerConnection conn)
            throws MalformedURLException, IOException, java.net.MalformedURLException
    {
        final ObjectName domainRootObjectName = AMXGlassfish.DEFAULT.bootAMX(conn);
        final DomainRoot domainRoot = getProxyFactory().getDomainRootProxy();
        return domainRoot;
    }

    protected final MBeanServerConnection getMBeanServerConnection() { return mMBeanServerConnection; }
    
    private final MBeanServerConnection _getMBeanServerConnection()
            throws MalformedURLException, IOException
    {
        // service:jmx:rmi:///jndi/rmi://192.168.1.8:8686/jmxrmi
        // service:jmx:jmxmp://localhost:8888
        // CHANGE to RMI once it's working
        //
        // final String urlStr = "service:jmx:jmxmp://" + mHost + ":" + mPort;
        final String urlStr = "service:jmx:rmi:///jndi/rmi://" + mHost + ":" + mPort + "/jmxrmi";
        
        final long start = System.currentTimeMillis();

        final JMXServiceURL url = new JMXServiceURL(urlStr);

        final JMXConnector jmxConn = JMXConnectorFactory.connect(url);
        //debug( "BaseAMXTest: connecting to: " + url );
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
        //System.out.println( "Got connection, verified connectivity: " + (System.currentTimeMillis() - start));
        return conn;
    }

    protected static final String NL = System.getProperty("line.separator");

    protected static String getEnvString()
    {
        final Properties props = System.getProperties();
        final StringBuilder buf = new StringBuilder();
        buf.append("SYSTEM PROPERTIES:" + NL);
        for (final Object key : props.keySet())
        {
            buf.append(key);
            buf.append(" = ");
            buf.append("" + props.get(key) + NL);
        }
        final String result = buf.toString();
        return result;
    }
    
    

    /** subclass can override to add more */
    protected Interfaces getInterfaces()
    {
        return haveJSR77() ? new InterfacesGlassfish() : new Interfaces();
    }

    
    /** must be checked dynamically because it's not in the web distribution */
    protected static final Class<? extends AMXProxy> getJ2EEDomainClass()
        throws ClassNotFoundException
    {
        return Class.forName( "org.glassfish.admin.amx.j2ee.J2EEDomain" ).asSubclass(AMXProxy.class);
    }
    
    /** return true if we have the JSR 77 classes */
    protected boolean haveJSR77()
    {
        try
        {
            getJ2EEDomainClass();
            //System.out.println( "FOUND J2EEDomain" );
            return true;
        }
        catch( final Exception e )
        {
            //System.out.println( "NOT FOUND J2EEDomain" );
        }
        return false;
    }
    
    
    protected Set<AMXProxy> findAllContainingType( final String type )
    {
        final Set<AMXProxy> all = getQueryMgr().queryAll();
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
    
    protected <T extends AMXProxy> List<T> getAllDescendents( final AMXProxy top, final Class<T> clazz)
    {
        final AMXProxy[]  a = getQueryMgr().queryDescendants( top.objectName() );
        final List<AMXProxy>  list = ListUtil.newListFromArray(a);
        
        return Util.asProxyList( list, clazz );
    }
    
    List<AMXProxy> getAllMonitoring()
    {
        return getAllDescendents( getDomainRootProxy().getMonitoringRoot(), AMXProxy.class);
    }
}












