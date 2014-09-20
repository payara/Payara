/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * InteractionSpecJavaBeansCompliance.java
 *
 * Created on October 5, 2000, 5:02 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector.cci;

import java.io.File;
import java.beans.*;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;

/**
 *
 * @author  Jerome Dochez
 * @version 
 */
public class InteractionSpecJavaBeansCompliance extends ConnectionFactoryTest implements ConnectorCheck {


    /** <p>
     * all connector tests should implement this method. it run an individual
     * test against the resource adapter deployment descriptor.
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {
        
        boolean oneFailed=false;
        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if (isCCIImplemented(descriptor, result)) {
            //File jarFile = Verifier.getJarFile(descriptor.getModuleDescriptor().getArchiveUri());
//            File f=Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
            Class mcf = findImplementorOf(descriptor, "javax.resource.cci.InteractionSpec");
            if (mcf != null) {
                try {
                    BeanInfo bi = Introspector.getBeanInfo(mcf, Object.class);
                    PropertyDescriptor[] properties = bi.getPropertyDescriptors();
                    for (int i=0; i<properties.length;i++) {
                        // each property should have a getter/setter
                        if (properties[i].getReadMethod()==null || 
                            properties[i].getWriteMethod()==null) {
                                // this is an error.
                                oneFailed=true;
                                result.addErrorDetails(smh.getLocalString
						       ("tests.componentNameConstructor",
							"For [ {0} ]",
							new Object[] {compName.toString()}));
				result.failed(smh.getLocalString
					      (getClass().getName() + ".failed",
					       "Error: The javax.resource.cci.InteractionSpec implementation [ {0} ] of the property [ {1} ] is not JavaBeans compliant",
					       new Object[] {mcf.getName(), properties[i].getName()} ));                                                                                
			}
                        if (!properties[i].isConstrained() && !properties[i].isBound()) {
                            oneFailed=true;
                            result.addErrorDetails(smh.getLocalString
						   ("tests.componentNameConstructor",
						    "For [ {0} ]",
						    new Object[] {compName.toString()}));
			    result.failed(smh.getLocalString
					  (getClass().getName() + ".failed2",
					   "Error: The property [ {0} ] of the javax.resource.cci.InteractionSpec implementation [ {1} ] is not bound or constrained",
					   new Object[] {properties[i].getName(), mcf.getName()} ));                                                                                
                        }
                    }
                } catch (IntrospectionException ie) {
		    result.addNaDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
    	                (getClass().getName() + ".failed",
                        "Error: The javax.resource.cci.InteractionSpec implementation [ {0} ] is not JavaBeans compliant",
                        new Object[] {mcf.getName()} ));                                                
                    return result;
                }
                // now iterates over the properties and look for descrepencies
            } else {
		result.addNaDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString
    	            (getClass().getName() + ".nonexist",
                    "Error: While the CCI interfaces are implemented, the javax.resource.cci.InteractionSpec is not"));         
                return result;
            }
                
        } else {
	    result.addNaDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
    	        ("com.sun.enterprise.tools.verifier.tests.connector.cci.InteractionExistence.notapp",
                 "NotApplicable : The CCI interfaces do not seem to be implemented by this resource adapter"));                    
            return result;            
        }                
        if (!oneFailed) {
            result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));	
		result.passed(smh.getLocalString
                (getClass().getName() + ".passed",
                "The javax.resource.cci.InteractionSpec implementation is JavaBeans compliant"));                     
        }
        return result;
    }
}
