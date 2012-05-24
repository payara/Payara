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
import org.glassfish.ejb.deployment.descriptor.RelationRoleDescriptor;
import org.glassfish.ejb.deployment.descriptor.RelationshipDescriptor;

import java.util.Iterator;
import java.util.Set;

/**
 * Container managed relationship fields tests superclass, iterates over all
 * declated cmr fields and delegate actual tests to subclasses
 *
 * @author  Jerome Dochez
 * @version 
 */
abstract public class CmrFieldTest extends CMPTest {

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
    protected abstract boolean runIndividualCmrTest(Descriptor entity, RelationRoleDescriptor rrd, Class c, Result r);
    
    /** 
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbCMPEntityDescriptor descriptor) {

        Result result = getInitializedResult();
        addErrorDetails(result,
            getVerifierContext().getComponentNameConstructor());

	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean oneFailed = false;
	boolean found = false;

        Class c = loadEjbClass(descriptor, result);

        if (c!=null) {
            Set cmrFields = ((EjbCMPEntityDescriptor)descriptor).getPersistenceDescriptor().getRelationships();
            Iterator cmrIterator = cmrFields.iterator();

	    if (cmrIterator.hasNext()) {
		while (cmrIterator.hasNext()) {
		    RelationshipDescriptor cmfDescriptor = (RelationshipDescriptor) cmrIterator.next();
            {
                // test if this bean is the source in this relationship
                RelationRoleDescriptor role = cmfDescriptor.getSource();
                if (role.getOwner().equals(descriptor) && role.getCMRField()!=null) {
                found = true;
                if (!runIndividualCmrTest(descriptor, role, c, result)) {
                    oneFailed = true;
                }
                }
            }
            // we need to test for both source and sink because of self references
            {
                // test if this bean is the sink in this relationship
                RelationRoleDescriptor role = cmfDescriptor.getSink();
                if (role.getOwner().equals(descriptor) && role.getCMRField()!=null) {
                found = true;
                if (!runIndividualCmrTest(descriptor, role, c, result)) {
                    oneFailed = true;
                }
                }
            }
		}
		if (oneFailed) 
		    result.setStatus(Result.FAILED);
		else if (found == false) {
		     result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldTest.notApplicable",
				  "Not Applicable : The EJB has no CMR fields declared",
				  new Object[] {})); 
		}
		else 
		    result.setStatus(Result.PASSED);
	    }
	    else { 
		 result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				 ("com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp2.CmrFieldTest.notApplicable",
				  "Not Applicable : The EJB has no CMR fields declared",
				  new Object[] {})); 
	    } 
	}
        return result;
    }   
}
