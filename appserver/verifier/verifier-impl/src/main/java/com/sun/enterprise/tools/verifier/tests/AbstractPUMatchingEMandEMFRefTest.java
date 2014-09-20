/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.tools.verifier.Result;
import org.glassfish.deployment.common.Descriptor;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * For every entity manager reference and entity manager factory reference
 * in a component (ejb/servlet/app-client etc), there must be a matching
 * persistence unit defined in the scope of that component.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public abstract class AbstractPUMatchingEMandEMFRefTest extends VerifierTest
        implements VerifierCheck {
    final static String className = AbstractPUMatchingEMandEMFRefTest.class.getName();
    public Result check(Descriptor descriptor) {
        // initialize the result object
        Result result = getInitializedResult();
        addErrorDetails(result,
                getVerifierContext().getComponentNameConstructor());
        result.setStatus(Result.PASSED); //default status is PASSED
        
        BundleDescriptor bundleDescriptor = getBundleDescriptor(descriptor);
        
        for (EntityManagerReferenceDescriptor emRefDesc : getEntityManagerReferenceDescriptors(descriptor)) {
            String referringUnitName = emRefDesc.getUnitName();
            PersistenceUnitDescriptor pu = bundleDescriptor.findReferencedPU(referringUnitName);
            if (pu == null) {
                result.failed(smh.getLocalString(
                        className + "failed",
                        "There is no unique persistence unit found by name " +
                        "[ {0} ] in the scope of this component.",
                        new Object[]{referringUnitName}));
            } else {
                result.passed(smh.getLocalString(
                        className + "passed",
                        "Found a persistence unit by name [ {0} ] in the scope of this component",
                        new Object[]{referringUnitName}));
            }
        }
        for (EntityManagerFactoryReferenceDescriptor emfRefDesc : getEntityManagerFactoryReferenceDescriptors(descriptor)) {
            String referringUnitName = emfRefDesc.getUnitName();
            PersistenceUnitDescriptor pu = bundleDescriptor.findReferencedPU(referringUnitName);
            if (pu == null) {
                result.failed(smh.getLocalString(
                        className + "failed",
                        "There is no unique persistence unit found by name " +
                        "[ {0} ] in the scope of this component.",
                        new Object[]{referringUnitName}));
            } else {
                result.passed(smh.getLocalString(
                        className + "passed",
                        "Found a persistence unit by name [ {0} ] in the scope of this component",
                        new Object[]{referringUnitName}));
            }
        }
        
        StringBuilder visiblePUNames = new StringBuilder();
        final Map<String, PersistenceUnitDescriptor> visiblePUs =
                bundleDescriptor.getVisiblePUs();
        int count = 0;
        for(String puName : visiblePUs.keySet()) {
            visiblePUNames.append(puName);
            if(visiblePUs.size() != ++count) { // end not yet reached
                visiblePUNames.append(", ");
            }
        }
        String message = smh.getLocalString(className + ".puList",
                "PUs that are visible to this component are: [ {0} ]",
                new Object[]{visiblePUNames});
        result.addErrorDetails(message);
        result.addGoodDetails(message);
        
        return result;
    }
    
    protected abstract BundleDescriptor getBundleDescriptor(
            Descriptor descriptor);
    
    protected abstract Collection<EntityManagerReferenceDescriptor>
            getEntityManagerReferenceDescriptors(Descriptor descriptor);
    
    protected abstract Collection<EntityManagerFactoryReferenceDescriptor>
            getEntityManagerFactoryReferenceDescriptors(Descriptor descriptor);
    
}
