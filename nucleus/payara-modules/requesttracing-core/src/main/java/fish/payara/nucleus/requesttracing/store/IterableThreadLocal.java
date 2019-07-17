package fish.payara.nucleus.requesttracing.store;

import static java.util.Collections.synchronizedMap;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * A {@link ThreadLocal} that allows to iterate over all available {@link Thread}-value pairs.
 * Note that this is not strongly consistent and may differ from actual current state in moment of change.
 * 
 * @author Jan Bernitt
 *
 * @param <T> Type of the thread local value
 */
public class IterableThreadLocal<T> extends ThreadLocal<T> implements Iterable<Entry<Thread, T>> {

    private final Map<Thread, T> entries;
    private final Supplier<? extends T> initialValue;

    public IterableThreadLocal(Supplier<? extends T> initialValue) {
        this.initialValue = initialValue;
        this.entries = synchronizedMap(new WeakHashMap<>());
    }

    @Override
    protected T initialValue() {
        T value = initialValue != null ? initialValue.get() : super.initialValue();
        entries.put(Thread.currentThread(), value);
        return value;
    }

    @Override
    public void set(T value) {
        super.set(value);
        entries.put(Thread.currentThread(), value);
    }

    @Override
    public void remove() {
        super.remove();
        entries.remove(Thread.currentThread());
    }

    @Override
    public Iterator<Entry<Thread, T>> iterator() {
        return entries.entrySet().iterator();
    }

}
