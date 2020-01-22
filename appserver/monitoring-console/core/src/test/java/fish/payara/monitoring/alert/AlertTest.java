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

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import fish.payara.monitoring.alert.Alert.Frame;
import fish.payara.monitoring.alert.Alert.Level;
import fish.payara.monitoring.model.EmptyDataset;
import fish.payara.monitoring.model.Metric;
import fish.payara.monitoring.model.Series;
import fish.payara.monitoring.model.SeriesDataset;

/**
 * Tests some of the formal correctness of {@link Alert} logic.
 *
 * @author Jan Bernitt
 */
public class AlertTest {

    private final Series series = new Series("Series");

    @Test
    public void compactionDoesLimitFrames() {
        Alert a = new Alert(new Watch("name", new Metric(series)));
        long startTime = 0;
        for (int i = 0; i < Alert.MAX_FRAMES + 10; i++) {
            SeriesDataset cause = createDatasetWithPoints(i);
            Level level = i % 2 == 0 ? Level.AMBER : Level.RED;
            a.addTransition(level, cause, emptyList());
            if (i == 0) {
                startTime = a.getStartTime();
            }
            assertEquals(startTime, a.getStartTime()); // still same start time
            assertEquals(cause.lastTime(), a.getEndFrame().start);
            assertEquals(level, a.getLevel());
            assertFramesAreConnectedInTime(a);
            assertTrue("Too many frames at " + i, a.getFrameCount() <= Alert.MAX_FRAMES);
        }
    }

    private static void assertFramesAreConnectedInTime(Alert a) {
        Frame last = null;
        for (Frame f : a) {
            if (last != null) {
                assertEquals(last.getEnd(), f.start);
                assertNotSame(last.level, f.level);
            }
            last = f;
        }
    }

    private SeriesDataset createDatasetWithPoints(int n) {
        SeriesDataset data = new EmptyDataset("instance", series, n);
        long time = 0;
        for (int j = 0; j < n; j++) {
            time += 1000; // 1 sec
            data = data.add(time, j);
        }
        return data;
    }
}
