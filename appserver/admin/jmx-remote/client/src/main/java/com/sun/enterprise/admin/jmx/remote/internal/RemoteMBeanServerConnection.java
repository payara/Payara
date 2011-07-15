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
 * $Header: /cvs/glassfish/jmx-remote/rjmx-impl/src/java/com/sun/enterprise/admin/jmx/remote/internal/RemoteMBeanServerConnection.java,v 1.5 2005/12/25 04:26:34 tcfujii Exp $
 * $Revision: 1.5 $
 * $Date: 2005/12/25 04:26:34 $
 */

package com.sun.enterprise.admin.jmx.remote.internal;

import java.util.Set;
/* BEGIN -- S1WS_MOD */
import java.util.Map;
/* END -- S1WS_MOD */
import java.util.logging.Logger;
import java.io.IOException;
/* BEGIN -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
/* END -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.comm.IConnection;
import com.sun.enterprise.admin.jmx.remote.comm.HttpConnectorAddress;
import com.sun.enterprise.admin.jmx.remote.comm.ConnectionFactory;
import com.sun.enterprise.admin.jmx.remote.comm.MBeanServerMessageConductor;
import com.sun.enterprise.admin.jmx.remote.internal.MBeanServerConnectionExceptionThrower;
/* BEGIN -- S1WS_MOD */
import com.sun.enterprise.admin.jmx.remote.notification.ListenerInfo;
import com.sun.enterprise.admin.jmx.remote.notification.ClientNotificationManager;
/* END -- S1WS_MOD */

import javax.management.*;
import javax.management.remote.message.MBeanServerRequestMessage;
import javax.management.remote.message.MBeanServerResponseMessage;
import com.sun.enterprise.admin.jmx.remote.protocol.Version;

/**Class that represents the proxy to the MBeanServerConnection. The actual MBeanServerConnection is 
 * available remotely. Based on Java Serialization. This is the class
 * whose instance is used by that all the clients that wish to communicate
 * over the HTTP channel. As of now, (PE 8.0 FCS) by default a new HTTP connection is 
 * opened for every remote MBeanServerConnection {@link MBeanServerConnection}
 * method invocation. This can be configured later on if the server happens to
 * provide persistent HTTP (HTTP 1.1) connections.
 * The current state of implementation is that the server does not support the
 * remote notifications. But the client side is not affected by that. In this
 * implementation, if the clients attempt to add notification listeners, they
 * would recieve an {@link UnsupportedOperationException}.
 * @author Kedar Mhaswade
 * @since S1AS8.0
 * @version 1.0
 */

public class RemoteMBeanServerConnection implements MBeanServerConnection {
    
    private IConnection					physicalConnection  = null;
    private MBeanServerMessageConductor	conductor           = null;
    private HttpConnectorAddress        ad                  = null;
/* BEGIN -- S1WS_MOD */
    private ClientNotificationManager   notifMgr            = null;
    private Map env = null;
/* END -- S1WS_MOD */
	
