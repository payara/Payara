/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.timer.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.cp.lock.FencedLock;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.NonPersistentEJBTimerService;
import com.sun.ejb.containers.RuntimeTimerState;
import com.sun.ejb.containers.TimerPrimaryKey;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.micro.data.InstanceDescriptor;
import fish.payara.nucleus.cluster.ClusterListener;
import fish.payara.nucleus.cluster.MemberEvent;
import fish.payara.nucleus.cluster.PayaraCluster;
import fish.payara.nucleus.eventbus.ClusterMessage;
import fish.payara.nucleus.eventbus.MessageReceiver;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.ejb.EJBException;
import jakarta.ejb.FinderException;
import jakarta.ejb.TimerConfig;
import jakarta.transaction.TransactionManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.internal.api.Globals;

/**
 * Store for EJB timers that exist across a Hazelcast cluster.
 * @author steve
 * @since 4.1.1.163
 */
public class HazelcastTimerStore extends NonPersistentEJBTimerService implements ClusterListener,
        MessageReceiver<EjbTimerEvent> {

    private static final String EJB_TIMER_CACHE_NAME = "HZEjbTmerCache";
    private static final String EJB_TIMER_CONTAINER_CACHE_NAME = "HZEjbTmerContainerCache";
    private static final String EJB_TIMER_APPLICAION_CACHE_NAME = "HZEjbTmerApplicationCache";

    private final IMap<String, HZTimer> pkCache;
    private final IMap<Long, Set<TimerPrimaryKey>> containerCache;
    private final IMap<Long, Set<TimerPrimaryKey>> applicationCache;
    private final String serverName;
    private final HazelcastInstance hazelcast;

    private static final Logger logger = LogDomains.getLogger(HazelcastTimerStore.class, LogDomains.EJB_LOGGER);

    static void init(HazelcastCore core) {
        try {
            HazelcastTimerStore store = new HazelcastTimerStore(core);
            Globals.getDefaultBaseServiceLocator().getService(PayaraCluster.class).addClusterListener(store);
            EJBTimerService.setPersistentTimerService(store);
            Globals.getDefaultBaseServiceLocator().getService(PayaraCluster.class).getEventBus().addMessageReceiver(
                    EjbTimerEvent.EJB_TIMER_EVENTS_TOPIC, store);
        } catch (Exception ex) {
            Logger.getLogger(HazelcastTimerStore.class.getName()).log(Level.WARNING, "Problem when initialising Timer Store", ex);
        }
    }

    public HazelcastTimerStore(HazelcastCore core) throws Exception {

        if (!core.isEnabled()) {
            throw new Exception("Hazelcast MUST be enabled when using the HazelcastTimerStore");
        }
        hazelcast = core.getInstance();
        pkCache = hazelcast.getMap(EJB_TIMER_CACHE_NAME);
        containerCache = hazelcast.getMap(EJB_TIMER_CONTAINER_CACHE_NAME);
        applicationCache = hazelcast.getMap(EJB_TIMER_APPLICAION_CACHE_NAME);
        serverName = core.getAttribute(core.getInstance().getCluster().getLocalMember().getUuid(), HazelcastCore.INSTANCE_ATTRIBUTE);
        this.ownerIdOfThisServer_ = serverName;
        this.domainName_ = core.getInstance().getConfig().getClusterName();
        super.enableRescheduleTimers();
    }

    private void removeTimers(Set<TimerPrimaryKey> timerIdsToRemove) {
        for (TimerPrimaryKey timerPrimaryKey : timerIdsToRemove) {
            removeTimer(pkCache.get(timerPrimaryKey.timerId));
        }
    }

    @Override
    protected void _createTimer(TimerPrimaryKey timerId, long containerId, long applicationId, Object timedObjectPrimaryKey, String server_name, Date initialExpiration, long intervalDuration, EJBTimerSchedule schedule, TimerConfig timerConfig) throws Exception {
        if (timerConfig.isPersistent()) {

            pkCache.put(timerId.timerId, new HZTimer(timerId, containerId, applicationId, timedObjectPrimaryKey, server_name, server_name, initialExpiration, intervalDuration, schedule, timerConfig));

            // add to container cache
            Set<TimerPrimaryKey> keysForContainer = containerCache.get(containerId);
            if (keysForContainer == null) {
                keysForContainer = new HashSet<>();
            }
            keysForContainer.add(timerId);
            containerCache.put(containerId, keysForContainer);

            // add to application cache
            Set<TimerPrimaryKey> keysForApp = applicationCache.get(applicationId);
            if (keysForApp == null) {
                keysForApp = new HashSet<>();
            }
            keysForApp.add(timerId);
            applicationCache.put(applicationId, keysForApp);

            TransactionManager tm = ejbContainerUtil.getTransactionManager();
            boolean localTx = tm.getTransaction() == null;

            if (localTx) {
                tm.begin();
            }

            addTimerSynchronization(null,
                    timerId.getTimerId(), initialExpiration,
                    containerId, ownerIdOfThisServer_, true);
            if (localTx) {
                tm.commit();
            }

        } else {
            addTimerSynchronization(null,
                    timerId.getTimerId(), initialExpiration,
                    containerId, ownerIdOfThisServer_, false);
        }
    }

    @Override
    public void destroyAllTimers(long applicationId) {

        // remove all timers
        Set<TimerPrimaryKey> timerIds = applicationCache.get(applicationId);

        if (timerIds == null || timerIds.isEmpty()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "No timers to be deleted for id: {0}", applicationId);
            }
            return;
        }

        for (TimerPrimaryKey timerId : timerIds) {
            pkCache.remove(timerId.timerId);
        }
        logger.log(Level.INFO, "Destroyed {0} timers for application {1}", new Object[]{timerIds.size(), applicationId});
        timerIds.clear();
        applicationCache.remove(applicationId);

    }

    @Override
    public void destroyTimers(long containerId) {
        Set<TimerPrimaryKey> timerIds = containerCache.get(containerId);

        if (timerIds == null || timerIds.isEmpty()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "No timers to be deleted for id: {0}", containerId);
            }
            return;
        }

        logger.log(Level.INFO, "Destroyed {0} timers for container {1}", new Object[]{timerIds.size(), containerId});
        timerIds.clear();
        applicationCache.put(containerId, timerIds);
    }

    @Override
    protected void cancelTimer(TimerPrimaryKey timerId) throws FinderException, Exception {
        // Check non-persistent timers first
        if (!cancelNonPersistentTimer(timerId)) {

            HZTimer timer = pkCache.get(timerId.timerId);
            if (timer != null) {

                TransactionManager tm = ejbContainerUtil.getTransactionManager();
                boolean localTx = tm.getTransaction() == null;
                if (localTx) {
                    tm.begin();
                }
                super.cancelTimerSynchronization(null, timerId,
                        timer.getContainerId(), timer.getOwnerId());
                if (localTx) {
                    tm.commit();
                }
            }
        }

    }

    @Override
    protected Serializable getInfo(TimerPrimaryKey timerId) throws FinderException {
        // Check non-persistent timers first
        if (isNonpersistent(timerId)) {
            return super.getInfo(timerId);
        }

        HZTimer timer = pkCache.get(timerId.timerId);

        if (timer == null) {
            throw new FinderException("Unable to find timer " + timerId);
        } else {
            return timer.getTimerConfig().getInfo();
        }
    }

    @Override
    protected Date getNextTimeout(TimerPrimaryKey timerId) throws FinderException {
        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            return _getNextTimeout(rt);
        }

        // It's a persistent timer
        HZTimer timer = getPersistentTimer(timerId);
        Date initialExpiration = timer.getInitialExpiration();
        long intervalDuration = timer.getIntervalDuration();
        EJBTimerSchedule ts = timer.getSchedule();

        Date nextTimeout = null;
        if (ts != null) {
            nextTimeout = getNextScheduledTimeout(ts);
            // The caller is responsible to return 0 or -1 for the time remaining....

        } else if (intervalDuration > 0) {
            nextTimeout = calcNextFixedRateExpiration(initialExpiration,
                    intervalDuration);
        } else {
            nextTimeout = initialExpiration;
        }

        return nextTimeout;
    }

    @Override
    protected void cancelTimersByKey(long containerId, Object primaryKey) {

        // Get *all* timers for this entity bean identity.  This includes
        // even timers *not* owned by this server instance, but that
        // are associated with the same entity bean and primary key.
        Set<TimerPrimaryKey> timers = containerCache.get(containerId);

        if (timers != null) {
            HashSet<HZTimer> timersToCancel = new HashSet<>();
            for (TimerPrimaryKey timer : timers) {
                HZTimer hzTimer = pkCache.get(timer.timerId);
                if (hzTimer != null && hzTimer.getTimedObjectPk().equals(primaryKey)) {
                    timersToCancel.add(hzTimer);
                }
            }

            for (HZTimer hZTimer : timersToCancel) {
                removeTimer(hZTimer);
            }
        }
    }

    @Override
    public void createSchedules(long containerId, long applicationId, Map<MethodDescriptor, List<ScheduledTimerDescriptor>> methodDescriptorSchedules, String server_name) {
        TransactionManager tm = ejbContainerUtil.getTransactionManager();
        try {
            tm.begin();
            Set<TimerPrimaryKey> keys = containerCache.get(containerId);

            if (keys == null || keys.isEmpty()) {
                // No timers owned by this EJB
                createSchedules(containerId, applicationId, methodDescriptorSchedules, null, server_name, false, true);
            }

            tm.commit();

        } catch (Exception e) {
            recoverAndCreateSchedulesError(e, tm);
        }
    }

    @Override
    public void createSchedulesOnServer(EjbDescriptor ejbDescriptor, String server_name) {
        Map<MethodDescriptor, List<ScheduledTimerDescriptor>> schedules = new HashMap<>();
        for (ScheduledTimerDescriptor schd : ejbDescriptor.getScheduledTimerDescriptors()) {
            MethodDescriptor method = schd.getTimeoutMethod();
            if (method != null && schd.getPersistent()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "... processing {0}", method);
                }

                List<ScheduledTimerDescriptor> list = schedules.get(method);
                if (list == null) {
                    list = new ArrayList<>();
                    schedules.put(method, list);
                }
                list.add(schd);
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "EJBTimerService - creating schedules for {0}", ejbDescriptor.getUniqueId());
        }
        createSchedules(ejbDescriptor.getUniqueId(), ejbDescriptor.getApplication().getUniqueId(), schedules, server_name);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "EJBTimerService - finished processing schedules for BEAN ID: {0}", ejbDescriptor.getUniqueId());
        }
    }

    @Override
    protected void expungeTimer(TimerPrimaryKey timerId, boolean removeTimerBean) {

        HZTimer timer = pkCache.get(timerId.timerId);
        if (timer != null) {
            removeTimer(timer);
        }

        // And finish in the superclass...
        super.expungeTimer(timerId, removeTimerBean);
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(Collection<Long> containerIds) {
        Collection<TimerPrimaryKey> result = super.getTimerIds(containerIds);
        for (Long containerId : containerIds) {
            Set<TimerPrimaryKey> colKeys = containerCache.get(containerId);
            if (colKeys != null) {
                result.addAll(colKeys);
            }
        }
        return result;
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(long containerId, Object timedObjectPrimaryKey) {
        // The results should include all timers for the given ejb
        // and/or primary key, including timers owned by other server instances.

        // @@@ Might want to consider cases where we can use
        // timer cache to avoid some database access in PE/SE, or
        // even in EE with the appropriate consistency tradeoff.
        Collection<TimerPrimaryKey> timerIdsForTimedObject = new HashSet<>();

        if (timedObjectPrimaryKey == null) {
            Set<TimerPrimaryKey> contKeys = containerCache.get(containerId);
            if (contKeys != null) {
                timerIdsForTimedObject.addAll(contKeys);
            }
        } else {

            Collection<TimerPrimaryKey> timersForTimedObject = containerCache.get(containerId);
            if (timersForTimedObject != null) {
                for (TimerPrimaryKey timer : timersForTimedObject) {
                    HZTimer hzTimer = pkCache.get(timer.timerId);
                    if (hzTimer != null && hzTimer.getTimedObjectPk().equals(timedObjectPrimaryKey)) {
                        timerIdsForTimedObject.add(timer);
                    }

                }
            }
        }

        // Add active non-persistent timer ids
        timerIdsForTimedObject.addAll(super.getTimerIds(containerId, null));

        return timerIdsForTimedObject;
    }

    @Override
    protected EJBTimerSchedule getTimerSchedule(TimerPrimaryKey timerId) throws FinderException {
        // Check non-persistent timers first
        EJBTimerSchedule ts = null;
        if (isNonpersistent(timerId)) {
            ts = super.getTimerSchedule(timerId);
        } else {

            // @@@ We can't assume this server instance owns the persistent timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.
            HZTimer timer = getPersistentTimer(timerId);
            ts = timer.getSchedule();
        }
        return ts;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    protected boolean isCancelledByAnotherInstance(RuntimeTimerState timerState) {
        if (timerState.isPersistent() && !isDas) {

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "For Timer :{0}: check the database to ensure that the timer is still  valid, before delivering the ejbTimeout call",
                        timerState.getTimerId());
            }

            if (!checkForTimerValidity(timerState.getTimerId())) {
                // The timer for which a ejbTimeout is about to be delivered
                // is not present in the database. This could happen in the
                // SE/EE case as other server instances (other than the owner)
                // could call a cancel on the timer - deleting the timer from
                // the database.
                // Also it is possible that the timer is now owned by some other
                // server instance
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean isPersistent(TimerPrimaryKey timerId) throws FinderException {
        // Check non-persistent timers first
        if (isNonpersistent(timerId)) {
            // Found and active
            return false;
        }

        // @@@ We can't assume this server instance owns the persistent timer
        // so always ask the database.  Investigate possible use of
        // timer cache for optimization.
        getPersistentTimer(timerId);
        // If we reached here, it means the timer is persistent
        return true;
    }

    @Override
    protected boolean isValidTimerForThisServer(TimerPrimaryKey timerId, RuntimeTimerState timerState) {
        boolean result = true;
        if (timerState.isPersistent()) {
            HZTimer timer = pkCache.get(timerId.timerId);
            if (timer == null || !timer.getMemberName().equals(serverName)) {
                result = false;

                removeLocalTimer(timer);
            }
        }
        return result;
    }

    static String [] listTimers(Collection<HZTimer> timers, String[] serverIds) {
        String result[] = new String[serverIds.length];

        // count all server ids
        HashMap<String, Long> counts = new HashMap<>();
        for (HZTimer timer : timers) {
            String serverName = timer.getMemberName();
            Long val = counts.get(serverName);
            if (val == null) {
                val = new Long(0);
            }
            counts.put(serverName, new Long(val.intValue() + 1));
        }

        // copy counts into the strings
        for (int i = 0; i < serverIds.length; i++) {
            Long count = counts.get(serverIds[i]);
            if (count != null) {
                result[i] = counts.get(serverIds[i]).toString();
            } else {
                result[i] = "0";
            }
        }
        return result;
    }

    @Override
    public String[] listTimers(String[] serverIds) {
        return listTimers(pkCache.values(), serverIds);
    }

    @Override
    public int migrateTimers(String fromOwnerId) {
        String ownerIdOfThisServer = getOwnerIdOfThisServer();

        if (fromOwnerId.equals(ownerIdOfThisServer)) {
            logger.log(Level.WARNING, "Attempt to migrate timers from {0} to itself",
                    ownerIdOfThisServer);
            throw new IllegalStateException("Attempt to migrate timers from " + ownerIdOfThisServer + " to itself");
        }

        logger.log(Level.INFO, "Beginning timer migration process from owner {0} to {1}", new Object[]{fromOwnerId, ownerIdOfThisServer});

        TransactionManager tm = ejbContainerUtil.getTransactionManager();

        HashMap<String, HZTimer> toRestore = new HashMap<>();
        int totalTimersMigrated = 0;

        for (String pk : pkCache.keySet()) {
            HZTimer hZTimer = pkCache.get(pk);
            if (hZTimer.getOwnerId().equals(fromOwnerId)) {
                toRestore.put(pk, hZTimer);
                hZTimer.setOwnerId(ownerIdOfThisServer);
                hZTimer.setMemberName(serverName);
            }
        }

        for (Entry<String, HZTimer> entry : toRestore.entrySet()) {
            pkCache.put(entry.getKey(), entry.getValue());
            totalTimersMigrated++;
        }

        if (totalTimersMigrated > 0) {

            boolean success = false;
            try {

                logger.log(Level.INFO, "Timer migration phase 1 complete. Changed ownership of {0} timers.  Now reactivating timers...", toRestore.size());

                _notifyContainers(toRestore.values());

                tm.begin();
                _restoreTimers(toRestore.values());
                success = true;

                // Inform fromServer that timers have been migrated and it needs to clear its local cache
                _notifyMigratedFromInstance(fromOwnerId);
            } catch (Exception e) {

                logger.log(Level.FINE, "timer restoration error", e);

                //Propogate any exceptions caught as part of the transaction
                EJBException ejbEx = createEJBException(e);
                throw ejbEx;

            } finally {
                // We're not modifying any state in this tx so no harm in
                // always committing.
                try {
                    tm.commit();
                } catch (Exception re) {
                    logger.log(Level.FINE, "timer migration error", re);

                    if (success) {
                        //Propogate any exceptions caught when trying to commit
                        //the transaction
                        EJBException ejbEx = createEJBException(re);
                        throw ejbEx;
                    }
                }
            }
        } else {
            logger.log(Level.INFO, "{0} has 0 timers in need of migration", fromOwnerId);
        }

        return totalTimersMigrated;
    }

    @Override
    protected Map<TimerPrimaryKey, Method> recoverAndCreateSchedules(long containerId, long applicationId, Map<Method, List<ScheduledTimerDescriptor>> schedules, boolean deploy) {
        Map<TimerPrimaryKey, Method> result = new HashMap<>();
        boolean lostCluster = false;
        Set<HZTimer> activeTimers = new HashSet<>();

        // get all timers for this container
        Set<TimerPrimaryKey> containerKeys = containerCache.get(containerId);
        Set<TimerPrimaryKey> deadKeys = new HashSet<>();
        Set<HZTimer> timers = new HashSet<>();
        if (containerKeys != null) {
            for (TimerPrimaryKey containerKey : containerKeys) {
                HZTimer timer = pkCache.get(containerKey.timerId);
                if (timer != null) {
                    if (timer.getMemberName().equals(this.serverName)) {
                        activeTimers.add(timer);
                    } else {
                        timers.add(timer); //on another instance in the cluster
                    }
                } else {
                    deadKeys.add(containerKey);
                }
            }
            if (!deadKeys.isEmpty()) {
                // clean out dead keys
                logger.log(Level.INFO, "Cleaning out {0} dead timer ids from Container Cache ", deadKeys.size());
                for (TimerPrimaryKey deadKey : deadKeys) {
                    containerKeys.remove(deadKey);
                }
                containerCache.put(containerId, containerKeys);
            }
        } else if (containerKeys == null && deploy == false) {
            // we are in trouble as we are not deploying but our keys are null
            // looks like we lost the whole cluster storage
            // recreate timers
            logger.log(Level.INFO, "Looks like we lost the data grid storage will recreate timers");
            lostCluster = true;
        }

        timers.addAll(_restoreTimers(activeTimers));

        if (timers.size() > 0) {
            logger.log(Level.FINE, "Found {0} persistent timers for containerId: {1}", new Object[]{timers.size(), containerId});
        }

        boolean schedulesExist = (schedules.size() > 0);
        for (HZTimer timer : timers) {
            EJBTimerSchedule ts = timer.getSchedule();
            if (ts != null && ts.isAutomatic() && schedulesExist) {
                Iterator<Map.Entry<Method, List<ScheduledTimerDescriptor>>> schedulesIterator = schedules.entrySet().iterator();
                while (schedulesIterator.hasNext()){
                    Map.Entry<Method, List<ScheduledTimerDescriptor>> entry = schedulesIterator.next();
                    Method m = entry.getKey();
                    if (m.getName().equals(ts.getTimerMethodName())
                            && m.getParameterTypes().length == ts.getMethodParamCount()) {
                        result.put(new TimerPrimaryKey(timer.getKey().getTimerId()), m);
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "@@@ FOUND existing schedule: {0} FOR method: {1}", new Object[]{ts.getScheduleAsString(), m});
                        }
                        schedulesIterator.remove();
                    }
                }
            }
        }



        try {
            if (!schedules.isEmpty()) {
                createSchedules(containerId, applicationId, schedules, result, serverName, true,
                    (deploy && isDas) || lostCluster);
            }
        } catch (Exception ex) {
            Logger.getLogger(HazelcastTimerStore.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    @Override
    protected boolean redeliverTimeout(RuntimeTimerState timerState) {
        return (timerState.getNumFailedDeliveries()
                < getMaxRedeliveries());
    }

    @Override
    protected void resetLastExpiration(TimerPrimaryKey timerId, RuntimeTimerState timerState
    ) {
        if (timerState.isPersistent()) {
            HZTimer timer = pkCache.get(timerId.timerId);
            if (null == timer) {
                return;
            }

            if (removeLocalTimer(timer)) {
                return;
            }

            Date now = new Date();
            timer.setLastExpiration(now);
            pkCache.put(timer.getKey().timerId, timer);

            // Since timer was successfully delivered, update
            // last delivery time in database if that option is
            // enabled.
            // @@@ add configuration for update-db-on-delivery
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Setting last expiration for periodic timer {0} to {1}", new Object[]{timerState, now});
            }
        }
    }

    @Override
    protected void resetEJBTimers(String target) {
        if (target == null) {
            // target is null when accessed from the BaseContainer on load, i.e. where timers are running
            logger.log(Level.INFO, "==> Restoring Timers ... ");
            if (restoreEJBTimers()) {
                logger.log(Level.INFO, "<== ... Timers Restored.");
            }
        }
    }

    @Override
    protected boolean stopOnFailure() {
        return false;
    }

    @Override
    protected boolean timerExists(TimerPrimaryKey timerId) {
        boolean exists = super.timerExists(timerId);

        // Check persistent timers only if non-persistent is not found
        if (!exists) {

            // @@@ We can't assume this server instance owns the persistent timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.
            HZTimer timer = pkCache.get(timerId.timerId);
            if (timer != null) {
                // Make sure timer hasn't been cancelled within the current tx.
                exists = true;
            }
        }

        return exists;
    }

    @Override
    protected void stopTimers(long containerId) {
        super.stopTimers(containerId);
        stopTimers(containerCache.get(containerId));
    }

    private HZTimer getPersistentTimer(TimerPrimaryKey timerId) throws FinderException {
        HZTimer result = pkCache.get(timerId.timerId);
        if (result == null) {
            throw new FinderException("Unable to find timer " + timerId);
        }
        return result;
    }

    private void removeTimer(HZTimer timer) {
        pkCache.remove(timer.getKey().timerId);

        Set<TimerPrimaryKey> keys = applicationCache.get(timer.getApplicationId());
        if (keys != null) {
            keys.remove(timer.getKey());
            applicationCache.put(timer.getApplicationId(), keys);
        }

        keys = containerCache.get(timer.getContainerId());
        if (keys != null) {
            keys.remove(timer.getKey());
            containerCache.put(timer.getContainerId(), keys);
        }
    }

    /**
     * Common code for exception processing in recoverAndCreateSchedules
     */
    private void recoverAndCreateSchedulesError(Exception e, TransactionManager tm) {
        logger.log(Level.WARNING, "Timer restore or schedule creation error", e);

        try {
            tm.rollback();
        } catch (Exception re) {
            logger.log(Level.FINE, "Timer restore or schedule creation rollback error", re);
        }

        //Propagate the exception caught as an EJBException
        EJBException ejbEx = createEJBException(e);
        throw ejbEx;
    }

    private boolean checkForTimerValidity(TimerPrimaryKey timerId) {

        // check the timer still exists and is owned by me
        boolean result = false;

        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(HazelcastTimerStore.class.getClassLoader());
        HZTimer timer = pkCache.get(timerId.timerId);
        if (timer != null && timer.getMemberName().equals(this.serverName)) {
            result = true;
        }
        Thread.currentThread().setContextClassLoader(cl);
        return result;
    }

    /**
     * The portion of timer migration that notifies containers about automatic
     * timers being migrated to this instance
     */
    private void _notifyContainers(Collection<HZTimer> timers) {
        for (HZTimer timer : timers) {
            EJBTimerSchedule ts = timer.getSchedule();
            if (ts != null && ts.isAutomatic()) {
                addToSchedules(timer.getContainerId(), timer.getKey(), ts);
            }
        }
    }

    private void restoreTimers() throws Exception {

        // Optimization.  Skip timer restoration if there aren't any
        // applications with timed objects deployed.
        if (totalTimedObjectsInitialized_ == 0) {
            return;
        }

        _restoreTimers(findActiveTimersOwnedByThisServer());
    }

    /**
     * The portion of timer restoration that deals with registering the JDK
     * timer tasks and checking for missed expirations.
     *
     * @return the Set of restored timers
     */
    private Collection<HZTimer> _restoreTimers(Collection<HZTimer> timersEligibleForRestoration) {

        // Do timer restoration in two passes.  The first pass updates
        // the timer cache with each timer.  The second pass schedules
        // the JDK timer tasks.
        Map<RuntimeTimerState, Date> timersToRestore = new HashMap<>();
        Set<TimerPrimaryKey> timerIdsToRemove = new HashSet<>();
        Set<HZTimer> result = new HashSet<>();

        for (HZTimer timer : timersEligibleForRestoration) {

            TimerPrimaryKey timerId = timer.getKey();
            if (getTimerState(timerId) != null) {
                // Already restored. Add it to the result but do nothing else.
                logger.log(Level.FINE, "@@@ Timer already restored: {0}", timer);
                result.add(timer);
                continue;
            }

            long containerId = timer.getContainerId();

            // Timer might refer to an obsolete container.
            BaseContainer container = getContainer(containerId);
            if (container != null) {

                // Update applicationId if it is null (from previous version)
                long appid = timer.getApplicationId();
                if (appid == 0) {
                    timer.setApplicationId(container.getApplicationId());
                }
                if (!timer.getMemberName().equals(serverName)) {
                    timer.setMemberName(serverName);
                }
                //  End update

                Date initialExpiration = timer.getInitialExpiration();

                // Create an instance of RuntimeTimerState.
                // Only access timedObjectPrimaryKey if timed object is
                // an entity bean.  That allows us to lazily load the underlying
                // blob for stateless session and message-driven bean timers.
                Object timedObjectPrimaryKey = null;
                if (container.getContainerType() == BaseContainer.ContainerType.ENTITY) {
                    timedObjectPrimaryKey = timer.getTimedObjectPk();
                }

                RuntimeTimerState timerState = new RuntimeTimerState(timerId, initialExpiration,
                        timer.getIntervalDuration(), container,
                        timedObjectPrimaryKey,
                        timer.getSchedule(),
                        // Don't need to store the info ref for persistent timer
                        null, true);

                timerCache_.addTimer(timerId, timerState);

                // If a single-action timer is still in the database it never
                // successfully delivered, so always reschedule a timer task
                // for it.  For periodic timers, we use the last known
                // expiration time to decide whether we need to fire one
                // ejbTimeout to make up for any missed ones.
                Date expirationTime = initialExpiration;
                Date now = new Date();

                if (timerState.isPeriodic()) {
                    // lastExpiration time, or null if we either aren't
                    // tracking last expiration or an expiration hasn't
                    // occurred yet for this timer.
                    Date lastExpiration = timer.getLastExpiration();
                    EJBTimerSchedule ts = timer.getSchedule();

                    // @@@ need to handle case where last expiration time
                    // is not stored in database.  This will be the case
                    // when we add configuration for update-db-on-delivery.
                    // However, for now assume we do update the db on each
                    // ejbTimeout.  Therefore, if (lastExpirationTime == null),
                    // it means the timer didn't successfully complete any
                    // timer expirations.
                    if ((lastExpiration == null)
                            && now.after(initialExpiration)) {

                        if (!timerState.isExpired()) {
                            // This timer didn't even expire one time.
                            logger.log(Level.INFO, "Rescheduling missed expiration for periodic timer {0}. Timer expirations should  have been delivered starting at {1}",
                                    new Object[]{timerState, initialExpiration});
                        }

                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.
                    } else if ((lastExpiration != null)
                            && ((ts != null && ts.getNextTimeout(lastExpiration).getTimeInMillis()
                            < now.getTime())
                            || ((ts == null) && now.getTime() - lastExpiration.getTime()
                            > timer.getIntervalDuration()))) {

                        // Schedule-based timer is periodic
                        logger.log(Level.INFO, "Rescheduling missed expiration for periodic timer {0}.  Last timer expiration occurred at {1}",
                                new Object[]{timerState, lastExpiration});

                        // Timer expired at least once and at least one
                        // missed expiration has occurred.
                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.
                    } else {

                        // In this case, at least one expiration has occurred
                        // but that was less than one period ago so there were
                        // no missed expirations.
                        expirationTime = calcNextFixedRateExpiration(timerState);
                    }

                } else // single-action timer
                if (now.after(initialExpiration)) {
                    logger.log(Level.INFO, "Rescheduling missed expiration for single-action timer {0}. Timer expiration should  have been delivered at {1}",
                            new Object[]{timerState, initialExpiration});
                }

                if (expirationTime == null) {
                    // Schedule-based timer will never expire again - remove it.
                    logger.log(Level.INFO, "Removing schedule-based timer {0} that will never expire again", timerState);
                    timerIdsToRemove.add(timerId);
                } else {
                    timersToRestore.put(timerState, expirationTime);
                    result.add(timer);
                }

            } else {
                // Timed object's container no longer exists - remember its id.
                logger.log(Level.FINE, "Skipping timer {0} for container that is not up: {1}", new Object[]{timerId, containerId});
            }
        } // End -- for each active timer

        removeTimers(timerIdsToRemove);

        for (Map.Entry<RuntimeTimerState, Date> next : timersToRestore.entrySet()) {
            RuntimeTimerState nextTimer = next.getKey();
            TimerPrimaryKey timerId = nextTimer.getTimerId();
            Date expiration = next.getValue();
            scheduleTask(timerId, expiration);
            logger.log(Level.FINE, "EJBTimerService.restoreTimers(), scheduling timer {0}", nextTimer);
        }

        logger.log(Level.FINE, "DONE EJBTimerService.restoreTimers()");
        return result;
    }

    /**
     * The portion of timer restoration that deals with registering the JDK
     * timer tasks and checking for missed expirations.
     *
     * @return the Set of restored timers
     */
    private Set<HZTimer> _restoreTimers(Set<HZTimer> timersEligibleForRestoration) {

        // Do timer restoration in two passes.  The first pass updates
        // the timer cache with each timer.  The second pass schedules
        // the JDK timer tasks.
        Map<RuntimeTimerState, Date> timersToRestore = new HashMap<>();
        Set<TimerPrimaryKey> timerIdsToRemove = new HashSet<>();
        Set<HZTimer> result = new HashSet<>();

        for (HZTimer timer : timersEligibleForRestoration) {

            TimerPrimaryKey timerId = timer.getKey();
            if (getTimerState(timerId) != null) {
                // Already restored. Add it to the result but do nothing else.
                logger.log(Level.FINE, "@@@ Timer already restored: {0}", timer);
                result.add(timer);
                continue;
            }

            long containerId = timer.getContainerId();

            // Timer might refer to an obsolete container.
            BaseContainer container = getContainer(containerId);
            if (container != null) {

                // Update applicationId if it is null (from previous version)
                long appid = timer.getApplicationId();
                if (appid == 0) {
                    timer.setApplicationId(container.getApplicationId());
                }
                //  End update

                Date initialExpiration = timer.getInitialExpiration();

                // Create an instance of RuntimeTimerState.
                // Only access timedObjectPrimaryKey if timed object is
                // an entity bean.  That allows us to lazily load the underlying
                // blob for stateless session and message-driven bean timers.
                Object timedObjectPrimaryKey = null;
                if (container.getContainerType() == BaseContainer.ContainerType.ENTITY) {
                    timedObjectPrimaryKey = timer.getTimedObjectPk();
                }

                RuntimeTimerState timerState = new RuntimeTimerState(timerId, initialExpiration,
                        timer.getIntervalDuration(), container,
                        timedObjectPrimaryKey,
                        timer.getSchedule(),
                        // Don't need to store the info ref for persistent timer
                        null, true);

                timerCache_.addTimer(timerId, timerState);

                // If a single-action timer is still in the database it never
                // successfully delivered, so always reschedule a timer task
                // for it.  For periodic timers, we use the last known
                // expiration time to decide whether we need to fire one
                // ejbTimeout to make up for any missed ones.
                Date expirationTime = initialExpiration;
                Date now = new Date();

                if (timerState.isPeriodic()) {
                    // lastExpiration time, or null if we either aren't
                    // tracking last expiration or an expiration hasn't
                    // occurred yet for this timer.
                    Date lastExpiration = timer.getLastExpiration();
                    EJBTimerSchedule ts = timer.getSchedule();

                    // @@@ need to handle case where last expiration time
                    // is not stored in database.  This will be the case
                    // when we add configuration for update-db-on-delivery.
                    // However, for now assume we do update the db on each
                    // ejbTimeout.  Therefore, if (lastExpirationTime == null),
                    // it means the timer didn't successfully complete any
                    // timer expirations.
                    if ((lastExpiration == null)
                            && now.after(initialExpiration)) {

                        if (!timerState.isExpired()) {
                            // This timer didn't even expire one time.
                            logger.log(Level.INFO, "Rescheduling missed expiration for periodic timer {0}. "
                                    + "Timer expirations should  have been delivered starting at {1}", new Object[]{timerState, initialExpiration});
                        }

                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.
                    } else if ((lastExpiration != null)
                            && ((ts != null && ts.getNextTimeout(lastExpiration).getTimeInMillis()
                            < now.getTime())
                            || ((ts == null) && now.getTime() - lastExpiration.getTime()
                            > timer.getIntervalDuration()))) {

                        // Schedule-based timer is periodic
                        logger.log(Level.INFO, "Rescheduling missed expiration for periodic timer {0}.  Last timer expiration occurred at {1}",
                                new Object[]{timerState, lastExpiration});

                        // Timer expired at least once and at least one
                        // missed expiration has occurred.
                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.
                    } else {

                        // In this case, at least one expiration has occurred
                        // but that was less than one period ago so there were
                        // no missed expirations.
                        expirationTime
                                = calcNextFixedRateExpiration(timerState);
                    }

                } else // single-action timer
                if (now.after(initialExpiration)) {
                    logger.log(Level.INFO, "Rescheduling missed expiration for single-action timer {0}. Timer expiration should  have been delivered at {1}",
                            new Object[]{timerState, initialExpiration});
                }

                if (expirationTime == null) {
                    // Schedule-based timer will never expire again - remove it.
                    logger.log(Level.INFO, "Removing schedule-based timer {0} that will never expire again", timerState);
                    timerIdsToRemove.add(timerId);
                } else {
                    timersToRestore.put(timerState, expirationTime);
                    result.add(timer);
                }

            } else {
                // Timed object's container no longer exists - remember its id.
                logger.log(Level.FINE, "Skipping timer {0} for container that is not up: {1}", new Object[]{timerId, containerId});
            }
        } // End -- for each active timer

        removeTimers(timerIdsToRemove);

        for (Map.Entry<RuntimeTimerState, Date> next : timersToRestore.entrySet()) {
            RuntimeTimerState nextTimer = next.getKey();
            TimerPrimaryKey timerId = nextTimer.getTimerId();
            Date expiration = next.getValue();
            scheduleTask(timerId, expiration);
            logger.log(Level.FINE, "EJBTimerService.restoreTimers(), scheduling timer {0}", nextTimer);
        }

        logger.log(Level.FINE, "DONE EJBTimerService.restoreTimers()");
        return result;
    }

    private Collection<HZTimer> findActiveTimersOwnedByThisServer() {
        HashSet<HZTimer> result = new HashSet<>();
        for (HZTimer timer : pkCache.values()) {
            if (timer.getMemberName().equals(this.serverName)) {
                result.add(timer);
            }
        }
        return result;
    }

    private boolean restoreEJBTimers() {
        boolean rc = false;
        try {
            if (totalTimedObjectsInitialized_ > 0) {
                restoreTimers();
                rc = true;
            } else {
                int s = (findActiveTimersOwnedByThisServer()).size();
                if (s > 0) {
                    logger.log(Level.INFO, "[{0}] EJB Timers owned by this server will be restored when timeout beans are loaded", s);
                } else {
                    logger.log(Level.INFO, "There are no EJB Timers owned by this server");
                }
                rc = true;
            }
        } catch (Exception ex) {
            // Problem accessing timer service so disable it.
            EJBTimerService.setPersistentTimerService(null);

            logger.log(Level.WARNING, "ejb.timer_service_init_error", ex);

        }
        return rc;
    }

    @Override
    public void memberAdded(MemberEvent event) {
        //do nothing
    }

    @Override
    public void memberRemoved(MemberEvent event) {
        FencedLock hazelcastLock = hazelcast.getCPSubsystem().getLock("EJB-TIMER-LOCK");
        hazelcastLock.lock();
        try {
            Collection<HZTimer> allTimers = pkCache.values();
            Collection<HZTimer> removedTimers = new HashSet<>();
            for (HZTimer timer : allTimers) {
                if (timer.getMemberName().equals(event.getServer())) {
                    removedTimers.add(timer);
                }
            }



            if (!removedTimers.isEmpty()) {
                logger.log(Level.INFO, "==> Restoring Timers ... ");
                Collection<HZTimer> restored = _restoreTimers(removedTimers);
                for (HZTimer timer : restored) {
                    pkCache.put(timer.getKey().getTimerId(), timer);
                }
                logger.log(Level.INFO, "<== ... Timers Restored.");
            }
        } finally {
            hazelcastLock.unlock();
        }
    }

    /**
     * Remove all local timers that are no longer owned by this instance.
     */
    private void removeLocalTimers() {
        Collection<HZTimer> allTimers = pkCache.values();
        for (HZTimer timer : allTimers) {
            removeLocalTimer(timer);
        }
    }

    /**
     * Removes a given local timer if it is no longer owned by this instance
     * @param timer The timer to potentially be removed.
     * @return True if a timer was removed.
     */
    private boolean removeLocalTimer(HZTimer timer) {
        boolean result = false;
        TimerPrimaryKey timerId = timer.getKey();

        if (!timer.getOwnerId().equals(getOwnerIdOfThisServer()) && getTimerState(timerId) != null) {
            logger.log(Level.INFO,
                    "The timer (" + timerId + ") is now owned by (" + timer.getOwnerId() + "). Removing from " +
                            "local cache");

            // We don't want to expunge it from the Hazelcast caches since it's a distributed cache,
            // so only expunge from local cache
            super.expungeTimer(timerId, false);
            result = true;
        }

        return result;
    }

    /**
     * Sends an {@link EjbTimerEvent} across the DataGrid with the {@link InstanceDescriptor} of the instance from
     * which the EJB timers were migrated from, to allow other instances to react to the migration.
     * @param fromOwnerId The {@link InstanceDescriptor} of the instance from which the timers were migrated from
     */
    private void _notifyMigratedFromInstance(String fromOwnerId) {
        PayaraCluster cluster = Globals.getDefaultBaseServiceLocator().getService(PayaraCluster.class);
        PayaraInstanceImpl instance = Globals.getDefaultBaseServiceLocator().getService(PayaraInstanceImpl.class);

        if (cluster == null || instance == null) {
            return;
        }

        // Get the InstanceDescriptor of the fromOwnerId instance
        InstanceDescriptor fromOwnerInstanceDescriptor = null;
        for (InstanceDescriptor instanceDescriptor : instance.getClusteredPayaras()) {
            if (instanceDescriptor.getInstanceName().equals(fromOwnerId)) {
                fromOwnerInstanceDescriptor = instanceDescriptor;
                break;
            }
        }

        if (fromOwnerInstanceDescriptor == null) {
            return;
        }

        EjbTimerEvent ejbTimerEvent = new EjbTimerEvent(EjbTimerEvent.Event.MIGRATED, fromOwnerInstanceDescriptor);
        ClusterMessage<EjbTimerEvent> message = new ClusterMessage<>(ejbTimerEvent);
        cluster.getEventBus().publish(EjbTimerEvent.EJB_TIMER_EVENTS_TOPIC, message);
    }

    @Override
    public void receiveMessage(ClusterMessage<EjbTimerEvent> ejbTimerEvent) {
        if (ejbTimerEvent.getPayload().getEventType().equals(EjbTimerEvent.Event.MIGRATED)) {
            PayaraInstanceImpl instance = Globals.getDefaultBaseServiceLocator().getService(PayaraInstanceImpl.class);
            if (instance == null) {
                return;
            }

            if (ejbTimerEvent.getPayload().getId().equals(instance.getLocalDescriptor())) {
                removeLocalTimers();
            }
        }
    }
}
