/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.impl;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.enterprise.inject.Vetoed;

import org.eclipse.microprofile.metrics.SimpleTimer;

/**
 * A timer that aggregates timed durations and provides the total as {@link #getElapsedTime()} and the number of updates
 * as {@link #getCount()}.
 *
 * Durations are measured in nanoseconds.
 *
 * @author Jan Bernitt
 * @since 5.202
 */
@Vetoed
public class SimpleTimerImpl extends CompleteMinuteMinMaxTracker implements SimpleTimer {

    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong totalDurationNanos = new AtomicLong();

    public SimpleTimerImpl(Clock clock) {
        super(clock);
    }

    @Override
    public void update(Duration duration) {
        // synchronisation note: since there is no way of synchronously reading both updated values it does not matter
        // that both updates cannot be together atomically. Each is thread-safe on its own and that is as good as it gets
        long nanos = duration.toNanos();
        totalDurationNanos.addAndGet(nanos);
        callCount.incrementAndGet();
        updateValue(nanos);
    }

    @Override
    public <T> T time(Callable<T> event) throws Exception {
        long startTime = clock.getTick();
        try {
            return event.call();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public void time(Runnable event) {
        long startTime = clock.getTick();
        try {
            event.run();
        } finally {
            update(Duration.ofNanos(clock.getTick() - startTime));
        }
    }

    @Override
    public SimpleTimer.Context time() {
        return new Context(this, clock);
    }

    @Override
    public Duration getElapsedTime() {
        return Duration.ofNanos(totalDurationNanos.get());
    }

    @Override
    public long getCount() {
        return callCount.get();
    }

    @Override
    public Duration getMaxTimeDuration() {
        Long nanos = getMaxValue();
        return nanos == null ? null : Duration.ofNanos(nanos);
    }

    @Override
    public Duration getMinTimeDuration() {
        Long nanos = getMinValue();
        return nanos == null ? null : Duration.ofNanos(nanos);
    }

    @Override
    public String toString() {
        return "SimpleTimer[" + getCount() + "]";
    }

    private static final class Context implements SimpleTimer.Context {

        private final SimpleTimer timer;
        private final Clock clock;
        private final long startTime;

        Context(SimpleTimer timer, Clock clock) {
            this.timer = timer;
            this.clock = clock;
            this.startTime = clock.getTick();
        }

        @Override
        public long stop() {
            final long elapsed = clock.getTick() - startTime;
            timer.update(Duration.ofNanos(elapsed));
            return elapsed;
        }

        @Override
        public void close() {
            stop();
        }

    }
}
