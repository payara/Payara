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

package org.glassfish.admin.amx.base;

import javax.management.Attribute;
import javax.management.ObjectName;

import org.glassfish.admin.amx.annotation.ManagedOperation;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.glassfish.admin.amx.core.AMXProxy;
import org.glassfish.admin.amx.core.Util;

import org.glassfish.admin.amx.core.AMXMBeanMetadata;

/**
Bulk access to various JMX constructs. The term "bulk" refers to the fact the multiple
MBeans are accessed together on the server side, to minimize remote
invocation of many MBeans.
<p>
Because a failure can occur with a particular MBeans, results or failures are
communicated back in an array of the exact size of the original ObjectName[].
Examining the results array yields either the result, or a Throwable, if one
occured.  This is why all results are of type Object[].
<p>
Clients wishing to use this interface should note that they may first
need to obtain an ObjectName[] from a Set or Map of {@link AMXProxy}.  The easiest way
to do this is to use {@link Util#toObjectNames} followed by
conversion of the Set to an ObjectName[].
 */
@Taxonomy(stability = Stability.UNCOMMITTED)
@AMXMBeanMetadata(singleton = true, globalSingleton = true, leaf = true)
public interface BulkAccess extends AMXProxy, Singleton, Utility
{
    /**
    Call getMBeanInfo() for multiple MBeans.
    For objectNames[ i ], results[ i ] will be the resulting MBeanInfo[],
    or contain a Throwable if something was thrown.

    @param objectNames
    @return Info[], one for each objectName, null if not found
     */
    @ManagedOperation
    public Object[] bulkGetMBeanInfo(ObjectName[] objectNames);

    /**
    Call getMBeanInfo().getAttributes() for multiple MBeans.
    For objectNames[ i ], results[ i ] will be the resulting MBeanAttributeInfo[],
    or contain a Throwable if something was thrown.

    @param objectNames
    @return AttributeInfo[][], one AttributeInfo[] for each objectName, null if not found
     */
    @ManagedOperation
    public Object[] bulkGetMBeanAttributeInfo(ObjectName[] objectNames);

    /**
    Call getMBeanInfo().getOperations() for multiple MBeans.
    For objectNames[ i ], results[ i ] will be the resulting MBeanOperationInfo[],
    or contain a Throwable if something was thrown.

    @param objectNames
    @return OperationInfo[][], one OperationInfo[] for each objectName, null if not found
     */
    @ManagedOperation
    public Object[] bulkGetMBeanOperationInfo(ObjectName[] objectNames);

    /**
    Call getMBeanInfo().getAttributes() for multiple MBeans, then extracts the
    Attribute name from each Attribute.
    For objectNames[ i ], results[ i ] will be the resulting String[], consisting
    of all Attribute names for that MBean,
    or contain a Throwable if something was thrown.

    @param objectNames
    @return Object[][], one String[] for each objectName, null if not found, or a Throwable
     */
    @ManagedOperation
    public Object[] bulkGetAttributeNames(ObjectName[] objectNames);

    /**
    Call getAttribute( attributeName ) for multiple MBeans.
    For objectNames[ i ], results[ i ] will be the resulting value,
    or contain a Throwable if something was thrown..

    @param objectNames
    @param attributeName
    @return array of Object, which may be the Attribute value, or a Throwable
     */
    @ManagedOperation
    public Object[] bulkGetAttribute(final ObjectName[] objectNames,
                                     final String attributeName);

    /**
    Call setAttribute( attr ) for multiple MBeans.
    For objectNames[ i ], results[ i ] will be null if successful,
    or contain a Throwable if not.

    @param objectNames
    @param attr
    @return array of Object, each is null or a Throwable
     */
    @ManagedOperation
    public Object[] bulkSetAttribute(final ObjectName[] objectNames,
                                     final Attribute attr);

    /**
    Call getAttributes( attributeNames ) for multiple MBeans.
    For objectNames[ i ], results[ i ] will contain the resulting
    AttributeList, or a Throwable if unsuccessful.

    @return array of Object, which may be the AttributeList, or a Throwable
     */
    @ManagedOperation
    public Object[] bulkGetAttributes(final ObjectName[] objectNames,
                                      final String[] attributeNames);

    /**
    Call invoke( ... ) for multiple MBeans.
    For objectNames[ i ], results[ i ] will be the result,
    or contain a Throwable if something was thrown..
    <p>
    <b>WARNING: No guarantee can be made that the MBeans being
    invoked will not alter their arguments, thus altering the
    parameters that subsequent MBeans receive when invoked.</b>

    @param objectNames
    @param operationName
    @param args
    @param types
    @return array of Object, which will be the result of the invoke, or a Throwable
     */
    @ManagedOperation
    public Object[] bulkInvoke(final ObjectName[] objectNames,
                               final String operationName,
                               final Object[] args, final String[] types);

}


