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

package com.sun.enterprise.tools.verifier.tests.ejb.elements;

import com.sun.enterprise.deployment.EjbReferenceDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/** 
 * The Bean Provider must declare all enterprise bean's references to the homes
 * of other enterprise beans as specified in section 14.3.2 of the Moscone spec.
 * Check for one within the same jar file, can't check outside of jar file.
 * Load/locate & check other bean's home/remote/bean, ensure they match with 
 * what the linking bean says they should be; check for pair of referencing and 
 * referenced beans exist.
 */
public class EjbReferencesElement extends EjbTest implements EjbCheck { 
    
    
    Result result = null;
    ComponentNameConstructor compName = null;
    
    // Logger to log messages

    
    /** 
     * The Bean Provider must declare all enterprise bean's references to the homes
     * of other enterprise beans as specified in section 14.3.2 of the Moscone spec. 
     * Check for one within the same jar file, can't check outside of jar file.
     * Load/locate & check other bean's home/remote/bean, ensure they match with
     * what the linking bean says they should be; check for pair of referencing and
     * referenced beans exist.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        
        if ((descriptor instanceof EjbEntityDescriptor) ||
                (descriptor instanceof EjbSessionDescriptor)) {
            
            // RULE: References to other beans must be declared in the form of 
            //       references to other beans homes as specified in section 
            //       14.3.2 of the Moscone spec.
            
            // check for one bean within the same jar file; can't check outside of 
            // jar file.  need to load/locate and check other beans remote, home, bean
            // match with the linking bean says they should be. i.e. check for pair
            // of referencing & referenced bean exist, using reflection API
            
            EjbReferenceDescriptor ejbReference;
//	    EjbAbstractDescriptor ejbDescriptor;
            try {
                
                String fName = null;
                Set references = descriptor.getEjbReferenceDescriptors();
                if(references == null) {
                    logger.log(Level.INFO,getClass().getName() + ".refnull");
                    return result;
                }
                Iterator iterator = references.iterator();
                
                if (iterator.hasNext()) {
                    boolean oneFailed = false;
        //		    boolean foundBeanClassName = false;
        //		    boolean foundHomeClassName = false;
        //		    boolean foundRemoteClassName = false;
//                File fileName = Verifier.getArchiveFile(descriptor.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri());
//                if (fileName!=null){
//                    fName = fileName.getName();
//                }else{
                    fName = descriptor.getEjbBundleDescriptor().getModuleDescriptor().getArchiveUri();
//                }
                    while (iterator.hasNext()) {
                        ejbReference = (EjbReferenceDescriptor) iterator.next();
                        if (ejbReference.isLinked()) { 
                            // reset
                            if(ejbReference.getEjbHomeInterface() != null && 
                                    ejbReference.getEjbInterface() != null) {
                                oneFailed = commonToBothInterfaces(ejbReference.getEjbHomeInterface(),ejbReference.getEjbInterface(),fName);
                            }
                        } else {
                            // (e.g. external references)
                            result.addNaDetails(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[] {compName.toString()}));
                            result.notApplicable(smh.getLocalString
                                    (getClass().getName() + ".notApplicable2",
                                            "Not Applicable: [ {0} ] must be external reference to bean outside of [ {1} ].",
                                            new Object[] {ejbReference.getName(),fName}));
                        }
                    }
                    if (oneFailed) {
                        result.setStatus(result.FAILED);
                    } else {
                        result.setStatus(result.PASSED);
                    }
                } else {
                    result.addNaDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.notApplicable(smh.getLocalString
                            (getClass().getName() + ".notApplicable1",
                                    "There are no ejb references to other beans within this bean [ {0} ]",
                                    new Object[] {descriptor.getName()}));
                }
                
                return result;
            } catch (Exception e) {
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedRef",
                                "Exception occurred : [ {0} ]",
                                new Object[] {e.getMessage()}));
                return result;
            }
        } else {
            result.addNaDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                            "[ {0} ] not called \n with a Session or Entity bean.",
                            new Object[] {getClass()}));
            return result;
        }    
    }
    
    /** 
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param home for the Home Interface of the Ejb
     * @param remote or component for the Remote/Local interface of the Ejb. 
     * This parameter may be optional depending on the test 
     * @param fileName of the archive file. 
     * @return boolean the results for this assertion i.e if a test has failed or not
     */
    
    private boolean commonToBothInterfaces(String home, String remote,String fileName) {
        
        boolean foundHomeClassName = false;
        boolean foundRemoteClassName = false;
        boolean oneFailed = false;
        
        try {
            Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
            if (c != null) {
                foundHomeClassName = true;
                result.addGoodDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.passed(smh.getLocalString
                        (getClass().getName() + ".passed2",
                                "The referenced bean's home interface [ {0} ] exists and is loadable within [ {1} ].",
                                new Object[] {home, fileName}));
            } else {
                oneFailed = true;
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failed",
                                "Error: [ {0} ] class cannot be found within this jar [ {1} ].",
                                new Object[] {home,fileName}));
            }
            c = Class.forName(remote, false, getVerifierContext().getClassLoader());
            if (c != null) {
                foundRemoteClassName = true;
                result.addGoodDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.passed(smh.getLocalString
                        (getClass().getName() + ".passed3",
                                "The referenced bean's remote interface [ {0} ] exists and is loadable within [ {1} ].",
                                new Object[] {remote,fileName}));
            } else {
                oneFailed = true;
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failed",
                                "Error: [ {0} ] class cannot be found within this jar [ {1} ].",
                                new Object[] {compName,fileName}));
            }
            return oneFailed;
        } catch (Exception e) {
            Verifier.debug(e);
            if (!oneFailed) {
                oneFailed = true;
            }
            String classStr = "";
            if (!foundHomeClassName) {
                classStr = home;
            } else if (!foundRemoteClassName) {
                classStr = remote;
            }
            result.addErrorDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",
                            "Error: class [ {0} ] cannot be found within this jar [ {1} ].",
                            new Object[] {classStr, fileName}));
            return oneFailed;
        }
    }
}    


