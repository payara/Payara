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

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.Verifier;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** ejb [0,n]
 *    bean-pool ?
 *        steady-pool-size ? [String]
 *        pool-resize-quantity ? [String]
 *        max-pool-size ? [String]
 *        pool-idle-timeout-in-seconds ? [String]
 *        max-wait-time-in-millis ? [String]
 *
 * The pool-idle-timeout-in-seconds specifies the maximum time for which a SSB or
 * an MDB is allowed to be idle in the bean pool.
 *
 * Valid value are 0 to MAX_LONG
 *
 *
 * @author Irfan Ahmed
 */

public class ASEjbBPPoolIdleTime extends ASEjbBeanPool
{

    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String pool = null;
        String poolIdleTimeout = null;
        try
        {
            pool = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-pool");
            if (pool!=null)
            {
                poolIdleTimeout = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-pool/pool-idle-timeout-in-seconds");
                try{
                    if (poolIdleTimeout!=null)
                    {
                        poolIdleTimeout =  poolIdleTimeout.trim();
                        if (poolIdleTimeout.length()==0)
                        {
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName()+".failed",
                                "FAILED [AS-EJB bean-pool] : pool-idle-timeout-in-seconds cannot be empty"));

                        }else
                        {
                            long value = new Integer(poolIdleTimeout).longValue();
                            if(value < 0  || value > Long.MAX_VALUE)
                            {
                                addErrorDetails(result, compName);
                                result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                    "FAILED [AS-EJB bean-pool] : pool-idle-timeout-in-seconds cannot be {0}. It should be between 0 and {1}",
                                    new Object[]{new Long(value),new Long(Long.MAX_VALUE)}));
                            }
                            else
                            {
                                addGoodDetails(result, compName);
                                result.passed(smh.getLocalString(getClass().getName()+".passed",
                                    "PASSED [AS-EJB bean-pool] : pool-idle-timeout-in-seconds is {0}",
                                    new Object[]{new Long(value)}));
                            }
                        }
                    }else // if pool-idle-timeout is not specified
                    {
                        addNaDetails(result, compName);
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                            "NOT APPLICABLE [AS-EJB bean-pool] : pool-idle-timeout-in-seconds element not defined"));
                    }
                }catch(NumberFormatException nfex){
                    Verifier.debug(nfex);
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName()+".failed2",
                        "FAILED [AS-EJB bean-pool] : The value [ {0} ] for pool-idle-timeout-in-seconds is not a valid Integer number",new Object[]{poolIdleTimeout}));


                }

            }else  //if bean-pool elememnt is not present
            {
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable1",
                    "NOT APPLICABLE [AS-EJB] : bean-pool element not defined"));
            }

        }catch (Exception ex){
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create the descriptor object"));
        }
        return result;
    }
}
