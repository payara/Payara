/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.base.NotificationService;
import com.sun.appserv.management.base.NotificationServiceMgr;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.helper.NotificationServiceHelper;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.AttributeChangeNotification;
import javax.management.InstanceNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 */
public final class NotificationServiceTest
        extends AMXTestBase {
    public NotificationServiceTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    public NotificationService
    create() {
        final NotificationServiceMgr proxy = getNotificationServiceMgr();

        return (proxy.createNotificationService("test", 512));
    }

    public void
    testCreate()
            throws Exception {
        final NotificationService proxy = create();

        removeNotificationService(proxy);
    }

    public void
    testGetFromEmpty()
            throws Exception {
        final NotificationService proxy = create();

        assert (proxy.getListeneeSet().size() == 0);
        final Object id = proxy.createBuffer(10, null);
        final Map<String, Object> result = proxy.getBufferNotifications(id, 0);
        final Notification[] notifs = (Notification[]) result.get(proxy.NOTIFICATIONS_KEY);
        assertEquals(0, notifs.length);
    }

    private void
    removeNotificationService(final NotificationService service)
            throws InstanceNotFoundException {
        getNotificationServiceMgr().removeNotificationService(service.getName());
    }


    private static final class MyListener
            implements NotificationListener {
        private final List<Notification> mReceived;
        private final CountDownLatch mLatch;

        public MyListener( final int numNeeded ) {
            mReceived = Collections.synchronizedList(new ArrayList<Notification>());
            mLatch     = new CountDownLatch(numNeeded);
        }

        public void
        handleNotification(
                final Notification notif,
                final Object handback) {
            mReceived.add(notif);
            mLatch.countDown();
        }
        
        public boolean await( final long amt, final TimeUnit units )
            throws InterruptedException
        {
           return mLatch.await( amt, units);
        }

        public int
        getCount() {
            return (mReceived.size());
        }
    }

    private static void
    sleep(int duration) {
        try {
            Thread.sleep(duration);
        }
        catch (InterruptedException e) {
        }
    }

    public void
    testListen()
            throws Exception {
        //trace( "testListen: START" );
        final NotificationService proxy = create();

        final QueryMgr queryMgr = getQueryMgr();
        final ObjectName objectName = Util.getObjectName(queryMgr);

        final Object id = proxy.createBuffer(10, null);
        final NotificationServiceHelper helper = new NotificationServiceHelper(proxy, id);
        proxy.listenTo(objectName, null);
        assert (proxy.getListeneeSet().size() == 1);
        assert (Util.getObjectName((Util.asAMX(proxy.getListeneeSet().iterator().next()))).equals(objectName));

        //trace( "testListen: NEWING" );
        final MyListener myListener = new MyListener(2);    // we expect two changes, see below
        proxy.addNotificationListener(myListener, null, null);
        final String saveLevel = queryMgr.getMBeanLogLevel();
        queryMgr.setMBeanLogLevel("" + Level.FINEST);
        queryMgr.setMBeanLogLevel(saveLevel);

        //trace( "testListen: WAITING" );
        // delivery may be asynchronous; wait until done
        if ( ! myListener.await( 5, TimeUnit.SECONDS ) )
        {
            //trace( "testListen: FAILED TIMEOUT" );
            assert false : "NotificationServiceTest.testListen():  TIMED OUT waiting for Notifications";
        }
        
        //trace( "testListen: NOT FAILED" );
        assert (myListener.getCount() == 2);

        Notification[] notifs = helper.getNotifications();

        assertEquals(2, notifs.length);
        assert (notifs[0].getType().equals(AttributeChangeNotification.ATTRIBUTE_CHANGE));
        assert (notifs[1].getType().equals(AttributeChangeNotification.ATTRIBUTE_CHANGE));
        notifs = helper.getNotifications();
        assert (notifs.length == 0);


        proxy.dontListenTo(objectName);
        assert (proxy.getListeneeSet().size() == 0);

        removeNotificationService(proxy);
        //trace( "testListen: EXIT" );
    }

}


