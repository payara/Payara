/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import com.hazelcast.core.IMap;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.NonPersistentEJBTimerService;
import com.sun.ejb.containers.RuntimeTimerState;
import com.sun.ejb.containers.TimerPrimaryKey;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.logging.LogDomains;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.TimerConfig;
import javax.transaction.TransactionManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;

/**
 *
 * @author steve
 */
public class HazelcastTimerStore extends NonPersistentEJBTimerService {

    private static final String EJB_TIMER_CACHE_NAME = "HZEjbTmerCache";
    private static final String EJB_TIMER_CONTAINER_CACHE_NAME = "HZEjbTmerContainerCache";
    private static final String EJB_TIMER_APPLICAION_CACHE_NAME = "HZEjbTmerApplicationCache";

    private final IMap pkCache;
    private final IMap containerCache;
    private final IMap applicationCache;
    private final String serverName;

    private static final Logger logger
            = LogDomains.getLogger(HazelcastTimerStore.class, LogDomains.EJB_LOGGER);

    static void init(HazelcastCore core) {
        try {
                EJBTimerService.setPersistentTimerService(new HazelcastTimerStore(core));
        } catch (Exception ex) {
            Logger.getLogger(HazelcastTimerStore.class.getName()).log(Level.WARNING, "Problem when initialising Timer Store", ex);
        }
    }

    public HazelcastTimerStore(HazelcastCore core) throws Exception {
        pkCache = core.getInstance().getMap(EJB_TIMER_CACHE_NAME);
        containerCache = core.getInstance().getMap(EJB_TIMER_CONTAINER_CACHE_NAME);
        applicationCache = core.getInstance().getMap(EJB_TIMER_APPLICAION_CACHE_NAME);
        serverName = core.getInstance().getCluster().getLocalMember().getStringAttribute(HazelcastCore.INSTANCE_ATTRIBUTE);
        this.ownerIdOfThisServer_ = serverName;
        this.domainName_ = core.getInstance().getConfig().getGroupConfig().getName();
    }

    private void removeTimers(Set<TimerPrimaryKey> timerIdsToRemove) {
        for (TimerPrimaryKey timerPrimaryKey : timerIdsToRemove) {
            removeTimer((HZTimer) pkCache.get(timerPrimaryKey.timerId));
        }
    }

