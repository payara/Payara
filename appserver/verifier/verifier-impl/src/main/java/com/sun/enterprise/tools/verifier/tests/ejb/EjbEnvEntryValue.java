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

import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import java.util.Iterator;

/**
 * If the Bean Provider provides a value for an environment entry using the 
 * env-entry-value element, the value can be changed later by the Application 
 * Assembler or Deployer. The value must be a string that is valid for the 
 * constructor of the specified type that takes a single String parameter.
 */
public class EjbEnvEntryValue extends EjbTest implements EjbCheck {


    /**
     *If the Bean Provider provides a value for an environment entry using the 
     * env-entry-value element, the value can be changed later by the Application 
     * Assembler or Deployer. The value must be a string that is valid for the 
     * constructor of the specified type that takes a single String parameter.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();

        if (!descriptor.getEnvironmentProperties().isEmpty()) {
            // The value must be a string that is valid for the
            // constructor of the specified type that takes a single String parameter
            for (Iterator itr = descriptor.getEnvironmentProperties().iterator();
                 itr.hasNext();) {
                EnvironmentProperty nextEnvironmentProperty =
                        (EnvironmentProperty) itr.next();
                if ((nextEnvironmentProperty.getValue() != null)
                        && (nextEnvironmentProperty.getValue().length() > 0)) {
                    if(!validEnvType(nextEnvironmentProperty)) {
                        addErrorDetails(result, compName);
                        result.failed(smh.getLocalString
                                (getClass().getName() + ".failed",
                                "Error: Environment entry name [ {0} ] does not have" +
                                " valid value [ {1} ] for constructor of the specified type" +
                                " [ {2} ] that takes a single String parameter within bean [ {3} ]",
                                new Object[] {nextEnvironmentProperty.getName(),
                                nextEnvironmentProperty.getValue(), nextEnvironmentProperty.getType(),
                                descriptor.getName()}));
                    }
                }
            }
        }
        if(result.getStatus() != Result.FAILED) {
            addGoodDetails(result, compName);
            result.passed(smh.getLocalString
                    (getClass().getName() + ".passed",
                    "Environment entry name has valid value"));

            }
        return result;
    }

    private boolean validEnvType(EnvironmentProperty nextEnvironmentProperty) {

        try {
            if (nextEnvironmentProperty.getType().equals("java.lang.String"))  {
                new String(nextEnvironmentProperty.getValue());

            } else if (nextEnvironmentProperty.getType().equals("java.lang.Integer")) {
                new Integer(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Boolean")) {
                // don't need to do anything in this case, since any string results
                // in a valid object creation
                new Boolean(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Double")) {

                new Double(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Character")
                    && (nextEnvironmentProperty.getValue().length() == 1)) {
                char c = (nextEnvironmentProperty.getValue()).charAt(0);
                new Character(c);
            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Byte")) {
                new Byte(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Short")) {
                new Short(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Long")) {
                new Long(nextEnvironmentProperty.getValue());

            } else if  (nextEnvironmentProperty.getType().equals("java.lang.Float")) {
                new Float(nextEnvironmentProperty.getValue());

            } else {
                return false;
            }
        } catch (Exception ex) {
            Verifier.debug(ex);
            return false;
        }
        return true;
    }

}
