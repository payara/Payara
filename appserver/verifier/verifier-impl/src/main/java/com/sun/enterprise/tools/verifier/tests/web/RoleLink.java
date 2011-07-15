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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;
import java.util.*;
import java.util.logging.Level;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;


/**
 * The role-link element is used to link a security role reference to a
 * defined security role.  The role-link element must contain the name of
 * one of the security roles defined in the security-role elements.
 */
public class RoleLink extends WebTest implements WebCheck {

    /**
     * The role-link element is used to link a security role reference to a
     * defined security role.  The role-link element must contain the name of
     * one of the security roles defined in the security-role elements.
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (!descriptor.getWebComponentDescriptors().isEmpty()) {
	    boolean oneFailed = false;
            int na = 0;
            int noWd = 0;
	    for (WebComponentDescriptor next : descriptor.getWebComponentDescriptors()) {
                noWd++;
		boolean foundIt = false;
		// get the security role-link's in this .war
		if (next.getSecurityRoleReferences().hasMoreElements()) {
		    for (Enumeration ee = next.getSecurityRoleReferences(); ee.hasMoreElements();) {
			RoleReference rr = (RoleReference) ee.nextElement();
			foundIt = false;
			String linkName = rr.getValue();
                        logger.log(Level.FINE, "servlet linkName: " + linkName);
			// now check to see if role-link exist in security role names
			if (descriptor.getSecurityRoles().hasMoreElements()) {
			    for (Enumeration eee = descriptor.getSecurityRoles(); eee.hasMoreElements();) {
				SecurityRoleDescriptor srdNext = (SecurityRoleDescriptor) eee.nextElement();

				if (linkName.equals(srdNext.getName())) {
				    foundIt = true;
				    break;
				} else {
				    continue;
				}
			    }
			} else {
			    // if descriptor.getSecurityRoles().hasMoreElements())
			    foundIt = false;
			}

			if (foundIt) {
			    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
			    result.addGoodDetails(smh.getLocalString
						  (getClass().getName() + ".passed",
						   "role-link [ {0} ] links security role reference to a defined security role within web application [ {1} ]",
						   new Object[] {linkName, descriptor.getName()}));
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
						    "Error: role-link [ {0} ] does not link security role reference to a defined security role within web application [ {1} ]",
						    new Object[] {linkName, descriptor.getName()}));
			}
		    } // for loop next.getSecurityRoleReferences() has more elements
		} else {
		    result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addNaDetails(smh.getLocalString
					(getClass().getName() + ".notApplicable1",
					 "[ {0} ] has no role-link element defined within the web archive [ {1} ]",
					 new Object[] {next.getName(),descriptor.getName()}));
                    na++;
		}
	    } // for loop descriptor.getWebComponentDescriptors(); e.hasMoreElements()
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
            } else if (na == noWd) {
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
				  "There are no location elements within the web archive [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
