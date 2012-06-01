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

package com.sun.enterprise.tools.verifier.tests.ejb.homeintf.remotehomeintf;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.enterprise.tools.verifier.tests.ejb.RmiIIOPUtils;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.logging.Level;

/**
 * Enterprise beans home interface test.
 * 
 * The following are the requirements for the enterprise Bean's home interface 
 * signature: 
 * 
 * The home interface is allowed to have superinterfaces. Use of interface 
 * inheritance is subject to the RMI-IIOP rules for the definition of remote 
 * interfaces. 
 */
public class RemoteHomeInterfaceSuperInterface extends EjbTest implements EjbCheck { 
    
    /**
     * Enterprise beans home interface test.
     * 
     * The following are the requirements for the enterprise Bean's home interface 
     * signature: 
     * 
     * The home interface is allowed to have superinterfaces. Use of interface 
     * inheritance is subject to the RMI-IIOP rules for the definition of remote 
     * interfaces. 
     * 
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        if(descriptor.getHomeClassName() == null || "".equals(descriptor.getHomeClassName())){
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    ("com.sun.enterprise.tools.verifier.tests.ejb.localinterfaceonly.notapp",
                    "Not Applicable because, EJB [ {0} ] has Local Interfaces only.",
                    new Object[] {descriptor.getEjbClassName()}));
            return result;
        }
        
        if ((descriptor instanceof EjbSessionDescriptor) ||
                (descriptor instanceof EjbEntityDescriptor)) {
            
            boolean oneFailed = false;
            boolean ok = false;
            try {
                ClassLoader jcl = getVerifierContext().getClassLoader();
                Class c = Class.forName(descriptor.getHomeClassName(), false, jcl);
                Class remote = c;
                boolean validHomeInterface = false;
                // walk up the class tree 
                do {
                    Class[] interfaces = c.getInterfaces();
                    if ( interfaces.length == 0 ) {
                        ok = true;
                    } 
                    for (Class intf: interfaces) {
                        logger.log(Level.FINE, getClass().getName() + ".debug1",
                                new Object[] {intf.getName()});
                        
                        // check to see that interface is a valid RMI-IIOP interface
                        // requirement is met if one superinterface complies.
                        if (!ok) {
                            ok = RmiIIOPUtils.isValidRmiIIOPInterface(intf);
                        }
                        
                        if (RmiIIOPUtils.isValidRmiIIOPInterfaceMethods(intf)) {
                            // this interface is valid, continue
                            if (intf.getName().equals("javax.ejb.EJBHome")) {
                                validHomeInterface = true;
                                break;
                            }
                        } else {
                            // before you determine if this is EJBHome interface, and break
                            // out of loop, report status of SuperInterface
                            oneFailed = true;
                            addErrorDetails(result, compName);
                            result.addErrorDetails(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                    "Error: [ {0} ] does not properly conform to " +
                                    "rules of RMI-IIOP for superinterfaces.  All " +
                                    "enterprise beans home interfaces are allowed " +
                                    "to have superinterfaces that conform to the " +
                                    "rules of RMI-IIOP for superinterfaces .  [ {1} ] " +
                                    "is not a valid home interface.",
                                    new Object[] {intf.getName(),descriptor.getHomeClassName()}));
                        }
                    }
                } while ((((c=c.getSuperclass()) != null) && (!validHomeInterface)));
                // check that superinterface check was a success
                if ( !ok ) {
                    oneFailed = true;
                    addErrorDetails(result, compName);
                    result.addErrorDetails(smh.getLocalString
                            (getClass().getName() + ".failed",
                            "Error: [ {0} ] does not properly conform to rules of " +
                            "RMI-IIOP for superinterfaces.  All enterprise beans " +
                            "home interfaces are allowed to have superinterfaces " +
                            "that conform to the rules of RMI-IIOP for superinterfaces . " +
                            " [{1} ] is not a valid home interface.",
                            new Object[] {remote.getName(),descriptor.getHomeClassName()}));
                }
                
                
                // 
                if (validHomeInterface) {
                    addGoodDetails(result, compName);
                    result.addGoodDetails(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "[ {0} ] properly conforms to rules of RMI-IIOP for superinterfaces.",
                            new Object[] {descriptor.getHomeClassName()}));
                }
                
            } catch (ClassNotFoundException e) {
                Verifier.debug(e);
                addErrorDetails(result, compName);
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedException",
                        "Error: Home interface [ {0} ] does not exist or is not " +
                        "loadable within bean [ {1} ]",
                        new Object[] {descriptor.getHomeClassName(), descriptor.getName()}));
                oneFailed = true;
            }
            if (oneFailed) {
                result.setStatus(Result.FAILED);
            } else {
                result.setStatus(Result.PASSED);
            }
            return result;
            
        } else {
            addNaDetails(result, compName);
            result.notApplicable(smh.getLocalString
                    (getClass().getName() + ".notApplicable",
                    "{0} expected {1} bean or {2} bean, but called with {3}.",
                    new Object[] {getClass(),"Session","Entity",descriptor.getName()}));
            return result;
        } 
    }
}
