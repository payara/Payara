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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]
package org.glassfish.diagnostics.context.impl;

import static org.glassfish.diagnostics.context.ContextManager.WORK_CONTEXT_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.glassfish.contextpropagation.ContextMap;
import org.glassfish.contextpropagation.ContextViewFactory;
import org.glassfish.contextpropagation.spi.ContextMapHelper;
import org.glassfish.diagnostics.context.Context;
import org.junit.Test;
import org.junit.runner.RunWith;

import mockit.Expectations;
import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.integration.junit4.JMockit;

@RunWith(JMockit.class)
public class ContextManagerImplUnitTest {

    /**
     * Verify that ContextManagerImpl initialization registers a ContextViewFactory with the ContextMapHelper.
     *
     * This test assumes only that initialization takes place by the time the first new ContextManagerImpl has been created.
     */
    @Test
    public void testViewFactoryRegistration() {
        new MockUp<ContextMapHelper>() {
            
            @Mock
            public void registerContextFactoryForPrefixNamed(String prefixName, ContextViewFactory factory) {
                assertEquals(prefixName, WORK_CONTEXT_KEY);
                assertTrue("org.glassfish.diagnostics.context.impl.ContextManagerImpl$DiagnosticContextViewFactory"
                        .equals(factory.getClass().getName()));
            }
        };

        // Initialization takes place by the time the first new ContextManagerImpl has been created.
        @SuppressWarnings("unused")
        ContextManagerImpl cmi = new ContextManagerImpl();
    }

    /**
     * Verify the expected delegation to ContextMap by ContextManagerImpl on invocation of getContext.
     */
    //@Test
    public void testGetContextUseOfContextMap_new(@Mocked final ContextMap mockedContextMap) throws Exception {
        new Expectations() {

            // We expect ContextManagerImpl to call getScopeAwareContextMap, but
            // we also need that method to return a ContextMap instance so
            // we tell the mocking framework to return an instance.
            ContextMapHelper expectationsRefContextMapHelper;
            {
                expectationsRefContextMapHelper.getScopeAwareContextMap();
                returns(mockedContextMap, null);
            }

            // We expect ContextManagerImpl to then go ahead and use the
            // ContextMap - in particular to call get (from which we deliberately
            // return null) and the createViewCapable (from which we return null
            // which is in practice an exceptional condition (which will result
            // in a WARNING log message) but does fine for this test.
            ContextMap expectationsRefContextMap = mockedContextMap;
            {
                expectationsRefContextMap.get(WORK_CONTEXT_KEY);
                returns(null, null);
                
                expectationsRefContextMap.createViewCapable(WORK_CONTEXT_KEY);
                returns(null, null);
            }
        };

        ContextManagerImpl cmi = new ContextManagerImpl();
        Context ci = cmi.getContext();
    }

}
