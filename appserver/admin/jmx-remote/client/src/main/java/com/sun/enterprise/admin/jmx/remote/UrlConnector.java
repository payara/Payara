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

/*
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/UrlConnector.java,v 1.4 2005/12/25 04:26:30 tcfujii Exp $
 * $Revision: 1.4 $
 * $Date: 2005/12/25 04:26:30 $
 */
package com.sun.enterprise.admin.jmx.remote;

import java.util.Collections;
import java.util.Map;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import javax.security.auth.Subject;
import java.util.logging.Logger;

import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.NotificationFilter;
import javax.management.MBeanServerConnection;
import javax.management.ListenerNotFoundException;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.JMXConnector;

import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
import com.sun.enterprise.admin.jmx.remote.internal.RemoteMBeanServerConnection;
import com.sun.enterprise.admin.jmx.remote.notification.ClientNotificationManager;


/** Abstract class that implements the JMXConnector connected to a URL. 
 * It maintains a state and the state transitions are defined. It is important to know that
 * it handles a specific protocol which is based on <a href="http://java.sun.com/j2se/1.4.2/docs/guide/serialization/index.html">
 * Java Serialization </a>. Both clients and servers are exchanging following:
 * <ul>
 *   <li> Instances of MBeanServerRequestMessage {@link javax.management.remote.message.MBeanServerRequestMessage}. 
 *          This object encapsulates all the objects that clients send. </li>
 *   <li> Instance of MBeanServerResponseMessage {@link javax.management.remote.message.MBeanServerRequestMessage}.
 *          This object returns the response from MBeanServer invocation. </li>
 * </ul>
 * serialVersionUIDs are defined in both these classes.
 * Both client and server sides have to agree on the same versions of classes whose objects are transmitted.
 * Note that a concrete implementation of this class has to provide the actual wire transport, e.g. Http.
 * @author Kedar Mhaswade 
 * @since S1AS8.0
 * @version 1.0
 */

public abstract class UrlConnector implements JMXConnector {
    
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/
    
    protected final JMXServiceURL		serviceUrl;
    protected final Map				environment;
    protected final URL				connectionUrl;
    
    private MBeanServerConnection mbsc;
    private int state;
    private final Object stateLock = new Object();
    private final NotificationBroadcasterSupport connectionNotifier;
    
    private static final int CREATED		= 1;
    private static final int CONNECTED		= 2;
    private static final int CLOSED		= 3;
    
    
    private static final String PROTOCOL_PREFIX = "s1as";
    
    /** The only constructor that initialzes the connector for a client to use.
     * @param serviceUrl        specifies the JMXServiceURL which the server exposes.
     * @param environment       specifies the Map containing name-value pairs.
     * The connector should be in the CREATED state after constructor returns. It has
     * to be in CONNECTED state before calling any MBeanServerConnection method on
     * it.
     */
    protected UrlConnector(JMXServiceURL serviceUrl, Map environment) {
		//debuggerHook();
        logMap(environment);
        this.serviceUrl		= serviceUrl;
        this.environment	= environment;
        validateJmxServiceUrl();
        validateEnvironment();
        this.connectionUrl	= serviceUrl2Url(serviceUrl);
        changeState(CREATED);
        connectionNotifier	= new NotificationBroadcasterSupport();
        logger.fine("Connector created to the url: " + this.connectionUrl);
    }
    
    private void logMap(Map env) {
        final Iterator iter = env.keySet().iterator();
        while (iter.hasNext()) {
            final String key = (String) iter.next();
            final String str = (env.get(key) == null)?null:env.get(key).toString();
            logger.fine(str);
        }
    }
    
    public void addConnectionNotificationListener(NotificationListener listener,
    NotificationFilter filter, Object handback) {
        connectionNotifier.addNotificationListener(listener, filter, handback);
    }
    
    /** Closes the connector and underlying connection to the server, if any.
     * @throws IOException if there is an error in closing the connection
     */
    public void close() throws IOException {
        final String message = "UrlConnector.close: Requires that connector is CONNECTED";
        try {
            assertState(CONNECTED, message);
            //physicalConnection.close();
            /* should be actually closed, when I can take care of persistent connections etc. 
             as of now, it is a no-op. */
	    ClientNotificationManager notifMgr =
                ((RemoteMBeanServerConnection)mbsc).getNotificationManager();
            if (notifMgr != null)
                notifMgr.close();

        }
        catch(Exception e) {
            throw new IOException(e.getMessage());
        }
    }
    
    /** Connects to the remote server. Since there are no defaults, this method
     * as of now throws an UnsupportedException. 
     * @throws UnsupportedException
     * @throws IOException if could not be connected
     * @see #connect(Map)
     */
    public void connect() throws IOException {
        final String msg = "Environment has to be provided";
        throw new UnsupportedOperationException(msg);
    }
    
