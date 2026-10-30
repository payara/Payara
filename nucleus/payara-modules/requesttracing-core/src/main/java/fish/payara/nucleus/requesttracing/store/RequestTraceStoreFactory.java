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
import fish.payara.nucleus.requesttracing.store.strategy.LongestTraceStorageStrategy;
import fish.payara.nucleus.requesttracing.store.strategy.ReservoirTraceStorageStrategy;
import fish.payara.nucleus.requesttracing.store.strategy.TraceStorageStrategy;
import fish.payara.nucleus.store.ClusteredStore;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.internal.api.Globals;

import java.util.Map;
import java.util.UUID;

/**
 * A factory for generating a
 * {@link fish.payara.nucleus.requesttracing.store.RequestTraceStoreInterface}.
 */
public class RequestTraceStoreFactory {

    private static final String REQUEST_TRACE_STORE = "REQUEST_TRACE_STORE";
    private static final String HISTORIC_REQUEST_TRACE_STORE = "HISTORIC_REQUEST_TRACE_STORE";

    /**
     * Generates a request trace store.
     *
     * @param reservoirSamplingEnabled whether the store should remove items
     * based on a reservoir sampling algorithm.
     * @param historic whether the store is a historic store or not.
     * @return a request trace store.
     */
    public static RequestTraceStoreInterface getStore(boolean reservoirSamplingEnabled, boolean historic) {

        // Get the hazelcast store name for if it's a clustered store.
        String storeName;
        if (historic) {
            storeName = HISTORIC_REQUEST_TRACE_STORE;
        } else {
            storeName = REQUEST_TRACE_STORE;
        }

        // Determines a strategy for adding items to the store
        TraceStorageStrategy strategy;
        if (reservoirSamplingEnabled) {
            strategy = new ReservoirTraceStorageStrategy();
        } else {
            strategy = new LongestTraceStorageStrategy();
        }

        // Get a clustered store if possible
        ClusteredStore clusteredStore = null;
        ServiceHandle<ClusteredStore> serviceHandle = Globals.getDefaultHabitat().getServiceHandle(ClusteredStore.class);
        if (serviceHandle != null && serviceHandle.isActive()) {
            clusteredStore = serviceHandle.getService();
        }

        if (clusteredStore != null && clusteredStore.isEnabled()) {
            Map<UUID, RequestTrace> store = (Map) clusteredStore.getMap(storeName);
            return new ClusteredRequestTraceStore(store, strategy);
        }

        // Otherwise get a local store
        return new LocalRequestTraceStore(strategy);
    }

}
