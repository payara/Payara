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

import com.sun.appserv.management.base.AMXAttributes;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.ArrayUtil;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.HashSet;
import java.util.Set;

/**
 */
public final class BulkAccessTest
        extends AMXTestBase {
    public BulkAccessTest() {
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }


    public void
    testGetBulkAccess() {
        assert (getBulkAccess() != null);
    }

    public void
    testBulkGetMBeanAttributeInfos()
            throws Exception {
        final long start = now();

        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        // get everything in bulk....
        final Object[] infos =
                getBulkAccess().bulkGetMBeanAttributeInfo(objectNames);

        // now verify that getting it singly yields the same result.
        final MBeanServerConnection conn = getConnection();
        for (int i = 0; i < infos.length; ++i) {

            final MBeanAttributeInfo[] bulkAttributes = (MBeanAttributeInfo[]) infos[i];

            final MBeanInfo info = conn.getMBeanInfo(objectNames[i]);
            assert (ArrayUtil.arraysEqual(info.getAttributes(), bulkAttributes));
        }
        printElapsed("testBulkGetMBeanAttributeInfos", objectNames.length, start);
    }

    public void
    testBulkGetMBeanOperationInfos()
            throws Exception {
        final long start = now();

        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        final Object[] infos =
                getBulkAccess().bulkGetMBeanOperationInfo(objectNames);

        // now verify that getting it singly yields the same result.
        final MBeanServerConnection conn = getConnection();
        for (int i = 0; i < infos.length; ++i) {

            final MBeanOperationInfo[] bulkOperations = (MBeanOperationInfo[]) infos[i];

            final MBeanInfo info = conn.getMBeanInfo(objectNames[i]);
            assert (ArrayUtil.arraysEqual(info.getOperations(), bulkOperations));
        }
        printElapsed("testBulkGetMBeanOperationInfos", objectNames.length, start);
    }

    public void
    testAttributeNamesAttributeCorrect()
            throws Exception {
        final long start = now();

        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        final Object[] nameArrays =
                getBulkAccess().bulkGetAttributeNames(objectNames);

        final Set<ObjectName> failed = new HashSet<ObjectName>();
        // now verify that getting it singly yields the same result.
        for (int i = 0; i < nameArrays.length; ++i) {
            final String[] bulkNames = (String[]) nameArrays[i];

            // verify that the AttributeNames Attribute contains all the names
            final String[] attrNames = (String[])
                    getConnection().getAttribute(objectNames[i], "AttributeNames");

            final Set<String> bulkSet = GSetUtil.newStringSet(bulkNames);
            final Set<String> attrsSet = GSetUtil.newStringSet(attrNames);
            if (!bulkSet.equals(attrsSet)) {
                warning("testAttributeNamesAttributeCorrect failed for " + objectNames[i]);
                failed.add(objectNames[i]);
            }
        }

        if (failed.size() != 0) {
            assert false : "Failures: " + NEWLINE + CollectionUtil.toString(failed, NEWLINE);
        }

        printElapsed("testAttributeNamesAttributeCorrect", objectNames.length, start);
    }

    public void
    testBulkGetMBeanAttributeNames()
            throws Exception {
        final long start = now();

        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        final Object[] nameArrays =
                getBulkAccess().bulkGetAttributeNames(objectNames);

        for (int i = 0; i < nameArrays.length; ++i) {
            final String[] bulkNames = (String[]) nameArrays[i];

            final MBeanInfo info =
                    getConnection().getMBeanInfo(objectNames[i]);

            final String[] names =
                    JMXUtil.getAttributeNames(info.getAttributes());

            assert (ArrayUtil.arraysEqual(names, bulkNames));
        }

        printElapsed("testBulkGetMBeanAttributeNames", objectNames.length, start);
    }

    public void
    testBulkGetAttribute()
            throws Exception {
        final long start = now();

        final String attrName = AMXAttributes.ATTR_OBJECT_NAME;
        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        final Object[] values =
                getBulkAccess().bulkGetAttribute(objectNames, attrName);

        final MBeanServerConnection conn = getConnection();
        for (int i = 0; i < objectNames.length; ++i) {
            final Object value = conn.getAttribute(objectNames[i], attrName);

            assertEquals(values[i], value);
        }

        printElapsed("testBulkGetAttribute", objectNames.length, start);
    }


    public void
    testBulkGetAttributes()
            throws Exception {
        final long start = now();

        final String[] attrNames = new String[]{
                "FullType", "Group", "Name", "DomainRootObjectName", "ContainerObjectName"};
        final ObjectName[] objectNames = getTestUtil().getAllAMXArray();

        final Object[] values =
                getBulkAccess().bulkGetAttributes(objectNames, attrNames);

        final MBeanServerConnection conn = getConnection();
        for (int i = 0; i < objectNames.length; ++i) {
            final AttributeList bulkAttrs = (AttributeList) values[i];

            final AttributeList attrs = (AttributeList) conn.getAttributes(objectNames[i], attrNames);

            assertEquals(bulkAttrs, attrs);
        }
        printElapsed("testBulkGetAttributes", objectNames.length, start);
    }

}


