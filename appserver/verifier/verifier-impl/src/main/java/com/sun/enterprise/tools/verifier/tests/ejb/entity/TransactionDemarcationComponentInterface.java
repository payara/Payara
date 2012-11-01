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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.MethodUtils;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;


/**
 * Entity Bean transaction demarcation type for all methods of component
 * interface test.
 * The transaction attributes must be specified for the methods defined
 * in the bean's component interface and all the direct and indirect
 * superinterfaces of the component interface, excluding the "getEJBHome", 
 * "getHandle", "getPrimaryKey", "isIdentical" methods.
 */
 public class TransactionDemarcationComponentInterface extends EjbTest implements EjbCheck { 

    static String[] EJBObjectMethods =
    { "getEJBHome", "getHandle", "getEJBLocalHome", "getPrimaryKey", "isIdentical"
    };
     Result result = null;
     ComponentNameConstructor compName = null;


    /**
     * Entity Bean transaction demarcation type for all methods of component
     * interface test.
     * The transaction attributes must be specified for the methods defined
     * in the bean's component interface and all the direct and indirect
     * superinterfaces of the component interface, excluding the "getEJBHome", 
     * "getHandle", "getPrimaryKey", "isIdentical", "getEJBLocalHome" methods.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	result = getInitializedResult();
	compName = getVerifierContext().getComponentNameConstructor();

	// hack try/catch block around test, to exit gracefully instead of
	// crashing verifier on getMethodDescriptors() call, XML mods cause
	// java.lang.ClassNotFoundException: verifier.ejb.hello.BogusEJB
	// Replacing <ejb-class>verifier.ejb.hello.HelloEJB with
	//  <ejb-class>verifier.ejb.hello.BogusEJB...
	try  {
	    // The transaction attributes must be specified for the methods defined
	    // in the bean's component interface and all the direct and indirect
	    // superinterfaces of the component interface, excluding the "getEJBHome",
	    //  "getHandle", "getPrimaryKey", "isIdentical" methods of the 
	    // javax.ejb.EJBObject interface.

	    if (descriptor instanceof EjbEntityDescriptor) {
		boolean oneFailed = false;
		if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName()))
		    oneFailed = commonToBothInterfaces(descriptor.getRemoteClassName(),descriptor, MethodDescriptor.EJB_REMOTE);
		if(oneFailed == false) {
		    if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName()))
			oneFailed = commonToBothInterfaces(descriptor.getLocalClassName(),descriptor, MethodDescriptor.EJB_LOCAL);
		}
		if (oneFailed) {
		    result.setStatus(Result.FAILED);
		} else {
		    result.setStatus(Result.PASSED);
		}
		return result;
		
	    } else {
		addNaDetails(result, compName);
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "{0} expected {1} bean, but called with {2} bean.",
				      new Object[] {getClass(),"Entity","Session"}));
		return result;
	    } 
	} catch (Throwable t) {
	    addErrorDetails(result, compName);
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException1",
			   "Error: Component interface does not contain class [ {0} ] within bean [ {1} ]",
			   new Object[] { t.getMessage(), descriptor.getName()}));
	    return result;
	}
    }
     
  /** 
     * This method is responsible for the logic of the test. It is called for both local and component interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param component for the Remote/Local interface of the Ejb. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */

     private boolean commonToBothInterfaces(String component, EjbDescriptor descriptor, String methodIntf) {
	 boolean oneFailed = false;
	 try {
	     Arrays.sort(EJBObjectMethods);
	     
	     // retrieve the component interface methods
	     Class componentInterfaceClass = Class.forName(component, false, getVerifierContext().getClassLoader());
	     Method [] componentInterfaceMethods = componentInterfaceClass.getDeclaredMethods();
	     boolean lookForIt = false;
	     
	     for (int i = 0; i < componentInterfaceMethods.length; i++) {
               if (Arrays.binarySearch(EJBObjectMethods, componentInterfaceMethods[i].getName()) < 0) {
		     try {
			 ContainerTransaction containerTransaction = null;
			 boolean resolved = false;
/*
                         // This flag is a workaround introduced by Harminder
                         // because currently methodDescriptor.getEjbClassSymbol() is
                         // returning NULL
                         //boolean allMethods = false;
                         boolean wildCardWasPresent = false;
*/
                         
			 if (!descriptor.getMethodContainerTransactions().isEmpty()) {
			     for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
				lookForIt = false;
				MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();

    /*** Fixed the bug: 4883730. ejbClassSymbol is null when method-intf is not 
     * defined in the xml, since it is an optional field. Removed the earlier 
     * checks. A null method-intf indicates that the method is supposed to be 
     * in both Local & Home interfaces. ***/                    
/*
                                // This code is a workaround introduced by Harminder
                                // because currently methodDescriptor.getEjbClassSymbol() is
                                // returning NULL
                                String methodIntf = null;
                                try {
                                    methodIntf = methodDescriptor.getEjbClassSymbol();
                                } catch ( Exception ex ) {}
                                if ( methodIntf == null ) { //|| methodIntf.equals("") 
                                    //probably a wildcard was there
                                    wildCardWasPresent = true;
                                    continue;
                                }
                                 //allMethods = true;
                                 // end of workaround
*/
                                 
				 // here we have to check that each method descriptor
				 // corresponds to a or some methods on the component interface
				 // according to the six styles
				 // style 1)
                 if (methodDescriptor.getName().equals(MethodDescriptor.ALL_METHODS)) {
				     // if getEjbClassName() is Remote -> CARRY ON
				     // if Remote - PASS
                     if (methodDescriptor.getEjbClassSymbol() == null) {
					 lookForIt = true;
                     } else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE)) {
					 lookForIt = true;
					 // if empty String PASS
				     } else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL)) {
					 lookForIt = true;
				     }else if (methodDescriptor.getEjbClassSymbol().equals("")) {
					 lookForIt = true;
				     } else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)) {
					 lookForIt = false;
				     }  else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) {
					 lookForIt = false;
				     }else {
					 // carry on & don't look for 
					 // container transaction
					 lookForIt = false;
				     }
				     
				     
				 } else if (methodDescriptor.getParameterClassNames() == null) {
				     
				     
				     // if (getEjbClassSybol() is Remote or is the empty String AND if componentInterfaceMethods[i].getName().equals(methodDescriptor.getName()) 
				     if (((methodDescriptor.getEjbClassSymbol() == null) ||
					  methodDescriptor.getEjbClassSymbol().equals("") ||
					  methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE)||
					  methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL)) && 
					 (componentInterfaceMethods[i].getName().equals(methodDescriptor.getName()))) {
					 //  PASS
					 lookForIt = true;
				     } else {
					 // carry on
					 lookForIt = false;
				     }
				     
				     
				 } else {
				     
				     // if (getEjbClassSybol() is Remote or is the empty String AND if componentInterfaceMethods[i].getName().equals(methodDescriptor.getName()) AND 
				     // the parameters of the method[i] are the same as the parameters of the method descriptor ) 
				     
				     if (((methodDescriptor.getEjbClassSymbol() == null) ||
					  methodDescriptor.getEjbClassSymbol().equals("") ||
					  methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE)||
					  methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCAL))&& 
					 (componentInterfaceMethods[i].getName().equals(methodDescriptor.getName()))&& 
					 (MethodUtils.stringArrayEquals(methodDescriptor.getParameterClassNames(), (new MethodDescriptor(componentInterfaceMethods[i], methodIntf)).getParameterClassNames()))) { 
					 // PASS    	
					 lookForIt = true;
				     } else {
					 // CARRY ON
					 lookForIt = false;
				     }
				     
				 }
				 
				 if (lookForIt) {
				     containerTransaction = 
					 (ContainerTransaction) descriptor.getMethodContainerTransactions().get(methodDescriptor);
				     if (containerTransaction != null) {
					 String transactionAttribute  = 
					     containerTransaction.getTransactionAttribute();
					 
					 // danny is doing this in the DOL, but is it possible to not have 
					 // any value for containerTransaction.getTransactionAttribute() 
					 // in the DOL? if it is possible to have blank value for this, 
					 // then this check is needed here, otherwise we are done and we 
					 // don't need this check here
					 if (ContainerTransaction.NOT_SUPPORTED.equals(transactionAttribute)
					     || ContainerTransaction.SUPPORTS.equals(transactionAttribute)
					     || ContainerTransaction.REQUIRED.equals(transactionAttribute)
					     || ContainerTransaction.REQUIRES_NEW.equals(transactionAttribute)
					     || ContainerTransaction.MANDATORY.equals(transactionAttribute)
					     || ContainerTransaction.NEVER.equals(transactionAttribute)
					     || (!transactionAttribute.equals(""))) {
					     addGoodDetails(result, compName);
					     result.addGoodDetails(smh.getLocalString
								   (getClass().getName() + ".passed",
								    "Valid: [ {0} ] TransactionAttribute [ {1} ] for method [ {2} ] is valid. Transaction attributes must be defined for all methods except getEJBHome, getHandle, getPrimaryKey, and isIdentical methods of component interface [ {3} ].",
								    new Object[] {componentInterfaceMethods[i].getName(), transactionAttribute, methodDescriptor.getName(),component}));
					     resolved = true;
					 } else {
					     oneFailed = true;
					     addErrorDetails(result, compName);
					     result.addErrorDetails(smh.getLocalString
								    (getClass().getName() + ".failed",
								     "Error: [ {0} ] TransactionAttribute [ {1} ] for method [ {2} ] is not valid.   Transaction attributes must be defined for all methods except getEJBHome, getHandle, getPrimaryKey, and isIdentical methods of component interface [ {3} ]",
								     new Object[] {componentInterfaceMethods[i].getName(), transactionAttribute, methodDescriptor.getName(),component}));
					 } 
				     } else {
					 oneFailed = true;
					 addErrorDetails(result, compName);
					 result.addErrorDetails(smh.getLocalString
								(getClass().getName() + ".failedException",
								 "Error: TransactionAttribute is null for method [ {0} ]",
								 new Object[] {methodDescriptor.getName()}));
				     }       
				 }
			     }
			     // before you go on to the next method,
			     // did you resolve the last one okay?
			     if (!resolved) {
/*
                                // This if-stmt code is a workaround introduced by Harminder
                                // because currently methodDescriptor.getEjbClassSymbol() is
                                // returning NULL
                                //if (allMethods){
                                if (!wildCardWasPresent) {
*/
                                     oneFailed = true;
				     addErrorDetails(result, compName);
				     result.addErrorDetails(smh.getLocalString
							(getClass().getName() + ".failed1",
							 "Error: Transaction attributes must be specified for the methods defined in the component interface [ {0} ].  Method [ {1} ] has no transaction attribute defined within this bean [ {2} ].", 
							 new Object[] {component, componentInterfaceMethods[i].getName(),descriptor.getName()}));
                                 }
