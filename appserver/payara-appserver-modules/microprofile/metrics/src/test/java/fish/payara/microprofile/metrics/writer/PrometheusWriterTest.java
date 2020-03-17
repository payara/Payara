package fish.payara.microprofile.metrics.writer;

import java.io.PrintWriter;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricType;
import org.junit.Test;

import fish.payara.microprofile.metrics.impl.Clock;
import fish.payara.microprofile.metrics.impl.SimpleTimerImpl;

public class PrometheusWriterTest {

    @Test
    public void test() {
        SimpleTimerImpl timer = new SimpleTimerImpl(Clock.DEFAULT);
        timer.time().stop();
        timer.time().stop();
        Metadata metadata = Metadata.builder().withName("name").withType(MetricType.SIMPLE_TIMER).build();
        StringBuilder out = new StringBuilder();
        PrometheusWriter.writeMetric(out, new MetricID("name"), "reg-name-metric-name", timer, metadata);
        System.out.println(out);
    }
}
