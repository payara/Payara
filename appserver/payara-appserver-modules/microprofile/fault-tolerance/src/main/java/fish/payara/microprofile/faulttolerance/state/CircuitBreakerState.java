/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2017-2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.microprofile.faulttolerance.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that represents the state of a CircuitBreaker.
 * @author Andrew Pielage
 * @author Jan Bernitt (2.0)
 */
public class CircuitBreakerState {

    private static final Logger logger = Logger.getLogger(CircuitBreakerState.class.getName());

    public enum CircuitState {
        OPEN, CLOSED, HALF_OPEN
    }

    private final int failureThreshold;
    private final AtomicInteger halfOpenSuccessfulResultsCounter = new AtomicInteger(0);
    private final Map<CircuitState, StateTime> allStateTimes = new ConcurrentHashMap<>(CircuitState.values().length);
    private volatile StateTime currentStateTime;
    private final boolean[] failureBuffer;
    private int failureIndex = 0;
    private long outcomeUpdates = 0L;

    public CircuitBreakerState(int requestVolumeThreshold, double failureRatio) {
        this.failureBuffer = new boolean[Math.max(0, requestVolumeThreshold)];
        this.failureThreshold = (int) Math.round(requestVolumeThreshold * failureRatio);
        for(CircuitState state : CircuitState.values()) {
            this.allStateTimes.put(state, new StateTime(state));
        }
        this.currentStateTime = this.allStateTimes.get(CircuitState.CLOSED);
    }

    /**
     * Gets the current circuit state.
     * @return The current circuit state
     */
    public CircuitState getCircuitState() {
        return this.currentStateTime.state();
    }

    /**
     * Sets the CircuitBreaker state to the provided enum value.
     * @param circuitState The state to set the CircuitBreaker to.
     */
    public void setCircuitState(CircuitState circuitState) {
        this.currentStateTime.update();
        if (!this.currentStateTime.is(circuitState)) {
            StateTime nextStateTime = allStateTimes.get(circuitState);
            nextStateTime.reset();
            this.currentStateTime = nextStateTime;
        }
    }

    /**
     * Records a success or failure result to the CircuitBreaker.
     * @param success True for a success, false for a failure
     */
    public synchronized void recordClosedOutcome(boolean success) {
        failureBuffer[failureIndex] = !success;
        failureIndex = (failureIndex + 1) % failureBuffer.length;
        outcomeUpdates++;
    }

    public synchronized boolean isClosedOutcomeSuccessOnly() {
        if (failureBuffer.length == 0 || outcomeUpdates < failureBuffer.length) {
            return false;
        }
        for (boolean failure : failureBuffer) {
            if (failure)
                return false;
        }
        return true;
    }

    /**
     * Clears the results queue.
     */
    public synchronized void resetResults() {
        failureIndex = 0;
        outcomeUpdates = 0L;
    }

    /**
     * Increments the successful results counter for the half open state.
     */
    public void incrementHalfOpenSuccessfulResultCounter() {
        this.halfOpenSuccessfulResultsCounter.incrementAndGet();
    }

    /**
     * Resets the successful results counter for the half open state.
     */
    public void resetHalfOpenSuccessfulResultCounter() {
        this.halfOpenSuccessfulResultsCounter.set(0);
    }

    /**
     * Gets the successful results counter for the half open state.
     * @return The number of consecutive successful results.
     */
    public int getHalfOpenSuccessfulResultCounter() {
        return this.halfOpenSuccessfulResultsCounter.get();
    }

    /**
     * Checks to see if the CircuitBreaker is over the given failure threshold.
     */
    public synchronized boolean isOverFailureThreshold() {
        // Only check if the queue is full
        if (outcomeUpdates < failureBuffer.length) {
            logger.log(Level.FINE, "CircuitBreaker results queue isn't full yet.");
            return false;
        }
        int failures = 0;
        for (boolean failure : failureBuffer) {
            if (failure) {
                failures++;
                if (failures >= failureThreshold) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Updates if the current state equals the provided and returns the amount of nanos.
     * @return The amount of nanos.
     */
    public long updateAndGet(CircuitState circuitState) {
        return this.currentStateTime.is(circuitState)
                ? this.currentStateTime.update()
                : this.allStateTimes.get(circuitState).nanos();
    }

    public long nanosOpen() {
        return updateAndGet(CircuitState.OPEN);
    }

    public long nanosHalfOpen() {
        return updateAndGet(CircuitState.HALF_OPEN);
    }

    public long nanosClosed() {
        return updateAndGet(CircuitState.CLOSED);
    }

    public void close() {
        setCircuitState(CircuitState.CLOSED);
        resetHalfOpenSuccessfulResultCounter();
        resetResults();
    }

    public void open() {
        setCircuitState(CircuitState.OPEN);
        resetHalfOpenSuccessfulResultCounter();
    }

    public void halfOpen() {
        logger.log(Level.FINE, "Setting CircuitBreaker state to half open");
        setCircuitState(CircuitState.HALF_OPEN);
    }

    public boolean halfOpenSuccessfulClosedCircuit(int successThreshold) {
        incrementHalfOpenSuccessfulResultCounter();
        if (getHalfOpenSuccessfulResultCounter() == successThreshold) {
            close();
            return true;
        }
        return false;
    }
}
