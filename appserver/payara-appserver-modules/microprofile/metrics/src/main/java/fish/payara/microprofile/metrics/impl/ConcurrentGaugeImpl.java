/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import javax.enterprise.inject.Vetoed;
import org.eclipse.microprofile.metrics.ConcurrentGauge;

/**
 * @see ConcurrentGauge
 * @since 5.193
 */
@Vetoed
public class ConcurrentGaugeImpl implements ConcurrentGauge {

    private final LongAdder count = new LongAdder();
    
    /**
     * A map containing all the values that the count of the gauge has been at in the last minute.
     * The key is the time in seconds that the value changed, the value is the value of the count at the moment
     * before it changed
     */
    private Map<Instant, Long> lastCounts = new ConcurrentHashMap<>();

    /**
     * Increment the counter by one.
     */
    @Override
    public void inc() {
        lastCounts.put(Instant.now(), count.longValue());
        count.increment();
        clearOld();
    }

    /**
     * Returns the counter's current value.
     *
     * @return the counter's current value
     */
    @Override
    public long getCount() {
        return count.sum();
    }

    @Override
    public long getMax() {
        clearOld();
        long max = 0;
        for (Long value : lastCounts.values()) {
            if (value > max) {
                max = value;
            }
        }
        if (count.sum() > max) {
            max = count.sum();
        }
        return max;
    }

    @Override
    public long getMin() {
        clearOld();
        long min = Long.MAX_VALUE;
        for (Long value : lastCounts.values()) {
            if (value < min) {
                min = value;
            }
        }
        if (count.sum() < min) {
            min = count.sum();
        }
        return min;
    }

    @Override
    public void dec() {
        lastCounts.put(Instant.now(), count.longValue());
        count.decrement();
        clearOld();
    }
    
    private void clearOld() {
        Instant currentTime = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        Iterator<Instant> guages = lastCounts.keySet().iterator();
        while (guages.hasNext()) {
            Instant guageTime = guages.next();
            if (guageTime.isBefore(currentTime)) {
                lastCounts.remove(guageTime);
            }
        }
    }
}
