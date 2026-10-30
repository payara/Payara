/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2020] Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.notification.requesttracing.RequestTrace;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

/**
 * Strategy for finding traces that need removing from a list, according to a
 * reservoir sampling algorithm.
 * https://en.wikipedia.org/wiki/Reservoir_sampling.
 */
public class ReservoirTraceStorageStrategy implements TraceStorageStrategy {

    private final Random random;

    public ReservoirTraceStorageStrategy() {
        this.random = new Random();
    }

    protected ReservoirTraceStorageStrategy(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Gets the trace that needs removing. Removes the provided trace if present, or a random trace each trace having
     * the same probability of being kept or removed.
     *
     * @param traces        the list of traces to test.
     * @param maxSize       the maximum size of the list.
     * @param traceToRemove the trace to remove if present
     * @return the trace that needs removing, or null if no traces need removing.
     */
    @Override
    public RequestTrace getTraceForRemoval(Collection<RequestTrace> traces, int maxSize, RequestTrace traceToRemove) {
        // If the list isn't full nothing should be removed
        if (traces.size() <= maxSize) {
            return null;
        }
        return traceToRemove != null && traces.contains(traceToRemove) 
                ? traceToRemove 
                : findRandomTrace(traces);
    }

    /**
     * Since this implementation is only dealing with a special case of the Reservoir sampling scenario where there is a
     * single element more than the maximum size a selection of equal probability is simply to select any of the
     * elements at random whereby we are left with the elements to keep.
     */
    private RequestTrace findRandomTrace(Collection<RequestTrace> traces) {
        int itemToReplace = random.nextInt(traces.size());
        Iterator<RequestTrace> iter = traces.iterator();
        for (int i = 0; i < itemToReplace; i++) {
            iter.next();
        }
        return iter.next();
    }
}