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
package fish.payara.nucleus.eventbus;

import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

/**
 * A Hazelcast based Event Bus for Payara
 * @author steve
 */
@Service(name = "payara-event-bus")
@RunLevel(StartupRunLevel.VAL)
public class EventBus implements EventListener {
    
    private static final Logger logger = Logger.getLogger(EventBus.class.getCanonicalName());
    
    @Inject
    private HazelcastCore hzCore;
    
    @Inject
    private Events events;
    
    private HashMap<String,TopicListener> messageReceivers;
    
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
        messageReceivers = new HashMap<String, TopicListener>(2);
    }
    
    public boolean publish(String topic, ClusterMessage message) {
        boolean result = false;
        if (hzCore.isEnabled()) {
            hzCore.getInstance().getTopic(topic).publish(message);
            result = true;
        }
        return result;
    }
    
    public boolean addMessageReceiver(String topic, MessageReceiver mr) {
        boolean result = false;
        if (hzCore.isEnabled()) {
            TopicListener tl = messageReceivers.get(topic);
            if (tl == null) {
                // create a topic listener on the specified topic
                TopicListener newTL = new TopicListener(topic);
                String regId = hzCore.getInstance().getTopic(topic).addMessageListener(newTL);
                messageReceivers.put(topic, newTL);
                tl = newTL;
                tl.setRegistrationID(regId);
            }
            
            tl.addMessageReceiver(mr);
            result = true;
        }
        return result;
    }
    
    public void removeMessageReceiver(String topic, MessageReceiver mr) {
        TopicListener tl = messageReceivers.get(topic);
        if (tl != null) {
            int remaining = tl.removeMessageReceiver(mr);
            
            // for efficiency if we have no message receivers remove our registration
            if (remaining == 0) {
                messageReceivers.remove(topic);
                if (hzCore.isEnabled()) {
                    hzCore.getInstance().getTopic(topic).removeMessageListener(tl.getRegistrationID());
                }
            }
        }
    }

    @Override
    public void event(Event event) {
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
            if (hzCore.isEnabled()) {
                logger.info("Payara Clustered Event Bus Enabled");
                // add message receivers if any as this maybe a
                for (String topic : messageReceivers.keySet()) {
                    TopicListener tl = messageReceivers.get(topic);
                    String regId = hzCore.getInstance().getTopic(topic).addMessageListener(tl);
                    tl.setRegistrationID(regId);                   
                }
            }
        }
    }
    
}
