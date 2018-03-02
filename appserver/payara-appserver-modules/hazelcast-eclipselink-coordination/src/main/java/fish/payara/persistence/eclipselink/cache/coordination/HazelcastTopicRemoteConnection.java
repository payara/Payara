package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.eclipse.persistence.internal.sessions.coordination.broadcast.BroadcastRemoteConnection;
import org.eclipse.persistence.sessions.coordination.Command;
import org.eclipse.persistence.sessions.coordination.RemoteCommandManager;

/**
 * Hazelcast {@link BroadcastRemoteConnection} implementing {@link MessageListener} interface.
 *
 * @author Sven Diedrichsen
 * @version $Id$
 * @since 23.02.18
 */
public class HazelcastTopicRemoteConnection extends BroadcastRemoteConnection implements MessageListener<Command> {
    /**
     * The topic to publish commands to and receive messages from.
     */
    private final HazelcastTopic topic;
    /**
     * The message listener id to use for removal.
     */
    private final String messageListenerId;

    public HazelcastTopicRemoteConnection(RemoteCommandManager rcm, HazelcastTopic topic) {
        super(rcm);
        this.topic = topic;
        this.messageListenerId = this.topic.registerMessageListener(this.rcm.getChannel(), this);
    }

    @Override
    protected Object executeCommandInternal(Object o) throws Exception {
        if(o != null && Command.class.isAssignableFrom(o.getClass())) {
            this.topic.publish(this.rcm.getChannel(), (Command)o);
        }
        return null;
    }

    @Override
    protected void closeInternal() throws Exception {
        this.topic.removeMessageListener(this.rcm.getChannel(), this.messageListenerId);
    }

    @Override
    public void onMessage(Message<Command> message) {
        String messageId = message.getPublishingMember().getUuid() + "-" + String.valueOf(message.getPublishTime());
        this.processReceivedObject(message.getMessageObject(), messageId);
    }

}
