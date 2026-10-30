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

import java.time.Clock;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link SampleFilter} that changes the sample rate dynamically to meet a given
 * target.
 */
public class AdaptiveSampleFilter extends SampleFilter {

    private static final Logger LOGGER = Logger.getLogger(AdaptiveSampleFilter.class.getName());

    /**
     * The target number of samples.
     */
    private final int targetCount;

    /**
     * The time within which the target count of requests should be matched.
     */
    private final long targetTimeInMillis;

    /**
     * Stores the times (in milliseconds) of previously occurring samples
     * between now and the
     * {@link AdaptiveSampleFilter#timeWindowStartMillis timeWindowStart}.
     */
    private volatile Queue<Long> sampleTimes;

    /**
     * Determines whether to allow the sample rate to change.
     * Starts disabled, but is enabled when the sample rate is accurate enough.
     */
    private boolean allowSampleRateChanging;

    /**
     * The clock to use to get the current time.
     * Absolute time is irrelevant, but this allows relative time to be changed in testing to remove time dependency.
     */
    protected Clock clock;

    /**
     * Initialises the sample filter with a specified initial sample rate and
     * target.
     *
     * @param sampleRate the initial rate at which to sample requests.
     * @param targetCount the target number of requests to match in a time
     * frame.
     * @param targetTimeValue the value for the time frame.
     * @param targetTimeUnit the unit of the value for the time frame.
     */
    public AdaptiveSampleFilter(double sampleRate, Integer targetCount, Integer targetTimeValue, TimeUnit targetTimeUnit) {
        super(sampleRate);
        if (targetCount == null || targetTimeValue == null || targetTimeUnit == null) {
            throw new IllegalArgumentException(getClass().getCanonicalName() + " requires a non null targetCount, targetTimeValue and targetTimeUnit.");
        }
        this.targetCount = targetCount;
        this.targetTimeInMillis = TimeUnit.MILLISECONDS.convert(targetTimeValue, targetTimeUnit);
        this.sampleTimes = new ConcurrentLinkedQueue<>();
        this.sampleRate = sampleRate;
        this.allowSampleRateChanging = false;
        this.clock = Clock.systemUTC();
    }

    @Override
    public boolean sample() {
        boolean sample = super.sample();

        // Regardless of whether it's being sampled, record the time it occurred and add it to the list.
        long occurringTime = clock.millis();
        sampleTimes.add(occurringTime);

        // Remove times that happened more than the target time ago
        long timeWindowStartMillis = occurringTime - targetTimeInMillis;
        synchronized (sampleTimes) {
            for(Long samplePeek = sampleTimes.peek(); samplePeek < timeWindowStartMillis || samplePeek == null; samplePeek = sampleTimes.peek()) {
                sampleTimes.poll();

                // Once the time window has passed, allow the sample rate to start changing.
                allowSampleRateChanging = true;
                LOGGER.finer("Sample rate changing was enabled as the target time has elapsed.");
            }
        }

        // Only make predictions with more than 3 data entries
        if (sampleTimes.size() > 3) {
            double requiredSampleRate = Math.min(1.0, Math.max(0.0, (double)targetCount / sampleTimes.size()));

            if (allowSampleRateChanging) {
                LOGGER.finest(String.format("The sample rate was changed from %4.1f to %4.1f, as in the last time period %d requests were received.", sampleRate, requiredSampleRate, sampleTimes.size()));
                sampleRate = requiredSampleRate;
            }
        }
        
        return sample;
    }

}
