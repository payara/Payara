/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2020-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  file and include the License.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.lang.reflect.Method;

import jakarta.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Timer;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.junit.Test;

import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricUtils;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Tests the business logic of {@link TimedInterceptor}.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class TimedInterceptorTest {

    private final InvocationContext context = mock(InvocationContext.class);
    private final MetricRegistryImpl registry = new MetricRegistryImpl(MetricRegistry.APPLICATION_SCOPE);

    @Test
    @Timed
    public void timerNoAttributes() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(absolute = true)
    public void timerAbsolute() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed()
    public void timerReusable() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(name = "name")
    public void timerWithName() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed
    public void timerWithDisplayName() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(description = "description")
    public void timerWithDescription() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(unit = "unit")
    public void timerWithUnit() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(tags = "a=b")
    public void timerWithTag() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(tags = {"a=b", "b=c"})
    public void timerWithTags() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(name= "name", tags = {"a=b", "b=c"})
    public void timerWithNameAndTags() throws Exception {
        assertTimed(0);
    }

    @Test
    @Timed(absolute = true, name= "name", tags = {"a=b", "b=c"})
    public void timerWithAbsoluteNameAndTagsReusable() throws Exception {
        assertTimed(0);
        assertTimed(3); // this tries to register the timer again, but it gets reused so we start at 2
    }

    private void assertTimed(int expectedStartCount) throws Exception {
        Method element = TestUtils.getTestMethod();
        Class<?> bean = getClass();
        AnnotationReader<Timed> reader = AnnotationReader.TIMED;
        Timer timer = MetricUtils.getOrRegisterByMetadataAndTags(registry, Timer.class,
                reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        TimedInterceptor.proceedTimed(context, element, bean, registry::getMetricCustomScope);
        assertEquals(expectedStartCount + 1, timer.getCount());
        TimedInterceptor.proceedTimed(context, element, bean, registry::getMetricCustomScope);
        assertEquals(expectedStartCount + 2, timer.getCount());
        verify(context, times(2)).proceed();
        // now test error when it does not exist
        try {
            TimedInterceptor.proceedTimed(context, element, bean, (metricId, type, string) -> null);
            fail("Expected a IllegalStateException because the metric does not exist");
        } catch (IllegalStateException ex) {
            assertEquals("No Timer with ID [" + reader.metricID(bean, element)
                    + "] found in application registry", ex.getMessage());
        }
        verify(context, times(2)).proceed();
        // test a annotated method that throws an exception
        when(context.proceed()).thenThrow(new RuntimeException("Error in method"));
        try {
            TimedInterceptor.proceedTimed(context, element, bean, registry::getMetricCustomScope);
            fail("Expected a RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("Error in method", ex.getMessage());
        }
        verify(context, times(3)).proceed();
        reset(context); //need to remove the 'thenThrow' behaviour
        assertEquals(expectedStartCount + 3, timer.getCount());
        //assertTrue(timer.getMeanRate() > 3d); // test should run in < 1 sec
    }
}
