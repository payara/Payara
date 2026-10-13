/*
 *
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2023-2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced with your identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 *
 */

package fish.payara.opentracing;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.RequestTracingService;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.glassfish.hk2.api.IterableProvider;
import org.glassfish.hk2.api.ServiceHandle;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RequestTracingBridgeProviderTest {

    @Test
    public void lazyProcessorIsStartAndEndRequiredUnconditionally() throws Exception {
        RequestTracingBridgeProvider provider = new RequestTracingBridgeProvider();

        // Mock the handle with reflection (provider class is package-private)
        ServiceHandle<RequestTracingService> serviceHandle = mock(ServiceHandle.class);
        IterableProvider<RequestTracingService> iterableProvider = mock(IterableProvider.class);
        when(iterableProvider.getHandle()).thenReturn(serviceHandle);

        java.lang.reflect.Field field = RequestTracingBridgeProvider.class.getDeclaredField("requestTracingServiceHandle");
        field.setAccessible(true);
        field.set(provider, iterableProvider);

        // Start with service inactive
        when(serviceHandle.isActive()).thenReturn(false);

        // Create provider with lazy processor
        RequestTracingBridgeProvider.LazyRequestTracingProcessor lazyProc =
            provider.new LazyRequestTracingProcessor();

        // isStartRequired() and isEndRequired() should return true unconditionally
        assertEquals("isStartRequired should always return true", true, lazyProc.isStartRequired());
        assertEquals("isEndRequired should always return true", true, lazyProc.isEndRequired());
    }

    @Test
    public void lazyProcessorDoesNotThrowWhenServiceInactiveAtBuildTime() throws Exception {
        RequestTracingBridgeProvider provider = new RequestTracingBridgeProvider();

        ServiceHandle<RequestTracingService> serviceHandle = mock(ServiceHandle.class);
        IterableProvider<RequestTracingService> iterableProvider = mock(IterableProvider.class);
        when(iterableProvider.getHandle()).thenReturn(serviceHandle);
        when(serviceHandle.isActive()).thenReturn(false);

        java.lang.reflect.Field field = RequestTracingBridgeProvider.class.getDeclaredField("requestTracingServiceHandle");
        field.setAccessible(true);
        field.set(provider, iterableProvider);

        RequestTracingBridgeProvider.LazyRequestTracingProcessor lazyProc =
            provider.new LazyRequestTracingProcessor();

        // Build tracer with lazy processor (service inactive)
        Tracer tracer = SdkTracerProvider.builder()
                .addSpanProcessor(lazyProc)
                .build().get("test");

        // Start and end a span - should not throw
        Span root = tracer.spanBuilder("test-span").startSpan();
        Span child = tracer.spanBuilder("child").setParent(Context.root().with(root)).startSpan();
        child.end();
        root.end();
        // Test passes if no exception thrown
    }

    @Test
    public void lazyProcessorActivatesWhenServiceBecomesActive() throws Exception {
        RequestTracingBridgeProvider provider = new RequestTracingBridgeProvider();

        ServiceHandle<RequestTracingService> serviceHandle = mock(ServiceHandle.class);
        IterableProvider<RequestTracingService> iterableProvider = mock(IterableProvider.class);
        RequestTracingService requestTracingService = mock(RequestTracingService.class);
        when(iterableProvider.getHandle()).thenReturn(serviceHandle);
        when(requestTracingService.isRequestTracingEnabled()).thenReturn(true);
        when(requestTracingService.startTrace(any(RequestTraceSpan.class))).thenAnswer(inv -> inv.getArgument(0));

        java.lang.reflect.Field field = RequestTracingBridgeProvider.class.getDeclaredField("requestTracingServiceHandle");
        field.setAccessible(true);
        field.set(provider, iterableProvider);

        // Start with service inactive
        when(serviceHandle.isActive()).thenReturn(false);
        when(iterableProvider.get()).thenReturn(requestTracingService);

        RequestTracingBridgeProvider.LazyRequestTracingProcessor lazyProc =
            provider.new LazyRequestTracingProcessor();

        Tracer tracer = SdkTracerProvider.builder()
                .addSpanProcessor(lazyProc)
                .build().get("test");

        // Start and end span while service inactive - nothing reaches RT service
        Span root1 = tracer.spanBuilder("root-span-1").startSpan();
        root1.end();
        verify(requestTracingService, never()).startTrace(any(RequestTraceSpan.class));

        // Now service becomes active for future delegate() calls
        when(serviceHandle.isActive()).thenReturn(true);

        // Start a second root - now the delegate should be created and work
        Span root2 = tracer.spanBuilder("root-span-2").startSpan();
        root2.end();

        // Verify that startTrace was called after activation
        ArgumentCaptor<RequestTraceSpan> startCaptor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(requestTracingService).startTrace(startCaptor.capture());
        RequestTraceSpan capturedSpan = startCaptor.getValue();
        assertEquals("Root span should be TRACE_START", EventType.TRACE_START, capturedSpan.getEventType());

        // Verify traceSpan was called
        verify(requestTracingService).traceSpan(any(RequestTraceSpan.class), anyLong());
    }

    @Test
    public void lazyProcessorCreatesExactlyOneDelegateUnderConcurrentCreation() throws Exception {
        int N = 8;
        RequestTracingBridgeProvider provider = new RequestTracingBridgeProvider();

        ServiceHandle<RequestTracingService> serviceHandle = mock(ServiceHandle.class);
        IterableProvider<RequestTracingService> iterableProvider = mock(IterableProvider.class);
        RequestTracingService requestTracingService = mock(RequestTracingService.class);
        when(iterableProvider.getHandle()).thenReturn(serviceHandle);
        when(requestTracingService.isRequestTracingEnabled()).thenReturn(true);
        when(requestTracingService.startTrace(any(RequestTraceSpan.class))).thenAnswer(inv -> inv.getArgument(0));

        CountDownLatch inWindow = new CountDownLatch(N);
        when(serviceHandle.isActive()).thenAnswer(inv -> {
            inWindow.countDown();
            inWindow.await(200, TimeUnit.MILLISECONDS);
            return true;
        });

        AtomicInteger getInvocationCount = new AtomicInteger(0);
        when(iterableProvider.get()).thenAnswer(inv -> {
            getInvocationCount.incrementAndGet();
            return requestTracingService;
        });

        java.lang.reflect.Field field = RequestTracingBridgeProvider.class.getDeclaredField("requestTracingServiceHandle");
        field.setAccessible(true);
        field.set(provider, iterableProvider);

        RequestTracingBridgeProvider.LazyRequestTracingProcessor lazyProc =
            provider.new LazyRequestTracingProcessor();

        Tracer tracer = SdkTracerProvider.builder()
                .addSpanProcessor(lazyProc)
                .build().get("test");

        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(N);
        ConcurrentLinkedQueue<Exception> workerExceptions = new ConcurrentLinkedQueue<>();

        // N threads all released simultaneously
        for (int i = 0; i < N; i++) {
            Thread t = new Thread(() -> {
                try {
                    latch.await();
                    Span root = tracer.spanBuilder("root-span-" + Thread.currentThread().getId()).startSpan();
                    root.end();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    workerExceptions.offer(e);
                } finally {
                    completionLatch.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        latch.countDown();
        boolean finished = completionLatch.await(5, TimeUnit.SECONDS);
        if (!finished) {
            throw new AssertionError("Timeout waiting for " + N + " threads to complete");
        }

        assertEquals("Worker threads should have no exceptions", 0, workerExceptions.size());

        // Verify startTrace was called exactly N times (once per root span)
        ArgumentCaptor<RequestTraceSpan> captor = ArgumentCaptor.forClass(RequestTraceSpan.class);
        verify(requestTracingService, times(N)).startTrace(captor.capture());
        assertEquals("startTrace should be called exactly " + N + " times", N, captor.getAllValues().size());

        // Verify the delegate was created exactly once (IterableProvider.get() called once)
        assertEquals("IterableProvider.get() should be called exactly once", 1, getInvocationCount.get());
    }
}
