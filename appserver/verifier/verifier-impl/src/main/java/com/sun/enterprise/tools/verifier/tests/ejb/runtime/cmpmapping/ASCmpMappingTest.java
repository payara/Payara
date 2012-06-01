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

package com.sun.enterprise.tools.verifier.tests.ejb.runtime.cmpmapping;

import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbCheck;
import com.sun.enterprise.tools.verifier.tests.ejb.EjbTest;
import com.sun.jdo.spi.persistence.support.ejb.ejbc.JDOCodeGenerator;
import com.sun.jdo.spi.persistence.support.ejb.ejbqlc.EJBQLException;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.IASEjbCMPEntityDescriptor;

import java.util.Collection;
import java.util.Iterator;

public class ASCmpMappingTest extends EjbTest implements EjbCheck {

    public Result check(EjbDescriptor descriptor)
    {
        Result result = getInitializedResult();
        ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        try {
            if (descriptor instanceof IASEjbCMPEntityDescriptor) {
                Collection col = null;
                if(getVerifierContext().getJDOException()!=null){
                    result.addErrorDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.failed (smh.getLocalString(getClass().getName() + ".failed1",
                            "Error: Exception [ {0} ] while initializing JDOCodeGenerator. Please check your descriptors and mapping files for consistency ",
                            new Object[] {getVerifierContext().getJDOException().getMessage()}));

                    return result;
                }else{
                    try{
                        JDOCodeGenerator jdc= getVerifierContext().getJDOCodeGenerator();
                        col = jdc.validate((IASEjbCMPEntityDescriptor)descriptor);
                    }catch(Exception ex){
                        result.addErrorDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                        result.failed (smh.getLocalString(getClass().getName() + ".failed",
                                "Error: Exception [ {0} ] when calling JDOCodeGenerator.validate().",
                                new Object[] {ex.getMessage()}));
                        return result;
                    }
                }
                if (col.isEmpty()){
                    result.addGoodDetails(smh.getLocalString
                            ("tests.componentNameConstructor",
                                    "For [ {0} ]",
                                    new Object[] {compName.toString()}));
                    result.passed(smh.getLocalString(getClass().getName() + ".passed",
                            "The mappings for the cmp beans (if any) are correct."));
                }else {
                    // collect all the cmpmapping related errors
                    String allErrors = null;
                    Iterator it = col.iterator();
                    while (it.hasNext()) {
                        Exception e = (Exception)it.next();
                        if (!(e instanceof EJBQLException)) {
                            allErrors = e.getMessage() + "\n\n";
                        }
                    }
                    if (allErrors != null) {
                        result.addErrorDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                        result.failed(smh.getLocalString(getClass().getName() + ".parseError",
                                "Error: Entity bean [ {0} ] has the following error(s) [ {1} ]."
                                , new Object[] {descriptor.getEjbClassName(), "\n" + allErrors} ));
                    }
                    else {
                        result.addGoodDetails(smh.getLocalString
                                ("tests.componentNameConstructor",
                                        "For [ {0} ]",
                                        new Object[] {compName.toString()}));
                        result.passed(smh.getLocalString(getClass().getName() + ".passed",
                                "The mappings for the cmp beans (if any) are correct."));
                    }
                  }
            } else
            {
                result.addNaDetails(smh.getLocalString
                        ("tests.componentNameConstructor",
                                "For [ {0} ]",
                                new Object[] {compName.toString()}));
                result.notApplicable(smh.getLocalString(getClass().getName() + ".notApplicable",
                        "Not applicable: Test only applies to container managed EJBs"));
            }
        } catch(Exception e) {
            result.addErrorDetails(smh.getLocalString
                    ("tests.componentNameConstructor",
                            "For [ {0} ]",
                            new Object[] {compName.toString()}));
            result.failed (smh.getLocalString(getClass().getName() + ".failed",
                    "Error: Exception [ {0} ] when calling JDOCodeGenerator.validate().",
                    new Object[] {e.getMessage()}));
        }
        return result;
    }
}
