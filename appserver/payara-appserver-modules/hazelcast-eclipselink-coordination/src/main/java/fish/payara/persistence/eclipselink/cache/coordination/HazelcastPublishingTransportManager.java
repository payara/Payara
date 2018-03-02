package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.ITopic;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import org.eclipse.persistence.sessions.coordination.Command;
import org.eclipse.persistence.sessions.coordination.RemoteCommandManager;
import org.eclipse.persistence.sessions.coordination.broadcast.BroadcastTransportManager;

/**
 * Hazelcast based {@link BroadcastTransportManager}.
 *
 * @author Sven Diedrichsen
 * @version $Id$
 * @since 23.02.18
 */
public class HazelcastPublishingTransportManager extends BroadcastTransportManager {

    private HazelcastTopicRemoteConnection connection;

    public HazelcastPublishingTransportManager(RemoteCommandManager rcm) {
        super(rcm);
    }

    @Override
    public void createLocalConnection() {
        RemoteCommandManager rcm = this.getRemoteCommandManager();
        this.connection = new HazelcastTopicRemoteConnection(rcm, new HazelcastTopic(rcm.getChannel()));
    }

    @Override
    public void removeLocalConnection() {
        this.connection.close();
    }

}
