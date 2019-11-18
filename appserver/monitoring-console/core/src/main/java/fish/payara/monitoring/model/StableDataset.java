/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.monitoring.model;

import java.math.BigInteger;

/**
 * A {@link StableDataset} is a dataset of a {@link Series} that was a {@link PartialDataset} before it became stable
 * for such a long duration that it moved to a {@link StableDataset}.
 * 
 * In contrast to a {@link ConstantDataset} a {@link StableDataset} does have a history of observed values changes and
 * differing sum, minimum and maximum values. A {@link ConstantDataset} on the other hand has only ever observed the
 * very same value which is its minimum, maximum and average value for any number of observed values.
 * 
 * @author Jan Bernitt
 */
public final class StableDataset extends ConstantDataset {

    private final int observedValueChanges;
    private final long observedMax;
    private final long observedMin;
    private final BigInteger observedSum;
    private final int stableCount;

    public StableDataset(SeriesDataset predecessor, long time) {
        super(predecessor, time);
        this.observedValueChanges = predecessor.getObservedValueChanges();
        this.observedMax = predecessor.getObservedMax();
        this.observedMin = predecessor.getObservedMin();
        this.observedSum = predecessor.getObservedSum().add(BigInteger.valueOf(predecessor.lastValue()));
        this.stableCount = predecessor.getStableCount() + 1;
    }

    @Override
    public int getObservedValueChanges() {
        return observedValueChanges;
    }

    @Override
    public long getObservedMax() {
        return observedMax;
    }

    @Override
    public long getObservedMin() {
        return observedMin;
    }

    @Override
    public BigInteger getObservedSum() {
        return observedSum;
    }

    @Override
    public int getStableCount() {
        return stableCount;
    }

    @Override
    public SeriesDataset add(long time, long value) {
        if (time == lastTime()) {
            return new PartialDataset(this, time, value + lastValue());
        }
        return value == lastValue() ? new StableDataset(this, time) : new PartialDataset(this, time, value);
    }

    @Override
    public int estimatedBytesMemory() {
        return 100;
    }
}
