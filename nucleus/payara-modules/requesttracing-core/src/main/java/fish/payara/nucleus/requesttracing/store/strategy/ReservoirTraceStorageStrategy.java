/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.nucleus.requesttracing.RequestTrace;
import java.util.Collection;
import java.util.Random;

/**
 * Strategy for finding traces that need removing from a list, according to a
 * reservoir sampling algorithm.
 * https://en.wikipedia.org/wiki/Reservoir_sampling.
 */
public class ReservoirTraceStorageStrategy implements TraceStorageStrategy {

    private final Random random;
    private long counter;

    public ReservoirTraceStorageStrategy() {
        this.random = new Random();
        this.counter = 0;
    }

    /**
     * Gets the trace that needs removing. Uses a reservoir sampling algorithm
     * to determine this.
     *
     * @param traces the list of traces to test.
     * @param maxSize the maximum size of the list.
     * @return the trace that needs removing, or null if no traces need
     * removing.
     */
    @Override
    public RequestTrace getTraceForRemoval(Collection<RequestTrace> traces, int maxSize) {
        if (counter < Long.MAX_VALUE) {
            counter++;
        }

        // Probability of keeping the new item
        double probability = (double) maxSize / counter;
        boolean keepItem = random.nextDouble() < probability;

        // If the probability fails or the list isn't full, nothing should be removed
        if (!keepItem || traces.size() <= maxSize) {
            return null;
        }

        // Pick a random item in the list
        int itemToReplace = random.nextInt(traces.size());
        return traces.toArray(new RequestTrace[]{})[itemToReplace];
    }

    /**
     * Gets the trace that needs removing. Removes the provided trace if present, or a random trace ala reservoir 
     * style trace storage. This method assumes that the reservoir style trace storage removal probability has already
     * been calculated and come out positive.
     *
     * @param traces the list of traces to test.
     * @param maxSize the maximum size of the list.
     * @param traceToRemove the trace to remove if present
     * @return the trace that needs removing, or null if no traces need
     * removing.
     */
    @Override
    public RequestTrace getTraceForRemoval(Collection<RequestTrace> traces, int maxSize, RequestTrace traceToRemove) {
        // If the list isn't full nothing should be removed
        if (traces.size() <= maxSize) {
            return null;
        }
        
        if (traces.contains(traceToRemove)) {
            return traceToRemove;
        } else {
            // Pick a random item in the list
            int itemToReplace = random.nextInt(traces.size());
            return traces.toArray(new RequestTrace[0])[itemToReplace];
        }
    }
}