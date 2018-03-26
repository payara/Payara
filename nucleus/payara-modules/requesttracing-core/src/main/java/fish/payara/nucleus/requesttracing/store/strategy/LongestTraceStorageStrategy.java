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

import fish.payara.notification.requesttracing.RequestTrace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Strategy for finding traces that need removing from a list. Will remove the
 * trace at the end of the
 * {@link RequestTrace RequestTrace} object's
 * natural ordering.
 */
public class LongestTraceStorageStrategy implements TraceStorageStrategy {

    /**
     * Gets the trace that needs removing. Sorts by the request trace's natural
     * ordering.
     *
     * @param traces the list of traces to test.
     * @param maxSize the maximum size of the list.
     * @return the trace that needs removing, or null if no traces need
     * removing.
     */
    @Override
    public RequestTrace getTraceForRemoval(Collection<RequestTrace> traces, int maxSize) {
        if (traces == null || traces.isEmpty() || traces.size() <= maxSize) {
            return null;
        }
        // Sort and return shortest request
        List<RequestTrace> tracesCopy = new ArrayList<>(traces);
        Collections.sort(tracesCopy);
        return traces.toArray(new RequestTrace[]{})[traces.size() - 1];
    }

    /**
     * Gets the trace that needs removing. Removes the provided trace if present, or sorts by the request traces' 
     * natural ordering if not.
     *
     * @param traces the list of traces to test.
     * @param maxSize the maximum size of the list.
     * @param traceToRemove the trace to remove if present
     * @return the trace that needs removing, or null if no traces need
     * removing.
     */
    @Override
    public RequestTrace getTraceForRemoval(Collection<RequestTrace> traces, int maxSize, RequestTrace traceToRemove) {
        if (traces == null || traces.isEmpty() || traces.size() <= maxSize) {
            return null;
        }
        
        if (traces.contains(traceToRemove)) {
            return traceToRemove;
        } else {
            // Sort and return shortest request
            List<RequestTrace> tracesCopy = new ArrayList<>(traces);
            Collections.sort(tracesCopy);
            return traces.toArray(new RequestTrace[]{})[traces.size() - 1];
        }
    }
}
