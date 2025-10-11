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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fish.payara.notification.requesttracing.RequestTrace;

/**
 * Tests the correct behaviour of the {@link LongestTraceStorageStrategy}.
 *  
 * @author Jan Bernitt
 */
public class LongestTraceStorageStrategyTest extends AbstractStorageStrategyTest {

    public LongestTraceStorageStrategyTest() {
        super(new LongestTraceStorageStrategy());
    }

    @Test
    public void shortestTraceIsRemovedAboveMaxSize() {
        int maxSize = 10;
        List<RequestTrace> traces = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            traces.add(createTrace(1000000000L * (i+1))); // 1-10sec long traces
        }
        List<RequestTrace> added = new ArrayList<>();
        List<RequestTrace> removed = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            RequestTrace newTrace = createTrace(1000000000L * (i+1) - 500000000L); // 0.5-9.5ms long traces
            added.add(newTrace);
            traces.add(newTrace);
            RequestTrace traceForRemoval = strategy.getTraceForRemoval(traces, maxSize, null);
            assertTrue(traceForRemoval.getElapsedTime() <= newTrace.getElapsedTime());
            for (int j = 0; j < maxSize; j++) {
                RequestTrace candidate = traces.get(j);
                assertSame(candidate, strategy.getTraceForRemoval(traces, maxSize, candidate));
            }
            removed.add(traceForRemoval);
            traces.remove(traceForRemoval);
        }
        // remaining elements should be half from the original 10 items, half from the later added ones
        added.removeAll(removed);
        assertEquals(maxSize/2, added.size());
    }
}
