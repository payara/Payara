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
package fish.payara.nucleus.healthcheck;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import fish.payara.nucleus.notification.domain.BoundedTreeSet;
import org.glassfish.api.admin.ServerEnvironment;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.SortedSet;
import java.util.logging.Level;

/**
 * Stores historic health check events with descending occurring date. Comparator is implemented on {@link HistoricHealthCheckEvent}
 *
 * @author mertcaliskan
 */
@Service
@Singleton
public class HistoricHealthCheckEventStore {

    private static final String HISTORIC_HEALTHCHECK_EVENT_STORE = "HISTORIC_HEALTHCHECK_EVENT_STORE";

    @Inject
    private HazelcastCore hzCore;

    @Inject
    private ServerEnvironment serverEnv;

    private HazelcastInstance instance;

    private SortedSet<HistoricHealthCheckEvent> historicStore;

    void initialize(int storeSize) {
        historicStore = Collections.synchronizedSortedSet(new BoundedTreeSet<HistoricHealthCheckEvent>(storeSize));

        if (hzCore.isEnabled()) {
            instance = hzCore.getInstance();
            String instanceName = serverEnv.getInstanceName();
            IMap<String, SortedSet<HistoricHealthCheckEvent>> map
                    = instance.getMap(HISTORIC_HEALTHCHECK_EVENT_STORE);
            if (map != null) {
                SortedSet<HistoricHealthCheckEvent> instanceHistoricStore = map.get(instanceName);
                if (instanceHistoricStore == null) {
                    map.put(instanceName, historicStore);
                }
                else {
                    historicStore = instanceHistoricStore;
                }
            }
        }
    }

    public void addTrace(long occurringTime, Level level, String userMessage, String message, Object[] parameters) {
        historicStore.add(new HistoricHealthCheckEvent(occurringTime, level, userMessage, message, parameters));
    }

    public HistoricHealthCheckEvent[] getTraces() {
        HistoricHealthCheckEvent[] emptyArray = new HistoricHealthCheckEvent[0];
        if (historicStore != null) {
            return historicStore.toArray(emptyArray);
        }
        return emptyArray;
    }

    public HistoricHealthCheckEvent[] getTraces(Integer limit) {
        HistoricHealthCheckEvent[] result;
        HistoricHealthCheckEvent[] historicEvents = historicStore.toArray(new HistoricHealthCheckEvent[historicStore.size()]);
        if (limit < historicEvents.length) {
            result = new HistoricHealthCheckEvent[limit];
            System.arraycopy(historicEvents, 0, result, 0, limit);
        }
        else {
            result = historicEvents;
        }
        return result;
    }
}

