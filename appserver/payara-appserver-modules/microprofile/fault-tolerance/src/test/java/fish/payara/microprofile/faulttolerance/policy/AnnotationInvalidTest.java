/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
