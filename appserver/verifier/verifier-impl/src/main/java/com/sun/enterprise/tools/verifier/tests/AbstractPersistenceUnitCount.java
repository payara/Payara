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
import org.glassfish.deployment.common.RootDeploymentDescriptor;

/**
 * Assertion :
 *
 *  persistemce.xml should have atleast one persistence unit.
 *
 * @author bshankar@sun.com
 */
public abstract class AbstractPersistenceUnitCount extends VerifierTest
        implements VerifierCheck {
    final static String className = AbstractPersistenceUnitCount.class.getName();
    
    public Result check(Descriptor descriptor) {
        
        RootDeploymentDescriptor rootDescriptor = getRootDescriptor(descriptor);
        
        Result result = getInitializedResult();
        addErrorDetails(result,
                getVerifierContext().getComponentNameConstructor());
        result.setStatus(Result.PASSED); //default status is PASSED
        
        if(rootDescriptor.getExtensionsDescriptors(PersistenceUnitsDescriptor.class).size() == 0)
            result.setStatus(Result.NOT_APPLICABLE);
        
        for(PersistenceUnitsDescriptor pus : rootDescriptor.getExtensionsDescriptors(PersistenceUnitsDescriptor.class)) {
            if (pus.getPersistenceUnitDescriptors().size() == 0) {
                result.failed(
                        smh.getLocalString(
                        className + ".failed",
                        "persistence.xml in persistence unit root [ {0} ] has no persistence units.",
                        new Object[]{pus.getPuRoot()}));
            }
        }
        return result;
    }
    
    protected abstract RootDeploymentDescriptor getRootDescriptor(Descriptor descriptor);
    
}
