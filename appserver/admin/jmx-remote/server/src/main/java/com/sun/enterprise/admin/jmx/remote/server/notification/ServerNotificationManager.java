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

package com.sun.enterprise.admin.jmx.remote.server.notification;

import java.io.IOException;
import java.io.OutputStream;

import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.*;
import javax.servlet.http.*;

import javax.management.*;

import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;
import com.sun.enterprise.admin.jmx.remote.notification.NotificationWrapper;
import com.sun.enterprise.admin.jmx.remote.notification.ListenerInfo;


/**
 * This is the NotificationManager on the server-side responsible for
 * dipatching notifications to the clients.
 * The ServerNotificationManager maintains a list of NotificationConnection objects
 * for each client.
 */
public class ServerNotificationManager implements Runnable {
    private HashMap connections = null;
    private HashMap listenerMap = null;
    private boolean exiting = false;
    private Thread keepAliveThr = null;
    private MBeanServerConnection mbsc = null;
    private int bufsiz = DefaultConfiguration.NOTIF_MAX_BUFSIZ;

    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

    public ServerNotificationManager(MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
        connections = new HashMap();
        listenerMap = new HashMap();
        keepAliveThr = new Thread(this);
        keepAliveThr.start();
    }

    /**
     * Initializes the notification buffer size for new NotificationConnection objects.
     * The buffer size can be set via the property com.sun.jmx.remote.http.notification.bufsize
     * The property can be set as an init-param for the server-side connector servlet.
     */
    public void setBufSiz(ServletConfig cfg) {
        String bsiz = cfg.getInitParameter(DefaultConfiguration.NOTIF_BUFSIZ_PROPERTY_NAME);
        if (bsiz == null || bsiz.trim().length() == 0)
            bsiz = System.getProperty("com.sun.web.jmx.connector.notification.bufsiz");
        try {
            bufsiz = Integer.parseInt(bsiz);
        } catch (NumberFormatException nume) {
            bufsiz = DefaultConfiguration.NOTIF_MAX_BUFSIZ;
        }
        if (bufsiz <= DefaultConfiguration.NOTIF_MIN_BUFSIZ)
            bufsiz = DefaultConfiguration.NOTIF_MAX_BUFSIZ;
    }

    private void closeConnection(String id, boolean unregisterNotifications) {
        NotificationConnection conn =
                    (NotificationConnection) connections.get(id);
        if (conn != null) {
            if (unregisterNotifications) {
                unregisterNotifications(id);
            }
            connections.remove(id);
            conn.close();
            synchronized (conn) {
                conn.notify();
            }
        }
    }

    private void unregisterNotifications(String id) {
        synchronized (listenerMap) {
            Iterator itr = listenerMap.keySet().iterator();
            while (itr.hasNext()) {
                ObjectName mbean = (ObjectName) itr.next();
                ArrayList list = (ArrayList) listenerMap.get(mbean);
                for (int i=0, len=list.size(); i < len; i++) {
                    ListenerInfo info = (ListenerInfo) list.get(i);
                    if (info.proxy != null &&
                        ((NotificationListenerProxy)info.proxy).getId() == id) {
                        list.remove(i);
                        try {
                            mbsc.removeNotificationListener(
                                mbean, ((NotificationListener)info.proxy));
                        } catch (Exception ex) {
                            // XXX: Log it
                        }
                    }
                }
                listenerMap.put(mbean, list);
            }
        }
    }

    /**
     * getNotifications() is called a client connects to the server or is sending a close message.
     * This method will create a new NotificationConnection object to represent the client.
     * This method will block until the connection needs to be closed or if the
     * connection drops.
     * In case the client is reconnecting (because the connection had dropped),
     * then a NotificationConnection object is created, only if a connection object
     * for the client is not already found.
     */
    public void getNotifications(HttpServletRequest req, HttpServletResponse res) {
        String id = req.getParameter(DefaultConfiguration.NOTIF_ID_PARAM);
        String cmd = req.getParameter(DefaultConfiguration.NOTIF_CMD_PARAM);
        res.setStatus(res.SC_OK);
        res.setHeader("Content-Type", "application/octet-stream");
        res.setHeader("Connection", "Keep-Alive");
        if (cmd != null && cmd.trim().equals(DefaultConfiguration.NOTIF_CMD_CLOSE)) {
            synchronized (connections) {
                closeConnection(id, true);
            }
            return;
        }
        NotificationConnection connection = null;
        try {
            OutputStream out = res.getOutputStream();
            synchronized (connections) {
                connection = (NotificationConnection) connections.get(id);
                if (connection == null) {
                    connection = new NotificationConnection(out, bufsiz);
                    connections.put(id, connection);
                } else {
                    connection.reinit(out);
                }
            }
            out.flush();
        } catch (IOException ioex) {
            // TODO: Log it
            try {
                res.sendError(res.SC_SERVICE_UNAVAILABLE, "Unable to send notifications, since OutputStream could not be opened");
            } catch (IOException ioe) {
                // TODO: Log it
            }
            return;
        }
        synchronized (connection) {
            while (!connection.hasIOExceptionOccurred()) {
                try {
                    connection.wait();
                    break; // somebody notified
                } catch (InterruptedException intre) {
                    // continue
                }
            }
        }
    }

