package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import org.eclipse.persistence.sessions.coordination.Command;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.glassfish.api.event.EventListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Service(name = "hazelcast-topic-storage")
@RunLevel(StartupRunLevel.VAL)
public class HazelcastTopicStorage implements EventListener {

    private static HazelcastTopicStorage storage;

    private final Map<String, ITopic<Command>> topicCache = new ConcurrentHashMap<>();

    @Inject
    private Events events;

    @PostConstruct
    public void postConstruct() {
        storage = this;
        events.register(this);
    }

    public static HazelcastTopicStorage getInstance() {
        return storage;
    }

    @Override
    public void event(Event event) {
        if (event.is(EventTypes.SERVER_SHUTDOWN)) {
            topicCache.clear();
        }
    }

    public String registerMessageListener(String topic, MessageListener<Command> listener) {
        return getTopic(topic).addMessageListener(listener);
    }

    public void removeMessageListener(String topic, String messageListenerId) {
        getTopic(topic).removeMessageListener(messageListenerId);
    }

    public void publish(String topic, Command command) {
        getTopic(topic).publish(command);
    }

    private ITopic<Command> getTopic(String name) {
        ITopic<Command> topic = topicCache.get(name);
        if(topic == null) {
            topic = HazelcastCore.getCore().getInstance().getTopic(name);
            topicCache.put(name, topic);
        }
        return topic;
    }

}
