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
import static fish.payara.monitoring.alert.Alert.Level.RED;
import static fish.payara.monitoring.alert.Alert.Level.WHITE;
import static fish.payara.monitoring.alert.Condition.Operator.GT;
import static fish.payara.monitoring.alert.Condition.Operator.LT;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;
import fish.payara.monitoring.model.SeriesLookup;

/**
 * Tests formal correctness of {@link Circumstance} logic.
 *
 * @author Jan Bernitt
 */
public class CircumstanceTest implements SeriesLookup {

    @Override
    public List<SeriesDataset> selectSeries(Series series, String... instances) {
        fail("Unexpected call to method");
        return null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void onlyUnspecifiedCanHaveLevelWhite() {
        assertNotNull(new Circumstance(WHITE, new Condition(GT, 5)));
    }

    @Test
    public void unspecifiedDoesNeverStart() {
        Circumstance never = Circumstance.UNSPECIFIED;
        assertTrue(never.isUnspecified());
        assertFalse(never.starts(createSet(), this));
        assertFalse(never.starts(createSet(1), this));
        assertFalse(never.starts(createSet(0), this));
        assertFalse(never.starts(createSet(0, 0, 0, 0), this));
        assertFalse(never.starts(createSet(123, 124, 125, 126), this));
    }

    @Test
    public void startOnlyStartsWhenStartIsSatisfied() {
        Circumstance simpleStart = new Circumstance(AMBER, new Condition(GT, 5));
        assertFalse(simpleStart.isUnspecified());
        assertFalse(simpleStart.starts(createSet(5), this));
        assertTrue(simpleStart.starts(createSet(5, 6, 7), this));
    }

    @Test
    public void startOnlyStopsWhenStartIsNoLongerSatisfied() {
        Circumstance simpleStart = new Circumstance(AMBER, new Condition(GT, 5));
        assertFalse(simpleStart.stops(createSet(5, 6, 7)));
        assertTrue(simpleStart.stops(createSet(5,6,7,5)));
    }

    @Test
    public void startAndStopStartsWhenStartIsSatisfied() {
        Circumstance startStop = new Circumstance(AMBER, new Condition(GT, 5), new Condition(LT, 5));
        assertFalse(startStop.isUnspecified());
        assertFalse(startStop.starts(createSet(5), this));
        assertTrue(startStop.starts(createSet(5, 6, 7), this));
    }

    @Test
    public void startAndStopStopsWhenStopIsSatisfied() {
        Circumstance startStop = new Circumstance(AMBER, new Condition(GT, 5), new Condition(LT, 5));
        assertFalse(startStop.stops(createSet(5, 6, 7, 5)));
        assertTrue(startStop.stops(createSet(5, 6, 7, 5, 4)));
    }

    @Test
    public void startWithSupressStartsWhenStartIsSatisfiedUnlessSupressIsSatisfied() {
        Circumstance startWithSurpress = new Circumstance(RED, new Condition(GT, 5))
                .suppressedWhen(new Metric(new Series("Surpressing")), new Condition(GT, 13));
        assertFalse(startWithSurpress.starts(createSet(6), (series, instances) -> singletonList(createSet(14))));
        assertTrue(startWithSurpress.starts(createSet(6), (series, instances) -> singletonList(createSet(13))));
    }

    @Test
    public void toStringIsReadable() {
        assertEquals("AMBER: value > 5", //
                new Circumstance(AMBER, new Condition(GT, 5)).toString());
        assertEquals("AMBER: value > 5 for last 3x", //
                new Circumstance(AMBER, new Condition(GT, 5).forLastTimes(3)).toString());
        assertEquals("AMBER: value > 5 for last 3000ms", //
                new Circumstance(AMBER, new Condition(GT, 5).forLastMillis(3000)).toString());
        assertEquals("AMBER: value > 5 for average of last 3x", //
                new Circumstance(AMBER, new Condition(GT, 5).forLastTimes(3).onAverage()).toString());
        assertEquals("AMBER: value > 5 for average of last 3000ms", //
                new Circumstance(AMBER, new Condition(GT, 5).forLastMillis(3000).onAverage()).toString());
        assertEquals("RED: value > 5 until value < 5", //
                new Circumstance(RED, new Condition(GT, 5), new Condition(LT, 5)).toString());
        assertEquals("RED: value > 5 unless Surpressing value > 13", //
                new Circumstance(RED, new Condition(GT, 5))
                .suppressedWhen(new Metric(new Series("Surpressing")), new Condition(GT, 13)).toString());
    }

    private static SeriesDataset createSet(long... values) {
        SeriesDataset set = new EmptyDataset("instance", new Series("Series"), values.length);
        long time = 0;
        for (int i = 0; i < values.length; i++) {
            time += 1000;
            set = set.add(time, values[i]);
        }
        return set;
    }
}
