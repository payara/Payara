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

package org.glassfish.web.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;

/**
 * Represents the env-entry web application configuration customization.
 * 
 * @author tjquinn
 */
@Configured
public interface EnvEntry extends ConfigBeanProxy {

    @Element
    public String getDescription();
    public void setDescription(String value);
    
    @Element(required=true,key=true)
    public String getEnvEntryName();
    public void setEnvEntryName(String value);
    
    @Element(required=true)
    public String getEnvEntryType();
    public void setEnvEntryType(String value);
    
    @Element(required=true)
    public String getEnvEntryValue();
    public void setEnvEntryValue(String value);


    @Attribute(dataType=Boolean.class, defaultValue="false")
    public String getIgnoreDescriptorItem();
    public void setIgnoreDescriptorItem(String value);

    /**
     * Validates the value in the env-entry-value subelement against the
     * type stored in the env-entry-type subelement.
     * <p>
     * @throws IllegalArgumentException if the type does not match one of the legal ones
     * @throws NumberFormatException if the value cannot be parsed according to the type
     */
    @DuckTyped
    public void validateValue();

    public class Duck {
        public static void validateValue(final EnvEntry me) {
            String type = me.getEnvEntryType();
            String value = me.getEnvEntryValue();
            Util.validateValue(type, value);
        }
    }

    /**
     * Utility class.
     */
    public class Util {

        /**
         * Validates the specified value string against the indicated type.  The
         * recognized types are (from the spec):
         * <ul>
         * <li>java.lang.Boolean
         * <li>java.lang.Byte
         * <li>java.lang.Character
         * <li>java.lang.Double
         * <li>java.lang.Float
         * <li>java.lang.Integer
         * <li>java.lang.Long
         * <li>java.lang.Short
         * <li>java.lang.String
         * </ul>
         *
         * @param type valid type for env-entry-type (from the spec)
         * @param value value to be checked
         * @throws IllegalArgumentException if the type does not match one of the legal ones
         * @throws NumberFormatException if the value cannot be parsed according to the type
         */
        public static void validateValue(final String type, final String value) {
            if (type == null) {
                throw new IllegalArgumentException("type");
            }
            if (value == null) {
                throw new IllegalArgumentException("value");
            }
            if (type.equals("java.lang.Boolean")) {
                Boolean.parseBoolean(value);
            } else if (type.equals("java.lang.Byte")) {
                Byte.parseByte(value);
            } else if (type.equals("java.lang.Character")) {
                if (value.length() > 1) {
                    throw new IllegalArgumentException("length(\"" + value + "\") > 1");
                }
            } else if (type.equals("java.lang.Double")) {
                Double.parseDouble(value);
            } else if (type.equals("java.lang.Float")) {
                Float.parseFloat(value);
            } else if (type.equals("java.lang.Integer")) {
                Integer.parseInt(value);
            } else if (type.equals("java.lang.Long")) {
                Long.parseLong(value);
            } else if (type.equals("java.lang.Short")) {
                Short.parseShort(value);
            } else if (type.equals("java.lang.String")) {
                // no-op
            } else {
                throw new IllegalArgumentException(type);
            }
        }
    }
}
