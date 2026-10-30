/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.persistence.eclipselink.cache.coordination;

import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.EventBus;
import fish.payara.nucleus.eventbus.MessageReceiver;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.executorservice.PayaraExecutorService;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * Represents a possibility to delay {@link MessageReceiver} registration.
 *
 * @author Sven Diedrichsen
 */
@Service(name = "hazelcast-topic-storage")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastTopicStorage implements EventListener {

    /**
     * The singleton instance of the storage.
     */
    private static HazelcastTopicStorage storage;
    /**
     * The message listener cache.
     */
    private final Map<String, ReceiverMapping> messageReceiver = new ConcurrentHashMap<>();
    /**
     * Event bus to propagate cache coordination messages over.
     */
    @Inject
    private EventBus eventBus;
    /**
     * Executor to process incoming messages.
     */
    @Inject
    private PayaraExecutorService executorService;
    /**
     * System-Events interface.
     */
    @Inject
    private Events events;

    @PostConstruct
    public void postConstruct() {
        storage = this;
        this.events.register(this);
    }

    @PreDestroy
    public void preDestroy() {
        storage = null;
        this.events.unregister(this);
    }

    /**
     * Returns the singleton instance of this storage.
     * @return Singleton storage instance.
     */
    public static HazelcastTopicStorage getInstance() {
        return storage;
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            clearMessageReceivers();
        } else if(event.is(HazelcastEvents.HAZELCAST_SHUTDOWN_STARTED)) {
            unregisterRegisteredReceivers();
        } else if(event.is(HazelcastEvents.HAZELCAST_BOOTSTRAP_COMPLETE)) {
            registerUnregisteredReceivers();
        }
    }

    /**
     * Registers all yet unregistered {@link MessageReceiver}.
     */
    private void registerUnregisteredReceivers() {
        messageReceiver.values().stream()
            .filter(mapping -> !mapping.isRegistered())
            .forEach(mapping -> mapping.setRegistered(
                eventBus.addMessageReceiver(mapping.getTopic(), mapping.getMessageReceiver())
            ));
    }

    /**
     * Unregisters all registered {@link MessageReceiver}.
     */
    private void unregisterRegisteredReceivers() {
        messageReceiver.values().stream()
            .filter(ReceiverMapping::isRegistered)
            .forEach(mapping -> {
                eventBus.removeMessageReceiver(mapping.getTopic(), mapping.getMessageReceiver());
                mapping.setRegistered(false);
            });
    }

    /**
     * Removing all listeners.
     */
    private void clearMessageReceivers() {
        messageReceiver.clear();
    }

    /**
     * Processes the submitted work asynchronously.
     *
     * @param work The work to process.
     * @return The {@link Future} representing a handle for the processing.
     */
    Future<?> process(final Runnable work) {
        return executorService.submit(work);
    }

    /**
     * Tries to register the message listener with the provided topic by its name.
     * @param topic The name of the topic to register the listener with.
     * @param receiver The receiver to register
     * @return The internal id for the registered listener usable for removing the listener.
     */
    String registerMessageReceiver(String topic, MessageReceiver<HazelcastPayload> receiver) {
        ReceiverMapping receiverMapping = new ReceiverMapping(topic, receiver);
        receiverMapping.setRegistered(eventBus.addMessageReceiver(topic, receiver));
        messageReceiver.put(receiverMapping.getInternalId(), receiverMapping);
        return receiverMapping.getInternalId();
    }

    /**
     * Unregisters the listener identified by its internal id for the provided topic.
     * @param internalId The internal id identifying the listener.
     */
    void removeMessageReceiver(String internalId) {
        ReceiverMapping removedReceiver = messageReceiver.remove(internalId);
        if (removedReceiver != null) {
            eventBus.removeMessageReceiver(removedReceiver.getTopic(), removedReceiver.getMessageReceiver());
        }
    }

    /**
     * Publishes the {@link HazelcastPayload} at the topic.
     * @param topic The name of the topic to publish the payload.
     * @param payload The payload to publish.
     */
    void publish(String topic, HazelcastPayload payload) {
        eventBus.publish(topic, new ClusterMessage<>(payload));
    }

}
