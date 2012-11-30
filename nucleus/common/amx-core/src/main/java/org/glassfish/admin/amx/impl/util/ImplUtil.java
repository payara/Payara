/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Set;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;

public final class ImplUtil {

    private static void debug(final String s) {
        System.out.println(s);
    }

    /**
    Unload this AMX MBean and all its children.
    MBean should be unloaded at the leafs first, working back to DomainRoot so as to
    not violate the rule that a Container must always be present for a Containee.
     */
    public static void unregisterAMXMBeans(final AMXProxy top) {
        if (top == null) {
            throw new IllegalArgumentException();
        }

        //debug( "ImplUtil.unregisterOneMBean: unregistering hierarchy under: " + top.objectName() );

        final MBeanServer mbeanServer = (MBeanServer) top.extra().mbeanServerConnection();

        final Set<AMXProxy> children = top.extra().childrenSet();
        if (children != null) {
            // unregister all Containees first
            for (final AMXProxy amx : children) {
                unregisterAMXMBeans(amx);
            }
        }

        unregisterOneMBean(mbeanServer, top.objectName());
    }

    /** see javadoc for unregisterAMXMBeans(AMX) */
    public static void unregisterAMXMBeans(final MBeanServer mbs, final ObjectName objectName) {
        unregisterAMXMBeans(ProxyFactory.getInstance(mbs).getProxy(objectName, AMXProxy.class));
    }

    /**
    Unregister a single MBean, returning true if it was unregistered, false otherwise.
     */
    public static boolean unregisterOneMBean(final MBeanServer mbeanServer, final ObjectName objectName) {
        boolean success = false;
        //getLogger().fine( "UNREGISTER MBEAN: " + objectName );
        //debug( "ImplUtil.unregisterOneMBean: unregistering: " + objectName );
        try {
            mbeanServer.unregisterMBean(objectName);
        } catch (final Exception e) {
            // ignore
            success = false;
        }
        return success;
    }
}































