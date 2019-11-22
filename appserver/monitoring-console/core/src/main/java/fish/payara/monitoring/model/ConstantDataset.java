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
 * A special {@link SeriesDataset} for {@link Series} for only same value was observed so far.
 * 
 * The main advantage over a {@link PartialDataset} is the low memory footprint and overall smaller object size for
 * values that do not change anyway. Should they change the {@link #add(long, long)} method returns a
 * {@link PartialDataset}.
 * 
 * A minor second advantage is that the observed span can have any length and still be represented with just two points.
 * So in contrast to a {@link PartialDataset} which only has information for a fixed sliding time-frame the constant
 * nature allows the {@link ConstantDataset} to span any time-frame giving the user a more information while using less
 * resources.
 * 
 * Last but not least the {@link ConstantDataset} does not risk to become {@link PartialDataset#isOutdated()}.
 * 
 * @author Jan Bernitt
 */
public class ConstantDataset extends SeriesDataset {

    /**
     * The time expected at {@link #time(int)} called with zero. If this time is different the window is out-dated.
     */
    private final long stableSince;
    /**
     * The most recent time the {@link #value} was observed
     */
    private final long time;
    /**
     * The constant value (only observed value)
     */
    private final long value;

    private final int capacity;

    public ConstantDataset(SeriesDataset predecessor, long time) {
        super(predecessor);
        this.capacity = predecessor.capacity();
        this.stableSince = predecessor.getStableSince();
        this.time = time;
        this.value = predecessor.lastValue();
    }

    public ConstantDataset(EmptyDataset predecessor, long time, long value) {
        super(predecessor.getSeries(), predecessor.getInstance(), time, 1);
        this.capacity = predecessor.capacity();
        this.stableSince = time;
        this.time = time;
        this.value = value;
    }

    @Override
    public long[] points() {
        return size() == 1
                ? new long[] { stableSince, value }
                : new long[] { stableSince, value, time, value };
    }

    @Override
    public SeriesDataset add(long time, long value) {
        if (time == lastTime()) {
            return new PartialDataset(this, time, value + lastValue());
        }
        return value == lastValue() ? new ConstantDataset(this, time) : new PartialDataset(this, time, value);
    }

    @Override
    public long getObservedMin() {
        return value;
    }

    @Override
    public long getObservedMax() {
        return value;
    }

    @Override
    public BigInteger getObservedSum() {
        return BigInteger.valueOf(getObservedValues()).multiply(BigInteger.valueOf(value));
    }

    @Override
    public int getObservedValueChanges() {
        return 1;
    }

    @Override
    public long getStableSince() {
        return stableSince;
    }

    @Override
    public int getStableCount() {
        return getObservedValues();
    }

    @Override
    public final boolean isOutdated() {
        return false;
    }

    @Override
    public final int size() {
        return stableSince == time ? 1 : 2;
    }

    @Override
    public final long firstTime() {
        return stableSince;
    }

    @Override
    public final long lastTime() {
        return time;
    }

    @Override
    public final long lastValue() {
        return value;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public int estimatedBytesMemory() {
        return 56;
    }
}
