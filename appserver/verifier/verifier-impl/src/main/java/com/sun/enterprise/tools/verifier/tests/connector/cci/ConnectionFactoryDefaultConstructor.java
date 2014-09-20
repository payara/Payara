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
 * ManagedConnectionGetMetaData.java
 *
 * Created on August 26, 2002
 */

package com.sun.enterprise.tools.verifier.tests.connector.cci;

import java.io.File;
import java.lang.reflect.Method;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;

/**
 * Test that the implementation class for  
 * javax.resource.cci.ConnectionFactory provides a default constructor 
 *
 * @author Anisha Malhotra 
 * @version 
 */
public class ConnectionFactoryDefaultConstructor
    extends ConnectionFactoryTest 
    implements ConnectorCheck 
{
  /** <p>
   * Test that the implementation class for  
   * javax.resource.cci.ConnectionFactory provides a default constructor 
   * </p>
   *
   * @param descriptor deployment descriptor for the rar file
   * @return result object containing the result of the individual test
   * performed
   */
  public Result check(ConnectorDescriptor descriptor) {

    Result result = getInitializedResult();
    ComponentNameConstructor compName = 
      getVerifierContext().getComponentNameConstructor();
    Class connFactoryImpl = null;
    if(isCCIImplemented(descriptor, result))
    {
      connFactoryImpl = testConnectionFactoryImpl(descriptor, result);
      if (connFactoryImpl == null) 
        return result;
    }
    else
    {
      // test is NA
      result.addNaDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.notApplicable(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.cci.notApp",
           "The CCI interfaces do not seem to be implemented by this resource adapter"));
      return result;
    }
    // check if connectionfactory-impl-class has a default constructor
    try
    {
      connFactoryImpl.getConstructor(new Class[0]);
      result.addGoodDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.passed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.cci.ConnectionFactoryDefaultConstructor.defConstr", 
           "The connectionfactory-impl-class: [ {0} ] provides a default constructor.", new Object[] {connFactoryImpl.getName()} ));    
    }
    catch(NoSuchMethodException nsme)
    {
      result.addErrorDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.failed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.cci.ConnectionFactoryDefaultConstructor.noDefConstr", 
           "Error: The connectionfactory-impl-class: [ {0} ] must provide a default constructor.", new Object[] {connFactoryImpl.getName()} ));    
    }
    catch(SecurityException se)
    {
    }
    return result;
  }
}
