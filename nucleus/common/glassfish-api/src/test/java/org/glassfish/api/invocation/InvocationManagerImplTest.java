/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.api.invocation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.APP_CLIENT_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.EJB_INVOCATION;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVICE_STARTUP;
import static org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.glassfish.api.invocation.ComponentInvocation.ComponentInvocationType;
import org.junit.Test;

/**
 * Tests formal correctness of the {@link InvocationManagerImpl}. This includes some multi-threading tests but it does
 * not verify general thread-safety of the implementation. It does verify that in a controlled multi-threading situation
 * the implementation shows the expected behaviour.
 *
 * @author Jan Bernitt
 */
public class InvocationManagerImplTest {

    @Test
    public void singleHandlerIsCalled() {
        ComponentInvocationHandler handler = fakeHandler();
        InvocationManager manager = new InvocationManagerImpl(handler);
        assertCalledTimes(1, manager, handler, SERVLET_INVOCATION);
        assertCalledTimes(1, manager, handler, EJB_INVOCATION);
        assertCalledTimes(1, manager, handler, APP_CLIENT_INVOCATION);
    }

    @Test
    public void multipleHandlersAreCalled() {
        ComponentInvocationHandler handler = fakeHandler();
        InvocationManager manager = new InvocationManagerImpl(handler, handler, handler);
        assertCalledTimes(3, manager, handler, SERVLET_INVOCATION);
        assertCalledTimes(3, manager, handler, EJB_INVOCATION);
        assertCalledTimes(3, manager, handler, APP_CLIENT_INVOCATION);
    }

    @Test
    public void nullHandlerDoesNotCauseErrors() {
        InvocationManager manager = new InvocationManagerImpl();
        assertCalledTimes(0, manager, null, SERVLET_INVOCATION);
        assertCalledTimes(0, manager, null, EJB_INVOCATION);
        assertCalledTimes(0, manager, null, APP_CLIENT_INVOCATION);
    }

    @Test
    public void emptyHandlerDoesNotCauseErrors() {
        InvocationManager manager = new InvocationManagerImpl(new ComponentInvocationHandler[0]);
        assertCalledTimes(0, manager, null, SERVLET_INVOCATION);
        assertCalledTimes(0, manager, null, EJB_INVOCATION);
        assertCalledTimes(0, manager, null, APP_CLIENT_INVOCATION);
    }

    @Test
    public void singleRegisteredHandlerIsCalled() {
        InvocationManager manager = new InvocationManagerImpl();
        RegisteredComponentInvocationHandler handler = fakeRegisteredHandler();
        manager.registerComponentInvocationHandler(SERVLET_INVOCATION, handler);
        assertCalledTimes(1, manager, handler.getComponentInvocationHandler(), SERVLET_INVOCATION);
        assertCalledTimes(0, manager, handler.getComponentInvocationHandler(), EJB_INVOCATION);
        assertCalledTimes(0, manager, handler.getComponentInvocationHandler(), APP_CLIENT_INVOCATION);
    }

    @Test
    public void multipleRegisteredHandlersAreCalled() {
        InvocationManager manager = new InvocationManagerImpl();
        RegisteredComponentInvocationHandler servletHandler = fakeRegisteredHandler();
        manager.registerComponentInvocationHandler(SERVLET_INVOCATION, servletHandler);
        RegisteredComponentInvocationHandler ejbHandler = fakeRegisteredHandler();
        manager.registerComponentInvocationHandler(EJB_INVOCATION, ejbHandler);
        assertCalledTimes(1, manager, servletHandler.getComponentInvocationHandler(), SERVLET_INVOCATION);
        assertCalledTimes(0, manager, servletHandler.getComponentInvocationHandler(), EJB_INVOCATION);
        assertCalledTimes(0, manager, servletHandler.getComponentInvocationHandler(), APP_CLIENT_INVOCATION);
        assertCalledTimes(0, manager, ejbHandler.getComponentInvocationHandler(), SERVLET_INVOCATION);
        assertCalledTimes(1, manager, ejbHandler.getComponentInvocationHandler(), EJB_INVOCATION);
        assertCalledTimes(0, manager, ejbHandler.getComponentInvocationHandler(), APP_CLIENT_INVOCATION);
    }

