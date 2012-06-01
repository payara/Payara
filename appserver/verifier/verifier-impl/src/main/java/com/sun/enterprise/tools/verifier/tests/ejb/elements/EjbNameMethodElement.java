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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.util.Iterator;
import java.util.logging.Level;


/**
 * The ejb-name element within the method element must be the name of one 
 * of the enterprise beans declared in the deployment descriptor.
 */
public class EjbNameMethodElement extends EjbTest implements EjbCheck { 


    /**
     * The ejb-name element within the method element must be the name of one 
     * of the enterprise beans declared in the deployment descriptor.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	// get ejb's methods

	// DOL doesn't save "ejb-name" element inside of method element
	// so i can't seem to get at raw representation of XML data needed
	// for this test, 
        // <ejb-name> within <method> element is the name of the ejb 
        // descriptor where you got the method descriptor from,
        // so you can't trip negative assertion and test should always pass
        // once plugged into new DOL, where access to raw XML is
        // available, then this test can be properly modified,
	// then i would use DOL similar to this:
	//Set methods = descriptor.getMethodDescriptors();

	//for (Iterator itr = methods.iterator(); itr.hasNext();) {

	//MethodDescriptor methodDescriptor = (MethodDescriptor) itr.next();

	boolean found = false;
	for (Iterator itr2 = 
		 descriptor.getEjbBundleDescriptor().getEjbs().iterator();
	     itr2.hasNext();) {
	    EjbDescriptor ejbDescriptor = (EjbDescriptor) itr2.next();
        logger.log(Level.FINE, getClass().getName() + ".debug1",
                new Object[] {ejbDescriptor.getName()});
	    
	    // now iterate over all methods to ensure that ejb-name exist
	    //if (methodDescriptor.getName().equals(ejbDescriptor.getName())) {

	    // for now, do this test, which should always pass, since DOL lacks
	    // raw XML data representation
            // <ejb-name> within <method> element is the name of the ejb
            // descriptor where you got the method descriptor from
	    if (descriptor.getName().equals(ejbDescriptor.getName())) {
		found = true;
		if (result.getStatus() != Result.FAILED){
		    result.setStatus(Result.PASSED);
		    // for now, pass in details string via addGoodDetails
		    // until DOL raw data issue gets resolved
		    result.addGoodDetails(smh.getLocalString
					  ("tests.componentNameConstructor",
					   "For [ {0} ]",
					   new Object[] {compName.toString()}));
		    result.addGoodDetails
			(smh.getLocalString
			 (getClass().getName() + ".passed",
			  "[ {0} ] is valid and contained within jar.",
			  new Object[] {descriptor.getName()}));
		}
	    }
	}
	if (!found) {
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.addErrorDetails(smh.getLocalString
			("tests.componentNameConstructor",
			"For [ {0} ]",
			new Object[] {compName.toString()}));
		result.failed
		(smh.getLocalString
		 (getClass().getName() + ".failed",
		  "Error: [ {0} ] is not the name of one of the EJB's within jar.",
		  new Object[] {descriptor.getName()}));
	    //(methodDescriptor.getName() pending DOL update
	}
	//}
	return result;

    }

}
