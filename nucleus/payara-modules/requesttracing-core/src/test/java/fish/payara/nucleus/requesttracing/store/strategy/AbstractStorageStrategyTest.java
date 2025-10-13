/*
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.store.strategy;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.notification.requesttracing.RequestTraceSpan;

/**
 * Tests common to {@link LongestTraceStorageStrategy} and {@link ReservoirTraceStorageStrategy}.
 *  
 * @author Jan Bernitt
 */
public abstract class AbstractStorageStrategyTest {

    protected final TraceStorageStrategy strategy;

    public AbstractStorageStrategyTest(TraceStorageStrategy strategy) {
        this.strategy = strategy;
    }

    @Test
    public void emptyTracesReturnsNull() {
        assertNull(strategy.getTraceForRemoval(emptyList(), 20, null));
        assertNull(strategy.getTraceForRemoval(emptyList(), 20, createTrace()));
    }

    @Test
    public void noTracesAreRemovedBelowMaxSize() {
        int maxSize = 10;
        List<RequestTrace> traces = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            traces.add(createTrace());
            assertNull(strategy.getTraceForRemoval(traces, maxSize, null));
            for (int j = 0; j < traces.size(); j++) {
                RequestTrace traceToRemove = traces.get(j);
                assertNull(strategy.getTraceForRemoval(traces, maxSize, traceToRemove));
            }
            assertTrue(traces.size() <= maxSize);
        }
    }

    @Test
    public void traceIsRemovedAboveMaxSize() {
        int maxSize = 10;
        int loops = 30;
        List<RequestTrace> traces = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            traces.add(createTrace());
        }
        for (int i = maxSize; i < loops + maxSize; i++) {
            traces.add(createTrace());
            RequestTrace traceForRemoval = strategy.getTraceForRemoval(traces, maxSize, null);
            assertNotNull(traceForRemoval);
            for (int j = 0; j < traces.size(); j++) {
                RequestTrace traceToRemove = traces.get(j);
                assertSame(traceToRemove, strategy.getTraceForRemoval(traces, maxSize, traceToRemove));
            }
            traces.remove(traceForRemoval);
            assertTrue(traces.size() <= maxSize);
        }
    }

    protected static RequestTrace createTrace() {
        return createTrace(123000000L);
    }

    protected static RequestTrace createTrace(long durationNanos) {
        RequestTrace trace = new RequestTrace();
        RequestTraceSpan span = new RequestTraceSpan(EventType.TRACE_START, "op1");
        Instant start = Instant.now().minusNanos(durationNanos);
        span.setStartInstant(start);
        trace.addEvent(span);
        trace.endTrace();
        return trace;
    }
}
