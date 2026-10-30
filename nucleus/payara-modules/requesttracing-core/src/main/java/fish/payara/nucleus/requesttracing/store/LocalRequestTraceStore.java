/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.store.strategy.TraceStorageStrategy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

/**
 * A store of {@link RequestTrace} objects.
 * Stores the list locally.
 */
public class LocalRequestTraceStore implements RequestTraceStoreInterface {

    private final Set<RequestTrace> store = ConcurrentHashMap.newKeySet();
    private IntSupplier maxStoreSize;

    private final TraceStorageStrategy strategy;

    LocalRequestTraceStore(TraceStorageStrategy strategy) {
        this.maxStoreSize = () -> 0;
        this.strategy = strategy;
    }

    @Override
    public RequestTrace addTrace(RequestTrace trace) {
        return addTrace(trace, null);
    }

    @Override
    public RequestTrace addTrace(RequestTrace trace, RequestTrace traceToRemove) {
        store.add(trace);
        traceToRemove = strategy.getTraceForRemoval(getTraces(), maxStoreSize.getAsInt(), traceToRemove);
        if (traceToRemove == null) {
            return null;
        }
        store.remove(traceToRemove);

        return traceToRemove;
    }

    @Override
    public Collection<RequestTrace> getTraces() {
        return store;
    }

    @Override
    public Collection<RequestTrace> getTraces(int limit) {
        return store.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public void setSize(IntSupplier maxSize) {
        int currentMaxSize = maxSize.getAsInt();
        while (store.size() > currentMaxSize) {
            store.remove(strategy.getTraceForRemoval(getTraces(), currentMaxSize, null));
        }
        this.maxStoreSize = maxSize;
    }

    @Override
    public int getStoreSize() {
        return maxStoreSize.getAsInt();
    }

    @Override
    public Collection<RequestTrace> emptyStore() {
        Collection<RequestTrace> traces = new ArrayList<>(store);
        store.clear();
        return traces;
    }

}
