/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier.tests.ejb.messagebean;

import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.tools.verifier.Result;
import com.sun.enterprise.tools.verifier.SpecVersionMapper;
import com.sun.enterprise.tools.verifier.tests.ComponentNameConstructor;
import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbMessageBeanDescriptor;

import java.util.Collection;
import java.util.Iterator;

/**
 * Message-driven beans with container-managed transaction demarcation must use
 * Required or NotSupported transaction attribute.
 *
 * @author  Jerome Dochez
 * @version 
 */
public class HasValidMethodDescriptor extends MessageBeanTest {

  /** 
   * Run a verifier test against an individual declared message
   * drive bean component
   * 
   * @param descriptor the Enterprise Java Bean deployment descriptor
   * @return <code>Result</code> the results for this assertion
   */
  public Result check(EjbMessageBeanDescriptor descriptor) {

    Result result = getInitializedResult();
    ComponentNameConstructor compName = 
      getVerifierContext().getComponentNameConstructor();

    if (descriptor.getTransactionType().equals
        (EjbDescriptor.CONTAINER_TRANSACTION_TYPE)) {

      // returns the Message Listener methods and "ejbTimeout" if the bean is a
      // TimedObject. 
      Collection methods = descriptor.getTransactionMethodDescriptors();

      if (methods.size()==0) {
        addNaDetails(result, compName);
        result.notApplicable(smh.getLocalString
            (getClass().getName()+".notApplicable1",
             "Message-driven bean [ {0} ] does not define any method",
             new Object[] {descriptor.getName()}));                            
        return result;
      }

      Iterator iterator = methods.iterator();
      while(iterator.hasNext())
      {
        MethodDescriptor method = (MethodDescriptor) iterator.next();
        // if the MDB is also a TimedObject then don't check the
        // transaction attribute of ejbTimeout. The
        // timer/HasValidEjbTimeout test will check the transaction
        // attribute for ejbTimeout

        if( descriptor.isTimedObject() &&
            (method.getName()).equals("ejbTimeout") )
          continue;
        ContainerTransaction txAttr = descriptor.
          getContainerTransactionFor(method);
        if(txAttr == null)
        {
            if(getVerifierContext().getJavaEEVersion().compareTo(SpecVersionMapper.JavaEEVersion_5)<0) {
          // transaction attribute is not specified for method.
          addErrorDetails(result, compName);
          result.failed(smh.getLocalString
              (getClass().getName()+".failed4",
               "Error : Message-driven bean [ {0} ] method definition [ {1} ] does not have a valid container transaction descriptor.",
               new Object[] {descriptor.getName(), method.getName()}));                                 
            } // default transaction attr in EJB 3.0 is REQUIRED
          continue;
        }
        String ta = txAttr.getTransactionAttribute();
        if (ContainerTransaction.REQUIRED.equals(ta) || 
            ContainerTransaction.NOT_SUPPORTED.equals(ta)) {
          addGoodDetails(result, compName);
          result.passed(smh.getLocalString
              (getClass().getName()+".passed",
               "Message-driven bean [ {0} ] method definition [ {1} ] in assembly-descriptor is correct",
               new Object[] {descriptor.getName(), method.getName()}));                                            
        } else {
          addErrorDetails(result, compName);
          result.failed(smh.getLocalString
              (getClass().getName()+".failed3",
               "Error : Message-driven bean [ {0} ] method definition [ {1} ] transaction attribute must be Required or NotSupported",
               new Object[] {descriptor.getName(), method.getName()}));                                 
        }
      }
      return result;                    
    } else {
      addNaDetails(result, compName);
      result.notApplicable(smh.getLocalString
          (getClass().getName()+".notApplicable2",
           "Message-driven bean [ {0} ] does not use container-managed transaction",
           new Object[] {descriptor.getName()})); 
    }
    return result;
  }
}
