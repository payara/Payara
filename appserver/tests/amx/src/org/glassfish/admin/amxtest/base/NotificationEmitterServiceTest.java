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

/*
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/base/NotificationEmitterServiceTest.java,v 1.5 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.5 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.base.NotificationEmitterService;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.util.jmx.NotificationBuilder;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.Notification;
import javax.management.NotificationListener;


/**
 */
public final class NotificationEmitterServiceTest
        extends AMXTestBase {
    public NotificationEmitterServiceTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    public NotificationEmitterService
    getNotificationEmitterService() {
        return getDomainRoot().getDomainNotificationEmitterService();
    }

    public void
    testGet() {
        assert getNotificationEmitterService() != null;
    }


    private final static class testEmitListener
            implements NotificationListener {
        static final String TEST_TYPE = "unittests.testEmitListener";
        private Notification mNotification;
        private int mNumHeard;

        public testEmitListener() {
            mNumHeard = 0;
            mNotification = null;
        }

        public void
        handleNotification(
                final Notification notif,
                final Object handback) {
            mNotification = notif;
            ++mNumHeard;
        }

        public Notification getLast() { return mNotification; }

        public int getNumHeard() { return mNumHeard; }

        public void clear() {
            mNumHeard = 0;
            mNotification = null;
        }
    }

    private static final String TEST_SOURCE = "NotificationEmitterServiceTest";
    private static final String TEST_MESSAGE = "Message";
    private static final String TEST_KEY = "TestKey";
    private static final String TEST_VALUE = "test value";

    public void
    testEmit() {
        final NotificationEmitterService nes = getNotificationEmitterService();

        final NotificationBuilder builder =
                new NotificationBuilder(testEmitListener.TEST_TYPE, TEST_SOURCE);

        final testEmitListener listener = new testEmitListener();
        nes.addNotificationListener(listener, null, null);
        final Notification notif = builder.buildNew(TEST_MESSAGE);
        builder.putMapData(notif, TEST_KEY, TEST_VALUE);

        // call emitNotification() and verify it was emitted
        nes.emitNotification(notif);
        while (listener.getLast() == null) {
            // wait...
            mySleep(20);
        }
        final Notification retrieved = listener.getLast();
        assert (retrieved.getType().equals(notif.getType()));
        assert (Util.getAMXNotificationValue(retrieved, TEST_KEY, String.class).equals(TEST_VALUE));
        assert (retrieved.getSource().equals(TEST_SOURCE));
        assert (retrieved.getMessage().equals(TEST_MESSAGE));

        // now emit many Notifications.
        listener.clear();
        long start = now();
        final int ITER = 200;
        for (int i = 0; i < ITER; ++i) {
            final Notification temp = builder.buildNew(TEST_MESSAGE);
            builder.putMapData(notif, TEST_KEY, TEST_VALUE);
            nes.emitNotification(temp);
        }
        printElapsedIter("Emitted Notifications", start, ITER);
        start = now();
        while (listener.getNumHeard() < ITER) {
            mySleep(10);
        }
        printElapsedIter("After sending, received emitted Notifications", start, ITER);
    }
}










