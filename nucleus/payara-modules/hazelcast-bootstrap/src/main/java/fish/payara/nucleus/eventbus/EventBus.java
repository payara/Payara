/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
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
package fish.payara.nucleus.eventbus;

import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.monitor.LocalTopicStats;
import com.hazelcast.topic.impl.TopicService;

/**
 * A Hazelcast based Event Bus for Payara
 * @author steve
 * @since 4.1.153
 */
@Service(name = "payara-event-bus")
@RunLevel(StartupRunLevel.VAL)
public class EventBus implements EventListener, MonitoringDataSource {
    
    private static final Logger logger = Logger.getLogger(EventBus.class.getCanonicalName());
    
    @Inject
    private HazelcastCore hzCore;
    
    @Inject
    private Events events;
    
    private Map<String, TopicListener> messageReceivers;
    
    
    @PostConstruct
    public void postConstruct() {
        events.register(this);
        messageReceivers = new ConcurrentHashMap<>(2);
    }

    @Override
    public void collect(MonitoringDataCollector rootCollector) {
        MonitoringDataCollector eventCollector = rootCollector.in("event");
        eventCollector
            .type("topic").collectAll(messageReceivers, EventBus::collectTopicListener);
        if (hzCore.isEnabled()) {
            HazelcastInstance hz = hzCore.getInstance();
            for (DistributedObject obj : hz.getDistributedObjects()) {
                if (TopicService.SERVICE_NAME.equals(obj.getServiceName())) {
                    eventCollector.type("topic").entity(obj.getName())
                        .collectObject(hz.getTopic(obj.getName()).getLocalTopicStats(), LocalTopicStats.class);
                }
            }
        }
    }

    private static void collectTopicListener(MonitoringDataCollector collector, TopicListener listener) {
        collector
            .collect("receiverCount", listener.getReceiverCount());
    }

    /**
     * Sends out a message to all listeners in the Hazelcast sluster that are
     * listening to the topic
     * @param topic
     * @param message
     * @return 
     */
    public boolean publish(String topic, ClusterMessage message) {
        boolean result = false;
        if (hzCore.isEnabled()) {
            hzCore.getInstance().getTopic(topic).publish(message);
            result = true;
        }
        return result;
    }
    
    /**
     * Adds a message receiver to listen to message send on the Hazelcast EventBus
     * @param topic The name of the topic to recive messages on
     * @param mr A {@link MessageReciever} to listen for messages
     * @return true if successfully registered, false otherwise (i.e. if Hazelcast
     * is not enabled)
     */
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
    
    /**
     * Stops a message receiver from listening to messages on the specified topic
     * @param topic The name of the topic that messages have been received on
     * @param mr The {@link MessageReciever} to stop listening for messages
     */
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
        if (event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE) && hzCore.isEnabled()) {
            logger.config("Payara Clustered Event Bus Enabled");
        }
    }
    
}
