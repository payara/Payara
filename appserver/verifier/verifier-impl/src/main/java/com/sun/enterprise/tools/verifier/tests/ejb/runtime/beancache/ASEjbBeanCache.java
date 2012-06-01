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

 package com.sun.enterprise.tools.verifier.tests.ejb.runtime.beancache;


import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.BeanCacheDescriptor;


/** ejb [0,n]
 *    bean-cache ?
 *        max-cache-size ? [String]
 *        is-cache-overflow-allowed ? [String]
 *        cache-idle-timout-in-seconds ? [String]
 *        removal-timeout-in-seconds ? [String]
 *        victim-selection-policy ? [String]
 *
 * The bean-cache element specifies the bean cache properties for the bean.
 * This is valid only for entity beans and stateful session beans
 * @author Irfan Ahmed
 */

public class ASEjbBeanCache extends EjbTest implements EjbCheck { 

    public BeanCacheDescriptor beanCache;
    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
	    ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String beanCache = null;
        try{
            beanCache = getXPathValue("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/bean-cache");
            if(beanCache!=null)
            {
                if(descriptor instanceof EjbEntityDescriptor
                        || (descriptor instanceof EjbSessionDescriptor
                        && ((EjbSessionDescriptor)descriptor).getSessionType().equals(EjbSessionDescriptor.STATEFUL)))
                {
                    addGoodDetails(result, compName);
                    result.passed(smh.getLocalString(getClass().getName()+".passed",
                                "PASSED [AS-EJB ejb] : bean-cache Element parsed"));
                }
                else
                {
                    addWarningDetails(result, compName);
                    result.warning(smh.getLocalString(getClass().getName()+".warning1",
                            "WARNING [AS-EJB ejb] : bean-cache should be defined only for Stateful Session and Entity Beans"));
                }
            }
            else
            {
                addNaDetails(result, compName);
                result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB ejb] : bean-cache element not defined"));
            }
        }catch(Exception ex)
        {
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                    (getClass().getName() + ".notRun",
                            "NOT RUN [AS-EJB] : Could not get a beanCache object"));
        }
        return result;
    }
}
