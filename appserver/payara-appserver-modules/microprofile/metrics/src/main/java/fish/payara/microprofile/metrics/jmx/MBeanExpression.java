/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.microprofile.metrics.jmx;

import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.ATTRIBUTE_SEPARATOR;
import static fish.payara.microprofile.metrics.jmx.MetricsMetadataHelper.SUB_ATTRIBUTE_SEPARATOR;
import java.lang.management.ManagementFactory;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static java.util.logging.Level.WARNING;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

public class MBeanExpression {

    private String mBean;

    private String attributeName;

    private String subAttributeName;

    private ObjectName objectName;

    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private static final Logger LOGGER = Logger.getLogger(MBeanExpression.class.getName());

    public MBeanExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("MBean Expression is null");
        }
        int slashIndex = expression.lastIndexOf(ATTRIBUTE_SEPARATOR);
        if (slashIndex < 0) {
            throw new IllegalArgumentException("MBean Expression is invalid : " + expression);
        }
        mBean = expression.substring(0, slashIndex);
        attributeName = expression.substring(slashIndex + 1);
        if (attributeName.contains(SUB_ATTRIBUTE_SEPARATOR)) {
            int hashIndex = attributeName.indexOf(SUB_ATTRIBUTE_SEPARATOR);
            subAttributeName = attributeName.substring(hashIndex + 1);
            attributeName = attributeName.substring(0, hashIndex);
        }
        try {
            objectName = new ObjectName(mBean);
        } catch (MalformedObjectNameException ex) {
            throw new IllegalArgumentException("MBean Expression is invalid : " + expression);
        }
    }

    public String getMBean() {
        return mBean;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getSubAttributeName() {
        return subAttributeName;
    }

    public ObjectName getObjectName() {
        return objectName;
    }

    public Object getAttribute() throws Exception {
        return mBeanServer.getAttribute(objectName, getAttributeName());
    }

    public String findDynamicKey() {
        for (Map.Entry<String, String> entry : objectName.getKeyPropertyList().entrySet()) {
            if (entry.getValue().contains("*")) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Number getNumberValue() {
        try {
            Object attribute = getAttribute();
            if (attribute instanceof Number) {
                return (Number) attribute;
            } else if (attribute instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) attribute;
                return (Number) compositeData.get(getSubAttributeName());
            } else {
                throw new IllegalArgumentException("The MBean expression is neither a number nor CompositeData: " + getMBean() + ", attribute " + attributeName + ", type " + attribute.getClass());
            }
        } catch (InstanceNotFoundException ex) {
            throw new IllegalStateException("MBean instance not found: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to get numeric value of MBean: " + ex.getMessage(), ex);
        }
    }

    public Set<ObjectName> queryNames(QueryExp query) {
        return mBeanServer.queryNames(objectName, query);
    }

    public List<MBeanAttributeInfo> queryAttributes(ObjectName objectName) {
        try {
            return asList(mBeanServer.getMBeanInfo(objectName).getAttributes());
        } catch (InstanceNotFoundException | IntrospectionException | ReflectionException ex) {
            LOGGER.log(
                    WARNING,
                    String.format("Error in queryAttributes operation where objectName [%s]", objectName),
                    ex
            );
        }
        return emptyList();
    }

    public Object querySubAttributes(ObjectName objectName, String attribute){
        Object subAttributes = null;
        try {
            subAttributes = mBeanServer.getAttribute(objectName, attribute);
        } catch (MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException ex) {
            LOGGER.log(
                    WARNING,
                    String.format(
                            "Error in querySubAttributes operation where objectName [%s] and attribute [%s]",
                            objectName,
                            attribute
                    ), ex
            );
        }
        return subAttributes;
    }

}
