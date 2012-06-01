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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.SpecVersionMapper;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Enumeration;

/**
 * Session/Entity Bean Transaction demarcation type test.  
 * Application Assembler may define attributes for the methods of the 
 * remote/home interfaces of the beans that require container managed 
 * transaction demarcation.  All beans of the this type (container managed 
 * transactions) require container managed tranaction demarcation through
 * the use of "container-transaction" element.
 */
public class TransactionDemarcationType extends EjbTest implements EjbCheck { 

    static String[] EJBObjectMethods = 
    { 
        "getHomeHandle", "getEJBMetaData",
        "getEJBHome", "getEJBLocalHome","getHandle", 
        "getPrimaryKey", "isIdentical"
    };


    /**
     * Session/Entity Bean Transaction demarcation type test.
     * Application Assembler may define attributes for the methods of the
     * remote/home interfaces of the beans that require container managed
     * transaction demarcation.  All beans of the this type (container managed
     * transactions) require container managed tranaction demarcation through
     * the use of "container-transaction" element.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	// hack try/catch block around test, to exit gracefully instead of
	// crashing verifier on getMethodDescriptors() call, XML mods cause
	// java.lang.ClassNotFoundException: verifier.ejb.hello.BogusEJB
	// Replacing <ejb-class>verifier.ejb.hello.HelloEJB with
	//  <ejb-class>verifier.ejb.hello.BogusEJB...
	try  {
	    if ((descriptor instanceof EjbSessionDescriptor) ||
		(descriptor instanceof EjbEntityDescriptor)) {
                boolean oneFailed = false;
		String transactionType = descriptor.getTransactionType();
		if (EjbDescriptor.CONTAINER_TRANSACTION_TYPE.equals(transactionType)) {
		    try  {
                        Arrays.sort(EJBObjectMethods);
			ContainerTransaction containerTransaction = null;
                        if (!descriptor.getMethodContainerTransactions().isEmpty()) {
                            for (Enumeration ee = descriptor.getMethodContainerTransactions().keys(); ee.hasMoreElements();) {
                                MethodDescriptor methodDescriptor = (MethodDescriptor) ee.nextElement();
				if (Arrays.binarySearch(EJBObjectMethods, methodDescriptor.getName()) < 0) {
				    containerTransaction =
                                                (ContainerTransaction) descriptor.getMethodContainerTransactions().get(methodDescriptor);
    		        
                    if (containerTransaction != null && 
                            containerTransaction.getTransactionAttribute()!=null) {
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
					        || ContainerTransaction.NEVER.equals(transactionAttribute)) {
						addGoodDetails(result, compName);
						result.addGoodDetails(smh.getLocalString
								      (getClass().getName() + ".passed",
								       "TransactionAttribute [ {0} ] for method [ {1} ] is valid.",
								       new Object[] {transactionAttribute, methodDescriptor.getName()}));
					    } else {
						oneFailed = true;
						addErrorDetails(result, compName);
						result.addErrorDetails(smh.getLocalString
								       (getClass().getName() + ".failed",
								        "Error: TransactionAttribute [ {0} ] for method [ {1} ] is not valid.",
								        new Object[] {transactionAttribute, methodDescriptor.getName()}));
					    } 
				        } else {
                        // Null transaction attributes are allowed in EJB 3. Default is REQUIRED.
                        if(getVerifierContext().getJavaEEVersion().compareTo(SpecVersionMapper.JavaEEVersion_5)<0) {
					    oneFailed = true;
					    addErrorDetails(result, compName);
					    result.addErrorDetails(smh.getLocalString
							           (getClass().getName() + ".failedException",
								    "Error: TransactionAttribute is null for method [ {0} ]",
								    new Object[] {methodDescriptor.getName()}));
                        }
					    
                        }
                                } // if you found a business method
				else {
				    //check if the ejb is a session bean 
				    //and the method with transaction attribute belongs 
                                //to home/local home interface
				    String ejbClass = methodDescriptor.getEjbClassSymbol();

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
                                        continue;
                                    }

*/
                    boolean session = descriptor instanceof EjbSessionDescriptor;
				    boolean entity = descriptor instanceof EjbEntityDescriptor;
				    if (((ejbClass == null) 
					 || ejbClass.equals(MethodDescriptor.EJB_HOME) 
					 || ejbClass.equals(MethodDescriptor.EJB_LOCALHOME)) 
					&& session) {
					oneFailed = true;
					addErrorDetails(result, compName);
					result.addErrorDetails(smh.getLocalString
							       (getClass().getName() + ".failedHome",
								"Error: TransactionAttribute for method [ {0} ] is not valid. Home or Local Home interface methods of a session bean must not hvae a transaction attribute.",
								new Object[] {methodDescriptor.getName()}));
				    }
				    //check if it is a session bean with remote/local interface 
				    //and method with Tx attribute is "remove"
				    else if (((ejbClass == null) 
                          || ejbClass.equals(MethodDescriptor.EJB_REMOTE)
					      || ejbClass.equals(MethodDescriptor.EJB_LOCAL)) 
					     && session && methodDescriptor.getName().equals("remove")) {
					//check for style 3
					//if remove method defined has parameters then pass else fail
					if (methodDescriptor.getParameterClassNames() == null
					    || methodDescriptor.getParameterClassNames().length == 0 ) {
					    //style 2
					    oneFailed = true;
					    addErrorDetails(result, compName);
					    result.addErrorDetails(smh.getLocalString
								   (getClass().getName() + ".failedComp",
								    "Error: TransactionAttribute for method [ {0} ] is not valid. 'remove' method in Remote/Local interface of a session bean must not have a transaction attribute.",
								    new Object[] {methodDescriptor.getName()}));
					} else {
					    addGoodDetails(result, compName);
					    result.addGoodDetails(smh.getLocalString
								  (getClass().getName() + ".passedTest",
								   "TransactionAttribute for method [ {0} ] is valid.",
								   new Object[] {methodDescriptor.getName()}));
					}
				    }
				    else if (((ejbClass == null) 
                          || ejbClass.equals(MethodDescriptor.EJB_HOME) 
					      || ejbClass.equals(MethodDescriptor.EJB_LOCALHOME)) 
					     && entity) {
					if (methodDescriptor.getParameterClassNames() == null
					    || methodDescriptor.getParameterClassNames().length == 0) {
					    //style 2
					    oneFailed = true;
					    addErrorDetails(result, compName);
					    result.addErrorDetails(smh.getLocalString
								   (getClass().getName() + ".failed1",
								    "Error: TransactionAttribute for method [ {0} ] is not valid. ",
								    new Object[] {methodDescriptor.getName()}));
					} else {
					    addGoodDetails(result, compName);
					    result.addGoodDetails(smh.getLocalString
								  (getClass().getName() + ".passedTest",
								   "TransactionAttribute for method [ {0} ] is valid.",
								   new Object[] { methodDescriptor.getName()}));
					}
				    }
				    else if (((ejbClass == null) 
					 || ejbClass.equals(MethodDescriptor.EJB_REMOTE) 
					 || ejbClass.equals(MethodDescriptor.EJB_LOCAL)) 
					&& entity) {
					if ((methodDescriptor.getName()).equals("isIdentical")) {
					    if(methodDescriptor.getParameterClassNames() == null
					       || methodDescriptor.getParameterClassNames().length == 0 ) {
						addGoodDetails(result, compName);
						result.addGoodDetails(smh.getLocalString
								      (getClass().getName() + ".passedTest",
								       "TransactionAttribute for method [ {0} ] is valid.",
								       new Object[] {methodDescriptor.getName()}));
					    } else {
						String[] paramList = methodDescriptor.getParameterClassNames();
						if(Array.getLength(paramList) == 1) {
						    if (paramList[0].equals("javax.ejb.EJBObject")) {
							//style 3
							oneFailed = true;
							addErrorDetails(result, compName);
							result.addErrorDetails(smh.getLocalString
									       (getClass().getName() + ".failed1",
										"Error: TransactionAttribute for method [ {0} ] is not valid.",
										new Object[] { methodDescriptor.getName()}));
						    }
						    else {
							addGoodDetails(result, compName);
							result.addGoodDetails(smh.getLocalString
									      (getClass().getName() + ".passedTest",
									       "TransactionAttribute for method [ {0} ] is valid.",
									       new Object[] { methodDescriptor.getName()}));
						    }
						} else {
						    addGoodDetails(result, compName);
						    result.addGoodDetails(smh.getLocalString
									  (getClass().getName() + ".passedTest",
									   "TransactionAttribute for method [ {0} ] is valid.",
									   new Object[] { methodDescriptor.getName()}));
						}
					    }
					}
					else { //for all other methods in entity bean
					    if ((methodDescriptor.getName()).equals("remove")) {
						addGoodDetails(result, compName);
						result.addGoodDetails(smh.getLocalString
								      (getClass().getName() + ".passedTest",
								       "TransactionAttribute for method [ {0} ] is valid.",
								       new Object[] { methodDescriptor.getName()}));
					    }
					    else {
						if (methodDescriptor.getParameterClassNames() == null 
						    || methodDescriptor.getParameterClassNames().length == 0) {
						    //style 2
						    oneFailed = true;
						    addErrorDetails(result, compName);
						    result.failed(smh.getLocalString
								  (getClass().getName() + ".failedException1",
								   "Error: [ {0} ] should not have a Transaction Attribute",
								   new Object[] {methodDescriptor.getName()}));
						}
					    }
					}
				    }
				}
			    }
			} else {
			    addNaDetails(result, compName);
			    result.notApplicable(smh.getLocalString
						 (getClass().getName() + ".notApplicable1",
						  "There are no method permissions within this bean [ {0} ]", 
						  new Object[] {descriptor.getName()}));
			}

		    } catch (Exception e) {
			oneFailed = true;
			addErrorDetails(result, compName);
			result.failed(smh.getLocalString
				      (getClass().getName() + ".failedException2",
				       "Error: [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
				       new Object[] {descriptor.getName(), e.getMessage(), descriptor.getName()}));
			return result;
		    }
                    if (oneFailed) {
                        result.setStatus(Result.FAILED);
                    } else {
                        result.setStatus(Result.PASSED);
                    }
                    return result;
		} else {
		    // not container managed, but is a session/entity bean
		    addNaDetails(result, compName);
		    result.notApplicable(smh.getLocalString
					 (getClass().getName() + ".notApplicable2",
					  "Bean [ {0} ] is not {1} managed, it is [ {2} ] managed.", 
					  new Object[] {descriptor.getName(),EjbDescriptor.CONTAINER_TRANSACTION_TYPE,transactionType}));
		}
		return result;
	    } else {
		addNaDetails(result, compName);
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable",
				      "[ {0} ] not called \n with a Session or Entity bean.",
				      new Object[] {getClass()}));
		return result;
	    } 
	} catch (Throwable t) {
	    addErrorDetails(result, compName);
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException2",
			   "Error: [ {0} ] does not contain class [ {1} ] within bean [ {2} ]",
			   new Object[] {descriptor.getName(), t.getMessage(), descriptor.getName()}));
	    return result;
	}

    }
}