    @Override
    protected void _createTimer(TimerPrimaryKey timerId, long containerId, long applicationId, Object timedObjectPrimaryKey, String server_name, Date initialExpiration, long intervalDuration, EJBTimerSchedule schedule, TimerConfig timerConfig) throws Exception {
        if (timerConfig.isPersistent()) {
            
            pkCache.put(timerId.timerId, new HZTimer(timerId, containerId, applicationId, timedObjectPrimaryKey, this.serverName, ownerIdOfThisServer_, initialExpiration, intervalDuration, schedule, timerConfig));

            // add to container cache
            HashSet<TimerPrimaryKey> keysForContainer = (HashSet<TimerPrimaryKey>) containerCache.get(containerId);
            if (keysForContainer == null) {
                keysForContainer = new HashSet<>();
            }
            keysForContainer.add(timerId);
            containerCache.put(containerId, keysForContainer);

            // add to application cache
            HashSet<TimerPrimaryKey> keysForApp = (HashSet<TimerPrimaryKey>) applicationCache.get(applicationId);
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
        Set<TimerPrimaryKey> timerIds = (Set<TimerPrimaryKey>) applicationCache.get(applicationId);

        if (timerIds == null || timerIds.isEmpty()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "No timers to be deleted for id: " + applicationId);
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
        Set<TimerPrimaryKey> timerIds = (Set<TimerPrimaryKey>) containerCache.get(containerId);

        if (timerIds == null || timerIds.isEmpty()) {
            if (logger.isLoggable(Level.INFO)) {
                logger.log(Level.INFO, "No timers to be deleted for id: " + containerId);
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

            HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
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
        if (!super.isPersistent(timerId)) {
            return super.getInfo(timerId);
        }

        HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);

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
        Collection<TimerPrimaryKey> timers = (Collection<TimerPrimaryKey>) containerCache.get(containerId);

        if (timers != null) {
            HashSet<HZTimer> timersToCancel = new HashSet<>();
            for (TimerPrimaryKey timer : timers) {
                HZTimer hzTimer = (HZTimer) pkCache.get(((TimerPrimaryKey)primaryKey).timerId);
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
            Collection<TimerPrimaryKey> keys = (Collection<TimerPrimaryKey>) containerCache.get(containerId);

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
        Map<MethodDescriptor, List<ScheduledTimerDescriptor>> schedules
                = new HashMap<MethodDescriptor, List<ScheduledTimerDescriptor>>();
        for (ScheduledTimerDescriptor schd : ejbDescriptor.getScheduledTimerDescriptors()) {
            MethodDescriptor method = schd.getTimeoutMethod();
            if (method != null && schd.getPersistent()) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "... processing " + method);
                }

                List<ScheduledTimerDescriptor> list = schedules.get(method);
                if (list == null) {
                    list = new ArrayList<ScheduledTimerDescriptor>();
                    schedules.put(method, list);
                }
                list.add(schd);
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "EJBTimerService - creating schedules for " + ejbDescriptor.getUniqueId());
        }
        createSchedules(ejbDescriptor.getUniqueId(), ejbDescriptor.getApplication().getUniqueId(), schedules, server_name);

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "EJBTimerService - finished processing schedules for BEAN ID: " + ejbDescriptor.getUniqueId());
        }
    }

    @Override
    protected void expungeTimer(TimerPrimaryKey timerId, boolean removeTimerBean) {

        HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
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
            Collection<TimerPrimaryKey> colKeys = (Collection<TimerPrimaryKey>) containerCache.get(containerId);
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
        Collection<TimerPrimaryKey> timerIdsForTimedObject
                = new HashSet<TimerPrimaryKey>();

        if (timedObjectPrimaryKey == null) {
            Collection<TimerPrimaryKey> contKeys = (Collection<TimerPrimaryKey>) containerCache.get(containerId);
            if (contKeys != null) {
                timerIdsForTimedObject.addAll(contKeys);
            }
        } else {

            Collection<TimerPrimaryKey> timersForTimedObject = (Collection<TimerPrimaryKey>) containerCache.get(containerId);
            if (timersForTimedObject != null) {
                for (TimerPrimaryKey timer : timersForTimedObject) {
                    HZTimer hzTimer = (HZTimer) pkCache.get(timer.timerId);
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
        if (!super.isPersistent(timerId)) {
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
                logger.log(Level.FINE, "For Timer :" + timerState.getTimerId()
                        + ": check the database to ensure that the timer is still "
                        + " valid, before delivering the ejbTimeout call");
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
        if (!super.isPersistent(timerId)) {
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
            HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
            if (timer == null || !timer.getMemberName().equals(serverName)) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public String[] listTimers(String[] serverIds) {
        String result[] = new String[serverIds.length];

        // count all server ids
        HashMap<String, Long> counts = new HashMap<>();
        for (Object timer : pkCache.values()) {
            HZTimer hzTimer = (HZTimer) timer;
            String serverName = hzTimer.getMemberName();
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
    public int migrateTimers(String fromOwnerId) {
        String ownerIdOfThisServer = getOwnerIdOfThisServer();

        if (fromOwnerId.equals(ownerIdOfThisServer)) {
            /// Error. The server from which timers are being
            // migrated should never be up and running OR receive this
            // notification.
            logger.log(Level.WARNING, "Attempt to migrate timers from "
                    + "an active server instance " + ownerIdOfThisServer);
            throw new IllegalStateException("Attempt to migrate timers from "
                    + " an active server instance "
                    + ownerIdOfThisServer);
        }

        logger.log(Level.INFO, "Beginning timer migration process from "
                + "owner " + fromOwnerId + " to " + ownerIdOfThisServer);

        TransactionManager tm = ejbContainerUtil.getTransactionManager();

        HashMap<String, HZTimer> toRestore = new HashMap<>();
        int totalTimersMigrated = 0;

        for (String pk : (Collection<String>) pkCache.keySet()) {
            HZTimer hZTimer = (HZTimer) pkCache.get(pk);
            if (hZTimer.getOwnerId().equals(fromOwnerId)) {
                toRestore.put(pk, hZTimer);
                hZTimer.setOwnerId(ownerIdOfThisServer);
                hZTimer.setMemberName(serverName);
            }
        }

        for (String pk : toRestore.keySet()) {
            pkCache.put(pk, toRestore.get(pk));
            totalTimersMigrated++;
        }

// XXX if( totalTimersMigrated  == toRestore.size() ) { XXX ???
        if (totalTimersMigrated > 0) {

            boolean success = false;
            try {

                logger.log(Level.INFO, "Timer migration phase 1 complete. "
                        + "Changed ownership of " + toRestore.size()
                        + " timers.  Now reactivating timers...");

                _notifyContainers(toRestore.values());

                tm.begin();
                _restoreTimers(toRestore.values());
                success = true;

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
            logger.log(Level.INFO, fromOwnerId + " has 0 timers in need "
                    + "of migration");
        }

        return totalTimersMigrated;
    }

    @Override
    protected Map<TimerPrimaryKey, Method> recoverAndCreateSchedules(long containerId, long applicationId, Map<Method, List<ScheduledTimerDescriptor>> schedules, boolean deploy) {
        Map<TimerPrimaryKey, Method> result = new HashMap<TimerPrimaryKey, Method>();

        Set<HZTimer> activeTimers = new HashSet<>();

        // get all timers for this container
        Collection<TimerPrimaryKey> containerKeys = (Collection<TimerPrimaryKey>) containerCache.get(containerId);
        Collection<TimerPrimaryKey> deadKeys = new HashSet<>();
        if (containerKeys != null) {
            for (TimerPrimaryKey containerKey : containerKeys) {
                HZTimer timer = (HZTimer) pkCache.get(containerKey.timerId);
                if (timer != null && timer.getMemberName().equals(this.serverName)) {
                    activeTimers.add(timer);
                } else if (timer == null) {
                    deadKeys.add(containerKey);
                }
            }
            if (!deadKeys.isEmpty()) {
                // clean out dead keys
                logger.info("Cleaning out " + deadKeys.size() + " dead timer ids from Container Cache ");
                for (TimerPrimaryKey deadKey : deadKeys) {
                    containerKeys.remove(deadKey);
                }
                containerCache.put(containerId, containerKeys);
            }
        } else if (containerKeys == null && deploy == false) {
            // we are in trouble as we are not deploying but our keys are null
            // looks like we lost the whole cluster storage
            // recreate timers
            deploy = true;
        }

        Set<HZTimer> timers = _restoreTimers(activeTimers);

        if (timers.size() > 0) {
            logger.log(Level.FINE, "Found " + timers.size()
                    + " persistent timers for containerId: " + containerId);
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
                            logger.log(Level.FINE, "@@@ FOUND existing schedule: "
                                    + ts.getScheduleAsString() + " FOR method: " + m);
                        }
                        schedulesIterator.remove();
                    }
                }
            }
        }

        
        
        try {
            if (!schedules.isEmpty()) {
                createSchedules(containerId, applicationId, schedules, result, serverName, true,
                    (deploy && isDas));
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
            HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
            if (null == timer) {
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
                logger.log(Level.FINE,
                        "Setting last expiration "
                        + " for periodic timer " + timerState
                        + " to " + now);
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
            HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
            if (timer != null) {
                // Make sure timer hasn't been cancelled within the current tx.
                exists = true;
            }
        }

        return exists;
    }

    @Override
    protected void stopTimers(long containerId
    ) {
        super.stopTimers(containerId);
        stopTimers((Set<TimerPrimaryKey>) containerCache.get(containerId));
    }

    private HZTimer getPersistentTimer(TimerPrimaryKey timerId) throws FinderException {
        HZTimer result = (HZTimer) pkCache.get(timerId.timerId);
        if (result == null) {
            throw new FinderException("Unable to find timer " + timerId);
        }
        return result;
    }

    private void removeTimer(HZTimer timer) {
        pkCache.remove(timer.getKey().timerId);

        Collection<TimerPrimaryKey> keys = (Collection<TimerPrimaryKey>) applicationCache.get(timer.getApplicationId());
        if (keys != null) {
            keys.remove(timer.getKey());
            applicationCache.put(timer.getApplicationId(), keys);
        }

        keys = (Collection<TimerPrimaryKey>) containerCache.get(timer.getContainerId());
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
        HZTimer timer = (HZTimer) pkCache.get(timerId.timerId);
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
        Map timersToRestore = new HashMap();
        Set<TimerPrimaryKey> timerIdsToRemove = new HashSet<>();
        Set<HZTimer> result = new HashSet<HZTimer>();

        for (HZTimer timer : timersEligibleForRestoration) {

            TimerPrimaryKey timerId = timer.getKey();
            if (getTimerState(timerId) != null) {
                // Already restored. Add it to the result but do nothing else.
                logger.log(Level.FINE, "@@@ Timer already restored: " + timer);
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
                            logger.log(Level.INFO,
                                    "Rescheduling missed expiration for "
                                    + "periodic timer "
                                    + timerState + ". Timer expirations should "
                                    + " have been delivered starting at "
                                    + initialExpiration);
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
                        logger.log(Level.INFO,
                                "Rescheduling missed expiration for "
                                + "periodic timer "
                                + timerState + ".  Last timer expiration "
                                + "occurred at " + lastExpiration);

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
                    logger.log(Level.INFO,
                            "Rescheduling missed expiration for "
                            + "single-action timer "
                            + timerState + ". Timer expiration should "
                            + " have been delivered at "
                            + initialExpiration);
                }

                if (expirationTime == null) {
                    // Schedule-based timer will never expire again - remove it.
                    logger.log(Level.INFO,
                            "Removing schedule-based timer " + timerState
                            + " that will never expire again");
                    timerIdsToRemove.add(timerId);
                } else {
                    timersToRestore.put(timerState, expirationTime);
                    result.add(timer);
                }

            } else {
                // Timed object's container no longer exists - remember its id.
                logger.log(Level.FINE,
                        "Skipping timer " + timerId
                        + " for container that is not up: " + containerId);
            }
        } // End -- for each active timer

        if (timerIdsToRemove.size() > 0) {
            for (HZTimer hZTimer : result) {

            }
            removeTimers(timerIdsToRemove);
        }

        for (Iterator entries = timersToRestore.entrySet().iterator();
                entries.hasNext();) {
            Map.Entry next = (Map.Entry) entries.next();
            RuntimeTimerState nextTimer = (RuntimeTimerState) next.getKey();
            TimerPrimaryKey timerId = nextTimer.getTimerId();
            Date expiration = (Date) next.getValue();
            scheduleTask(timerId, expiration);
            logger.log(Level.FINE,
                    "EJBTimerService.restoreTimers(), scheduling timer "
                    + nextTimer);
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
        Map timersToRestore = new HashMap();
        Set<TimerPrimaryKey> timerIdsToRemove = new HashSet<>();
        Set<HZTimer> result = new HashSet<HZTimer>();

        for (HZTimer timer : timersEligibleForRestoration) {

            TimerPrimaryKey timerId = timer.getKey();
            if (getTimerState(timerId) != null) {
                // Already restored. Add it to the result but do nothing else.
                logger.log(Level.FINE, "@@@ Timer already restored: " + timer);
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
                            logger.log(Level.INFO,
                                    "Rescheduling missed expiration for "
                                    + "periodic timer "
                                    + timerState + ". Timer expirations should "
                                    + " have been delivered starting at "
                                    + initialExpiration);
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
                        logger.log(Level.INFO,
                                "Rescheduling missed expiration for "
                                + "periodic timer "
                                + timerState + ".  Last timer expiration "
                                + "occurred at " + lastExpiration);

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
                    logger.log(Level.INFO,
                            "Rescheduling missed expiration for "
                            + "single-action timer "
                            + timerState + ". Timer expiration should "
                            + " have been delivered at "
                            + initialExpiration);
                }

                if (expirationTime == null) {
                    // Schedule-based timer will never expire again - remove it.
                    logger.log(Level.INFO,
                            "Removing schedule-based timer " + timerState
                            + " that will never expire again");
                    timerIdsToRemove.add(timerId);
                } else {
                    timersToRestore.put(timerState, expirationTime);
                    result.add(timer);
                }

            } else {
                // Timed object's container no longer exists - remember its id.
                logger.log(Level.FINE,
                        "Skipping timer " + timerId
                        + " for container that is not up: " + containerId);
            }
        } // End -- for each active timer

        if (timerIdsToRemove.size() > 0) {
            removeTimers(timerIdsToRemove);
        }

        for (Iterator entries = timersToRestore.entrySet().iterator();
                entries.hasNext();) {
            Map.Entry next = (Map.Entry) entries.next();
            RuntimeTimerState nextTimer = (RuntimeTimerState) next.getKey();
            TimerPrimaryKey timerId = nextTimer.getTimerId();
            Date expiration = (Date) next.getValue();
            scheduleTask(timerId, expiration);
            logger.log(Level.FINE,
                    "EJBTimerService.restoreTimers(), scheduling timer "
                    + nextTimer);
        }

        logger.log(Level.FINE, "DONE EJBTimerService.restoreTimers()");
        return result;
    }

    private Collection<HZTimer> findActiveTimersOwnedByThisServer() {
        HashSet<HZTimer> result = new HashSet<>();
        for (HZTimer timer : (Collection<HZTimer>) pkCache.values()) {
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
                    logger.log(Level.INFO, "[" + s + "] EJB Timers owned by this server will be restored when timeout beans are loaded");
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

}
