package fish.payara.monitoring.collect;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;

/**
 * Utility to collect beans using their getter {@link Method}s.
 * 
 * Only return types a known collection function has been {@link #register(Class, BiConsumer)}ed for are included.
 * 
 * By default {@link RangeStatistic} and {@link CountStatistic} are mapped.
 * 
 * The usual usage of this helper is to call {@link #collectObject(MonitoringDataCollector, Object)}.
 */
public final class MonitoringDataCollection {

    private MonitoringDataCollection() {
        throw new UnsupportedOperationException("util");
    }

    private static final Map<Class<?>, BiConsumer<MonitoringDataCollector, ?>>  MAPPED_TYPES = new ConcurrentHashMap<>();

    static {
        register(RangeStatistic.class, MonitoringDataCollection::collectRange);
        register(CountStatistic.class, MonitoringDataCollection::collectCount);
    }

    /**
     * Can be used to plug in collection functions for types that otherwise would not be collected even for objects that
     * are passed to collection elsewhere.
     * 
     * @param type the type as stated by the return type of the getter method returning the value to collect from an object
     * @param collectWith the function to use to collect values of the given type
     */
    public static <T> void register(Class<T> type, BiConsumer<MonitoringDataCollector, T> collectWith) {
        MAPPED_TYPES.putIfAbsent(type, collectWith);
    }

    public static void collectObject(MonitoringDataCollector collector, Object obj) {
        for (Method getter : obj.getClass().getMethods()) {
            if (isGetter(getter)) {
                try {
                    Object value = getter.invoke(obj);
                    Class<?> returnType = getter.getReturnType();
                    if (MAPPED_TYPES.containsKey(returnType)) {
                        collectMapped(collector, returnType, value);
                    }
                } catch (Exception ex) {
                    // ignore this getter
                }
            }
        }
    }

    private static boolean isGetter(Method method) {
        return method.getParameterCount() == 0 
                && (method.getName().startsWith("get") || method.getName().startsWith("is"));
    }

    @SuppressWarnings("unchecked")
    private static <T> void collectMapped(MonitoringDataCollector collector, Class<T> type, Object value) {
        collector.collectObject((T) value, (BiConsumer<MonitoringDataCollector, T>) MAPPED_TYPES.get(type));
    }

    public static void collectRange(MonitoringDataCollector collector, RangeStatistic range) {
        collector
            .collect(range.getName(), range.getCurrent())
            .group(range.getName())
                .collect("lowWaterMark", range.getLowWaterMark())
                .collect("highWaterMark", range.getHighWaterMark());
    }

    public static void collectCount(MonitoringDataCollector collector, CountStatistic count) {
        collector
            .collect(count.getName(), count.getCount());
    }
}
