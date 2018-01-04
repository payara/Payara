/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 * 
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 * 
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 * 
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */

package fish.payara.microprofile.metrics.impl;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import org.eclipse.microprofile.metrics.Snapshot;

/**
 * A statistical snapshot of a {@link WeightedSnapshot}.
 */
public class WeightedSnapshot extends Snapshot {


    private final long[] values;
    private final double[] normWeights;
    private final double[] quantiles;

    /**
     * Create a new {@link Snapshot} with the given values.
     *
     * @param values an unordered set of values in the reservoir
     */
    public WeightedSnapshot(Collection<WeightedSample> values) {
        final WeightedSample[] copy = values.toArray(new WeightedSample[]{});

        Arrays.sort(copy, Comparator.comparing(w -> w.value));

        this.values = new long[copy.length];
        this.normWeights = new double[copy.length];
        this.quantiles = new double[copy.length];

        double sumWeight = 0;
        for (WeightedSample sample : copy) {
            sumWeight += sample.weight;
        }

        for (int i = 0; i < copy.length; i++) {
            this.values[i] = copy[i].value;
            this.normWeights[i] = copy[i].weight / sumWeight;
        }

        for (int i = 1; i < copy.length; i++) {
            this.quantiles[i] = this.quantiles[i - 1] + this.normWeights[i - 1];
        }
    }

    /**
     * Returns the value at the given quantile.
     *
     * @param quantile a given quantile, in {@code [0..1]}
     * @return the value in the distribution at {@code quantile}
     */
    @Override
    public double getValue(double quantile) {
        if (quantile < 0.0 || quantile > 1.0 || Double.isNaN(quantile)) {
            throw new IllegalArgumentException(quantile + " is not in [0..1]");
        }

        if (values.length == 0) {
            return 0.0;
        }

        int posx = Arrays.binarySearch(quantiles, quantile);
        if (posx < 0) {
            posx = ((-posx) - 1) - 1;
        }

        if (posx < 1) {
            return values[0];
        }

        if (posx >= values.length) {
            return values[values.length - 1];
        }

        return values[posx];
    }

    /**
     * Returns the number of values in the snapshot.
     *
     * @return the number of values
     */
    @Override
    public int size() {
        return values.length;
    }

    /**
     * Returns the entire set of values in the snapshot.
     *
     * @return the entire set of values
     */
    @Override
    public long[] getValues() {
        return Arrays.copyOf(values, values.length);
    }

    /**
     * Returns the highest value in the snapshot.
     *
     * @return the highest value
     */
    @Override
    public long getMax() {
        if (values.length == 0) {
            return 0;
        }
        return values[values.length - 1];
    }

    /**
     * Returns the lowest value in the snapshot.
     *
     * @return the lowest value
     */
    @Override
    public long getMin() {
        if (values.length == 0) {
            return 0;
        }
        return values[0];
    }

    /**
     * Returns the weighted arithmetic mean of the values in the snapshot.
     *
     * @return the weighted arithmetic mean
     */
    @Override
    public double getMean() {
        if (values.length == 0) {
            return 0;
        }

        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += values[i] * normWeights[i];
        }
        return sum;
    }

    /**
     * Returns the weighted standard deviation of the values in the snapshot.
     *
     * @return the weighted standard deviation value
     */
    @Override
    public double getStdDev() {
        // two-pass algorithm for variance, avoids numeric overflow

        if (values.length <= 1) {
            return 0;
        }

        final double mean = getMean();
        double variance = 0;

        for (int i = 0; i < values.length; i++) {
            final double diff = values[i] - mean;
            variance += normWeights[i] * diff * diff;
        }

        return Math.sqrt(variance);
    }

    /**
     * Writes the values of the snapshot to the given stream.
     *
     * @param output an output stream
     */
    @Override
    public void dump(OutputStream output) {
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(output, UTF_8))) {
            for (long value : values) {
                out.printf("%d%n", value);
            }
        }
    }
    /**
     * A single sample item with value and its weights for
     * {@link WeightedSnapshot}.
     */
    public static class WeightedSample {
        
        public final long value;
        public final double weight;
        
        public WeightedSample(long value, double weight) {
            this.value = value;
            this.weight = weight;
        }
    }
}
