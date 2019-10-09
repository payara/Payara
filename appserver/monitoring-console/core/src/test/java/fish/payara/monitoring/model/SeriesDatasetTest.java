/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.monitoring.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;

import org.junit.Test;

import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * Tests the basic correctness of {@link SeriesDataset} implementation, in particular the correctness of the sliding
 * window mechanism.
 * 
 * @author Jan Bernitt
 */
public class SeriesDatasetTest {

    private static final String INSTANCE = "server";
    private static final Series SERIES = new Series("test");

    @Test
    public void emptyDefaults() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        assertEquals(0, set.size());
        assertEquals(Long.MAX_VALUE, set.getObservedMin());
        assertEquals(Long.MIN_VALUE, set.getObservedMax());
        assertEquals(BigInteger.ZERO, set.getObservedAvg());
        assertEquals(0, set.getObservedValues());
        assertEquals(0, set.getObservedValueChanges());
        assertEquals(-1L, set.firstTime());
        assertEquals(0L, set.lastValue());
    }

    @Test
    public void fillToCapacity() {
        int capacity = 3;
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
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
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
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
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
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
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        for (int i = 0; i < 100; i++) {
            set = set.add(i, i);
        }
        assertValues(set, 97, 98, 99);
        assertEquals(100, set.getObservedValues());
        assertEquals(100, set.getObservedValueChanges());
        assertEquals(BigInteger.valueOf(49), set.getObservedAvg());
        assertEquals(99L, set.getObservedMax());
        assertEquals(0L, set.getObservedMin());
        assertEquals(BigInteger.valueOf(50), set.add(100, 100).getObservedAvg());
    }

    @Test
    public void constantToPartialDataset() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        assertEquals(0, set.size());
        long t = 0;
        set = set.add(t++, 5);
        assertValues(set, false, 5);
        for (int i = 0; i < 20; i++) {
            set = set.add(t++, 5);
        }
        assertValues(set, false, 5, 5);
        assertEquals(0, set.getStableSince());
        assertEquals(21, set.getStableCount());
        assertEquals(5, set.getObservedMin());
        assertEquals(5, set.getObservedMax());
        assertEquals(BigInteger.valueOf(5), set.getObservedAvg());
        set = set.add(t++, 6);
        assertValues(set, false, 5, 5, 6);
        assertEquals(21, set.getStableSince());
        assertEquals(1, set.getStableCount());
        assertEquals(5, set.getObservedMin());
        assertEquals(6, set.getObservedMax());
        assertEquals(BigInteger.valueOf(5), set.getObservedAvg());
        assertEquals(22, set.getObservedValues());
        assertEquals(2, set.getObservedValueChanges());
        assertEquals(3, set.capacity());
    }

    @Test
    public void constantDataset() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        assertEquals(0, set.size());
        long t = 0;
        set = set.add(t++, 5);
        assertValues(set, false, 5);
        for (int i = 0; i < 20; i++) {
            set = set.add(t++, 5);
            assertEquals(2, set.size());
        }
        assertValues(set, false, 5, 5);
        assertEquals(0, set.getStableSince());
        assertEquals(21, set.getStableCount());
        assertEquals(20, set.points()[2]);
        assertEquals(21, set.getObservedValues());
        assertEquals(1, set.getObservedValueChanges());
        assertEquals(BigInteger.valueOf(5), set.getObservedAvg());
        assertEquals(5, set.getObservedMin());
        assertEquals(5, set.getObservedMax());
        set = set.add(20, 5);
        assertEquals(20, set.points()[2]);
        assertEquals(3, set.capacity());
    }

    @Test
    public void partialToStableDataset() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 2);
        set = set.add(3, 3);
        assertEquals(3, set.size());
        set = set.add(4, 3);
        assertEquals(3, set.size());
        set = set.add(5, 3);
        assertEquals(3, set.size());
        set = set.add(6, 3);
        assertEquals(3, set.size());
        set = set.add(7, 3);
        assertEquals(2, set.size());
        assertEquals(3, set.getStableSince());
        assertEquals(5, set.getStableCount());
        assertEquals(3, set.capacity());
    }

    @Test
    public void stableToPartialDataset() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 2);
        // at this point we are partial
        set = set.add(3, 3);
        set = set.add(4, 3);
        set = set.add(5, 3);
        set = set.add(6, 3);
        set = set.add(7, 3);
        // now stable again
        assertEquals(2, set.size());
        assertEquals(3, set.capacity());
        assertValues(set.add(8, 3), false, 3, 3);
        assertValues(set.add(8, 4), false, 3, 3, 4);
        assertEquals(3, set.add(8, 4).capacity());
    }

    @Test
    public void perSecondDelta() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 10);
        set = set.add(1000, 1);
        set = set.add(2000, 2);
        set = set.add(4000, 4);
        set = set.add(5000, 4);
        set = set.add(7000, 8);
        set = set.add(8000, 10);
        set = set.add(9500, 19);

        long[] perSecond = SeriesDataset.perSecond(set.points());
        assertEquals((set.size() - 1) * 2, perSecond.length);
        assertArrayEquals(new long[] { 2000L, 1L, 4000L, 1L, 5000L, 0L, 7000L, 2L, 8000L, 2L, 9500L, 6L }, perSecond);
    }

    @Test
    public void constantAddingWithSameTimeSumsValue() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 1);
        assertTrue(set instanceof ConstantDataset);
        set = set.add(2, 3);
        assertTrue(set instanceof PartialDataset);
        assertEquals(1 + 3, set.lastValue());
        assertEquals(2, set.size());
        assertEquals(3, set.getObservedValues());
        assertEquals(2, set.lastTime());
    }

    @Test
    public void partialAddingWithSameTimeSumsValue() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 2);
        assertTrue(set instanceof PartialDataset);
        set = set.add(2, 4);
        assertEquals(2 + 4, set.lastValue());
        assertEquals(2, set.size());
        assertEquals(3, set.getObservedValues());
        assertEquals(2, set.lastTime());
    }

    @Test
    public void stableAddingWithSameTimeSumsValue() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 3);
        set = set.add(1, 1);
        set = set.add(2, 2);
        assertTrue(set instanceof PartialDataset);
        set = set.add(3, 3);
        set = set.add(4, 3);
        set = set.add(5, 3);
        set = set.add(6, 3);
        set = set.add(7, 3);
        assertTrue(set instanceof StableDataset);
        set = set.add(7, 5);
        assertTrue(set instanceof PartialDataset);
        assertEquals(3 + 5, set.lastValue());
        assertEquals(8, set.getObservedValues());
        assertEquals(7, set.lastTime());
    }

    @Test
    public void partialAddingWithSameTimeSumsValueCycle() {
        SeriesDataset set = new EmptyDataset(INSTANCE, SERIES, 6);
        set = set.add(1, 1);
        set = set.add(2, 2);
        assertTrue(set instanceof PartialDataset);
        for (int i = 3; i < 100; i++) {
            String msg = "At point " + i;
            set = set.add(i, 2);
            assertEquals(msg, i, set.lastTime());
            assertEquals(msg, 2, set.lastValue());
            set = set.add(i, 3);
            assertEquals(msg, i, set.lastTime());
            assertEquals(msg, 5, set.lastValue());
            assertEquals(msg, 2 + ((i - 2) * 2), set.getObservedValues());
        }
    }

    private static void assertValues(SeriesDataset set, long... values) {
        assertValues(set, true, values);
    }

    private static void assertValues(SeriesDataset set, boolean timesAsWell, long... values) {
        int size = set.size();
        assertEquals(values.length, size);
        long[] points = set.points();
        for (int i = 0; i < size; i++) {
            if (timesAsWell) {
                assertEquals(values[i], points[i*2]);
            }
            assertEquals(values[i], points[i*2+1]);
        }
    }
}
