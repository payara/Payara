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

package com.sun.enterprise.tools.verifier.tests.ejb.homeintf;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Modifier;

/**
 * All enterprise beans home interface's must be declared as public.
 */
abstract public class HomeInterfacePublic extends EjbTest implements EjbCheck { 
 
    /** Method tells the name of the home interface class that called this test
     */
    abstract protected String getHomeInterfaceName(EjbDescriptor descriptor);
    
    /** 
     * All enterprise beans home interface's must be declared as public.
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */

   
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if(getHomeInterfaceName(descriptor) == null || "".equals(getHomeInterfaceName(descriptor))){
            result.addNaDetails(smh.getLocalString
                        ("tests.componentNameConstructor", "For [ {0} ]",
                         new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString
                       ("com.sun.enterprise.tools.verifier.tests.ejb.localinterfaceonly.notapp",
                        "Not Applicable because, EJB [ {0} ] has Local Interfaces only.",
                                          new Object[] {descriptor.getEjbClassName()}));

	    return result;
	}


	if ((descriptor instanceof EjbSessionDescriptor) ||
	    (descriptor instanceof EjbEntityDescriptor)) {
 
	    try {
		VerifierTestContext context = getVerifierContext();
		ClassLoader jcl = context.getClassLoader();
		Class c = Class.forName(getClassName(descriptor), false, jcl);

		// remote interface must be defined as public
		boolean isPublic = false;
		int modifiers = c.getModifiers();
		if (Modifier.isPublic(modifiers)) {
		    isPublic = true;
		}
 
		// it extends the proper EJBHome, but is it's modifier public
		if (!isPublic){
		    result.addErrorDetails(smh.getLocalString
					   ("tests.componentNameConstructor",
					    "For [ {0} ]",
					    new Object[] {compName.toString()}));
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failed",
				   "Error: [ {0} ] is not defined as public.  All enterprise beans home interfaces must be defined as public.  [ {1} ] is not a valid home interface.",
				   new Object[] {getClassName(descriptor),getClassName(descriptor)}));
		} else {
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
			
		result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "[ {0} ] properly declares the home interface as public.",
				   new Object[] {getClassName(descriptor)}));
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
			       new Object[] {getClassName(descriptor)}));
	    }  
	    return result;
 
	} else {
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] expected {1} bean or {2} bean, but called with {3}.",
				  new Object[] {getClass(),"Session","Entity",descriptor.getName()}));
	    return result;
	}
    }

    private String getClassName(EjbDescriptor descriptor) {
	return getHomeInterfaceName(descriptor);
    } 
}
