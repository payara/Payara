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
public class SimpleTimerImpl implements SimpleTimer {

    private final AtomicLong callCount = new AtomicLong();
    private final AtomicLong totalDurationNanos = new AtomicLong();
    private final Clock clock;

    public SimpleTimerImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void update(Duration duration) {
        // synchronisation note: since there is no way of synchronously reading both updated values it does not matter
        // that both updates cannot be together atomically. Each is thread-safe on its own and that is as good as it gets
        totalDurationNanos.addAndGet(duration.toNanos());
        callCount.incrementAndGet();
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
