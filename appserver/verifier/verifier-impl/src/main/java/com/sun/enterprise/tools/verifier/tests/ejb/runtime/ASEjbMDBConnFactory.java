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

import com.sun.enterprise.deployment.ResourcePrincipal;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;
import org.glassfish.ejb.deployment.descriptor.runtime.MdbConnectionFactoryDescriptor;

/** ejb [0,n]
 *    mdb-connection-factory ?
 *        jndi-name [String]
 *        default-resource-principal ?
 *            name [String]
 *            password [String]
 *
 * The mdb-connection-factory specifies the connection factory associated with
 * an MDB
 * @author
 */
public class ASEjbMDBConnFactory extends EjbTest implements EjbCheck { 

    public Result check(EjbDescriptor descriptor)
    {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneFailed = false;
        boolean oneWarn = false;
        try{
            IASEjbExtraDescriptors iasEjbExtraDesc = descriptor.getIASEjbExtraDescriptors();
            MdbConnectionFactoryDescriptor mdbConnFacDesc = iasEjbExtraDesc.getMdbConnectionFactory();

            if(mdbConnFacDesc != null){
                String jndiName = mdbConnFacDesc.getJndiName();
                if(jndiName == null || jndiName.length()==0){
                    oneFailed = true;
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString(getClass().getName()+".failed",
                        "FAILED [AS-EJB mdb-connection-factory] : jndi-name cannot be an empty string"));
                }else{
                    if(jndiName.startsWith("jms/")){
                        addGoodDetails(result, compName);
                        result.passed(smh.getLocalString(getClass().getName()+".passed",
                            "PASSED [AS-EJB mdb-connection-factory] : jndi-name is {0}",new Object[]{jndiName}));
                    }else{
                        oneWarn = true;
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString(getClass().getName()+".warning",
                            "WARNING [AS-EJB mdb-connection-factory] : jndi-name {0} should start with jms/",
                            new Object[]{jndiName}));
                    }
                }
                
                ResourcePrincipal defPrinci = mdbConnFacDesc.getDefaultResourcePrincipal();
                if(defPrinci != null){
                    String name = defPrinci.getName();
                    if(name == null || name.length()==0){
                        oneFailed = true;
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString(getClass().getName()+".failed1",
                            "FAILED [AS-EJB default-resource-principal] : name cannot be an empty string"));
                    }else{
                        addGoodDetails(result, compName);
                        result.passed(smh.getLocalString(getClass().getName()+".passed1",
                            "PASSED [AS-EJB default-resource-principal] : name is {0}",new Object[]{name}));
                    }

                    String password = defPrinci.getPassword();
                    if(password == null || password.length()==0)
                    {
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString(getClass().getName()+".warning2",
                            "WARNING [AS-EJB default-resource-principal] : password is an empty string"));
                    }else{
                        addGoodDetails(result, compName);
                        result.passed(smh.getLocalString(getClass().getName()+".passed2",
                            "PASSED [AS-EJB default-resource-principal] : password is  {0}",new Object[]{password}));
                    }
                }else{
                    addNaDetails(result, compName);
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB mdb-connection-factory] : default-resource-prncipal element is not defined"));
                }
            }else {
                if(descriptor instanceof EjbMessageBeanDescriptor){
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
                        EjbMessageBeanDescriptor mdbDesc = (EjbMessageBeanDescriptor)descriptor;
                        if(mdbDesc.hasTopicDest() && mdbDesc.hasDurableSubscription()){
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.failed(smh.getLocalString(getClass().getName()+".failed2",
                                "FAILED [AS-EJB ejb] : mdb-connection-factory has to be defined for an MDB with destination-type " + 
                                "as Topic and subscription-durability as Durable"));
                        }
                        /**
                        else{
                            oneWarn = true;
                            result.warning(smh.getLocalString(getClass().getName()+".warning1",
                                 "WARNING [AS-EJB ejb] : mdb-connection-factory should be defined for a Message Driven Bean"));
                        }
                        **/
                    }
                }else{
                    addNaDetails(result, compName);
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable1",
                        "NOT APPLICABLE [AS-EJB ejb] : mdb-connection-factory element is not defined"));
                }
            }
            if(oneFailed)
                result.setStatus(Result.FAILED);
            else if(oneWarn)
                result.setStatus(Result.WARNING);
        }catch(Exception ex){
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString(getClass().getName()+".notRun",
                "NOT RUN [AS-EJB cmp] Could not create descriptor Object."));
            
        }
        return result;
    }
}
