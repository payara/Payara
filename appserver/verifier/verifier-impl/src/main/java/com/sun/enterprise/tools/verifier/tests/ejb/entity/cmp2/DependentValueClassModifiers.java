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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbUtils;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Set;


/**
 * Dependent value class must be public and not abstract and must be serializable
 *
 * @author  Jerome Dochez
 * @version 
 */
public class DependentValueClassModifiers extends CmpFieldTest {

    /**
     * run an individual verifier test of a declated cmp field of the class
     *
     * @param entity the descriptor for the entity bean containing the cmp-field    
     * @param f the descriptor for the declared cmp field
     * @param c the class owning the cmp field
     * @parma r the result object to use to put the test results in
     * 
     * @return true if the test passed
     */    
    protected boolean runIndividualCmpFieldTest(Descriptor entity, Descriptor persistentField, Class c, Result result) {
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor(); 
        String fieldName = persistentField.getName();
        String getMethodName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method getMethod = getMethod(c, getMethodName, null);
        if (getMethod != null) {        
            Class returnType = getMethod.getReturnType();
            // check if this is a reference to a primitive or an array of primitive type
            if (returnType.isArray()) {
                returnType = returnType.getComponentType();
            }
            if (returnType.isPrimitive()) {
		result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                result.addGoodDetails(smh.getLocalString(
                    "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.notApplicable",
                    "Field [ {0} ] is not a dependent value class reference",
                    new Object[] {fieldName}));        
                return true;
            }
	    if (returnType.isInterface()) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
                result.addGoodDetails(smh.getLocalString(
							 "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.notApplicable",
							 "Field [ {0} ] is not a dependent value class reference",
							 new Object[] {fieldName}));        
                return true;
            }
	    if (returnType.toString().startsWith("class java.")) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
                result.addGoodDetails(smh.getLocalString(
							 "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.notApplicable",
							 "Field [ {0} ] is not a dependent value class reference",
							 new Object[] {fieldName}));        
                return true;
            }
             // it must be a reference to a bean's home or remote interface
            EjbBundleDescriptorImpl bundle = ((EjbDescriptor) entity).getEjbBundleDescriptor();
            if ((isValidInterface(returnType, bundle.getEjbs(),MethodDescriptor.EJB_REMOTE)) ||
		(isValidInterface(returnType, bundle.getEjbs(),MethodDescriptor.EJB_LOCAL))) {
		result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.addGoodDetails(smh.getLocalString(
							 "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.notApplicable",
							 "Field [ {0} ] is not a dependent value class reference",
							 new Object[] {fieldName}));        
		return true;
	    }
      
            // this is a reference to a dependent value class
            int modifiers = returnType.getModifiers();
	    if (Modifier.isPublic(modifiers) && 
		Modifier.isAbstract(modifiers) == false && 
		EjbUtils.isValidSerializableType(returnType)) {
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
		result.addGoodDetails(smh.getLocalString(
				       "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.passed",
				       "Dependent value class [ {0} ] reference by cmp field [ {1} ] is public, not abstract and serializable",
				       new Object[] {returnType.getName(), fieldName}));        
		return true;
            } else {
		result.addWarningDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));
                result.addWarningDetails(smh.getLocalString(
				       "com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.DependentValueClassModifiers.failed",
				       "Verifier cannot find out if [ {0} ] is a Dependent value class (reference by cmp field [ {1} ]) ",
				       new Object[] {returnType.getName(), fieldName})); 
		return false;
            }
        } else {
	    result.addErrorDetails(smh.getLocalString
		  	          ("tests.componentNameConstructor",
				   "For [ {0} ]",
				   new Object[] {compName.toString()}));
            result.addErrorDetails(smh.getLocalString
				   ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed2",
				    "Error : Cannot find accessor [ {0} ] method for [ {1} ] field ",
		 new Object[] {getMethodName, fieldName}));       
            return false;
        }
    }
    
    private boolean isValidInterface(Class fieldType, Set<EjbDescriptor> entities, String interfaceType) {
        
        if (entities==null)
            return false;
        
        Iterator<EjbDescriptor> iterator = entities.iterator();
	if (interfaceType.equals(MethodDescriptor.EJB_REMOTE)) {
	    while (iterator.hasNext()) {
		EjbDescriptor entity = iterator.next();
		if (fieldType.getName().equals(entity.getHomeClassName()) ||
		    fieldType.getName().equals(entity.getRemoteClassName()))
		    return true;
	    }
	}
 	if (interfaceType.equals(MethodDescriptor.EJB_LOCAL)) {
	    while (iterator.hasNext()) {
		EjbDescriptor entity = iterator.next();
		if (fieldType.getName().equals(entity.getLocalHomeClassName()) ||
		    fieldType.getName().equals(entity.getLocalClassName()))
		    return true;
	    } 
	}
	return false;
    }
}