	private static Version cv;
	static {
		try {
			cv = (Version)Class.forName(Version.CLASS_NAME).newInstance(); 
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
    private final static Object EMPTY	= new String();
	/* Comment about EMPTY - This is an "empty" object that will
	   be sent over the wire in case a null is required. An
	   Object created with new Object() is not Serializable
	   but java.lang.String is and hence it is selected. */

    private final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/
	
    public RemoteMBeanServerConnection(IConnection connectionToServer) {
        //physicalConnection	= connectionToServer;
        //conductor		= new MBeanServerMessageConductor(physicalConnection);
    }
    
	/** Creates a new instance of this class and connects it to the 
	 * server resource identified by the argument. 
	 */
/* BEGIN -- S1WS_MOD */
//    public RemoteMBeanServerConnection(HttpConnectorAddress ad) throws Exception {
    public RemoteMBeanServerConnection(HttpConnectorAddress ad, Map env) throws Exception {
/* END -- S1WS_MOD */
        this.ad = ad;
/* BEGIN -- S1WS_MOD */
        this.env = env;
/* END -- S1WS_MOD */
        connect();
/* BEGIN -- S1WS_MOD */
        Boolean enabled = (Boolean) env.get(DefaultConfiguration.NOTIF_ENABLED_PROPERTY_NAME);
        
        if (enabled != null && enabled.booleanValue() == true) {
            notifMgr = new ClientNotificationManager(ad, env);
        }
/* END -- S1WS_MOD */
        logger.finer("Connected to: Address = " + ad.getHost() + ":" + ad.getPort());
    }

/* BEGIN -- S1WS_MOD */
    public ClientNotificationManager getNotificationManager() {
        return notifMgr;
    }
    
    private void checkNotifInit() throws IOException {
        if (notifMgr == null)
            return;
        notifMgr.reinit();
    }
/* END -- S1WS_MOD */

    private void connect() throws java.io.IOException {
        physicalConnection = ConnectionFactory.createConnection(ad);
        conductor = new MBeanServerMessageConductor(physicalConnection);
    }
    
    public void addNotificationListener(ObjectName objectName,
    NotificationListener notificationListener,
    NotificationFilter notificationFilter, Object obj) throws
    InstanceNotFoundException, IOException {
		try {
/* BEGIN -- S1WS_MOD */
            if (notifMgr == null)
                return; //XXX: Ideally throw an Unsupportedexception
            checkNotifInit();
            String id = notifMgr.addNotificationListener(
                                              objectName,
                                              notificationListener,
                                              notificationFilter,
                                              obj);
/* END -- S1WS_MOD */
			connect();
			final MBeanServerResponseMessage response = conductor.invoke(
			MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENERS, 
/* BEGIN -- S1WS_MOD */
			toArray(objectName, notifMgr.getId(), id, null) );
//			toArray(objectName, notificationListener, notificationFilter, obj) );
/* END -- S1WS_MOD */
            MBeanServerResponseActor.voidOrThrow(response);
		}
        catch (Exception e) {
			MBeanServerConnectionExceptionThrower.addNotificationListenerObjectName(e);
        }
    }
    
    public void addNotificationListener(ObjectName objectName,
    ObjectName objectName1, NotificationFilter notificationFilter,
    Object obj) throws InstanceNotFoundException, IOException {
		try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
			connect();
            ListenerInfo info = new ListenerInfo(null, notificationFilter, obj);
			final MBeanServerResponseMessage response = conductor.invoke(
			MBeanServerRequestMessage.ADD_NOTIFICATION_LISTENER_OBJECTNAME,
			toArray(objectName, objectName1, notificationFilter, obj, info.computeId()) );
            MBeanServerResponseActor.voidOrThrow(response);
		}
		catch(Exception e) {
			MBeanServerConnectionExceptionThrower.addNotificationListeners(e);
		}
    }
    
    public ObjectInstance createMBean(String str, ObjectName objectName) throws
    ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
    IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.CREATE_MBEAN, toArray(str, objectName) );
            //the server should return the correct object, otherwise a CCE results
            return ( (ObjectInstance) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch (Exception e) {
            MBeanServerConnectionExceptionThrower.createMBean(e);
            return null;
        }
    }
    
