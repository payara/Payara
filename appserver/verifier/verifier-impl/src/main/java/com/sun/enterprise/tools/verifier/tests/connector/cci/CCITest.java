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
 * CCITest.java
 *
 * Created on August 28, 2002
 */

package com.sun.enterprise.tools.verifier.tests.connector.cci;

import com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.ConnectionDefDescriptor;
import com.sun.enterprise.deployment.OutboundResourceAdapter;
import com.sun.enterprise.tools.verifier.tests.*;
import java.lang.ClassLoader;
import java.util.Iterator;
import java.util.Set;
/**
 * Contains helper methods for all tests pertinent to CCI 
 *
 * @author Anisha Malhotra 
 * @version 
 */
public abstract class CCITest extends ConnectorTest {

  /**
   * <p>
   * Checks whether the resource adapater is implementing the CCI interfaces
   * </p>
   * @param descriptor the deployment descriptor
   * @param result to put the result
   * @return true if the CCI is implemented
   */
  protected boolean isCCIImplemented(ConnectorDescriptor descriptor, 
      Result result) {
    ComponentNameConstructor compName = 
      getVerifierContext().getComponentNameConstructor();
    OutboundResourceAdapter outboundRA =
      descriptor.getOutboundResourceAdapter();
    if(outboundRA == null)
    {
      return false;
    }
    Set connDefs = outboundRA.getConnectionDefs();
    Iterator iter = connDefs.iterator();
    while(iter.hasNext()) 
    {
      ConnectionDefDescriptor connDefDesc = (ConnectionDefDescriptor)
        iter.next();
      // check if intf implements javax.resource.cci.ConnectionFactory
      String intf = connDefDesc.getConnectionFactoryIntf();
      Class implClass = null;
      try
      {
        implClass = Class.forName(intf, false, getVerifierContext().getClassLoader());
      }
      catch(ClassNotFoundException e)
      {
      result.addErrorDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
        result.failed(smh.getLocalString
            ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.isClassLoadable.failed", 
             "The class [ {0} ] is not contained in the archive file",
             new Object[] {intf}));                
        continue;
      }
      if(isImplementorOf(implClass, "javax.resource.cci.ConnectionFactory"))
      {
        return true;
      }
    }
    return false;
  }


  /**
   * <p>
   * Returns the connection-interface that implements
   * "javax.resource.cci.Connection"
   * </p>
   * @param descriptor the deployment descriptor
   * @param result to put the result
   * @return interface name 
   */
  protected String getConnectionInterface(ConnectorDescriptor descriptor,
      Result result)
  {
    ComponentNameConstructor compName = 
      getVerifierContext().getComponentNameConstructor();
    OutboundResourceAdapter outboundRA =
      descriptor.getOutboundResourceAdapter();
    if(outboundRA == null)
    {
      return null;
    }
    Set connDefs = outboundRA.getConnectionDefs();
    Iterator iter = connDefs.iterator();
    while(iter.hasNext()) 
    {
      ConnectionDefDescriptor connDefDesc = (ConnectionDefDescriptor)
        iter.next();
      String intf = connDefDesc.getConnectionIntf();
      VerifierTestContext context = getVerifierContext();
      ClassLoader jcl = context.getRarClassLoader();
      Class intfClass = null;
      try
      {
        intfClass = Class.forName(intf, false, getVerifierContext().getClassLoader());    
      }
      catch(ClassNotFoundException e)
      {
        result.addErrorDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.failed(smh.getLocalString
            ("com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest.isClassLoadable.failed", 
             "The class [ {0} ] is not contained in the archive file",
             new Object[] {intf}));                
        continue;
      }
      if(isImplementorOf(intfClass, "javax.resource.cci.Connection"))
      {
        return intf;
      }
    }
    return null;
  }

}
