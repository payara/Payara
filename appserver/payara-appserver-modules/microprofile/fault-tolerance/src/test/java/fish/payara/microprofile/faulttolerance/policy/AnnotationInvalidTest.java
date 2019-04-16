package fish.payara.microprofile.faulttolerance.policy;

import static fish.payara.microprofile.faulttolerance.test.TestUtils.assertAnnotationInvalid;

import java.sql.Connection;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.junit.Test;

/**
 * Testing scenarios from {@code org.eclipse.microprofile.fault.tolerance.tck.invalidParameters} package that cannot be
 * run via TCK since the expected exception is wrapped by weld.
 * 
 * @author Jan Bernitt
 */
public class AnnotationInvalidTest {

    /*
     * InvalidAsynchronousClassTest + InvalidAsynchronousMethodTest
     */

    @Test
    public void asynchronousDoesNotReturnFutureOrCompletionStage() {
        assertAnnotationInvalid("Asynchronous does not return a Future or CompletionStage but: java.lang.String");
    }

    @Asynchronous
    public String asynchronousDoesNotReturnFutureOrCompletionStage_Method() {
        return "foo";
    }

    /*
     * InvalidBulkheadAsynchQueueTest
     */

    @Test
    public void bulkheadNegativeWaitingTaskQueue() {
        assertAnnotationInvalid("Bulkhead has a waitingTaskQueue value less than 0: -1");
    }

    @Bulkhead(waitingTaskQueue = -1)
    public Connection bulkheadNegativeWaitingTaskQueue_Method() {
        return null;
    }

    /*
     * InvalidBulkheadValueTest
     */

    @Test
    public void bulkheadNegativeValue() {
        assertAnnotationInvalid("Bulkhead has a value less than 1: -1");
    }

    @Bulkhead(-1)
    public Connection bulkheadNegativeValue_Method() {
        return null;
    }

    @Test
    public void bulkheadZeroValue() {
        assertAnnotationInvalid("Bulkhead has a value less than 1: 0");
    }

    @Bulkhead(0)
    public Connection bulkheadZeroValue_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerDelayTest
     */

    @Test
    public void circuitBreakerNegativeDelay() {
        assertAnnotationInvalid("CircuitBreaker has a failureRatio value less than 0.0: -1.0");
    }

    @CircuitBreaker(failureRatio = -1)
    public Connection circuitBreakerNegativeDelay_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureRatioNegTest
     */

    @Test
    public void circuitBreakerNegativeFailureRatio() {
        assertAnnotationInvalid("CircuitBreaker has a failureRatio value less than 0.0: -0.1");
    }

    @CircuitBreaker(failureRatio = -0.1)
    public Connection circuitBreakerNegativeFailureRatio_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureRatioPosTest
     */

    @Test
    public void circuitBreakerTooLargeFailureRatio() {
        assertAnnotationInvalid("CircuitBreaker has a failureRatio value greater than 1.0: 1.1");
    }

    @CircuitBreaker(failureRatio = 1.1)
    public Connection circuitBreakerTooLargeFailureRatio_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureReqVol0Test
     */

    @Test
    public void circuitBreakerZeroRequestVolumeThreshold() {
        assertAnnotationInvalid("CircuitBreaker has a requestVolumeThreshold value less than 1: 0");
    }

    @CircuitBreaker(requestVolumeThreshold = 0)
    public Connection circuitBreakerZeroRequestVolumeThreshold_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureReqVolNegTest
     */

    @Test
    public void circuitBreakerNegativeRequestVolumeThreshold() {
        assertAnnotationInvalid("CircuitBreaker has a requestVolumeThreshold value less than 1: -1");
    }

    @CircuitBreaker(requestVolumeThreshold = -1)
    public Connection circuitBreakerNegativeRequestVolumeThreshold_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureSuccess0Test
     */

    @Test
    public void circuitBreakerZeroSuccessThreshold() {
        assertAnnotationInvalid("CircuitBreaker has a successThreshold value less than 1: 0");
    }

    @CircuitBreaker(successThreshold = 0)
    public Connection circuitBreakerZeroSuccessThreshold_Method() {
        return null;
    }

    /*
     * InvalidCircuitBreakerFailureSuccessNegTest
     */

    @Test
    public void circuitBreakerNegativeSuccessThreshold() {
        assertAnnotationInvalid("CircuitBreaker has a successThreshold value less than 1: 0");
    }

    @CircuitBreaker(successThreshold = 0)
    public Connection circuitBreakerNegativeSuccessThreshold_Method() {
        return null;
    }

    /*
     * InvalidRetryDelayDurationTest
     */

    @Test
    public void retryMaxDurationLessThanDelay() {
        assertAnnotationInvalid("Retry has a maxDuration value less than or equal to the delay value: 500");
    }

    @Retry(delay = 1000, maxDuration = 500)
    public Connection retryMaxDurationLessThanDelay_Method() {
        return null;
    }

    /*
     * InvalidRetryDelayTest
     */

    @Test
    public void retryNegativeDelay() {
        assertAnnotationInvalid("Retry has a delay value less than 0: -1");
    }

    @Retry(delay = -1)
    public Connection retryNegativeDelay_Method() {
        return null;
    }

    /*
     * InvalidRetryJitterTest
     */

    @Test
    public void retryNegativeJitter() {
        assertAnnotationInvalid("Retry has a jitter value less than 0: -1");
    }

    @Retry(jitter = -1)
    public Connection retryNegativeJitter_Method() {
        return null;
    }

    /*
     * InvalidRetryMaxRetriesTest
     */

    @Test
    public void retryNegativeMaxRetries() {
        assertAnnotationInvalid("Retry has a maxRetries value less than -1: -3");
    }

    @Retry(maxRetries = -3)
    public Connection retryNegativeMaxRetries_Method() {
        return null;
    }

    /*
     * InvalidTimeoutValueTest
     */

    @Test
    public void timeoutNegativeValue() {
        assertAnnotationInvalid("Timeout has a value less than 0: -1");
    }

    @Timeout(-1)
    public Connection timeoutNegativeValue_Method() {
        return null;
    }
}
