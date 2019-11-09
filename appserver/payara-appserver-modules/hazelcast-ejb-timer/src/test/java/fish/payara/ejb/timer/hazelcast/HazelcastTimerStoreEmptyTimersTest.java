package fish.payara.ejb.timer.hazelcast;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

public class HazelcastTimerStoreEmptyTimersTest extends HazelcastTimerStoreTestBase {
  private Collection<HZTimer> timers = Collections.emptyList();

  @Test
  public void emptyTimersShallResultInZeroTimersCountedForServer() {
    String [] serverIds = new String[] { "a" };

    String [] counts = callListTimers(timers, serverIds);

    assert counts[0].equals("0") : "With no timers defined, zero timers is expected for given server id";
  }

  @Test
  public void emptyTimersShallResultInArrayOfTheSameSizeAsServerIds() {
    String [] serverIds = new String[] { "a", "b", "c", "d" };

    String [] counts = callListTimers(timers, serverIds);

    assert counts.length == serverIds.length : "Size of counters array shall match the size of server ids array";
  }
}

