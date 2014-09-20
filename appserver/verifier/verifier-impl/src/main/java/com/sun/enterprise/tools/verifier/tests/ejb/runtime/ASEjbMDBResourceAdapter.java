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

/** enterprise-beans
 *   ejb [1,n]
 *     mdb-resource-adapter ?
 *       resource-adapter-mid  [String]
 *       activation-config ?
 *         description ?  [String]
 *         activation-config-property +
 *           activation-config-property-name  [String]
 *           activation-config-property-value  [String]
 *
 * This is the name of the enterprise java bean.
 * @author
 */
public class ASEjbMDBResourceAdapter extends EjbTest implements EjbCheck {

    /**
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        boolean oneFailed = false;
	    Result result = getInitializedResult();
	    ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String value=null;
        int count = 0;
        try{
            count = getCountNodeSet("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter");
            if (count>0){
                value = getXPathValue("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/resource-adapter-mid");
                if(value==null || value.length()==0){
                    oneFailed=true;
                    result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString
                        (getClass().getName() + ".failed1",
                        "FAILED [AS-EJB mdb-resource-adapter] : resource-adapter-mid cannot be empty.",
                        new Object[] {descriptor.getName()}));
                }else{
                    result.addGoodDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                    result.passed(smh.getLocalString(
                                    getClass().getName() + ".passed1",
                        "PASSED [AS-EJB mdb-resource-adapter] : resource-adapter-mid is {1}",
                        new Object[] {descriptor.getName(),value}));
                }
                //activation-config
                count = getCountNodeSet("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/activation-config");
                if (count>0){
                    count = getCountNodeSet("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/activation-config/activation-config-property");
                    if (count>0){
                        for (int i=1;i<=count;i++){
                            value = getXPathValue("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/activation-config/activation-config-property/activation-config-property-name");
                            if(value==null || value.length()==0){
                                oneFailed=true;
                                result.addErrorDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                                result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed2",
                                    "FAILED [AS-EJB mdb-resource-adapter] : activation-config-property-name cannot be empty.",
                                    new Object[] {descriptor.getName()}));
                            }else{
                                result.addGoodDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                                result.passed(smh.getLocalString(
                                                getClass().getName() + ".passed2",
                                    "PASSED [AS-EJB mdb-resource-adapter] : activation-config-property-name is {1}",
                                    new Object[] {descriptor.getName(),value}));
                            }

                            value = getXPathValue("sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/mdb-resource-adapter/activation-config/activation-config-property/activation-config-property-value");
                            if(value==null || value.length()==0){
                                oneFailed=true;
                                result.addErrorDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                                result.failed(smh.getLocalString
                                    (getClass().getName() + ".failed3",
                                    "FAILED [AS-EJB mdb-resource-adapter] : activation-config-property-value cannot be empty.",
                                    new Object[] {descriptor.getName()}));
                            }else{
                                result.addGoodDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                                result.passed(smh.getLocalString(
                                                getClass().getName() + ".passed3",
                                    "PASSED [AS-EJB mdb-resource-adapter] : activation-config-property-value is {1}",
                                    new Object[] {descriptor.getName(),value}));
                            }
                        }
                    }else{
                        oneFailed=true;
                        result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString
                            (getClass().getName() + ".failed4",
                            "FAILED [AS-EJB mdb-resource-adapter] : activation-config-property is not defined",
                            new Object[] {descriptor.getName()}));
                    }
                }
            }else{
                    result.addNaDetails(smh.getLocalString
				        ("tests.componentNameConstructor",
				        "For [ {0} ]",
				        new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB ejb] : mdb-resource-adapter is not defined."));
            }
        }catch(Exception ex){
            oneFailed = true;
            result.addErrorDetails(smh.getLocalString
                (getClass().getName() + ".notRun",
                "NOT RUN [AS-EJB] : Could not create descriptor object"));
        }
        if(oneFailed)
            result.setStatus(Result.FAILED);
        return result;
    }
}
