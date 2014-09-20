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

import com.sun.enterprise.deployment.xml.DTDRegistry;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbBundleDescriptorImpl;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.node.runtime.EjbBundleRuntimeNode;

/** DOCTYPE
 *
 * The ias ejb deployment descriptor has PUBLIC identifier with a PubidLiteral
 * of an acceptable type
 * @author Irfan Ahmed
 */
public class ASEjbJarPublicID extends EjbTest implements EjbCheck { 

    /** 
     * Ejb PUBLIC identifier test
     * The ejb deployment descriptor has PUBLIC identifier with a PubidLiteral 
     * of "-//Sun Microsystems, Inc.//DTD Enterprise JavaBeans 1.1//EN" 
     *
     * @param descriptor the Ejb deployment descriptor 
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
//EXCEPTION as descriptor.getDocType() returns null. Also how to differnetiate between sun-ejb-jar or ejb-jar DocType        

	Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
       
        if (!getVerifierContext().getisXMLBasedOnSchema()){
        try{
            String acceptablePubidLiterals[] = { DTDRegistry.SUN_EJBJAR_200_DTD_PUBLIC_ID,
                                                 DTDRegistry.SUN_EJBJAR_210_DTD_PUBLIC_ID};
            String acceptableURLs[] = {DTDRegistry.SUN_EJBJAR_200_DTD_SYSTEM_ID,
                                       DTDRegistry.SUN_EJBJAR_210_DTD_SYSTEM_ID};
            
            boolean foundDOCTYPE = false, foundPubid = false, foundURL = false;
            EjbBundleDescriptorImpl ejbBundleDesc = descriptor.getEjbBundleDescriptor();
            EjbBundleRuntimeNode ejbBundleRuntimeNode = new EjbBundleRuntimeNode(ejbBundleDesc);
            
            String s = ejbBundleRuntimeNode.getDocType();
            if(s != null) {
                if(s.indexOf("DOCTYPE") > -1)
                    foundDOCTYPE = true;
                if(foundDOCTYPE){
                    for (int i=0;i<acceptablePubidLiterals.length;i++) {
                        if (s.indexOf(acceptablePubidLiterals[i]) > -1) {
                            foundPubid = true;
                            result.addGoodDetails(smh.getLocalString
                                               ("tests.componentNameConstructor",
                                               "For [ {0} ]",
                                               new Object[] {compName.toString()}));
                            result.addGoodDetails
                                        (smh.getLocalString
                                        (getClass().getName() + ".passed1", 
                                        "PASSED [AS-EJB ] : The Sun deployment descriptor has the proper PubidLiteral: {0}", 
                                        new Object[] {acceptablePubidLiterals[i]})); 
                        }
                        //check if the URLs match as well  
                        if (s.indexOf(acceptableURLs[i]) > -1) {
                            foundURL = true;
                            result.addGoodDetails(smh.getLocalString
                                               ("tests.componentNameConstructor",
                                               "For [ {0} ]",
                                               new Object[] {compName.toString()}));
                            result.addGoodDetails
                                        (smh.getLocalString
                                        (getClass().getName() + ".passed2", 
                                        "PASSED [AS-EJB] : The Sun deployment descriptor has the proper URL corresponding the the PubIdLiteral: {0}", 
                                        new Object[] {acceptableURLs[i]})); 
                        }
                    }
                }
            }
            if(!foundDOCTYPE){
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                result.failed
                        (smh.getLocalString
                        (getClass().getName() + ".failed1", 
                        "FAILED [AS-EJB] :  No document type declaration found in the deployment descriptor for {0}",
                        new Object[] {descriptor.getName()}));
            }else if(!foundPubid) {
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                result.failed
                        (smh.getLocalString
                        (getClass().getName() + ".failed2", 
                        "FAILED [AS-EJB ejb] : The deployment descriptor for {0} does not have an expected PubidLiteral ",
                        new Object[] {descriptor.getName()}));
            } else if (!foundURL){
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                        "For [ {0} ]",
                        new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failed", 
                        "The deployment descriptor {0} doesnot have the right URL corresponding to the PubIdLiteral", 
                        new Object[] {descriptor.getName()})); 
            }
            
            
        }catch(Exception ex){
            result.failed(smh.getLocalString(getClass().getName()+".notRun",
                "NOT RUN [AS-EJB cmp] Could not create descriptor Object."));
        }
        }else{
        //NOT APPLICABLE               
                result.addNaDetails(smh.getLocalString
				       ("tests.componentNameConstructor",
					"For [ {0} ]",
					new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
		    "NOT-APPLICABLE: No DOCTYPE found for [ {0} ]",
		     new Object[] {descriptor.getName()}));
    
    
    }
        return result;
    }
}
