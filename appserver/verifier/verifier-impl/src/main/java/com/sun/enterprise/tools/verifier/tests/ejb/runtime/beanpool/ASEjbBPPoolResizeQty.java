/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

/** ejb [0,n]
 *    bean-pool ?
 *        steady-pool-size ? [String]
 *        resize-quantity ? [String]
 *        max-pool-size ? [String]
 *        pool-idle-timeout-in-seconds ? [String]
 *        max-wait-time-in-millis ? [String]
 *
 * The resize-quantity specifies the number of beans to be created if the
 * pool is empty
 *
 * valid values are o tp MAX_INT
 *
 *
 * @author Irfan Ahmed
 */
public class ASEjbBPPoolResizeQty extends ASEjbBeanPool
{

    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String pool = null;
        String poolResizeQty = null;
        String maxPoolSize = null;
        boolean oneFailed = false;

        try{
            pool = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-pool");
            if (pool!=null)
            {
                poolResizeQty = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-pool/resize-quantity");
                try{
                    if (poolResizeQty!=null)
                    {
                        poolResizeQty = poolResizeQty.trim();
                        if (poolResizeQty.length()==0)
                        {
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName()+".failed",
                                "FAILED [AS-EJB bean-pool] : resize-quantity cannot be empty"));
                        }else
                        {
                            int resizeQtyVal = Integer.parseInt(poolResizeQty);
                            if (resizeQtyVal < 0  || resizeQtyVal > Integer.MAX_VALUE)
                            {
                                addErrorDetails(result, compName);
                                result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                        "FAILED [AS-EJB bean-pool] : resize-quantity cannot be [ {0} ]. It should be between 0 and {1}",
                                        new Object[]{new Integer(poolResizeQty),new Integer(Integer.MAX_VALUE)}));
                            }else
                            {
                                int poolSizeVal=0;
                                maxPoolSize = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-pool/max-pool-size");
                                if (maxPoolSize!=null)
                                {
                                    try{
                                        poolSizeVal = Integer.parseInt(maxPoolSize);
                                    }catch(NumberFormatException nfe){
                                        oneFailed = true;
                                        addErrorDetails(result, compName);
                                        result.failed(smh.getLocalString(getClass().getName()+".failed2",
                                            "FAILED [AS-EJB bean-pool] : The value [ {0} ] for max-pool-size is not a valid Integer number",new Object[]{maxPoolSize}));

                                    }
                                    if (!oneFailed){
                                        if (resizeQtyVal <= poolSizeVal)
                                        {
                                            addGoodDetails(result, compName);
                                            result.passed(smh.getLocalString(getClass().getName()+".passed",
                                                "PASSED [AS-EJB bean-pool] : resize-quantity is [ {0} ] and is less-than/equal to max-pool-size[{1}]",
                                                new Object[]{new Integer(poolResizeQty), new Integer(maxPoolSize)}));
                                        }
                                        else
                                        {
                                            addWarningDetails(result, compName);
                                            result.warning(smh.getLocalString(getClass().getName()+".warning",
                                                "WARNING [AS-EJB bean-pool] : resize-quantity [ {0} ] is greater than max-pool-size [{1}]",new Object[]{new Integer(poolResizeQty), new Integer(maxPoolSize)}));
                                        }
                                    }
                                }else
                                {
                                    addGoodDetails(result, compName);
                                    result.passed(smh.getLocalString(getClass().getName()+".passed1",
                                            "PASSED [AS-EJB bean-pool] : resize-quantity is [ {0} ]", new Object[]{new Integer(poolResizeQty)}));
                                }
                            }
                        }
                    }else // if resize-quantity not defined
                    {
                        addNaDetails(result, compName);
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                                "NOT APPLICABLE [AS-EJB bean-pool] : resize-quantity element not defined"));
                    }
                }catch(NumberFormatException nfex){
                    Verifier.debug(nfex);
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName()+".failed3",
                            "FAILED [AS-EJB bean-pool] : The value [ {0} ] for resize-quantity is not a valid Integer number",
                            new Object[]{poolResizeQty}));
                }
            }else // if bean-pool is not defined
            {
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable1",
                        "NOT APPLICABLE [AS-EJB] : bean-pool element not defined"));
            }
        }catch(Exception ex)
        {
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                 (getClass().getName() + ".notRun",
                  "NOT RUN [AS-EJB] : Could not create the descriptor object"));
        }
        return result;
    }
}
