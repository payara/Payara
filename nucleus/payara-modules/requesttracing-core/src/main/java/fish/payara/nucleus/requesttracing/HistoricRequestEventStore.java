/*
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.requesttracing;

import com.google.common.collect.Sets;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.requesttracing.domain.HistoricRequestEvent;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Stores historic request traces with descending elapsed time. Comparator is implemented on {@link HistoricRequestEvent}
 *
 * @author mertcaliskan
 */
@Service
@Singleton
public class HistoricRequestEventStore {

    private static final String HISTORIC_REQUEST_EVENT_STORE = "HISTORIC_REQUEST_EVENT_STORE";

    @Inject
    private HazelcastCore hzCore;

    @Inject
    private ServerEnvironment serverEnv;

    private HazelcastInstance instance;

    private SortedSet<HistoricRequestEvent> historicStore;

    void initialize(int storeSize) {
        historicStore = Collections.synchronizedSortedSet(new BoundedTreeSet<HistoricRequestEvent>(storeSize));

        if (hzCore.isEnabled()) {
            instance = hzCore.getInstance();
            String instanceName = serverEnv.getInstanceName();
            IMap<String, SortedSet<HistoricRequestEvent>> map
                    = instance.getMap(HISTORIC_REQUEST_EVENT_STORE);
            SortedSet<HistoricRequestEvent> instanceHistoricStore = map.get(instanceName);
            if (instanceHistoricStore == null) {
                map.put(instanceName, historicStore);
            }
            else {
                historicStore = instanceHistoricStore;
            }
        }
    }

    void addTrace(long elapsedTime, String message) {
        historicStore.add(new HistoricRequestEvent(elapsedTime, message));
    }

    public HistoricRequestEvent[] getTraces() {
        HistoricRequestEvent[] emptyArray = new HistoricRequestEvent[0];
        if (historicStore != null) {
            return (HistoricRequestEvent[]) historicStore.toArray(emptyArray);
        }
        return emptyArray;
    }

    public HistoricRequestEvent[] getTraces(Integer limit) {
        HistoricRequestEvent[] result;
        HistoricRequestEvent[] historicRequestEvents = historicStore.toArray(new HistoricRequestEvent[0]);
        if (limit < historicRequestEvents.length) {
            result = new HistoricRequestEvent[limit];
            System.arraycopy(historicRequestEvents, 0, result, 0, limit);
        }
        else {
            result = historicRequestEvents;
        }
        return result;
    }
}

class BoundedTreeSet<N extends Comparable> extends TreeSet<N> {

    private final int maxSize;

    BoundedTreeSet(int maxSize) {
        super();
        this.maxSize = maxSize;
    }

    public boolean add(N n) {
        super.add(n);

        if(size() > maxSize) {
            remove(last());
        }
        return true;
    }
}