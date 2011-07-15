/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.runtime.jsr77;

import java.lang.reflect.Constructor;
import java.util.Iterator;
import javax.management.*;

/**
 * Dynamic MBean for managing web module
 */

public class WebModuleMB extends WebModuleMdl implements DynamicMBean {

    private String className = this.getClass().getName();
    private String description = "Web Module MBean";

    private MBeanAttributeInfo[] mbAttrInfoArr = new MBeanAttributeInfo[16];
    private MBeanConstructorInfo[] mbConstrInfoArr = new MBeanConstructorInfo[1];
    private MBeanOperationInfo[] mbOpInfoArr = new MBeanOperationInfo[3];
    private MBeanInfo mbInfo = null;

    public WebModuleMB(String name,boolean state, boolean statistics) {
        // FIXME
        super(name,state,statistics);
	buildMBeanInfo();
    }

    public Object getAttribute(String attribute_name) 
	throws AttributeNotFoundException,
	       MBeanException,
	       ReflectionException {

	// Check attribute_name is not null to avoid NullPointerException later on
	if (attribute_name == null) {
	    throw new RuntimeOperationsException(
                new IllegalArgumentException("Attribute name cannot be null"), 
		"Cannot invoke a getter of " + className + " with null attribute name");
	}

	// Check for a recognized attribute_name and call the corresponding getter
	if (attribute_name.equals("J2EEApplication")) {
	    return getJ2EEApplication();
	} 
	if (attribute_name.equals("J2EEServer")) {
	    return getJ2EEServer();
	} 
	if (attribute_name.equals("ModuleName")) {
	    return getModuleName();
	} 
	if (attribute_name.equals("deploymentDescriptor")) {
	    return getdeploymentDescriptor();
	} 
	if (attribute_name.equals("servlets")) {
	    return getservlets();
	} 
	if (attribute_name.equals("eventProvider")) {
	    return iseventProvider();
	} 
	if (attribute_name.equals("eventTypes")) {
	    return geteventTypes();
	} 
	if (attribute_name.equals("j2eeType")) {
	    return getj2eeType();
	} 
	if (attribute_name.equals("javaVMs")) {
	    return getjavaVMs();
	} 
	if (attribute_name.equals("name")) {
	    return getname();
	} 
	if (attribute_name.equals("objectName")) {
	    return getobjectName();
	} 
	if (attribute_name.equals("server")) {
	    return getserver();
	} 
	if (attribute_name.equals("startTime")) {
	    return getstartTime();
	} 
	if (attribute_name.equals("state")) {
	    return getstate();
	}
	if (attribute_name.equals("stateManageable")) {
	    return isstateManageable();
	}
	if (attribute_name.equals("statisticsProvider")) {
	    return isstatisticsProvider();
	}

	// If attribute_name has not been recognized throw an AttributeNotFoundException
	throw(new AttributeNotFoundException(
            "Cannot find " + attribute_name + " attribute in " + className));
    }

    /**
     * Sets the value of the specified attribute of the Dynamic MBean.
     */
    public void setAttribute(Attribute attribute) 
	throws AttributeNotFoundException,
	       InvalidAttributeValueException,
	       MBeanException, 
	       ReflectionException {

	// Check attribute is not null to avoid NullPointerException later on
	if (attribute == null) {
	    throw new RuntimeOperationsException(
            new IllegalArgumentException("Attribute cannot be null"), 
	    "Cannot invoke a setter of " + className + " with null attribute");
	}
	String name = attribute.getName();
	Object value = attribute.getValue();

	if (name == null) {
	    throw new RuntimeOperationsException(
                new IllegalArgumentException("Attribute name cannot be null"), 
		"Cannot invoke the setter of " + className + " with null attribute name");
	}

	// Check for a recognized attribute_name and call the corresponding getter
	if (name.equals("state")) {
	    setstate((java.lang.Integer)value);
	} else {
	    throw(new AttributeNotFoundException("Attribute " + name +
	        " not found in " + this.getClass().getName()));
	}
    }

