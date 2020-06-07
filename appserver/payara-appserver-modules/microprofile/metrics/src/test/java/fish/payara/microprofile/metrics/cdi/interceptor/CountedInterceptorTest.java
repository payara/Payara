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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.junit.Test;

import fish.payara.microprofile.metrics.cdi.AnnotationReader;
import fish.payara.microprofile.metrics.cdi.MetricUtils;
import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;
import fish.payara.microprofile.metrics.test.TestUtils;

/**
 * Tests the {@link CountedInterceptor} business logic
 *
 * @author Jan Bernitt
 * @since 5.202
 */
public class CountedInterceptorTest {

    private final InvocationContext context = mock(InvocationContext.class);
    private final MetricRegistryImpl registry = new MetricRegistryImpl();

    @Test
    @Counted
    public void counterNoAttributes() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(absolute = true)
    public void counterAbsolute() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(reusable = true)
    public void counterReusable() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(name = "name")
    public void counterWithName() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(displayName = "displayName")
    public void counterWithDisplayName() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(description = "description")
    public void counterWithDescription() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(unit = "unit")
    public void counterWithUnit() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(tags = "a=b")
    public void counterWithTag() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(tags = {"a=b", "b=c"})
    public void counterWithTags() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(name= "name", tags = {"a=b", "b=c"})
    public void counterWithNameAndTags() throws Exception {
        assertCounterIncrements();
    }

    @Test
    @Counted(absolute = true, name= "name", tags = {"a=b", "b=c"}, reusable = true)
    public void counterWithAbsoluteNameAndTagsReusable() throws Exception {
        assertCounterIncrements(0);
        assertCounterIncrements(2); // this tries to register the counter again, but it gets reused so we start at 2
    }

    private void assertCounterIncrements() throws Exception {
        assertCounterIncrements(0);
    }

    private void assertCounterIncrements(int expectedStartCount) throws Exception {
        Method element = TestUtils.getTestMethod();
        Class<?> bean = getClass();
        AnnotationReader<Counted> reader = AnnotationReader.COUNTED;
        Counter counter = MetricUtils.getOrRegisterByMetadataAndTags(registry, Counter.class,
                reader.metadata(bean, element), reader.tags(reader.annotation(bean, element)));
        CountedInterceptor.proceedCounted(context, element, bean, registry::getMetric);
        assertEquals(expectedStartCount + 1, counter.getCount());
        CountedInterceptor.proceedCounted(context, element, bean, registry::getMetric);
        assertEquals(expectedStartCount + 2, counter.getCount());
        verify(context, times(expectedStartCount + 2)).proceed();
        // now test error when it does not exist
        try {
            CountedInterceptor.proceedCounted(context, element, bean, (metricId, type) -> null);
            fail("Expected a IllegalStateException because the metric does not exist");
        } catch (IllegalStateException ex) {
            assertEquals("No Counter with ID [" + reader.metricID(bean, element)
                    + "] found in application registry", ex.getMessage());
        }
        verify(context, times(expectedStartCount + 2)).proceed();
    }
}
