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
package fish.payara.nucleus.requesttracing.sampling;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class SampleFilterTest {

    private int sampleCount = 100000;
    private double expectedSampleRate;
    private final double allowedErrorMargin = 0.02;

    @Test
    public void OneHundredPercentTest() {
        SampleFilter sampleFilter = new SampleFilter();

        for(int i = 0; i < sampleCount; i ++) {
            assertTrue("No samples should fail when a sample rate of 1 is provided.", sampleFilter.sample());
        }
    }

    @Test
    public void zeroPercentTest() {
        SampleFilter sampleFilter = new SampleFilter(0);

        for(int i = 0; i < sampleCount; i ++) {
            assertFalse("All should fail when a sample rate of 0 is provided.", sampleFilter.sample());
        }
    }

    @Test
    public void twentyPercentTest() {
        expectedSampleRate = 0.2;
        runErrorMarginTest();
    }

    @Test
    public void fiftyPercentTest() {
        expectedSampleRate = 0.5;
        runErrorMarginTest();
    }

    @Test
    public void eightyPercentTest() {
        expectedSampleRate = 0.8;
        runErrorMarginTest();
    }

    public void runErrorMarginTest() {
        SampleFilter sampleFilter = new SampleFilter(expectedSampleRate);

        int passedSamples = 0;
        for(int i = 0; i < sampleCount; i++) {
            if (sampleFilter.sample()) {
                passedSamples ++;
            }
        }
        double observedErrorMargin = (double)(Math.max(passedSamples, expectedSampleRate * sampleCount) / Math.min(passedSamples, expectedSampleRate * sampleCount)) - 1;
        assertTrue(String.format("The observed sample rate strayed too far from %d%%. Allowed error rate was %4.3f, but was actually %4.3f.",
                        (int) (expectedSampleRate * 100), allowedErrorMargin, observedErrorMargin),
            observedErrorMargin <= allowedErrorMargin);
    }

}