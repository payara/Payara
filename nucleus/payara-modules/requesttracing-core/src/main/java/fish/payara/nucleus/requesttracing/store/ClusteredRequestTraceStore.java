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
package fish.payara.nucleus.requesttracing.store;

import com.hazelcast.core.MultiMap;
import fish.payara.nucleus.requesttracing.RequestTrace;
import fish.payara.nucleus.requesttracing.store.strategy.TraceStorageStrategy;
import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A store of {@link fish.payara.nucleus.requesttracing.RequestTrace} objects.
 * Stores the list across a cluster.
 */
public class ClusteredRequestTraceStore implements RequestTraceStoreInterface, Serializable {

    private final String instanceId;
    private final MultiMap<String, RequestTrace> store;
    private int maxStoreSize;
    
    private final TraceStorageStrategy strategy;

    ClusteredRequestTraceStore(MultiMap<String, RequestTrace> store, String instanceId, TraceStorageStrategy strategy) {
        this.store = store;
        this.instanceId = instanceId;
        this.maxStoreSize = store.size();
        this.strategy = strategy;
    }

    @Override
    public RequestTrace addTrace(RequestTrace trace) {
        store.put(instanceId, trace);
        RequestTrace traceToRemove = strategy.getTraceForRemoval(getTraces(), maxStoreSize);
        if (traceToRemove == null) {
            return null;
        }
        store.remove(traceToRemove);

        return traceToRemove;
    }
    
    @Override
    public RequestTrace addTrace(RequestTrace trace, RequestTrace traceToRemove) {
        store.put(instanceId, trace);
        traceToRemove = strategy.getTraceForRemoval(getTraces(), maxStoreSize, traceToRemove);
        if (traceToRemove == null) {
            return null;
        }
        
        Set<String> keys = store.localKeySet();
        for (String key : keys){
                if (store.remove(key, trace)){
                    break;
                }
        }
        
        return traceToRemove;
    }

    @Override
    public Collection<RequestTrace> getTraces() {
        return store.values();
    }

    @Override
    public Collection<RequestTrace> getTraces(int limit) {
        return store.values().stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public void setSize(int maxSize) {
        this.maxStoreSize = maxSize;
    }

    @Override
    public int getStoreSize() {
        return this.maxStoreSize;
    }

    @Override
    public Collection<RequestTrace> emptyStore() {
        Collection<RequestTrace> traces = new LinkedList<>();
        
        store.keySet().forEach((id) -> traces.addAll(store.remove(id)));
        
        return traces;
    }

}
