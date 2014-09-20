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

package com.sun.enterprise.tools.verifier.tests.ejb.homeintf.remotehomeintf;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

/**
 * Home Interface follows the standard rules for RMI-IIOP remote interfaces 
 * test.  
 *
 * All enterprise beans home interface's must follow the standard rules 
 * for RMI-IIOP remote interfaces.
 */
public class RemoteHomeInterfaceRmiIIOP extends EjbTest implements EjbCheck { 


    /** 
     * Home Interface follows the standard rules for RMI-IIOP remote interfaces 
     * test.  
     *
     * All enterprise beans home interface's must follow the standard rules 
     * for RMI-IIOP remote interfaces.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	
	if(descriptor.getHomeClassName() == null || "".equals(descriptor.getHomeClassName())) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                       ("com.sun.enterprise.tools.verifier.tests.ejb.localinterfaceonly.notapp",
                        "Not Applicable because, EJB [ {0} ] has Local Interfaces only.",
                                          new Object[] {descriptor.getEjbClassName()}));

	    return result;
	}
	
	if ((descriptor instanceof EjbSessionDescriptor) ||
	    (descriptor instanceof EjbEntityDescriptor)) {
 
	    try {
		ClassLoader jcl = getVerifierContext().getClassLoader();
		Class c = Class.forName(descriptor.getHomeClassName(), false, jcl);

		// remote interface must be defined as valid Rmi-IIOP remote interface
		boolean isValidRmiIIOPInterface = false;
		if (RmiIIOPUtils.isValidRmiIIOPInterface(c) && RmiIIOPUtils.isValidRmiIIOPInterfaceMethods(c)) {
		    isValidRmiIIOPInterface = true;
		}
 
		// remote interface must be defined as valid Rmi-IIOP remote interface
		if (!isValidRmiIIOPInterface){
		    addErrorDetails(result, compName);
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failed",
				   "Error: [ {0} ] is not defined as valid RMI-IIOP remote interface.  All enterprise beans home interfaces must be defined as valid RMI-IIOP remote interface.  [ {1} ] is not a valid remote home interface.",
				   new Object[] {descriptor.getHomeClassName(),descriptor.getHomeClassName()}));
		} else {
		    addGoodDetails(result, compName);
		    result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "[ {0} ] properly declares the home interface as valid RMI-IIOP remote interface.",
				   new Object[] {descriptor.getHomeClassName()}));
		}
	    } catch (ClassNotFoundException e) {
		Verifier.debug(e);
		addErrorDetails(result, compName);
		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException",
			       "Error: [ {0} ] class not found.",
			       new Object[] {descriptor.getHomeClassName()}));
	    }  
	    return result;
 
	} else {
	    addNaDetails(result, compName);
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean or {2} bean, but called with {3}.",
				  new Object[] {getClass(),"Session","Entity",descriptor.getName()}));
	    return result;
	}
    }
}
