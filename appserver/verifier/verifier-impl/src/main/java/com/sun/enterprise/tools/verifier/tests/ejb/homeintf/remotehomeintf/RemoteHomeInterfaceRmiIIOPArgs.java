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

package com.sun.enterprise.tools.verifier.tests.ejb.homeintf.remotehomeintf;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import com.sun.enterprise.tools.verifier.tests.ejb.homeintf.HomeMethodTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.lang.reflect.Method;

/** 
 * Enterprise beans home interface test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * The methods defined in this interface must follow the rules for RMI-IIOP. 
 * This means that their arguments must be of valid types for RMI-IIOP
 * 
 */
public class RemoteHomeInterfaceRmiIIOPArgs extends HomeMethodTest { 
 /**
     * <p>
     * run an individual verifier test against a declared method of the 
     * local interface.
     * </p>
     * 
     * @param descriptor the deployment descriptor for the bean
     * @param method the method to run the test on
     */
 protected void runIndividualHomeMethodTest(Method method,EjbDescriptor descriptor, Result result) {
     
     Class[] methodParameterTypes = method.getParameterTypes();
     ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
     // The methods arguments types must be legal types for
     // RMI-IIOP.  This means that their arguments
     // must be of valid types for RMI-IIOP,
     if (RmiIIOPUtils.isValidRmiIIOPParameters(methodParameterTypes)) {
         // these method parameters are valid, continue
         addGoodDetails(result, compName);
         result.passed(smh.getLocalString
                 (getClass().getName() + ".passed",
                 "[ {0} ] properly declares method with valid RMI-IIOP parameter.",
                 new Object[] {method.getDeclaringClass().getName()}));
     } else {
         addErrorDetails(result, compName);
         result.failed(smh.getLocalString
                 (getClass().getName() + ".failed",
                 "Error: [ {0} ] method was found, but does not have valid RMI-IIOP " +
                 "method parameters.",
                 new Object[] {method.getName()}));
     }
 }

    protected String getHomeInterfaceName(EjbDescriptor descriptor) {
	return descriptor.getRemoteClassName();
    }
    
    protected String getInterfaceType() {
	return "remote";
    }

    protected String getSuperInterface() {
	return "javax.ejb.EJBHome";
    }
}
