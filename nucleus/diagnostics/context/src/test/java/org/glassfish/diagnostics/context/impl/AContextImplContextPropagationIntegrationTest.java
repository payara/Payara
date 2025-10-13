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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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

import static java.lang.Thread.currentThread;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.LinkedList;
import java.util.List;

import org.glassfish.contextpropagation.bootstrap.ContextBootstrap;
import org.glassfish.diagnostics.context.Context;
import org.glassfish.diagnostics.context.ContextManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration test between diagnostics context implementation and context propagation.
 *
 * This test explicitly initializes the context propagation module and the ContextManagerImpl under test.
 */
public class AContextImplContextPropagationIntegrationTest {

    private ContextManager mContextManager;

    @BeforeClass
    public static void setUpOncePerTestClass() throws Exception {
        // Natural place to add initialization of context propagation but
        // the ContextBootstrap therein initializes as part of class
        // load. We could wait for lazy initialization per test but
        // clearer to force it to happen here
        Class.forName(ContextBootstrap.class.getName());
    }

    @Before
    public void setUpOncePerTestMethod() {
        mContextManager = new ContextManagerImpl();
    }

    /**
     * Verify that multiple calls to get the current diagnostics context return the same instance.
     */
    @Test
    public void testThreadLocalBehaviour() {
        Context diagnosticsContextStart = mContextManager.getContext();

        assertEquals(
                "The implementation class of diagnosticsContext1 is not as expected.",
                diagnosticsContextStart.getClass().getName(), ContextImpl.class.getName());

        for (int i = 0; i < 13; i++) {
            Context diagnosticsContext = mContextManager.getContext();

            assertSame(
                    "The diagnostics context instance returned in iteration " + i + 
                    " is not the same instance as fetched at the start of the test.",
                    diagnosticsContextStart, diagnosticsContext);
        }
    }

    /**
     * Verify that values set on the incumbent diagnostics context remain accessible on subsequent fetches of the
     * diagnostics context.
     */
    @Test
    public void testValuePersistence() {

        final String propagatingKey = "propagatingKey";
        final String propagatingValue = "propagatingValue";
        final String nonPropagatingKey = "nonPropagatingKey";
        final String nonPropagatingValue = "nonPropagatingValue";

        {
            Context diagnosticsContextStart = mContextManager.getContext();
            diagnosticsContextStart.put(propagatingKey, propagatingValue, true);
            diagnosticsContextStart.put(nonPropagatingKey, nonPropagatingValue, false);
        }

        for (int i = 0; i < 17; i++) {
            Context diagnosticsContext = mContextManager.getContext();

            assertEquals(
                    "The value associated with key " + propagatingKey + " is not as expected.", 
                    propagatingValue, diagnosticsContext.get(propagatingKey));
            assertEquals(
                    "The value associated with key " + nonPropagatingKey + " is not as expected.",
                    nonPropagatingValue, diagnosticsContext.get(nonPropagatingKey));
        }
    }

    /**
     *
     */
    @Test
    public void testValuePropagationAndNonPropagation() throws Exception {

        final String propagatingKey = "propagatingKey";
        final String propagatingValue = "propagatingValue";
        final String nonPropagatingKey = "nonPropagatingKey";
        final String nonPropagatingValue = "nonPropagatingValue";
        final ContextManager contextManager = mContextManager;
        final List<Throwable> exceptionList = new LinkedList<>();
        final List<Thread> threadList = new LinkedList<>();

        {
            Context diagnosticsContextStart = mContextManager.getContext();
            diagnosticsContextStart.put(propagatingKey, propagatingValue, true);
            diagnosticsContextStart.put(nonPropagatingKey, nonPropagatingValue, false);
        }

        for (int i = 0; i < 17; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        String threadName = currentThread().getName();
                        Context diagnosticsContext = contextManager.getContext();

                        assertEquals(
                                "The value associated with key " + propagatingKey + 
                                " on thread " + threadName + " is not as expected.",
                                propagatingValue, diagnosticsContext.get(propagatingKey));
                        assertNull(
                                "The null value should be associated with key " + nonPropagatingKey + 
                                " on thread " + threadName,
                                diagnosticsContext.get(nonPropagatingKey));
                    } catch (Throwable e) {
                        synchronized (exceptionList) {
                            exceptionList.add(e);
                        }
                    }
                }
            });
            
            thread.setName("Child_" + i + "_of_parent_'" + currentThread().getName() + "'");
            thread.start();
            threadList.add(thread);
        }

        for (Thread thread : threadList) {
            thread.join();
        }

        if (exceptionList.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (Throwable e : exceptionList) {
                sb.append("\n  ").append(e.getMessage());
            }
            sb.append("\n");

            // TODO: Enable this assertion when/if contextpropagation takes
            // place to child thread.
            /* Assert.fail("Compound failure: " + sb.toString()); */
        }
    }
}