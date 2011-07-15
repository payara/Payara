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

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.client.ProxyFactory;
import com.sun.appserv.management.util.jmx.ObjectNameComparator;
import com.sun.appserv.management.util.misc.ClassUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import com.sun.appserv.management.util.misc.TypeCast;
import org.glassfish.admin.amx.util.AMXDebugStuff;

import org.glassfish.admin.amxtest.support.AMXComparator;

import javax.management.ObjectName;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 Observes various things as tests are run.
 */
public final class TestUtil {
    private final DomainRoot mDomainRoot;
    private final String NEWLINE;

    public TestUtil(final DomainRoot domainRoot) {
        mDomainRoot = domainRoot;
        NEWLINE = System.getProperty("line.separator");
    }

    private void
    trace(final Object o) {
        System.out.println("" + o);
    }

    public AMXDebugStuff
    asAMXDebugStuff(final AMX amx) {
        final String[] attrNames = Util.getExtra(amx).getAttributeNames();

        AMXDebugStuff result = null;
        if (GSetUtil.newUnmodifiableStringSet(attrNames).contains("AMXDebug")) {
            final ProxyFactory factory = Util.getExtra(amx).getProxyFactory();

            try {
                final Class amxClass =
                        ClassUtil.getClassFromName(Util.getExtra(amx).getInterfaceName());
                final Class[] interfaces = new Class[]{amxClass, AMXDebugStuff.class};

                final ObjectName objectName = Util.getObjectName(amx);

                return (AMXDebugStuff)
                        factory.newProxyInstance(objectName, interfaces);
            }
            catch (Exception e) {
                trace(ExceptionUtil.toString(e));
                throw new RuntimeException(e);
            }
        }

        return result;
    }


    /**
     @return Set of j2eeTypes found in Set<AMX>
     */
    public Set<String>
    getJ2EETypes(final Set<AMX> amxs) {
        final Set<String> registered = new HashSet<String>();

        for (final AMX amx : amxs) {
            registered.add(amx.getJ2EEType());
        }

        return registered;
    }

    /**
     @return Set of j2eeTypes for which no MBeans exist
     */
    public Set<String>
    findRegisteredJ2EETypes() {
        return getJ2EETypes(mDomainRoot.getQueryMgr().queryAllSet());
    }

    public String
    setToSortedString(
            final Set<String> s,
            final String delim) {
        final String[] a = GSetUtil.toStringArray(s);
        Arrays.sort(a);

        return StringUtil.toString(NEWLINE, (Object[]) a);
    }


    public static SortedSet<ObjectName>
    newSortedSet(final ObjectName[] objectNames) {
        final SortedSet<ObjectName> s = new TreeSet<ObjectName>(ObjectNameComparator.INSTANCE);

        for (final ObjectName objectName : objectNames) {
            s.add(objectName);
        }

        return s;
    }

    public static SortedSet<ObjectName>
    newSortedSet(final Collection<ObjectName> c) {
        final ObjectName[] objectNames = new ObjectName[c.size()];
        c.toArray(objectNames);

        return newSortedSet(objectNames);
    }

    /**
     As an optimization to speed up testing, we always get the Set of AMX
     ObjectNames using Observer, which maintains such a list.
     */
    public SortedSet<ObjectName>
    getAllObjectNames() {
        final Set<ObjectName> s =
                Observer.getInstance().getCurrentlyRegisteredAMX();

        return newSortedSet(s);
    }


    /**
     @return all AMX, sorted by ObjectName
     */
    public SortedSet<AMX>
    getAllAMX() {
        final SortedSet<ObjectName> all = getAllObjectNames();

        final SortedSet<AMX> allAMX = new TreeSet<AMX>(new AMXComparator<AMX>());
        final ProxyFactory proxyFactory = Util.getExtra(mDomainRoot).getProxyFactory();
        for (final ObjectName objectName : all) {
            try {
                final AMX amx = proxyFactory.getProxy(objectName, AMX.class);

                allAMX.add(amx);
            }
            catch (Exception e) {
                trace(ExceptionUtil.toString(e));
            }
        }

        return allAMX;
    }

    public <T> SortedSet<T>
    getAllAMX(final Class<T> theInterface) {
        final SortedSet<AMX> all = getAllAMX();
        final TreeSet<AMX> allOfInterface = new TreeSet<AMX>(new AMXComparator<AMX>());

        for (final AMX amx : all) {
            if (theInterface.isAssignableFrom(amx.getClass())) {
                allOfInterface.add(amx);
            }
        }

        return TypeCast.asSortedSet(allOfInterface);

    }

    public ObjectName[]
    getAllAMXArray() {
        final SortedSet<ObjectName> s = getAllObjectNames();
        final ObjectName[] objectNames = new ObjectName[s.size()];
        s.toArray(objectNames);

        return (objectNames);
    }

    public Set<String>
    getAvailJ2EETypes() {
        final SortedSet<ObjectName> allObjectNames = getAllObjectNames();
        final Set<String> j2eeTypes = new HashSet<String>();

        for (final ObjectName objectName : allObjectNames) {
            final String value = Util.getJ2EEType(objectName);

            j2eeTypes.add(value);
        }
        return (j2eeTypes);
    }

}