    public AttributeList getAttributes(String[] attributeNames) {

	// Check attributeNames is not null to avoid NullPointerException later on
	if (attributeNames == null) {
	    throw new RuntimeOperationsException(
                new IllegalArgumentException("attributeNames[] cannot be null"),
	        "Cannot invoke a getter of " + className);
	}
	AttributeList resultList = new AttributeList();

	// if attributeNames is empty, return an empty result list
	if (attributeNames.length == 0)
	    return resultList;
        
	// build the result attribute list
	for (int i=0 ; i<attributeNames.length ; i++){
	    try {        
		Object value = getAttribute((String) attributeNames[i]);     
		resultList.add(new Attribute(attributeNames[i],value));
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	return resultList;
    }

    /**
     * Sets the values of several attributes of the Dynamic MBean, and returns the
     * list of attributes that have been set.
     */
    public AttributeList setAttributes(AttributeList attributes) {

	// Check attributes is not null to avoid NullPointerException later on
	if (attributes == null) {
	    throw new RuntimeOperationsException(
                new IllegalArgumentException("AttributeList attributes cannot be null"),
		"Cannot invoke a setter of " + className);
	}
	AttributeList resultList = new AttributeList();

	// if attributeNames is empty, nothing more to do
	if (attributes.isEmpty())
	    return resultList;

	// for each attribute, try to set it and add to the result list if successfull
	for (Iterator i = attributes.iterator(); i.hasNext();) {
	    Attribute attr = (Attribute) i.next();
	    try {
		setAttribute(attr);
		String name = attr.getName();
		Object value = getAttribute(name); 
		resultList.add(new Attribute(name,value));
	    } catch(Exception e) {
		e.printStackTrace();
	    }
	}
	return resultList;
    }

    public Object invoke(String operationName, Object params[], String signature[])
	throws MBeanException,
	       ReflectionException {

	// Check operationName is not null to avoid NullPointerException later on
	if (operationName == null) {
	    throw new RuntimeOperationsException(
                new IllegalArgumentException("Operation name cannot be null"), 
		"Cannot invoke a null operation in " + className);
	}
	// Check for a recognized operation name and call the corresponding operation
	if (operationName.equals("start")){
	    start();
	} else if (operationName.equals("startRecursive")){
	    start();
	} else if (operationName.equals("stop")){
	    stop();
	} else { 
	    // unrecognized operation name:
	    throw new ReflectionException(
                new NoSuchMethodException(operationName), 
		"Cannot find the operation " + operationName + " in " + className);
	}
        return null;
    }

    public MBeanInfo getMBeanInfo() {
	return mbInfo;
    }


    private void buildMBeanInfo() {

	Constructor[] constructors = this.getClass().getConstructors();
	mbConstrInfoArr[0] = new MBeanConstructorInfo(
            "constructor for web modul mBean",
	    constructors[0]);
        
	mbAttrInfoArr[0] = new MBeanAttributeInfo(
                "J2EEApplication", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[1] = new MBeanAttributeInfo(
                "J2EEServer", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[2] = new MBeanAttributeInfo(
                "ModuleName", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[3] = new MBeanAttributeInfo(
                "deploymentDescriptor", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[4] = new MBeanAttributeInfo(
                "servlets", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[5] = new MBeanAttributeInfo(
                "eventProvider", "java.lang.String", null,
		true, false, true);

	mbAttrInfoArr[6] = new MBeanAttributeInfo(
                "eventTypes", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[7] = new MBeanAttributeInfo(
                "j2eeType", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[8] = new MBeanAttributeInfo(
                "javaVMs", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[9] = new MBeanAttributeInfo(
                "name", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[10] = new MBeanAttributeInfo(
                "objectName", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[11] = new MBeanAttributeInfo(
                "server", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[12] = new MBeanAttributeInfo(
                "startTime", "java.lang.String", null,
		true, false, false);

	mbAttrInfoArr[13] = new MBeanAttributeInfo(
                "state", "java.lang.String", null,
		true, true, false);

	mbAttrInfoArr[14] = new MBeanAttributeInfo(
                "stateManageable", "java.lang.String", null,
		true, false, true);

	mbAttrInfoArr[15] = new MBeanAttributeInfo(
                "statisticsProvider", "java.lang.String", null,
		true, false, true);

	mbOpInfoArr[0] = new MBeanOperationInfo(
                "start", null, null, "void", MBeanOperationInfo.ACTION);
        
	mbOpInfoArr[1] = new MBeanOperationInfo(
                "startRecursive", null, null, "void", MBeanOperationInfo.ACTION);
        
	mbOpInfoArr[2] = new MBeanOperationInfo(
                "stop", null, null, "void", MBeanOperationInfo.ACTION);
        
	mbInfo = new MBeanInfo(className,
				description,
				mbAttrInfoArr,
				mbConstrInfoArr,
				mbOpInfoArr,
				new MBeanNotificationInfo[0]);
    }

}
