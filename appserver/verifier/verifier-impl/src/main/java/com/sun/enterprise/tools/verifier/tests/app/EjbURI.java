/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.app;

import com.sun.enterprise.tools.verifier.tests.app.ApplicationTest;
import java.util.*;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;

/** 
 * The ejb element specifies the URI of a ejb-jar, relative to
 *  the top level of the application package.
 *
 */

public class EjbURI extends ApplicationTest implements AppCheck { 


    /** The ejb element specifies the URI of a ejb-jar, relative to
     *  the top level of the application package.
     *
     * @param descriptor the Application deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(Application descriptor) {

	Result result = getInitializedResult();

  
	if (descriptor.getBundleDescriptors(EjbBundleDescriptor.class).size() > 0) {
	    boolean oneFailed = false;
	    for (Iterator itr = descriptor.getBundleDescriptors(EjbBundleDescriptor.class).iterator(); itr.hasNext();) {
		EjbBundleDescriptor ejbd = (EjbBundleDescriptor) itr.next();
    

		// not sure what we can do to test this string?
		if (ejbd.getModuleDescriptor().getArchiveUri().endsWith(".jar")) {
		    result.passed
			(smh.getLocalString
			 (getClass().getName() + ".passed",
			  "[ {0} ] specifies the URI [ {1} ] of an ejb-jar, relative to the top level of the application package [ {2} ].",
			  new Object[] {ejbd.getName(), ejbd.getModuleDescriptor().getArchiveUri(), descriptor.getName()}));
		} else {
		    if (!oneFailed) {
			oneFailed =true;
		    }
		    result.addErrorDetails
			(smh.getLocalString
			 (getClass().getName() + ".failed",
			  "Error: [ {0} ] does not specify the URI [ {1} ] of an ejb-jar, relative to the top level of the application package [ {2} ], or does not end with \".jar\"",
			  new Object[] {ejbd.getName(), ejbd.getModuleDescriptor().getArchiveUri(), descriptor.getName()}));
		}
	    }
	    if (oneFailed) {
		result.setStatus(Result.FAILED);
	    } else {
		result.setStatus(Result.PASSED);
	    }
	} else {
	    result.notApplicable(smh.getLocalString
				 (getClass().getName() + ".notApplicable",
				  "There are no ejb components in application [ {0} ]",
				  new Object[] {descriptor.getName()}));
	}

	return result;
    }
}
