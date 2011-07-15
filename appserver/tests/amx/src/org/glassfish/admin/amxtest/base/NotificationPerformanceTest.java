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

import com.sun.appserv.management.base.Util;
import org.glassfish.admin.amx.mbean.TestDummy;
import org.glassfish.admin.amx.mbean.TestDummyMBean;
import org.glassfish.admin.amxtest.JMXTestBase;

import javax.management.JMException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;

import org.glassfish.admin.amxtest.JMXTestBase;

/**
 */
public final class NotificationPerformanceTest
        extends JMXTestBase {
    // built-into server already
    private static final String IMPL_CLASSNAME = TestDummy.class.getName();

    public NotificationPerformanceTest() {
    }

    private ObjectName
    createTestDummy(final String name)
            throws JMException, IOException {
        ObjectName objectName =
                Util.newObjectName("NotificationPerformanceTest:name=" + name);

        final MBeanServerConnection conn = getMBeanServerConnection();

        if (!conn.isRegistered(objectName)) {
            objectName =
                    conn.createMBean(IMPL_CLASSNAME, objectName).getObjectName();
        }

        return objectName;
    }

    public void
    testNotificationPerformance()
            throws JMException, IOException {
        final ObjectName objectName = createTestDummy("testNotificationPerformance");

        final TestDummyMBean test = newProxy(objectName, TestDummyMBean.class);

        final int ITER = 10;
        final int COUNT = 1024 * 1024;

        for (int iter = 0; iter < ITER; ++iter) {
            final long elapsed =
                    test.emitNotifications("NotificationPerformanceTest.test", COUNT);

            final float rate = (elapsed == 0) ? (float) 0.0 : (1000 * ((float) COUNT / (float) elapsed));
            final String rateString = (elapsed == 0) ? "N/A" : "" + (int) rate;

            System.out.println("Millis to emit " + COUNT + " Notifications: " + elapsed +
                    " = " + rateString + " notifications/sec");
        }
    }
}


























