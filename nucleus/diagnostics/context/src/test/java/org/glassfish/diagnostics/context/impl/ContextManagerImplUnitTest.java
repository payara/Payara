/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.diagnostics.context.impl;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Verifications;
import mockit.Expectations;
import mockit.NonStrictExpectations;
import mockit.integration.junit4.JMockit;

import org.glassfish.contextpropagation.*;
import org.glassfish.contextpropagation.spi.ContextMapHelper;

import org.glassfish.diagnostics.context.Context;
import org.glassfish.diagnostics.context.ContextManager;

import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JMockit.class)
public class ContextManagerImplUnitTest {

 /**
  * Verify that ContextManagerImpl initialization registers a
  * ContextViewFactory with the ContextMapHelper.
  *
  * This test assumes only that initialization takes place by the time
  * the first new ContextManagerImpl has been created.
  */
  @Test
  public void testViewFactoryRegistration()
  {
    new MockUp<ContextMapHelper>(){
      @Mock
      public void registerContextFactoryForPrefixNamed(
        String prefixName, ContextViewFactory factory)
      {
        Assert.assertEquals(prefixName, ContextManager.WORK_CONTEXT_KEY);
        Assert.assertTrue("org.glassfish.diagnostics.context.impl.ContextManagerImpl$DiagnosticContextViewFactory".equals(factory.getClass().getName()));
      }
    };

    ContextManagerImpl cmi = new ContextManagerImpl();
  }

 /**
  * Verify the expected delegation to ContextMap by
  * ContextManagerImpl on invocation of getContext.
  */
  @Test
  public void testGetContextUseOfContextMap_new(
    @Mocked final ContextMap mockedContextMap)
  throws Exception
  {
    new Expectations(){

      // We expect ContextManagerImpl to call getScopeAwareContextMap, but
      // we also need that method to return a ContextMap instance so 
      // we tell the mocking framework to return an instance. 
      ContextMapHelper expectationsRefContextMapHelper;
      {
        expectationsRefContextMapHelper.getScopeAwareContextMap(); returns(mockedContextMap);
      }

      // We expect ContextManagerImpl to then go ahead and use the
      // ContextMap - in particular to call get (from which we deliberately
      // return null) and the createViewCapable (from which we return null
      // which is in practice an exceptional condition (which will result
      // in a WARNING log message) but does fine for this test.
      ContextMap expectationsRefContextMap = mockedContextMap;
      {
        expectationsRefContextMap.get(ContextManager.WORK_CONTEXT_KEY); returns(null);
        expectationsRefContextMap.createViewCapable(ContextManager.WORK_CONTEXT_KEY); returns(null);
      }
    };

    ContextManagerImpl cmi = new ContextManagerImpl();
    Context ci = cmi.getContext();
  }

}
