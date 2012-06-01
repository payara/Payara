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

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * Find Methods should have deployment descriptors associated with them
 * 
 * @author  Jerome Dochez
 * @version 
 */
public class FindMethodHasDescriptors extends QueryMethodTest {

    /**
     * <p>
     * Run an individual test against a finder method (single or multi)
     * </p>
     * 
     * @param method is the finder method reference
     * @param descriptor is the entity bean descriptor
     * @param targetClass is the class to apply to tests to
     * @param result is where to place the result
     * 
     * @return true if the test passes
     */
    protected boolean runIndividualQueryTest(Method method, EjbCMPEntityDescriptor descriptor, Class targetClass, Result result) {
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        if (method.getName().equals("findByPrimaryKey")) {
	    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.addGoodDetails(smh.getLocalString
				  ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.FindMethodHasDescriptors.passed1",
				   "Passed: Found method findByPrimaryKey",
				   new Object[] {})); 
            return true;
	}
        
        // We don't use getQueryFor to free ourselfves from classloader issues.
        Set set = descriptor.getPersistenceDescriptor().getQueriedMethods();
        Iterator iterator = set.iterator();
	if (iterator.hasNext()) {
	    while(iterator.hasNext()) {
		MethodDescriptor queryMethod = (MethodDescriptor) iterator.next();
		if (queryMethod.getName().equals(method.getName())) {
		    Class mParms[] = method.getParameterTypes();
		    String queryParms[] = queryMethod.getParameterClassNames();
            int queryParamsLen;
            if(queryParms == null)
              queryParamsLen = 0;
            else
              queryParamsLen = queryParms.length;
		    if (queryParamsLen == mParms.length) {
			boolean same = true;
            if(queryParamsLen > 0)
            {
			for (int i=0;i<mParms.length;i++) {
			    if (!mParms[i].getName().equals(queryParms[i]))
				same=false;                    
			}
            }
			if (same) {  
			    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
				("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.FindMethodHasDescriptors.passed",
				 "[ {0} ] has a query element associated with it",
				 new Object[] {method}));       
			    return true;
			}
		    }
		}
	    }
	    result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.addErrorDetails(smh.getLocalString
			    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.FindMethodHasDescriptors.failed",
			     "Error : [ {0} ] seems to be a finder method but has no query element associated with it",
			     new Object[] {method}));       
	    return false;               
	}
	else {
	    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
	    result.addGoodDetails(smh.getLocalString
	    ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.FindMethodHasDescriptors.notApplicable",
	     "NotApplicable : No Query methods found",
	     new Object[] {})); 
	    return true;
	}
       
    }
}
