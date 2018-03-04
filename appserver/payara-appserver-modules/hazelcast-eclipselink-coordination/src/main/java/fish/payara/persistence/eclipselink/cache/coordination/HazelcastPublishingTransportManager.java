package fish.payara.persistence.eclipselink.cache.coordination;

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
        this.connection = new HazelcastTopicRemoteConnection(this.getRemoteCommandManager());
    }

    @Override
    public void removeLocalConnection() {
        this.connection.close();
    }

}
