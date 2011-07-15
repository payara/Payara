/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.appclient.server.core.jws;

import java.util.HashMap;
import java.util.Map;

/**
 * Models the state (and transitions) related to Java Web Start support of
 * app clients for a given client.
 * <p>
 * As an app is enabled, disabled, suspended, or resumed, the state of
 * how Java Web Start should respond is different.  What transitions are
 * permitted in each state is also enforced.
 * <p>
 * Implementation note: I tried recording the valid transitions (action plus
 * new state) as part of the state value definitions themselves in the enum.
 * This did not seem to work; in any {@link Transition} values for earlier
 * states, references to states defined later in the enum were null!  It's
 * probably a little cleaner design to keep the states and the set of valid
 * transitions separate from each other anyway.
 *
 * @author tjquinn
 */
public class JavaWebStartState {

    /**
     * Records a transition to the new state that is implied by the current
     * state and the specified action,
     * executing the associated task as part of the transition (if the
     * transition is valid).
     * <p>
     * The specified action must match a valid transition from the
     * current state.
     * 
     * @param action the Action the server is notifying us about
     * @param task the work to perform
     */
    public void transition(final Action action, final Runnable task) {
        final Transition t = validTransitions.get(currentStateValue).get(action);

        if (t == null) {
            throw new IllegalStateException(currentStateValue.toString() + " : " + action.toString());
        }
        final StateValue newStateValue = t.newStateValue;
        if (task != null && ! t.isNOOP) {
            task.run();
        }
        currentStateValue = newStateValue;
    }

    /**
     * Reports whether the state represents a "running" condition.
     * @return
     */
    public boolean isRunning() {
        return currentStateValue == StateValue.RUNNING;
    }

    /**
     * Reports whether the state represents a "suspended" condition.
     * @return
     */
    public boolean isSuspended() {
        return currentStateValue == StateValue.SUSPENDED;
    }

    /**
     * Possible states
     */
    private enum StateValue {
        STOPPED, RUNNING, SUSPENDED
    }

    /**
     * Possible actions the server can tell us about.
     */
    public enum Action {

        START, STOP, SUSPEND, RESUME
    }

    /**
     * Records a valid transition given an action to a new state.
     */
    private static class Transition {

        final Action action;
        final StateValue newStateValue;
        final boolean isNOOP;

        Transition(final Action action, final StateValue newState) {
            this(action, newState, false);
        }

        Transition(final Action action, final StateValue newState, final boolean isNOOP) {
            super();
            this.action = action;
            this.newStateValue = newState;
            this.isNOOP = isNOOP;
        }
    }

    private StateValue currentStateValue = StateValue.STOPPED;
    
    /** the valid transitions for this state engine */
    private final Map<StateValue, Map<Action, Transition>> validTransitions = initValidTransitions();

    /**
     * Legal state transitions:
     * <table border="1">
     * <tr><th>Action</th><th colspan="2">Transition</th></tr>
     * <tr><th/><th>From</th><th>To</th></tr>
     * <tr><td>start</td><td>STOPPED</td><td>RUNNING</td></tr>
     * <tr><td>stop</td><td>RUNNING<br>SUSPENDED<br>STOPPED(no-op)</td><td>STOPPED</td></tr>
     * <tr><td>suspend</td><td>RUNNING<br>SUSPENDED(no-op)</td><td>SUSPENDED</td></tr>
     * <tr><td>resume</td><td>SUSPENDED</td><td>RUNNING</td></tr>
     * </table>
     *
     */
    private Map<StateValue, Map<Action, Transition>> initValidTransitions() {
        final Map<StateValue, Map<Action, Transition>> result = new HashMap<StateValue, Map<Action, Transition>>();
        addTransitionSet(result, StateValue.STOPPED,
                new Transition(Action.START, StateValue.RUNNING),
                new Transition(Action.STOP, StateValue.STOPPED, true /* is NOOP */));

        addTransitionSet(result, StateValue.RUNNING,
                new Transition(Action.STOP, StateValue.STOPPED),
                new Transition(Action.SUSPEND, StateValue.SUSPENDED));

        addTransitionSet(result, StateValue.SUSPENDED,
                new Transition(Action.RESUME, StateValue.RUNNING),
                new Transition(Action.SUSPEND, StateValue.SUSPENDED, true /* is NOOP */));
        return result;
    }

    private void addTransitionSet(final Map<StateValue, Map<Action, Transition>> map, StateValue originalState, Transition... transitions) {
        Map<Action, Transition> transitionsForState = map.get(originalState);
        if (transitionsForState == null) {
            transitionsForState = new HashMap<Action, Transition>();
            map.put(originalState, transitionsForState);
        }
        for (Transition t : transitions) {
            transitionsForState.put(t.action, t);
        }
    }
}
