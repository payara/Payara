/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.nucleus.requesttracing.sampling;

import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class AdaptiveSampleFilterTest {

    /*
     * Configure where the sample rate should start, as well as the target values to give to the filter.
     */
    private double startingSampleRate = 1.0;
    private int targetSampleCount;
    private int targetTime = 100;
    private TimeUnit targetTimeUnit = TimeUnit.MILLISECONDS;

    /*
     * Configure how many requests to send over how long. E.g. send {100} requests over {200} {milliseconds}.
     */
    private int sampleCount = 100;
    private int testLength = 200;
    private TimeUnit testLengthUnit = TimeUnit.MILLISECONDS;

    /*
     * Configure the expected sample rate at the end, as well as how far off (as a multiplier) the actual sample rate can be.
     */
    private double expectedSampleRate;
    private final double allowedErrorMargin = 0.05;

    /**
     * Run 100 requests over 200ms. Assert that to meet the target of 25 per 100ms, the resolved sample rate is roughly 0.5.
     */
    @Test
    public void fiftyPercentTest() throws InterruptedException {
        targetSampleCount = 25;
        expectedSampleRate = 0.5;
        runTest();
    }

    /**
     * Run 100 requests over 200ms. Assert that to meet the target of 5 per 100ms, the resolved sample rate is roughly 0.1.
     */
    @Test
    public void tenPercentTest() throws InterruptedException {
        targetSampleCount = 5;
        expectedSampleRate = 0.1;
        runTest();
    }

    /**
     * Run 100 requests over 200ms. Assert that to meet the target of 45 per 100ms, the resolved sample rate is roughly 0.9.
     */
    @Test
    public void ninetyPercentTest() throws InterruptedException {
        targetSampleCount = 45;
        expectedSampleRate = 0.9;
        runTest();
    }

    public void runTest() throws InterruptedException {
        AdaptiveSampleFilter sampleFilter = new AdaptiveSampleFilter(startingSampleRate, targetSampleCount, targetTime, targetTimeUnit);

        // Run the specified number of samples over the specified time period
        long samplePeriodMilliseconds = TimeUnit.MILLISECONDS.convert(testLength, testLengthUnit) / sampleCount;
        long suiteStartTime = System.currentTimeMillis() + 10;
        for (long testStartTime = suiteStartTime; testStartTime < suiteStartTime + TimeUnit.MILLISECONDS.convert(testLength, testLengthUnit); testStartTime += samplePeriodMilliseconds) {
            sampleFilter.clock = Clock.fixed(Instant.ofEpochMilli(testStartTime), ZoneId.systemDefault());
            sampleFilter.sample();
        }

        // Calculate error margin
        double observedErrorMargin = (Math.max(sampleFilter.sampleRate, expectedSampleRate) / Math.min(sampleFilter.sampleRate, expectedSampleRate)) - 1;
        // Check it's less than the allowed error margin
        assertTrue(String.format("The observed sample rate strayed too far from %d%%. Allowed error rate was %4.3f, but was actually %4.3f.",
                        (int) (expectedSampleRate * 100), allowedErrorMargin, observedErrorMargin),
            observedErrorMargin <= allowedErrorMargin);
    }

}