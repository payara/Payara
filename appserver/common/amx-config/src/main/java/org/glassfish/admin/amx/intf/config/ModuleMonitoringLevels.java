/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.amx.intf.config;

import org.glassfish.admin.amx.base.Singleton;
import org.glassfish.admin.amx.util.SetUtil;

import javax.management.Attribute;
import javax.management.AttributeList;
import java.util.Map;
import java.util.Set;

import static org.glassfish.external.amx.AMX.*;

@Deprecated
public interface ModuleMonitoringLevels
        extends Singleton, ConfigElement, PropertiesAccess {
    /**
     * Value indicating the maximum level of monitoring is enabled.
     */
    public final static String HIGH = "HIGH";
    /**
     * Value indicating some level of monitoring is enabled.s
     */
    public final static String LOW = "LOW";
    /**
     * Value indicating that monitoring is disabled.
     */
    public final static String OFF = "OFF";


    public String getThreadPool();

    public String getHttpService();

    public String getWebContainer();

    public String getEjbContainer();

    public String getJmsService();

    public String getTransactionService();

    public String getConnectorService();

    public String getOrb();

    public void setEjbContainer(String param1);

    public void setWebContainer(String param1);

    public String getSecurity();

    public void setSecurity(String param1);

    public String getDeployment();

    public void setDeployment(String param1);

    public String getJvm();

    public void setJvm(String param1);

    public void setConnectorService(String param1);

    public void setJmsService(String param1);

    public String getConnectorConnectionPool();

    public void setConnectorConnectionPool(String param1);

    public void setHttpService(String param1);

    public String getJdbcConnectionPool();

    public void setJdbcConnectionPool(String param1);

    public void setOrb(String param1);

    public void setThreadPool(String param1);

    public void setTransactionService(String param1);

    public String getWebServicesContainer();

    public void setWebServicesContainer(String param1);

    public String getJpa();

    public void setJpa(String param1);

    public String getJersey();

    public void setJersey(String value);

    public final class Helper {
        private Helper() {
        }

        /**
         * set all monitoring levels to the specified one.
         * Return a Map keyed by attribute name of the previous values that changed
         */
        public static AttributeList setAllMonitoringLevel(
                final ModuleMonitoringLevels levels,
                final String newLevel) {
            final Set<String> excluded =
                    SetUtil.newUnmodifiableStringSet(ATTR_NAME, ATTR_PARENT, ATTR_CHILDREN, "Property");

            final Map<String, Object> attrs = levels.attributesMap();
            final AttributeList attributeList = new AttributeList();
            final AttributeList originalValues = new AttributeList();
            for (final String attrName : attrs.keySet()) {
                final Object originalValue = attrs.get(attrName);
                if (excluded.contains(attrName) || originalValue == null || !(originalValue instanceof String)) {
                    continue;
                }

                final String strValue = "" + originalValue;
                if (!strValue.equals(newLevel)) {
                    attributeList.add(new Attribute(attrName, newLevel));
                    originalValues.add(new Attribute(attrName, originalValue));
                }
            }
            if (attributeList.size() != 0) {
                try {
                    levels.extra().setAttributes(attributeList);
                }
                catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return originalValues;
        }
    }
}



