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

package com.sun.enterprise.tools.verifier.tests.persistence;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;
import java.util.List;
import java.util.ArrayList;

/**
 * A persistence unit must have a name.
 * Only one persistence unit of any given name may be defined
 * within a single EJB-JAR file, within a single WAR file,
 * within a single application client jar, or within
 * an EAR (in the EAR root or lib directory).
 * See section #6.2 of EJB 3.0 Persistence API spec
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class DuplicatePUNameTest extends VerifierTest implements VerifierCheck {
    public Result check(Descriptor descriptor) {
        PersistenceUnitDescriptor pu = PersistenceUnitDescriptor.class.cast(
                descriptor);
        Result result = getInitializedResult();
        addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
        result.setStatus(Result.PASSED); // default status is PASSED
        int count = 0;
        for(PersistenceUnitDescriptor nextPU : getPUsInSameScope(pu)) {
            result.addErrorDetails(smh.getLocalString(getClass().getName() + "puName",
                    "Found a persistence unit by name [ {0} ] in persistence unit root [ {1} ]",
                    new Object[]{nextPU.getName(), nextPU.getPuRoot()}));
                    if (nextPU.getName().equals(pu.getName())) count++;
        }
        if (count != 1) {
            result.failed(smh.getLocalString(getClass().getName() + "failed",
                    "There are [ {0} ] number of persistence units by name [ {1} ]",
                    new Object[]{count, pu.getName()}));
        }
        return result;
    }
    
    /**
     * @return the list of PersistenceUnits which will be tested to see if they contain duplicate PU name.
     */
    private List<PersistenceUnitDescriptor> getPUsInSameScope(PersistenceUnitDescriptor pu) {
        List<PersistenceUnitDescriptor> result;
        if(pu.getParent().getParent().isApplication()) {
            // for ear, the PU name has to be unique only within a jar file.
            result = pu.getParent().getPersistenceUnitDescriptors();
        } else {
            // for war/ejb-jar/appclient-jar, PU name has to be unique within the whole BundleDescriptor.
            result = new ArrayList<PersistenceUnitDescriptor>();
            for (PersistenceUnitsDescriptor pus : pu.getParent().getParent().getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
                for(PersistenceUnitDescriptor nextPU : pus.getPersistenceUnitDescriptors()) {
                    result.add(nextPU);
                }
            }
        }
        return result;
    }
    
}
