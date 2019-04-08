package fish.payara.microprofile.faulttolerance;

import java.util.concurrent.Future;

import javax.interceptor.InvocationContext;

import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

public interface FaultToleranceExecution {

    CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context);

    void scheduleHalfOpen(long delayMillis, CircuitBreakerState circuitBreakerState) throws Exception;

    BulkheadSemaphore getExecutionSemaphoreOf(int maxConcurrentThreads, InvocationContext context);

    BulkheadSemaphore getWaitingQueueSemaphoreOf(int queueCapacity, InvocationContext context);

    Future<?> runAsynchronous(InvocationContext context) throws Exception;

    //completeAsynchronous

    /**
     * @return A future that can be cancelled if the method execution completes before the interrupt happens
     */
    Future<?> timeoutIn(long timeoutMillis) throws Exception;

    void startTrace(String method, InvocationContext context);

    void endTrace();
}
