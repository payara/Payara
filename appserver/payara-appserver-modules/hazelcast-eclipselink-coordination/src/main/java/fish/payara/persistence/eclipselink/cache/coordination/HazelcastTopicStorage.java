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
package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a cache for {@link ITopic} instances ond a
 * possibility to delay {@link MessageListener} registration.
 */
@Service(name = "hazelcast-topic-storage")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastTopicStorage implements EventListener {

    private static HazelcastTopicStorage storage;

    private final Map<String, ITopic<HazelcastPayload>> topicCache = new ConcurrentHashMap<>();

    private final Map<MessageListener<HazelcastPayload>, TopicIdMapping> messageListener = new ConcurrentHashMap<>();

    @Inject
    private Events events;

    @PostConstruct
    public void postConstruct() {
        storage = this;
        events.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        storage = null;
        events.unregister(this);
    }

    public static HazelcastTopicStorage getInstance() {
        return storage;
    }

    @Override
    public void event(Event event) {
        if (isShutdown(event)) {
            destroy();
        } else if(isBootstrapComplete(event)) {
            registerUnregisteredListener();
        }
    }

    private static boolean isBootstrapComplete(Event event) {
        return event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE);
    }

    private static boolean isShutdown(Event event) {
        return event.is(EventTypes.SERVER_SHUTDOWN)
                || event.is(HazelcastEvents.HAZELCAST_SHUTDOWN_COMPLETE);
    }

    /**
     * Registers all yet unregistered {@link MessageListener}.
     */
    private void registerUnregisteredListener() {
        messageListener.entrySet().stream()
            .filter(entry -> !entry.getValue().hasExternalId())
            .forEach(entry -> {
                TopicIdMapping mapping = entry.getValue();
                ITopic<HazelcastPayload> topic = getTopic(mapping.getTopic());
                mapping.setExternalId(topic.addMessageListener(entry.getKey()));
            });
    }

    /**
     * Destroys this storage by destroying all topics, clearing the cache and all listeners.
     */
    private void destroy() {
        destroyTopics(topicCache.values());
        topicCache.clear();
        messageListener.clear();
    }

    /**
     * Destroys all provided {@link ITopic} instances.
     * @param topics The topics to destroy.
     */
    private void destroyTopics(Collection<ITopic<HazelcastPayload>> topics) {
        for (ITopic<HazelcastPayload> topic : topics) {
            try {
                topic.destroy();
            } catch (RuntimeException e) {
                Logger.getLogger(HazelcastTopicStorage.class.getName())
                    .log(
                        Level.WARNING,
                        MessageFormat.format("Failure destroying topic {0}.", topic),
                        e
                    );
            }
        }
    }

    String registerMessageListener(String topic, MessageListener<HazelcastPayload> listener) {
        TopicIdMapping topicIdMapping = new TopicIdMapping(topic);
        if(HazelcastCore.getCore().isEnabled()) {
            topicIdMapping.setExternalId(getTopic(topic).addMessageListener(listener));
        }
        messageListener.put(listener, topicIdMapping);
        return topicIdMapping.getInternalId();
    }

    void removeMessageListener(String topic, String messageListenerId) {
        Iterator<Map.Entry<MessageListener<HazelcastPayload>, TopicIdMapping>> iterator = messageListener.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MessageListener<HazelcastPayload>, TopicIdMapping> entry = iterator.next();
            if(Objects.equals(entry.getValue().getInternalId(), messageListenerId)) {
                if(entry.getValue().hasExternalId()
                        && !getTopic(topic).removeMessageListener(entry.getValue().getExternalId())) {
                    Logger.getLogger(HazelcastTopicStorage.class.getName())
                            .warning(
                                MessageFormat.format("Topic {0}: Removal of MessageListener {1} failed.", topic, messageListenerId)
                            );
                }
                iterator.remove();
            }
        }
    }

    void publish(String topic, HazelcastPayload payload) {
        getTopic(topic).publish(payload);
    }

    void destroyTopic(String name) {
        ITopic<HazelcastPayload> topic = topicCache.remove(name);
        if (topic != null) {
            destroyTopics(Collections.singleton(topic));
        }
    }

    /**
     * Returns the eventually cached topic.
     * @param name The topic name.
     * @return The {@link ITopic} for the name.
     */
    private ITopic<HazelcastPayload> getTopic(String name) {
        ITopic<HazelcastPayload> topic = topicCache.get(name);
        if(topic == null) {
            if (HazelcastCore.getCore().isEnabled()) {
                topic = HazelcastCore.getCore().getInstance().getTopic(name);
                topicCache.put(name, topic);
            } else {
                throw new IllegalStateException("Hazelcast integration not enabled.");
            }
        }
        return topic;
    }

    /**
     * @return The members UUID.
     */
    String getMemberUuid() {
        return HazelcastCore.getCore().getUUID();
    }

}
