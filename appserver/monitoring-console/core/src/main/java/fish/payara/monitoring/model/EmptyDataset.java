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
 * The {@link EmptyDataset} is the starting point for any of the other {@link SeriesDataset} implementations.
 *
 * When first point is added to the {@link EmptyDataset} it becomes a {@link ConstantDataset}.
 * 
 * {@link EmptyDataset} are initialised with a {@link #capacity} so it can be passed on as the set eventually evolves
 * into a {@link PartialDataset} which has a {@link #capacity()} limit.
 *
 * @author Jan Bernitt
 */
public final class EmptyDataset extends SeriesDataset {

    private final int capacity;

    public EmptyDataset(String instance, Series series, int capacity) {
        super(series, instance, -1L, 0);
        this.capacity = capacity;
    }

    @Override
    public int getObservedValueChanges() {
        return 0;
    }

    @Override
    public long[] points() {
        return new long[0];
    }

    @Override
    public SeriesDataset add(long time, long value) {
        return new ConstantDataset(this, time, value);
    }

    @Override
    public long getObservedMin() {
        return Long.MAX_VALUE;
    }

    @Override
    public long getObservedMax() {
        return Long.MIN_VALUE;
    }

    @Override
    public BigInteger getObservedSum() {
        return BigInteger.ZERO;
    }

    @Override
    public long getStableSince() {
        return -1;
    }

    @Override
    public int getStableCount() {
        return 0;
    }

    @Override
    public boolean isOutdated() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public long lastValue() {
        return 0;
    }

    @Override
    public long firstTime() {
        return -1;
    }

    @Override
    public long lastTime() {
        return -1;
    }

    @Override
    public int capacity() {
        return capacity;
    }

    @Override
    public int estimatedBytesMemory() {
        return 32;
    }
}
