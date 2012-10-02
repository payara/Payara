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
package org.glassfish.admin.amx.impl.mbean;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanInfo;
import javax.management.ObjectName;
import javax.management.openmbean.*;
import javax.management.remote.JMXServiceURL;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

import org.glassfish.admin.amx.base.Sample;
import org.glassfish.admin.amx.util.jmx.JMXUtil;
import org.glassfish.admin.amx.util.jmx.OpenMBeanUtil;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.ListUtil;
import org.glassfish.admin.amx.util.SetUtil;
import org.glassfish.admin.amx.util.StringUtil;

import org.glassfish.admin.amx.util.CollectionUtil;
import org.glassfish.admin.amx.core.Util;

/**
@see Sample
 */
public final class SampleImpl extends AMXImplBase {
    // all Attributes live in a Map

    private final Map<String, Serializable> mAttributes;
    private MBeanInfo mExtendedMBeanInfo;

    public void emitNotifications(final Serializable data, final int numNotifs, final long interval) {
        if (numNotifs <= 0) {
            throw new IllegalArgumentException("" + numNotifs);
        }

        new EmitterThread(data, numNotifs, interval).start();
    }

    public SampleImpl(final ObjectName parentObjectName) {
        super(parentObjectName, Sample.class);
        mAttributes = Collections.synchronizedMap(new HashMap<String, Serializable>());
        mExtendedMBeanInfo = null;
    }

    public void addAttribute(final String name, final Serializable value) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException();
        }

        mAttributes.put(name, value);
        //mExtendedMBeanInfo	= null;
    }

    public void removeAttribute(final String name) {
        mAttributes.remove(name);
        mExtendedMBeanInfo = null;
    }

    private synchronized MBeanInfo createMBeanInfo(final MBeanInfo baseMBeanInfo) {
        final MBeanAttributeInfo[] dynamicAttrInfos = new MBeanAttributeInfo[mAttributes.keySet().size()];
        int i = 0;
        for (final String name : mAttributes.keySet()) {
            final Object value = mAttributes.get(name);
            final String type = value == null ? String.class.getName() : value.getClass().getName();

            dynamicAttrInfos[i] = new MBeanAttributeInfo(name, type, "dynamically-added Attribute",
                    true, true, false);
            ++i;
        }

        final MBeanAttributeInfo[] attrInfos =
                JMXUtil.mergeMBeanAttributeInfos(dynamicAttrInfos, baseMBeanInfo.getAttributes());

        return (JMXUtil.newMBeanInfo(baseMBeanInfo, attrInfos));
    }

    public synchronized MBeanInfo getMBeanInfo() {
        if (mExtendedMBeanInfo == null) {
            mExtendedMBeanInfo = createMBeanInfo(super.getMBeanInfo());
        }

        return (mExtendedMBeanInfo);
    }

    protected Serializable getAttributeManually(final String name) {
        if (!mAttributes.containsKey(name)) {
            throw new RuntimeException(new AttributeNotFoundException(name));
        }
        return mAttributes.get(name);
    }

    protected void setAttributeManually(final Attribute attr) {
        mAttributes.put(attr.getName(), Serializable.class.cast(attr.getValue()));
    }

    private final class EmitterThread extends Thread {

        private final Serializable mData;
        private final int mNumNotifs;
        private final long mIntervalMillis;

        public EmitterThread(final Serializable data, final int numNotifs, final long intervalMillis) {
            mData = data;
            mNumNotifs = numNotifs;
            mIntervalMillis = intervalMillis;
        }

        public void run() {
            for (int i = 0; i < mNumNotifs; ++i) {
                sendNotification(Sample.SAMPLE_NOTIFICATION_TYPE, Sample.USER_DATA_KEY, mData);

                try {
                    Thread.sleep(mIntervalMillis);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    public void uploadBytes(final byte[] bytes) {
        // do nothing; just a bandwidth test
    }
    private final static int MEGABYTE = 1024 * 1024;

    public byte[] downloadBytes(final int numBytes) {
        if (numBytes < 0 || numBytes > 10 * MEGABYTE) {
            throw new IllegalArgumentException("Illegal count: " + numBytes);
        }

        final byte[] bytes = new byte[numBytes];

        return (bytes);
    }

    public ObjectName[] getAllAMX() {
        final List<ObjectName> all = Util.toObjectNameList(getDomainRootProxy().getQueryMgr().queryAll());

        return CollectionUtil.toArray(all, ObjectName.class);
    }

    /** Purpose: have the AMXValidator  check what we're returning as acceptable */
    public Object[] getAllSortsOfStuff() {
        final List<Object> stuff = ListUtil.newList();

        // generate a bunch of fields for a CompositeData, naming them with a simple type eg "Byte"
        final Map<String, Object> values = MapUtil.newMap();
        values.put("ByteField", Byte.valueOf((byte) 0));
        values.put("ShortField", Short.valueOf((short) 0));
        values.put("IntegerField", Integer.valueOf(0));
        values.put("LongField", Long.valueOf(0));
        values.put("FloatField", new Float(0.0));
        values.put("DoubleField", Double.valueOf(0.0));
        values.put("BigDecimalField", new java.math.BigDecimal("999999999999999999999999999999.999999999999999999999999999999"));
        values.put("BigIntegerField", new java.math.BigInteger("999999999999999999999999999999999999999999999999999999999999"));
        values.put("CharacterField", 'x');
        values.put("StringField", "hello");
        values.put("BooleanField", true);
        values.put("DateField", new java.util.Date());
        values.put("ObjectNameField", getObjectName());
        CompositeData data = null;
        try {

            data = OpenMBeanUtil.mapToCompositeData("org.glassfish.test.Sample1", "test", values);
            stuff.add(data);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        // Add all those open types to our main list too
        stuff.add(data);
        stuff.addAll(values.values());
        stuff.add(MapUtil.newMap());
        stuff.add(ListUtil.newList());
        stuff.add(SetUtil.newSet());

        TabularDataSupport table = null;
        try {
            // might not be appropriate TabularData, investigate...
            final String[] indexNames = CollectionUtil.toArray(values.keySet(), String.class);
            final CompositeType rowType = data.getCompositeType();
            final TabularType tabularType = new TabularType("org.glassfish.test.Sample2", "test", rowType, indexNames);
            table = new TabularDataSupport(tabularType);
            table.put(data);
        } catch (final OpenDataException e) {
            throw new RuntimeException(e);
        }
        stuff.add(table);

        final Object[] result = CollectionUtil.toArray(stuff, Object.class);

        return result;
    }
}











