/*
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing.store;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.notification.requesttracing.RequestTraceSpan;

/**
 * Common tests for {@link LocalRequestTraceStore} and {@link ClusteredRequestTraceStore}.
 *  
 * @author Jan Bernitt
 */
public abstract class AbstractRequestTraceStoreTest {

    protected final RequestTraceStoreInterface store;

    public AbstractRequestTraceStoreTest(RequestTraceStoreInterface store) {
        this.store = store;
    }

    @Test
    public void addingTracesUpToTheMaxSizeDoesNotRemoveAnyTraces() {
        store.setSize(() -> 10);
        assertEquals(10, store.getStoreSize());
        addRandomTraces(10);
        assertEquals(10, store.getTraces().size());
    }

    @Test
    public void addingTraceExceedingMaxSizeDoesRemoveTraces() {
        store.setSize(() -> 10);
        assertEquals(10, store.getStoreSize());
        addRandomTraces(10);
        for (int i = 0; i < 20; i++) {
            store.addTrace(createTrace());
            assertEquals(10, store.getTraces().size());
        }
    }

    @Test
    public void reducingTheMaxSizeRemovesExceedingTraces() {
        store.setSize(() -> 10);
        assertEquals(10, store.getStoreSize());
        addRandomTraces(10);
        assertEquals(10, store.getTraces().size());
        store.setSize(() -> 5);
        assertEquals(5, store.getStoreSize());
        assertEquals(5, store.getTraces().size());
    }

    @Test
    public void increasingTheMaxSizeDoesNotRemoveTraces() {
        store.setSize(() -> 10);
        assertEquals(10, store.getStoreSize());
        addRandomTraces(10);
        store.setSize(() -> 15);
        assertEquals(15, store.getStoreSize());
        assertEquals(10, store.getTraces().size());
    }

    @Test
    public void emptyingTheStoreReturnsAllContainedTraces() {
        store.setSize(() -> 10);
        assertEquals(10, store.getStoreSize());
        addRandomTraces(6);
        assertEquals(6, store.emptyStore().size());
        assertEquals(0, store.getTraces().size());
    }

    private void addRandomTraces(int n) {
        for (int i = 0; i < n; i++) {
            store.addTrace(createTrace());
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
