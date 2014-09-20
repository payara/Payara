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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The Bean Provider must assume that the content of transient fields may be 
 * lost between the ejbPassivate and ejbActivate notifications. Therefore, the
 * Bean Provider should not store in a transient field a reference to any of 
 * the following objects: SessionContext object; environment JNDI naming 
 * context and any its subcontexts; home and remote interfaces; and the 
 * UserTransaction interface. The restrictions on the use of transient fields 
 * ensure that Containers can use Java Serialization during passivation and 
 * activation.
 */
public class TransientFieldsSerialization extends EjbTest implements EjbCheck {



    /**
     * The Bean Provider must assume that the content of transient fields may be 
     * lost between the ejbPassivate and ejbActivate notifications. Therefore, the
     * Bean Provider should not store in a transient field a reference to any of 
     * the following objects: SessionContext object; environment JNDI naming 
     * context and any its subcontexts; home and remote interfaces; and the 
     * UserTransaction interface. The restrictions on the use of transient fields 
     * ensure that Containers can use Java Serialization during passivation and 
     * activation.
     *
     * @param descriptor the Enterprise Java Bean deployment descriptor
     *   
     * @return <code>Result</code> the results for this assertion
     */
    public Result check(EjbDescriptor descriptor) {

        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        boolean isEjb30 = descriptor.getEjbBundleDescriptor()
                              .getSpecVersion().equalsIgnoreCase("3.0");

        if (descriptor instanceof EjbSessionDescriptor) {
            try {
                Class c = Class.forName(((EjbSessionDescriptor)descriptor).getEjbClassName(), false,
                                   getVerifierContext().getClassLoader());
                //  Bean Provider should not store in a transient field a reference to
                // any of the following objects: SessionContext object; environment
                // JNDI naming context and any its subcontexts; home and remote
                // interfaces; and the UserTransaction interface.
                Field [] fields = c.getDeclaredFields();
                for (int i = 0; i < fields.length; i++) {
                    int modifiers = fields[i].getModifiers();
                    if (!Modifier.isTransient(modifiers)) {
                        continue;
                    } else {
                        Class fc = fields[i].getType();
                        // can't do anything with environment JNDI naming context and
                        // any its subcontexts
                        //sg133765: do we need to do something for business interface
                        if ((fc.getName().equals("javax.ejb.SessionContext")) ||
                                (fc.getName().equals("javax.transaction.UserTransaction")) ||
                                (fc.getName().equals(descriptor.getRemoteClassName())) ||
                                (fc.getName().equals(descriptor.getHomeClassName()))||
                                (fc.getName().equals(descriptor.getLocalClassName())) ||
                                (fc.getName().equals(descriptor.getLocalHomeClassName())) ||
                                (isEjb30 && fc.getName().equals("javax.ejb.EntityManager")) ||
                                (isEjb30 && fc.getName().equals("javax.ejb.EntityManagerFactory"))) {

                            result.failed(smh.getLocalString
                                    ("tests.componentNameConstructor",
                                            "For [ {0} ]",
                                            new Object[] {compName.toString()}));
                            result.addErrorDetails(smh.getLocalString
                                    (getClass().getName() + ".failed",
                                    "Error: Field [ {0} ] defined within" +
                                    " session bean class [ {1} ] is defined as transient. " +
                                    "Session bean fields should not store in a " +
                                    "transient field a reference to any of the following objects: " +
                                    "SessionContext object; environment JNDI naming context and any " +
                                    "its subcontexts; home and remote interfaces;" +
                                    " and the UserTransaction interface.",
                                            new Object[] {fields[i].getName(),
                                            ((EjbSessionDescriptor)descriptor).getEjbClassName()}));
                        }
                    }
                }

            } catch (ClassNotFoundException e) {
                Verifier.debug(e);
                result.addErrorDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.failed(smh.getLocalString
                        (getClass().getName() + ".failedException",
                                "Error: [ {0} ] class not found.",
                                new Object[] {((EjbSessionDescriptor)descriptor).getEjbClassName()}));
            }
        }
        if(result.getStatus()!=Result.FAILED) {
            addGoodDetails(result, compName);
		    result.passed(smh.getLocalString
				  (getClass().getName() + ".passed",
				   "This session bean class has not stored in a " +
                    "transient field a reference to any of the following objects: " +
                    "SessionContext object; environment JNDI naming context and" +
                    " any its subcontexts; home and remote interfaces; and the " +
                    "UserTransaction interface."));
        }
        return result;
    }
}