/*
                                 else {
					     result.addGoodDetails(smh.getLocalString
								   ("tests.componentNameConstructor",
								    "For [ {0} ]",
								    new Object[] {compName.toString()}));
					     result.addGoodDetails(smh.getLocalString
								   (getClass().getName() + ".passed",
								    "Valid: [ {0} ] TransactionAttribute [ {1} ] for method [ {2} ] is valid. Transaction attributes must be defined for all methods except getEJBHome, getHandle, getPrimaryKey, and isIdentical methods of component interface [ {3} ].",
								    new Object[] {componentInterfaceMethods[i].getName(), "*", "*" ,component}));
//                              }
                              // End of workaround code. Note : this else also has to be removed once
                              // the original bug of methodDesc.getEjbClassSymbol() is fixed
			     }
*/
			 } else {
			     oneFailed = true;
			     addErrorDetails(result, compName);
			     result.addErrorDetails(smh.getLocalString
						    (getClass().getName() + ".failed2",
						     "Error: There are no method permissions within this bean [ {0} ].   Transaction attributes must be specified for the methods defined in the component interface [ {1} ].  Method [ {2} ] has no transaction attribute defined.", 
						     new Object[] {descriptor.getName(),component, componentInterfaceMethods[i].getName()}));
			 }
			 if(oneFailed == true && i==componentInterfaceMethods.length-1)
                            return oneFailed;
                        else
                           continue;
             		     } catch (Exception e) {
			 addErrorDetails(result, compName);
			 result.failed(smh.getLocalString
				       (getClass().getName() + ".failedException1",
					"Error: Component interface [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
					new Object[] {component, e.getMessage(), descriptor.getName()}));
			 oneFailed = true;
			 return oneFailed;
		     }
		 } // if you found a business method
		 else {
		     oneFailed = true;
		     addErrorDetails(result, compName);
		     result.failed(smh.getLocalString
				   (getClass().getName() + ".failedExcep",
				    "Error: Method [ {0} ] should not be assigned a transaction attribute.",
				    new Object[] {componentInterfaceMethods[i].getName()}));
		     
		 }
	     } // for all component interface methods
	     return oneFailed;
	 } catch (ClassNotFoundException e) {
	     Verifier.debug(e);
         addErrorDetails(result, compName);
	     result.failed(smh.getLocalString
			   (getClass().getName() + ".failedException2",
			    "Error: Component interface [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			    new Object[] {component,descriptor.getName()}));
	     oneFailed = true;
	     return oneFailed;
	 }
	 
     }
 }
     
