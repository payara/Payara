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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Superclass containing various tests for all field related tests in cmp2.0 
 * these tests should apply to cmp and cmr fields for entity classes
 *
 * @author  Jerome Dochez
 * @version 
 */
public abstract class CMPTest extends EjbTest {

    abstract protected Result check(EjbCMPEntityDescriptor descriptor);
    
    public Result check(EjbDescriptor descriptor) {
        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        if (descriptor instanceof EjbEntityDescriptor) {
	    String persistentType = 
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistentType)) {
                if (((EjbCMPEntityDescriptor) descriptor).getCMPVersion()<EjbCMPEntityDescriptor.CMP_2_x) {
                    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	            result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.notApplicable3",
				  "Test do not apply to this cmp-version of container managed persistence EJBs"));
        	    return result;                    
                } 
                return check((EjbCMPEntityDescriptor) descriptor);
            } else { // if (BEAN_PERSISTENCE.equals(persistentType))
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.notApplicable2",
				      "Test do not apply to EJB declared with persistence type [ {0} ]",
				      new Object[] {persistentType}));
    	        return result;            
	    } 
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.notApplicable1",
				  "Test do not apply to non entity EJB "));
	    return result;
	}                     
    }
      
    /**
     * check if a field has been declared in a class
     * 
     * @param fieldName the field name to look for declaration
     * @param c the class to look into
     * @param result where to place the test result
     */
    public static boolean isFieldAbstract(String fieldName, Class c, Result result)
    {        
        Class savedClass = c;
        boolean foundField = false;
        do {
	    try {                                            
	        Field f = c.getDeclaredField(fieldName);
                foundField = true;
	
                result.addErrorDetails(smh.getLocalString
		        ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isFieldDeclared.failed",
                         "Error: [ {0} ] field is declared in the class [ {1} ]",
			 new Object[] {fieldName, c.getName()}));
	    } catch (NoSuchFieldException e) {
            }
        } while (((c = c.getSuperclass()) != null) && (!foundField));
                       
        if (!foundField) {
	  
            result.addGoodDetails(smh.getLocalString
		("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isFieldDeclared.success",
                "[ {0} ] field is not declared in the class [ {1} ]",
		new Object[] {fieldName, savedClass.getName()})); 
            return true;
        } else return false;
    }
    
    /**
     * <p>
     * check if fields accessor methods conformant to Spec 2.0 paragraph 9.4.1 have
     * been declared in a class.
     * </p>
     * 
     * @param fieldName the field name to look accessor methods for
     * @param fieldType the field type, maybe null, then the return type of the 
     * get method is used
     * @param c the class to look for accessors in
     * @param result where to place the result 
     */
    public static boolean isAccessorDeclared(String fieldName, Class fieldType,  Class c, Result result ) 
    {  
        String getMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String setMethodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getMethod = getMethod(c, getMethodName, null);
        if (getMethod != null) {
            if (fieldType != null) {
                if (!fieldType.getName().equals(getMethod.getReturnType().getName())) {
		 
		    result.addErrorDetails(smh.getLocalString
		            ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed1",
			     "Error : [ {0} ] field accessor method has the wrong return type [ {1} ] ",
			     new Object[] {fieldName, getMethod.getReturnType().getName()}));         
		    return false;
                }
            }
	    // now look for the setmethod
            Class parms[] = { getMethod.getReturnType() };
            Method setMethod = getMethod(c, setMethodName, parms);
            if (setMethod != null) {
		if (setMethod.getReturnType().getName().equals("void")){ 
		  
                    result.addGoodDetails(smh.getLocalString
    		        ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.success",
			 "[ {0} ] field accessor methods exist and have the correct signaures",
			 new Object[] {fieldName}));                     
		    return true;
                } else { 
		  
		    result.addErrorDetails(smh.getLocalString
		            ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed1",
			     "Error : [ {0} ] field accessor method has the wrong return type [ {1} ] ",
			     new Object[] {fieldName, setMethod.getReturnType().getName()}));         
                }
            } else {
	
		result.addErrorDetails(smh.getLocalString
		    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed2",
		     "Error : Cannot find accessor [ {0 } ] method for [ {1} ] field ",
		     new Object[] {setMethodName , fieldName}));       
            }            
        } else {
	  
	    result.addErrorDetails(smh.getLocalString
		("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed2",
		 "Error : Cannot find accessor [ {0} ] method for [ {1} ] field ",
		 new Object[] {getMethodName , fieldName}));       
        }
        return false;  
    }
    
    
    /**
     * <p>
     * Checks that a field name starts with a lowercased letter
     * </p>
     * 
     * @param fieldName is the field name
     * @param result where to put the test result in
     * 
     * @return true if the test passed
     */
    public static boolean startWithLowercasedLetter(String fieldName, Result result) {
        if (Character.isLowerCase(fieldName.charAt(0))) {
	
            result.addGoodDetails(smh.getLocalString
		("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.startWithLowercasedLetter.success",
                "[ {0} ] field first letter is lowercase",
		new Object[] {fieldName})); 
            return true;
        } else {
	 
            result.addErrorDetails(smh.getLocalString
		("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.startWithLowercasedLetter.failed",
                "Error : [ {0} ] field first letter is not lowercase",
		new Object[] {fieldName}));         
            return false;
        }    
    }
    
    public static boolean accessorMethodModifiers(String fieldName, Class c, Result result) {
        String getMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String setMethodName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        
        // check the get first
        Method getMethod = getMethod(c, getMethodName, null);        
        if (getMethod != null) {
            int modifiers = getMethod.getModifiers();
            if (Modifier.isAbstract(modifiers) && (Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers))) {
                // now look for the setmethod
                Class parms[] = { getMethod.getReturnType() };
                Method setMethod = getMethod(c, setMethodName, parms);
                if (setMethod != null) {
                    modifiers = setMethod.getModifiers();
                    if (Modifier.isAbstract(modifiers) && (Modifier.isProtected(modifiers) || Modifier.isPublic(modifiers))) {
			result.addGoodDetails(smh.getLocalString
    		            ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.accessorMethodModifiers.success",
                            "[ {0} ] field accessor methods are abstract and public or protected",
		            new Object[] {fieldName}));                     
                        return true;
                    }
                }
	
                result.addErrorDetails(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.accessorMethodModifiers.failed",
                    "Error : [ {0} ] accessor method for field [ {1} ] is not abstract and public or protected",
		    new Object[] {setMethodName, fieldName}));         
                return false;
            }
        }

        result.addErrorDetails(smh.getLocalString
	    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.accessorMethodModifiers.failed",
            "Error : [ {0} ] accessor method for field [ {1} ] is not abstract and public or protected",
	    new Object[] {getMethodName, fieldName}));         
        return false;
    }       
}
