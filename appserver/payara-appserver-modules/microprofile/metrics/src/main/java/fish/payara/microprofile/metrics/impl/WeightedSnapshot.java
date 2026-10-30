/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * *****************************************************************************
 * Copyright 2010-2013 Coda Hale and Yammer, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    
    private ConfigurationProperties configurationProperties;

    /**
     * Create a new {@link Snapshot} with the given values.
     *
     * @param values an unordered set of values in the reservoir
     */
    public WeightedSnapshot(Collection<WeightedSample> values, ConfigurationProperties configurationProperties) {
        this(values);
        this.configurationProperties = configurationProperties;
    }
    
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
            this.normWeights[i] = sumWeight == 0d ? 0d : copy[i].weight / sumWeight;
        }

        for (int i = 1; i < copy.length; i++) {
            this.quantiles[i] = this.quantiles[i - 1] + this.normWeights[i - 1];
        }
    }



    /**
     * Returns the number of values in the snapshot.
     *
     * @return the number of values
     */
    @Override
    public long size() {
        return values.length;
    }


    /**
     * Returns the highest value in the snapshot.
     *
     * @return the highest value
     */
    @Override
    public double getMax() {
        if (values.length == 0) {
            return 0;
        }
        return values[values.length - 1];
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

    @Override
    public PercentileValue[] percentileValues() {
        PercentileValue[] percentileValues = null;
        if (configurationProperties != null) {
            Double[] percentiles = configurationProperties.percentileValues();
            percentileValues = new PercentileValue[percentiles.length];
            for (int i = 0; i < percentiles.length; i++) {
                percentileValues[i] = new PercentileValue(percentiles[i], getValue(percentiles[i]));
            }
        } else {
            double[] percentiles = {0.5, 0.75, 0.95, 0.98, 0.99, 0.999};
            if (values.length > 0 && quantiles.length > 0 && values.length == quantiles.length) {
                percentileValues = new PercentileValue[percentiles.length];
                for (int i = 0; i < percentiles.length; i++) {
                    percentileValues[i] = new PercentileValue(percentiles[i], getValue(percentiles[i]));
                }
            } else {
                percentileValues = new PercentileValue[percentiles.length];
                for (int i = 0; i < percentiles.length; i++) {
                    percentileValues[i] = new PercentileValue(percentiles[i], 0);
                }
            }
        }
        return percentileValues;
    }

    @Override
    public HistogramBucket[] bucketValues() {
        Double[] buckets = configurationProperties.bucketValues();
        Arrays.sort(buckets);
        HistogramBucket[] histogramBuckets = new HistogramBucket[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            histogramBuckets[i] = new HistogramBucket(buckets[i], 0);
        }
        return histogramBuckets;
    }

    private double getValue(double quantile) {
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
    
    public long[] getValues() {
        return values;
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
    
    public ConfigurationProperties getConfigAdapter() {
        return this.configurationProperties;
    }

    @Override
    public String toString() {
        return "Snapshot[" + size() + "]";
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
