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

import java.util.logging.Logger;
import javax.management.*;

import com.sun.enterprise.admin.jmx.remote.notification.NotificationWrapper;
import com.sun.enterprise.admin.jmx.remote.DefaultConfiguration;

/**
 * A Proxy for NotificationListener.
 * An object of NotificationListenerProxy is registered to the NotificationBroadcaster
 * for every notification listener that is registered by the client.
 * Whenever the NotificationBroadcaster calls this proxy's handleNotification method,
 * this proxy object will invoke ServerNotificationManager.fireNotification(...)
 */
public class NotificationListenerProxy implements NotificationListener {
    private String id = null;
    private ServerNotificationManager mgr = null;
    private ObjectName objname = null;
    private Notification notification = null;

    private static final Logger logger = Logger.getLogger(
        DefaultConfiguration.JMXCONNECTOR_LOGGER);/*, 
        DefaultConfiguration.LOGGER_RESOURCE_BUNDLE_NAME );*/

    public NotificationListenerProxy(ObjectName objname,
                                     ServerNotificationManager mgr,
                                     String id) {
        this.objname = objname;
        this.mgr = mgr;
        this.id = id;
    }

    /**
     * Returns the client id, which has registered the notification listener
     * represented by this proxy object.
     */
    public String getId() {
        return id;
    }


    public NotificationWrapper getNotificationWrapper() {
        return ( new NotificationWrapper(objname, notification) );
    }

    public Notification getNotification() {
        return notification;
    }

    public void handleNotification( Notification notification,
                                    Object handback) {
        this.notification = notification;
        mgr.fireNotification(this);
    }
}
