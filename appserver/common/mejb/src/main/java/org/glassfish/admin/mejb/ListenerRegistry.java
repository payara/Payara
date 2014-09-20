/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.mejb;

import java.rmi.RemoteException;
import java.util.Hashtable;
import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import javax.management.j2ee.Management;


/** 
 * ListenerRegistry provides an implementation of ListenerRegistration
 * This implementation creates instances of RemoteListenerConnectors which
 * are registered on the MEJB on behalf of the local listener.
 *
 * Note that this cannot possibly work for remote listeners due to MEJBUtility not supporting 
 * anything but the local MBeanServer.
 *
 * @author Hans Hrasna
 */
public final class ListenerRegistry implements javax.management.j2ee.ListenerRegistration {
    //private static final String OMG_ORB_INIT_PORT_PROPERTY = "org.omg.CORBA.ORBInitialPort";
    
    private static final boolean debug = true;
    private static void debug( final String s ) { if ( debug ) { System.out.println(s); } }

    private final Hashtable<NotificationListener, RemoteListenerConnector> listenerConnectors =
            new Hashtable<NotificationListener, RemoteListenerConnector>();
    private final String serverAddress; // the hostname or ip address of the MEJB

    public ListenerRegistry(String ip) {
        serverAddress = ip;
    }

    /**
     * Add a listener to a registered managed object.
     *
     * @param name The name of the managed object on which the listener should be added.
     * @param listener The listener object which will handle the notifications emitted by the registered managed object.
     * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
     * @param handback The context to be sent to the listener when a notification is emitted.
     *
     * @exception InstanceNotFoundException The managed object name provided does not match any of the registered managed objects.
     *
     */
    public void addNotificationListener(
            final ObjectName name,
            final NotificationListener listener,
            final NotificationFilter filter,
            final Object handback)
            throws RemoteException {
        final String proxyAddress = EventListenerProxy.getEventListenerProxy().getProxyAddress();
        try {
            debug("ListenerRegistry:addNotificationListener() to " + name);
            final RemoteListenerConnector connector = new RemoteListenerConnector(proxyAddress);
            getMEJBUtility().addNotificationListener(name, connector, filter, connector.getId());
            EventListenerProxy.getEventListenerProxy().addListener(connector.getId(), listener, handback);
            listenerConnectors.put(listener, connector);
        } catch (final InstanceNotFoundException inf) {
            throw new RemoteException(inf.getMessage(), inf);
        }
    }

    /**
     * Remove a listener from a registered managed object.
     *
     * @param name The name of the managed object on which the listener should be removed.
     * @param listener The listener object which will handle the notifications emitted by the registered managed object.
     * This method will remove all the information related to this listener.
     *
     * @exception InstanceNotFoundException The managed object name provided does not match any of the registered managed objects.
     * @exception ListenerNotFoundException The listener is not registered in the managed object.
     */
    public void removeNotificationListener(
            final ObjectName name,
            final NotificationListener listener)
            throws RemoteException {
        final EventListenerProxy proxy = EventListenerProxy.getEventListenerProxy();
        try {
            debug("ListenerRegistry.removeNotificationListener: " + listener + " for " + name);
            //debug("ListenerRegistry.listenerProxy = " + listenerConnectors.get(((RemoteListenerConnector) listener).getId()));
            final RemoteListenerConnector connector = listenerConnectors.get(listener);
            getMEJBUtility().removeNotificationListener(name, connector);
            proxy.removeListener(connector.getId());
            listenerConnectors.remove(listener);
        } catch (final InstanceNotFoundException inf) {
            throw new RemoteException(inf.getMessage(), inf);
        } catch (final ListenerNotFoundException lnf) {
            throw new RemoteException(lnf.getMessage(), lnf);
        }
    }

    private MEJBUtility getMEJBUtility() {
        return MEJBUtility.getInstance();
    }
}
