/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.healthcheck.preliminary;

import fish.payara.notification.healthcheck.HealthCheckResultEntry;
import fish.payara.notification.healthcheck.HealthCheckResultStatus;
import fish.payara.nucleus.healthcheck.HealthCheckResult;
import org.junit.Before;
import org.junit.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import fish.payara.nucleus.healthcheck.HealthCheckWithThresholdExecutionOptions;

import static fish.payara.nucleus.healthcheck.HealthCheckConstants.CONCURRENT_G1GC;
import static fish.payara.nucleus.healthcheck.HealthCheckConstants.OLD_G1GC;
import static fish.payara.nucleus.healthcheck.HealthCheckConstants.YOUNG_G1GC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link GarbageCollectorHealthCheck}.
 * Verifies recognition of young, old, and concurrent GC MXBeans (e.g. on Java 21+).
 */
public class GarbageCollectorHealthCheckTest {

    private GarbageCollectorHealthCheck healthCheck;

    @Before
    public void setUp() {
        healthCheck = new GarbageCollectorHealthCheck();
        healthCheck.options = new HealthCheckWithThresholdExecutionOptions(
                true, 5L, TimeUnit.MINUTES, false, "80", "50", "0");
    }

    @Test
    public void testIsYoungGenerationGC() {
        GarbageCollectorMXBean bean = mockGcBean(YOUNG_G1GC, 10, 500);
        assertTrue(GarbageCollectorHealthCheck.isYoungGenerationGC(bean));
        assertFalse(GarbageCollectorHealthCheck.isOldGenerationGC(bean));
        assertFalse(GarbageCollectorHealthCheck.isConcurrentGC(bean));
    }

    @Test
    public void testIsOldGenerationGC() {
        GarbageCollectorMXBean bean = mockGcBean(OLD_G1GC, 2, 200);
        assertFalse(GarbageCollectorHealthCheck.isYoungGenerationGC(bean));
        assertTrue(GarbageCollectorHealthCheck.isOldGenerationGC(bean));
        assertFalse(GarbageCollectorHealthCheck.isConcurrentGC(bean));
    }

    @Test
    public void testIsConcurrentGC() {
        GarbageCollectorMXBean bean = mockGcBean(CONCURRENT_G1GC, 5, 150);
        assertFalse(GarbageCollectorHealthCheck.isYoungGenerationGC(bean));
        assertFalse(GarbageCollectorHealthCheck.isOldGenerationGC(bean));
        assertTrue(GarbageCollectorHealthCheck.isConcurrentGC(bean));
    }

    @Test
    public void testJava21G1GcBeansRecognizedWithoutError() {
        GarbageCollectorMXBean youngBean = mockGcBean(YOUNG_G1GC, 10, 500);
        GarbageCollectorMXBean oldBean = mockGcBean(OLD_G1GC, 2, 200);
        GarbageCollectorMXBean concurrentBean = mockGcBean(CONCURRENT_G1GC, 4, 100);

        List<GarbageCollectorMXBean> beans = Arrays.asList(youngBean, oldBean, concurrentBean);
        HealthCheckResult result = healthCheck.doCheck(beans);

        assertEquals(3, result.getEntries().size());

        for (HealthCheckResultEntry entry : result.getEntries()) {
            assertNotEquals("Should not produce CHECK_ERROR for recognized G1 GC beans: " + entry.getMessage(),
                    HealthCheckResultStatus.CHECK_ERROR, entry.getStatus());
        }

        // Check entry messages
        boolean foundConcurrent = false;
        for (HealthCheckResultEntry entry : result.getEntries()) {
            if (entry.getMessage().contains(CONCURRENT_G1GC)) {
                foundConcurrent = true;
                assertTrue(entry.getMessage().contains("Concurrent GC"));
            }
        }
        assertTrue("Concurrent GC entry should be present in results", foundConcurrent);
    }

    @Test
    public void testUnknownGcBeanProducesCheckError() {
        GarbageCollectorMXBean unknownBean = mockGcBean("UnknownCollector", 1, 50);
        HealthCheckResult result = healthCheck.doCheck(Collections.singletonList(unknownBean));

        assertEquals(1, result.getEntries().size());
        HealthCheckResultEntry entry = result.getEntries().get(0);
        assertEquals(HealthCheckResultStatus.CHECK_ERROR, entry.getStatus());
        assertTrue(entry.getMessage().contains("Could not identify GarbageCollectorMXBean with name: UnknownCollector"));
    }

    private static GarbageCollectorMXBean mockGcBean(String name, long count, long time) {
        GarbageCollectorMXBean bean = mock(GarbageCollectorMXBean.class);
        when(bean.getName()).thenReturn(name);
        when(bean.getCollectionCount()).thenReturn(count);
        when(bean.getCollectionTime()).thenReturn(time);
        return bean;
    }
}
