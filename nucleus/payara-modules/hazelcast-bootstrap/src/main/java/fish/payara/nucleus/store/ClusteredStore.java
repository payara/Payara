/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.store;

import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
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
public class ClusteredStore implements EventListener {
    private static final Logger logger = Logger.getLogger(ClusteredStore.class.getCanonicalName());
    
    @Inject
    private HazelcastCore hzCore;
    
    @Inject
    private Events events;
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
    }
    
    public boolean isEnabled() {
        return hzCore.isEnabled();
    }
    
    public boolean set(String storeName, Serializable key, Serializable value) {
        boolean result = false;
        if (isEnabled()) {
            hzCore.getInstance().getMap(storeName).set(key, value);
            result = true;
        }
        return result;
    }
    
    public boolean remove(String storeName, Serializable key) {
        boolean result = false;
        if (isEnabled()) {
            Object value = hzCore.getInstance().getMap(storeName).remove(key);
            result = true;
        }
        return result;
    }
    
    public boolean containsKey(String storeName, Serializable key) {
         boolean result = false;
        if (isEnabled()) {
            result = hzCore.getInstance().getMap(storeName).containsKey(key);
        }
        return result;       
    }
    
    public Serializable get(String storeName, Serializable key) {
        Serializable result = null;
        if (isEnabled()) {
            result = (Serializable) hzCore.getInstance().getMap(storeName).get(key);
        }
        return result;
    }

    @Override
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)){
            if (hzCore.isEnabled()) {
                logger.info("Payara Clustered Store Service Enabled");
            }
        }
    }
    
}
