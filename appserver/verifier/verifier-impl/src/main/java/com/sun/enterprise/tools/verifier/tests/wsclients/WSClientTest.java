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

package com.sun.enterprise.tools.verifier.tests.wsclients;

import com.sun.enterprise.deployment.*;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.util.io.FileUtils;

import java.io.File;
import java.lang.ClassLoader;


/**
 * Superclass for all EJB tests, contains common services.
 *
 * @version 
 */
/* 
 *   @class.setup_props: ; 
 */ 

/*  
 *   @testName: check  
 *   @assertion_ids:  ; 
 *   @test_Strategy: 
 *   @class.testArgs: Additional arguments (if any) to be passed when execing the client  
 */

public abstract class WSClientTest extends VerifierTest implements VerifierCheck, WSClientCheck {
        
    /**
     * <p>
     * run an individual test against the deployment descriptor for the 
     * archive the verifier is performing compliance tests against.
     * </p>
     *
     * @param descriptor deployment descriptor for the archive
     * @return result object containing the result of the individual test
     * performed
     */    
    public Result check(Descriptor descriptor) {
        return check((ServiceReferenceDescriptor) descriptor);
    }
   
    /**
     * @param descriptor deployment descriptor for the archive file
     * @return result object containing the result of the individual test
     * performed
     */    
    public abstract Result check(ServiceReferenceDescriptor descriptor);     
    
    protected String getAbstractArchiveUri(ServiceReferenceDescriptor desc) {
        String archBase = getVerifierContext().getAbstractArchive().
                getURI().toString();
        final ModuleDescriptor moduleDescriptor = desc.getBundleDescriptor().
                getModuleDescriptor();
        if (moduleDescriptor.isStandalone()) {
            return archBase; // it must be a stand-alone module; no such physical dir exists
        } else {
            return archBase + "/" +
                    FileUtils.makeFriendlyFilename(moduleDescriptor.getArchiveUri());
        }
    }

}
