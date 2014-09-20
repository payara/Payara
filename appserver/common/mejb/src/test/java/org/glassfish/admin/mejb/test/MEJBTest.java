/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 */
package org.glassfish.admin.mejb.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.rmi.PortableRemoteObject;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.naming.InitialContext;

import javax.management.j2ee.ManagementHome;

import com.sun.enterprise.security.ee.auth.login.ProgrammaticLogin;
import org.glassfish.external.amx.AMXGlassfish;

import javax.management.j2ee.Management;


import org.junit.Ignore;

/**
    Standalone MEJB test -- requires running server and disabling security on MEJB.
    
export V3M=/v3/glassfish/modules
export MAIN=org.glassfish.admin.mejb.test.MEJBTest
java -cp $V3M/gf-client.jar:$V3M/javax.management.j2ee.jar:target/MEJB.jar $MAIN

 */
@Ignore
public class MEJBTest {    
    private final Management mMEJB;
    
    public MEJBTest( final Management mejb )
    {
        mMEJB = mejb;
    }
    
    private void test() {
        try
        {
            _test();
        }
        catch( final Exception e)
        {
            e.printStackTrace();
        }
        println( "DONE with MEJB test." );
    }
    
    
    private void testMBean( final ObjectName objectName)
        throws Exception
    {
        println( "" );
        println( "" + objectName );
        
        final Management mejb = mMEJB;
        final MBeanInfo info = mejb.getMBeanInfo(objectName);
        final String[] attrNames = getAttributeNames( info.getAttributes() );
        
        println( "attributes: " + toString( newListFromArray(attrNames), ", " ) );
        
        final AttributeList list = mejb.getAttributes( objectName, attrNames );
        
        for( final String attrName : attrNames)
        {
            try
            {
                final Object value = mejb.getAttribute( objectName, attrName );
            }
            catch( Exception e )
            {
                println( "Attribute failed: " + attrName );
            }
        }
    }
    
    private void _test()
        throws Exception
    {
        final Management mejb = mMEJB;
        
        final String defaultDomain = mejb.getDefaultDomain();
        println("MEJB default domain = " + defaultDomain + ", MBeanCount = " + mejb.getMBeanCount() );
        
        final String domain = AMXGlassfish.DEFAULT.amxJMXDomain();
        final ObjectName pattern = newObjectName( domain + ":*" );
        final Set<ObjectName> items = mejb.queryNames( pattern, null);
        println("Queried " + pattern + ", got mbeans: " + items.size() );
        for( final ObjectName objectName : items )
        {
            if ( mejb.isRegistered(objectName) )
            {
                testMBean(objectName);
            }
        }
        
        // add listeners to all
        println( "Listener are not supported, skipping." );
        /*
        println( "Adding listeners to every MBean..." );
        
        final ListenerRegistration reg = mejb.getListenerRegistry();
        println( "Got ListenerRegistration: " + reg );
        final NotificationListener listener = new NotifListener();
        for( final ObjectName objectName : items )
        {
            if ( mejb.isRegistered(objectName) )
            {
                final NotificationFilter filter = null;
                final Object handback = null;
                try {
                    reg.addNotificationListener( objectName, listener, filter, handback );
                }
                catch( final Exception e )
                {
                    e.printStackTrace();
                }
            }
        }
        */
    }
    
    
    private static final class NotifListener implements NotificationListener
    {
        public NotifListener()
        {
        }
        
        public void handleNotification( final Notification notif, final Object handback )
        {
            System.out.println( "NotifListener: " + notif);
        }
    }

		public static String
	toString(
		final Collection<?> c,
		final String	 delim )
	{
        final StringBuffer buf = new StringBuffer();
        
        for( final Object item : c )
        {
            buf.append( "" + item );
            buf.append( delim );
        }
        if( c.size() != 0)
        {
            buf.setLength( buf.length() - delim.length() );
        }
        
        return buf.toString();
    }
            
		public static <T> List<T>
	newListFromArray( final T []  items )
	{
		final List<T>	list	= new ArrayList<T>();
		
		for( int i = 0; i < items.length; ++i )
		{
			list.add( items[ i ] );
		}

		return( list );
	}


	public static String []
	getAttributeNames( final MBeanAttributeInfo[]	infos  )
	{
		final String[]	names	= new String[ infos.length ];
		
		for( int i = 0; i < infos.length; ++i )
		{
			names[ i ]	= infos[ i ].getName();
		}
		
		return( names );
	}

    static ObjectName
	newObjectName( final String name )
	{
		try
		{
			return( new ObjectName( name ) ); 
		}
		catch( Exception e )
		{
			throw new RuntimeException( e.getMessage(), e );
		}
	}



public static void main(String[] args) {
    try {
        final String mejbName = "java:global/mejb/MEJBBean";
        final String username = "admin";
        final String password = "";
        final String realm = "admin-realm";
        System.out.println( "Authenticating with \"" + username + "\", \"" + password + "\"");
        
        final ProgrammaticLogin pm = new ProgrammaticLogin();
        pm.login( username, password, realm, true);

        println("Looking up: " + mejbName);
        final InitialContext initial = new InitialContext();
        final Object objref = initial.lookup(mejbName);

        final ManagementHome home = (ManagementHome) PortableRemoteObject.narrow(objref, ManagementHome.class);
        try
        {
            final ManagementHome home2 = (ManagementHome)objref;
        }
        catch( final Exception e )
        {
            println("WARNING: (ManagementHome)PortableRemoteObject.narrow(objref, ManagementHome.class) works, but (ManagementHome)objref does not!" );
        }
        
        //println("ManagementHome: " + home + " for " + mejbName);
        final Management mejb = (Management)home.create();
        println("Got the MEJB");

        new MEJBTest( mejb ).test();

        println( "Calling mejb.remove()" );
        mejb.remove();

    } catch (Exception ex) {
        System.err.println("Caught an unexpected exception!");
        ex.printStackTrace();
    }
    println( "Exiting main() forcibly" );
    System.exit( -1 );
}

    private static final void println(final Object o) {
        System.out.println("" + o);
    }
}
