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
 * CheckActivationSpecJavaBean.java
 *
 * Created on August 29, 2002
 */

package com.sun.enterprise.tools.verifier.tests.connector.messageinflow;

import com.sun.enterprise.tools.verifier.tests.connector.ConnectorTest;
import com.sun.enterprise.tools.verifier.tests.connector.ConnectorCheck;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.deployment.InboundResourceAdapter;
import com.sun.enterprise.deployment.MessageListener;
import com.sun.enterprise.deployment.EnvironmentProperty;
import com.sun.enterprise.tools.verifier.tests.*;
import java.util.*;
import java.beans.*;

/**
 * Test for each message-listener, that "activationspec-class" is a Java Bean.
 *
 * @author Anisha Malhotra 
 * @version 
 */
public class CheckActivationSpecJavaBean
    extends ConnectorTest 
    implements ConnectorCheck 
{

  /** <p>
   * Test for each message-listener, that "activationspec-class" is a Java Bean.
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
    if(!descriptor.getInBoundDefined())
    {
      result.addNaDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));
      result.notApplicable(smh.getLocalString
          ("com.sun.enterprise.tools.verifier.tests.connector.messageinflow.notApp",
           "Resource Adapter does not provide inbound communication"));
      return result;
    }
    InboundResourceAdapter ra = descriptor.getInboundResourceAdapter();
    Set msgListeners = ra.getMessageListeners();
    boolean oneFailed = false;
    Iterator iter = msgListeners.iterator();
    while(iter.hasNext()) 
    {
      MessageListener msgListener = (MessageListener) iter.next();
      String impl = msgListener.getActivationSpecClass();
      Class implClass = null;
      try
      {
        implClass = Class.forName(impl, false, getVerifierContext().getClassLoader());
      }
      catch(ClassNotFoundException e)
      {
        result.addErrorDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.failed(smh.getLocalString
            ("com.sun.enterprise.tools.verifier.tests.connector.messageinflow.nonexist",
             "Error: The class [ {0} ] as defined under activationspec-class in the deployment descriptor does not exist",
             new Object[] {impl}));
        return result;
      }
      Set configProps = msgListener.getConfigProperties();
      Iterator propIter = configProps.iterator();
      BeanInfo bi = null;
      try
      {
        bi = Introspector.getBeanInfo(implClass, Object.class);
      } 
      catch (IntrospectionException ie) {
        oneFailed = true;
        result.addErrorDetails(smh.getLocalString
            ("tests.componentNameConstructor",
             "For [ {0} ]",
             new Object[] {compName.toString()}));
        result.failed(smh.getLocalString
            (getClass().getName() + ".failed",
             "Error: The activationspec-class [ {0} ] is not JavaBeans compliant",
             new Object[] {impl} ));
        return result;
      }

      PropertyDescriptor[] properties = bi.getPropertyDescriptors();
      Hashtable<String, PropertyDescriptor> props = new Hashtable<String, PropertyDescriptor>();
      for(int i=0;i<properties.length;i++)
      {
        props.put(properties[i].getName(), properties[i]);
      }
      while(propIter.hasNext()) 
      {
        EnvironmentProperty envProp = (EnvironmentProperty) propIter.next();
        String name = envProp.getName();
        String type = envProp.getType();

        PropertyDescriptor propDesc = (PropertyDescriptor) props.get(
            name.substring(0,1).toLowerCase() + name.substring(1));
        if(propDesc != null)
        {
          if (propDesc.getReadMethod()==null || propDesc.getWriteMethod()==null)
          {
            oneFailed = true;
            result.addErrorDetails(smh.getLocalString
                ("tests.componentNameConstructor",
                 "For [ {0} ]",
                 new Object[] {compName.toString()}));
            result.failed(smh.getLocalString
                (getClass().getName() + ".failed1",
                 "Error: The activationspec-class [ {0} ] does not provide accessor methods for [ {1} ].",
                 new Object[] {impl, name} ));
            return result;
          }
        }
        else
        {
          oneFailed = true;
          result.addErrorDetails(smh.getLocalString
              ("tests.componentNameConstructor",
               "For [ {0} ]",
               new Object[] {compName.toString()}));
          result.failed(smh.getLocalString
              (getClass().getName() + ".failed1",
               "Error: The activationspec-class [ {0} ] does not provide accessor methods for [ {1} ].",
               new Object[] {impl, name} ));
          return result;
        }
      }
    }
    if(!oneFailed)
    {
      result.addGoodDetails(smh.getLocalString
          ("tests.componentNameConstructor",
           "For [ {0} ]",
           new Object[] {compName.toString()}));	
      result.passed(smh.getLocalString(getClass().getName() + ".passed",
            "Success: Each activationspec-class is a Java Bean"));                     
    }
    return result;
  }
}
