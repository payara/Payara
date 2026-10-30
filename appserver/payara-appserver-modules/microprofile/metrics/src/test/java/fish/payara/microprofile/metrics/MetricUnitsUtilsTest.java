/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.microprofile.metrics;

import static fish.payara.microprofile.metrics.MetricUnitsUtils.scaleToBaseUnit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.junit.Test;

/**
 * Tests correctness of {@link MetricUnitsUtils}.
 *
 * @author Jan Bernitt
 */
public class MetricUnitsUtilsTest {

    @Test(expected = IllegalAccessException.class)
    public void utilCannotBeInstantiated() throws InstantiationException, IllegalAccessException {
        MetricUnitsUtils.class.newInstance();
    }

    @Test
    public void anyBitsUnitHasBytesBaseUnit() {
        assertBaseUnit(MetricUnits.BYTES,
            MetricUnits.BITS, MetricUnits.KILOBITS, MetricUnits.MEGABITS, MetricUnits.GIGABITS,
            MetricUnits.KIBIBITS, MetricUnits.MEBIBITS, MetricUnits.GIBIBITS);
    }

    @Test
    public void anyBytesUnitHasBytesBaseUnit() {
        assertBaseUnit(MetricUnits.BYTES,
                MetricUnits.BYTES, MetricUnits.KILOBYTES, MetricUnits.MEGABYTES, MetricUnits.GIGABYTES);
    }

    @Test
    public void anyTimeUnitHasSecondsBaseUnit() {
        assertBaseUnit(MetricUnits.SECONDS,
                MetricUnits.NANOSECONDS, MetricUnits.MICROSECONDS, MetricUnits.MILLISECONDS,
                MetricUnits.SECONDS, MetricUnits.MINUTES, MetricUnits.HOURS, MetricUnits.DAYS);
    }

    @Test
    public void anyOtherUnitHasItselfAsBaseUnit() {
        assertBaseUnit(MetricUnits.NONE, MetricUnits.NONE);
        assertBaseUnit(MetricUnits.PER_SECOND, MetricUnits.PER_SECOND);
        assertBaseUnit(MetricUnits.PERCENT, MetricUnits.PERCENT);
    }

    @Test
    public void bitsToBytes() {
        assertScalesToBytes(0.25, 2, MetricUnits.BITS);
        assertScalesToBytes(1, 8, MetricUnits.BITS);
        assertScalesToBytes(4, 32, MetricUnits.BITS);
    }

    @Test
    public void kilobitsToBytes() {
        assertScalesToBytes(125, 1, MetricUnits.KILOBITS);
        assertScalesToBytes(25, 0.2, MetricUnits.KILOBITS);
    }

    @Test
    public void megabitsToBytes() {
        assertScalesToBytes(125000, 1, MetricUnits.MEGABITS);
        assertScalesToBytes(500000, 4, MetricUnits.MEGABITS);
        assertScalesToBytes(1250, 0.01, MetricUnits.MEGABITS);
    }

    @Test
    public void gigabitsToBytes() {
        assertScalesToBytes(125000000, 1, MetricUnits.GIGABITS);
        assertScalesToBytes(1250, 0.00001, MetricUnits.GIGABITS);
    }


    @Test
    public void mebibitsToBytes() {
        assertScalesToBytes(128, 1d/1024, MetricUnits.MEBIBITS);
        assertScalesToBytes(128 * 1024, 1, MetricUnits.MEBIBITS);
    }

    @Test
    public void gibibitsToBytes() {
        assertScalesToBytes(128, 1d/1024/1024, MetricUnits.GIBIBITS);
        assertScalesToBytes(128 * 1024 * 1024, 1d, MetricUnits.GIBIBITS);
    }

    @Test
    public void bytesToBytes() {
        assertScalesToBytes(13, 13, MetricUnits.BYTES);
        assertScalesToBytes(42, 42, MetricUnits.BYTES);
        assertScalesToBytes(0.5, 0.5, MetricUnits.BYTES);
    }

