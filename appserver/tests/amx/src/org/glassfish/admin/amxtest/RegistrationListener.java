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

package org.glassfish.admin.amxtest;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.jmx.MBeanRegistrationListener;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 A NotificationListener which tracks registration of MBeans.
 */
public final class RegistrationListener
        extends MBeanRegistrationListener {
    private final MBeanServerConnection mConn;

    private final Set<ObjectName> mRegistered;
    private final Set<ObjectName> mUnregistered;
    private final Set<ObjectName> mCurrentlyRegistered;

    private RegistrationListener(
            final String name,
            final MBeanServerConnection conn)
            throws InstanceNotFoundException, java.io.IOException {
        super("RegistrationListener", conn, null);

        mConn = conn;

        mRegistered = new HashSet<ObjectName>();
        mUnregistered = new HashSet<ObjectName>();
        mCurrentlyRegistered = new HashSet<ObjectName>();

        queryAllAMX();
    }

    public static RegistrationListener
    createInstance(
            final String name,
            final MBeanServerConnection conn)
            throws InstanceNotFoundException, java.io.IOException {
        final RegistrationListener listener = new RegistrationListener(name, conn);

        JMXUtil.listenToMBeanServerDelegate(conn, listener, null, null);

        return listener;
    }


    private void
    queryAllAMX() {
        try {
            final ObjectName pat = Util.newObjectNamePattern(AMX.JMX_DOMAIN, "*");
            final Set<ObjectName> all = JMXUtil.queryNames(mConn, pat, null);

            mCurrentlyRegistered.addAll(all);
        }
        catch (IOException e) {
        }
    }

    public void
    notifsLost() {
        queryAllAMX();
    }

    private boolean
    isAMX(final ObjectName objectName) {
        return objectName.getDomain().equals(AMX.JMX_DOMAIN);
    }

    protected synchronized void
    mbeanRegistered(final ObjectName objectName) {
        if (isAMX(objectName)) {
            mRegistered.add(objectName);
            mCurrentlyRegistered.add(objectName);
        }
    }

    protected synchronized void
    mbeanUnregistered(final ObjectName objectName) {
        if (isAMX(objectName)) {
            mUnregistered.add(objectName);
            mCurrentlyRegistered.remove(objectName);
        }
    }

    public Set<ObjectName>
    getRegistered() {
        return Collections.unmodifiableSet(mRegistered);
    }

    public Set<ObjectName>
    getUnregistered() {
        return Collections.unmodifiableSet(mUnregistered);
    }

    public synchronized Set<ObjectName>
    getCurrentlyRegistered() {
        final Set<ObjectName> all = new HashSet<ObjectName>(mCurrentlyRegistered);

        return all;
    }

}
















