/*
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.nucleus.store;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.MultiMap;
import com.hazelcast.map.impl.MapService;
import com.hazelcast.monitor.LocalMapStats;
import com.hazelcast.topic.impl.TopicService;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * Very Simple Store interface to Hazelcast
 * @author steve
 */
@Service(name = "payara-cluster-store")
@RunLevel(StartupRunLevel.VAL)
public class ClusteredStore implements EventListener, MonitoringDataSource {
    private static final Logger logger = Logger.getLogger(ClusteredStore.class.getCanonicalName());
    
    @Inject
    private HazelcastCore hzCore;
    
    @Inject
    private Events events;
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }

    @Override
    public void collect(MonitoringDataCollector collector) {
        if (hzCore.isEnabled()) {
            HazelcastInstance hz = hzCore.getInstance();
            for (DistributedObject obj : hz.getDistributedObjects()) {
                if (MapService.SERVICE_NAME.equals(obj.getServiceName())) {
                    IMap<Object, Object> map = hz.getMap(obj.getName());
                    if (map != null) {
                        collector.in("store").type("map").entity(map.getName())
                            .collect("size", map.size())
                            .collectObject(map.getLocalMapStats(), LocalMapStats.class);
                    }
                }
            }
        }
    }

    public String getInstanceId() {
        return hzCore.getUUID();
    }
    
    /**
     * Returns true if Hazelcast is enabled
     * @return 
     */
    public boolean isEnabled() {
        return hzCore.isEnabled();
    }
    
    /**
     * Stores a value in Hazelcast
     * @param storeName The name of the store to put the value into.
     * This will be created if it does not already exist.
     * @param key
     * @param value
     * @return true if the operation succeeded, false otherwise
     */
    public boolean set(String storeName, Serializable key, Serializable value) {
        boolean result = false;
        if (isEnabled()) {
            hzCore.getInstance().getMap(storeName).set(key, value);
            result = true;
        }
        return result;
    }
    
    /**
     * Removes a key/value pair of a Hazelcast store.
     * The store will be created if it does not already exist.
     * @param storeName The name of the store to remove from
     * @param key The key to remove
     * @return true if the operation succeeded, false otherwise
     */
    public boolean remove(String storeName, Serializable key) {
        boolean result = false;
        if (isEnabled()) {
            IMap map = hzCore.getInstance().getMap(storeName);
            if (map != null) {
                Object value = map.remove(key);
                result = true;
            }
        }
        return result;
    }
    
    /**
     * Checks to see if a a key already exists in Hazelcast.
     * The store will be created if it does not already exist.
     * @param storeName
     * @param key
     * @return 
     */
    public boolean containsKey(String storeName, Serializable key) {
         boolean result = false;
        if (isEnabled()) {
            IMap map = hzCore.getInstance().getMap(storeName);
            if (map != null) {
                result = map.containsKey(key);
            }
        }
        return result;       
    }
    
    /**
     * Gets the value from Hazelcast with the specified key in the given store.
     * The store will be created if it does not already exist.
     * @param storeName
     * @param key
     * @return 
     */
    public Serializable get(String storeName, Serializable key) {
        Serializable result = null;
        if (isEnabled()) {
            IMap map = hzCore.getInstance().getMap(storeName);
            if (map != null) {
                result = (Serializable) map.get(key);
            }
        }
        return result;
    }

    @Override
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)){
            if (hzCore.isEnabled()) {
                logger.config("Payara Clustered Store Service Enabled");
            }
        }
    }

    /**
     * Gets all the key/value pairs in a given Hazelcast store.
     * <p> Part of MicroProfile Config
     * @param storeName The store name to lookup
     * @return
     * @since 4.1.2.173
     */
    public Map<Serializable, Serializable> getMap(String storeName) {
        HashMap<Serializable,Serializable> result = new HashMap<>();
        if (hzCore.isEnabled()) {
            IMap map = hzCore.getInstance().getMap(storeName);
            if (map != null) {
                Set<Serializable> keys = map.keySet();
                for (Serializable key : keys) {
                    result.put(key, (Serializable) map.get(key));
                }
            }
        }
        return result;
    }
    
    /**
     * Gets the requested MultiMap from Hazelcast. Use this if you want to store a collection in Hazelcast.
     * @param storeName The MultiMap to get
     * @return The MultiMap matching the storeName parameter, or null if the MultiMap does not exist
     */
    public MultiMap<Serializable, Serializable> getMultiMap(String storeName) {
        MultiMap<Serializable, Serializable> multiMap = null;
        if (hzCore.isEnabled()) {
            multiMap = hzCore.getInstance().getMultiMap(storeName);
        } 
        
        
        
        return multiMap;
    }
}
