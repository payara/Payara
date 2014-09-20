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
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.PersistentFieldInfo;

import java.lang.reflect.Method;

/**
 * EJB 2.0 Spec 9.4.11 Set Accessor method for primary key fields should not be 
 * exposed in the remote/local interface
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class CmpFieldsAccessorExposition extends CMPTest {
    Result result = null;
    ComponentNameConstructor compName = null;

    /** 
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbCMPEntityDescriptor descriptor) {

	result = getInitializedResult();
        boolean oneFailed = false;
	compName = getVerifierContext().getComponentNameConstructor();
        
	if (descriptor.getRemoteClassName() != null && !((descriptor.getRemoteClassName()).equals(""))) 
	    oneFailed = commonToBothInterfaces(descriptor.getRemoteClassName(),descriptor); 
	if(oneFailed == false) {
	    if (descriptor.getLocalClassName() != null && !((descriptor.getLocalClassName()).equals(""))) 
		oneFailed = commonToBothInterfaces(descriptor.getLocalClassName(),descriptor); 
	}
	if (oneFailed) 
            result.setStatus(Result.WARNING);
        else 
            result.setStatus(Result.PASSED);
        return result;
    }

 /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param remote for the Remote/Local interface of the Ejb. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */
  
    private boolean commonToBothInterfaces(String remote, EjbDescriptor descriptor) {
	boolean oneFailed = false;
	try { 
	   Class c = Class.forName(remote, false, getVerifierContext().getClassLoader());   
	    boolean foundAtLeastOne = false;
            
	    try {
		// Check first that pk fields set methods are mot part of the remote interface                
		PersistentFieldInfo[] pkFieldInfos = ((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getPkeyFieldInfo();
		for (int i=0;i<pkFieldInfos.length;i++) {
		    foundAtLeastOne = true;
		    PersistentFieldInfo info = pkFieldInfos[i];
		    // check that setXXX is not part of the remote interface
		    String setMethodName = "set" + Character.toUpperCase(info.name.charAt(0)) + info.name.substring(1);                
		    Class parms[] = { info.type };
		    Method setMethod = getMethod(c, setMethodName, parms );        
		    if (setMethod != null) {
			// oopss
			result.addWarningDetails(smh.getLocalString
						 ("tests.componentNameConstructor",
						  "For [ {0} ]",
						  new Object[] {compName.toString()}));
			result.addWarningDetails(smh.getLocalString
			    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmpFieldsAccessorExposition.failed",
			     "Error : Primary key field set accessor method [ {0} ] is exposed through the component interface [ {1} ]",
			     new Object[] {info.name,remote}));  
			oneFailed = true;
		    } else {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString
			    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmpFieldsAccessorExposition.passed",
			     "Primary key field set accessor method [ {0} ] is not exposed through the component interface [ {1} ]",
			     new Object[] {info.name,remote}));                    
		    }
		}
		if (foundAtLeastOne == false) {
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
		     ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmpFieldsAccessorExposition.notApplicable",
		      "No persistent fields found.",
		      new Object[] {})); 
		    return oneFailed;
		}
		
	    } catch (RuntimeException rt) {
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.failed(smh.getLocalString
			      ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmpFieldsAccessorExposition.failedException1",
			   "Exception occured while trying to access Primary key info in PersistenceDescriptor.",
			   new Object[] {}));
	    }
	    return oneFailed;
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
	    oneFailed = true;
	    return oneFailed;
	    
	}            
    }       
}