    @Test
    public void exceptionInHandlerBeforePreInvokeDoesStillPushInvocation() {
        ComponentInvocationHandler handler = fakeHandler();
        doThrow(new RuntimeException("error in handler")).when(handler).beforePreInvoke(any(), any(), any());
        InvocationManager manager = new InvocationManagerImpl(handler);
        assertCalledTimes(1, manager, handler, SERVLET_INVOCATION);
    }

    @Test
    public void exceptionInHandlerAfterPreInvokeDoesStillPushIncocation() {
        ComponentInvocationHandler handler = fakeHandler();
        doThrow(new RuntimeException("error in handler")).when(handler).afterPreInvoke(any(), any(), any());
        InvocationManager manager = new InvocationManagerImpl(handler);
        assertCalledTimes(1, manager, handler, SERVLET_INVOCATION);
    }

    @Test
    public void serviceStartupPreInvokeDoesNotPushInvocation() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.preInvoke(newInvocation(SERVICE_STARTUP));
        assertNull(manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertEquals(emptyList(), manager.getAllInvocations());
    }

    @Test
    public void serviceStartupPostInvokeDoesNotPopInvocation() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.postInvoke(newInvocation(SERVICE_STARTUP));
        assertNull(manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertEquals(emptyList(), manager.getAllInvocations());
    }

    @Test
    public void exceptionInHandlerBeforePostInvokeDoesStillPopInvocation() {
        ComponentInvocationHandler handler = fakeHandler();
        doThrow(new RuntimeException("error in handler")).when(handler).beforePostInvoke(any(), any(), any());
        InvocationManager manager = new InvocationManagerImpl(handler);
        assertCalledTimes(1, manager, handler, SERVLET_INVOCATION);
    }

    @Test
    public void exceptionInHandlerAfterPostInvokeDoesStillPopInvocation() {
        ComponentInvocationHandler handler = fakeHandler();
        doThrow(new RuntimeException("error in handler")).when(handler).afterPostInvoke(any(), any(), any());
        InvocationManager manager = new InvocationManagerImpl(handler);
        assertCalledTimes(1, manager, handler, SERVLET_INVOCATION);
    }

