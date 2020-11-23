/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
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
 */
package fish.payara.microprofile.metrics.cdi.interceptor;

import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricUtils;
import fish.payara.microprofile.metrics.impl.Clock;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.eclipse.microprofile.metrics.annotation.ConcurrentGauge;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.interceptor.InvocationContext;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * Tests the business logic of the {@link ConcurrentGaugeInterceptor}.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class ConcurrentGaugeInterceptorTest implements Clock {

    private final InvocationContext context = mock(InvocationContext.class);
    private MetricRegistry registry;

    private int minute = 0;

    @Before
    public void setUp() {
        registry = new MetricRegistryImpl(Type.APPLICATION, this);
    }

    @Test
    @ConcurrentGauge
    public void concurrentGaugeNoAttributes() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(absolute = true)
    public void concurrentGaugeAbsolute() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge()
    public void concurrentGaugeReusable() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(name = "name")
    public void concurrentGaugeWithName() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(displayName = "displayName")
    public void concurrentGaugeWithDisplayName() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(description = "description")
    public void concurrentGaugeWithDescription() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(unit = "unit")
    public void concurrentGaugeWithUnit() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(tags = "a=b")
    public void concurrentGaugeWithTag() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(tags = {"a=b", "b=c"})
    public void concurrentGaugeWithTags() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(name= "name", tags = {"a=b", "b=c"})
    public void concurrentGaugeWithNameAndTags() throws Exception {
        assertGaugeProbesThreads();
    }

    @Test
    @ConcurrentGauge(absolute = true, name= "name", tags = {"a=b", "b=c"})
    public void concurrentGaugeWithAbsoluteNameAndTagsReusable() throws Exception {
        assertGaugeProbesThreads(0);
        assertGaugeProbesThreads(0); // this tries to register the counter again, but it gets reused so we start at 2
    }

    public static class Bean {

        @ConcurrentGauge
        public Bean() {

        }
    }

    @Test
    public void concurrentGaugeConstructorNoAttributes() throws Exception {
        assertGaugeProbesThreads(0, Bean.class, Bean.class.getConstructor());
    }

    private void assertGaugeProbesThreads() throws Exception {
        assertGaugeProbesThreads(0);
    }

    private void assertGaugeProbesThreads(long expectedStartCount) throws Exception {
        Method element = TestUtils.getTestMethod();
        Class<?> bean = getClass();
        assertGaugeProbesThreads(expectedStartCount, bean, element);
    }

    private <E extends Member & AnnotatedElement> void assertGaugeProbesThreads(long expectedStartCount, Class<?> bean, E element)
            throws Exception, InterruptedException {
        AnnotationReader<ConcurrentGauge> reader = AnnotationReader.CONCURRENT_GAUGE;
        org.eclipse.microprofile.metrics.ConcurrentGauge gauge = MetricUtils.getOrRegisterByMetadataAndTags(
                registry, org.eclipse.microprofile.metrics.ConcurrentGauge.class,
                reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        CompletableFuture<Void> waiter = new CompletableFuture<>();
        Mockito.when(context.proceed()).thenAnswer(invocation -> {
            waiter.get();
            return null;
        });
        List<Exception> thrown = new ArrayList<>();
        Runnable proceed = () -> {
            try {
                ConcurrentGaugeInterceptor.proceedProbed(context, element, bean, registry::getMetric);
            } catch (Exception ex) {
                thrown.add(ex);
                throw new RuntimeException(ex);
            }
        };
        Thread async1 = new Thread(proceed);
        async1.start();
        await().until(gauge::getCount, equalTo(expectedStartCount + 1));
        Thread async2 = new Thread(proceed);
        async2.start();
        await().until(gauge::getCount, equalTo(expectedStartCount + 2));
        waiter.complete(null);
        async1.join();
        async2.join();
        assertEquals(0, thrown.size());
        assertEquals(0, gauge.getCount());
        // now test error when it does not exist
        try {
            ConcurrentGaugeInterceptor.proceedProbed(context, element, bean, (metricId, type) -> null);
            fail("Expected a IllegalStateException because the metric does not exist");
        } catch (IllegalStateException ex) {
            assertEquals("No ConcurrentGauge with ID [" + reader.metricID(bean, element)
                    + "] found in application registry", ex.getMessage());
        }
        minute++; // simulate next minute starts
        assertEquals(0, gauge.getMin());
        assertEquals(2, gauge.getMax());
        minute++;
        assertEquals(0, gauge.getMin());
        assertEquals(0, gauge.getMax());
    }

    @Override
    public long getTick() {
        return TimeUnit.MINUTES.toNanos(minute);
    }

    @Override
    public long getTime() {
        return TimeUnit.MINUTES.toMillis(minute);
    }
}