    public ObjectInstance createMBean(String str, ObjectName objectName, ObjectName loaderName)
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
    InstanceNotFoundException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.CREATE_MBEAN_LOADER, toArray(str, objectName, loaderName) );
            //the server should return the correct object, otherwise a CCE results
            return ( (ObjectInstance) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.createMBeanLoader(e);
            return null;
        }
    }
    
    public ObjectInstance createMBean(String str, ObjectName objectName,
    Object[] params, String[] signature) throws ReflectionException,
    InstanceAlreadyExistsException, MBeanRegistrationException, MBeanException,
    NotCompliantMBeanException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.CREATE_MBEAN_PARAMS,
            toArray(str, objectName, params, signature) );
            //the server should return the correct object, otherwise a CCE results
            return ( (ObjectInstance) MBeanServerResponseActor.returnOrThrow(response));
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.createMBeanParams(e);
            return null;
        }
    }
    
    public ObjectInstance createMBean(String str, ObjectName objectName,
    ObjectName loaderName, Object[] params, String[] signature)
    throws ReflectionException, InstanceAlreadyExistsException,
    MBeanRegistrationException, MBeanException, NotCompliantMBeanException,
    InstanceNotFoundException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.CREATE_MBEAN_LOADER_PARAMS,
            toArray(str, objectName, loaderName, params, signature) );
            //the server should return the correct object, otherwise a CCE results
            return ( (ObjectInstance)MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.createMBeanLoaderParams(e);
            return null;
        }
    }
    
    public Object getAttribute(ObjectName objectName, String str) throws
    MBeanException, AttributeNotFoundException, InstanceNotFoundException,
    ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_ATTRIBUTE, toArray(objectName, str) );
            return ( MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getAttribute(e);
            return null;
        }
    }
    
    public AttributeList getAttributes(ObjectName objectName, String[] attributes)
    throws InstanceNotFoundException, ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_ATTRIBUTES,
            toArray(objectName, attributes) );
            //the server should return the correct object, otherwise a CCE results
            return ( (AttributeList) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getAttributes(e);
            return null;
        }
    }
    
    public String getDefaultDomain() throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_DEFAULT_DOMAIN, toArray(EMPTY));
            //the server should return the correct object, otherwise a CCE results
            return ( (String) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getDefaultDomain(e);
            return null;
        }
    }
    
    public String[] getDomains() throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_DOMAINS, toArray(EMPTY));
            //the server should return the correct object, otherwise a CCE results
            return ( (String[]) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getDomains(e);
            return null;
        }
    }
    
    public Integer getMBeanCount() throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_MBEAN_COUNT, toArray(EMPTY));
            //the server should return the correct object, otherwise a CCE results
            return ( (Integer) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getMBeanCount(e);
            return null;
        }
    }
    
    public MBeanInfo getMBeanInfo(ObjectName objectName) throws
    InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_MBEAN_INFO, toArray(objectName));
            //the server should return the correct object, otherwise a CCE results
            return ( (MBeanInfo) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getMBeanInfo(e);
            return null;
        }
    }
    
    public ObjectInstance getObjectInstance(ObjectName objectName) throws
    InstanceNotFoundException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.GET_OBJECT_INSTANCE, toArray(objectName));
            //the server should return the correct object, otherwise a CCE results
            return ( (ObjectInstance) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.getObjectInstance(e);
            return null;
        }
    }
    
    public Object invoke(ObjectName objectName, String methodName,
    Object[] params, String[] signature) throws InstanceNotFoundException,
    MBeanException, ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.INVOKE,
            toArray(objectName, methodName, params, signature));
            return ( MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.invoke(e);
            return null;
        }
    }
    
    public boolean isInstanceOf(ObjectName objectName, String className) throws
    InstanceNotFoundException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.IS_INSTANCE_OF,
            toArray(objectName, className));
            //the server should return the correct object, otherwise a CCE results
            return ( ((Boolean) MBeanServerResponseActor.returnOrThrow(response)).booleanValue() );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.isInstanceOf(e);
            return false;
        }
    }
    
    public boolean isRegistered(ObjectName objectName) throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.IS_REGISTERED,
            toArray(objectName));
            //the server should return the correct object, otherwise a CCE results
            return ( ((Boolean) MBeanServerResponseActor.returnOrThrow(response)).booleanValue() );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.isRegistered(e);
            return false;
        }
    }
    
    public Set queryMBeans(ObjectName objectName, QueryExp queryExp)
    throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.QUERY_MBEANS, toArray(objectName, queryExp));
            //the server should return the correct object, otherwise a CCE results
            return ( (Set) MBeanServerResponseActor.returnOrThrow(response));
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.queryMBeans(e);
            return null;
        }
    }
    
    public Set queryNames(ObjectName objectName, QueryExp queryExp)
    throws IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.QUERY_NAMES, toArray(objectName, queryExp));
            //the server should return the correct object, otherwise a CCE results
            return ( (Set) MBeanServerResponseActor.returnOrThrow(response));
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.queryNames(e);
            return null;
        }
    }
    
    public void removeNotificationListener(ObjectName objectName, NotificationListener notificationListener) 
    throws InstanceNotFoundException, ListenerNotFoundException, java.io.IOException {
/* BEGIN -- S1WS_MOD */
        if (notifMgr == null)
            return; // XXX: Ideally throw an UnsupportedException
/* END -- S1WS_MOD */
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
            String[] ids = notifMgr.removeNotificationListener(
                                                    objectName,
                                                    notificationListener);
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER,
/* BEGIN -- S1WS_MOD */
            toArray(objectName, notifMgr.getId(), ids) );
//            toArray(objectName, notificationListener, null) );
/* END -- S1WS_MOD */
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.removeNotificationListener(e);
        }
    }
    
    public void removeNotificationListener(ObjectName objectName, ObjectName objectName1) 
    throws InstanceNotFoundException, ListenerNotFoundException, java.io.IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
/* BEGIN -- S1WS_MOD */
//            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER,
            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME,
