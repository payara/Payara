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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;

import java.util.Iterator;
import java.util.Set;


/**
 * Container-managed persistent fields related tests superclass 
 *
 * @author  Jerome Dochez
 * @version 
 */
abstract public class CmpFieldTest extends CMPTest {


    /**
     * run an individual verifier test of a declated cmp field of the class
     *
     * @param entity the descriptor for the entity bean containing the cmp-field
     * @param f the descriptor for the declared cmp field
     * @param c the class owning the cmp field
     * @parma r the result object to use to put the test results in
     * 
     * @return true if the test passed
     */
    protected abstract boolean runIndividualCmpFieldTest(Descriptor entity, Descriptor f, Class c, Result r);
    
    /** 
     *
     * Container-managed persistent fields test, iterates over all declared
     * cmp fields and invoke the runIndividualCmpFieldTest nethod
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbCMPEntityDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        Class c = loadEjbClass(descriptor, result);
        if (c!=null) {
            Descriptor persistentField;
	    boolean oneFailed = false;
            
            Set persistentFields = descriptor.getPersistenceDescriptor().getCMPFields();
            Iterator iterator = persistentFields.iterator();
	    if (iterator.hasNext()) {	  	
		while (iterator.hasNext()) {
		    persistentField = (Descriptor)iterator.next();
		    boolean status  = runIndividualCmpFieldTest(descriptor, persistentField, c, result);
		    if (!status) 
			oneFailed=true;                       
		    
		}
		if (oneFailed) {
		    result.setStatus(Result.FAILED);
		} else { 
		    result.setStatus(Result.PASSED);
		}
	    }	    
	    else { 
		result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmpFieldTest.notApplicable",
				      "Not Applicable : The EJB has no CMP fields declared",
				      new Object[] {})); 
	    }
	} 
        return result;
    }
}
