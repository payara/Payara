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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.glassfish.admin.amx.impl.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.util.jmx.JMXUtil;

import static org.glassfish.external.amx.AMX.*;

/**
Class used to build ObjectNameBuilder for AMX MBeans.
 */
public final class ObjectNameBuilder {

    private final MBeanServer mMBeanServer;
    private final String mJMXDomain;
    private final ObjectName mParent;

    public ObjectNameBuilder(final MBeanServer mbeanServer, final String jmxDomain) {
        mMBeanServer = mbeanServer;
        mJMXDomain = jmxDomain;
        mParent = null;
    }

    public ObjectNameBuilder(final MBeanServer mbeanServer, final ObjectName parent) {
        mMBeanServer = mbeanServer;
        if (parent == null) {
            throw new IllegalArgumentException("null ObjecName for parent");
        }

        mParent = parent;
        mJMXDomain = parent.getDomain();
    }

    public String getJMXDomain() {
        return (mJMXDomain);
    }

    public static String makeWild(String props) {
        return (Util.concatenateProps(props, JMXUtil.WILD_PROP));
    }

    /**
    Return a list of ancestors, with the child itself last in the list.
     */
    public static List<ObjectName> getAncestors(
            final MBeanServer server,
            final ObjectName start) {
        AMXProxy amx = ProxyFactory.getInstance(server).getProxy(start, AMXProxy.class);
        final List<ObjectName> ancestors = new ArrayList<ObjectName>();

        AMXProxy parent = null;
        while ((parent = amx.parent()) != null) {
            ancestors.add(parent.extra().objectName());
            amx = parent;
        }

        Collections.reverse(ancestors);

        ancestors.add(start);

        return ancestors;
    }

    public ObjectName buildChildObjectName(
            final ObjectName parent,
            final String type,
            final String childName) {
        return buildChildObjectName(mMBeanServer, parent, type, childName);
    }

    public ObjectName buildChildObjectName(
            final String type,
            final String childName) {
        return buildChildObjectName(mMBeanServer, mParent, type, childName);
    }

    public ObjectName buildChildObjectName(final Class<?> intf) {
        return buildChildObjectName(mMBeanServer, mParent, intf);
    }

    public ObjectName buildChildObjectName(final Class<?> intf, final String name) {
        return buildChildObjectName(mMBeanServer, mParent, intf, name);
    }

    /**
    Build an ObjectName for an MBean logically contained within the parent MBean.
    The child may be a true child (a subtype), or simply logically contained
    within the parent.

    @param parent
    @param type  type to be used in the ObjectName
    @param pathType   type to be used in the path, null if to be the same as type
    @return ObjectName
     */
    public static ObjectName buildChildObjectName(
            final MBeanServer server,
            final ObjectName parent,
            final String type,
            final String childName) {
        String props = Util.makeRequiredProps(type, childName);

        final AMXProxy parentProxy = ProxyFactory.getInstance(server).getProxy(parent, AMXProxy.class);
        final String parentPath = parentProxy.path();
        final String parentPathProp = Util.makeProp(PARENT_PATH_KEY, Util.quoteIfNeeded(parentPath));
        props = Util.concatenateProps(parentPathProp, props);

        return JMXUtil.newObjectName(parent.getDomain(), props);
    }

    public static ObjectName buildChildObjectName(
            final MBeanServer server,
            final ObjectName parent,
            final Class<?> intf,
            final String name) {
        final String type = Util.deduceType(intf);

        return buildChildObjectName(server, parent, type, name);
    }

    public static ObjectName buildChildObjectName(
            final MBeanServer server,
            final ObjectName parent,
            final Class<?> intf) {
        return buildChildObjectName(server, parent, intf, null);
    }
}
