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

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.misc.CollectionUtil;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.util.misc.MapUtil;
import com.sun.appserv.management.util.misc.StringUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 */
public final class GetSetAttributeTest
        extends AMXTestBase {
    public GetSetAttributeTest() {
    }


    private static final Set<String> SKIP_IDENTITY_SET_TEST =
            Collections.unmodifiableSet(GSetUtil.newStringSet(
                    "DynamicReconfigurationEnabled"
            ));

    private void
    testGetSetAttributes(final AMX amx)
            throws Exception {
        final ObjectName objectName = Util.getObjectName(amx);

        boolean skipIdentitySet = false;
        if (amx.getJ2EEType().equals(XTypes.CONFIG_DOTTED_NAMES)) {
            skipIdentitySet = true;
            trace("GetSetAttributeTest.testGetSetAttributes: skipping identity set for " + objectName +
                    " because too many Attributes misbehave.");
        }

        final MBeanServerConnection conn = getMBeanServerConnection();
        final MBeanInfo mbeanInfo = Util.getExtra(amx).getMBeanInfo();

        final Map<String, MBeanAttributeInfo> attrInfos =
                JMXUtil.attributeInfosToMap(mbeanInfo.getAttributes());
        final String[] attrNames = GSetUtil.toStringArray(attrInfos.keySet());

        // get all the Attributes
        final AttributeList values = conn.getAttributes(objectName, attrNames);

        final Map<String, Object> valuesMap = JMXUtil.attributeListToValueMap(values);

        final Set<String> getFailed = new HashSet<String>();
        final Map<String, Object> setFailed = new HashMap<String, Object>();

        for (final MBeanAttributeInfo attrInfo : attrInfos.values()) {
            final String name = attrInfo.getName();
            if (!valuesMap.keySet().contains(name)) {
                getFailed.add(name);
                continue;
            }

            if (attrInfo.isReadable()) {
                final Object value = valuesMap.get(name);

                if (attrInfo.isWritable() && (!skipIdentitySet)) {
                    if (SKIP_IDENTITY_SET_TEST.contains(name)) {
                        trace("Skipping identity-set check for known problem attribute " +
                                StringUtil.quote(name) +
                                " of MBean " + JMXUtil.toString(objectName));
                    } else {
                        // set it to the same value as before
                        try {
                            final Attribute attr = new Attribute(name, value);
                            conn.setAttribute(objectName, attr);
                        }
                        catch (Exception e) {
                            setFailed.put(name, value);

                            warning("Could not set Attribute " + name + " of MBean " +
                                    StringUtil.quote(objectName) +
                                    " to the same value: " +
                                    StringUtil.quote("" + value));
                        }
                    }
                }
            }
        }

        if (getFailed.size() != 0) {
            warning("(SUMMARY) Could not get Attributes for " +
                    StringUtil.quote(objectName) + NEWLINE +
                    CollectionUtil.toString(getFailed, NEWLINE));

            for (final String attrName : getFailed) {
                try {
                    final Object value = conn.getAttribute(objectName, attrName);
                    warning("Retry of Attribute " +
                            attrName + " succeed with value " + value);
                }
                catch (Exception e) {
                    warning("Attribute " + attrName + " failed with " +
                            e.getClass() + ": " + e.getMessage());
                }
            }
        }

        if (setFailed.size() != 0) {
            warning("(SUMMARY) Could not identity-set Attributes for " +
                    StringUtil.quote(objectName) + NEWLINE +
                    MapUtil.toString(setFailed, NEWLINE));
        }
    }


    public void
    testGetSetAttributes()
            throws Exception {
        final Set<AMX> all = getAllAMX();

        for (final AMX amx : all) {
            testGetSetAttributes(amx);
        }
    }
}
















