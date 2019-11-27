package fish.payara.ejb.timer.hazelcast;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HazelcastTimerStoreEmptyTimersTest extends HazelcastTimerStoreTestBase {
  private Collection<HZTimer> timers = Collections.emptyList();

  @Test
  public void emptyTimersShallResultInZeroTimersCountedForServer() {
    String [] serverIds = new String[] { "a" };

    String [] counts = callListTimers(timers, serverIds);

    assertEquals("With no timers defined, zero timers is expected for given server id", "0", counts[0]);
  }

  @Test
  public void emptyTimersShallResultInArrayOfTheSameSizeAsServerIds() {
    String [] serverIds = new String[] { "a", "b", "c", "d" };

    String [] counts = callListTimers(timers, serverIds);

    assertEquals("Size of counters array shall match the size of server ids array", serverIds.length, counts.length);
  }
}

