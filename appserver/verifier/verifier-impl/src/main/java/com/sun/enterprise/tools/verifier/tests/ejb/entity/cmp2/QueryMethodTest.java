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

import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;

import java.lang.reflect.Method;

/**
 * Superclass for all finder method test
 *
 * @author  Jerome Dochez
 * @version 
 */
abstract public class QueryMethodTest extends CMPTest {
    ComponentNameConstructor compName = null;
    /**
     * <p>
     * Run an individual test against a finder method (single or multi)
     * </p>
     * 
     * @param method is the finder method reference
     * @param descriptor is the entity bean descriptor
     * @param targetClass is the class to apply to tests to
     * @param result is where to place the result
     * 
     * @return true if the test passes
     */
    protected abstract boolean runIndividualQueryTest(Method method, EjbCMPEntityDescriptor descriptor, Class targetClass, Result result);
    
     /**
     * check if a field has been declared in a class
     * 
     * @param fieldName the field name to look for declaration
     * @param c the class to look into
     * @param result where to place the test result
     */
    public Result check(EjbCMPEntityDescriptor descriptor) {
        
        boolean allIsWell = true;
        Result result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();
        
	if (descriptor.getHomeClassName() != null && !((descriptor.getHomeClassName()).equals("")) &&
	    descriptor.getRemoteClassName() != null && !((descriptor.getRemoteClassName()).equals(""))) {
	    allIsWell = commonToBothInterfaces(descriptor.getHomeClassName(),descriptor.getRemoteClassName(),descriptor, result);
	}   
	if(allIsWell == true) {
	    if (descriptor.getLocalHomeClassName() != null && !((descriptor.getLocalHomeClassName()).equals("")) &&
		descriptor.getLocalClassName() != null && !((descriptor.getLocalClassName()).equals(""))) {
		allIsWell = commonToBothInterfaces(descriptor.getLocalHomeClassName(),descriptor.getLocalClassName(),descriptor, result);
	    } 
	}    
     
        if (allIsWell) 
            result.setStatus(Result.PASSED);
        else 
            result.setStatus(Result.FAILED);
            
        return result;
    }
  /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param ejbHome for the Home interface of the Ejb. 
     * @param result Result of the test
     * @param remote Remote/Local interface
     * @return boolean the results for this assertion i.e if a test has failed or not
     */


    private boolean commonToBothInterfaces(String ejbHome, String remote, EjbDescriptor descriptor, Result result) {
	boolean allIsWell = true;
	boolean found = false;
	String ejbClassName = descriptor.getEjbClassName();
	VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
        try {
            Class ejbClass = Class.forName(ejbClassName, false,
                                getVerifierContext().getClassLoader());
            Method[] methods = Class.forName(ejbHome, false,
                                getVerifierContext().getClassLoader()).getMethods();
            for (int i=0;i<methods.length;i++) {
                String methodName = methods[i].getName();
                // get the expected return type
                String methodReturnType = methods[i].getReturnType().getName();
                if (methodName.startsWith("find")) {
		    found = true;
                    if (methodReturnType.equals(remote) ||                     
			isSubclassOf(Class.forName(methodReturnType, false,
                    getVerifierContext().getClassLoader()), "java.util.Collection") ||
			isImplementorOf(Class.forName(methodReturnType, false,
                    getVerifierContext().getClassLoader()), "java.util.Collection")) {
                        
                        if (!runIndividualQueryTest(methods[i], (EjbCMPEntityDescriptor) descriptor, ejbClass, result)) 
                            allIsWell=false;
                    }
                }
	    }
	    if (found == false) {
		result.addGoodDetails(smh.getLocalString
			  ("com.sun.enterprise.tools.verifier.tests.ejb.EjbTest.passed",
			   "Not Applicable : No find methods found",
                new Object[] {}));  
	    }   
            
	    return allIsWell;
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
       		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			  ("com.sun.enterprise.tools.verifier.tests.ejb.EjbTest.failedException",
			   "Error: [ {0} ] class not found.",
                new Object[] {descriptor.getEjbClassName()}));                    
            allIsWell= false;
	    return allIsWell;
        }
    }
}
