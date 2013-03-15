/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;
import org.jvnet.hk2.config.ConfigListener;
import org.jvnet.hk2.config.UnprocessedChangeEvents;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import org.glassfish.api.logging.LogLevel;

/**
 * Used to hold cluster history. This information is backed by
 * a ConcurrentMap, so iterating over the instances is
 * "weakly consistent" as the state could change at any time
 * (especially during cluster startup).
 */
public final class HealthHistory implements ConfigListener {

    //private final static Logger logger = LogDomains.getLogger(
    //    HealthHistory.class, LogDomains.CORE_LOGGER);

    private static final StringManager strings =
        StringManager.getManager(HealthHistory.class);

    @LoggerInfo(subsystem = "CLSTR", description="Group Management Service Logger", publish=true)
    private static final String GMSBS_LOGGER_NAME = "javax.enterprise.cluster.gms.bootstrap";


    @LogMessagesResourceBundle
    private static final String LOG_MESSAGES_RB = "org.glassfish.cluster.gms.bootstrap.LogMessages";

    static final Logger GMSBS_LOGGER = Logger.getLogger(GMSBS_LOGGER_NAME, LOG_MESSAGES_RB);

    @LogMessageInfo(message = "Adding instance {0} to health history table.", level="INFO")
    private static final String GMS_ADDING_INSTANCE="NCLS-CLSTR-20001";

    @LogMessageInfo(message = "Instance {0} was not in map when deleted from health history table.",
                    level="WARNING",
                    cause="More than one call may have been made to remove this instance" +
                          " from the cluster. This has no other effect on the health history information.",
                    action="No action is necessary.")
    private static final String GMS_INSTANCE_NOT_PRESENT="NCLS-CLSTR-20002";

    // deleting_instance=GMSBS2003: Deleting instance {0} from health history table.
    @LogMessageInfo(message = "Deleting instance {0} from health history table.", level="INFO")
    private static final String GMS_DELETE_INSTANCE="NCLS-CLSTR-20003";

    // duplicate_instance=GMSBS2004: Duplicate instance {0} ignored in health history.
    @LogMessageInfo(message = "Duplicate instance {0} ignored in health history.",
                    level="WARNING",
                    cause="There may be more than one instance in the cluster with the same name.",
                    action="Check that instance names are unique within the cluster.")
    private static final String GMS_DUPLICATE_INSTANCE="NCLS-CLSTR-20004";

    // key_already.present=GMSBS2005: State already known for instance {0}. Not adding to health history table.
    @LogMessageInfo(message = "State already known for instance {0}. Not adding to health history table.", level="INFO")
    private static final String GMS_INSTANCE_ALREADY_PRESENT="NCLS-CLSTR-20005";

    // unknown_instance=GMSBS2006: New state {0} added for unknown instance {1}
    @LogMessageInfo(message = "New state {0} added for unknown instance {1}", level="INFO")
    private static final String GMS_INSTANCE_UNKNOWN_STATE="NCLS-CLSTR-20006";

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
            if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
                GMSBS_LOGGER.log(LogLevel.FINE, String.format(
                    "instance name in HealthHistory constructor %s",
                    server.getName()));
            }
            if (healthMap.putIfAbsent(server.getName(),
                new InstanceHealth(STATE.NOT_RUNNING, NOTIME)) != null) {
                GMSBS_LOGGER.log(LogLevel.WARNING, GMS_DUPLICATE_INSTANCE, server.getName());
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
        if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
            GMSBS_LOGGER.log(LogLevel.FINE, "signal: " + signal.toString());
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
                if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
                    GMSBS_LOGGER.log(LogLevel.FINE, "it's a joined and ready");
                }
                state = STATE.RUNNING;
            } else {
                if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
                    GMSBS_LOGGER.log(LogLevel.FINE, "it's a rejoin");
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
            if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
                GMSBS_LOGGER.log(LogLevel.FINE, String.format(
                    "Signal %s not handled in updateHealth",
                    signal.toString()));
            }
            return;
        }
        InstanceHealth ih = new InstanceHealth(state, time);
        if (GMSBS_LOGGER.isLoggable(LogLevel.FINE)) {
            GMSBS_LOGGER.log(LogLevel.FINE, String.format(
                "updating health with %s : %s for signal %s",
                name, ih.toString(), signal.toString()));
        }
        if (healthMap.put(name, ih) == null) {
            GMSBS_LOGGER.log(LogLevel.INFO, GMS_INSTANCE_UNKNOWN_STATE,
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
        GMSBS_LOGGER.log(LogLevel.INFO, GMS_DELETE_INSTANCE, name);
        InstanceHealth oldHealth = healthMap.remove(name);
        if (oldHealth == null) {
            GMSBS_LOGGER.log(LogLevel.WARNING, GMS_INSTANCE_NOT_PRESENT, name);
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
        GMSBS_LOGGER.log(LogLevel.INFO, GMS_ADDING_INSTANCE, name);
        InstanceHealth oldHealth = healthMap.putIfAbsent(name,
            new InstanceHealth(STATE.NOT_RUNNING, NOTIME));
        if (oldHealth != null) {
            GMSBS_LOGGER.log(LogLevel.INFO, GMS_INSTANCE_ALREADY_PRESENT, name);
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
