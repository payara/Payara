package fish.payara.monitoring.alert;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Unit;

public class WatchTest {

    @Test
    public void livelinessUp() {
        Watch livelinessUp = new Watch("Liveliness UP", new Metric(new Series("ns:health LivelinessUp"), Unit.PERCENT))
                .red(-40, null, false, null, null, false)
                .amber(-100, null, false, null, null, false)
                .green(100, null, false, null, null, false);

        SeriesDataset set = new EmptyDataset("server", livelinessUp.watched.series, 60);
        set = set.add(1, 50);
        List<SeriesDataset> matches = new ArrayList<>();
        matches.add(set);
        SeriesLookup lookup = (series, instances) -> matches;
        List<Alert> alerts = livelinessUp.check(lookup);
        assertEquals(1, alerts.size());
        Alert alert = alerts.get(0);
        assertEquals(Level.AMBER, alert.getLevel());

        set = set.add(2, 50);
        matches.set(0, set);

        alerts = livelinessUp.check(lookup);
        assertEquals(0, alerts.size());
    }
}
