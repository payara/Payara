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

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import java.util.Hashtable;
import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;


/** 
 * The EventListenerProxy recieves notifications from RemoteListenerConnectors
 * registered on the server managed objects and forwards them to the corresponding
 * local listeners
 *
 * @author Hans Hrasna
 */
public class EventListenerProxy extends UnicastRemoteObject implements RemoteEventListener {
    private Hashtable listenerTable = new Hashtable();
    private Hashtable handbackTable = new Hashtable();
    private String proxyAddress;
    private static EventListenerProxy eventProxy = null;
    private static int portnum=1100;
    private String rmiName;
    private static boolean debug = false;

    public synchronized static EventListenerProxy getEventListenerProxy() {
        if (eventProxy == null) {
            EventListenerProxy newProxy = null;
            try {
                newProxy = new EventListenerProxy();
                Naming.rebind(newProxy.proxyAddress, newProxy);
                eventProxy = newProxy;
                if(debug) System.out.println(eventProxy.rmiName + " bound to existing registry at port " + portnum );

            } catch (RemoteException re) {
                if(debug) System.out.println("Naming.rebind("+ (newProxy != null ? newProxy.proxyAddress : "null") +", eventProxy): " + re);
                try {
                    newProxy = new EventListenerProxy();
                    Registry r = LocateRegistry.createRegistry(portnum);
                    r.bind(newProxy.rmiName, newProxy);
                    eventProxy = newProxy;
                    if(debug) System.out.println(newProxy.rmiName + " bound to newly created registry at port " + portnum );
                } catch(Exception e) {
                    eventProxy = null;
                    if(debug) e.printStackTrace();
                }
            } catch (Exception e) {
                if(debug) e.printStackTrace();
            }
        }
        return eventProxy;
    }

    public EventListenerProxy() throws java.rmi.RemoteException {
        String hostName;
        rmiName = "RemoteEventListener" + hashCode() + System.currentTimeMillis();
        try {
            hostName = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (java.net.UnknownHostException e) {
            hostName = "localhost";
            System.out.println(e);
        }
        proxyAddress = "//"+ hostName + ":" + portnum + "/" + rmiName;
    }

	public void handleNotification(Notification n, Object h) throws RemoteException {
        if (debug) System.out.println("EventListenerProxy:handleNotification(" + n + ")");
        NotificationListener listener = (NotificationListener)listenerTable.get((String)h);
        if (listener != null) {
            Object handback = handbackTable.get((String)h);
            listener.handleNotification(n,handback);
        } else {
            System.out.println("EventListenerProxy: listener id " + h + " not found");
        }
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void addListener(String id, NotificationListener l, Object handback) {
        if (debug) System.out.println("EventListenerProxy.addListener()");
        listenerTable.put(id, l);
        handbackTable.put(id, handback);
    }

    public void removeListener(String id) throws ListenerNotFoundException {
        if(listenerTable.remove(id) == null) {
            throw new ListenerNotFoundException();
        } else {
            handbackTable.remove(id);
        }
    }
}

