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

/*
 * CmpEjbNoInvalidCreateMethod.java
 *
 * Created on November 13, 2001, 9:36 AM
 */

package com.sun.enterprise.tools.verifier.tests.ejb.entity.cmp;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;
/**
 * Per Ejb 2.0 spec, $14.1.8 create<METHOD> are not supported by 
 * CMP 1.1. EJBs.
 *
 * @author  Jerome Dochez
 * @version 
 */
public class CmpEjbNoInvalidCreateMethod extends EjbTest implements EjbCheck { 


    /** 
     * Entity beans with CMP 1.1 must not define create<METHOD>
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

	Result result = getInitializedResult();
        try {

	if (descriptor instanceof EjbCMPEntityDescriptor) {
	    String persistence =
		((EjbEntityDescriptor)descriptor).getPersistenceType();
	    if (EjbEntityDescriptor.CONTAINER_PERSISTENCE.equals(persistence) &&
                   ((EjbCMPEntityDescriptor)descriptor).getCMPVersion()==EjbCMPEntityDescriptor.CMP_1_1) {

                try {
		    Class c = Class.forName(descriptor.getHomeClassName(), false, getVerifierContext().getClassLoader());
		    Method [] methods = c.getDeclaredMethods();
                    boolean oneFailed = false;
                    for (int i=0;i<methods.length;i++) {
                        Method aMethod = methods[i];
                        if (aMethod.getName().startsWith("create")) {
                            if (!aMethod.getName().endsWith("create")) {
				    result.addErrorDetails(smh.getLocalString
							   (getClass().getName() + ".failed",
							    "CMP 1.1 entity beans are not authorized to define [ {0} ] method",
							    new Object[] {aMethod.getName()}));
                                     oneFailed = true;
                            } 
                        }
                    }
                    if (oneFailed) {
		        result.setStatus(Result.FAILED);
                    } else {
                        result.passed(smh.getLocalString
					(getClass().getName() + ".passed",
                                        "No create<METHOD> defined for this CMP 1.1 entity bean [ {0} ] ",
					 new Object[] {descriptor.getName()}));
                    }
                    return result;
                } catch(ClassNotFoundException cnfe) {
		    result.failed(smh.getLocalString
				  (getClass().getName() + ".failedException",
				   "Error: [ {0} ] class not found.",
				   new Object[] {descriptor.getHomeClassName()}));
                    return result;
                } 
            } 
        }
        } catch(Exception e) {
            e.printStackTrace();
        }
        result.notApplicable(smh.getLocalString
                             (getClass().getName() + ".notApplicable",
                              "[ {0} ] is not a CMP 1.1 Entity Bean.",
                              new Object[] {descriptor.getName()}));
       return result;
    }                        
}
