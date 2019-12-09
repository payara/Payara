package fish.payara.monitoring.store;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.nucleus.executorservice.PayaraExecutorService;

/**
 * A {@link JobHandle} is a utility class for safely starting and stopping scheduled tasks.
 *
 * @author Jan Bernitt
 */
public final class JobHandle {

    protected static final Logger LOGGER = Logger.getLogger(JobHandle.class.getName());

    private final AtomicReference<ScheduledFuture<?>> job = new AtomicReference<>();
    private final String description;

    public JobHandle(String description) {
        this.description = description;
    }

    public void start(PayaraExecutorService executor, int time, TimeUnit unit, Runnable work) {
        if (job.get() == null) {
            ScheduledFuture<?> task = executor.scheduleAtFixedRate(work, 0L, time, unit);
            if (!job.compareAndSet(null, task)) {
                cancelTask(task, description);
            }
        }
    }

    public void stop() {
        cancelTask(job.getAndUpdate(job -> null), description);
    }

    private static void cancelTask(ScheduledFuture<?> task, String description) {
        if (task != null) {
            LOGGER.info("Stopping " + description +".");
            try {
                task.cancel(false);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to cancel " + description + ".", e);
            }
        }
    }

    @Override
    public String toString() {
        return JobHandle.class.getSimpleName() + " for " + description;
    }
}
