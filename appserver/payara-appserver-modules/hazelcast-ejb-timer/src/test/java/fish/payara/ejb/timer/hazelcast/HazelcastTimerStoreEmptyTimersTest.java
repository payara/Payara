package fish.payara.ejb.timer.hazelcast;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HazelcastTimerStoreEmptyTimersTest extends HazelcastTimerStoreTestBase {
  private Collection<HZTimer> timers = Collections.emptyList();

  @Test
  public void emptyTimersShallResultInZeroTimersCountedForServer() {
    String [] counts = callListTimers(timers, "a");

    assertEquals("With no timers defined, zero timers is expected for given server id", "0", counts[0]);
  }

  @Test
  public void emptyTimersShallResultInArrayOfTheSameSizeAsServerIds() {
    String [] counts = callListTimers(timers, "a", "b", "c", "d");

    assertEquals("Size of counters array shall match the size of server ids array", 4, counts.length);
  }
}

