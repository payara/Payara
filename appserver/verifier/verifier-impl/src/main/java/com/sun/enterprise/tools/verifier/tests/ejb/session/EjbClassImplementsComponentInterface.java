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

package com.sun.enterprise.tools.verifier.tests.ejb.session;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;

import java.util.logging.Level;

/**
 * Optionally implements the enterprise Bean's remote interface test.  
 * The class may, but is not required to, implement the enterprise Bean's 
 * remote interface.  It is recommended that the enterprise bean class 
 * not implement the remote interface to prevent inadvertent passing of 
 * this as a method argument or result. 
 * Note: Display warning to user in this instance. 
 */
public class EjbClassImplementsComponentInterface extends EjbTest implements EjbCheck { 
    Result result = null;
    ComponentNameConstructor compName = null;
    /**
     * Optionally implements the enterprise Bean's remote interface test.  
     * The class may, but is not required to, implement the enterprise Bean's 
     * remote interface.  It is recommended that the enterprise bean class 
     * not implement the remote interface to prevent inadvertent passing of 
     * this as a method argument or result. 
     * Note: Display warning to user in this instance. 
     *   
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {
        result = getInitializedResult();
        compName = getVerifierContext().getComponentNameConstructor();
        if (descriptor instanceof EjbSessionDescriptor) {
            if(descriptor.getRemoteClassName() != null && !"".equals(descriptor.getRemoteClassName()))
                commonToBothInterfaces(descriptor.getRemoteClassName(),(EjbSessionDescriptor)descriptor);
            if(descriptor.getLocalClassName() != null && !"".equals(descriptor.getLocalClassName()))
                commonToBothInterfaces(descriptor.getLocalClassName(),(EjbSessionDescriptor)descriptor);
        }
        if(result.getStatus()!=Result.FAILED && result.getStatus()!=Result.WARNING) {
            addGoodDetails(result, compName);
            result.addGoodDetails(smh.getLocalString
                            (getClass().getName() + ".passed",
                            "Bean class does not implement the enterprise Bean's remote interface"));
        }
        return result;
    }

    private void commonToBothInterfaces(String component, EjbSessionDescriptor descriptor) {
        try {
            Class c = Class.forName(descriptor.getEjbClassName(), false, getVerifierContext().getClassLoader());
            Class rc = Class.forName(component, false, getVerifierContext().getClassLoader());
            // walk up the class tree
            do {
                for (Class interfaces : c.getInterfaces()) {
                    logger.log(Level.FINE, getClass().getName() + ".debug1",
                            new Object[] {interfaces.getName()});
                    if (interfaces.getName().equals(rc.getName())) {
                        // display warning to user
                        addWarningDetails(result, compName);
                        result.warning(smh.getLocalString
                                (getClass().getName() + ".warning",
                                 "Warning: [ {0} ] class implments the " +
                                "enterprise Bean's remote interface [ {1} ].  " +
                                "It is recommended that the enterprise bean class not" +
                                " implement the remote interface to prevent " +
                                "inadvertent passing of this as a method argument or result. ",
                                 new Object[] {descriptor.getEjbClassName(),rc.getName()}));
                        break;
                    }
                }
            } while ((c=c.getSuperclass()) != null);
        } catch (ClassNotFoundException e) {
            Verifier.debug(e);
            addErrorDetails(result, compName);
            result.failed(smh.getLocalString
                        (getClass().getName() + ".failedException",
                                "Error: [ {0} ] class not found.",
                                new Object[] {descriptor.getEjbClassName()}));
        }
    }
}
