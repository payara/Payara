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

package com.sun.enterprise.tools.verifier.tests.web;

import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import org.glassfish.web.deployment.descriptor.AuthorizationConstraintImpl;
import org.glassfish.web.deployment.descriptor.SecurityConstraintImpl;


/** 
 * The Web role-name element contains the name of a security role.
 */
public class WebSecurityRoleName extends WebTest implements WebCheck { 

    
    /** 
     * The Web role-name element contains the name of a security role.
     *
     * @param descriptor the Web deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (descriptor.getSecurityConstraints().hasMoreElements()) {
	    boolean oneFailed = false;
	    boolean foundIt = false;
            int naSr = 0;
            int naAci = 0;
            int noAci = 0;
            int noSc = 0;
	    // get the security role name's in this .war
	    for (Enumeration e = descriptor.getSecurityConstraints() ; e.hasMoreElements() ;) {
		foundIt = false;
                noSc++;
		SecurityConstraintImpl securityConstraintImpl = (SecurityConstraintImpl)
		    e.nextElement();
		AuthorizationConstraintImpl aci = (AuthorizationConstraintImpl) securityConstraintImpl.getAuthorizationConstraint();
		if (aci != null) {
                    noAci++;
                    if (aci.getSecurityRoles().hasMoreElements()) {
			for (Enumeration ee = aci.getSecurityRoles(); ee.hasMoreElements();) {
			    SecurityRoleDescriptor srd = (SecurityRoleDescriptor) ee.nextElement();
			    String roleName = srd.getName(); 
			    // jsb, nothing to test here...?
			    if (roleName.length() > 0) {
				foundIt = true;
			    } else {
				foundIt = false;
			    }
           
			    if (foundIt) {
				result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.addGoodDetails(smh.getLocalString
						      (getClass().getName() + ".passed",
						       "The security role name [ {0} ] found within web application [ {1} ]",
						       new Object[] {roleName, descriptor.getName()}));
			    } else {
				if (!oneFailed) {
				    oneFailed = true;
				}
				result.addErrorDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
				result.addErrorDetails(smh.getLocalString
						       (getClass().getName() + ".failed",
							"Error: The security role name [ {0} ] not found within web application [ {1} ]",
							new Object[] {roleName, descriptor.getName()}));
			    }
			}
		    } else {
			result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			result.notApplicable(smh.getLocalString
					     (getClass().getName() + ".notApplicable1",
					      "Not Applicable: There are no security roles in this security constraint within [ {0} ]",
					      new Object[] {descriptor.getName()}));                    naSr++;
		    }
                } else {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
                                         (getClass().getName() + ".notApplicable2",
                                          "Not Applicable: There is no authorization constraint in this security constraint within [ {0} ]",
                                          new Object[] {descriptor.getName()}));
                    naAci++;
                }
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else if ((noSc == naAci) || (noAci == naSr)) {
		result.setStatus(Result.NOT_APPLICABLE);
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
				  "There are no role-name elements within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
