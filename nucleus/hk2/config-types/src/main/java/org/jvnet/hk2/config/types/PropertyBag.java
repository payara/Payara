/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.types;

import org.glassfish.hk2.api.Customize;
import org.glassfish.hk2.api.Customizer;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.DuckTyped;

import java.util.List;

import javax.xml.bind.annotation.XmlElement;

/**
 * Base interface for those configuration objects that has nested &lt;property> elements.
 * @author Kohsuke Kawaguchi
 */
@Customizer(PropertyBagCustomizer.class)
public interface PropertyBag {
    /**
     * Gets the value of the property property.
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link Property }
     * @return the property list
     */
    @XmlElement(name="property")
    @Element("property")
    List<Property> getProperty();
    Property addProperty(Property property);
    Property lookupProperty(String name);
    Property removeProperty(String name);
    Property removeProperty(Property removeMe);

    @Customize
    @DuckTyped
    Property getProperty(String name);

    /**
     * Returns a property value if the bean has properties and one of its
     * properties name is equal to the one passed.
     *
     * @param name the property name requested
     * @return the property value or null if not found
     */
    @Customize
    @DuckTyped
    String getPropertyValue(String name);

    /**
     * Returns a property value if the bean has properties and one of its
     * properties name is equal to the one passed. Otherwise return
     * the default value.
     *
     * @param name the property name requested
     * @param defaultValue is the default value to return in case the property
     * of that name does not exist in this bag
     * @return the property value
     */
    @Customize
    @DuckTyped
    String getPropertyValue(String name, String defaultValue);

    public class Duck {
        public static Property getProperty(PropertyBag me, String name) {
            for (Property prop : me.getProperty()) {
                if (prop.getName().equals(name)) {
                    return prop;
                }
            }
            return null;
        }

        public static String getPropertyValue(PropertyBag me, String name) {
            return getPropertyValue(me,name,null);
        }

        public static String getPropertyValue(PropertyBag me, String name, String defaultValue) {
            Property prop = getProperty(me, name);
            if (prop != null) {
                return prop.getValue();
            }
            return defaultValue;
        }
    }
}
