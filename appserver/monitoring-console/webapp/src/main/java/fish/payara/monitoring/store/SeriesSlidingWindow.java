package fish.payara.monitoring.store;

import java.io.Serializable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keeps point data for a fixed window size.
 * 
 * When {@link #add(long, long)}ing points the window is first filled to it capacity. Since the buffers are twice the
 * window size they start to slide once the {@link #length()} reaches the {@link #capacity()}. When the sliding window
 * reaches the end of the buffer the most recent halve is copied to the first half and sliding starts from there again.
 * This keeps copying memory only occur every {@link #capacity()} points.
 * 
 * @author Jan Bernitt
 */
public final class SeriesSlidingWindow implements Serializable {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Series series;
    private final long[] times;
    private final long[] values;
    private volatile int offset;
    private volatile int length;

    public SeriesSlidingWindow(Series series, int size) {
        this.series = series;
        this.length = 0;
        this.offset = 0;
        this.values = new long[size * 2];
        this.times = new long[size * 2];
    }

    public Series getSeries() {
        return series;
    }

    public int length() {
        return length;
    }

    public int capacity() {
        return times.length / 2;
    }

    public long value(int index) {
        return values[offset + index];
    }

    public long time(int index) {
        return times[offset + index];
    }

    public long last() {
        return values[offset + length - 1];
    }

    public long start() {
        return length == 0 ? -1 : times[offset];
    }

    public Point[] snapshot() {
        lock.readLock().lock();
        try {
            Point[] points = new Point[length];
            for (int i = 0; i < length; i++) {
                points[i] = new Point(time(i), value(i));
            }
            return points;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(long time, long value) {
        lock.writeLock().lock();
        try {
            if (length < capacity()) {
                times[length] = time;
                values[length++] = value;
            } else {
                if (offset == length) { // time to roll over
                    System.arraycopy(times, offset, times, 0, length);
                    System.arraycopy(values, offset, values, 0, length);
                    offset = 0;
                }
                times[offset + length] = time;
                values[offset + length] = value;
                offset++;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < length; i++) {
            str.append(time(i)).append(':').append(value(i)).append('\n');
        }
        return str.toString();
    }
}
