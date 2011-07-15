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

package com.sun.enterprise.tools.verifier.tests.appclient;

import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;

/** 
 * The application client ejb-ref-name element contains the name of an EJB 
 * reference. The EJB reference is an entry in the enterprise bean's 
 * environment.  It is recommended that name is prefixed with "ejb/".
 */
public class AppClientEjbRefNamePrefixed extends AppClientTest implements AppClientCheck { 


    /** 
     * The application client ejb-ref-name element contains the name of an EJB 
     * reference. The EJB reference is an entry in the enterprise bean's 
     * environment.  It is recommended that name is prefixed with "ejb/".
     *
     * @param descriptor the app-client deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(ApplicationClientDescriptor descriptor) {
	Result result = getInitializedResult();
ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	boolean oneWarning = false;
	if (!descriptor.getEjbReferenceDescriptors().isEmpty()) {
	    for (Iterator itr = descriptor.getEjbReferenceDescriptors().iterator();
		 itr.hasNext();) {
		EjbReferenceDescriptor nextEjbReference = (EjbReferenceDescriptor) itr.next();
		String ejbRefName = nextEjbReference.getName();

		if (ejbRefName.startsWith("ejb/")) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails
			(smh.getLocalString
			 (getClass().getName() + ".passed",
			  "[ {0} ] is prefixed with recommended string \"ejb/\" within application client [ {1} ]",
			  new Object[] {ejbRefName,descriptor.getName()}));
		}  else {
		    if (!oneWarning) {
			oneWarning = true;
		    }
		    result.addWarningDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addWarningDetails
			(smh.getLocalString
			 (getClass().getName() + ".warning",
			  "Warning: [ {0} ] is not prefixed with recommended string \"ejb/\" within application client [ {1} ]",
			  new Object[] {ejbRefName,descriptor.getName()}));
		}
	    }
	    if (oneWarning) {
		result.setStatus(Result.WARNING);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no ejb references to other beans within this application client [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}
	return result;
    }
}
