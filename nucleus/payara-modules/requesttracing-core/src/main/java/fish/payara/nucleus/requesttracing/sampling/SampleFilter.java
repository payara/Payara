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

import java.util.Random;
import java.util.logging.Logger;

/**
 * Determines whether to sample or not, given a specified sample rate.
 */
public class SampleFilter {

    private static final Logger LOGGER = Logger.getLogger(SampleFilter.class.getName());
    
    /**
     * The rate at which to sample requests.
     */
    protected double sampleRate;
    private volatile Random random;

    /**
     * Initialises the sample filter.
     *
     * @param sampleRate the rate, between 0 and 1, at which to sample requests.
     */
    public SampleFilter(double sampleRate) {
        this.random = new Random();
        this.sampleRate = sampleRate;
    }

    /**
     * Initialises the sample filter with a sample rate of 1. This means that
     * every request will be sampled.
     */
    public SampleFilter() {
        this(1.0);
    }

    /**
     * Whether to sample or not.
     *
     * @return true if the request should be sampled, or false otherwise.
     */
    public boolean sample() {
        double randomDouble = random.nextDouble();
        boolean sample = randomDouble < sampleRate;
        if (sample) {
            LOGGER.finest(String.format("Request was traced as sample rate was: %4.1f and the random float was: %4.1f", sampleRate, randomDouble));
        } else {
            LOGGER.finest(String.format("Request wasn't traced as sample rate was: %4.1f but the random float was: %4.1f", sampleRate, randomDouble));
        }
        return sample;
    }

}
