/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.monitoring.alert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * Tests formal correctness of the {@link Condition} type, in particular its
 * {@link Condition#isSatisfied(SeriesDataset)} method.
 * 
 * @author Jan Bernitt
 */
public class ConditionTest {

    private static final long ONE_SECOND = 1000L;

    @Test
    public void noneIsAlwaysSatisfied() {
        assertSatisfied(Condition.NONE, 0);
        assertSatisfied(Condition.NONE, -1);
        assertSatisfied(Condition.NONE, 1);
        assertSatisfied(Condition.NONE, 100);
        assertSatisfied(Condition.NONE, 100, 200, 300);
        assertSatisfied(Condition.NONE, 1, 2, 3);
        assertSatisfied(Condition.NONE, 0, 0, 0, 0, 0);
    }

    @Test
    public void emptySetNeverSatisfied() {
        SeriesDataset empty = new EmptyDataset("instance", new Series("Series"), 3);
        for (Operator op : Operator.values()) {
            assertFalse(new Condition(op, 5).isSatisfied(empty));
        }
    }

    @Test
    public void lessThanThreshold() {
        Condition lt5 = new Condition(Operator.LT, 5);
        assertSatisfied(lt5, 1);
        assertSatisfied(lt5, 1, 2);
        assertSatisfied(lt5, 1, 2, 4);
        assertNotSatisfied(lt5, 1, 2, 4, 5);
        assertNotSatisfied(lt5, 5);
        assertSatisfied(lt5, 5, 4);
        assertSatisfied(lt5, -1);
        assertSatisfied(lt5, 0, 0, 0, 0, 0);
        assertFalse(lt5.isForLastPresent());
        assertEquals("value < 5", lt5.toString());
    }

    @Test
    public void lessThanOrEqualThreshold() {
        Condition ltOrEq5 = new Condition(Operator.LE, 5);
        assertSatisfied(ltOrEq5, 1);
        assertSatisfied(ltOrEq5, 1, 2);
        assertSatisfied(ltOrEq5, 1, 2, 4);
        assertSatisfied(ltOrEq5, 1, 2, 4, 5);
        assertNotSatisfied(ltOrEq5, 1, 2, 4, 6);
        assertNotSatisfied(ltOrEq5, 6);
        assertSatisfied(ltOrEq5, 6, 5);
        assertSatisfied(ltOrEq5, -1);
        assertSatisfied(ltOrEq5, 0, 0, 0, 0, 0);
        assertFalse(ltOrEq5.isForLastPresent());
        assertEquals("value <= 5", ltOrEq5.toString());
    }

    @Test
    public void equalThreshold() {
        Condition eq5 = new Condition(Operator.EQ, 5);
        assertNotSatisfied(eq5, 1);
        assertNotSatisfied(eq5, 1, 2);
        assertSatisfied(eq5, 1, 2, 5);
        assertNotSatisfied(eq5, 1, 2, 5, 4);
        assertNotSatisfied(eq5, -1);
        assertNotSatisfied(eq5, 0, 0, 0, 0, 0);
        assertSatisfied(eq5, 5, 5, 5, 5, 5);
        assertFalse(eq5.isForLastPresent());
        assertEquals("value = 5", eq5.toString());
    }

    @Test
    public void greaterThanThreshold() {
        Condition gt5 = new Condition(Operator.GT, 5);
        assertNotSatisfied(gt5, 1);
        assertNotSatisfied(gt5, 1, 2);
        assertNotSatisfied(gt5, 1, 2, 5);
        assertSatisfied(gt5, 1, 2, 5, 6);
        assertNotSatisfied(gt5, 5);
        assertSatisfied(gt5, 6);
        assertNotSatisfied(gt5, -1);
        assertNotSatisfied(gt5, 0, 0, 0, 0, 0);
        assertFalse(gt5.isForLastPresent());
        assertEquals("value > 5", gt5.toString());
    }

    @Test
    public void greaterThanOrEqualThreshold() {
        Condition ge5 = new Condition(Operator.GE, 5);
        assertNotSatisfied(ge5, 1);
        assertNotSatisfied(ge5, 1, 2);
        assertSatisfied(ge5, 1, 2, 5);
        assertSatisfied(ge5, 1, 2, 5, 6);
        assertNotSatisfied(ge5, 4);
        assertSatisfied(ge5, 5);
        assertSatisfied(ge5, 6);
        assertNotSatisfied(ge5, -1);
        assertNotSatisfied(ge5, 0, 0, 0, 0, 0);
        assertSatisfied(ge5, 5, 5, 5, 5, 5);
        assertNotSatisfied(ge5, 4, 4, 4, 4, 4);
        assertFalse(ge5.isForLastPresent());
        assertEquals("value >= 5", ge5.toString());
    }

    @Test
    public void greaterThanThresholdForLastTimes() {
        Condition gt5for3x = new Condition(Operator.GT, 5, 3, false);
        assertNotSatisfied(gt5for3x, 6);
        assertNotSatisfied(gt5for3x, 6, 7);
        assertNotSatisfied(gt5for3x, 6, 7, 5);
        assertSatisfied(gt5for3x, 6, 7, 8);
        assertNotSatisfied(gt5for3x, 6, 7, 5, 8, 9);
        assertSatisfied(gt5for3x, 6, 7, 5, 8, 9, 6);
        assertNotSatisfied(gt5for3x, 0, 0, 0, 0, 0);
        assertNotSatisfied(gt5for3x, 5, 5, 5, 5, 5);
        assertSatisfied(gt5for3x, 6, 6, 6, 6, 6);
        assertTrue(gt5for3x.isForLastPresent());
        assertEquals("value > 5 for last 3x", gt5for3x.toString());
    }

    @Test
    public void greaterThanThresholdForLastTimesOnAverage() {
        Condition avgGt5for3x = new Condition(Operator.GT, 5, 3, true);
        assertSatisfied(avgGt5for3x, 6, 7, 5);
        assertNotSatisfied(avgGt5for3x, 6, 7);
        assertNotSatisfied(avgGt5for3x, 6);
        assertSatisfied(avgGt5for3x, 6, 7, 8);
        assertSatisfied(avgGt5for3x, 6, 7, 5, 8, 9);
        assertSatisfied(avgGt5for3x, 6, 7, 5, 8, 9, 6);
        assertNotSatisfied(avgGt5for3x, 6, 4, 6, 5);
        assertNotSatisfied(avgGt5for3x, 0, 0, 0, 0, 0);
        assertNotSatisfied(avgGt5for3x, 5, 5, 5, 5, 5);
        assertSatisfied(avgGt5for3x, 6, 6, 6, 6, 6);
        assertTrue(avgGt5for3x.isForLastPresent());
        assertEquals("value > 5 for average of last 3x", avgGt5for3x.toString());
    }

    @Test
    public void greaterThanThresholdForLastMillis() {
        Condition gt5for3sec = new Condition(Operator.GT, 5, 3000L, false);
        assertNotSatisfied(gt5for3sec, 6);
        assertNotSatisfied(gt5for3sec, 6, 7);
        assertSatisfied(gt5for3sec, 6, 7, 8, 6);
        assertNotSatisfied(gt5for3sec, 6, 7, 5, 6);
        assertNotSatisfied(gt5for3sec, 6, 7, 5, 8, 9);
        assertNotSatisfied(gt5for3sec, 6, 7, 5, 8, 9, 6);
        assertSatisfied(gt5for3sec, 6, 7, 5, 8, 9, 6, 7);
        assertSatisfied(gt5for3sec, 6, 7, 4, 6, 6, 6, 6);
        assertNotSatisfied(gt5for3sec, 0, 0, 0, 0, 0);
        assertNotSatisfied(gt5for3sec, 5, 5, 5, 5, 5);
        assertSatisfied(gt5for3sec, 6, 6, 6, 6, 6);
        assertTrue(gt5for3sec.isForLastPresent());
        assertEquals("value > 5 for last 3000ms", gt5for3sec.toString());
    }

    @Test
    public void greaterThanThresholdForLastMillisOnAverage() {
        Condition avgOf3secGt5 = new Condition(Operator.GT, 5, 3000L, true);
        assertNotSatisfied(avgOf3secGt5, 6);
        assertNotSatisfied(avgOf3secGt5, 6, 7);
        assertSatisfied(avgOf3secGt5, 6, 7, 8, 6);
        assertSatisfied(avgOf3secGt5, 6, 7, 5, 6);
        assertSatisfied(avgOf3secGt5, 6, 7, 5, 8, 9);
        assertSatisfied(avgOf3secGt5, 6, 7, 5, 8, 9, 6);
        assertSatisfied(avgOf3secGt5, 6, 7, 5, 8, 9, 6, 7);
        assertNotSatisfied(avgOf3secGt5, 6, 7, 4, 6);
        assertSatisfied(avgOf3secGt5, 6, 7, 4, 16, 6);
        assertSatisfied(avgOf3secGt5, 6, 7, 4, 16, 6, 6);
        assertNotSatisfied(avgOf3secGt5, 0, 0, 0, 0, 0);
        assertNotSatisfied(avgOf3secGt5, 5, 5, 5, 5, 5);
        assertSatisfied(avgOf3secGt5, 6, 6, 6, 6, 6);
        assertTrue(avgOf3secGt5.isForLastPresent());
        assertEquals("value > 5 for average of last 3000ms", avgOf3secGt5.toString());
    }

    @Test
    public void trueForAnyInLastTimes() {
        Condition anyGt5 = new Condition(Operator.GT, 5, 0, false);
        assertNotSatisfied(anyGt5, 5);
        assertSatisfied(anyGt5, 6);
        assertSatisfied(anyGt5, 6, 5);
        assertSatisfied(anyGt5, 5, 6);
        assertSatisfied(anyGt5, 5, 6, 5);
        assertSatisfied(anyGt5, 5, 6, 5, 4);
        assertNotSatisfied(anyGt5, 5, 5, 5, 4);
        assertEquals("value > 5 in sample", anyGt5.toString());
    }

    @Test
    public void trueForAnyInLast3Times() {
        Condition anyGt5in3x = new Condition(Operator.GT, 5, -3, false);
        assertNotSatisfied(anyGt5in3x, 5);
        assertSatisfied(anyGt5in3x, 6);
        assertSatisfied(anyGt5in3x, 6, 5);
        assertSatisfied(anyGt5in3x, 5, 6);
        assertSatisfied(anyGt5in3x, 5, 6, 5);
        assertSatisfied(anyGt5in3x, 5, 6, 5, 4);
        assertNotSatisfied(anyGt5in3x, 6, 5, 5, 4);
        assertNotSatisfied(anyGt5in3x, 5, 5, 5, 4);
        assertEquals("value > 5 in last 3x", anyGt5in3x.toString());
    }

    @Test
    public void trueForAnyInLast3Seconds() {
        Condition anyGt5in3sec = new Condition(Operator.GT, 5, -3000L, false);
        assertSatisfied(anyGt5in3sec, 6);
        assertNotSatisfied(anyGt5in3sec, 5);
        assertSatisfied(anyGt5in3sec, 6, 5);
        assertSatisfied(anyGt5in3sec, 5, 6);
        assertSatisfied(anyGt5in3sec, 5, 6, 5);
        assertSatisfied(anyGt5in3sec, 5, 6, 5, 4);
        assertNotSatisfied(anyGt5in3sec, 6, 5, 5, 5, 4);
        assertNotSatisfied(anyGt5in3sec, 5, 5, 5, 4);
        assertEquals("value > 5 in last 3000ms", anyGt5in3sec.toString());
    }

    private static void assertSatisfied(Condition c, long... points) {
        assertTrue(c.isSatisfied(createSet(ONE_SECOND, points)));
        assertBasicProperties(c);
    }

    private static void assertNotSatisfied(Condition c, long... points) {
        assertFalse(c.isSatisfied(createSet(ONE_SECOND, points)));
        assertBasicProperties(c);
    }

    public static void assertBasicProperties(Condition c) {
        assertEquals(c, c);
        assertNotEquals(null, c);
        Condition other = new Condition(Operator.EQ, 111);
        assertNotEquals(other, c);
        assertEquals(c.hashCode(), c.hashCode());
        assertNotEquals(other.hashCode(), c.hashCode());
        assertEquals(c.toString(), c.toString());
        assertNotEquals(other.toString(), c.toString());
    }

    private static SeriesDataset createSet(long timeBetweenPoints, long... points) {
        long time = 0L;
        SeriesDataset set = new EmptyDataset("Instance", new Series("Metric"), points.length);
        for (int i = 0; i < points.length; i++) {
            time += timeBetweenPoints;
            set = set.add(time, points[i]);
        }
        return set;
    }
}
