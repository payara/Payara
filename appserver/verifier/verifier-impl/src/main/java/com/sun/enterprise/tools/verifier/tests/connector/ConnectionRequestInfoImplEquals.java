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

/*
 * ConnectionRequestInfoImplEquals.java
 *
 * Created on September 27, 2000, 10:21 AM
 */

package com.sun.enterprise.tools.verifier.tests.connector;

import java.io.File;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;

/**
 * Test wether the implementatin of the ConnectionRequestInfo interface
 * properly overrides the equals method
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ConnectionRequestInfoImplEquals extends ConnectorTest implements ConnectorCheck {


    /** <p>
     * Test wether the implementatin of the ConnectionRequestInfo interface
     * properly overrides the equals method.
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {
        
        Result result = getInitializedResult();
	ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
        
        // let's get first the the default implementation of the ConnectionManager
        //File jarFile = Verifier.getJarFile(descriptor.getModuleDescriptor().getArchiveUri());            
//        File f=Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
        Class c = findImplementorOf(descriptor, "javax.resource.spi.ConnectionRequestInfo");
        
        if (c == null) {
	    result.addNaDetails(smh.getLocalString
				  ("tests.componentNameConstructor",
				   "For [ {0} ]",
				   new Object[] {compName.toString()}));	
            result.notApplicable(smh.getLocalString
	    ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.optionalInterfaceMissing", 
            "Warning: There is no implementation of the optional [ {0} ] interface",
            new Object[] {"javax.resource.spi.ConnectionRequestInfo"}));  
        } else {
            // An implementation of the interface is provided, let's check the equals method
            checkMethodImpl(c, "equals", new Class[] {Object.class}, "public boolean equals(java.lang.Object)", result);                    
        }
        return result;
    }        
}
