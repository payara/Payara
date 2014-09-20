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

import com.sun.enterprise.deployment.RoleReference;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.security.common.Role;

import java.util.Iterator;
import java.util.Set;

/**
 * Security role references test.
 * The Bean provider must declare all of the enterprise's bean references 
 * to security roles as specified in section 15.2.1.3 of the Moscone spec.
 * Role names must be mapped to names within the jar.
 */
public class SecurityRolesRefs extends EjbTest implements EjbCheck { 


    /** 
     * Security role references test.
     * The Bean provider must declare all of the enterprise's bean references
     * to security roles as specified in section 15.2.1.3 of the Moscone spec.
     * Role names must be mapped to names within the jar.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	if ((descriptor instanceof EjbEntityDescriptor) ||
	    (descriptor instanceof EjbSessionDescriptor)) {
        
	    // RULE: Role names must be mapped to names within the ejb-jar
	    Set roleReferences = descriptor.getRoleReferences();
	    Iterator roleRefsIterator = roleReferences.iterator();
	    EjbBundleDescriptorImpl bundleDescriptor = descriptor.getEjbBundleDescriptor();
	    Set roles = bundleDescriptor.getRoles();
	    Iterator roleIterator = roles.iterator();
	    Role role = null;
	    RoleReference roleReference = null;
	    boolean found = false;
	    boolean oneFailed = false;
      
	    if (roleRefsIterator.hasNext()) {
		while (roleRefsIterator.hasNext()) {
		    found = false;
		    roleReference = (RoleReference)roleRefsIterator.next();

		    while (roleIterator.hasNext()) {
			role = (Role)roleIterator.next();
			if (role.getName().equals(roleReference.getValue())) {
			    found = true;
			    //reset this so next time it drop back into here
			    roleIterator = roles.iterator();
			    break;
			}
		    }

		    if (!found) {
			// print the roleReference with no corresponding env-prop
			result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
			result.addErrorDetails(smh.getLocalString
					       (getClass().getName() + ".failed",
						"Erro: The security role reference [ {0} ] has no corresponding linked security role name [ {1} ]",
						new Object[] {roleReference.getName(),roleReference.getValue()}));
			if (!oneFailed) {
			    oneFailed = true;
			}
		    } else {      
			result.addGoodDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
			result.addGoodDetails(smh.getLocalString
					      (getClass().getName() + ".passed",
					       "The security role reference [ {0} ] has corresponding linked security role name [ {1} ]",
					       new Object[] {roleReference.getName(),roleReference.getValue()}));
		    }
		}
	    } else { 
		result.addNaDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
		result.notApplicable(smh.getLocalString
				     (getClass().getName() + ".notApplicable1",
				      "There are no role references within this bean [ {0} ]",
				      new Object[] {descriptor.getName()}));
		return result;
	    }

	    // if one of 'em failed reset the status appropriately, in case
	    // status got stomped on within the while loop by the next env-prop
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }

	    return result;
	} else {
	    result.addNaDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "[ {0} ] not called \n with a Session or Entity bean.",
				  new Object[] {getClass()}));
	    return result;
	}
    }
}
