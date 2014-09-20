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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/base/GenericTest.java,v 1.8 2007/05/05 05:23:53 tcfujii Exp $
* $Revision: 1.8 $
* $Date: 2007/05/05 05:23:53 $
*/
package org.glassfish.admin.amxtest.base;

import com.sun.appserv.management.util.jmx.JMXUtil;
import com.sun.appserv.management.util.jmx.ReadWriteAttributeFilter;
import com.sun.appserv.management.util.misc.ArrayConversion;
import com.sun.appserv.management.util.misc.ArrayUtil;
import com.sun.appserv.management.util.misc.ClassUtil;
import com.sun.appserv.management.util.misc.ExceptionUtil;
import com.sun.appserv.management.util.stringifier.ArrayStringifier;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.Capabilities;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 */
public final class GenericTest
        extends AMXTestBase {
    final boolean mDoInfo;
    final boolean mDoAttributes;
    final boolean mDoOperations;
    final boolean mwarnings;

    public GenericTest()
            throws IOException {
        mDoInfo = true;
        mDoAttributes = true;
        mDoOperations = true;
        mwarnings = true;
    }

    public static Capabilities
    getCapabilities() {
        return getOfflineCapableCapabilities(true);
    }

    private Map<String, Throwable>
    validateAttributesSingly(
            final ObjectName objectName,
            final String[] attrNames,
            Map<String, Throwable> failures,
            Map<String, Throwable> warnings)
            throws Exception {
        MBeanServerConnection conn = getConnection();

        for (int i = 0; i < attrNames.length; ++i) {
            final String attrName = attrNames[i];

            try {
                final Object a = conn.getAttribute(objectName, attrName);

                if (a == null) {
                    // null is legal, apparently
                }
            }
            catch (NotSerializableException e) {
                warnings.put(attrName, e);
            }
            catch (IOException e) {
                failures.put(attrName, e);
            }
            catch (Exception e) {
                failures.put(attrName, e);
            }
        }

        return (failures);
    }


    private String
    getExceptionMsg(final Throwable e) {
        String msg = null;

        if (e instanceof IOException) {
            msg = "received an exception of class " + e.getClass().getName();

            if (shouldPrintStackTraces()) {
                msg = msg + "Stack trace = \n" +
                        ExceptionUtil.getStackTrace(ExceptionUtil.getRootCause(e));
            }
        } else {
            msg = "threw an Exception of type " +
                    e.getClass().getName() + ", message =  " + e.getMessage();

            if (shouldPrintStackTraces()) {
                msg = msg + "\n" + ExceptionUtil.getStackTrace(e);
            }
        }

        final Throwable rootCause = ExceptionUtil.getRootCause(e);

        if (rootCause != e) {
            msg = msg + "...\nRoot cause was exception of type " + e.getClass().getName() + ", message = " +
                    rootCause.getMessage();


            if (shouldPrintStackTraces()) {
                msg = msg + "\n" + ExceptionUtil.getStackTrace(rootCause);
            }
        }

        return (msg);
    }

    MBeanAttributeInfo
    findAttributeInfo(
            final MBeanAttributeInfo[] infos,
            String attrName) {
        MBeanAttributeInfo info = null;

        for (int i = 0; i < infos.length; ++i) {
            if (infos[i] != null && infos[i].getName().equals(attrName)) {
                info = infos[i];
                break;
            }
        }

        assert (info != null);
        return (info);
    }

    private void
    displayAttributeFailuresOrWarnings(
            final boolean failure,
            final ObjectName objectName,
            final MBeanAttributeInfo[] infos,
            final Map<String, Throwable> problems)
            throws Exception {
        trace("");
        trace(problems.keySet().size() + (failure ? " Failures: " : " Warnings: ") + objectName);

        int i = 0;
        for (final String attrName : problems.keySet()) {
            final Throwable t = problems.get(attrName);

            final MBeanAttributeInfo info = findAttributeInfo(infos, attrName);

            final String prefix = "(" + (i + 1) + ")" + " getting Attribute \"" + attrName + "\" of type " +
                    info.getType() + " ";

            if (t == null) {
                trace(prefix + "returned null");
            } else {
                trace(prefix + getExceptionMsg(t));
            }
            ++i;
        }
    }


    private boolean
    validateMBeanInfo(
            final ObjectName objectName,
            final MBeanInfo info) {
        boolean valid = true;

        if (ArrayUtil.arrayContainsNulls(info.getAttributes())) {
            warning("MBean has nulls in its MBeanAttributeInfo[]: " + objectName);
            valid = false;
        }

        if (ArrayUtil.arrayContainsNulls(info.getConstructors())) {
            warning("MBean has nulls in its MBeanConstructorInfo[]: " + objectName);
            valid = false;
        }

        if (ArrayUtil.arrayContainsNulls(info.getOperations())) {
            warning("MBean has nulls in its MBeanOperationInfo[]: " + objectName);
            valid = false;
        }

        if (ArrayUtil.arrayContainsNulls(info.getNotifications())) {
            warning("MBean has nulls in its MBeanNotificationInfo[]: " + objectName);
            valid = false;
        }

        return (valid);
    }

    static final private String SECTION_LINE =
            "--------------------------------------------------------------------------------";


    private void
    printDuplicateAttributes(
            final ObjectName objectName,
            MBeanAttributeInfo[] attrInfos,
            String name) {
        String msg = "MBean " + quote(objectName) + " has the same Attribute listed more than once:\n";

        for (int i = 0; i < attrInfos.length; ++i) {
            final MBeanAttributeInfo a = attrInfos[i];

            if (a.getName().equals(name)) {
                msg = msg + name + ": " + a.getType() + ", " + quote(a.getDescription());
            }
        }

        warning(msg);
    }

    private boolean
    validateUniqueAttributeNames(
            final ObjectName objectName,
            MBeanAttributeInfo[] attrInfos) {
        boolean valid = true;
        final MBeanAttributeInfo[] infos =
                JMXUtil.filterAttributeInfos(attrInfos, ReadWriteAttributeFilter.READABLE_FILTER);
        final String[] names = JMXUtil.getAttributeNames(infos);

        if (ArrayConversion.arrayToSet(names).size() != attrInfos.length) {
            final Set<String> set = new HashSet<String>();

            for (int i = 0; i < names.length; ++i) {
                final String name = names[i];

                if (set.contains(name)) {
                    valid = false;

                    printDuplicateAttributes(objectName, attrInfos, name);
                } else {
                    set.add(name);
                }
            }
            set.clear();
        }

        return (valid);
    }

    private boolean
    validateMissingAndEmptyAttributeNames(final ObjectName objectName) {
        boolean valid = true;
        final MBeanServerConnection conn = getConnection();

        AttributeList attrs = null;
        try {
            attrs = conn.getAttributes(objectName, new String[0]);
            if (attrs == null) {
                warning("MBean " + quote(objectName) +
                        " returned NULL for an empty AttributeList");
                valid = false;
            } else if (attrs.size() != 0) {
                warning("MBean " + quote(objectName) +
                        " returned attributes for an empty AttributeList");
                valid = false;
            }
        }
        catch (Exception e) {
            valid = false;

            warning("MBean " + quote(objectName) +
                    " threw an exception getting an empty attribute list");
        }

        try {
            final String notFoundName = "bogus." + System.currentTimeMillis();
            attrs = conn.getAttributes(objectName, new String[]{notFoundName});
            if (attrs == null) {
                warning("MBean " + quote(objectName) +
                        " returned NULL for a missing Attribute");
                valid = false;
            } else if (attrs.size() != 0) {
                warning("MBean " + quote(objectName) +
                        " returned attributes for a non-existent name");
                valid = false;
            }
        }
        catch (Exception e) {
            valid = false;

            warning("MBean " + quote(objectName) +
                    " threw an exception when getAttributes() was called with a " +
                    "non-existent Attribute, exception class = " +
                    e.getClass().getName());
        }

        return (valid);
    }

    private boolean
    validateAttributeTypes(
            final ObjectName objectName,
            final AttributeList attrs,
            final MBeanAttributeInfo[] attrInfos)
            throws Exception {
        boolean valid = true;

        final Map<String, MBeanAttributeInfo> attrInfosMap = JMXUtil.attributeInfosToMap(attrInfos);

        final Iterator iter = attrs.iterator();
        while (iter.hasNext()) {
            final Attribute attr = (Attribute) iter.next();

            final String name = attr.getName();
            final Object value = attr.getValue();
            final MBeanAttributeInfo attrInfo = (MBeanAttributeInfo) attrInfosMap.get(name);
            if (attrInfo == null) {
                valid = false;
                warning("MBean " + objectName + " returned an Attribute not " +
                        "declared in its MBeanInfo: " + name);
            } else if (value != null) {
                final String typeName = attrInfo.getType();
                final Class<?> infoClass = ClassUtil.getClassFromName(typeName);
                final Class<?> valueClass = value.getClass();

                if (infoClass == null) {
                    valid = false;
                    warning("Can't find class for: " + typeName);
                } else if (!infoClass.isAssignableFrom(valueClass)) {
                    final Class<?> objectClass = ClassUtil.PrimitiveClassToObjectClass(infoClass);

                    if (!objectClass.isAssignableFrom(valueClass)) {
                        valid = false;
                        warning("MBean " + objectName + " returned Attribute " +
                                name + "=" + value +
                                " of class " + value.getClass().getName() +
                                " not matching its MBeanInfo: " + infoClass.getName());
                    }
                }
            }
        }

        return (valid);
    }

    private boolean
    validateAttributes(
            final ObjectName objectName,
            final MBeanAttributeInfo[] attrInfos)
            throws Exception {
        boolean valid = true;

        final MBeanAttributeInfo[] readableInfos = JMXUtil.filterAttributeInfos(attrInfos,
                                                                                ReadWriteAttributeFilter.READABLE_FILTER);
        final String[] attrNames = JMXUtil.getAttributeNames(readableInfos);
        Arrays.sort(attrNames);

        if (attrNames.length != 0) {
            // if we can fetch all the attributes, then the MBean is OK;
            // try this first for efficiency
            try {
                //trace( objectName.getKeyProperty( "j2eeType" ) + ": " + attrNames.length );
                final AttributeList attrs = getConnection().getAttributes(objectName, attrNames);

                if (attrs == null) {
                    warning("MBean " + quote(objectName) + " returned NULL for its AttributeList");
                    valid = false;
                } else if (attrs.size() != readableInfos.length) {
                    // mismatch between claimed number of attributes and actual
                    final ArrayStringifier as = new ArrayStringifier(", ", true);
                    final String claimedString = as.stringify(attrNames);

                    final Set<String> actualSet = JMXUtil.attributeListToValueMap(attrs).keySet();
                    final Set<String> missingSet = ArrayConversion.arrayToSet(attrNames);
                    missingSet.removeAll(actualSet);

                    final String[] missingNames = (String[]) ArrayConversion.setToArray(missingSet, true);
                    Arrays.sort(missingNames);
                    final String missingString = as.stringify(missingNames);

                    warning("MBean " + quote(objectName) +
                            " did not supply the " +
                            missingNames.length + " attributes " + missingString);
                }

                valid = validateAttributeTypes(objectName, attrs, readableInfos);
            }
            catch (Exception e) {
                trace(SECTION_LINE);
                final String msg = "getAttributes() failed on " + quote(objectName) + ", exception =\n" + e;

                if (e instanceof NotSerializableException) {
                    warning(msg);
                } else {
                    warning(msg);
                    valid = false;
                }
                // do them one-at-a time to see where failure occurs
                final Map<String, Throwable> failures = new HashMap<String, Throwable>();
                final Map<String, Throwable> warnings = new HashMap<String, Throwable>();

                validateAttributesSingly(objectName, attrNames, failures, warnings);

                trace("Validating attributes one-at-a-time using getAttribute() for " + quote(objectName));
                if (failures.size() == 0 && warnings.size() == 0) {
                    warning(" during getAttributes(" +
                            ArrayStringifier.stringify(attrNames, ",") + ") for: " + objectName +
                            " (but Attributes work when queried one-at-a-time).\nIt " +
                            getExceptionMsg(e));
                }

                if (failures.size() != 0) {
                    displayAttributeFailuresOrWarnings(true, objectName, readableInfos, failures);
                }

                if (warnings.size() != 0) {
                    displayAttributeFailuresOrWarnings(false, objectName, readableInfos, warnings);
                }

                trace(SECTION_LINE);
            }
        } else {
            valid = true;
        }

        if (!validateUniqueAttributeNames(objectName, attrInfos)) {
            valid = false;
        }

        if (!validateMissingAndEmptyAttributeNames(objectName)) {
            valid = false;
        }

        return (valid);
    }

    void
    checkObjectNameReturnValue(
            MBeanServerConnection conn,
            ObjectName callee,
            MBeanOperationInfo operationInfo,
            ObjectName resultOfCall)
            throws Exception {
        try {
            printVerbose("checking MBean info for: " + resultOfCall);
            final MBeanInfo mbeanInfo = conn.getMBeanInfo(resultOfCall);
        }
        catch (InstanceNotFoundException e) {
            trace("WARNING: MBean " + resultOfCall + " returned from " +
                    operationInfo.getReturnType() + " " + operationInfo.getName() + "() does not exist");

        }
        catch (Exception e) {
            trace("WARNING: MBean " + resultOfCall + " returned from " +
                    operationInfo.getReturnType() + " " + operationInfo.getName() +
                    "() can't supply MBeanInfo: " + getExceptionMsg(e)
            );

            if (e instanceof IOException) {
                throw (IOException) e;
            }
        }
    }

    void
    checkGetterResult(
            MBeanServerConnection conn,
            ObjectName callee,
            MBeanOperationInfo operationInfo,
            Object resultOfCall)
            throws Exception {
        if (resultOfCall instanceof ObjectName) {
            final ObjectName name = (ObjectName) resultOfCall;

            checkObjectNameReturnValue(conn, callee, operationInfo, name);
        } else if (resultOfCall instanceof ObjectName[]) {
            final ObjectName[] names = (ObjectName[]) resultOfCall;

            for (int i = 0; i < names.length; ++i) {
                checkObjectNameReturnValue(conn, callee, operationInfo, names[i]);
            }
        }
    }

    private boolean
    validateGetters(
            final ObjectName objectName,
            final MBeanOperationInfo[] operationInfos)
            throws Exception {
        boolean valid = true;
        MBeanServerConnection conn = getConnection();

        for (int i = 0; i < operationInfos.length; ++i) {
            final MBeanOperationInfo info = operationInfos[i];

            if (JMXUtil.isGetter(info)) {
                boolean opValid = false;

                try {
                    printVerbose("invoking getter: " + info.getName() + "()");
                    final Object result = conn.invoke(objectName, info.getName(), null, null);

                    checkGetterResult(conn,
                                      objectName, info, result);
                }
                catch (Exception e) {
                    warning("Failure: calling " + info.getName() + "() on " + objectName +
                            ": " + getExceptionMsg(e));

                    if (e instanceof IOException) {
                        throw ((IOException) e);
                    }
                    valid = false;
                }
            }
        }

        return (valid);
    }


    boolean
    shouldPrintStackTraces() {
        return (true);
    }


    private boolean
    validate(final ObjectName objectName)
            throws Exception {
        boolean valid = true;

        MBeanServerConnection conn = getConnection();

        MBeanInfo info = null;
        try {
            info = conn.getMBeanInfo(objectName);
        }
        catch (Exception e) {
            valid = false;
            warning(" during getMBeanInfo() for: " + objectName + "\n" +
                    " message = " + e.getMessage());
            // abort--the connection has died
            throw e;
        }

        if (mDoInfo && !validateMBeanInfo(objectName, info)) {
            trace("validateMBeanInfo failed for: " + objectName);
            valid = false;
        }


        if (mDoAttributes &&
                !validateAttributes(objectName, info.getAttributes())) {
            trace("validateAttributes failed for: " + objectName);
            valid = false;
        }

        if (mDoOperations &&
                !validateGetters(objectName, info.getOperations())) {
            trace("validateGetters failed for: " + objectName);
            valid = false;
        }

        return (valid);
    }


    private void
    validate(final ObjectName[] objectNames)
            throws Exception {
        int failureCount = 0;

        trace("Validating: ");
        if (mDoInfo) {
            trace("- MBeanInfo");
        }
        if (mDoAttributes) {
            trace("- Attributes");
        }
        if (mDoOperations) {
            trace("- Operations (getters)");
        }

        trace("");

        for (int i = 0; i < objectNames.length; ++i) {
            final ObjectName objectName = objectNames[i];

            printVerbose("Validating: " + objectName);

            if (!shouldTest(objectName)) {
                notTested(objectName);
                continue;
            }

            final boolean valid = validate(objectName);
            if (!valid) {
                ++failureCount;
            }
        }

        trace("Total mbeans failing: " + failureCount);
    }


    public void
    testGenerically()
            throws Exception {
        final Set<ObjectName> all = getTestUtil().getAllObjectNames();

        final ObjectName[] allObjectNames = new ObjectName[all.size()];
        all.toArray(allObjectNames);
        validate(allObjectNames);
    }

}


