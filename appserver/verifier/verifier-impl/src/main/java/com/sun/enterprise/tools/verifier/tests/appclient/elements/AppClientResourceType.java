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

package com.sun.enterprise.tools.verifier.tests.appclient.elements;

import com.sun.enterprise.tools.verifier.tests.appclient.AppClientTest;
import com.sun.enterprise.tools.verifier.tests.appclient.AppClientCheck;
import java.util.*;
import java.util.logging.Level;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;


/**
 * The application client resource-type element specifies the Java class type
 * of the data source.
 */
public class AppClientResourceType extends AppClientTest implements AppClientCheck {

    /**
     * The application client resource-type element specifies the Java class type
     * of the data source.
     *
     * @param descriptor the Application client deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(ApplicationClientDescriptor descriptor) {

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

	if (!descriptor.getResourceReferenceDescriptors().isEmpty()) {
	    boolean oneFailed = false;
	    boolean foundIt = false;
	    Set resources = descriptor.getResourceReferenceDescriptors();
	    Iterator itr = resources.iterator();
	    // get the res-ref's in this .ear
	    while(itr.hasNext()) {
		foundIt = false;
		ResourceReferenceDescriptor next = (ResourceReferenceDescriptor) itr.next();
		String resType = next.getType();
                logger.log(Level.FINE, "servlet resType: " + resType);
		if ((resType.equals("javax.sql.DataSource")) ||
		    (resType.equals("javax.jms.QueueConnectionFactory")) ||
		    (resType.equals("javax.jms.TopicConnectionFactory")) ||
		    (resType.equals("javax.jms.ConnectionFactory")) ||
		    (resType.equals("javax.mail.Session")) ||
		    (resType.equals("java.net.URL"))) {
		    foundIt = true;
		} else {
		    foundIt = false;
                    String specVerStr = descriptor.getSpecVersion();
                    double specVer = 0;
                    specVer = (Double.valueOf(specVerStr)).doubleValue();
                    if (Double.compare(specVer, 1.4) >= 0) {
                      // with J2EE 1.4, resource-ref can be any userdefined type
                      foundIt = true;
                    }
		}

		if (foundIt) {
		    result.addGoodDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
		    result.addGoodDetails(smh.getLocalString
					  (getClass().getName() + ".passed",
					   "The resource-type [ {0} ] element specifies the Java class type of the data source within application client [ {1} ]",
					   new Object[] {resType, descriptor.getName()}));
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
					    "Error: The resource-type [ {0} ] element does not specify valid Java class type of the data source within application client [ {1} ]",
					    new Object[] {resType, descriptor.getName()}));
		}
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
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
				  "There are no resource-type elements within the application client [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
