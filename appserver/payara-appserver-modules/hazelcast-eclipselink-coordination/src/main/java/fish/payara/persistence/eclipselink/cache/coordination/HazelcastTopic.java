package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.MessageListener;

/**
 * Representation of the Hazelcast topic to allow for proxying.
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
     * Ctor.
     * @param name The name of the topic.
     */
    HazelcastTopic(String name, MessageListener<HazelcastPayload> listener) {
        this.name = name;
        this.messageListenerId = getStorage().registerMessageListener(name, listener);
    }

    /**
     * Publishes the command.
     * @param payload The {@link HazelcastPayload} to publish.
     */
    void publish(HazelcastPayload payload) {
        getStorage().publish(name, payload);
    }

    /**
     * Destroys the referenced topic.
     */
    void destroy() {
        getStorage().removeMessageListener(name, messageListenerId);
        getStorage().destroyTopic(name);
    }

    /**
     * @return The storage UUID representing this cluster member.
     */
    String getMemberUuid() {
        return getStorage().getMemberUuid();
    }

    /**
     * @return The hz topic storage.
     */
    private HazelcastTopicStorage getStorage() {
        return HazelcastTopicStorage.getInstance();
    }

}
