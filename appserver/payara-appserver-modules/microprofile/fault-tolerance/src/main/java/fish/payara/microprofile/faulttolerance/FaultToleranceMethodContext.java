package fish.payara.microprofile.faulttolerance;

import java.lang.reflect.Method;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.FallbackHandler;

import fish.payara.microprofile.faulttolerance.state.CircuitBreakerState;

public interface FaultToleranceMethodContext {

    FaultToleranceMetrics getMetrics(boolean enabled);

    /*
     * State
     */

    /**
     * 
     * @param requestVolumeThreshold when negative no state is created if it does not already exist
     * @return the created or existing state, or null if non existed and requestVolumeThreshold was null 
     */
    CircuitBreakerState getState(int requestVolumeThreshold);

    BlockingQueue<Thread> getConcurrentExecutions(int maxConcurrentThreads);

    AtomicInteger getQueuingOrRunningPopulation();

    /*
     * Processing
     */

    /**
     * Proceeds execution to the annotated method body.
     * 
     * @return result returned by the annotated method
     * @throws Exception in case the annotated method threw an {@link Exception}.
     */
    Object proceed() throws Exception;

    /**
     * Delays the current thread by the given duration. The delay is traced.
     * 
     * @param delayMillis the time to sleep in milliseconds
     * @param context     current context delayed
     * @throws InterruptedException In case waiting is interrupted
     */
    void delay(long delayMillis) throws InterruptedException;

    /**
     * Runs a given task after a certain waiting time.
     * 
     * @param delayMillis time to wait in milliseconds before running the given task
     * @param task        operation to run
     * @return A future that can be cancelled if the operation should no longer be run
     */
    Future<?> runDelayed(long delayMillis, Runnable task) throws Exception;

    /**
     * Runs the task asynchronously and completes the given asyncResult with the its outcome.
     * 
     * @param asyncResult a not yet completed {@link CompletableFuture} that should receive the result of the operation
     *                    when it is executed
     * @param context     the currently processed context (for e.g. tracing)
     * @param task        an operation that must compute a value of type {@link Future} or {@link CompletionStage}.
     * @throws RejectedExecutionException In case the task could not be accepted for execution. Usually due to too many
     *                                    work in progress.
     */
    void runAsynchronous(CompletableFuture<Object> asyncResult, Callable<Object> task)
            throws RejectedExecutionException;

    /**
     * Invokes the instance of the given {@link FallbackHandler} {@link Class} defined in the given context to handle
     * the given {@link Exception}.
     * 
     * @param fallbackClass the type of {@link FallbackHandler} to resolve or instantiate and use
     * @param context       the currently processed context to use for arguments
     * @param ex            the {@link Exception} thrown by the FT processing to handle by the {@link FallbackHandler}
     * @return the result returned by the invoked {@link FallbackHandler}
     * @throws Exception in case resolving, instantiating or invoking the handler method fails
     */
    Object fallbackHandle(Class<? extends FallbackHandler<?>> fallbackClass, Exception ex)
            throws Exception;

    /**
     * Invokes the given fallback {@link Method} in the given context.
     * 
     * @param fallbackMethod the {@link Method} to invoke
     * @param context        the currently processed context to use for target instance and method arguments
     * @return the result returned by the invoked fallback method
     * @throws Exception in case invoking the method fails or the invoked method threw an {@link Exception}
     */
    Object fallbackInvoke(Method fallbackMethod) throws Exception;

    /*
     * Tracing
     */

    /**
     * Starts tracing the given context named with the given method label.
     * 
     * @param method  the label to use for the trace
     * @param context the currently processed context
     */
    void trace(String method);

    /**
     * Ends the innermost trace.
     */
    void endTrace();
}
