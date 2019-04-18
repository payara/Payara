package fish.payara.microprofile.faulttolerance;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import fish.payara.microprofile.faulttolerance.service.Stereotypes;
import fish.payara.microprofile.faulttolerance.state.BulkheadSemaphore;
import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

/**
 * Essentially a list of all methods needed to process FT behaviour.
 * 
 * Decouples the FT processing facilities and state from any specific implementation to allow e.g. unit testing.
 *
 * @author Jan Bernitt
 */
public interface FaultToleranceService {

    /*
     * Factory methods
     */

    FaultToleranceConfig getConfig(InvocationContext context, Stereotypes stereotypes);

    FaultToleranceMetrics getMetrics(InvocationContext context);

    /*
     * State
     */

    CircuitBreakerState getState(int requestVolumeThreshold, InvocationContext context);

    BulkheadSemaphore getConcurrentExecutions(int maxConcurrentThreads, InvocationContext context);

    BulkheadSemaphore getWaitingQueuePopulation(int queueCapacity, InvocationContext context);

    /*
     * Processing
     */

    /**
     * Delays the current thread by the given duration. The delay is traced.
     * 
     * @param delayMillis the time to sleep in milliseconds
     * @param context     current context delayed
     * @throws InterruptedException In case waiting is interrupted
     */
    void delay(long delayMillis, InvocationContext context) throws InterruptedException;

    /**
     * Runs a given task after a certain waiting time.
     * 
     * @param delayMillis time to wait in milliseconds before running the given task
     * @param task        operation to run
     * @return A future that can be cancelled if the operation should no longer be run
     */
    Future<?> scheduleDelayed(long delayMillis, Runnable task) throws Exception;

    /**
     * Runs the task asynchronously and completes the given asyncResult with the its outcome.
     * 
     * @param asyncResult a not yet completed {@link CompletableFuture} that should receive the result of the operation
     *                    when it is executed
     * @param task        an operation that must compute a value of type {@link Future} or {@link CompletionStage}.
     * @throws RejectedExecutionException In case the task could not be accepted for execution. Usually due to too many
     *                                    work in progress.
     */
    void runAsynchronous(CompletableFuture<Object> asyncResult, Callable<Object> task)
            throws RejectedExecutionException;

    Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, InvocationContext context, Exception ex)
            throws Exception;

    Object fallbackInvoke(Method fallbackMethod, InvocationContext context) throws Exception;

    /*
     * Tracing
     */

    void trace(String method, InvocationContext context);

    void endTrace();

}
