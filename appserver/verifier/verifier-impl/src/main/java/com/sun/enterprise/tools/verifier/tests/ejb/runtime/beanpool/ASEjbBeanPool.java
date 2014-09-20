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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime.beanpool;

import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;




/** ejb [0,n]
 *    bean-pool ?
 *        steady-pool-size ? [String]
 *        pool-resize-quantity ? [String]
 *        max-pool-size ? [String]
 *        pool-idle-timeout-in-seconds ? [String]
 *        max-wait-time-in-millis ? [String]
 *
 * The bean-pool element specifies the bean pool properties for the beans
 *
 * The bean-pool is valid only for Stateless Session Beans (SSB) and
 * Message-Driven Beans (MDB).
 * @author Irfan Ahmed
 */

public class ASEjbBeanPool extends EjbTest implements EjbCheck {

    
    public BeanPoolDescriptor beanPool;
    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
    	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        IASEjbExtraDescriptors ejbJar = descriptor.getIASEjbExtraDescriptors();
        if(ejbJar!=null)
        {
            try
            {
                beanPool = ejbJar.getBeanPool();
                if(beanPool!=null)
                {
                    if(descriptor instanceof EjbSessionDescriptor && ((EjbSessionDescriptor)descriptor).getSessionType().equals(EjbSessionDescriptor.STATEFUL))
                    {
                        result.addWarningDetails(smh.getLocalString
                                      ("tests.componentNameConstructor",
                                       "For [ {0} ]",
                                       new Object[] {compName.toString()}));
                        result.warning(smh.getLocalString(getClass().getName()+".warning",
                            "WARNING [AS-EJB ejb] : bean-pool should be defined for Stateless Session Beans, Entity Beans or Message Driven Beans"));
                    }else if(descriptor instanceof EjbMessageBeanDescriptor || (descriptor instanceof EjbSessionDescriptor && ((EjbSessionDescriptor)descriptor).getSessionType().equals(EjbSessionDescriptor.STATELESS)) || descriptor instanceof EjbEntityDescriptor){
                        result.addGoodDetails(smh.getLocalString
                                      ("tests.componentNameConstructor",
                                       "For [ {0} ]",
                                       new Object[] {compName.toString()}));
                        result.passed(smh.getLocalString(getClass().getName()+".passed",
                            "PASSED [AS-EJB ejb] : bean-pool is correctly defined"));                
                    
                    }
                }
                else
                {
                    result.addNaDetails(smh.getLocalString
                                  ("tests.componentNameConstructor",
                                   "For [ {0} ]",
                                   new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB ejb] : bean-pool element not defined"));
                }
                    return result;
            }catch(Exception ex)
            {
                result.addErrorDetails(smh.getLocalString
                                       ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                result.addErrorDetails(smh.getLocalString
                        (getClass().getName() + ".notRun",
                            "NOT RUN [AS-EJB] : Could not create a beanPool object"));
            }
        }
        else
        {
            result.addErrorDetails(smh.getLocalString
                                   ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
            result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create an IASEjbExtraDescriptor object"));
        }
        return result;
    }    
}
        
