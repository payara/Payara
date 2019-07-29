package fish.payara.monitoring.store;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import fish.payara.monitoring.store.Series;
import fish.payara.monitoring.store.SeriesSlidingWindow;

/**
 * Tests the basic correctness of {@link SeriesSlidingWindow}, in particular the correctness of the sliding window mechanism.
 * 
 * @author Jan Bernitt
 */
public class SeriesSlidingWindowTest {

    private static final Series SERIES = new Series("test");

    @Test
    public void fillToCapacity() {
        int capacity = 3;
        SeriesSlidingWindow window = new SeriesSlidingWindow(SERIES, capacity);
        assertEquals(capacity, window.capacity());
        assertEquals(0, window.length());
        window.add(1, 1);
        assertValues(window, 1);
        window.add(2, 2);
        assertValues(window, 1, 2);
        window.add(3, 3);
        assertValues(window, 1, 2, 3);
    }

    @Test
    public void fillAndSlideByCapacity() {
        SeriesSlidingWindow window = new SeriesSlidingWindow(SERIES, 3);
        window.add(1, 1);
        window.add(2, 2);
        window.add(3, 3);
        // now capacity is reached
        assertValues(window, 1, 2, 3);
        window.add(4, 4);
        assertValues(window, 2, 3, 4);
        window.add(5, 5);
        assertValues(window, 3, 4, 5);
        window.add(6, 6);
        assertValues(window, 4, 5, 6);
    }

    @Test
    public void fillAndSlideOverCapacity() {
        SeriesSlidingWindow window = new SeriesSlidingWindow(SERIES, 3);
        window.add(1, 1);
        window.add(2, 2);
        window.add(3, 3);
        // capacity reached
        window.add(4, 4);
        window.add(5, 5);
        window.add(6, 6);
        assertValues(window, 4, 5, 6);
        // did slide by capacity 
        window.add(7, 7);
        assertValues(window, 5, 6, 7);
        window.add(8, 8);
        assertValues(window, 6, 7, 8);
        window.add(9, 9);
        assertValues(window, 7, 8, 9);
        window.add(10, 10);
        assertValues(window, 8, 9, 10);
    }

    @Test
    public void fillAndSlideManyTimesOverCapacity() {
        SeriesSlidingWindow window = new SeriesSlidingWindow(SERIES, 3);
        for (int i = 0; i < 100; i++) {
            window.add(i, i);
        }
        assertValues(window, 97, 98, 99);
    }

    private static void assertValues(SeriesSlidingWindow window, long... values) {
        assertEquals(values.length, window.length());
        for (int i = 0; i < values.length; i++) {
            assertEquals(values[i], window.value(i));
        }
    }
}
