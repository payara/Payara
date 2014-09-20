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
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.CMRFieldInfo;
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;

import java.lang.reflect.Method;

/**
 * EJB 2.0 Spec 9.4.11 CMR accessor methods for relationships
 *  between entity beans should not be exposed in the remote interface
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class CmrFieldsAccessorExposition extends CmrFieldTest {

   /**
     * run an individual verifier test of a declated cmr field of the class
     *
     * @param entity the descriptor for the entity bean containing the cmp-field    
     * @param info the descriptor for the declared cmr field
     * @param c the class owning the cmp field
     * @parma r the result object to use to put the test results in
     * 
     * @return true if the test passed
     */            
    protected boolean runIndividualCmrTest(Descriptor descriptor, RelationRoleDescriptor rrd, Class c, Result result) {
        
        // check first if this is one-to-one or many-to-one relationship ...previous version of ejb specs
	//   if ((!rrd.getIsMany() && !rrd.getPartner().getIsMany()) ||
	//     (rrd.getIsMany() && !rrd.getPartner().getIsMany())) {                
	    //  }
        // everyone falls back and should be checked

        boolean pass = true;
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
      
        // should not have accessor methods exposed. 
       	if (((EjbDescriptor)descriptor).getRemoteClassName() != null && 
	    !((((EjbDescriptor)descriptor).getRemoteClassName()).equals(""))) {
	    String interfaceType = ((EjbDescriptor)descriptor).getRemoteClassName();
	    try {             
		CMRFieldInfo info = rrd.getCMRFieldInfo();
		Class remoteInterface = Class.forName(interfaceType, false, getVerifierContext().getClassLoader());
		String getMethodName = "get" + Character.toUpperCase(info.name.charAt(0)) + info.name.substring(1);        
		String setMethodName = "set" + Character.toUpperCase(info.name.charAt(0)) + info.name.substring(1);        
		
		Method getMethod = getMethod(remoteInterface, getMethodName, null);
		if (getMethod != null) {
            addErrorDetails(result, compName);
		    result.addErrorDetails(smh.getLocalString
		    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldsAccessorExposition.failed",
		     "Error : CMR field {0} accessor method [ {1} ] is exposed through the component interface [ {2} ]",
		     new Object[] {"get", info.name, interfaceType}));     
		    pass = false;
		} else {
		     result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
			("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldsAccessorExposition.passed",
			 "CMR field {0} accessor method [ {1} ] is not exposed through the component interface [ {2} ]",
			 new Object[] {"get", info.name, interfaceType}));        
		    pass = true;           
		}
		
		Class parms[] = { info.type };
		Method setMethod = getMethod(remoteInterface, setMethodName, parms );        
		if (setMethod != null) {
            addErrorDetails(result, compName);
		    result.addErrorDetails(smh.getLocalString
		       ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldsAccessorExposition.failed",
		       "Error : CMR field {0} accessor method [ {1} ] is exposed through the component interface [ {2} ]",
			new Object[] {"set", info.name, interfaceType}));   
		    
		    pass = false;
		} else {
		     result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
			("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldsAccessorExposition.passed",
			 "CMR field [{0}] accessor method [ {1} ] is not exposed through the component interface [ {2} ]",
			 new Object[] {"set", info.name, interfaceType}));                    
		}  
		
	    } catch (Exception e) {
		Verifier.debug(e);
        addErrorDetails(result, compName);
		result.addErrorDetails(smh.getLocalString
			      ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldsAccessorExposition.failedException",
			       "Error:  [{0}] class not found or local interface not defined",
			       new Object[] {interfaceType}));
		pass = false;
	
	    }                 
	} 
        return pass;        
    }
}
