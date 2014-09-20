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
 * ManagedConnectionFactorySerializable.java
 *
 * Created on September 26, 2000, 2:10 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector.managed;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;

/**
 *
 * verify the declared ManagedConnectionFactory implements the 
 * java.io.Serializable interface
 
 * @author  Jerome Dochez
 * @version 
 */
public class ManagedConnectionFactorySerializable 
    extends ManagedConnectionFactoryTest
    implements ConnectorCheck 
{

 
    /** <p>
     * all connector tests should implement this method. it run an individual
     * test against the resource adapter deployment descriptor.
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
      public Result check(ConnectorDescriptor descriptor) {
            
        Result result = getInitializedResult();
        ComponentNameConstructor compName = 
          getVerifierContext().getComponentNameConstructor();
        // test NA for inboundRA
        if(!descriptor.getOutBoundDefined())
        {
          result.addNaDetails(smh.getLocalString
              ("tests.componentNameConstructor",
               "For [ {0} ]",
               new Object[] {compName.toString()}));
          result.notApplicable(smh.getLocalString
              ("com.sun.enterprise.tools.verifier.tests.connector.managed.notApplicableForInboundRA",
               "Resource Adapter does not provide outbound communication"));
          return result;
        }
        testImplementationOf(descriptor, "java.io.Serializable", result);
        return result;
    }
}
