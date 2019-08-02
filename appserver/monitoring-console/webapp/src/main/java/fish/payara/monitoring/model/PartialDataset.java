package fish.payara.monitoring.model;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * Keeps point data for a fixed window size.
 * 
 * The implementation is "effectively immutable". While all primitive fields are immutable the sliding window itself is
 * shared. This does not affect the immutability of the observable window as long as the number of added points since
 * the instance was created is less than the window size. An out-dated window can be detected though using
 * {@link #isOutdated()}. In practice the instances can be treated as if they are fully immutable as long as they are
 * monotonically updated and reading is done from the more recent instances.
 * 
 * When {@link #add(long, long)}ing points the window is first filled to it capacity. Since the buffers are twice the
 * window size they start to slide once the {@link #size()} reaches the {@link #capacity()}. When the sliding window
 * reaches the end of the buffer the most recent halve is copied to the first half and sliding starts from there again.
 * This keeps copying memory only occur every {@link #capacity()} points.
 * 
 * @author Jan Bernitt
 */
public final class PartialDataset extends SeriesDataset {

    /**
     * The time expected at {@link #time(int)} called with zero. If this time is different the window is out-dated.
     */
    private final long time0;
    /**
     * Sliding window twice the observable {@link #capacity()} for time and values alternating: [t1,v1,t2,v2,...]
     * 
     * Using a single array has the advantage of data being co-located in memory and copying while rolling over is
     * reduced to just one operation. Also a snapshot of the points can be created in a single copy operation and
     * represented efficiently in memory as a long[].
     */
    private final long[] data;
    private final int offset;
    private final int size;

    // further statistics 
    private final int observedValueChanges;
    private final long observedMax;
    private final long observedMin;
    private final BigInteger observedSum;
    private final int stableCount;
    private final long stableSince;

    PartialDataset(ConstantDataset predecessor, long time, long value) {
        super(predecessor.getSeries(), predecessor.getObservedValues() + 1);
        this.size = predecessor.size() + 1;
        this.offset = 0;
        this.data = new long[predecessor.capacity() * 4];
        int i = 0;
        this.data[i++] = predecessor.getStableSince();
        this.data[i++] = predecessor.lastValue();
        if (size == 3) {
            this.data[i++] = predecessor.firstTime();
            this.data[i++] = predecessor.lastValue();
        }
        this.data[i++] = time;
        this.data[i] = value;
        this.time0 = data[0];
        this.observedValueChanges = predecessor.getObservedValueChanges() + 1;
        this.observedMax = Math.max(value, predecessor.getObservedMax());
        this.observedMin = Math.min(value, predecessor.getObservedMin());
        this.observedSum = predecessor.getObservedSum().add(BigInteger.valueOf(value));
        this.stableCount = 1;
        this.stableSince = time;
    }

    private PartialDataset(PartialDataset predecessor, int size, int offset, long time, long value) {
        super(predecessor.getSeries(), predecessor.getObservedValues() + 1);
        this.size = size;
        this.offset = offset;
        this.data = predecessor.data;
        boolean stable = predecessor.size != 0 && value == predecessor.lastValue();
        this.observedValueChanges =  predecessor.observedValueChanges + (stable ? 0 : 1);
        this.time0 = predecessor.size == 0 ? time : predecessor.time0;
        this.observedMax = Math.max(value, predecessor.observedMax);
        this.observedMin = Math.min(value, predecessor.observedMin);
        this.observedSum = predecessor.observedSum.add(BigInteger.valueOf(value));
        this.stableCount = stable ? predecessor.stableCount + 1 : 1;
        this.stableSince = stable ? predecessor.stableSince : time;
    }

    @Override
    public boolean isOutdated() {
        return time(0) != time0;
    }

    @Override
    public long getStableSince() {
        return stableSince;
    }

    @Override
    public int getStableCount() {
        return stableCount;
    }

    @Override
    public long getObservedMin() {
        return observedMin;
    }

    @Override
    public long getObservedMax() {
        return observedMax;
    }

    @Override
    public BigInteger getObservedSum() {
        return observedSum;
    }

    @Override
    public int getObservedValueChanges() {
        return observedValueChanges;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int capacity() {
        return data.length / 4; // 2x because of window, 2x because time and values are in one array
    }

    public long value(int index) {
        return data[2 * (offset + index) + 1];
    }

    public long time(int index) {
        return data[2 * (offset + index)];
    }

    @Override
    public long lastValue() {
        return value(size - 1);
    }

    @Override
    public long firstTime() {
        return size == 0 ? -1 : time(0);
    }

    @Override
    public long[] points() {
        return Arrays.copyOfRange(data, 2 * offset, 2 * (offset + size));
    }

    @Override
    public SeriesDataset add(long time, long value) {
        int newOffset = offset;
        int newSize = size;
        if (size < capacity()) {
            data[2 * size] = time;
            data[2 * size + 1] = value;
            newSize++; // expand towards the end
        } else {
            if (offset == size) { // is it time to roll over?
                if (isStable(value)) { // never observed a different value ?
                    // go back to stable form, no point in occupying memory for something stable
                    return new StableDataset(this, time);
                }
                System.arraycopy(data, offset * 2, data, 0, size * 2);
                newOffset = 0;
            }
            data[2 * (newOffset + size)] = time;
            data[2 * (newOffset + size) + 1] = value;
            newOffset++; // slide the window towards the end
        }
        return new PartialDataset(this, newSize, newOffset, time, value);
    }

    private boolean isStable(long value) {
        int i = data.length - 1;
        for (int j = 0; j < size; j++) {
            if (data[i] != value) {
                return false;
            }
            i -=2;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < size; i++) {
            str.append(time(i)).append(':').append(value(i)).append('\n');
        }
        return str.toString();
    }

    @Override
    public int estimatedBytesMemory() {
        return 92 + (data.length * 8);
    }
}
