/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.gms.bootstrap;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.ee.cms.core.*;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to hold cluster history. This information is backed by
 * a ConcurrentMap, so iterating over the instances is
 * "weakly consistent" as the state could change at any time
 * (especially during cluster startup).
 */
public final class HealthHistory implements ConfigListener {

    private final static Logger logger = LogDomains.getLogger(
        HealthHistory.class, LogDomains.CORE_LOGGER);

    private static final StringManager strings =
        StringManager.getManager(HealthHistory.class);

    // NOT_RUNNING means there is no time information associated
    public static enum STATE {
        NOT_RUNNING (strings.getString("state.not_running")),
        RUNNING     (strings.getString("state.running")),
        REJOINED    (strings.getString("state.rejoined")),
        FAILURE     (strings.getString("state.failure")),
        SHUTDOWN    (strings.getString("state.shutdown"));

        private final String stringVal;

        STATE(String stringVal) {
            this.stringVal = stringVal;
        }

        @Override
        public String toString() {
            return stringVal;
        }
    };

    /**
     * Used when no time information is known, for instance at
     * cluster startup before an instance has started. 
     */
    public static final long NOTIME = -1l;
    
    private final ConcurrentMap<String, InstanceHealth> healthMap;

    /*
     * Creates a health history that knows about the expected
     * list of instances. This is called from the GMS adapter
     * during initialization, before 
     */
    public HealthHistory(Cluster cluster) {
        healthMap = new ConcurrentHashMap<String, InstanceHealth>(
            cluster.getInstances().size());
        for (Server server : cluster.getInstances()) {
            if (server.isDas()) {
                continue;
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, String.format(
                    "instance name in HealthHistory constructor %s",
                    server.getName()));
            }
            if (healthMap.putIfAbsent(server.getName(),
                new InstanceHealth(STATE.NOT_RUNNING, NOTIME)) != null) {
                logger.log(Level.WARNING,
                    "duplicate.instance", server.getName());
            }
        }
    }

    /**
     * Returns the state/time of a specific instance.
     */
    public InstanceHealth getHealthByInstance(String name) {
        return healthMap.get(name);
    }

    /**
     * The returned list may be modified without affecting
     * the information in the HealthHistory object.
     */
    public List<String> getInstancesByState(STATE targetState) {
        List<String> retVal = new ArrayList<String>(healthMap.size());
        for (String name : healthMap.keySet()) {
            if (healthMap.get(name).state == targetState) {
                retVal.add(name);
            }
        }
        return retVal;
    }

    /**
     * Returns a copy of the instance names. 
     */
    public Set<String> getInstances() {
        return Collections.unmodifiableSet(healthMap.keySet());
    }

    /**
     * Called by GMS subsystem to update the health of an instance.
     *
     * TODO: add try/catch around everything for safety
     */
    public void updateHealth(Signal signal) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "signal: " + signal.toString());
        }
        String name = signal.getMemberToken();
        long time = signal.getStartTime();
        STATE state = null;

        // a little if-elsie....
        if (signal instanceof  JoinNotificationSignal) {
            /*
             * Means an instance is running. We will usually get a
             * JoinedAndReadyNotificationSignal after this in a usual
             * startup. If not, it means the DAS is restarting and
             * the cluster is already up. In that case, we need
             * the original startup time, not the time of the signal.
             */
            state = STATE.RUNNING;
        }   else if (signal instanceof JoinedAndReadyNotificationSignal) {
            /*
             * During a normal startup, this will occur after the
             * JoinNotificationSignal. If it's not a Rejoin, we
             * don't need to process the data since it's already
             * happened during the Join event. But it doesn't hurt.
             */
            JoinedAndReadyNotificationSignal jar =
                (JoinedAndReadyNotificationSignal) signal;
            RejoinSubevent sub = jar.getRejoinSubevent();
            if (sub == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "it's a joined and ready");
                }
                state = STATE.RUNNING;
            } else {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "it's a rejoin");
                }
                state = STATE.REJOINED;
                time = sub.getGroupJoinTime();
            }
        } else if (signal instanceof FailureNotificationSignal) {
            state = STATE.FAILURE;
            time = System.currentTimeMillis();
        } else if (signal instanceof PlannedShutdownSignal) {
            state = STATE.SHUTDOWN;
            time = System.currentTimeMillis();
        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, String.format(
                    "Signal %s not handled in updateHealth",
                    signal.toString()));
            }
            return;
        }
        InstanceHealth ih = new InstanceHealth(state, time);
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, String.format(
                "updating health with %s : %s for signal %s",
                name, ih.toString(), signal.toString()));
        }
        if (healthMap.put(name, ih) == null) {
            logger.log(Level.INFO, "unknown.instance",
                new Object [] {state, name});
        }
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        Object oldVal;
        Object newVal;
        for (PropertyChangeEvent event : events) {
            oldVal = event.getOldValue();
            newVal = event.getNewValue();
            if (oldVal instanceof ServerRef && newVal == null) {
                ServerRef instance = (ServerRef) oldVal;
                deleteInstance(instance.getRef());
            } else if (newVal instanceof ServerRef && oldVal == null) {
                ServerRef instance = (ServerRef) newVal;
                addInstance(instance.getRef());
            }
        }
        return null;
    }

    private void deleteInstance(String name) {
        logger.log(Level.INFO, "deleting.instance", name);
        InstanceHealth oldHealth = healthMap.remove(name);
        if (oldHealth == null) {
            logger.log(Level.WARNING, "delete.key.not.present", name);
        }
    }

    /*
     * We only want to add the instance if it's not already
     * in the map. It could exist already if some trick of time
     * caused a GMS message to be received from the instance
     * before the config changes were processed. We could use
     * current time in the instance health object, but we should
     * be consistent with startup behavior.
     */
    private void addInstance(String name) {
        logger.log(Level.INFO, "adding.instance", name);
        InstanceHealth oldHealth = healthMap.putIfAbsent(name,
            new InstanceHealth(STATE.NOT_RUNNING, NOTIME));
        if (oldHealth != null) {
            logger.log(Level.INFO, "key.already.present", name);
        }
    }

    /*
     * Information in an InstanceHealth object is immutable. For
     * convenience, the fields are public for direct access.
     */
    public static final class InstanceHealth {

        /**
         * The last-known state of the instance.
         */
        public final STATE state;

        /**
         * The time, if known, corresponding to the last change in state.
         */
        public final long time;

        InstanceHealth(STATE state, long time) {
            this.state = state;
            this.time = time;
        }

        @Override
        public String toString() {
            return String.format("InstanceHealth: state '%s' time '%s'",
                state, new Date(time).toString());
        }
    }

}
