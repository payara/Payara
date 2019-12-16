package fish.payara.monitoring.alert;

import static org.junit.Assert.assertFalse;
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
    }

    private static void assertSatisfied(Condition c, long... points) {
        assertTrue(c.isSatisfied(createSet(ONE_SECOND, points)));
    }

    private static void assertNotSatisfied(Condition c, long... points) {
        assertFalse(c.isSatisfied(createSet(ONE_SECOND, points)));
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
