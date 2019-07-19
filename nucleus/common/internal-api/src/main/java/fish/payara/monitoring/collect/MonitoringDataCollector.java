package fish.payara.monitoring.collect;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

/**
 * API for collecting monitoring data points from a {@link MonitoringDataSource}.
 * 
 * @author Jan Bernitt
 */
public interface MonitoringDataCollector {

    /**
     * Collect a single metric data point (within the current context of tags of this collector).
     * 
     * @param key the plain (context free) name of the metric (e.g. "size")
     * @param value the current value of the metric
     * @return this collector for chaining (with unchanged tags)
     */
    MonitoringDataCollector collect(CharSequence key, long value);

    /**
     * Creates a collector with an extended context.
     * 
     * For example if this collector has the context "foo=bar" and this method is called with name "x" and value "y" the
     * resulting context is "foo=bar x=y".
     * 
     * When the context of this collector already contains the tag name the context restarts from that tag regardless if
     * the value is different or identical to the previous one. For example if this collector has the context "foo=bar
     * x=y" and tag is called with name "foo" and value "baz" the new context is "foo=baz" (not "foo=bar x=y foo=baz").
     * 
     * @param name  name of the type of context
     * @param value identifier within the context type
     * @return A collector that collects data within the context of this collector extended by the given name-value
     *         pair.
     */
    MonitoringDataCollector tag(CharSequence name, CharSequence value);

    /*
     * Helper methods for convenience and consistent tagging.
     */

    /**
     * Same as {@link #collect(CharSequence, long)} except that zero value are ignored and not collected.
     */
    default MonitoringDataCollector collectNonZero(CharSequence key, long value) {
        if (value != 0L) {
            collect(key, value);
        }
        return this;
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}. Double values are converted to long by multiplying with 10K
     * effectively offering a precision of 4 fraction digits when converting back to FP number later on.
     */
    default MonitoringDataCollector collect(CharSequence key, double value) {
        return collect(key, Math.round(value * 10000L));
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}, true becomes 1L, false zero. 
     */
    default MonitoringDataCollector collect(CharSequence key, boolean value) {
        return collect(key, value ? 1L : 0L);
    }

    /**
     * Similar to {@link #collect(CharSequence, long)}, the char simply becomes a number
     */
    default MonitoringDataCollector collect(CharSequence key, char value) {
        return collect(key, (long) value);
    }

    default MonitoringDataCollector collect(CharSequence key, Number value) {
        if (value != null) {
            if (value instanceof Double || value instanceof Float) {
                return collect(key, value.doubleValue());
            }
            return collect(key, value.longValue());
        }
        return this;
    }

    /**
     * Same as calling {@link #collect(CharSequence, long)} with a non null value and {@link Instant#toEpochMilli()}.
     */
    default MonitoringDataCollector collect(CharSequence key, Instant value) {
        if (value != null) {
            return collect(key, value.toEpochMilli());
        }
        return this;
    }

    default <V> MonitoringDataCollector collectObject(V obj, BiConsumer<MonitoringDataCollector, V> collect) {
        collect.accept(this, obj);
        return this;
    }

    default <V> MonitoringDataCollector collectObjects(Collection<V> entries, BiConsumer<MonitoringDataCollector, V> collect) {
        for (V entry : entries) {
            collect.accept(this, entry);
        }
        return this;
    }

    default <K extends CharSequence, V> MonitoringDataCollector collectAll(Map<K, V> entries,
            BiConsumer<MonitoringDataCollector, V> collect) {
        collectNonZero("size", entries.size());
        for (Entry<K,V> entry : entries.entrySet()) {
            collect.accept(entity(entry.getKey()), entry.getValue());
        }
        return this;
    }

    default MonitoringDataCollector app(CharSequence appName) {
        return tag("app", appName);
    }

    /**
     * Namespaces are used to distinguish data points of different origin.
     * Each origin uses its own namespace. The namespace should be the first (top/right most) tag added.
     * 
     * @param namespace the namespace to use, e.g. "health-monitor"
     * @return A collector with the namespace context added/set
     */
    default MonitoringDataCollector in(CharSequence namespace) {
        return tag("ns", namespace);
    }

    default MonitoringDataCollector type(CharSequence type) {
        return tag("type", type);
    }

    /**
     * The entity tag gives the identity that distinguishes values of one entry from another within the same collection of entries.
     * The entity is usually the second tag added after the namespace (if needed).
     * 
     * @param entity the entity to use, e.g. "log-notifier" (as opposed to "teams-notifier")
     * @return A collector with the entity context added/set
     */
    default MonitoringDataCollector entity(CharSequence entity) {
        return tag("entity", entity);
    }

}
