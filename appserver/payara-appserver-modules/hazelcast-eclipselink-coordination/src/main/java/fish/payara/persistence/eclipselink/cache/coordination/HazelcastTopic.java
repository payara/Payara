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

import fish.payara.nucleus.eventbus.MessageReceiver;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Representation of the Hazelcast topic to allow for proxying.
 *
 * @author Sven Diedrichsen
 */
final class HazelcastTopic {

    /**
     * The topic name to use.
     */
    private final String name;
    /**
     * The message listener id to unregister with.
     */
    private final String messageListenerId;
    /**
     * Stores the Ids of the messages published by this topic.
     */
    private final Set<UUID> publishedMessageIds = new ConcurrentSkipListSet<>();

    /**
     * Ctor.
     * @param name The name of the topic.
     */
    HazelcastTopic(String name, MessageReceiver<HazelcastPayload> receiver) {
        this.name = name;
        this.messageListenerId = getStorage().registerMessageReceiver(name, receiver);
    }

    /**
     * Publishes the command.
     * @param payload The {@link HazelcastPayload} to publish.
     */
    void publish(HazelcastPayload payload) {
        publishedMessageIds.add(payload.getId());
        getStorage().publish(name, payload);
    }

    /**
     * Checks if the provided payload has been published by this topic.
     * @param payload The payload to check if it has been published with this topic.
     * @return True if it has been published.
     */
    boolean hasPublished(HazelcastPayload payload) {
        return publishedMessageIds.remove(payload.getId());
    }

    /**
     * Destroys the referenced topic.
     */
    void destroy() {
        publishedMessageIds.clear();
        getStorage().removeMessageReceiver(messageListenerId);
    }

    /**
     * @return The hz topic storage.
     */
    private HazelcastTopicStorage getStorage() {
        return HazelcastTopicStorage.getInstance();
    }

}
