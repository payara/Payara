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

import static fish.payara.monitoring.alert.Alert.Level.AMBER;
import static fish.payara.monitoring.alert.Alert.Level.GREEN;
import static fish.payara.monitoring.alert.Alert.Level.RED;
import static fish.payara.monitoring.alert.Alert.Level.WHITE;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;


import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.alert.Condition.Operator;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.model.SeriesLookup;
import fish.payara.monitoring.model.Unit;

/**
 * Tests some example cases to check basic correctness of {@link Watch} logic.
 *
 * @author Jan Bernitt
 */
public class WatchTest {

    /**
     * An example with {@link Operator#LT} (triggers when value is below limit not above).
     */
    private final Watch livelinessUp = new Watch("Liveliness UP", 
            new Metric(new Series("ns:health LivelinessUp"), Unit.PERCENT))
            .red(-60, null, false, null, null, false)
            .amber(-100, null, false, null, null, false)
            .green(100, null, false, null, null, false);

    /**
     * An example with {@link Condition#onAverage}
     */
    private final Watch heapUsage = new Watch("Heap Usage", 
            new Metric(new Series("ns:health HeapUsage"), Unit.PERCENT))
            .red(80, 5, true, 75L, 5, true)
            .amber(60, 5, true, 55L, 5, true)
            .green(0, null, false, null, null, false);

    /**
     * An example without {@link Condition#onAverage} and without a {@link Level#GREEN} state.
     */
    private final Watch cpuUsage = new Watch("CPU Usage", 
            new Metric(new Series("ns:health CpuUsage"), Unit.PERCENT))
            .red(80, 5, false, 75L, 5, false)
            .amber(60, 5, false, 55L, 5, false);

    @Test
    public void livelinessUpIsWhiteOnEmptyDataset() {
        assertValuesCauseStates(livelinessUp, new long[0], //
                0, WHITE);
    }

    @Test
    public void livelinessUpIsRedBelow60Percent() {
        assertValuesCauseStates(livelinessUp, new long[] {50, 50}, //
                1, RED, RED);
    }

    @Test
    public void livelinessUpIsAmberAbove60AndBelow100Percent() {
        assertValuesCauseStates(livelinessUp, new long[] {60, 70}, //
                1, AMBER, AMBER);
    }

    @Test
    public void livelinessUpSwitchesBetweenAmberAndRed() {
        assertValuesCauseStates(livelinessUp, new long[] {50, 70, 59}, //
                1, RED, AMBER, RED);
    }

    @Test
    public void livelinessUpSwitchesBetweenGreenAmberAndRed() {
        assertValuesCauseStates(livelinessUp, new long[] {100, 50, 70, 59, 100, 80}, //
                2, GREEN, RED, AMBER, RED, GREEN, AMBER);
    }

    @Test
    public void heapUsageIsWhiteOnEmptyDataset() {
        assertValuesCauseStates(heapUsage, new long[0], //
                0, WHITE);
    }

    @Test
    public void heapUsageIsAmberWhenAverageOf5ValuesIsAbove60Percent() {
        assertValuesCauseStates(heapUsage, new long[] {70, 75, 80, 90, 70}, //
                1, GREEN, GREEN, GREEN, GREEN, AMBER);
    }

    @Test
    public void heapUsageBecomesGreenWhenAverageOf5ValuesIsBelow55Percent() {
        assertValuesCauseStates(heapUsage, new long[] {70, 75, 80, 90, 70, 50, 50, 50, 50, 55}, //
                1, GREEN, GREEN, GREEN, GREEN, AMBER, AMBER, AMBER, AMBER, GREEN, GREEN);
    }

    @Test
    public void heapUsageIsAmberWhenAverageOf5ValuesIsAbove80Percent() {
        assertValuesCauseStates(heapUsage, new long[] {70, 75, 80, 90, 95}, //
                1, GREEN, GREEN, GREEN, GREEN, RED);
    }

    @Test
    public void heapUsageBecomesAmberWhenAverageOf5ValuesIsBelow75Percent() {
        assertValuesCauseStates(heapUsage, new long[] {70, 75, 80, 90, 95, 70, 70, 70, 70, 70}, //
                1, GREEN, GREEN, GREEN, GREEN, RED, RED, RED, RED, RED, AMBER);
    }

    @Test
    public void cpuUsageIsWhiteOnEmptyDataset() {
        assertValuesCauseStates(cpuUsage, new long[0], //
                0, WHITE);
    }

    @Test
    public void cpuUsageIsAmberWhen5ConsecutiveValuesAreAbove60Percent() {
        assertValuesCauseStates(cpuUsage, new long[] {70, 75, 80, 90, 70}, //
                1, WHITE, WHITE, WHITE, WHITE, AMBER);
    }

