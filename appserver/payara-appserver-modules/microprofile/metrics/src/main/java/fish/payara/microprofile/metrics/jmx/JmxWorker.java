/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import org.eclipse.microprofile.metrics.Metadata;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;

public class JmxWorker {

    private static final String PLACEHOLDER = "%s";
    private static MBeanServer mBeanServer;
    private static JmxWorker worker;
    
    private static final Logger LOGGER = Logger.getLogger(JmxWorker.class.getName());

    private JmxWorker() { /* singleton */ }

    public static JmxWorker getInstance() {
        if (worker == null) {
            worker = new JmxWorker();
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
        }
        return worker;
    }

    /**
     * Read a value from the MBeanServer
     *
     * @param mbeanExpression The expression to look for
     * @return The value of the Mbean attribute
     */
    public Number getValue(String mbeanExpression) {

        if (mbeanExpression == null) {
            throw new IllegalArgumentException("MBean Expression is null");
        }
        if (!mbeanExpression.contains("/")) {
            throw new IllegalArgumentException(mbeanExpression);
        }

        int slashIndex = mbeanExpression.indexOf('/');
        String mbean = mbeanExpression.substring(0, slashIndex);
        String attName = mbeanExpression.substring(slashIndex + 1);
        String subItem = null;
        if (attName.contains("#")) {
            int hashIndex = attName.indexOf('#');
            subItem = attName.substring(hashIndex + 1);
            attName = attName.substring(0, hashIndex);
        }

        try {
            ObjectName objectName = new ObjectName(mbean);
            Object attribute = mBeanServer.getAttribute(objectName, attName);
            if (attribute instanceof Number) {
                return (Number) attribute;
            } else if (attribute instanceof CompositeData) {
                CompositeData compositeData = (CompositeData) attribute;
                return (Number) compositeData.get(subItem);
            } else {
                throw new IllegalArgumentException(mbeanExpression);
            }
        } catch (IllegalArgumentException | AttributeNotFoundException | InstanceNotFoundException | MBeanException | MalformedObjectNameException | ReflectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * We need to expand entries that are marked with the <b>multi</b> flag into
     * the actual MBeans. This is done by replacing a placeholder of <b>%s</b>
     * in the name and MBean name with the real Mbean key-value.
     *
     * @param entries List of entries
     */
    public void expandMultiValueEntries(List<JMXBeanMetadata> entries) {
        List<JMXBeanMetadata> result = new ArrayList<>();
        List<Metadata> toBeRemoved = new ArrayList<>(entries.size());
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        for (JMXBeanMetadata entry : entries) {
            if (entry.isMulti()) {
                String name = entry.getMbean().replace(PLACEHOLDER, "*");
                String attName;
                String queryableName;
                int slashIndex = name.indexOf('/');

                // MBeanName is invalid, lets skip this altogether
                if (slashIndex < 0) {
                    toBeRemoved.add(entry);
                    continue;
                }

                queryableName = name.substring(0, slashIndex);
                attName = name.substring(slashIndex + 1);

                try {
                    ObjectName objectName = new ObjectName(queryableName);
                    String keyHolder = findKeyForValueToBeReplaced(objectName);

                    Set<ObjectName> objNames = mbs.queryNames(objectName, null);
                    for (ObjectName oName : objNames) {
                        String keyValue = oName.getKeyPropertyList().get(keyHolder);
                        String newName = entry.getName();
                        if (!newName.contains(PLACEHOLDER)) {
                            LOGGER.log(Level.WARNING, "Name [{0}] did not contain a %s, no replacement will be done, check the configuration", newName);
                        }
                        newName = newName.replace(PLACEHOLDER, keyValue);
                        String newDisplayName = entry.getDisplayName().replace(PLACEHOLDER, keyValue);
                        String newDescription = entry.getDescription().replace(PLACEHOLDER, keyValue);
                        JMXBeanMetadata newEntry = new JMXBeanMetadata(newName, newDisplayName, newDescription,
                                entry.getTypeRaw(), entry.getUnit());
                        String newObjectName = oName.getCanonicalName() + "/" + attName;
                        newEntry.setMbean(newObjectName);
                        result.add(newEntry);
                    }
                    toBeRemoved.add(entry);
                } catch (MalformedObjectNameException e) {
                    e.printStackTrace(); 
                }
            }
        }
        entries.removeAll(toBeRemoved);
        entries.addAll(result);
    }

    private String findKeyForValueToBeReplaced(ObjectName objectName) {
        for (Entry<String, String> entry : objectName.getKeyPropertyList().entrySet()) {
            if (entry.getValue().equals("*")) {
                return entry.getKey();
            }
        }
        return null;
    }
}
