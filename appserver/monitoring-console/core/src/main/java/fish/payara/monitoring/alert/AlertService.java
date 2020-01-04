package fish.payara.monitoring.alert;

import java.util.Collection;
import java.util.function.Predicate;

import org.jvnet.hk2.annotations.Contract;

import fish.payara.monitoring.model.Series;

/**
 * The {@link AlertService} manages and evaluates {@link Watch}s that cause {@link Alert}s.
 *
 * @author Jan Bernitt
 */
@Contract
public interface AlertService {

    /*
     * Alerts
     */

    class AlertStatistics {
        /**
         * Can be used by (asynchronous) consumers to determine if they have seen the most recent state of alerts. If
         * the change count is still the same they have processed already there is nothing new to process.
         */
        public int changeCount;
        public int unacknowledgedRedAlerts;
        public int acknowledgedRedAlerts;
        public int unacknowledgedAmberAlerts;
        public int acknowledgedAmberAlerts;
        public int watches;
    }

    AlertStatistics getAlertStatistics();

    Collection<Alert> alertsMatching(Predicate<Alert> filter);

    default Alert alertBySerial(int serial) {
        Collection<Alert> matches = alertsMatching(alert -> alert.serial == serial);
        return matches.isEmpty() ? null : matches.iterator().next();
    }

    default Collection<Alert> alertsFor(Series series) {
        return alertsMatching(alert -> alert.getSeries().equalTo(series));
    }

    default Collection<Alert> alerts() {
        return alertsMatching(alert -> true);
    }

    /*
     * Watches
     */

    /**
     * Adds a watch to the evaluation loop. To remove the watch just use {@link Watch#stop()}.
     *
     * @param watch new watch to add to evaluation loop
     */
    void addWatch(Watch watch);

    /**
     * @return All watches registered for evaluation.
     */
    Collection<Watch> watches();

    /**
     * @param series a simple or pattern {@link Series}, not null
     * @return All watches matching the given {@link Series}. {@link Series#ANY} will match all watches similar to
     *         {@link #watches()}.
     */
    Collection<Watch> wachtesFor(Series series);
}
