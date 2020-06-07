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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.annotation.SimplyTimed;
import org.junit.Test;

import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricUtils;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Tests the business logic of {@link SimplyTimedInterceptor}.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class SimplyTimedInterceptorTest {

    private final InvocationContext context = mock(InvocationContext.class);
    private final MetricRegistryImpl registry = new MetricRegistryImpl();

    @Test
    @SimplyTimed
    public void simpleTimerNoAttributes() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(absolute = true)
    public void simpleTimerAbsolute() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(reusable = true)
    public void simpleTimerReusable() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(name = "name")
    public void simpleTimerWithName() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(displayName = "displayName")
    public void simpleTimerWithDisplayName() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(description = "description")
    public void simpleTimerWithDescription() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(unit = "unit")
    public void simpleTimerWithUnit() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(tags = "a=b")
    public void simpleTimerWithTag() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(tags = {"a=b", "b=c"})
    public void simpleTimerWithTags() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(name= "name", tags = {"a=b", "b=c"})
    public void simpleTimerWithNameAndTags() throws Exception {
        assertTimed(0);
    }

    @Test
    @SimplyTimed(absolute = true, name= "name", tags = {"a=b", "b=c"}, reusable = true)
    public void simpleTimerWithAbsoluteNameAndTagsReusable() throws Exception {
        assertTimed(0);
        assertTimed(3); // this tries to register the timer again, but it gets reused so we start at 2
    }

    private void assertTimed(int expectedStartCount) throws Exception {
        Method element = TestUtils.getTestMethod();
        Class<?> bean = getClass();
        AnnotationReader<SimplyTimed> reader = AnnotationReader.SIMPLY_TIMED;
        SimpleTimer timer = MetricUtils.getOrRegisterByMetadataAndTags(registry, SimpleTimer.class,
                reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        SimplyTimedInterceptor.proceedTimed(context, element, bean, registry::getMetric);
        assertEquals(expectedStartCount + 1, timer.getCount());
        SimplyTimedInterceptor.proceedTimed(context, element, bean, registry::getMetric);
        assertEquals(expectedStartCount + 2, timer.getCount());
        verify(context, times(2)).proceed();
        // now test error when it does not exist
        try {
            SimplyTimedInterceptor.proceedTimed(context, element, bean, (metricId, type) -> null);
            fail("Expected a IllegalStateException because the metric does not exist");
        } catch (IllegalStateException ex) {
            assertEquals("No SimpleTimer with ID [" + reader.metricID(bean, element)
                    + "] found in application registry", ex.getMessage());
        }
        verify(context, times(2)).proceed();
        // test a annotated method that throws an exception
        when(context.proceed()).thenThrow(new RuntimeException("Error in method"));
        try {
            SimplyTimedInterceptor.proceedTimed(context, element, bean, registry::getMetric);
            fail("Expected a RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("Error in method", ex.getMessage());
        }
        verify(context, times(3)).proceed();
        reset(context); //need to remove the 'thenThrow' behaviour
        assertEquals(expectedStartCount + 3, timer.getCount());
        assertTrue(timer.getElapsedTime().toNanos() > 0);
    }
}
