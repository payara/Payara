/*
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import fish.payara.notification.requesttracing.RequestTrace;

/**
 * Tests the correct behaviour of the {@link ReservoirTraceStorageStrategy}.
 *  
 * @author Jan Bernitt
 */
public class ReservoirTraceStorageStrategyTest extends AbstractStorageStrategyTest {

    public ReservoirTraceStorageStrategyTest() {
        super(new ReservoirTraceStorageStrategy());
    }

    @Test
    public void anyTraceHasSameProbabilityToBeRemovedAboveMaxSize() {
        int maxSize = 9;
        List<RequestTrace> traces = new ArrayList<>();
        for (int i = 0; i < maxSize; i++) {
            traces.add(createTrace());
        }
        Map<UUID, Integer> removedCounts = new HashMap<>();
        List<RequestTrace> added = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            RequestTrace newTrace = createTrace();
            added.add(newTrace);
            traces.add(newTrace);
            for (int j = 0; j < 1000; j++) {
                RequestTrace traceForRemoval = strategy.getTraceForRemoval(traces, maxSize, null);
                removedCounts.compute(traceForRemoval.getTraceId(), (key, value) -> value == null ? 0 : value + 1);
            }
            traces.remove(newTrace); // we remove always the new one to have easier time verifying the numbers
        }
        // now we did 10x100 evaluations, 
        // in an even distribution each of the 9 traces kept in the list should be removed about 1000 times (1/10 * 1000 * 10 runs)
        for (RequestTrace trace : traces) {
            int count = removedCounts.get(trace.getTraceId());
            int diff = Math.abs(1000 - count);
            assertTrue("Expected a vairation of below 100 but was "+diff, diff < 150);
        }
        // in an even distribution each of the 10 added traces should be removed about 100 times (1/10 * 1000 runs)
        for (RequestTrace trace : added) {
            int count = removedCounts.get(trace.getTraceId());
            int diff = Math.abs(100 - count);
            assertTrue("Expected a vairation of below 30 but was "+diff, diff < 30);
        }
    }
}
