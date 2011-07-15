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
 * ConnectionFactoryExistence.java
 *
 * Created on September 28, 2000, 4:59 PM
 */

package com.sun.enterprise.tools.verifier.tests.connector.cci;

import com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.*;


/**
 * Verify that the interface declared in the deployment descriptor 
 * connectionfactory-interface is actually contained in the archive
 *
 * @author  Jerome Dochez
 * @version 
 */
public class ConnectionFactoryInterfaceExistence extends ConnectionFactoryTest implements ConnectorCheck
{


    /**
     * <p>
     * Verify that the interface declared in the deployment descriptor 
     * connectionfactory-interface is actually contained in the archive
     * </p>
     *
     * @paramm descriptor deployment descriptor for the rar file
     * @return result object containing the result of the individual test
     * performed
     */
    public Result check(ConnectorDescriptor descriptor) {
        
      Result result = getInitializedResult();
      ComponentNameConstructor compName = getVerifierContext().getComponentNameConstructor();
      if(isCCIImplemented(descriptor, result))
      {
        /*String interfaceName = descriptor.getConnectionFactoryInterface();
        if (interfaceName == null) {
          result.addErrorDetails(smh.getLocalString
              ("tests.componentNameConstructor",
               "For [ {0} ]",
               new Object[] {compName.toString()}));
          result.failed(smh.getLocalString
              (getClass().getName() + ".nonexist",
               "Error: The deployment descriptor for the resource adapter do not define a connectionfactory-interface"));        
        }*/        
        isClassLoadable("javax.resource.cci.ConnectionFactory", result); 
      }
      else
      {
        result.addNaDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.notApplicable(smh.getLocalString
            ("com.sun.enterprise.tools.verifier.tests.connector.cci.notApp",
             "The CCI interfaces do not seem to be implemented by this resource adapter"));
      }
      return result;
    }
}
