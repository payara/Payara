/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.web.elements;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.web.*;
import com.sun.enterprise.tools.verifier.tests.web.WebCheck;
import com.sun.enterprise.tools.verifier.tests.web.WebTest;

import java.io.*;
import java.lang.ClassLoader;
import java.util.*;
import java.util.jar.*;
import java.util.zip.*;


/** 
 * The Bean Provider must declare all enterprise bean's references to the
 * homes of other enterprise beans as specified in section 14.3.2 of the 
 * Moscone spec.  Check for one within the same jar file, can't check 
 * outside of jar file.  Load/locate & check other bean's home/remote/bean,
 * ensure they match with what the linking bean says they should be; check
 * for pair of referencing and referenced beans exist.
 */
public class WebEjbReferencesElement extends WebTest implements WebCheck { 
    boolean oneFailed=false;
    /** 
     *
     * @param descriptor the Web deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(WebBundleDescriptor descriptor) {

        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
	String f=descriptor.getModuleDescriptor().getArchiveUri();
	loadWarFile(descriptor);
        result.notApplicable(smh.getLocalString
           ("tests.componentNameConstructor",
            "For [ {0} ]",
            new Object[] {compName.toString()}));
        result.addNaDetails(smh.getLocalString
                              (getClass().getName() + ".notApplicable",
                               "There is no ejb-ref inside [ {0} ].",
                               new Object[] {compName}));
        result.addGoodDetails(smh.getLocalString
                           ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName}));	
        result.addErrorDetails(smh.getLocalString
               ("tests.componentNameConstructor",
                "For [ {0} ]",
                new Object[] {compName.toString()}));


        Set references = descriptor.getEjbReferenceDescriptors();
        Iterator iterator = references.iterator();
        while (iterator.hasNext()) {
            EjbReferenceDescriptor ejbReference = (EjbReferenceDescriptor) iterator.next();
            checkInterface(result, ejbReference, ejbReference.getEjbHomeInterface(), f);
            checkInterface(result, ejbReference, ejbReference.getEjbInterface(), f);
        }
        return result;
    }

    private void checkInterface(Result result, EjbReferenceDescriptor ejbRef, String intf, String f){
        Class cl = loadClass(result, intf);
        if(cl==null){
            oneFailed=true;
            result.failed(smh.getLocalString
                          (getClass().getName() + ".failed",
                           "Error: For ejb-ref element [ {0} ] the home/component interface class [ {1} ] is not loadable within [ {2} ].",
                           new Object[] {ejbRef.getName(), intf, f}));
        }else if(!oneFailed) {
            result.passed(smh.getLocalString
                          (getClass().getName() + ".passed",
                           "For ejb-ref element [ {0} ] the home/component interface class [ {1} ] is loadable within [ {2} ].",
                           new Object[] {ejbRef.getName(), intf, f}));
        }
    }
}    