    @Test
    public void cpuUsageBecomesGreenWhen5ConsecutiveValuesAreBelow55Percent() {
        assertValuesCauseStates(cpuUsage, new long[] {70, 75, 80, 90, 70, 50, 50, 50, 50, 54}, //
                1, WHITE, WHITE, WHITE, WHITE, AMBER, AMBER, AMBER, AMBER, AMBER, WHITE);
    }

    @Test
    public void cpuUsageIsAmberWhen5ConsecutiveValuesAreAbove80Percent() {
        assertValuesCauseStates(cpuUsage, new long[] {70, 75, 80, 90, 95, 81, 85, 82}, //
                1, WHITE, WHITE, WHITE, WHITE, AMBER, AMBER, AMBER, RED);
    }

    @Test
    public void cpuUsageBecomesAmberWhen5ConsecutiveValuesAreBelow75Percent() {
        assertValuesCauseStates(cpuUsage, new long[] {70, 75, 81, 90, 95, 81, 90, 70, 70, 70, 70, 70}, //
                1, WHITE, WHITE, WHITE, WHITE, AMBER, AMBER, RED, RED, RED, RED, RED, AMBER);
    }

    @Test
    public void watchObjectProperties() {
        assertEquals(livelinessUp, livelinessUp);
        assertEquals(heapUsage, heapUsage);
        assertNotEquals(livelinessUp, heapUsage);
        assertEquals(livelinessUp.toString(), livelinessUp.toString());
        assertEquals(heapUsage.toString(), heapUsage.toString());
        assertNotEquals(heapUsage, livelinessUp);
    }

    @Test
    public void equalConditionsReturnsSameWatchInstance() {
        assertSame(heapUsage, heapUsage
                .red(80, 5, true, 75L, 5, true)
                .amber(60, 5, true, 55L, 5, true)
                .green(0, null, false, null, null, false));
    }

    @Test
    public void unequalConditionsReturnsDifferentWatchInstance() {
        assertNotSame(heapUsage, heapUsage.red(70, 5, true, 75L, 5, true));
        assertNotSame(heapUsage, heapUsage.red(80, 4, true, 75L, 5, true));
        assertNotSame(heapUsage, heapUsage.red(80, 5, false, 75L, 5, true));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, 74L, 5, true));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, null, 5, true));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, 75L, 5L, true));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, 75L, 5, false));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, null, null, false));
        assertNotSame(heapUsage, heapUsage.red(80, 5, true, 75L, null, false));
        assertNotSame(heapUsage, heapUsage.red(80, null, true, 75L, 5, true));
    }

    private static void assertValuesCauseStates(Watch watch, long[] actualValues, int expectedAlerts, Level... expectedLevels) {
        SeriesDataset set = new EmptyDataset("server", watch.watched.series, actualValues.length);
        if (actualValues.length == 0) {
            SeriesDataset empty = set;
            watch.check((series, instances) -> singletonList(empty));
            assertEquals(expectedLevels[0], watch.state(empty).level);
            return;
        }
        long time = 0;
        List<SeriesDataset> matches = new ArrayList<>();
        SeriesLookup matchLookup = (series, instances) -> matches;
        Level lastLevel = WHITE;
        SeriesDataset alertSet = null;
        List<Alert> totalAlerts = new ArrayList<>();
        for (int i = 0; i < actualValues.length; i++) {
            time += 1000;
            set = set.add(time, actualValues[i]);
            matches.clear();
            matches.add(set);
            List<Alert> alerts = watch.check(matchLookup);
            if (alertSet == null && alerts.size() > 0) {
                alertSet = set;
            }
            totalAlerts.addAll(alerts);
            Level expectedLevel = expectedLevels[i];
            int n = i + 1;
            if (lastLevel.isLessSevereThan(AMBER) && !expectedLevel.isLessSevereThan(AMBER)) {
                assertEquals(n + ". value unexpectedly caused no alerts", 1, alerts.size());
                assertEquals(n + ". value had unexpected level", expectedLevel, alerts.get(0).getLevel());
            } else {
                assertEquals(n + ". value unexpectedly caused alerts", 0, alerts.size());
                assertEquals(n + ". value had unexpected level", expectedLevel, watch.state(set).level);
            }
            lastLevel = expectedLevel;
        }
        assertEquals(expectedAlerts, totalAlerts.size());
        // when stopped all alerts should stop and even a set previously causing alerts no longer does so
        watch.stop();
        if (alertSet != null) {
            matches.clear();
            matches.add(alertSet);
            assertEquals(0, watch.check(matchLookup).size());
        }
        for (Alert a : totalAlerts) {
            assertTrue(a.isStopped());
            assertTrue(a.getLevel().isLessSevereThan(AMBER));
        }
    }
}
