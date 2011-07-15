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

package org.glassfish.admin.amxtest.support;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.util.stringifier.SmartStringifier;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;


/**
 A NotificationListener which expects to receive a
 CONFIG_CREATED_NOTIFICATION_TYPE and CONFIG_REMOVED_NOTIFICATION_TYPE
 from an MBean with a particular j2eeType and name.
 */
public final class CreateRemoveListener
        implements NotificationListener {
    private final String mNameExpected;
    private final String mJ2EETypeExpected;
    private final Container mSource;

    private Notification mCreateNotif;
    private Notification mRemoveNotif;

    public CreateRemoveListener(
            final Container source,
            final String j2eeTypeExpected,
            final String nameExpected) {
        mSource = source;
        mNameExpected = nameExpected;
        mJ2EETypeExpected = j2eeTypeExpected;

        mSource.addNotificationListener(this, null, null);
    }

    public void
    handleNotification(
            final Notification notifIn,
            final Object handback) {
        final String type = notifIn.getType();

        //final Map<String,Serializable>	m	= getAMXNotificationData * notifIn );
        final ObjectName objectName =
                Util.getAMXNotificationValue(notifIn, AMXConfig.CONFIG_OBJECT_NAME_KEY, ObjectName.class);

        //trace( "CreateRemoveListener:\n" + SmartStringifier.toString( notifIn ) + ":\n" + objectName );

        if (Util.getJ2EEType(objectName).equals(mJ2EETypeExpected) &&
                Util.getName(objectName).equals(mNameExpected)) {
            if (type.equals(AMXConfig.CONFIG_CREATED_NOTIFICATION_TYPE)) {
                mCreateNotif = notifIn;
            } else if (type.equals(AMXConfig.CONFIG_REMOVED_NOTIFICATION_TYPE)) {
                mRemoveNotif = notifIn;
            }
        }
    }

    protected void
    trace(Object o) {
        System.out.println(SmartStringifier.toString(o));
    }

    public static void
    mySleep(final long millis) {
        try {
            Thread.sleep(millis);
        }
        catch (InterruptedException e) {
        }
    }


    public void
    waitCreate() {
        long millis = 10;
        while (mCreateNotif == null) {
            mySleep(millis);
            trace("waiting " + millis + "ms for CONFIG_CREATED_NOTIFICATION_TYPE for " + mNameExpected);
            millis *= 2;
        }
    }

    public void
    waitRemove() {
        long millis = 10;
        while (mRemoveNotif == null) {
            mySleep(millis);
            trace("waiting " + millis + "ms for CONFIG_REMOVED_NOTIFICATION_TYPE for " + mNameExpected);
            millis *= 2;
        }
    }


    public void
    waitNotifs() {
        waitCreate();
        waitRemove();

        try {
            mSource.removeNotificationListener((NotificationListener) this, null, null);
        }
        catch (ListenerNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}

