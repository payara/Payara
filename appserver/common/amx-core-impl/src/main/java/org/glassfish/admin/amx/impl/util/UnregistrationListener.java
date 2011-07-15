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
package org.glassfish.admin.amx.impl.util;

import javax.management.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.glassfish.admin.amx.util.jmx.JMXUtil;

/**
Blocks until an MBean is UNregistered using a CountdownLatch (highly efficient).

 */
public final class UnregistrationListener implements NotificationListener {

    final MBeanServerConnection mMBeanServer;
    final ObjectName mObjectName;
    final CountDownLatch mLatch;

    public UnregistrationListener(final MBeanServerConnection conn, final ObjectName objectName) {
        mMBeanServer = conn;
        mObjectName = objectName;
        mLatch = new CountDownLatch(1);
        // DO NOT listen here; thread-safety problem
    }

    public void handleNotification(final Notification notifIn, final Object handback) {
        if (notifIn instanceof MBeanServerNotification) {
            final MBeanServerNotification notif = (MBeanServerNotification) notifIn;

            if (notif.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION) &&
                    mObjectName.equals(notif.getMBeanName())) {
                mLatch.countDown();
            }
        }
    }

    private static void cdebug(final String s) {
        System.out.println(s);
    }

    /**
    Wait (block) until the MBean is unregistered.
    @return true if unregistered, false if an error
     */
    public boolean waitForUnregister(final long timeoutMillis) {
        boolean unregisteredOK = false;

        try {
            // could have already been unregistered
            if (mMBeanServer.isRegistered(mObjectName)) {
                try {
                    // CAUTION: we must register first to avoid a race condition
                    JMXUtil.listenToMBeanServerDelegate(mMBeanServer, this, null, mObjectName);

                    if (mMBeanServer.isRegistered(mObjectName)) {
                        // block
                        final boolean unlatched = mLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
                        unregisteredOK = unlatched; // otherwise it timed-out
                    } else {
                        unregisteredOK = true;
                    }
                } catch (final java.lang.InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (final InstanceNotFoundException e) {
                    // fine, we're expecting it to be unregistered anyway
                } finally {
                    mMBeanServer.removeNotificationListener(JMXUtil.getMBeanServerDelegateObjectName(), this);
                }
            } else {
                unregisteredOK = true;
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
        return unregisteredOK;
    }
}































