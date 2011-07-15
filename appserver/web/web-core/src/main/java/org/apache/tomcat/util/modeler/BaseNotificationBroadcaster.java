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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.modeler;


import javax.management.*;
import java.util.ArrayList;
import java.util.Iterator;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;


/**
 * <p>Implementation of <code>NotificationBroadcaster</code> for attribute
 * change notifications.  This class is used by <code>BaseModelMBean</code> to
 * handle notifications of attribute change events to interested listeners.
 *</p>
 *
 * @author Craig R. McClanahan
 * @author Costin Manolache
 */

public class BaseNotificationBroadcaster implements NotificationBroadcaster {


    // ----------------------------------------------------------- Constructors


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of registered <code>BaseNotificationBroadcasterEntry</code>
     * entries.
     */
    protected ArrayList<BaseNotificationBroadcasterEntry> entries =
        new ArrayList<BaseNotificationBroadcasterEntry>();


    // --------------------------------------------------------- Public Methods


    /**
     * Add a notification event listener to this MBean.
     *
     * @param listener Listener that will receive event notifications
     * @param filter Filter object used to filter event notifications
     *  actually delivered, or <code>null</code> for no filtering
     * @param handback Handback object to be sent along with event
     *  notifications
     *
     * @exception IllegalArgumentException if the listener parameter is null
     */
    @Override
    public void addNotificationListener(NotificationListener listener,
                                        NotificationFilter filter,
                                        Object handback)
        throws IllegalArgumentException {

        synchronized (entries) {

            // Optimization to coalesce attribute name filters
            if (filter instanceof BaseAttributeFilter) {
                BaseAttributeFilter newFilter = (BaseAttributeFilter) filter;
                Iterator<BaseNotificationBroadcasterEntry> items =
                    entries.iterator();
                while (items.hasNext()) {
                    BaseNotificationBroadcasterEntry item = items.next();
                    if ((item.listener == listener) &&
                        (item.filter != null) &&
                        (item.filter instanceof BaseAttributeFilter) &&
                        (item.handback == handback)) {
                        BaseAttributeFilter oldFilter =
                            (BaseAttributeFilter) item.filter;
                        String newNames[] = newFilter.getNames();
                        String oldNames[] = oldFilter.getNames();
                        if (newNames.length == 0) {
                            oldFilter.clear();
                        } else {
                            if (oldNames.length != 0) {
                                for (int i = 0; i < newNames.length; i++)
                                    oldFilter.addAttribute(newNames[i]);
                            }
                        }
                        return;
                    }
                }
            }

            // General purpose addition of a new entry
            entries.add(new BaseNotificationBroadcasterEntry
                        (listener, filter, handback));
        }

    }


    /**
     * Return an <code>MBeanNotificationInfo</code> object describing the
     * notifications sent by this MBean.
     */
    @Override
    public MBeanNotificationInfo[] getNotificationInfo() {

        return (new MBeanNotificationInfo[0]);

    }


    /**
     * Remove a notification event listener from this MBean.
     *
     * @param listener The listener to be removed (any and all registrations
     *  for this listener will be eliminated)
     *
     * @exception ListenerNotFoundException if this listener is not
     *  registered in the MBean
     */
    @Override
    public void removeNotificationListener(NotificationListener listener)
        throws ListenerNotFoundException {

        synchronized (entries) {
            Iterator<BaseNotificationBroadcasterEntry> items =
                entries.iterator();
            while (items.hasNext()) {
                BaseNotificationBroadcasterEntry item = items.next();
                if (item.listener == listener)
                    items.remove();
            }
        }

    }


    /**
     * Remove a notification event listener from this MBean.
     *
     * @param listener The listener to be removed (any and all registrations
     *  for this listener will be eliminated)
     * @param handback Handback object to be sent along with event
     *  notifications
     *
     * @exception ListenerNotFoundException if this listener is not
     *  registered in the MBean
     */
    public void removeNotificationListener(NotificationListener listener,
                                           Object handback)
        throws ListenerNotFoundException {

        removeNotificationListener(listener);

    }


    /**
     * Remove a notification event listener from this MBean.
     *
     * @param listener The listener to be removed (any and all registrations
     *  for this listener will be eliminated)
     * @param filter Filter object used to filter event notifications
     *  actually delivered, or <code>null</code> for no filtering
     * @param handback Handback object to be sent along with event
     *  notifications
     *
     * @exception ListenerNotFoundException if this listener is not
     *  registered in the MBean
     */
    public void removeNotificationListener(NotificationListener listener,
                                           NotificationFilter filter,
                                           Object handback)
        throws ListenerNotFoundException {

        removeNotificationListener(listener);

    }


    /**
     * Send the specified notification to all interested listeners.
     *
     * @param notification The notification to be sent
     */
    public void sendNotification(Notification notification) {

        synchronized (entries) {
            Iterator<BaseNotificationBroadcasterEntry> items =
                entries.iterator();
            while (items.hasNext()) {
                BaseNotificationBroadcasterEntry item = items.next();
                if ((item.filter != null) &&
                    (!item.filter.isNotificationEnabled(notification)))
                    continue;
                item.listener.handleNotification(notification, item.handback);
            }
        }

    }

}


/**
 * Utility class representing a particular registered listener entry.
 */

class BaseNotificationBroadcasterEntry {

    public BaseNotificationBroadcasterEntry(NotificationListener listener,
                                            NotificationFilter filter,
                                            Object handback) {
        this.listener = listener;
        this.filter = filter;
        this.handback = handback;
    }

    public NotificationFilter filter = null;

    public Object handback = null;

    public NotificationListener listener = null;

}