/* END -- S1WS_MOD */
            toArray(objectName, objectName1) );
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
			MBeanServerConnectionExceptionThrower.removeNotificationListenerObjectName(e);
        }
    }
    
    public void removeNotificationListener(ObjectName objectName, ObjectName objectName1, 
    NotificationFilter notificationFilter, Object obj) 
    throws InstanceNotFoundException, ListenerNotFoundException, java.io.IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            ListenerInfo info = new ListenerInfo(null, notificationFilter, obj);
            final MBeanServerResponseMessage response = conductor.invoke(
/* BEGIN -- S1WS_MOD */
/*
            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER,
            toArray(objectName, objectName1, notificationFilter, obj) );
*/
            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_OBJECTNAME_FILTER_HANDBACK,
            toArray(objectName, objectName1, null, null, info.computeId()));
/* END -- S1WS_MOD */
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
			MBeanServerConnectionExceptionThrower.removeNotificationListenerObjectNameFilterHandback(e);
        }
    }
    
    public void removeNotificationListener(ObjectName objectName, 
    NotificationListener notificationListener, NotificationFilter notificationFilter, Object obj) 
    throws InstanceNotFoundException, ListenerNotFoundException, java.io.IOException {
/* BEGIN -- S1WS_MOD */
        if (notifMgr == null)
            return; //XXX: Ideally throw an UnsupportedException
/* END -- S1WS_MOD */
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
            String[] ids = notifMgr.removeNotificationListener(
                                                 objectName,
                                                 notificationListener,
                                                 notificationFilter,
                                                 obj);
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
/* BEGIN -- S1WS_MOD */
//            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER,
            MBeanServerRequestMessage.REMOVE_NOTIFICATION_LISTENER_FILTER_HANDBACK,
            toArray(objectName, notifMgr.getId(), ids.length > 0 ? ids[0] : null, null) );
//            toArray(objectName, notificationListener, notificationFilter, obj) );
/* END -- S1WS_MOD */
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
			MBeanServerConnectionExceptionThrower.removeNotificationListenerFilterHandback(e);
        }
    }

    public void setAttribute(ObjectName objectName, Attribute attribute) throws
    InstanceNotFoundException, AttributeNotFoundException,
    InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.SET_ATTRIBUTE, toArray(objectName, attribute));
            //return ( (ObjectInstance) response.getResult() );
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.setAttribute(e);
        }
    }
    
    public AttributeList setAttributes(ObjectName objectName, AttributeList
    list) throws InstanceNotFoundException, ReflectionException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.SET_ATTRIBUTES, toArray(objectName, list));
            //the server should return the correct object, otherwise a CCE results
            return ( (AttributeList) MBeanServerResponseActor.returnOrThrow(response) );
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.setAttributes(e);
            return null;
        }
    }
    
    public void unregisterMBean(ObjectName objectName) throws
    InstanceNotFoundException, MBeanRegistrationException, IOException {
        try {
/* BEGIN -- S1WS_MOD */
            checkNotifInit();
/* END -- S1WS_MOD */
            connect();
            final MBeanServerResponseMessage response = conductor.invoke(
            MBeanServerRequestMessage.UNREGISTER_MBEAN, toArray(objectName));
            //return ( (ObjectInstance) response.getResult() );
            MBeanServerResponseActor.voidOrThrow(response);
        }
        catch(Exception e) {
            MBeanServerConnectionExceptionThrower.unregisterMBean(e);
        }
    }
    
    private Object[] toArray(Object param1) {
		final Shifter s = new Shifter (new Object[]{param1});
		s.shiftRight(cv);
        return ( s.state() );
    }
    private Object[] toArray(Object param1, Object param2) {
		final Shifter s = new Shifter (new Object[]{param1, param2});
		s.shiftRight(cv);
        return ( s.state() );
    }
    private Object[] toArray(Object param1, Object param2, Object param3) {
		final Shifter s = new Shifter (new Object[]{param1, param2, param3});
		s.shiftRight(cv);
        return ( s.state() );
    }
    private Object[] toArray(Object param1, Object param2, Object param3,
    Object param4) {
		final Shifter s = new Shifter (new Object[]{param1, param2, param3, param4});
		s.shiftRight(cv);
        return ( s.state() );
    }
    private Object[] toArray(Object param1, Object param2, Object param3,
    Object param4, Object param5) {
		final Shifter s = new Shifter (new Object[]{param1, param2, param3, param4, param5});
		s.shiftRight(cv);
        return ( s.state() );
    }
}