    @Test(expected = InvocationException.class)
    public void postInvokeWithoutPreInvokeThrowsException() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.postInvoke(newInvocation(SERVLET_INVOCATION));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void getAllInvocationsReturnsCopy() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.preInvoke(newInvocation(SERVLET_INVOCATION));
        manager.preInvoke(newInvocation(SERVLET_INVOCATION));
        List<? extends ComponentInvocation> copy = manager.getAllInvocations();
        assertEquals(2, copy.size());
        manager.preInvoke(newInvocation(SERVLET_INVOCATION));
        assertEquals("copy is not independent", 2, copy.size());
        assertEquals(3, manager.getAllInvocations().size());
        ((List<ComponentInvocation>) copy).add(newInvocation(EJB_INVOCATION));
        assertEquals("changing copy changed inner state", 3, manager.getAllInvocations().size());
    }

    @Test
    public void popAllInvocationsPopsAllAndReturnsCopy() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.preInvoke(newInvocation(SERVLET_INVOCATION));
        manager.preInvoke(newInvocation(SERVLET_INVOCATION));
        List<? extends ComponentInvocation> copy = manager.getAllInvocations();
        assertEquals(2, copy.size());
        assertEquals(copy, manager.popAllInvocations());
        assertEquals(0, manager.getAllInvocations().size());
    }

    @Test
    public void putAllInvocationsReplacesInvocationStack() {
        InvocationManager manager = new InvocationManagerImpl();
        List<ComponentInvocation> expected = asList(newInvocation(SERVLET_INVOCATION), newInvocation(EJB_INVOCATION));
        manager.putAllInvocations(expected);
        assertEquals(expected, manager.getAllInvocations());
    }

    @Test
    public void servletInvocationChildThreadInitialisedToCurrentInvocation() throws InterruptedException {
        InvocationManager manager = new InvocationManagerImpl();
        ComponentInvocation prev = newInvocation(EJB_INVOCATION);
        ComponentInvocation cur = newInvocation(SERVLET_INVOCATION);
        manager.preInvoke(prev);
        manager.preInvoke(cur);
        runInChildThread(() -> {
            List<? extends ComponentInvocation> localFrames = manager.getAllInvocations();
            assertEquals(1, localFrames.size());
            assertEquals(SERVLET_INVOCATION, localFrames.get(0).getInvocationType());
        });
    }

    @Test
    public void nonEjbInvocationChildThreadInitialisedToCurrentInvocation() throws InterruptedException {
        InvocationManager manager = new InvocationManagerImpl();
        ComponentInvocation prev = newInvocation(SERVLET_INVOCATION);
        ComponentInvocation cur = newInvocation(APP_CLIENT_INVOCATION);
        manager.preInvoke(prev);
        manager.preInvoke(cur);
        runInChildThread(() -> {
            List<? extends ComponentInvocation> localFrames = manager.getAllInvocations();
            assertEquals(1, localFrames.size());
            assertEquals(APP_CLIENT_INVOCATION, localFrames.get(0).getInvocationType());
        });
    }

    @Test
    public void nullInvocationChildThreadInitialisedToEmptyFrames() throws InterruptedException {
        InvocationManager manager = new InvocationManagerImpl();
        runInChildThread(() -> assertEquals(emptyList(), manager.getAllInvocations()));
    }

    @Test
    public void emptyInvocationChildThreadInitialisedToEmptyFrames() throws InterruptedException {
        InvocationManager manager = new InvocationManagerImpl();
        ComponentInvocation cur = newInvocation(APP_CLIENT_INVOCATION);
        manager.preInvoke(cur);
        manager.postInvoke(cur);
        runInChildThread(() -> assertEquals(emptyList(), manager.getAllInvocations()));
    }

    @Test
    public void servletStartupInvocationChildThreadInitialisedToEmptyFrames() throws InterruptedException {
        InvocationManager manager = new InvocationManagerImpl();
        ComponentInvocation prev = newInvocation(APP_CLIENT_INVOCATION);
        ComponentInvocation cur = newInvocation(SERVICE_STARTUP);
        manager.preInvoke(prev);
        manager.preInvoke(cur);
        runInChildThread(() -> assertEquals(emptyList(), manager.getAllInvocations()));
        manager.postInvoke(cur); // startup is over, should init now
        runInChildThread(() -> assertEquals(1, manager.getAllInvocations().size()));
    }

    @Test
    public void peekEmptyAppEnvironmentReturnsNull() {
        InvocationManager manager = new InvocationManagerImpl();
        assertNull(manager.peekAppEnvironment());
    }

    @Test
    public void popEmptyAppEnvironmentDoesNotCauseError() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.popAppEnvironment();
    }

    @Test
    public void pushAddsAppEnvironmentToStack() {
        InvocationManager manager = new InvocationManagerImpl();
        ApplicationEnvironment env = () -> "Name";
        manager.pushAppEnvironment(env);
        assertSame(env, manager.peekAppEnvironment());
        ApplicationEnvironment env2 = () -> "Name2";
        manager.pushAppEnvironment(env2);
        assertSame(env2, manager.peekAppEnvironment());
        manager.popAppEnvironment();
        assertSame(env, manager.peekAppEnvironment());
        manager.popAppEnvironment();
        assertNull(manager.peekAppEnvironment());
    }

    @Test
    public void peekEmptyWebServiceMethodReturnsNull() {
        InvocationManager manager = new InvocationManagerImpl();
        assertNull(manager.peekWebServiceMethod());
    }

    @Test
    public void popEmptyWebServiceMethodDoesNotCauseError() {
        InvocationManager manager = new InvocationManagerImpl();
        manager.popWebServiceMethod();
    }

    @Test
    public void pushAddsWebServiceMethodToStack() throws Exception {
        InvocationManager manager = new InvocationManagerImpl();
        Method method = String.class.getMethod("length"); // does not matter which
        manager.pushWebServiceMethod(method);
        assertSame(method, manager.peekWebServiceMethod());
        Method env2 = String.class.getMethod("toCharArray"); // does not matter which
        manager.pushWebServiceMethod(env2);
        assertSame(env2, manager.peekWebServiceMethod());
        manager.popWebServiceMethod();
        assertSame(method, manager.peekWebServiceMethod());
        manager.popWebServiceMethod();
        assertNull(manager.peekWebServiceMethod());
    }

    @Test
    public void toStringDoesNotThrowException() {
        ComponentInvocation ci = newInvocation(SERVLET_INVOCATION);
        assertNotNull(ci.toString()); // test bare instance
        ci.setAppName("appName");
        ci.setInstanceName("instanceName");
        ci.setComponentId("componentId");
        ci.setContainer("container");
        assertEquals(Integer.toHexString(System.identityHashCode(ci)) + "@" + ci.getClass().getName() + "\n" +
                "\tcomponentId=componentId\n" +
                "\ttype=SERVLET_INVOCATION\n" +
                "\tinstance=instanceName\n" +
                "\tcontainer=container\n" +
                "\tappName=appName\n", ci.toString());
        ci.setInstanceName(null);
        ci.setInstance("instance");
        assertEquals(Integer.toHexString(System.identityHashCode(ci)) + "@" + ci.getClass().getName() + "\n" +
                "\tcomponentId=componentId\n" +
                "\ttype=SERVLET_INVOCATION\n" +
                "\tinstance=instance\n" +
                "\tcontainer=container\n" +
                "\tappName=appName\n", ci.toString());
    }

    private static void runInChildThread(Runnable test) throws InterruptedException {
        AtomicReference<Throwable> thrownByChild = new AtomicReference<>();
        Thread child = new Thread(() -> {
            try {
                test.run();
            } catch (Throwable e) {
                thrownByChild.set(e);
            }
        });
        child.start();
        child.join();
        Throwable thrown = thrownByChild.get();
        if (thrown != null) {
            if (thrown instanceof AssertionError) {
                throw (AssertionError) thrown;
            }
            throw new AssertionError("Child threw", thrown);
        }
    }

    private static RegisteredComponentInvocationHandler fakeRegisteredHandler() {
        ComponentInvocationHandler componentHandler = fakeHandler();
        RegisteredComponentInvocationHandler handler = mock(RegisteredComponentInvocationHandler.class);
        when(handler.getComponentInvocationHandler()).thenReturn(componentHandler);
        return handler;
    }

    private static ComponentInvocationHandler fakeHandler() {
        ComponentInvocationHandler handler = mock(ComponentInvocationHandler.class);
        return handler;
    }

    private static ComponentInvocation newInvocation(ComponentInvocationType type) {
        ComponentInvocation cur = new ComponentInvocation();
        cur.setInvocationType(type);
        return cur;
    }

    private static void assertCalledTimes(int times, InvocationManager manager, ComponentInvocationHandler handler,
            ComponentInvocationType type) {
        if (handler != null) {
            reset(handler);
        }
        assertCallTimesNonNested(times, manager, handler, type);

        if (handler != null) {
            reset(handler);
        }
        assertCallTimesNested(times, manager, handler, type);

        if (handler != null) {
            reset(handler);
        }
        assertCallTimesNested2Levels(times, manager, handler, type);
    }

    private static void assertCallTimesNested2Levels(int times, InvocationManager manager,
            ComponentInvocationHandler handler, ComponentInvocationType type) {
        ComponentInvocation level1 = newInvocation(type);
        ComponentInvocation level2 = newInvocation(type);
        ComponentInvocation level3 = newInvocation(type);
        manager.preInvoke(level1);
        assertSame(level1, manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        manager.preInvoke(level2);
        assertSame(level2, manager.getCurrentInvocation());
        assertSame(level1, manager.getPreviousInvocation());
        if (handler != null) {
            assertPreCalledTimes(times, handler, level1, level2);
        }
        manager.preInvoke(level3);
        assertSame(level3, manager.getCurrentInvocation());
        assertSame(level2, manager.getPreviousInvocation());
        if (handler != null) {
            assertPreCalledTimes(times, handler, level2, level3);
        }
        manager.postInvoke(level3);
        assertSame(level2, manager.getCurrentInvocation());
        assertSame(level1, manager.getPreviousInvocation());
        if (handler != null) {
            assertPostCalledTimes(times, handler, level2, level3);
        }
        manager.postInvoke(level2);
        assertSame(level1, manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        if (handler != null) {
            assertPostCalledTimes(times, handler, level1, level2);
        }
        manager.postInvoke(level1);
        assertNull(manager.getCurrentInvocation());
    }

    private static void assertCallTimesNested(int times, InvocationManager manager, ComponentInvocationHandler handler,
            ComponentInvocationType type) {
        ComponentInvocation outer = newInvocation(type);
        ComponentInvocation inner = newInvocation(type);
        manager.preInvoke(outer);
        assertSame(outer, manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertEquals(singletonList(outer), manager.getAllInvocations());
        manager.preInvoke(inner);
        assertSame(inner, manager.getCurrentInvocation());
        assertSame(outer, manager.getPreviousInvocation());
        assertEquals(asList(outer, inner), manager.getAllInvocations());
        if (handler != null) {
            assertPreCalledTimes(times, handler, outer, inner);
        }
        manager.postInvoke(inner);
        assertSame(outer, manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertEquals(singletonList(outer), manager.getAllInvocations());
        if (handler != null) {
            assertPostCalledTimes(times, handler, outer, inner);
        }
        manager.postInvoke(outer);
        assertNull(manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertEquals(emptyList(), manager.getAllInvocations());
    }

    private static void assertCallTimesNonNested(int times, InvocationManager manager,
            ComponentInvocationHandler handler, ComponentInvocationType type) {
        ComponentInvocation cur = newInvocation(type);
        assertTrue(manager.isInvocationStackEmpty());
        manager.preInvoke(cur);
        assertSame(cur, manager.getCurrentInvocation());
        assertNull(manager.getPreviousInvocation());
        assertFalse(manager.isInvocationStackEmpty());
        assertEquals(singletonList(cur), manager.getAllInvocations());
        manager.postInvoke(cur);
        assertNull(manager.getCurrentInvocation());
        assertTrue(manager.isInvocationStackEmpty());
        assertEquals(emptyList(), manager.getAllInvocations());
        if (handler != null) {
            assertCalledTimes(times, handler, null, cur);
        }
    }

    private static void assertCalledTimes(int times, ComponentInvocationHandler handler, ComponentInvocation prev,
            ComponentInvocation cur) {
        assertPreCalledTimes(times, handler, prev, cur);
        assertPostCalledTimes(times, handler, prev, cur);
    }

    private static void assertPostCalledTimes(int times, ComponentInvocationHandler handler, ComponentInvocation prev,
            ComponentInvocation cur) {
        verify(handler, times(times)).beforePostInvoke(any(), eq(prev), eq(cur));
        verify(handler, times(times)).afterPostInvoke(any(), eq(prev), eq(cur));
    }

    private static void assertPreCalledTimes(int times, ComponentInvocationHandler handler, ComponentInvocation prev,
            ComponentInvocation cur) {
        verify(handler, times(times)).beforePreInvoke(any(), eq(prev), eq(cur));
        verify(handler, times(times)).afterPreInvoke(any(), eq(prev), eq(cur));
    }
}
