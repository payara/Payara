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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/base/ContainerTest.java,v 1.7 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.7 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.MapUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import com.sun.appserv.management.util.misc.TypeCast;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 */
public final class ContainerTest
        extends AMXTestBase {
    public ContainerTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    public void
    checkContainerContainsChild(final ObjectName containeeObjectName)
            throws Exception {
        final MBeanServerConnection conn = getMBeanServerConnection();

        assert (containeeObjectName != null);
        assert (conn.isRegistered(containeeObjectName));
        final AMX containedProxy = getProxyFactory().getProxy(containeeObjectName, AMX.class);
        if (containedProxy instanceof DomainRoot) {
            // DomainRoot has no Container
            return;
        }

        final ObjectName containerObjectName = (ObjectName)
                conn.getAttribute(containeeObjectName, "ContainerObjectName");
        if (!conn.isRegistered(containerObjectName)) {
            warning("Container " + StringUtil.quote(containerObjectName) +
                    " for " + StringUtil.quote(containeeObjectName) +
                    " is not registered.");
            return;
        }

        final AMX parentProxy = containedProxy.getContainer();

        if (parentProxy instanceof Container) {
            if (!(parentProxy instanceof Container)) {
                trace("WARNING: proxy is instance of Container, but not Container: " +
                        Util.getExtra(parentProxy).getObjectName());
            } else {
                final Container container = (Container) parentProxy;

                if (container != null) {
                    final Set<AMX> containees = container.getContaineeSet();
                    final Set<ObjectName> containeeObjectNames = Util.toObjectNames(containees);

                    if (!containeeObjectNames.contains(Util.getExtra(containedProxy).getObjectName())) {
                        trace("ERROR: Container " + Util.getExtra(parentProxy).getObjectName() +
                                " does not contain its child: " + containeeObjectName);
                        assertTrue(false);
                    }
                }
            }
        }
    }


    public void
    testContainersContainChildren()
            throws Exception {
        testAll("checkContainerContainsChild");
    }


    public void
    checkIsContainer(final ObjectName objectName)
            throws Exception {
        final AMX proxy = getProxyFactory().getProxy(objectName, AMX.class);

        try {
            final Set<String> containedJ2EETypes = TypeCast.asSet(
                    Util.getExtra(proxy).getAttribute(Container.ATTR_CONTAINEE_J2EE_TYPES));

            if (containedJ2EETypes != null && containedJ2EETypes.size() != 0) {
                assert (proxy instanceof Container) :
                        "proxy has ContaineeJ2EETypes but is not a Container: " + objectName;
            }
        }
        catch (AttributeNotFoundException e) {
        }

    }

    public void
    testIsContainer()
            throws Exception {
        testAll("checkIsContainer");
    }

    private void
    checkMapAgreesWithSet(final Container container)
            throws Exception {
        final Set<String> containedJ2EETypes = container.getContaineeJ2EETypes();

        for (final String j2eeType : containedJ2EETypes) {
            final Map<String, AMX> containeeMap =
                    container.getContaineeMap(j2eeType);

            final Set<AMX> containeeSet =
                    container.getContaineeSet(j2eeType);

            assert (containeeMap.keySet().size() == containeeSet.size()) :
                    "containeeMap has " + containeeMap.keySet().size() +
                            " = " + toString(containeeMap) +
                            " but containeeSet has " + containeeSet.size() + " = " + toString(containeeSet);

            final Set<String> namesSet = Util.getNames(containeeSet);
            assert (containeeMap.keySet().equals(namesSet));
        }
    }

    public void
    checkMapAgreesWithSet(final AMX container)
            throws Exception {
        checkMapAgreesWithSet((Container) container);
    }

    private void
    checkContaineeMap(final Container container)
            throws Exception {
        final Set<String> containedJ2EETypes = container.getContaineeJ2EETypes();
        assert (containedJ2EETypes != null);
        assert (!containedJ2EETypes.contains(null));

        assert (container.getMultiContaineeMap((Set<String>) null) != null);
        assert (container.getMultiContaineeMap(containedJ2EETypes) != null);

        for (final String j2eeType : containedJ2EETypes) {
            final Map<String, AMX> containeeMap =
                    container.getContaineeMap(j2eeType);
            assert (containeeMap != null) :
                    "getContaineeObjectNameMap failed for " + j2eeType;
            final Set<String> nullValueKeys = MapUtil.getNullValueKeys(containeeMap);
            assert (nullValueKeys.size() == 0) :
                    "getContaineeObjectNameMap contains nulls for " + toString(nullValueKeys);
        }
    }

    public void
    checkContaineeMap(final AMX container)
            throws Exception {
        checkContaineeMap((Container) container);
    }


    private void
    checkContaineeSet(final Container container)
            throws Exception {
        final Set<String> containedJ2EETypes = container.getContaineeJ2EETypes();
        assert (containedJ2EETypes != null);
        assert (!containedJ2EETypes.contains(null));
        assert (container.getContaineeSet() != null);

        for (final String j2eeType : containedJ2EETypes) {
            final Set<AMX> containeeSet =
                    container.getContaineeSet(j2eeType);
            assert (containeeSet != null) :
                    "getContaineeSet for " + j2eeType;
            assert (!containeeSet.contains(null)) :
                    "getContaineeSet contains null for " + j2eeType;

            final Set<AMX> fromSet =
                    container.getContaineeSet(GSetUtil.newStringSet(j2eeType));
            assert (fromSet.equals(containeeSet));

            if (containeeSet.size() == 1) {
                assert (container.getContainee(j2eeType) != null);
            }
        }
    }

    public void
    checkContaineeSet(final AMX container)
            throws Exception {
        checkContaineeSet((Container) container);
    }


    private void
    checkGetByName(final Container container)
            throws Exception {
        final Set<String> containedJ2EETypes = container.getContaineeJ2EETypes();

        for (final String j2eeType : containedJ2EETypes) {
            final Map<String, AMX> containeeMap =
                    container.getContaineeMap(j2eeType);

            for (final String name : containeeMap.keySet()) {
                final AMX containee = container.getContainee(j2eeType, name);

                assert (containee != null) :
                        "can't get containee of type " + j2eeType + ", name = " + name +
                                " in " + Util.getObjectName(container);

                assert (containee.getJ2EEType().equals(j2eeType));
                assert (containee.getName().equals(name));

                final Set<AMX> byName =
                        container.getByNameContaineeSet(GSetUtil.newStringSet(j2eeType), name);
                assert (byName.size() == 1);
                assert (byName.iterator().next() == containee);
            }
        }
    }

    public void
    checkGetByName(final AMX container)
            throws Exception {
        checkGetByName((Container) container);
    }


    public void
    testMapAgreesWithSet()
            throws Exception {
        testAllProxies(getAllContainers(), "checkMapAgreesWithSet");
    }

    public void
    testContaineeMap()
            throws Exception {
        testAllProxies(getAllContainers(), "checkContaineeMap");
    }

    public void
    testContaineeSet()
            throws Exception {
        testAllProxies(getAllContainers(), "checkContaineeSet");
    }


    private <T extends AMX> boolean
    setsEqual(
            final Set<T> s1,
            final Set<T> s2) {
        final Set<ObjectName> t1 = Util.toObjectNames(s1);
        final Set<ObjectName> t2 = Util.toObjectNames(s2);

        return t1.equals(t2);
    }

    private <T extends AMX> boolean
    mapsEqual(
            final Map<String, T> m1,
            final Map<String, T> m2) {
        final Map<String, ObjectName> t1 = Util.toObjectNames(m1);
        final Map<String, ObjectName> t2 = Util.toObjectNames(m2);

        return t1.equals(t2);
    }

    private <T extends AMX> boolean
    mapsOfMapsEqual(
            final Map<String, Map<String, T>> m1,
            final Map<String, Map<String, T>> m2) {
        boolean equals = false;

        if (m1.keySet().equals(m2.keySet())) {
            equals = true;
            for (final String key : m1.keySet()) {
                final Map<String, T> x1 = m1.get(key);
                final Map<String, T> x2 = m2.get(key);
                if (!mapsEqual(x1, x2)) {
                    trace("x1: " + MapUtil.toString(x1));
                    trace("x2: " + MapUtil.toString(x2));
                    equals = false;
                    break;
                }
            }
        }

        return equals;
    }


    private void
    testContaineesOfType(
            final Container c,
            final String j2eeType) {
        final Set<String> j2eeTypes = c.getContaineeJ2EETypes();

        final Map<String, Map<String, AMX>> all = c.getMultiContaineeMap(j2eeTypes);
        final Map<String, Map<String, AMX>> allFromNull = c.getMultiContaineeMap(null);
        assert (mapsOfMapsEqual(all, allFromNull));

        final Map<String, AMX> byType = c.getContaineeMap(j2eeType);
        assert mapsEqual(byType, all.get(j2eeType));

        if (byType.keySet().size() == 1) {
            final AMX cc = c.getContainee(j2eeType);
            assert cc == byType.values().iterator().next();
        }

        final Set<AMX> s = c.getContaineeSet(j2eeType);
        final Set<AMX> sByType = new HashSet<AMX>(byType.values());
        assert setsEqual(sByType, s);

        final Set<String> nullSet = null;

        assert (setsEqual(c.getContaineeSet(nullSet), c.getContaineeSet(j2eeTypes)));
        assert (setsEqual(c.getContaineeSet(), c.getContaineeSet(nullSet)));

        for (final AMX amx : byType.values()) {
            final String itsName = amx.getName();
            final String itsType = amx.getJ2EEType();

            final Set<String> types = GSetUtil.newStringSet(itsType);
            final Set<AMX> x = c.getByNameContaineeSet(types, itsName);

            assert (x.size() == 1);
            assert (amx == x.iterator().next());
            assert (c.getContainee(itsType, itsName) == amx);
        }
    }

    private void
    testContainee(final Container c) {
        final Map<String, Map<String, AMX>> all = c.getMultiContaineeMap(null);

        for (final String j2eeType : all.keySet()) {
            testContaineesOfType(c, j2eeType);
        }
    }

    public void
    testContainees() {
        final Set<Container> containers = getAllContainers();

        for (final Container c : containers) {
            testContainee(c);
        }

    }


    public void
    testGetByName()
            throws Exception {
        testAllProxies(getAllContainers(), "checkGetByName");
    }


    private Set<Container>
    getAllContainers() {
        return getTestUtil().getAllAMX(Container.class);
    }
}





















