/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.deployment.node.runtime.gf;

import com.sun.enterprise.deployment.node.XMLElement;
import com.sun.enterprise.deployment.node.runtime.RuntimeDescriptorNode;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;
import org.glassfish.web.deployment.runtime.ConstraintField;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;

import java.util.Map;

/**
* node for cache-mapping tag
*
* @author Jerome Dochez
*/
public class ConstraintFieldNode extends RuntimeDescriptorNode<ConstraintField> {

    protected ConstraintField descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public ConstraintField getDescriptor() {
        if (descriptor==null) {
            descriptor = new ConstraintField();
        }
        return descriptor;
    }

    /**
     * all sub-implementation of this class can use a dispatch table to map xml element to
     * method name on the descriptor class for setting the element value. 
     *  
     * @return the map with the element name as a key, the setter method as a value
     */
    @Override
    protected Map<String, String> getDispatchTable() {    
	Map<String, String> dispatchTable = super.getDispatchTable();
        // for backward compatibility with S1AS 7 dtd 
	dispatchTable.put(RuntimeTagNames.VALUE, "addValue");
        dispatchTable.put(RuntimeTagNames.CONSTRAINT_FIELD_VALUE, "addValue");
	return dispatchTable;
    }

    @Override
    public void startElement(XMLElement element, Attributes attributes) {
        if (element.getQName().equals(RuntimeTagNames.CONSTRAINT_FIELD)) {
            ConstraintField descriptor = getDescriptor();
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.NAME.equals(attributes.getQName(i))) {
                    descriptor.setAttributeValue(ConstraintField.NAME,
                        attributes.getValue(i));
                } else 
                if (RuntimeTagNames.SCOPE.equals(attributes.getQName(i))) {
                    descriptor.setAttributeValue(ConstraintField.SCOPE,
                        attributes.getValue(i));
                } else 
                if (RuntimeTagNames.CACHE_ON_MATCH.equals(
                    attributes.getQName(i))) {
                    descriptor.setAttributeValue(
                        ConstraintField.CACHE_ON_MATCH,
                        attributes.getValue(i));
                } else 
                if (RuntimeTagNames.CACHE_ON_MATCH_FAILURE.equals(
                    attributes.getQName(i))) {
                    descriptor.setAttributeValue(
                        ConstraintField.CACHE_ON_MATCH_FAILURE,
                        attributes.getValue(i));
                } 
            }
        // From sun-web-app_2_3-0.dtd to sun-web-app_2_4-0.dtd,
        // the element name "value" is changed to "constraint-field-value",
        // need to make sure both will work
        } else if (element.getQName().equals(RuntimeTagNames.VALUE) || 
            element.getQName().equals(RuntimeTagNames.CONSTRAINT_FIELD_VALUE)) {
            ConstraintField descriptor = getDescriptor();
            int index = descriptor.sizeValue();
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.MATCH_EXPR.equals(
                    attributes.getQName(i))) {
                    descriptor.setAttributeValue(ConstraintField.VALUE, 
                        index, ConstraintField.MATCH_EXPR,
                        attributes.getValue(i));
                } else 
                if (RuntimeTagNames.CACHE_ON_MATCH.equals(
                    attributes.getQName(i))) {
                    descriptor.setAttributeValue(ConstraintField.VALUE, 
                        index, ConstraintField.CACHE_ON_MATCH,
                        attributes.getValue(i));
                } else 
                if (RuntimeTagNames.CACHE_ON_MATCH_FAILURE.equals(
                    attributes.getQName(i))) {
                    descriptor.setAttributeValue(ConstraintField.VALUE, 
                        index, ConstraintField.CACHE_ON_MATCH_FAILURE,
                        attributes.getValue(i));
                } 
            }
        } else super.startElement(element, attributes);
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name 
     * @param the array of descriptor to write
     * @return the DOM tree top node
     */    
    public void writeDescriptor(Node parent, String nodeName, ConstraintField[] descriptors) {
	for (int i=0;i<descriptors.length;i++) {
	    writeDescriptor(parent, nodeName, descriptors[i]);
	}
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param nodeName node name
     * @param descriptor the descriptor to write
     * @return the DOM tree top node
     */
    @Override
    public Node writeDescriptor(Node parent, String nodeName, ConstraintField descriptor) {
	
	Element constraintField = (Element) super.writeDescriptor(parent, nodeName, descriptor);
	
	// value*
	String[] values = descriptor.getValue();
	for (int i=0;i<values.length;i++) {
	    Element value = (Element) appendTextChild(constraintField, RuntimeTagNames.CONSTRAINT_FIELD_VALUE, values[i]);
	    setAttribute(value, RuntimeTagNames.MATCH_EXPR, (String) descriptor.getAttributeValue(ConstraintField.VALUE, i, ConstraintField.MATCH_EXPR));
	    setAttribute(value, RuntimeTagNames.CACHE_ON_MATCH, (String) descriptor.getAttributeValue(ConstraintField.VALUE, i, ConstraintField.CACHE_ON_MATCH));
	    setAttribute(value, RuntimeTagNames.CACHE_ON_MATCH_FAILURE, (String) descriptor.getAttributeValue(ConstraintField.VALUE, i, ConstraintField.CACHE_ON_MATCH_FAILURE));
	    
	}
	// name, scope, cache-on-match, cache-on-match-failure attributes        
	setAttribute(constraintField, RuntimeTagNames.NAME, (String) descriptor.getAttributeValue(ConstraintField.NAME));
	setAttribute(constraintField, RuntimeTagNames.SCOPE, (String) descriptor.getAttributeValue(ConstraintField.SCOPE));        
	setAttribute(constraintField, RuntimeTagNames.CACHE_ON_MATCH, (String) descriptor.getAttributeValue(ConstraintField.CACHE_ON_MATCH));
	setAttribute(constraintField, RuntimeTagNames.CACHE_ON_MATCH_FAILURE, (String) descriptor.getAttributeValue(ConstraintField.CACHE_ON_MATCH_FAILURE));
	
	return constraintField;	
    }
}
