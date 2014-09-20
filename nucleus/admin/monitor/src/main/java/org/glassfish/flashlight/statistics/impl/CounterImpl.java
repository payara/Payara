/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.flashlight.statistics.impl;

import org.glassfish.flashlight.statistics.*;
import java.util.concurrent.atomic.AtomicLong;
import org.glassfish.flashlight.datatree.impl.AbstractTreeNode;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * @author Harpreet Singh
 */
@Service(name = "counter")
@PerLookup
public class CounterImpl extends AbstractTreeNode implements Counter {

    /** DEFAULT_UPPER_BOUND is maximum value Long can attain */
    public static final long DEFAULT_MAX_BOUND = java.lang.Long.MAX_VALUE;
    /** DEFAULT_LOWER_BOUND is same as DEFAULT_VALUE i.e. 0 */
    public static final long DEFAULT_VALUE = java.math.BigInteger.ZERO.longValue();
    public static final long DEFAULT_MIN_BOUND = DEFAULT_VALUE;
    /** DEFAULT_VALUE of any statistic is 0 */
    protected static final String NEWLINE = System.getProperty("line.separator");
    private AtomicLong count = new AtomicLong(0);
    long max = 0;
    long min = 0;
    private AtomicLong lastSampleTime = new AtomicLong();
    private String DESCRIPTION = "Counter CountStatistic";
    private String UNIT = java.lang.Long.class.toString();

    private long startTime = 0;

    public CounterImpl() {
        startTime = System.currentTimeMillis();
    }

    public long getCount() {
        return count.get();
    }

    public void setCount(long count) {
        if (count > max) {
            max = count;
        } else {
            min = count;
        }
        this.count.set(count);
    }
    // TBD: remove reference to getSampleTime -> extremely inefficient implementation
    // Will have to be replaced by Timer implemenation
    public void increment() {
        long cnt = this.count.incrementAndGet();
        if (cnt > max) {
            max = cnt;
        // Remove this after refactoring to Timer Impl. This is inefficient
        }
        this.lastSampleTime.set(getSampleTime ());
    }

    //automatically add the increment to cnt
    public void  increment(long delta) {
        long cnt = this.count.addAndGet(delta);
        if(cnt > max) {
            max = cnt;
        }
        this.lastSampleTime.set(getSampleTime());
    }
    
    public void decrement() {
        long cnt = this.count.decrementAndGet();
        if (cnt < min) {
            min = cnt;
        }
    }

    public void setReset(boolean reset) {
        if (reset) {
            this.count.set(0);
        }
    }

    @Override
    public Object getValue() {
        return getCount();
    }

    public String getUnit() {
        return this.UNIT;
    }

    public String getDescription() {
        return this.DESCRIPTION;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getLastSampleTime() {
        return this.lastSampleTime.longValue();
    }

    /*
     * TBD
     * This is an inefficient implementation. Should schedule a Timer task
     * that gets a timeout event every 30s or so and updates this value
     */
    private long getSampleTime() {
        return System.currentTimeMillis();

    }
}
