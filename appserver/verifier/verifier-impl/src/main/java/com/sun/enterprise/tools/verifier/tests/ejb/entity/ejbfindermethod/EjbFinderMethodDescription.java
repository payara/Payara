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

package com.sun.enterprise.tools.verifier.tests.ejb.entity.ejbfindermethod;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** 
 * Note that the ejbFind<METHOD> names and parameter signatures do not 
 * provide the container tools with sufficient information for automatically 
 * generating the implementation of the finder methods for methods other than 
 * ejbFindByPrimaryKey. Therefore, the bean provider is responsible for 
 * providing a description of each finder method. The entity bean Deployer 
 * uses container tools to generate the implementation of the finder methods 
 * based in the description supplied by the bean provider. The Enterprise 
 * JavaBeans architecture does not specify the format of the finder method 
 * description.
 */
public class EjbFinderMethodDescription extends EjbTest implements EjbCheck { 


    /**
     * Note that the ejbFind<METHOD> names and parameter signatures do not 
     * provide the container tools with sufficient information for automatically 
     * generating the implementation of the finder methods for methods other than 
     * ejbFindByPrimaryKey. Therefore, the bean provider is responsible for 
     * providing a description of each finder method. The entity bean Deployer 
     * uses container tools to generate the implementation of the finder methods 
     * based in the description supplied by the bean provider. The Enterprise 
     * JavaBeans architecture does not specify the format of the finder method 
     * description.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();

	// Stub test class placeholder
	// fill in guts/logic - pass/fail accordingly in future
	result.setStatus(Result.NOT_IMPLEMENTED);
	result.addNaDetails
	    (smh.getLocalString
	     (getClass().getName() + ".notImplemented",
	      "No static testing done - yet."));
	return result;
    }
}
