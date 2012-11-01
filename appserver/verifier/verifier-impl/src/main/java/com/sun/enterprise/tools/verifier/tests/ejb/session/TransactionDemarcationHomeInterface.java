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

package com.sun.enterprise.tools.verifier.tests.ejb.session;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.MethodUtils;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Method;
import java.util.Enumeration;

/** 
 * Session Bean transaction demarcation type home interface methods test.  
 * Transaction attributes must not be specified for the methods of a session
 * bean's home interface.
 */
public class TransactionDemarcationHomeInterface extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;

    /** 
     * Session Bean transaction demarcation type home interface methods test.  
     * Transaction attributes must not be specified for the methods of a session
     * bean's home interface.
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
	    // Transaction attributes must not be specified for the methods of a session
	    // bean's home interface.
	    if (descriptor instanceof EjbSessionDescriptor) {
		String transactionType = descriptor.getTransactionType();
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
                if (EjbDescriptor.CONTAINER_TRANSACTION_TYPE.equals(transactionType)) {
		    boolean oneFailed = false;
		    if(descriptor.getHomeClassName() != null && !"".equals(descriptor.getHomeClassName())) {
			Class superclass = Class.forName(descriptor.getHomeClassName(), false, getVerifierContext().getClassLoader());
			do {			    
			    oneFailed = commonToBothInterfaces(superclass.getName(),(EjbSessionDescriptor)descriptor, MethodDescriptor.EJB_HOME);
			    if (oneFailed == true) {
				break;
			    }
			    superclass = superclass.getSuperclass();
			} while (superclass != null);
		    }
		    if(descriptor.getLocalHomeClassName() != null && !"".equals(descriptor.getLocalHomeClassName())) {
			Class superclass = Class.forName(descriptor.getLocalHomeClassName(), false, getVerifierContext().getClassLoader());
			do {			    
			    oneFailed = commonToBothInterfaces(superclass.getName(),(EjbSessionDescriptor)descriptor, MethodDescriptor.EJB_LOCALHOME);
			    if (oneFailed == true) {
				break;
			    }
			    superclass = superclass.getSuperclass();
			} while (superclass != null);
		    }  

                    if ((oneFailed == false) &&  (implementsEndpoints(descriptor))) {
                       result.addNaDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                 "For [ {0} ]",
                                 new Object[] {compName.toString()}));
                       result.notApplicable(smh.getLocalString
                       ("com.sun.enterprise.tools.verifier.tests.ejb.webservice.notapp",
                       "Not Applicable because, EJB [ {0} ] implements a Service Endpoint Interface.",
                       new Object[] {compName.toString()}));
                       result.setStatus(Result.NOT_APPLICABLE);
                       return result;
                    }
                    
		    if (oneFailed) {
			result.setStatus(Result.FAILED);
		    } else {
			result.setStatus(Result.PASSED);
		    }
  
		    return result;
                } else {
                    // not container managed, but is a session bean
                    addNaDetails(result, compName);
                    result.notApplicable(smh.getLocalString
                                         (getClass().getName() + ".notApplicable2",
                                          "Bean [ {0} ] is not [ {1} ] managed, it is [ {2} ] managed.",
                                          new Object[] {descriptor.getName(),EjbDescriptor.CONTAINER_TRANSACTION_TYPE,transactionType}));
                    return result;
                }

	    } else {
		addNaDetails(result, compName);
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "{0} expected {1} bean, but called with {2} bean.",
				      new Object[] {getClass(),"Session","Entity"}));
		return result;
	    } 

	} catch (Throwable t) {
	    addErrorDetails(result, compName);
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] does not exist or is not loadable within bean [ {1} ].",
			   new Object[] {t.getMessage(), descriptor.getName()}));
	    return result;
	}

    }

    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home interface of the Ejb. 
     * @param methodIntf for the interface type
     * @return boolean the results for this assertion i.e if a test has failed or not
     */


    private boolean commonToBothInterfaces(String home, EjbSessionDescriptor descriptor, String methodIntf) {
	boolean oneFailed = false;
	try {
	    VerifierTestContext context = getVerifierContext();
	    ClassLoader jcl = context.getClassLoader();
	    Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
	    Method methods[] = c.getDeclaredMethods();
	    boolean lookForIt = false;
	    for (int i=0; i< methods.length; i++) {
		try  {
		    ContainerTransaction containerTransaction = null;
		    boolean resolved = true;
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
			    // corresponds to a or some methods on the home interface
			    // according to the six styles
			    // style 1)
			    if (methodDescriptor.getName().equals(MethodDescriptor.ALL_METHODS)) {
				// if getEjbClassName() is Remote -> CARRY ON
				// if Home - PASS
				if (methodDescriptor.getEjbClassSymbol() == null) {
                    lookForIt = true;
                } else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)) {
				    lookForIt = true;
				    // if empty String PASS
				} else if (methodDescriptor.getEjbClassSymbol().equals("")) {
				    lookForIt = true;
				} else if (methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_REMOTE)) {
				    lookForIt = false;
				    // else (Bogus)
				} else {
				    // carry on & don't look for 
				    // container transaction
				    lookForIt = false;
				}								
			    } else if (methodDescriptor.getParameterClassNames() == null) {
				// if (getEjbClassSybol() is Home or is the empty String AND if methods[i].getName().equals(methodDescriptor.getName()) 
				if (((methodDescriptor.getEjbClassSymbol() == null) ||
				     methodDescriptor.getEjbClassSymbol().equals("") ||
				     methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)||
				     methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) && 
				    (methods[i].getName().equals(methodDescriptor.getName()))) { 
				    //  PASS
				    lookForIt = true;
				} else {
				    // carry on
				    lookForIt = false;
				}								
			    } else {				
				// if (getEjbClassSybol() is Home or is the empty String AND if methods[i].getName().equals(methodDescriptor.getName()) AND 
				// the parameters of the method[i] are the same as the parameters of the method descriptor ) 				
				if (((methodDescriptor.getEjbClassSymbol() == null) ||
				     methodDescriptor.getEjbClassSymbol().equals("") ||
				     methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)||
				     methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) && 
				    (methods[i].getName().equals(methodDescriptor.getName())) && 
				    (MethodUtils.stringArrayEquals(methodDescriptor.getParameterClassNames(), (new MethodDescriptor(methods[i], methodIntf)).getParameterClassNames()))) { 
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
					// if "*" ignore, test N/A
					if (!methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_HOME)&&
					    !methodDescriptor.getEjbClassSymbol().equals(MethodDescriptor.EJB_LOCALHOME)) {
					    result.addGoodDetails(smh.getLocalString
								  (getClass().getName() + ".passed",
								   "Valid: TransactionAttribute [ {0} ] for method [ {1} ] is not defined for home interface [ {2} ]",
								   new Object[] {transactionAttribute, methods[i].getName(),home}));
					} else {
					    oneFailed = true;
					    addErrorDetails(result, compName);
					    result.addErrorDetails(smh.getLocalString
								   (getClass().getName() + ".failed",
								    "Error: TransactionAttribute [ {0} ] for method [ {1} ] is not valid.   Transaction attributes must not be specified for all methods of session bean home interface [ {2} ].",
								    new Object[] {transactionAttribute, methods[i].getName(),home}));
					    resolved = false;
					}
				    } else {
					addGoodDetails(result, compName);
					result.addGoodDetails(smh.getLocalString
							      (getClass().getName() + ".passed",
							       "Valid: TransactionAttribute [ {0} ] for method [ {1} ] is not defined for home interface [ {2} ]",
							       new Object[] {transactionAttribute, methods[i].getName(),home}));
				    } 
				} else {
				    addGoodDetails(result, compName);
				    result.addGoodDetails(smh.getLocalString
							  (getClass().getName() + ".passedException",
							   "Valid: TransactionAttribute is null for method [ {0} ]",
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
						    "Error: Transaction attributes must not be specified for the methods defined in the session bean home interface [ {0} ].  Method [ {1} ] has transaction attribute defined within this bean [ {2} ].",
						    new Object[] {home, methods[i].getName(), descriptor.getName()}));
/*
                            }
                            else {
                            
                              result.addGoodDetails(smh.getLocalString
                                                                   ("tests.componentNameConstructor",
                                                                    "For [ {0} ]",
                                                                    new Object[] {compName.toString()}));
                              result.addGoodDetails(smh.getLocalString
                                                                  (getClass().getName() + ".passed",
                                                                   "Valid: TransactionAttribute [ {0} ] for method [ {1} ] is defined for remote interface [ {2} ]", new Object[] {"*", "*",home}));


                            }
                            // End of workaround code. Note : this else also has to be removed once
                            // the original bug of methodDesc.getEjbClassSymbol() is fixed
*/

			} else {
			    addGoodDetails(result, compName);
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed2",
						   "Valid: Transaction attributes must not be specified for the methods defined in the session bean home interface [ {0} ].  Method [ {1} ] has no transaction attribute defined within this bean [ {2} ].",
						   new Object[] {home, methods[i].getName(), descriptor.getName()}));
			}
		    } else {
			addGoodDetails(result, compName);
			result.passed(smh.getLocalString
				      (getClass().getName() + ".passed1",
				       "Valid: There are no method permissions within this bean [ {0} ].   Transaction attributes must not be specified for the methods defined in the session bean home interface [ {1} ].  Method [ {2} ] has no transaction attribute defined.", 
				       new Object[] {descriptor.getName(),home, methods[i].getName()}));
		    }
		    if (oneFailed == true)
			return oneFailed;
		} catch (Exception e) {
		    addErrorDetails(result, compName);
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failedException1",
				   "Error: Home interface [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
				   new Object[] {home, e.getMessage(), descriptor.getName()}));
		    return oneFailed;
		}
	    } // for all the methods within the home interface class, loop
	     return oneFailed; 
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
        addErrorDetails(result, compName);
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException",
			   "Error: Home interface [ {0} ] does not exist or is not loadable within bean [ {1} ]",
			   new Object[] {home, descriptor.getName()}));
	    return oneFailed;
	}
	
    }
}
