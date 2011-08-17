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
package org.glassfish.admin.amx.impl.mbean;

import org.glassfish.admin.amx.base.BulkAccess;
import org.glassfish.admin.amx.util.jmx.JMXUtil;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.ObjectName;

/**
 */
public class BulkAccessImpl extends AMXImplBase // implements BulkAccess
{

    public BulkAccessImpl(final ObjectName parentObjectName) {
        super(parentObjectName, BulkAccess.class);
    }

    public Object[] bulkGetMBeanInfo(final ObjectName[] objectNames) {
        final Object[] infos = new Object[objectNames.length];

        for (int i = 0; i < infos.length; ++i) {
            try {
                infos[i] = getMBeanServer().getMBeanInfo(objectNames[i]);
            } catch (Throwable t) {
                infos[i] = t;
            }
        }
        return (infos);
    }

    public Object[] bulkGetMBeanAttributeInfo(final ObjectName[] objectNames) {
        final Object[] results = new Object[objectNames.length];
        final Object[] mbeanInfos = bulkGetMBeanInfo(objectNames);

        for (int i = 0; i < results.length; ++i) {
            if (mbeanInfos[i] instanceof MBeanInfo) {
                results[i] = ((MBeanInfo) mbeanInfos[i]).getAttributes();
            } else {
                results[i] = mbeanInfos[i];
            }
        }
        return (results);
    }

    public Object[] bulkGetAttributeNames(final ObjectName[] objectNames) {
        final Object[] results = new Object[objectNames.length];
        final Object[] mbeanInfos = bulkGetMBeanInfo(objectNames);

        for (int i = 0; i < results.length; ++i) {
            if (mbeanInfos[i] instanceof MBeanInfo) {
                final MBeanInfo info = (MBeanInfo) mbeanInfos[i];

                results[i] = JMXUtil.getAttributeNames(info.getAttributes());
            } else {
                results[i] = mbeanInfos[i];
            }
        }
        return (results);
    }

    public Object[] bulkGetMBeanOperationInfo(final ObjectName[] objectNames) {
        final Object[] results = new Object[objectNames.length];
        final Object[] mbeanInfos = bulkGetMBeanInfo(objectNames);

        for (int i = 0; i < results.length; ++i) {
            if (mbeanInfos[i] instanceof MBeanInfo) {
                final MBeanInfo info = (MBeanInfo) mbeanInfos[i];

                results[i] = info.getOperations();
            } else {
                results[i] = mbeanInfos[i];
            }
        }
        return (results);
    }

    public Object[] bulkGetAttribute(
            final ObjectName[] objectNames,
            final String attributeName) {
        final Object[] results = new Object[objectNames.length];

        for (int i = 0; i < objectNames.length; ++i) {
            try {
                results[i] = getMBeanServer().getAttribute(objectNames[i], attributeName);
            } catch (Throwable t) {
                results[i] = t;
            }
        }
        return (results);
    }

    public Object[] bulkSetAttribute(
            final ObjectName[] objectNames,
            final Attribute attr) {
        final Object[] results = new Object[objectNames.length];

        for (int i = 0; i < objectNames.length; ++i) {
            try {
                results[i] = null;
                getMBeanServer().setAttribute(objectNames[i], attr);
            } catch (Throwable t) {
                results[i] = t;
            }
        }
        return (results);
    }

    public Object[] bulkGetAttributes(
            final ObjectName[] objectNames,
            final String[] attributeNames) {
        final Object[] results = new Object[objectNames.length];

        // check for empty list; this occurs occassionally and not all MBeans
        // are well-behaved if one asks for an empty list
        if (attributeNames.length != 0) {
            for (int i = 0; i < objectNames.length; ++i) {
                // copy names, in case an MBean messes with the array
                final String[] attributesCopy = attributeNames.clone();

                try {
                    results[i] = getMBeanServer().getAttributes(objectNames[i], attributesCopy);
                } catch (Throwable t) {
                    results[i] = t;
                }
            }
        }
        return (results);
    }

    public Object[] bulkSetAttributes(
            final ObjectName[] objectNames,
            final AttributeList attrs) {
        final Object[] results = new Object[objectNames.length];

        for (int i = 0; i < objectNames.length; ++i) {
            try {
                // avoid alterations to original copy
                final AttributeList attrsCopy = (AttributeList) attrs.clone();

                results[i] = getMBeanServer().setAttributes(objectNames[i], attrsCopy);
            } catch (Throwable t) {
                results[i] = t;
            }
        }
        return (results);
    }

    public Object[] bulkInvoke(
            final ObjectName[] objectNames,
            final String operationName,
            final Object[] args,
            final String[] types) {
        final Object[] results = new Object[objectNames.length];

        for (int i = 0; i < objectNames.length; ++i) {
            try {
                // hopefully the MBean won't alter the args or types
                results[i] = getMBeanServer().invoke(objectNames[i],
                        operationName, args, types);
            } catch (Throwable t) {
                results[i] = t;
            }
        }
        return (results);
    }
}











