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

package com.sun.enterprise.tools.verifier.tests.ejb;

import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;

import java.lang.reflect.Method;

/** 
 * The Bean Provider defines the application exceptions in the throws clauses 
 * of the methods of the home interface.  An application exception
 * is  an exception defined in the throws clause of a method in the Bean's home
 * interface, other than java.rmi.RemoteException.  An application 
 * exception must not be defined as a subclass of the 
 * java.lang.RuntimeException, or of the java.rmi.RemoteException. These are 
 * reserved for system exceptions. 
 * The javax.ejb.CreateException, javax.ejb.RemoveException, 
 * javax.ejb.FinderException, and subclasses thereof, are considered to be 
 * application exceptions.
 */
public class ApplicationExceptionHomeInterfaceMethods extends EjbTest implements EjbCheck { 

    Result result = null;
    ComponentNameConstructor compName = null;

    /**
     * The Bean Provider defines the application exceptions in the throws clauses 
     * of the methods of the home interface.  An application exception
     * is  an exception defined in the throws clause of a method in the Bean's home
     * interface, other than java.rmi.RemoteException.  An application 
     * exception must not be defined as a subclass of the 
     * java.lang.RuntimeException, or of the java.rmi.RemoteException. These are 
     * reserved for system exceptions. 
     * The javax.ejb.CreateException, javax.ejb.RemoveException, 
     * javax.ejb.FinderException, and subclasses thereof, are considered to be 
     * application exceptions.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        if ((descriptor instanceof EjbSessionDescriptor) ||
                (descriptor instanceof EjbEntityDescriptor)) {
            if(descriptor.getHomeClassName() != null)
                commonToBothInterfaces(descriptor.getHomeClassName(),descriptor);
            if(descriptor.getLocalHomeClassName() != null)
                commonToBothInterfaces(descriptor.getLocalHomeClassName(), descriptor);
        }

        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "All the methods of Home interface are defined properly"));
        }
        return result;
    }


    /**
     * This method is responsible for the logic of the test. It is called for both local and remote interfaces.
     * @param descriptor the Enterprise Java Bean deployment descriptor
     * @param home for the Home Interface of the Ejb
     * This parameter may be optional depending on the test
     */

    private void commonToBothInterfaces (String home, EjbDescriptor descriptor) {

        try {
            Class c = Class.forName(home, false, getVerifierContext().getClassLoader());
            Class [] methodExceptionTypes;

            for(Method methods : c.getDeclaredMethods()) {
                methodExceptionTypes = methods.getExceptionTypes();
                // methods must also throw java.rmi.RemoteException
                if (!(EjbUtils.isValidApplicationException(methodExceptionTypes))) {
                    addErrorDetails(result, compName);
                    result.failed(smh.getLocalString
                            (getClass().getName() + ".failed",
                            "For the Interface [ {0} ] Method [ {1} ] does" +
                            " not throw valid application exceptions",
                            new Object[] {home, methods.getName()}));
                }
            } // for all the methods within the home interface class, loop

        } catch (Exception e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                    (getClass().getName() + ".failedException",
                    "Error: Home interface [ {0} ] does not exist or is" +
                    " not loadable within bean [ {1} ]",
                    new Object[] {home, descriptor.getName()}));
        }
    }
}
