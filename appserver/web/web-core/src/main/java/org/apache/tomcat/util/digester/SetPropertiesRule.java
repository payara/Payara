/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

import org.apache.catalina.LogFacade;
import org.glassfish.web.util.IntrospectionUtils;
import org.xml.sax.Attributes;

import java.util.logging.Level;


/**
 * <p>Rule implementation that sets properties on the object at the top of the
 * stack, based on attributes with corresponding names.</p>
 *
 * <p>This rule supports custom mapping of attribute names to property names.
 * The default mapping for particular attributes can be overridden by using 
 * {@link #SetPropertiesRule(String[] attributeNames, String[] propertyNames)}.
 * This allows attributes to be mapped to properties with different names.
 * Certain attributes can also be marked to be ignored.</p>
 */

public class SetPropertiesRule extends Rule {

    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor sets only the the associated Digester.
     *
     * @param digester The digester with which this rule is associated
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #SetPropertiesRule()} instead.
     */
    public SetPropertiesRule(Digester digester) {

        this();

    }
    

    /**
     * Base constructor.
     */
    public SetPropertiesRule() {

        // nothing to set up 

    }
    
    /** 
     * <p>Convenience constructor overrides the mapping for just one property.</p>
     *
     * <p>For details about how this works, see
     * {@link #SetPropertiesRule(String[] attributeNames, String[] propertyNames)}.</p>
     *
     * @param attributeName map this attribute 
     * @param propertyName to a property with this name
     */
    public SetPropertiesRule(String attributeName, String propertyName) {
        
        attributeNames = new String[1];
        attributeNames[0] = attributeName;
        propertyNames = new String[1];
        propertyNames[0] = propertyName;
    }
    
    /** 
     * <p>Constructor allows attribute->property mapping to be overriden.</p>
     *
     * <p>Two arrays are passed in. 
     * One contains the attribute names and the other the property names.
     * The attribute name / property name pairs are match by position
     * In order words, the first string in the attribute name list matches
     * to the first string in the property name list and so on.</p>
     *
     * <p>If a property name is null or the attribute name has no matching
     * property name, then this indicates that the attibute should be ignored.</p>
     * 
     * <h5>Example One</h5>
     * <p> The following constructs a rule that maps the <code>alt-city</code>
     * attribute to the <code>city</code> property and the <code>alt-state</code>
     * to the <code>state</code> property. 
     * All other attributes are mapped as usual using exact name matching.
     * <code><pre>
     *      SetPropertiesRule(
     *                new String[] {"alt-city", "alt-state"}, 
     *                new String[] {"city", "state"});
     * </pre></code>
     *
     * <h5>Example Two</h5>
     * <p> The following constructs a rule that maps the <code>class</code>
     * attribute to the <code>className</code> property.
     * The attribute <code>ignore-me</code> is not mapped.
     * All other attributes are mapped as usual using exact name matching.
     * <code><pre>
     *      SetPropertiesRule(
     *                new String[] {"class", "ignore-me"}, 
     *                new String[] {"className"});
     * </pre></code>
     *
     * @param attributeNames names of attributes to map
     * @param propertyNames names of properties mapped to
     */
    public SetPropertiesRule(String[] attributeNames, String[] propertyNames) {
        // create local copies
        this.attributeNames = new String[attributeNames.length];
        for (int i=0, size=attributeNames.length; i<size; i++) {
            this.attributeNames[i] = attributeNames[i];
        }
        
        this.propertyNames = new String[propertyNames.length];
        for (int i=0, size=propertyNames.length; i<size; i++) {
            this.propertyNames[i] = propertyNames[i];
        } 
    }
        
    // ----------------------------------------------------- Instance Variables
    
    /** 
     * Attribute names used to override natural attribute->property mapping
     */
    private String [] attributeNames;
    /** 
     * Property names used to override natural attribute->property mapping
     */    
    private String [] propertyNames;


    // --------------------------------------------------------- Public Methods


    /**
     * Process the beginning of this element.
     *
     * @param attributes The attribute list of this element
     */
    public void begin(Attributes attributes) throws Exception {
        
        // Populate the corresponding properties of the top object
        Object top = digester.peek();
        if (digester.log.isLoggable(Level.FINE)) {
            if (top != null) {
                digester.log.log(Level.FINE, "[SetPropertiesRule]{" + digester.match +
                        "} Set " + top.getClass().getName() +
                        " properties");
            } else {
                digester.log.log(Level.FINE, "[SetPropertiesRule]{" + digester.match +
                        "} Set NULL properties");
            }
        }
        
        // set up variables for custom names mappings
        int attNamesLength = 0;
        if (attributeNames != null) {
            attNamesLength = attributeNames.length;
        }
        int propNamesLength = 0;
        if (propertyNames != null) {
            propNamesLength = propertyNames.length;
        }
        
        for (int i = 0; i < attributes.getLength(); i++) {
            String name = attributes.getLocalName(i);
            if ("".equals(name)) {
                name = attributes.getQName(i);
            }
            String value = attributes.getValue(i);
            
            // we'll now check for custom mappings
            for (int n = 0; n<attNamesLength; n++) {
                if (name.equals(attributeNames[n])) {
                    if (n < propNamesLength) {
                        // set this to value from list
                        name = propertyNames[n];
                    
                    } else {
                        // set name to null
                        // we'll check for this later
                        name = null;
                    }
                    break;
                }
            } 
            
            if (digester.log.isLoggable(Level.FINE)) {
                digester.log.log(Level.FINE, "[SetPropertiesRule]{" + digester.match +
                        "} Setting property '" + name + "' to '" +
                        value + "'");
            }
            if (!digester.isFakeAttribute(top, name) 
                    && !IntrospectionUtils.setProperty(top, name, value) 
                    && digester.getRulesValidation()) {
                digester.log.log(Level.WARNING, LogFacade.PROPERTIES_RULE_NOT_FIND_MATCHING_PROPERTY,
                                 new Object[] {digester.match, name, value});
            }
        }

    }


    /**
     * <p>Add an additional attribute name to property name mapping.
     * This is intended to be used from the xml rules.
     */
    public void addAlias(String attributeName, String propertyName) {
        
        // this is a bit tricky.
        // we'll need to resize the array.
        // probably should be synchronized but digester's not thread safe anyway
        if (attributeNames == null) {
            
            attributeNames = new String[1];
            attributeNames[0] = attributeName;
            propertyNames = new String[1];
            propertyNames[0] = propertyName;        
            
        } else {
            int length = attributeNames.length;
            String [] tempAttributes = new String[length + 1];
            for (int i=0; i<length; i++) {
                tempAttributes[i] = attributeNames[i];
            }
            tempAttributes[length] = attributeName;
            
            String [] tempProperties = new String[length + 1];
            for (int i=0; i<length && i< propertyNames.length; i++) {
                tempProperties[i] = propertyNames[i];
            }
            tempProperties[length] = propertyName;
            
            propertyNames = tempProperties;
            attributeNames = tempAttributes;
        }        
    }
  

    /**
     * Render a printable version of this Rule.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("SetPropertiesRule[");
        sb.append("]");
        return (sb.toString());

    }


}
