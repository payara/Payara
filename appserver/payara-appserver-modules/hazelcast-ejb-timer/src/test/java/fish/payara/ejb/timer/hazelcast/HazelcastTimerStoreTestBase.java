package fish.payara.ejb.timer.hazelcast;

import java.util.Collection;

public abstract class HazelcastTimerStoreTestBase {
  public String[] callListTimers(Collection timers, String[] serverIds) {
    return HazelcastTimerStore.listTimers(timers, serverIds);
  }
}

