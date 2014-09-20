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

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.QueryMgr;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AdminObjectResourceConfig;
import com.sun.appserv.management.config.ConnectorResourceConfig;
import com.sun.appserv.management.config.CustomResourceConfig;
import com.sun.appserv.management.config.JDBCResourceConfig;
import com.sun.appserv.management.config.JNDIResourceConfig;
import com.sun.appserv.management.config.MailResourceConfig;
import com.sun.appserv.management.config.ResourceConfig;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 */
public final class QueryMgrTest
        extends AMXTestBase {
    final QueryMgr mQM;

    public QueryMgrTest() {
        mQM = getQueryMgr();
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }


    private <T> void
    checkSetsEqual(
            final String msg,
            final Set<T> set1,
            final Set<T> set2) {
        if (!set1.equals(set2)) {
            final Set<T> conflict = GSetUtil.newNotCommonSet(set1, set2);
            final String values = CollectionUtil.toString(conflict, "\n");
            failure(msg + ", mismatch =\n" + values);
        }
    }


    private void
    checkAMXWithObjectNames(
            final String msg,
            final Set<? extends AMX> set1,
            final Set<ObjectName> set2) {
        if (set1.size() != set2.size()) {
            failure(msg + "set sizes don't match: " + set1.size() + " != " + set2.size());
        }

        final Set<ObjectName> set1ObjectNames = new HashSet<ObjectName>();
        for (final AMX item : set1) {
            final ObjectName objectName = Util.getObjectName(item);

            if (set1ObjectNames.contains(objectName)) {
                failure("set1 contains the same proxy twice with ObjectName: " + objectName);
            }
            set1ObjectNames.add(objectName);
        }

        checkSetsEqual(msg, set1ObjectNames, set2);
    }

    static final private Set<String> RESOURCE_TYPES = GSetUtil.newUnmodifiableStringSet(
            JDBCResourceConfig.J2EE_TYPE,
            MailResourceConfig.J2EE_TYPE,
            CustomResourceConfig.J2EE_TYPE,
            JNDIResourceConfig.J2EE_TYPE,
            ConnectorResourceConfig.J2EE_TYPE,
            AdminObjectResourceConfig.J2EE_TYPE);

    public void
    testQueryJ2EETypesSet()
            throws ClassNotFoundException {
        final long start = now();

        final Set<ResourceConfig> resources = mQM.queryJ2EETypesSet(RESOURCE_TYPES);
        final Set<ObjectName> resourcesObjectNames = mQM.queryJ2EETypesObjectNameSet(RESOURCE_TYPES);

        assert (resourcesObjectNames.size() >= 1) : "testQueryJ2EETypesSet: no resources found for " + CollectionUtil.toString(RESOURCE_TYPES, ", ");
        assert (resources.size() >= 1) : "testQueryJ2EETypesSet: no resource ObjectNames found!";
        ;

        checkAMXWithObjectNames(
                "queryJ2EETypesSet(...) != queryJ2EETypesObjectNameSet(...)",
                resources, resourcesObjectNames);

        //println( resourcesObjectNames );

        printElapsed("testQueryJ2EETypesSet", start);
    }


    public void
    testQueryJ2EETypePvsON()
            throws ClassNotFoundException {
        final long start = now();

        final String testType1 = XTypes.DOMAIN_CONFIG;
        checkAMXWithObjectNames(
                "queryJ2EETypeSet(XTypes.DOMAIN_CONFIG) != queryJ2EETypeObjectNameSet(XTypes.DOMAIN_CONFIG)",
                mQM.queryJ2EETypeSet(testType1),
                mQM.queryJ2EETypeObjectNameSet(testType1));
        printElapsed("testQueryJ2EETypePvsON", start);
    }

    public void
    testQueryJ2EENamePvsON()
            throws ClassNotFoundException {
        final long start = now();

        final String name = getDomainRoot().getName();
        checkAMXWithObjectNames(
                "queryJ2EENameSet() != queryJ2EENameObjectNameSet()",
                mQM.queryJ2EENameSet(name),
                mQM.queryJ2EENameObjectNameSet(name));
        printElapsed("testQueryJ2EENamePvsON", start);
    }

    public void
    testQueryPatternPvsON()
            throws ClassNotFoundException {
        final long start = now();

        final String domain = Util.getObjectName(mQM).getDomain();
        final ObjectName pat = JMXUtil.newObjectNamePattern(domain, "*");
        checkAMXWithObjectNames(
                "queryPatternSet() != queryPatternObjectNameSet()",
                mQM.queryPatternSet(pat),
                mQM.queryPatternObjectNameSet(pat));

        checkAMXWithObjectNames(
                "queryPatternSet() != queryPatternObjectNameSet()",
                mQM.queryPatternSet(domain, "*"),
                mQM.queryPatternObjectNameSet(domain, "*"));
        printElapsed("testQueryPatternPvsON", start);
    }

    public void
    testQueryPropsPvsON()
            throws ClassNotFoundException {
        final long start = now();

        final String domain = Util.getObjectName(mQM).getDomain();
        final String props = Util.makeRequiredProps(XTypes.DOMAIN_ROOT, domain);
        checkAMXWithObjectNames(
                "queryPropsSet() != queryPropsObjectNameSet()",
                mQM.queryPropsSet(props),
                mQM.queryPropsObjectNameSet(props));
        printElapsed("testQueryPropsPvsON", start);
    }

    public void
    testQueryWildPvsON() {
        final long start = now();

        final String[] wildNames = new String[]{"*"};
        final String[] wildValues = new String[]{"*"};
        checkAMXWithObjectNames(
                "queryWildSet() != queryWildObjectNameSet()",
                mQM.queryWildSet(wildNames, wildValues),
                mQM.queryWildObjectNameSet(wildNames, wildValues));
        printElapsed("testQueryWildPvsON", start);
    }

    public void
    testQueryInterfacePvsON()
            throws ClassNotFoundException {
        final long start = now();

        final Set<AMX> candidates = mQM.queryAllSet();
        final Set<ObjectName> candidateObjectNames = Util.toObjectNames(candidates);
        final String interfaceName = DomainRoot.class.getName();
        checkAMXWithObjectNames(
                "queryInterfaceSet() != queryInterfaceObjectNameSet()",
                mQM.queryInterfaceSet(interfaceName, candidateObjectNames),
                mQM.queryInterfaceObjectNameSet(interfaceName, candidateObjectNames));
        printElapsed("testQueryInterfacePvsON", start);
    }

    public void
    testQueryAll_AMXMatchesObjectName()
            throws ClassNotFoundException {
        final long start = now();

        final Set<AMX> allSet = mQM.queryAllSet();
        final Set<ObjectName> allObjectNameSet = mQM.queryAllObjectNameSet();

        assert (allSet.size() == allObjectNameSet.size());

        checkAMXWithObjectNames(
                "queryAllSet() != queryAllObjectNameSet()",
                allSet, allObjectNameSet);

        printElapsed("testQueryAll_AMXMatchesObjectName", start);
    }


    public void
    testGetQueryMgr() {
        assert (getQueryMgr() != null);
    }

    public void
    testQueryAll() {
        final long start = now();
        final Set result = getQueryMgr().queryAllSet();

        assert (result.size() > 1);

        printElapsed("testQueryAll", start);
    }

    public void
    testGetJ2EETypeObjectNames()
            throws Exception {
        final long start = now();

        final Set<String> j2eeTypes = getTestUtil().getAvailJ2EETypes();
        for (final String j2eeType : j2eeTypes) {
            final Set<AMX> results =
                    getQueryMgr().queryJ2EETypeSet(j2eeType);
            assert (results.size() >= 1);
        }

        printElapsed("testGetJ2EETypeObjectNames", start);
    }

    public void
    testGetJ2EETypeProxies()
            throws Exception {
        final long start = now();
        final Set<String> j2eeTypes = getTestUtil().getAvailJ2EETypes();

        for (final String j2eeType : j2eeTypes) {
            final Set<AMX> proxies =
                    getQueryMgr().queryJ2EETypeSet(j2eeType);

            assert (proxies != null);
        }

        printElapsed("testGetJ2EETypeProxies", start);
    }

    public void
    testQueryPatternProxies()
            throws Exception {
        final long start = now();

        final Set proxies = getQueryMgr().queryPropsSet("*");

        assert proxies != null;

        printElapsed("testQueryPatternProxies", start);
    }

    private <T extends AMX> Set<String>
    getAllInterfaceNames(final Set<T> amxSet) {
        final Set<String> interfaceNames = new HashSet<String>();
        for (final T amx : amxSet) {
            interfaceNames.add(Util.getExtra(amx).getInterfaceName());
        }

        return (interfaceNames);
    }

    /**
     Final all available interfaces, then for each type of interface, query
     for all items that have the interface and verify that's the interface
     they return from getInterfaceName().
     */
    public void
    testQueryInterfaceObjectNames()
            throws Exception {
        final long start = now();

        final QueryMgr queryMgr = getQueryMgr();
        final Set<AMX> allAMX = getQueryMgr().queryAllSet();
        final Set<ObjectName> allAMXObjectNames = Util.toObjectNames(allAMX);
        final Set<String> interfaceNames = getAllInterfaceNames(allAMX);

        for (final String interfaceName : interfaceNames) {
            final Set amxs = queryMgr.queryInterfaceSet(interfaceName, allAMXObjectNames);

            final Iterator amxIter = amxs.iterator();
            while (amxIter.hasNext()) {
                final AMX amx = Util.asAMX(amxIter.next());
                assert (interfaceName.equals(Util.getExtra(amx).getInterfaceName()));
            }
        }

        printElapsed("testQueryInterfaceObjectNames", start);
    }


}






