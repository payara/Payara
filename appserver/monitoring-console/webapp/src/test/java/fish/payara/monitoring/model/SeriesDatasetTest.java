package fish.payara.monitoring.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.PartialDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * Tests the basic correctness of {@link PartialDataset}, in particular the correctness of the sliding window mechanism.
 * 
 * @author Jan Bernitt
 */
public class SeriesDatasetTest {

    private static final Series SERIES = new Series("test");

    @Test
    public void fillToCapacity() {
        int capacity = 3;
        SeriesDataset set = new EmptyDataset(SERIES, 3);
        assertEquals(capacity, set.capacity());
        assertEquals(0, set.size());
        set = set.add(1, 1);
        assertValues(set, 1);
        set = set.add(2, 2);
        assertValues(set, 1, 2);
        set = set.add(3, 3);
        assertValues(set, 1, 2, 3);
    }

    @Test
    public void fillAndSlideByCapacity() {
        SeriesDataset set = new EmptyDataset(SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 2);
        set = set.add(3, 3);
        // now capacity is reached
        assertValues(set, 1, 2, 3);
        set = set.add(4, 4);
        assertValues(set, 2, 3, 4);
        set = set.add(5, 5);
        assertValues(set, 3, 4, 5);
        set = set.add(6, 6);
        assertValues(set, 4, 5, 6);
    }

    @Test
    public void fillAndSlideOverCapacity() {
        SeriesDataset set = new EmptyDataset(SERIES, 3);
        set = set.add(1, 1);
        SeriesDataset set1 = set;
        set = set.add(2, 2);
        set = set.add(3, 3);
        // capacity reached
        set = set.add(4, 4);
        set = set.add(5, 5);
        set = set.add(6, 6);
        assertFalse(set1.isOutdated());
        assertValues(set, 4, 5, 6);
        // did slide by capacity 
        set = set.add(7, 7);
        assertTrue(set.isOutdated());
        assertValues(set, 5, 6, 7);
        set = set.add(8, 8);
        assertValues(set, 6, 7, 8);
        set = set.add(9, 9);
        assertValues(set, 7, 8, 9);
        set = set.add(10, 10);
        assertValues(set, 8, 9, 10);
    }

    @Test
    public void fillAndSlideManyTimesOverCapacity() {
        SeriesDataset set = new EmptyDataset(SERIES, 3);
        for (int i = 0; i < 100; i++) {
            set = set.add(i, i);
        }
        assertValues(set, 97, 98, 99);
        assertEquals(100, set.getObservedValues());
        assertEquals(100, set.getObservedValueChanges());
        assertEquals(BigInteger.valueOf(49), set.getObservedAvg());
        assertEquals(99L, set.getObservedMax());
        assertEquals(0L, set.getObservedMin());
    }
    
    //TODO add tests for constant => partial, partial => constant

    private static void assertValues(SeriesDataset set, long... values) {
        int size = set.size();
        assertEquals(values.length, size);
        long[] points = set.points();
        for (int i = 0; i < size; i++) {
            assertEquals(values[i], points[i*2]);
            assertEquals(values[i], points[i*2+1]);
        }
    }
}
