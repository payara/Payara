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
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;


/**
 * Check that the field type for all declated cmp fields of the entity bean
 * are of an acceptable type :
 *      - Java Primitive Type
 *      - Java Serializable class
 *      - Reference to a bean's home or bean's remote interface
 * 
 * @author  Jerome Dochez
 * @version 1.0
 */
public class CmpFields extends CmpFieldTest {
 
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
        String cmpFieldName = persistentField.getName();
        String getMethodName = "get" + Character.toUpperCase(cmpFieldName.charAt(0)) + cmpFieldName.substring(1);
        Method getMethod = getMethod(c, getMethodName, null);
        Class fieldType;

        if (getMethod != null) {
	    boolean run = false;
            // get the return type for the setMethod
            fieldType = getMethod.getReturnType();
	    EjbBundleDescriptorImpl bundle = ((EjbDescriptor) entity).getEjbBundleDescriptor();
	    if (!RmiIIOPUtils.isValidRmiIDLPrimitiveType(fieldType) &&
		!EjbUtils.isValidSerializableType(fieldType)) {
		// it must be a reference to a bean's home or local interface
		if (!isValidInterface(fieldType, bundle.getEjbs(),MethodDescriptor.EJB_REMOTE, result)) {
		     result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addErrorDetails(smh.getLocalString
			   (getClass().getName() + ".failed",
			   "Error : Invalid type assigned for container managed field [ {0} ] in bean [ {1} ]",
			    new Object[] {((Descriptor)persistentField).getName(),entity.getName()}));
		    return false;
		}
		if (!isValidInterface(fieldType, bundle.getEjbs(),MethodDescriptor.EJB_LOCAL, result)) {
		     result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addErrorDetails(smh.getLocalString
			      (getClass().getName() + ".failed",
			       	"Error : Invalid type assigned for container managed field [ {0} ] in bean [ {1} ]",
				new Object[] {((Descriptor)persistentField).getName(),entity.getName()}));
				return false;
		}
	    }   
	    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()})); 
	    result.addGoodDetails(smh.getLocalString
		   (getClass().getName() + ".passed",
		    "Valid type assigned for container managed field [ {0} ] in bean [ {1} ]",
		    new Object[] {((Descriptor)persistentField).getName(),entity.getName()}));
	    run = true;  
	    return run;
	    
        } else {
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
            result.addErrorDetails(smh.getLocalString
		("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CMPTest.isAccessorDeclared.failed2",
		 "Error : Cannot find accessor [ {0} ] method for [ {1} ] field ",
		 new Object[] {getMethodName , persistentField.getName()}));       
        }
        return false;
    }

    private boolean isValidInterface(Class fieldType, Set<EjbDescriptor> entities,String interfaceType, Result result) {
	try {  
        if (entities==null)
            return false;
        
        Iterator<EjbDescriptor> iterator = entities.iterator();
	if(interfaceType.equals(MethodDescriptor.EJB_REMOTE)) {
	    while (iterator.hasNext()) {
		EjbDescriptor entity = iterator.next();
		
		if (fieldType.getName().equals(entity.getHomeClassName()) ||
		    fieldType.getName().equals(entity.getRemoteClassName()))
		    return true;
	    }
	}
	if(interfaceType.equals(MethodDescriptor.EJB_LOCAL)) {
	    while (iterator.hasNext()) {
		EjbDescriptor entity = iterator.next();
		
		if (fieldType.getName().equals(entity.getLocalHomeClassName()) ||
		    fieldType.getName().equals(entity.getLocalClassName()))
		    return true;
	    }
	}
        return false;
	}catch(Throwable t) {
	    result.addErrorDetails(smh.getLocalString
			      (getClass().getName() + ".failed",
			       	"Error occured in accessing remote/local interface",
				new Object[] {}));
	    return false;
	}
    }
}
