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
import org.glassfish.web.deployment.runtime.CacheMapping;
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
public class CacheMappingNode extends RuntimeDescriptorNode<CacheMapping> {
    
    public CacheMappingNode() {
	
        registerElementHandler(new XMLElement(RuntimeTagNames.CONSTRAINT_FIELD), 
                               ConstraintFieldNode.class, "addNewConstraintField"); 			       
    }

    protected CacheMapping descriptor = null;

    /**
     * @return the descriptor instance to associate with this XMLNode
     */
    @Override
    public CacheMapping getDescriptor() {
        if (descriptor==null) {
            descriptor = new CacheMapping();
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
	dispatchTable.put(RuntimeTagNames.SERVLET_NAME, "setServletName");
	dispatchTable.put(RuntimeTagNames.URL_PATTERN, "setURLPattern");
	dispatchTable.put(RuntimeTagNames.CACHE_HELPER_REF, "setCacheHelperRef");	
	dispatchTable.put(RuntimeTagNames.TIMEOUT, "setTimeout");
	dispatchTable.put(RuntimeTagNames.HTTP_METHOD, "addNewHttpMethod");	
	dispatchTable.put(RuntimeTagNames.DISPATCHER, "addNewDispatcher");	
	return dispatchTable;
    }

    public void startElement(XMLElement element, Attributes attributes) {
        CacheMapping descriptor = getDescriptor();
	if (element.getQName().equals(RuntimeTagNames.TIMEOUT)) {
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.NAME.equals(attributes.getQName(i))) {
		    descriptor.setAttributeValue(CacheMapping.TIMEOUT, CacheMapping.NAME, attributes.getValue(i));
                } else 
                if (RuntimeTagNames.SCOPE.equals(attributes.getQName(i))) {
                    int index=0;
                    while (descriptor.getAttributeValue(CacheMapping.TIMEOUT, index, CacheMapping.NAME)!=null) {
                        index++;
                    }
		    descriptor.setAttributeValue(CacheMapping.TIMEOUT, index-1, CacheMapping.SCOPE, attributes.getValue(i));
	        }  
            }
 	} else 
	if (element.getQName().equals(RuntimeTagNames.REFRESH_FIELD)) {
	    descriptor.setRefreshField(true);
            for (int i=0; i<attributes.getLength();i++) {
	        if (RuntimeTagNames.NAME.equals(attributes.getQName(i))) {
		    descriptor.setAttributeValue(CacheMapping.REFRESH_FIELD, 0, CacheMapping.NAME, attributes.getValue(i));
	        } else
	        if (RuntimeTagNames.SCOPE.equals(attributes.getQName(i))) { 
                    descriptor.setAttributeValue(CacheMapping.REFRESH_FIELD, 0, CacheMapping.SCOPE, attributes.getValue(i));
                }
	    }  
	} else
	if (element.getQName().equals(RuntimeTagNames.KEY_FIELD)) {
	    descriptor.addKeyField(true);
            for (int i=0; i<attributes.getLength();i++) {
                if (RuntimeTagNames.NAME.equals(attributes.getQName(i))) {
		    descriptor.setAttributeValue(CacheMapping.KEY_FIELD, CacheMapping.NAME, attributes.getValue(i));
	        } else
                if (RuntimeTagNames.SCOPE.equals(attributes.getQName(i))) {
                    int index = descriptor.sizeKeyField();               
	            descriptor.setAttributeValue(CacheMapping.KEY_FIELD, index-1, CacheMapping.SCOPE, attributes.getValue(i));
	        } 
            }
	} else super.startElement(element, attributes);
    }
    
    /**
     * write the descriptor class to a DOM tree and return it
     *
     * @param parent node for the DOM tree
     * @param node name 
     * @param the descriptor to write
     * @return the DOM tree top node
     */    
    public Node writeDescriptor(Node parent, String nodeName, CacheMapping descriptor) { 
	Node cacheMapping = super.writeDescriptor(parent, nodeName, descriptor);
	if (descriptor.getServletName()!=null) {
	    appendTextChild(cacheMapping, RuntimeTagNames.SERVLET_NAME, descriptor.getServletName());
	} else {
	    appendTextChild(cacheMapping, RuntimeTagNames.URL_PATTERN, descriptor.getURLPattern());
	}
	
	// cache-helper-ref 
	appendTextChild(cacheMapping, RuntimeTagNames.CACHE_HELPER_REF, 
			(String) descriptor.getValue(CacheMapping.CACHE_HELPER_REF));
	
	//dispatcher* 
	String[] dispatchers = descriptor.getDispatcher();
	if (dispatchers!=null) {
	    for (int i=0;i<dispatchers.length;i++) {
		appendTextChild(cacheMapping, RuntimeTagNames.DISPATCHER, dispatchers[i]);
	    }
	}

	// timeout?
	Element timeout = (Element) forceAppendTextChild(cacheMapping, RuntimeTagNames.TIMEOUT, 
			(String) descriptor.getValue(CacheMapping.TIMEOUT));
        // timeout attributes
        String name = descriptor.getAttributeValue(CacheMapping.TIMEOUT, CacheMapping.NAME);
        if (name!=null) {
            setAttribute(timeout, RuntimeTagNames.NAME, name);
        }
        String scope = descriptor.getAttributeValue(CacheMapping.TIMEOUT, CacheMapping.SCOPE);
        if (scope!=null) {
            setAttribute(timeout, RuntimeTagNames.SCOPE, scope);
        }
	
        //refresh-field?, 
	if (descriptor.isRefreshField()) {
	    Element refreshField = (Element) appendChild(cacheMapping, RuntimeTagNames.REFRESH_FIELD);
	    setAttribute(refreshField, RuntimeTagNames.NAME, 
			(String) descriptor.getAttributeValue(CacheMapping.REFRESH_FIELD, CacheMapping.NAME));
	    setAttribute(refreshField, RuntimeTagNames.SCOPE, 
			(String) descriptor.getAttributeValue(CacheMapping.REFRESH_FIELD, CacheMapping.SCOPE));
	}
	
	//http-method* 
	String[] httpMethods = descriptor.getHttpMethod();
	if (httpMethods!=null) {
	    for (int i=0;i<httpMethods.length;i++) {
		appendTextChild(cacheMapping, RuntimeTagNames.HTTP_METHOD, httpMethods[i]);
	    }
	}
	
	//key-field*
	if (descriptor.sizeKeyField()>0) {
	    for (int i=0;i<descriptor.sizeKeyField();i++) {
		
		if (descriptor.isKeyField(i)) {
		    Element keyField = (Element) appendChild(cacheMapping, RuntimeTagNames.KEY_FIELD);
		    setAttribute(keyField, RuntimeTagNames.NAME, 
			(String) descriptor.getAttributeValue(CacheMapping.KEY_FIELD, i, CacheMapping.NAME));
		    setAttribute(keyField, RuntimeTagNames.SCOPE, 
			(String) descriptor.getAttributeValue(CacheMapping.KEY_FIELD, i, CacheMapping.SCOPE));
		}
	    }
	}
	
	//constraint-field*
	if (descriptor.sizeConstraintField()>0) {
            ConstraintField[] constraintFields = descriptor.getConstraintField();
            ConstraintFieldNode cfn = new ConstraintFieldNode();
	    cfn.writeDescriptor(cacheMapping, RuntimeTagNames.CONSTRAINT_FIELD, constraintFields);
	}
	
	return cacheMapping;
    }	
}
