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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

/** 
 * ejb-jar file must contain the java class file of the enterprise bean 
 * implementation class, and any of the classes that it depends on.
 */
public class JarFileContainsProperEJBClasses extends EjbTest implements EjbCheck { 



    /** 
     * ejb-jar file must contain the java class file of the enterprise bean 
     * implementation class, and any of the classes that it depends on.
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor 
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	try {
	    VerifierTestContext context = getVerifierContext();
        Class c = Class.forName(descriptor.getEjbClassName(), false,
                             getVerifierContext().getClassLoader());
            // if we are dealing with a CMP2 entity bean, the class is abstract..
            if (descriptor instanceof EjbEntityDescriptor) {
	        String persistentType =
		    ((EjbEntityDescriptor)descriptor).getPersistenceType();
	        if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistentType)) {
                    if (EjbCMPEntityDescriptor.CMP_1_1!=((EjbCMPEntityDescriptor) descriptor).getCMPVersion()) {
			result.addGoodDetails(smh.getLocalString
					      ("tests.componentNameConstructor",
					       "For [ {0} ]",
					       new Object[] {compName.toString()}));

		        result.passed(smh.getLocalString
				      (getClass().getName() + ".passed",
				       "Bean class [ {0} ] exists and it's supporting classes exist.",
				       new Object[] {descriptor.getEjbClassName()}));
        	        return result;
                    }
                }
            }

            try {
		c.newInstance();
		result.addGoodDetails(smh.getLocalString
				      ("tests.componentNameConstructor",
				       "For [ {0} ]",
				       new Object[] {compName.toString()}));

		result.passed(smh.getLocalString
			      (getClass().getName() + ".passed",
			       "Bean class [ {0} ] exists and it's supporting classes exist.",
			       new Object[] {descriptor.getEjbClassName()}));
	    } catch (InstantiationException e) {
		Verifier.debug(e);
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));

		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException",
			       "Error: Could not instantiate [ {0} ] within bean [ {1} ]",
			       new Object[] {descriptor.getEjbClassName(),descriptor.getName()}));
	    } catch (IllegalAccessException e) {
		Verifier.debug(e);
		result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));

		result.failed(smh.getLocalString
			      (getClass().getName() + ".failedException1",
			       "Error: Illegal Access while trying to instantiate [ {0} ] within bean [ {1} ]",
			       new Object[] {descriptor.getEjbClassName(),descriptor.getName()}));
	    }
	} catch (ClassNotFoundException e) {
	    Verifier.debug(e);
	    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    
	    result.failed(smh.getLocalString
			  (getClass().getName() + ".failedException2",
			   "Error: Can't find class [ {0} ] within bean [ {1} ]",
			   new Object[] {descriptor.getEjbClassName(),descriptor.getName()}));
        } catch (Throwable t) {
	    Verifier.debug(t);
	    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
	    
            result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "Not Applicable: [ {0} ] class encountered [ {1} ]. Cannot create instance of class [ {2} ] becuase [ {3} ] is not accessible within [ {4} ].",
				  new Object[] {(descriptor).getEjbClassName(),t.toString(), descriptor.getEjbClassName(), t.getMessage(), descriptor.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri()}));
	}
	return result;
    }
}
