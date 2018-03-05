package fish.payara.persistence.eclipselink.cache.coordination;

import com.hazelcast.core.ITopic;
import com.hazelcast.core.MessageListener;
import fish.payara.nucleus.events.HazelcastEvents;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import org.eclipse.persistence.sessions.coordination.Command;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.glassfish.api.event.EventListener;

import java.util.Collection;
import java.util.Collections;
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
        if (event.is(EventTypes.SERVER_SHUTDOWN) || event.is(HazelcastEvents.HAZELCAST_SHUTDOWN_COMPLETE)) {
            destroyTopics(topicCache.values());
            topicCache.clear();
        }
    }

    private void destroyTopics(Collection<ITopic<Command>> topics) {
        for (ITopic<Command> topic : topics) {
            topic.destroy();
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

    public void destroyTopic(String name) {
        ITopic<Command> topic = topicCache.remove(name);
        if (topic != null) {
            destroyTopics(Collections.singleton(topic));
        }
    }

    private ITopic<Command> getTopic(String name) {
        ITopic<Command> topic = topicCache.get(name);
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

}
