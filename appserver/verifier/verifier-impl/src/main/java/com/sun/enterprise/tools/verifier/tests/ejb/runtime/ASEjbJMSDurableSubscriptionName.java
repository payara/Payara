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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;


/** ejb [0,n]
 *    jms-durable-subscription-name ? [String]
 *
 * The jms-durable-subscription-name is the name of a durable subscription associated
 * with a message-driven bean.
 * The name should not be a null string.
 * The value is required if destination-type is Topic
 * and subscription-durability is durable
 * @author Irfan Ahmed
 */
public class ASEjbJMSDurableSubscriptionName extends EjbTest implements EjbCheck { 

    public Result check(EjbDescriptor descriptor)
    {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneFailed = false;
        try{
            EjbMessageBeanDescriptor msgBeanDesc = (EjbMessageBeanDescriptor)descriptor;
            String jmsDurableName = msgBeanDesc.getDurableSubscriptionName();
            if(jmsDurableName != null){
                if(jmsDurableName.length()==0)
                {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName()+".failed",
                        "FAILED [AS-EJB ejb] : jms-durable-subscription-name cannot be an empty string value"));
                }else{
                    addGoodDetails(result, compName);
                    result.passed(smh.getLocalString(getClass().getName()+".passed",
                        "PASSED [AS-EJB ejb] : jms-durable-subscription-name is {0}", new Object[]{jmsDurableName}));
                }
            }else{
                if(descriptor instanceof EjbMessageBeanDescriptor){
                    if(msgBeanDesc.hasTopicDest() && msgBeanDesc.hasDurableSubscription()){
                        boolean failed = false;
                        int count = getCountNodeSet("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter");
                        if (count > 0) {
                            String value = getXPathValue("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/resource-adapter-mid");
                            if(value==null || value.length()==0){
                                failed = true;
                            }
                        } 
                        else {
                            failed = true;
                        }
                        if (failed) {
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                "FAILED [AS-EJB ejb] : jms-durable-subscription-name should be defined for an MDB with"+
                                " destination type Topic and Durable subscription type"));
                        }
                    }else{
                        addNaDetails(result, compName);
                        result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                            "NOT APPLICABLE [AS-EJB ejb] : jms-durable-subscription-name element is not defined"));
                    }
                }else{
                    addNaDetails(result, compName);
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB ejb] : jms-durable-subscription-name element is not defined"));
                }
            }
        }catch(Exception ex){
            addErrorDetails(result, compName);
            result.addErrorDetails(smh.getLocalString
                (getClass().getName() + ".notRun",
                "NOT RUN [AS-EJB] : Could not create a descriptor object"));
        }
        return result;
    }
}
