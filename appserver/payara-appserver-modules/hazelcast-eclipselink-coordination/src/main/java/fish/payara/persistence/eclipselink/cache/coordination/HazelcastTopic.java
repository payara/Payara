package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.MessageListener;
import org.eclipse.persistence.sessions.coordination.Command;

/**
 * Allow for Hazelcast topic proxying.
 */
public interface HazelcastTopic {
    /**
     * Registers the message listener and returns its id.
     * @param topic The topic name to register with.
     * @param listener The listener to register.
     * @return the listener id.
     */
    String registerMessageListener(String topic, MessageListener<Command> listener);
    /**
     * Removes the message listener.
     * @param topic The topic name to remove from.
     * @param messageListenerId The listener id.
     */
    void removeMessageListener(String topic, String messageListenerId);

    /**
     * Publishes the command.
     * @param topic The topic name to publish the command with.
     * @param command The command to publish.
     */
    void publish(String topic, Command command);
}