    /**
     * Called whenever the webapp is being shutdown by the servlet container
     */
    public void close() {
        exiting = true;
        while (true) {
            try {
                keepAliveThr.join();
                break;
            } catch (InterruptedException intr) {
            }
        }
        NotificationConnection conn = null;
        synchronized (connections) {
            HashMap conns = (HashMap) connections.clone();
            Iterator itr = conns.keySet().iterator();
            while (itr.hasNext()) {
                String id = (String) itr.next();
                closeConnection(id, false);
            }
        }
    }

    public boolean isExiting() {
        return exiting;
    }

    /**
     * The keepalive thread that sends an empty notification to every client every 10 seconds.
     */
    public void run() {
        while (!isExiting()) {
            try {
                Thread.sleep(DefaultConfiguration.NOTIF_WAIT_INTERVAL);
            } catch (InterruptedException intrEx) {
                // Ignore any interrupts
            }
            if (isExiting())
                break;
            Iterator itr = getConnectionsIterator();
            NotificationConnection conn = null;
            synchronized (connections) {
                while (itr.hasNext() && !isExiting()) {
                    String id = (String) itr.next();
                    conn = (NotificationConnection) connections.get(id);
                    conn.fireWaitNotif();
                }
            }
        }
    }

    private Iterator getConnectionsIterator() {
        Iterator itr = null;
        synchronized (connections) {
            itr = connections.keySet().iterator();
        }
        return itr;
    }

    /**
     * fireNotification() is invoked by NotificationListenerProxy, whenever a notification
     * is sent by any NotificationBroadcaster.
     */
    public void fireNotification(NotificationListenerProxy proxy) {
        String id = proxy.getId();
        if (id == null || id.trim().length() == 0)
            return; // Drop this notification; Nobody is listening for this notification
        NotificationConnection conn = null;
        synchronized (connections) {
            conn = (NotificationConnection) connections.get(id);
        }
        if (conn == null)
            return; // Drop this notification; Nobody is listening for this notification

        conn.fireNotification(proxy.getNotificationWrapper());
    }

    private synchronized void addListenerInfo(ObjectName mbean,
                                              ListenerInfo info) {
        ArrayList list = (ArrayList) listenerMap.get(mbean);
        if (list == null)
            list = new ArrayList();
        list.add(info);
        listenerMap.put(mbean, list);
    }

    /**
     * Registers a notification listener for proper removal, for all registrations
     * done by a call to addNotificationListener(ObjectName, ObjectName, ...).
     * Since, the reference of the filter and handback objects is matched by
     * the NotificationBroadcaster, the ServerNotificationManager would register
     * the actual filter and handback object registered, with the NotificationBroadcaster,
     * and is bound to an id, refering this registration, sent by the client.
     */
    public String addObjNameNotificationListener(ObjectName mbean,
                                                 NotificationFilter filter,
                                                 Object handback,
                                                 String id) {
        ListenerInfo info = new ListenerInfo();
        info.filter = filter;
        info.handback = handback;
        info.id = id;
        addListenerInfo(mbean, info);
        return info.id;
   }

    /**
     * Registers a notification listener for proper removal, for all registrations
     * done by a call to addNotificationListener(ObjectName, NotificationListener, ...).
     * Since, the reference of the filter and handback objects is matched by
     * the NotificationBroadcaster, the ServerNotificationManager would register
     * the actual filter and handback object registered, with the NotificationBroadcaster,
     * and is bound to an id, refering this registration, sent by the client.
     */
    public String addNotificationListener(ObjectName mbean,
                                          String id,
                                          Object proxy) {
        ListenerInfo info = new ListenerInfo();
        info.id = id;
        info.proxy = proxy;
        addListenerInfo(mbean, info);

        return info.id;
    }

    private synchronized Object removeListenerInfo(
                ObjectName mbean, String id, boolean getProxy) {
        ArrayList list = (ArrayList) listenerMap.get(mbean);
        Iterator itr = null;
        if (list == null)
            return null;
        itr = list.iterator();
        while (itr.hasNext()) {
            ListenerInfo info = (ListenerInfo) itr.next();
            if (info.id.equals(id)) {
                list.remove(list.indexOf(info));
                Object retObj = null;
                if (getProxy)
                    retObj = info.proxy;
                else
                    retObj = info;
                return retObj;
            }
        }

        return null;
    }

    /**
     * Removes a registered NotificationListener, when
     * removeNotificationListener(ObjectName, ObjectName, ...) is called.
     */
    public ListenerInfo removeObjNameNotificationListener(ObjectName mbean,
                                                          String id) {
        return (ListenerInfo) removeListenerInfo(mbean, id, false);
    }

    /**
     * Removes a registered NotificationListener, when
     * removeNotificationListener(ObjectName, NotificationListener, ...) is called.
     */
    public Object removeNotificationListener(   ObjectName mbean,
                                                String id) {
        return removeListenerInfo(mbean, id, true);
    }
}

