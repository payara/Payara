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

package com.sun.enterprise.tools.verifier.tests.web;

import com.sun.enterprise.deployment.InjectionTarget;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import java.util.Set;
import javax.servlet.Servlet;
import javax.servlet.SingleThreadModel;

/**
 * Assertion
 *
 *  EntityManager should not be injected into a web application that uses multithread model.
 *  EntityManager is not thread safe, hence it should not be injected into a web application
 *  that uses multithreaded model.
 *
 * @author bshankar@sun.com
 */
public class EntityManagerInjection extends WebTest implements WebCheck  {
    
    final static String className = EntityManagerInjection.class.getName();
    
    public Result check(WebBundleDescriptor descriptor) {
        
        Result result = getInitializedResult();
        addWarningDetails(result,
                getVerifierContext().getComponentNameConstructor());
        result.setStatus(Result.PASSED); //default status is PASSED
        
        for(EntityManagerReferenceDescriptor emRefDesc : descriptor.getEntityManagerReferenceDescriptors()) {
            Set<InjectionTarget> injectionTargets = emRefDesc.getInjectionTargets();
            if(injectionTargets != null) {
                for(InjectionTarget it : injectionTargets) {
                    String itClassName = it.getClassName();
                    String errMsg = smh.getLocalString(className + ".warning",
                            "Found a persistence unit by name [ {0} ] injected into [ {1} ].",
                            new Object[]{emRefDesc.getUnitName(), itClassName});
                    try {
                        Class c = Class.forName(itClassName, false, getVerifierContext().getClassLoader());
                        if(!(Servlet.class.isAssignableFrom(c))) {
                            result.warning(errMsg);
                        } else if (!(SingleThreadModel.class.isAssignableFrom(c))) {
                            result.warning(errMsg);
                        }
                    } catch(Exception ex) {
                        result.warning(errMsg);
                    }
                }
            }
        }
        return result;
    }
    
}
