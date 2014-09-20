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

package com.sun.enterprise.tools.verifier.tests.appclient;

import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.EntityManagerFactoryReferenceDescriptor;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
import com.sun.enterprise.tools.verifier.tests.VerifierCheck;
import com.sun.enterprise.tools.verifier.tests.VerifierTest;
import com.sun.enterprise.tools.verifier.Result;
import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Assertions :
 *
 *  1) A persistence unit with JTA transaction type is not supported in application client.
 *  2) Reference to a PU whose transaction type is JTA is not supported in application client.
 *
 * @author bshankar@sun.com
 */

public class PUTransactionType extends VerifierTest implements VerifierCheck {
    
    public Result check(Descriptor descriptor) {
        ApplicationClientDescriptor appClient = (ApplicationClientDescriptor) descriptor;
        Result result = getInitializedResult();
        addErrorDetails(result, getVerifierContext().getComponentNameConstructor());
        result.setStatus(Result.PASSED); // default status is PASSED
        
        for(PersistenceUnitsDescriptor pus : appClient.getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            for(PersistenceUnitDescriptor nextPU : pus.getPersistenceUnitDescriptors()) {
                if("JTA".equals(nextPU.getTransactionType())) {
                    result.failed(smh.getLocalString(getClass().getName() + ".puName",
                            "Found a persistence unit by name [ {0} ] in persistence unit root [ {1} ] with JTA transaction type.",
                            new Object[]{nextPU.getName(), nextPU.getPuRoot()}));
                }
            }
        }
        
        for(EntityManagerFactoryReferenceDescriptor emfRef : appClient.getEntityManagerFactoryReferenceDescriptors()) {
            String unitName = emfRef.getUnitName();
            PersistenceUnitDescriptor nextPU = appClient.findReferencedPU(unitName);
            if(nextPU == null) continue;
            if("JTA".equals(nextPU.getTransactionType())) {
                result.failed(smh.getLocalString(getClass().getName() + ".puRefName",
                        "Found a reference to a persistence unit by name [ {0} ] in persistence unit root [ {1} ] with JTA transaction type.",
                        new Object[]{nextPU.getName(), nextPU.getPuRoot()}));
            }
        }
        
        return result;
    }
    
}
