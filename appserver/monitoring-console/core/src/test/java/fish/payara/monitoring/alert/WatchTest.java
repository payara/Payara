package fish.payara.monitoring.alert;

import static fish.payara.monitoring.alert.Condition.Operator.GE;
import static fish.payara.monitoring.alert.Condition.Operator.LT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.Unit;

public class WatchTest {

    @Test
    public void test() {
        assertNotNull(new Watch("Heap Usage", new Metric(new Series("ns:jvm HeapUsage"), Unit.PERCENT), 
                new Circumstance(Level.RED, new Condition(GE, 30, 5, 0, 0), new Condition(LT, 30, 5, 0, 0)), 
                new Circumstance(Level.AMBER, new Condition(GE, 20, 5, 0, 0), new Condition(LT, 20, 5, 0, 0)), 
                new Circumstance(Level.GREEN, new Condition(LT, 20, 1, 0, 0), Condition.NONE), 
                new Metric(new Series("ns:jvm CpuUsage"), Unit.PERCENT)));
    }
}