    @Test
    public void kilobytesToBytes() {
        assertScalesToBytes(1000, 1, MetricUnits.KILOBYTES);
        assertScalesToBytes(42000, 42, MetricUnits.KILOBYTES);
        assertScalesToBytes(500, 0.5, MetricUnits.KILOBYTES);
    }

    @Test
    public void megabytesToBytes() {
        assertScalesToBytes(1000 * 1000, 1, MetricUnits.MEGABYTES);
        assertScalesToBytes(100 * 1000, 0.1, MetricUnits.MEGABYTES);
    }

    @Test
    public void nanosecondsToSeconds() {
        assertScalesToSeconds(0.0000001d, 1000, MetricUnits.NANOSECONDS);
        assertScalesToSeconds(0.0000421d, 421000, MetricUnits.NANOSECONDS);
    }

    @Test
    public void microsecondsToSeconds() {
        assertScalesToSeconds(0.0000001d, 1, MetricUnits.MICROSECONDS);
        assertScalesToSeconds(0.0000105d, 105, MetricUnits.MICROSECONDS);
    }

    @Test
    public void millisecondsToSeconds() {
        assertScalesToSeconds(0.0001d, 1, MetricUnits.MILLISECONDS);
        assertScalesToSeconds(42.5d, 42500, MetricUnits.MILLISECONDS);
    }

    @Test
    public void secondsToSeconds() {
        assertScalesToSeconds(1, 1, MetricUnits.SECONDS);
        assertScalesToSeconds(42, 42, MetricUnits.SECONDS);
        assertScalesToSeconds(0.5d, 0.5f, MetricUnits.SECONDS);
    }

    @Test
    public void minutesToSeconds() {
        assertScalesToSeconds(60, 1, MetricUnits.MINUTES);
        assertScalesToSeconds(30, 0.5, MetricUnits.MINUTES);
        assertScalesToSeconds(3600, 60, MetricUnits.MINUTES);
    }

    @Test
    public void hoursToSeconds() {
        assertScalesToSeconds(3600, 1, MetricUnits.HOURS);
        assertScalesToSeconds(1800, 0.5, MetricUnits.HOURS);
        assertScalesToSeconds(60 * 60 * 5, 5, MetricUnits.HOURS);
    }

    @Test
    public void daysToSeconds() {
        assertScalesToSeconds(24 * 3600, 1, MetricUnits.DAYS);
        assertScalesToSeconds(12 * 3600, 0.5, MetricUnits.DAYS);
    }

    @Test
    public void othersDoNotScale() {
        assertScalesTo(3, 3, MetricUnits.NONE);
        assertScalesTo(2, 2, MetricUnits.PER_SECOND);
        assertScalesTo(100, 100, MetricUnits.PERCENT);
    }

    @Test
    public void metadataWithoutUnitDoesNotScaleValues() {
        Number value = 42.2d;
        assertSame(value, scaleToBaseUnit(value, Metadata.builder().withName("somename").build()));
        assertSame(value, scaleToBaseUnit(value, Metadata.builder().withName("somename").withUnit(MetricUnits.NONE).build()));
    }

    private static void assertScalesToSeconds(Number expectedSeconds, Number actualValue, String actualUnit) {
        assertBaseUnit(MetricUnits.SECONDS, actualUnit);
        assertScalesTo(expectedSeconds, actualValue, actualUnit);
    }

    private static void assertScalesToBytes(Number expectedBytes, Number actualValue, String actualUnit) {
        assertBaseUnit(MetricUnits.BYTES, actualUnit);
        assertScalesTo(expectedBytes, actualValue, actualUnit);
    }

    private static void assertScalesTo(Number expectedValue, Number actualValue, String actualUnit) {
        assertEquals(expectedValue.doubleValue(),
                MetricUnitsUtils.scaleToBaseUnit(actualValue, actualUnit).doubleValue(), 0.001d);
    }

    private static void assertBaseUnit(String expectedBaseUnit, String... units) {
        for (String unit : units) {
            assertEquals(expectedBaseUnit, MetricUnitsUtils.baseUnit(unit));
        }
    }
}
