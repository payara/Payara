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

/*
 * SelectMethodTest.java
 *
 * Created on December 14, 2000, 4:36 PM
 */

package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;

import java.lang.reflect.Method;

/**
 *
 * @author  dochez
 * @version 
 */
abstract public class SelectMethodTest extends CMPTest {

    protected abstract boolean runIndividualSelectTest(Method m, EjbCMPEntityDescriptor descriptor, Result result);
    
    /** 
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbCMPEntityDescriptor descriptor) {
        
        boolean allIsWell = true;
        Result result = getInitializedResult();
	boolean found = false;        
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        Class ejbClass = loadEjbClass(descriptor, result);
        if (ejbClass!=null) {
            Method[] methods = ejbClass.getDeclaredMethods();
	    if (methods != null) {
		for (int i=0;i<methods.length;i++) {
		    String methodName = methods[i].getName();
		    if (methodName.startsWith("ejbSelect")) {
			found = true;
			if (!runIndividualSelectTest(methods[i], (EjbCMPEntityDescriptor) descriptor, result))
			    allIsWell=false;
		    }
		}
		if (found == false) {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
					  ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.SelectMethodTest.nptApplicable",
					   "Not Applicable : No select methods found",
					   new Object[] {}));     
		}
        if (result.getStatus() != Result.NOT_APPLICABLE) {    
            if (allIsWell) 
                result.setStatus(Result.PASSED);
            else 
                result.setStatus(Result.FAILED);            
            }
        }
	}    
	return result;
    }    
}
