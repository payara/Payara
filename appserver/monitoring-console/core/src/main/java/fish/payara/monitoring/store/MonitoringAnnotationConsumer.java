package fish.payara.monitoring.store;

/**
 * A {@link MonitoringAnnotationConsumer} observes all monitoring annotations as a sequence of calls to
 * {@link #accept(CharSequence, long, String[])} where all values have been converted to longs.
 * 
 * An annotation is linked to the corresponding monitoring data by series name and time.
 * 
 * @author Jan Bernitt
 * @since 5.201
 */
@FunctionalInterface
public interface MonitoringAnnotationConsumer {

    /**
     * Publishes an annotation to this consumer.
     *
     * @param series      the full metric name, e.g. <code>x:y a:b xCount</code>
     * @param value       numeric value of the metric
     * @param attrs       a sequence of key-value pairs the value is annotated with. 
     *                    For example: ["name", "Foo", "age", "7"]
     */
    void accept(CharSequence series, long value, String[] attrs);
}
