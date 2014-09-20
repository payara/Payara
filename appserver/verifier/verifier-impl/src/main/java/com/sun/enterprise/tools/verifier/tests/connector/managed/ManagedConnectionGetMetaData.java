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

package com.sun.enterprise.tools.verifier.tests.connector.managed;

import java.io.File;
import java.lang.reflect.Method;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.Verifier;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.tools.verifier.tests.*;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;

/**
 * Test that the return type of
 * javax.resource.spi.ManagedConnection.getMetaData() implements 
 * the ManagedConnectionMetaData interface.
 *
 * @author Anisha Malhotra 
 * @version 
 */
public class ManagedConnectionGetMetaData
    extends ConnectorTest 
    implements ConnectorCheck 
{
  /** <p>
   * Test that the return type of
   * javax.resource.spi.ManagedConnection.getMetaData() implements 
   * the ManagedConnectionMetaData interface.
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
	//File jarFile = Verifier.getJarFile(descriptor.getModuleDescriptor().getArchiveUri());
//        File f=Verifier.getArchiveFile(descriptor.getModuleDescriptor().getArchiveUri());
        Class c = findImplementorOf(descriptor, "javax.resource.spi.ManagedConnection");
    if(c == null)
    {
      result.addErrorDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.failed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.findImplementor.failed", 
           "Error: There is no implementation of the [ {0} ] provided",
           new Object[] {"javax.resource.spi.ManagedConnection"}));        
      return result;
    }
    // get return type of getMetaData()
    Method m = null;
    do {
      try {
        m = c.getMethod("getMetaData", (Class[])null);
      } catch(NoSuchMethodException nsme) {
      } catch(SecurityException se) {
      }
      c = c.getSuperclass();
    } while (m != null && c != Object.class);            
    if(m == null)
    {
      result.addErrorDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.failed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.managed.ManagedConnectionGetMetaData.failed", 
           "Error: There is no implementation of getMetaData() provided"));
      return result;
    }
    Class returnType = m.getReturnType();
    if(VerifierTest.isImplementorOf(returnType, 
          "javax.resource.spi.ManagedConnectionMetaData"))
    {
      result.addGoodDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.passed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.managed.ManagedConnectionGetMetaData.passed", 
           "ManagedConnection.getMetaData() returns an instance of the" + 
           "javax.resource.spi.ManagedConnectionMetaData interface"));
    }
    else
    {
      result.addErrorDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.failed(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.managed.ManagedConnectionGetMetaData.failed1", 
           "Error: getMetaData() does not return an instance of the" + 
           "javax.resource.spi.ManagedConnectionMetaData interface"));
    }
    return result;
  }
}
