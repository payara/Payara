/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.util.List;

/**
 * Base interface for those configuration objects that has nested &lt;system-property> elements.
 * <p>
 * <b>Important: document legal properties using PropertiesDesc, one PropertyDesc
 * for each legal system-property</b>.
 */
public interface SystemPropertyBag extends ConfigBeanProxy {
    /**
     * Gets the list of system-property.
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSystemProperty().add(newItem);
     * </pre>
     * Objects of the following type(s) are allowed in the list
     * {@link SystemProperty }
     */
    @Element("system-property")
    List<SystemProperty> getSystemProperty();

    @DuckTyped
    SystemProperty getSystemProperty(String name);

    /**
     * Returns a property value if the bean has system properties and one of its
     * system-property names is equal to the one passed.
     *
     * @param name the system property name requested
     * @return the property value or null if not found
     */
    @DuckTyped
    String getSystemPropertyValue(String name);

    /**
     * Returns a property value if the bean has properties and one of its
     * properties name is equal to the one passed. Otherwise return
     * the default value.
     *
     * @param name the property name requested
     */
    @DuckTyped
    String getPropertyValue(String name, String defaultValue);

    @DuckTyped
    boolean containsProperty(String name);

    class Duck {
        public static SystemProperty getSystemProperty(final SystemPropertyBag me, final String name) {
            for (final SystemProperty prop : me.getSystemProperty()) {
                if (prop.getName().equals(name)) {
                    return prop;
                }
            }
            return null;
        }

        public static String getSystemPropertyValue(final SystemPropertyBag me, final String name) {
            return getSystemPropertyValue(me,name,null);
        }

        public static String getSystemPropertyValue(final SystemPropertyBag me, final String name, final String defaultValue) {
            final SystemProperty prop = getSystemProperty(me, name);
            if (prop != null) {
                return prop.getValue();
            }
            return defaultValue;
        }

        public static boolean containsProperty(SystemPropertyBag me, String name) {
            return me.getSystemProperty(name) != null;    
        }
    }
}
