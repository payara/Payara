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

package com.sun.enterprise.tools.verifier.tests.ejb.intf.remoteintf;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.util.logging.Level;

/**
 * Remote interface/business methods test.  
 * Verify the following:
 * 
 *   The remote interface is allowed to have superinterfaces. Use of interface 
 *   inheritance is subject to the RMI-IIOP rules for the definition of remote 
 *   interfaces. 
 * 
 */
public class RemoteInterfaceSuperInterface extends EjbTest implements EjbCheck { 
    
    /**
     * Remote interface/business methods test.
     * Verify the following:
     *
     *   The remote interface is allowed to have superinterfaces. Use of interface
     *   inheritance is subject to the RMI-IIOP rules for the definition of remote
     *   interfaces.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if (!(descriptor instanceof EjbSessionDescriptor) &&
                !(descriptor instanceof EjbEntityDescriptor)) {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.homeintf.HomeMethodTest.notApplicable1",
                    "Test apply only to session or entity beans."));
            return result;                
        }
        
        if(descriptor.getRemoteClassName() == null || "".equals(descriptor.getRemoteClassName())){
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.intf.InterfaceTest.notApplicable",
                    "Not Applicable because, EJB [ {0} ] does not have {1} Interface.",
                    new Object[] {descriptor.getEjbClassName(), "Remote"}));
            
            return result;
        }
        
        
        boolean oneFailed = false;
        try {
            ClassLoader jcl = getVerifierContext().getClassLoader();
            Class c = Class.forName(descriptor.getRemoteClassName(), false, jcl);
            Class remote = c;
            boolean validRemoteInterface = false;
            boolean ok = false;
            // walk up the class tree
            do {
                Class[] interfaces = c.getInterfaces();
                if ( interfaces.length == 0 ) {
                    ok = true;
                } 
                for (Class intf : interfaces) {
                    logger.log(Level.FINE, getClass().getName() + ".debug1",
                            new Object[] {intf.getName()});
                    
                    //  The remote interface is allowed to have superinterfaces. Use
                    //  of interface inheritance is subject to the RMI-IIOP rules for
                    //  the definition of remote interfaces.
                    // requirement is met if one superinterface complies.
                    if (!ok) {
                        ok = RmiIIOPUtils.isValidRmiIIOPInterface(intf);
                    }
                    
                    // check the methods now.
                    if (RmiIIOPUtils.isValidRmiIIOPInterfaceMethods(intf)) {
                        // this interface is valid, continue
                        if (intf.getName().equals("javax.ejb.EJBObject")) {
                            validRemoteInterface = true;
                            break;
                        }
                    } else {
                        oneFailed = true;
                        addErrorDetails(result, compName);
                        result.addErrorDetails(smh.getLocalString
                                (getClass().getName() + ".failed",
                                "Error: [ {0} ] does not properly conform to " +
                                "rules of RMI-IIOP for superinterfaces.  All " +
                                "enterprise beans remote interfaces are allowed " +
                                "to have superinterfaces that conform to the " +
                                "rules of RMI-IIOP for superinterfaces .  [ {1} ]" +
                                " is not a valid remote interface.",
                                new Object[] {intf.getName(),descriptor.getRemoteClassName()}));
                    }
                    
                }
                
            } while ((((c=c.getSuperclass()) != null) && (!validRemoteInterface)));
            
            if (!ok) {  // check that one superinterface met rmiiiop requirement
                oneFailed = true;
                addErrorDetails(result, compName);
                result.addErrorDetails(smh.getLocalString
                        (getClass().getName() + ".failed",
                        "Error: [ {0} ] does not properly conform to rules of " +
                        "RMI-IIOP for superinterfaces.  All enterprise beans " +
                        "remote interfaces are allowed to have superinterfaces " +
                        "that conform to the rules of RMI-IIOP for superinterfaces. " +
                        " [ {1} ] is not a valid remote interface.",
                        new Object[] {remote.getName(),descriptor.getRemoteClassName()}));
            }
            
            if (validRemoteInterface){
                addGoodDetails(result, compName);
                result.passed(smh.getLocalString
                        (getClass().getName() + ".passed",
                        "[ {0} ] properly conforms to rules of RMI-IIOP for superinterfaces.",
                        new Object[] {descriptor.getRemoteClassName()}));
            }
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addGoodDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: Remote interface [ {0} ] does not exist or is not " +
                    "loadable within bean [ {1} ]",
                    new Object[] {descriptor.getRemoteClassName(),descriptor.getName()}));
            oneFailed = true;
        }
        if (oneFailed) {
            result.setStatus(Result.FAILED);
        } else {
            result.setStatus(Result.PASSED);
        }
        return result;
    }
}
