package fish.payara.microprofile.faulttolerance.state;

import java.util.concurrent.Semaphore;

public final class BulkheadSemaphore extends Semaphore {

    final int totalPermits;

    public BulkheadSemaphore(int permits) {
        super(permits, true);
        this.totalPermits = permits;
    }

    public int getTotalPermits() {
        return totalPermits;
    }

    /**
     * Note that the number of acquired permits is only correct if {@link #acquire()} and {@link #release()} are used
     * consistent.
     * 
     * @return number of currently acquire permits
     */
    public int acquiredPermits() {
        return totalPermits - availablePermits();
    }
}
