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

package com.sun.enterprise.tools.verifier.tests.ejb.intf;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

/** 
 *  Interface test.  
 * Verify that the bean local or remote interface class exist and is loadable.
 */
abstract public class InterfaceClassExist extends EjbTest implements EjbCheck { 
    
    
    /**
     * Following 2 methods are used to determine whether this method is being called by 
     * local/remote interface.
     */
    abstract protected String getInterfaceName(EjbDescriptor descriptor);
    abstract protected String getInterfaceType();
    
    /**  
     * Local Interface test.
     * Verify that the bean remote or local interface class exist and is loadable.
     *  
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if (!(descriptor instanceof EjbSessionDescriptor) &&
                !(descriptor instanceof EjbEntityDescriptor)) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceClassExist.notApplicable1",
                    "Test apply only to session or entity beans."));
            return result;
        }
        
        if(getInterfaceName(descriptor) == null || "".equals(getInterfaceName(descriptor))){
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceClassExist.notApplicable2",
                    "Not Applicable because, EJB [ {0} ] does not have {1} Interface.",
                    new Object[] {descriptor.getEjbClassName(), getInterfaceType()}));
            return result;
        }
        
        // verify that the local or remote interface class exist and is loadable
        try {
            ClassLoader jcl = getVerifierContext().getClassLoader();
            Class c = Class.forName(getClassName(descriptor), false, jcl);
            if(!c.isInterface()) {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceClassExist.failed",
                        "[ {0} ] is defined as a class. It should be an interface.",
                        new Object[] {getClassName(descriptor)}));
            }
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failed",
                    "Error: "+ getInterfaceType() +" interface [ {0} ] does not exist or is not loadable.",
                    new Object[] {getClassName(descriptor)}));
        }
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);	
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    getInterfaceType() + " interface [ {0} ] exist and is loadable.",
                    new Object[] {getClassName(descriptor)}));
        }
        return result;
    }
    
    private String getClassName(EjbDescriptor descriptor) {
        return getInterfaceName(descriptor);
    }
}
