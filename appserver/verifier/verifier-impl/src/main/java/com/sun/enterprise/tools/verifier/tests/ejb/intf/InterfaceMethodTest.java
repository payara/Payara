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

package com.sun.enterprise.tools.verifier.tests.ejb.intf;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Superclass for all local/remote interfaces method testing.
 *
 */
abstract public class InterfaceMethodTest extends EjbTest {
    
    static String[] EJBObjectMethods = 
            { "getEJBHome", "getHandle", "getPrimaryKey",
              "isIdentical", "remove", "getEJBLocalHome",
            };
    
    /**
     * Methods to get the type of interface: local/remote and the name of the class
     */
    
    abstract protected String getInterfaceName(EjbDescriptor descriptor);
    abstract protected String getInterfaceType();
    
    
    /**
     * <p>
     * run an individual verifier test against a declared method of the 
     * local or remote interface.
     * </p>
     * 
     * @param descriptor the deployment descriptor for the bean
     * @param method the method to run the test on
     * @return true if the test passes
     */
    
    abstract protected boolean runIndividualMethodTest(EjbDescriptor descriptor, Method method, Result result);
    
    /**
     * Run the verifier test against the local or remote interface, get all methods
     * and delegate actual testing for individual methods to the 
     * runIndividualMethodTest
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if (!(descriptor instanceof EjbSessionDescriptor) &&
                !(descriptor instanceof EjbEntityDescriptor)) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.homeintf.HomeMethodTest.notApplicable1",
                    "Test apply only to session or entity beans."));
            return result;                
        }
        
        if(getInterfaceName(descriptor) == null || "".equals(getInterfaceName(descriptor))){
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceTest.notApplicable",
                    "Not Applicable because, EJB [ {0} ] does not have {1} Interface.",
                    new Object[] {descriptor.getEjbClassName(), getInterfaceType()}));
            return result;
        }
        
        try {
            
            Arrays.sort(EJBObjectMethods);
            
            // retrieve the local/remote interface methods
            ClassLoader jcl = getVerifierContext().getClassLoader();
            Class interfaceClass = Class.forName(getClassName(descriptor), false, jcl);
            
            if (studyInterface(descriptor, interfaceClass, result)) {
                result.setStatus(Result.PASSED);
            } else {
                result.setStatus(Result.FAILED);
            }                 
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: "+ getInterfaceType()+"interface [ {0} ] does not " +
                    "exist or is not loadable within bean [ {1} ]",
                    new Object[] {getClassName(descriptor),descriptor.getName()}));
        }
        
        return result;
    }
    
    /**
     * <p>
     * study an interface by running an individual test on each method of the
     * inteface then recursively study all the interfaces this interface extends
     * </p>
     * 
     * @param descriptor the bean deployment descriptor
     * @param clazz the interface to study
     * @param result to place the results of the tests in
     * @return true if all tests passed
     */
    private boolean studyInterface(EjbDescriptor descriptor, Class clazz, Result result) {
        
        boolean allGood = true;
        Method [] interfaceMethods = clazz.getDeclaredMethods();
        
        for (Method interfaceMethod : interfaceMethods) {
            if (Arrays.binarySearch(EJBObjectMethods, interfaceMethod.getName()) < 0) {
                
                if (!runIndividualMethodTest(descriptor, interfaceMethod,result))
                    allGood = false;
                
            } // if you found a business method
        } // for all local or remote interface methods for the current class
        
        // now all superinterfaces....
        for (Class intf : clazz.getInterfaces()) {
            if (!studyInterface(descriptor, intf, result)) 
                allGood = false;
        }
        return allGood;
    }
    
    private String getClassName(EjbDescriptor descriptor) {
        return getInterfaceName(descriptor);
    } 
}

