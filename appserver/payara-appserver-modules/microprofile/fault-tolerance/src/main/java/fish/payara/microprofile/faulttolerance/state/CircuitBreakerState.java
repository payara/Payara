/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *    Copyright (c) [2017] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 * 
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 * @author Andrew Pielage
 */
public class CircuitBreakerState {  
        
    public enum CircuitState {
        OPEN, CLOSED, HALF_OPEN
    }
    
    private final BlockingQueue<Boolean> closedResultsQueue;
    private volatile int halfOpenSuccessfulResultsCounter;
    private volatile CircuitState circuitState = CircuitState.CLOSED;
    
    public CircuitBreakerState(int requestVolumeThreshold) {
        closedResultsQueue = new LinkedBlockingQueue<>(requestVolumeThreshold);
        halfOpenSuccessfulResultsCounter = 0;
    }
    
    public CircuitState getCircuitState() {
        return circuitState;
    }
    
    public void setCircuitState(CircuitState circuitState) {
        this.circuitState = circuitState;
    }
    
    public void recordClosedResult(Boolean result) {
        if (!closedResultsQueue.offer(result)) {
            closedResultsQueue.poll();
            closedResultsQueue.offer(result);
        }
    }
    
    public void resetResults() {
        closedResultsQueue.clear();
    }
    
    public void incrementHalfOpenSuccessfulResultCounter() {
        halfOpenSuccessfulResultsCounter++;
    }
    
    public void resetHalfOpenSuccessfulResultCounter() {
        halfOpenSuccessfulResultsCounter = 0;
    }
    
    public int getHalfOpenSuccessFulResultCounter() {
        return halfOpenSuccessfulResultsCounter;
    }
    
    public Boolean isOverFailureThreshold(long failureThreshold) {
        Boolean over = false;
        int failures = 0;
        
        if (closedResultsQueue.remainingCapacity() == 0) {
            for (Boolean success : closedResultsQueue) {
                if (!success) {
                    failures++;

                    if (failures == failureThreshold) {
                        over = true;
                        break;
                    }
                }
            }
        }
        
        return over;
    }
}
