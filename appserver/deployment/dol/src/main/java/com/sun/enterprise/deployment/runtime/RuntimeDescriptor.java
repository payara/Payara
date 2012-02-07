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

package com.sun.enterprise.deployment.runtime;

import org.glassfish.deployment.common.Descriptor;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This base class defines common behaviour and data for all runtime
 * descriptors.
 *
 * @author Jerome Dochez
 */
public abstract class RuntimeDescriptor extends Descriptor {
    
    protected PropertyChangeSupport propListeners;
    
    /** Creates a new instance of RuntimeDescriptor */
    public RuntimeDescriptor(RuntimeDescriptor other) {
	super(other);
	propListeners = new PropertyChangeSupport(this); // not copied  
    }

    /** Creates a new instance of RuntimeDescriptor */
    public RuntimeDescriptor() {
	propListeners = new PropertyChangeSupport(this);    
    }
    
    /** 
     * Add a property listener for this bean
     * @param the property listener
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        propListeners.addPropertyChangeListener(l);
    }
    
    /**
     * removes a property listener for this bean
     * @param the property listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener l) {
        propListeners.removePropertyChangeListener(l);
    }
    
    /**
     * Add a property listener for a specific property name
     * @param the property name
     * @param the property listener
     */
    public void addPropertyChangeListener(String n, PropertyChangeListener l) {
        propListeners.addPropertyChangeListener(n, l);
    }
    
    /**
     * Remover a property listener for specific property name 
     * @param the property name
     * @param the property listener
     */
    public void removePropertyChangeListener(String n, PropertyChangeListener l) {
        propListeners.removePropertyChangeListener(n, l);
    }
    
    /**
     * Sets a property value
     * @param the property name
     * @param the property value
     */
    public void setValue(String name, Object value) {
        Object oldValue = getExtraAttribute(name);
        addExtraAttribute(name, value);
        propListeners.firePropertyChange(name, oldValue, value);
    }
    
    /**
     * @return a property value
     */
    public Object getValue(String name) { 
        return getExtraAttribute(name);
    }
    
    /**
     * indexed property support
     */
    protected void setValue(String name, int index, Object value) {
	List list = getIndexedProperty(name);
	list.set(index, value);
	setValue(name, list);
    }
    
    protected Object getValue(String name, int index) {
	List list = getIndexedProperty(name);
	return list.get(index);	
    }
    
    protected int addValue(String name, Object value) {
	List list = getIndexedProperty(name);
	list.add(value);
	setValue(name, list);
	return list.indexOf(value);
    }
    
    protected int removeValue(String name, Object value) {
	List list = getIndexedProperty(name);
	int index = list.indexOf(value);
	list.remove(index);
	return index;
    }
    
    protected void removeValue(String name, int index) {
	List list = getIndexedProperty(name);
	list.remove(index);
    }
    
    protected void setValues(String name, Object[] values) {
	List list = getIndexedProperty(name);
	for (int i=0;i<values.length;) {
	    list.add(values[i]);
	}
    }
    
    protected Object[] getValues(String name) {
	List list = (List) getValue(name);
	if (list!=null && list.size()>0) {
	    Class c = list.get(0).getClass();
	    Object array = java.lang.reflect.Array.newInstance(c, list.size());
	    return list.toArray((Object[]) array);
	}
	else 
	    return null;
    }
    
    protected int size(String name) {
	List list = (List) getValue(name);
	if (list!=null) 
	    return list.size();
	else 
	    return 0;
    }
    
    private List getIndexedProperty(String name) {
	Object o = getValue(name);
	if (o==null) {
	    return new ArrayList();
	} else {
	    return (List) o;
	}
    }
    
    // xml attributes support
    public void setAttributeValue(String elementName, String attributeName, Object value) {
	// here we have to play games...
	// the nodes cannot know if the property scalar is 0,1 or n 
	// so we look if the key name is already used (means property* 
	// DTD langua) and find the last one entered
	
	
	int index = 0;
	while (getValue(elementName + "-" + index + "-" + attributeName)!=null) {
	    index++;
	}

        setValue(elementName + "-" + index + "-" + attributeName, value);
    }
    
    public String getAttributeValue(String elementName, String attributeName) {
	return getAttributeValue(elementName,0, attributeName);
    }
    
    // attribute stored at the descriptor level are treated like elements
    public void setAttributeValue(String attributeName, String value) {
	setValue(attributeName, value);
    }
    
    public String getAttributeValue(String attributeName) {
	return (String) getValue(attributeName);
    }
    
    // indexed xml attributes support
    public void setAttributeValue(String elementName, int index, String attributeName, Object value) {
	setValue(elementName + "-" + index + "-" + attributeName, value);
    }
    
    public String getAttributeValue(String elementName, int index, String attributeName) {
	return (String) getValue(elementName + "-" + index + "-" + attributeName);
    }    
}
