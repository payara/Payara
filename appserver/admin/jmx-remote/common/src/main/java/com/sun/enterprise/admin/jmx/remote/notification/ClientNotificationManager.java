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

package com.sun.enterprise.admin.jmx.remote.notification;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.net.InetAddress;
import javax.management.*;

import com.sun.enterprise.admin.jmx.remote.notification.SimpleQueue;
import com.sun.enterprise.admin.jmx.remote.notification.NotificationWrapper;
import com.sun.enterprise.admin.jmx.remote.notification.ListenerInfo;
import com.sun.enterprise.admin.jmx.remote.comm.HttpConnectorAddress;
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;


/**
 * A NotificationManager class used on the client-side to  mainly
 * call the NotificationListeners appropriately
 * whenever a notification is received from the server-side.
 * The ClientNotificationManager uses a NotificationReceiver running on a
 * separate thread to listen for notifications.
 * The ClientNotificationManager when constructed will make a connection
 * to the server-side with an unique id identifying this ClientNotificationManager.
 */
public class ClientNotificationManager implements Runnable {
    private Map env = null;
    private HashMap listenerMap = null;
    private String  mgrId          = null;
    private SimpleQueue que = null;
    private NotificationReceiver receiver = null;
    private Thread eventThread = null;
    private boolean exit = false;
    
    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

    public ClientNotificationManager(HttpConnectorAddress ad, Map env)
            throws IOException {
        this.env = env;
        que = new SimpleQueue();
        listenerMap = new HashMap();
        
        String hname="null";            
        try { 
            hname = InetAddress.getLocalHost().getHostName(); 
        } catch (Exception ex) { 
            /*ignore*/ 
            ex.printStackTrace();
        }        
        mgrId = (new java.rmi.server.UID()).toString() + ":" + hname;        

        eventThread = new Thread(this);
        eventThread.start();
        receiver = new NotificationReceiver(ad, this);
    }

    public String getId() {
        return mgrId;
    }

    /**
     * A method to reinitialize the connection to the server-side in case
     * the connection gets dropped.
     * This method is called for every MBeanServerConnection method call.
     */
    public boolean reinit() throws IOException {
        return receiver.reinit();
    }

    /**
     * Registers the notification listener so that they may be called
     * in case a notification is received.
     */
    public String addNotificationListener(  ObjectName           objname,
                                            NotificationListener listener,
                                            NotificationFilter   filter,
                                            Object               handback) {
        ListenerInfo info = new ListenerInfo();

        info.listener   = listener;
        info.filter     = filter;
        info.handback   = handback;
        info.id         = info.computeId();

        ArrayList list = (ArrayList)listenerMap.get(objname);
        if (list == null) {
            list = new ArrayList();
        }
        list.add(info);

        listenerMap.put(objname, list);

        return info.id;
    }

    public String[] removeNotificationListener(
                            ObjectName mbean,
                            NotificationListener listener) {
        String[] strs = removeNotificationListener(mbean, listener, null, null, true);
        return strs;
    }

    public String[] removeNotificationListener(
                            ObjectName mbean,
                            NotificationListener listener,
                            NotificationFilter filter,
                            Object handback) {
        String[] strs = removeNotificationListener( mbean,
                                                    listener,
                                                    filter,
                                                    handback, false);
        return strs;
    }

    /**
     * Unregisters the notification listeners as per the logic defined by
     * MBeanServer.removeNotificationListener
     * 1. <mbean, listener> -- all the registrations for the listener to the specified mbean
     *			       are removed.
     * 2. <mbean, listener, filter, handback>
     *			    -- only that registration, for the listener, which was registered
     *			       with this filter and handback will be removed.
     */
    private String[] removeNotificationListener(
                            ObjectName mbean,
                            NotificationListener listener,
                            NotificationFilter filter,
                            Object handback,
                            boolean listenerOnly) {
        ArrayList idlist = new ArrayList();
        ArrayList list = (ArrayList) listenerMap.get(mbean);
        if (list == null) {
            return (new String[0]);
        }

        ListenerInfo info1 = new ListenerInfo();
        info1.listener = listener;
        info1.filter = filter;
        info1.handback = handback;
        info1.id = info1.computeId();

        Iterator itr = list.iterator();
        // Because updating the list when we are iterating the list throws an exception,
        // unless we return immediately
        ArrayList list1 = (ArrayList) list.clone();
        while (itr.hasNext()) {
            ListenerInfo info = (ListenerInfo) itr.next();
            if (!listenerOnly && info.id.equals(info1.id)) {
                list1.remove(list1.indexOf(info));
                idlist.add(info.id);
            } else if (listenerOnly && info.listener == listener) {
                list1.remove(list1.indexOf(info));
                idlist.add(info.id);
            }
        }

        listenerMap.put(mbean, list1);

        String[] ids = new String[idlist.size()];
        ids = (String[]) idlist.toArray(ids);
        return ids;
    }

    public void raiseEvent(NotificationWrapper wrapr) {
        synchronized (que) {
            que.add(wrapr);
            que.notify();
        }
    }

    private boolean isExiting() {
        return exit;
    }

    public void close() throws Exception {
        exit = true;
        try {
            receiver.exit();
        } finally {
            synchronized (que) {
                que.notify();
            }
            eventThread.join();
        }
    }

    /**
     * This is the event dispatch thread, which calls the notification listeners
     * as and when a notification is received for that listener.
     */
    public void run() {
        while (!isExiting()) {
            synchronized (que) {
                while (que.isEmpty() && !isExiting()) {
                    try {
                        que.wait();
                    } catch (InterruptedException intre) {
                    }
                }
            }
            if (isExiting())
                break;
            while (!que.isEmpty() && !isExiting()) {
                NotificationWrapper wrapr = (NotificationWrapper) que.remove();
                ObjectName source = wrapr.getSource();
                Notification notif = wrapr.getNotification();

                ArrayList listeners = (ArrayList) listenerMap.get(source);
                Iterator itr = listeners.iterator();
                while (itr.hasNext() && !isExiting()) {
                    ListenerInfo info = (ListenerInfo) itr.next();
                    boolean callListener = true;
                    if (info.filter != null)
                        callListener = info.filter.isNotificationEnabled(notif);
                    if (callListener)
                        info.listener.handleNotification(notif, info.handback);
                }
            }
        }
    }
}
