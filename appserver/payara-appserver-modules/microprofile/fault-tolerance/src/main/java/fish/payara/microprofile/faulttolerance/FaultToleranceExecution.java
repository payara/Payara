package fish.payara.microprofile.faulttolerance;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

public interface FaultToleranceExecution {

    CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context);

    BulkheadSemaphore getExecutionSemaphoreOf(int maxConcurrentThreads, InvocationContext context);

    BulkheadSemaphore getWaitingQueueSemaphoreOf(int queueCapacity, InvocationContext context);

    void delay(long delayMillis, InvocationContext context) throws InterruptedException;

    void runAsynchronous(CompletableFuture<Object> asyncResult, Callable<Object> operation) throws Exception;

    /**
     * @return A future that can be cancelled if the method execution completes before the interrupt happens
     */
    Future<?> scheduleDelayed(long delayMillis, Runnable operation) throws Exception;

    Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, InvocationContext context, Exception exception) throws Exception;

    Object fallbackInvoke(String fallbackMethod, InvocationContext context) throws Exception;

    void startTrace(String method, InvocationContext context);

    void endTrace();

}
