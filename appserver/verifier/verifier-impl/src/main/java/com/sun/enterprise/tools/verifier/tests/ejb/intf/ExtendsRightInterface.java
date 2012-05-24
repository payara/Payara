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
 * Remote interfaces extend the EJBObject interface test. Local interfaces extend 
 * the EJBLocalObject interface test. 
 * All enterprise beans remote interfaces must extend the EJBObject interface 
 * and/or local interfaces must extend the EJBLocalObject interface.
 * 
 * @author Sheetal Vartak
 */
abstract public class ExtendsRightInterface extends EjbTest implements EjbCheck { 
    /**
     * Following 3 methods are used to determine whether this method is being called by 
     * local/remote interface.
     */
    abstract protected String getInterfaceName(EjbDescriptor descriptor);
    abstract protected String getSuperInterface();
    abstract protected String getInterfaceType();
    
    /** 
     * local interfaces extend the EJBLocalObject interface and remote interfaces 
     * extend the EJBObject interface test.  
     * All enterprise beans remote interfaces must extend the EJBObject interface 
     * and/or local interfaces must extend the EJBLocalObject interface.
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        String str = null;
        
        if (!(descriptor instanceof EjbSessionDescriptor) &&
                !(descriptor instanceof EjbEntityDescriptor)) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.homeintf.HomeMethodTest.notApplicable1",
                    "Test apply only to session or entity beans."));
            return result;
        }
        
        if(getInterfaceName(descriptor) == null || "".equals(getInterfaceName(descriptor))) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceTest.notApplicable",
                    "Not Applicable because, EJB [ {0} ] does not have {1} Interface.",
                    new Object[] {descriptor.getEjbClassName(), getInterfaceType()}));
            return result;
        }
        
        try {
            ClassLoader jcl = getVerifierContext().getClassLoader();	   
            Class c = Class.forName(getClassName(descriptor), false, jcl);
            str = getSuperInterface();
            
            if (isImplementorOf(c, str)) {
                addGoodDetails(result, compName);	
                result.passed(smh.getLocalString
                        (getClass().getName() + ".passed",
                        "[ {0} ] " + getInterfaceType() +" interface properly extends the" + str + " interface.",
                        new Object[] {getClassName(descriptor)}));
            } else {
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failed",
                        "Error: [ {0} ] does not properly extend the EJBObject interface. "+
                        " All enterprise bean" + getInterfaceType() + " interfaces must extend the" + str + "  interface."+
                        " [ {1} ] is not a valid "+ getInterfaceType() + "interface within bean [ {2} ]",
                        new Object[] {getClassName(descriptor),getClassName(descriptor),descriptor.getName()}));
            }
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: [ {0} ] class not found.",
                    new Object[] {getClassName(descriptor)}));
        }
        return result;
    }
    //get the interface class name
    private String getClassName(EjbDescriptor descriptor) {
        return getInterfaceName(descriptor);
    }
}
