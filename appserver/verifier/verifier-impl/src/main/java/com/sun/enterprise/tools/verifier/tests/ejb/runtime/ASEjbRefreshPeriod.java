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
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;


/** ejb [0,n]
 *    refresh-period-in-seconds ? [String]
 *
 * The refresh-period-in-seconds denotes the rate at which a read-only-bean
 * is refreshed.
 * Is applicable only if it is a ROB.
 * The value should be between 0 and MAX_INT
 * @author
 */
public class ASEjbRefreshPeriod extends EjbTest implements EjbCheck {

    public Result check(EjbDescriptor descriptor)
    {
	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        boolean oneWarning = false;
        boolean oneFailed = false;
        boolean isReadOnly = false;
        String refreshPeriod = null;
        try{
            String s1 = ("/sun-ejb-jar/enterprise-beans/ejb[ejb-name=\""+descriptor.getName()+"\"]/refresh-period-in-seconds");
            refreshPeriod = getXPathValue(s1);
            
            IASEjbExtraDescriptors iasEjbExtraDesc = descriptor.getIASEjbExtraDescriptors();
            isReadOnly = iasEjbExtraDesc.isIsReadOnlyBean();
            
            if(refreshPeriod!=null)
            {
                refreshPeriod=refreshPeriod.trim();
                if(refreshPeriod.length()==0)
                {
                    oneFailed = true;
                    result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                    result.failed(smh.getLocalString(getClass().getName()+".failed",
                        "FAILED [AS-EJB ejb] : refresh-period-in-seconds is invalid. It should be between 0 and " + Integer.MAX_VALUE));
                }else{
                    if(!(descriptor instanceof EjbEntityDescriptor
                    && isReadOnly)) 
                    {
                            oneWarning = true;
                            result.addWarningDetails(smh.getLocalString
                                       ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                            result.warning(smh.getLocalString(getClass().getName()+".warning",
                            "WARNING [AS-EJB ejb] : refresh-period-in-seconds should be defined for Read Only Beans."));
                        return result;
                    }
                    try{
                        int refValue = Integer.parseInt(refreshPeriod);
                        if(refValue<0 || refValue>Integer.MAX_VALUE)
                        {
                            oneFailed = true;
                            result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                            result.failed(smh.getLocalString(getClass().getName()+".failed1",
                                "FAILED [AS-EJB ejb] : refresh-period-in-seconds cannot be greater than " + Integer.MAX_VALUE + " or less than 0"));
                        }
                        else
                            result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                            result.passed(smh.getLocalString(getClass().getName()+".passed",
                                "PASSED [AS-EJB ejb] : refresh-period-in-seconds is {0}",new Object[]{new Integer(refValue)}));
                    }catch(NumberFormatException nfex){
                        oneFailed = true;
                        Verifier.debug(nfex);
                        result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString(getClass().getName()+".failed",
                            "FAILED [AS-EJB ejb] : refresh-period-in-seconds is invalid. It should be between 0 and " + Integer.MAX_VALUE ));
                    }
                }
            }else {
                if((descriptor instanceof EjbEntityDescriptor)
                        && (isReadOnly))
                {
                    oneWarning = true;
                    result.addWarningDetails(smh.getLocalString
                                       ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                    result.warning(smh.getLocalString(getClass().getName()+".warning",
                    "WARNING [AS-EJB ejb] : refresh-period-in-seconds should be defined for Read Only Beans"));
                }
                else
                {
                    result.addNaDetails(smh.getLocalString
				("tests.componentNameConstructor",
				 "For [ {0} ]",
				 new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString(getClass().getName()+".notApplicable",
                        "NOT APPLICABLE [AS-EJB ejb] : refresh-period-in-seconds is not defined"));
                }
            }
            if(oneWarning)
                result.setStatus(Result.WARNING);
            if(oneFailed)
                result.setStatus(Result.FAILED);
        }catch(Exception ex){
            result.addErrorDetails(smh.getLocalString
				   ("tests.componentNameConstructor",
				    "For [ {0} ]",
				    new Object[] {compName.toString()}));
            result.failed(smh.getLocalString(getClass().getName()+".notRun",
                "NOT RUN [AS-EJB cmp] Could not create descriptor Object."));
        }
        return result;
    }
    }
