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

package com.sun.enterprise.tools.verifier.tests.ejb.entity;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.util.logging.Level;

/** 
 * The interfaces/classes that the entity bean implements must be serializable 
 * directly or indirectly.
 * @author Sheetal Vartak
 */
public class EntityBeanExtendsSerializableClass extends EjbTest implements EjbCheck { 

    /**
     * The interfaces/classes that the entity bean implements must be 
     * serializable directly or indirectly.
     * Ejb 2.1 says that "The bean class that uses the timer service must 
     * implement the javax.ejb.TimedObject interface."
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor instanceof EjbEntityDescriptor) {
	    try {
		Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());

		boolean validBean = true;
		Class superClass = c.getSuperclass();
		if (validBean == true) {
		    // walk up the class tree 
		  if(!(superClass.getName()).equals("java.lang.Object")) {
			if (!isValidSerializableType(superClass) &&
					!isTimedObject(superClass)) {
			    validBean = false;
			    result.addWarningDetails(smh.getLocalString
						     ("tests.componentNameConstructor",
						      "For [ {0} ]",
						      new Object[] {compName.toString()}));
			    result.addWarningDetails(smh.getLocalString
						     (getClass().getName() + ".failed1",
						      "[ {0} ] extends class [ {1} ] which is not serializable. ",
						      new Object[] {descriptor.getEjbClassName(),superClass.getName()}));
			    result.setStatus(Result.WARNING);
			    return result;
			} else {
			    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed1",
						   "Bean [ {0} ] extends class [ {1} ] which is serializable. ",
						   new Object[] {descriptor.getEjbClassName(), superClass.getName()}));
			    do {
				Class[] interfaces = c.getInterfaces();
				
				for (int i = 0; i < interfaces.length; i++) {
				    
                    logger.log(Level.FINE, getClass().getName() + ".debug1",
                            new Object[] {interfaces[i].getName()});

				    if (!isValidSerializableType(interfaces[i])
					 && !isTimedObject(interfaces[i])) {
					validBean = false;
					result.addWarningDetails(smh.getLocalString
								 ("tests.componentNameConstructor",
								  "For [ {0} ]",
								  new Object[] {compName.toString()}));
					result.addWarningDetails(smh.getLocalString
						   (getClass().getName() + ".failed",
						   "[ {0} ] implements interface [ {1} ] which is not serializable. ",
						   new Object[] {descriptor.getEjbClassName(),interfaces[i].getName()}));
					result.setStatus(Result.WARNING);
					break;
				    }
				}
			    } while ((((c=c.getSuperclass()) != null) && (validBean != false)));
			}
		    }
		    if (validBean == true){
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.passed(smh.getLocalString
				      (getClass().getName() + ".passed",
				       "Bean [ {0} ] implements interfaces which are all serializable. ",
				       new Object[] {descriptor.getEjbClassName()}));
			result.setStatus(Result.PASSED);
		    }
		}


	    } catch (ClassNotFoundException e) {
		Verifier.debug(e);
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException",
			       "Error: [ {0} ] class not found.",
			       new Object[] {descriptor.getEjbClassName()}));
	    }  

	    return result;
	    
	} else {
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean, but called with {2} bean.",
				  new Object[] {getClass(),"Entity","Session"}));
	    return result;
	}
    }

    /** Class checked for implementing java.io.Serializable interface test.
     * Verify the following:
     *
     *   The class must implement the java.io.Serializable interface, either
     *   directly or indirectly.
     *
     * @param serClass the class to be checked for Rmi-IIOP value type
     *        compliance
     *
     * @return <code>boolean</code> true if class implements java.io.Serializable, false otherwise
     */
    public static boolean isValidSerializableType(Class serClass) {

        if (java.io.Serializable.class.isAssignableFrom(serClass))
            return true;
        else
            return false;
    }

    public static boolean isTimedObject(Class serClass) {
        if (javax.ejb.TimedObject.class.isAssignableFrom(serClass))
            return true;
        else
            return false;
    }
}
