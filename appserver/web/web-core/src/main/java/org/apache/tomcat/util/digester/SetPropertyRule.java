/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.digester;


import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.web.util.IntrospectionUtils;
import org.xml.sax.Attributes;

import java.util.logging.Level;


/**
 * Rule implementation that sets an individual property on the object at the
 * top of the stack, based on attributes with specified names.
 */

public class SetPropertyRule extends Rule {

    @LogMessageInfo(
            message = "[SetPropertyRule]{0} Setting property {1} to {2} did not find a matching property.",
            level = "WARNING"
    )
    public static final String PROPERTY_RULE_NOT_FIND_MATCHING_PROPERTY = "AS-WEB-CORE-00534";


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a "set property" rule with the specified name and value
     * attributes.
     *
     * @param digester The digester with which this rule is associated
     * @param name Name of the attribute that will contain the name of the
     *  property to be set
     * @param value Name of the attribute that will contain the value to which
     *  the property should be set
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #SetPropertyRule(String name, String value)} instead.
     */
    public SetPropertyRule(Digester digester, String name, String value) {

        this(name, value);

    }

    /**
     * Construct a "set property" rule with the specified name and value
     * attributes.
     *
     * @param name Name of the attribute that will contain the name of the
     *  property to be set
     * @param value Name of the attribute that will contain the value to which
     *  the property should be set
     */
    public SetPropertyRule(String name, String value) {

        this.name = name;
        this.value = value;

    }

    // ----------------------------------------------------- Instance Variables


    /**
     * The attribute that will contain the property name.
     */
    protected String name = null;


    /**
     * The attribute that will contain the property value.
     */
    protected String value = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Process the beginning of this element.
     *
     * @param attributes The attribute list of this element
     *
     * @exception NoSuchMethodException if the bean does not
     *  have a writeable property of the specified name
     */
    public void begin(Attributes attributes) throws Exception {

        // Identify the actual property name and value to be used
        String actualName = null;
        String actualValue = null;
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            if ("".equals(name)) {
                name = attributes.getQName(i);
            }
            String value = attributes.getValue(i);
            if (name.equals(this.name)) {
                actualName = value;
            } else if (name.equals(this.value)) {
                actualValue = value;
            }
        }

        // Get a reference to the top object
        Object top = digester.peek();

        // Log some debugging information
        if (digester.log.isLoggable(Level.FINE)) {
            digester.log.log(Level.FINE, "[SetPropertyRule]{" + digester.match +
                    "} Set " + top.getClass().getName() + " property " +
                    actualName + " to " + actualValue);
        }

        // Set the property (with conversion as necessary)
        if (!digester.isFakeAttribute(top, actualName) 
                && !IntrospectionUtils.setProperty(top, actualName, actualValue) 
                && digester.getRulesValidation()) {
            digester.log.log(Level.WARNING, PROPERTY_RULE_NOT_FIND_MATCHING_PROPERTY,
                             new Object[] {digester.match, name, value});
        }

    }


    /**
     * Render a printable version of this Rule.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SetPropertyRule[");
        sb.append("name=");
        sb.append(name);
        sb.append(", value=");
        sb.append(value);
        sb.append("]");
        return (sb.toString());

    }


}
