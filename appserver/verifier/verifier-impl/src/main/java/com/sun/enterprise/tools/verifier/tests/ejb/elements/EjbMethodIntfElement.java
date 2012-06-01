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

package com.sun.enterprise.tools.verifier.tests.ejb.elements;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.MethodPermission;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** 
 * The method-intf element must be one of the following "Remote" or "Home" or "LocalHome" or "Local".
 */
public class EjbMethodIntfElement extends EjbTest implements EjbCheck {


    /** 
     * The method-intf element must be one of the following "Remote" or "Home" or "LocalHome" or "Local".
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();


        // method-intf don't make sense for messagedriven bean
        if (descriptor instanceof EjbMessageBeanDescriptor) {
	    result.addNaDetails(smh.getLocalString
				  ("tests.componentNameConstructor",
				   "For [ {0} ]",
				   new Object[] {compName.toString()}));         
            result.notApplicable(smh.getLocalString
                (getClass().getName() + ".notApplicable",
                "There are no <method-intf> elements within this bean [ {0} ]",
                new Object[] {descriptor.getName()}));
            return result;
        }
        
	// hack try/catch block around test, to exit gracefully instead of
	// crashing verifier on getMethodDescriptors() call, XML mods cause
	// java.lang.ClassNotFoundException: verifier.ejb.hello.BogusEJB
	// Replacing <ejb-class>verifier.ejb.hello.HelloEJB with
	//  <ejb-class>verifier.ejb.hello.BogusEJB...
	try  {

	    boolean na = false;
	    boolean na1 = false;
	    boolean oneFailed = false;
            if (!descriptor.getMethodContainerTransactions().isEmpty()) {
                for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
  
		    MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
                    String methodIntf = methodDescriptor.getEjbClassSymbol();
                    if ( methodIntf == null ) { //|| methodIntf.equals("") 
                        continue;
                    }
                    // The method-intf element must be one of the following
                    // Home Remote LocalHome Local ServiceEndpoint
                    if (!( (methodIntf.equals(MethodDescriptor.EJB_REMOTE)) ||
                        (methodIntf.equals(MethodDescriptor.EJB_HOME)) ||
                        (methodIntf.equals(MethodDescriptor.EJB_LOCALHOME)) || 
                        (methodIntf.equals(MethodDescriptor.EJB_LOCAL)) ||
                        (methodIntf.equals(MethodDescriptor.EJB_WEB_SERVICE)) ||
                        (methodIntf.length()==0))) {
                    // The method-intf element must be one of the following "Remote" or "Home"
//		    if (!((methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE))  ||
//			  (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)) ||
//			  (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) || 
//			  (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL)) ||
//                          (methodDescriptor.getEjbClassSymbol().length()==0))) {
			oneFailed =true;
			result.addErrorDetails(smh.getLocalString
					       ("tests.componentNameConstructor",
						"For [ {0} ]",
						new Object[] {compName.toString()}));
			result.addErrorDetails
			    (smh.getLocalString
			     (getClass().getName() + ".failed",
			      "Error: Container transaction method [ {0} ] method-intf element [ {1} ] must be one of the following: [ {2} ] or [ {3} ] or [ {4} ] or [ {5} ]  within bean [ {6} ]",
			      new Object[] {methodDescriptor.getName(),methodDescriptor.getEjbClassSymbol(),
						MethodDescriptor.EJB_REMOTE.toString(), MethodDescriptor.EJB_HOME,
						MethodDescriptor.EJB_LOCAL, MethodDescriptor.EJB_LOCALHOME,
						descriptor.getName()}));
		    } else {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.addGoodDetails
			    (smh.getLocalString
			     (getClass().getName() + ".passed",
			      "Container Transaction method [ {0} ] method-intf element [ {1} ] is one of the following:  [ {2} ] or [ {3} ] or [ {4} ] or [ {5} ]  within bean [ {6} ]",
			      new Object[] {methodDescriptor.getName(),methodDescriptor.getEjbClassSymbol(),
						MethodDescriptor.EJB_REMOTE, MethodDescriptor.EJB_HOME,
						MethodDescriptor.EJB_LOCAL, MethodDescriptor.EJB_LOCALHOME,
						descriptor.getName()}));
		    } 
		} 
	    } else {
                na = true;
	    } 

            Map permissionedMethods = descriptor.getPermissionedMethodsByPermission();
            if (permissionedMethods.size() >0) {
		for (Iterator e = permissionedMethods.keySet().iterator(); e.hasNext();) {            
		    MethodPermission nextPermission = (MethodPermission) e.next();
		    Set permissionedMethodsForRole = (HashSet) permissionedMethods.get(nextPermission);

		    if (permissionedMethodsForRole != null) {
			Set convertedPermissionedMethods = new HashSet();
			for (Iterator itr = permissionedMethodsForRole.iterator(); itr.hasNext();) {
			    MethodDescriptor methodDescriptor = (MethodDescriptor) itr.next();

                            String methodIntf = methodDescriptor.getEjbClassSymbol();
                            if ( methodIntf == null  ) { //|| methodIntf.equals("")
                                continue;
                            }
                            // The method-intf element must be one of the following
                            // Home Remote LocalHome Local ServiceEndpoint
                            if (!( (methodIntf.equals(MethodDescriptor.EJB_REMOTE)) ||
                                (methodIntf.equals(MethodDescriptor.EJB_HOME)) ||
                                (methodIntf.equals(MethodDescriptor.EJB_LOCALHOME)) || 
                                (methodIntf.equals(MethodDescriptor.EJB_LOCAL)) ||
                                (methodIntf.equals(MethodDescriptor.EJB_WEB_SERVICE)) ||
                                (methodIntf.length()==0))) {
  
			    // The method-intf element must be one of the following "Remote" or "Home"
//			    if (!((methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE))  ||
//				  (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)) || 
//                                (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL)) || 
//				  (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) || 
//                                  (methodDescriptor.getEjbClassSymbol().length()==0))) {

				oneFailed =true;
				result.addErrorDetails(smh.getLocalString
						       ("tests.componentNameConstructor",
							"For [ {0} ]",
							new Object[] {compName.toString()}));
				result.addErrorDetails
				    (smh.getLocalString
				     (getClass().getName() + ".failed1",
				      "Error: Method permissions method [ {0} ] method-intf element [ {1} ] must be one of the interfaces of the bean [ {2} ]",
				      new Object[] {methodDescriptor.getName(),
							methodDescriptor.getEjbClassSymbol(), 
							descriptor.getName()}));
			    } else {
				result.addGoodDetails(smh.getLocalString
						      ("tests.componentNameConstructor",
						       "For [ {0} ]",
						       new Object[] {compName.toString()}));
				result.addGoodDetails
				    (smh.getLocalString
				     (getClass().getName() + ".passed1",
				      "Method permissions method [ {0} ] method-intf element [ {1} ] is one of the interfaces of the  bean [ {2} ]",
				      new Object[] {methodDescriptor.getName(),methodDescriptor.getEjbClassSymbol(), descriptor.getName()}));
			    } 
			} 
		    } 
		} 
	    } else {
                na1 = true;
	    } 


	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else if (na && na1) {
		result.addNaDetails(smh.getLocalString
				    ("tests.componentNameConstructor",
				     "For [ {0} ]",
				     new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "There are no <method-intf> elements within this bean [ {0} ]",
				      new Object[] {descriptor.getName()}));
	    } else {
		result.setStatus(Result.PASSED);
	    }
	    return result;
	} catch (Throwable t) {
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
			   new Object[] {descriptor.getName(), t.getMessage(), descriptor.getName()}));
	    return result;
	}
    }
}
