package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.MessageListener;
import org.eclipse.persistence.sessions.coordination.Command;

/**
 * Allow for Hazelcast topic proxying.
 */
public class HazelcastTopic {

    private final String name;

    /**
     * Ctor.
     * @param name The name of the topic.
     */
    public HazelcastTopic(String name) {
        this.name = name;
    }

    /**
     * Registers the message listener and returns its id.
     * @param listener The listener to register.
     * @return the listener id.
     */
    public String registerMessageListener(MessageListener<Command> listener) {
        return getStorage().registerMessageListener(name, listener);
    }

    /**
     * Removes the message listener.
     * @param messageListenerId The listener id.
     */
    public void removeMessageListener(String messageListenerId) {
        getStorage().removeMessageListener(name, messageListenerId);
    }

    /**
     * Publishes the command.
     * @param command The command to publish.
     */
    public void publish(Command command) {
        getStorage().publish(name, command);
    }

    private HazelcastTopicStorage getStorage() {
        return HazelcastTopicStorage.getInstance();
    }

}