    /** Attempts to connect to the remote server and creates the MBeanServerConnection instance that 
     * is used by clients. Returns immediately (does nothing), if connector is already in CONNECTED state. 
     * Sun ONE implementation requires that provided Map contains documented values.
     * @param env       a Map containing supported environment.
     * @throws IOException if the connection could not be established.
     */
    public void connect(Map env) throws IOException {
        final String message = "UrlConnector.connect: Requires that connector is not CLOSED";
        assertStateNot(CLOSED, message);
        if (connected()) {
            return;
        }
        try {
            mbsc = MBeanServerConnectionFactory.getRemoteMBeanServerConnection(environment, serviceUrl);
            changeState(CONNECTED);
        }
        catch (Exception e) {
	    e.printStackTrace();
            throw new IOException(e.getMessage());
        }
    }
    
    /** Retunrs the connection-id of the connection. 
     */
    public String getConnectionId() throws IOException {
        return "TODO";
        //return (physicalConnection.getConnectionId());
    }
    
    public MBeanServerConnection getMBeanServerConnection() throws IOException {
        final String message = "Connector should be in CONNECTED state";
        assertState(CONNECTED, message);
        return ( mbsc );
    }
    
    public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject)
    throws IOException {
        
        return ( null );
    }
    
    public void removeConnectionNotificationListener(NotificationListener listener)
    throws ListenerNotFoundException {
    }
    
    public void removeConnectionNotificationListener(NotificationListener l,
    NotificationFilter f, Object handback) throws ListenerNotFoundException {
    }
    
    protected void validateEnvironment() throws RuntimeException {
        final boolean userPresent	= environment.containsKey(DefaultConfiguration.ADMIN_USER_ENV_PROPERTY_NAME);
        final boolean pwdPresent	= environment.containsKey(DefaultConfiguration.ADMIN_PASSWORD_ENV_PROPERTY_NAME);
        logger.fine("USERPRESENT: " + userPresent);
        logger.fine("PWDPRESENT: " + pwdPresent);
        if (! (userPresent && pwdPresent) ) {
            throw new IllegalArgumentException("User and Password has to be there in the map");
        }
        final String adminUser = (String)
        environment.get(DefaultConfiguration.ADMIN_USER_ENV_PROPERTY_NAME);
        final String adminPassword = (String)
        environment.get(DefaultConfiguration.ADMIN_PASSWORD_ENV_PROPERTY_NAME);
        //validateString(adminUser);
        //validateString(adminPassword);
    }
    
    protected abstract void validateJmxServiceUrl() throws RuntimeException;
    
    private void validateString(String str) throws RuntimeException {
        //This may not be required -- username/password could be empty strings.
        if (str == null || str.length() == 0) {
            throw new RuntimeException(NULL_STR_MESSAGE);
        }
    }
    /** Utility method. If the passed serviceUrl is valid, it should always
     * create a valid URL.
     */
    protected URL serviceUrl2Url(JMXServiceURL serviceUrl) throws RuntimeException {
        try {
            final String transportProtocol	= getTransport(serviceUrl.getProtocol());
            final String host			= serviceUrl.getHost();
            final int port			= serviceUrl.getPort();
            
/* BEGIN -- S1WS_MOD */
            String remainder = serviceUrl.getURLPath();
            if (remainder == null || remainder.trim().length() == 0)
                remainder = DefaultConfiguration.DEFAULT_SERVLET_CONTEXT_ROOT;
/* END -- S1WS_MOD */
            return ( new URL(transportProtocol, host, port, remainder) );
        }
        catch (MalformedURLException mu) {
            throw new RuntimeException(mu.getMessage());
        }
    }
    
    private String getTransport(String proprietoryProtocolString) {
        return proprietoryProtocolString.substring(PROTOCOL_PREFIX.length());
    }
    
    private void assertState(int legalState, String message) throws IllegalStateException {
        synchronized(stateLock) {
            if (state != legalState) {
                throw new IllegalStateException(message);
            }
        }
    }
    private void assertStateNot(int illegalState, String message) throws IllegalStateException {
        synchronized(stateLock) {
            if (state == illegalState) {
                throw new IllegalStateException(message);
            }
        }
    }
    
    private void changeState(int toState) {
                                /* Since states are not directly changeable by the end-user, it needs
                                   to be asserted if there is any program error */
        assert	(toState == CREATED || toState == CONNECTED || toState == CLOSED):
            ("This is illegal state transition, to: " + toState);
            synchronized(stateLock) {
                state = toState;
            }
    }
    
    private boolean connected() {
        synchronized(stateLock) {
            return ( state == CONNECTED );
        }
    }
    //Localization
    private final String NULL_STR_MESSAGE		= "String is null";
	
	private void debuggerHook() {
		try {
			// DO NOT l10n
			System.out.println("Attached the debugger? Provide an integer to go ahead...");
			final int r = System.in.read();
		}
		catch(Exception e) {
                    e.printStackTrace();
                }
	}
}
