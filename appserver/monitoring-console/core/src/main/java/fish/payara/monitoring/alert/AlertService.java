package fish.payara.monitoring.alert;

import java.util.Collection;
import java.util.function.Predicate;

import org.jvnet.hk2.annotations.Contract;

import fish.payara.monitoring.model.Series;

@Contract
public interface AlertService {

    /*
     * Alerts
     */

    class AlertStatistics {
        /**
         * Can be used by (asynchronous) consumers to determine if they have seen the most recent state of alerts.
         * If the change count is still the same they have processed already there is nothing new to process.
         */
        public int changeCount;
        public int unacknowledgedRedAlerts;
        public int acknowledgedRedAlerts;
        public int unacknowledgedAmberAlerts;
        public int acknowledgedAmberAlerts;
    }

    AlertStatistics getAlertStatistics();
    Collection<Alert> alertsMatching(Predicate<Alert> filter);
    default Collection<Alert> alertsFor(Series series) {
        return alertsMatching(alert -> alert.getSeries().equalTo(series));
    }
    default Collection<Alert> alerts() {
        return alertsMatching(alert -> true);
    }

    /*
     * Watches
     */

    void addWatch(Watch watch);
    void removeWatchBySerial(int serial);
    Collection<Watch> watches();
    Collection<Watch> wachtesFor(Series series);
}
