/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2015 C2B2 Consulting Limited. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.ejb.timer.hazelcast;

import com.hazelcast.core.IMap;
import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.RuntimeTimerState;
import com.sun.ejb.containers.TimerPrimaryKey;
import com.sun.enterprise.deployment.MethodDescriptor;
import fish.payara.nucleus.hazelcast.HazelcastCore;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.FinderException;
import javax.ejb.TimerConfig;
import javax.transaction.TransactionManager;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;

/**
 *
 * @author steve
 */
public class HazelcastTimerStore extends EJBTimerService {

    private static final String EJB_TIMER_CACHE_NAME = "HZEjbTmerCache";
    private static final String EJB_TIMER_CONTAINER_CACHE_NAME = "HZEjbTmerContainerCache";
    private static final String EJB_TIMER_APPLICAION_CACHE_NAME = "HZEjbTmerApplicationCache";

    private final IMap pkCache;
    private final IMap containerCache;
    private final IMap applicationCache;

    static void init(HazelcastCore core) {
        try {
            EJBTimerService.setEJBTimerService(new HazelcastTimerStore(core));
        } catch (Exception ex) {
            Logger.getLogger(HazelcastTimerStore.class.getName()).log(Level.WARNING, "Problem when initialising Timer Store", ex);
        }
    }

    public HazelcastTimerStore(HazelcastCore core) throws Exception {
        pkCache = core.getInstance().getMap(EJB_TIMER_CACHE_NAME);
        containerCache = core.getInstance().getMap(EJB_TIMER_CONTAINER_CACHE_NAME);
        applicationCache = core.getInstance().getMap(EJB_TIMER_APPLICAION_CACHE_NAME);
    }

    @Override
    protected void _createTimer(TimerPrimaryKey timerId, long containerId, long applicationId, Object timedObjectPrimaryKey, String server_name, Date initialExpiration, long intervalDuration, EJBTimerSchedule schedule, TimerConfig timerConfig) throws Exception {
        if (timerConfig.isPersistent()) {
            TransactionManager tm = ejbContainerUtil.getTransactionManager();
            boolean localTx = tm.getTransaction() == null;
            if (localTx) {
                tm.begin();
            }
            pkCache.put(timedObjectPrimaryKey, new HZTimer(timerId, containerId, applicationId, timedObjectPrimaryKey, server_name, initialExpiration, intervalDuration, schedule, timerConfig));
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
        super.destroyAllTimers(applicationId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void destroyTimers(long containerId) {
        super.destroyTimers(containerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void cancelTimer(TimerPrimaryKey timerId) throws FinderException, Exception {
        if (!cancelNonPersistentTimer(timerId)) {
            
        }
 
    }

    @Override
    protected Serializable getInfo(TimerPrimaryKey timerId) throws FinderException {
        return super.getInfo(timerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Date getNextTimeout(TimerPrimaryKey timerId) throws FinderException {
        return super.getNextTimeout(timerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void cancelTimersByKey(long containerId, Object primaryKey) {
        super.cancelTimersByKey(containerId, primaryKey); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createSchedules(long containerId, long applicationId, Map<MethodDescriptor, List<ScheduledTimerDescriptor>> methodDescriptorSchedules, String server_name) {
        super.createSchedules(containerId, applicationId, methodDescriptorSchedules, server_name); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void createSchedulesOnServer(EjbDescriptor ejbDescriptor, String server_name) {
        super.createSchedulesOnServer(ejbDescriptor, server_name); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void expungeTimer(TimerPrimaryKey timerId, boolean removeTimerBean) {
        super.expungeTimer(timerId, removeTimerBean); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(Collection<Long> containerIds) {
        return super.getTimerIds(containerIds); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(long containerId, Object timedObjectPrimaryKey) {
        return super.getTimerIds(containerId, timedObjectPrimaryKey); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected EJBTimerSchedule getTimerSchedule(TimerPrimaryKey timerId) throws FinderException {
        return super.getTimerSchedule(timerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean isPersistent() {
        return true; 
    }

    @Override
    protected boolean isCancelledByAnotherInstance(RuntimeTimerState timerState) {
        return super.isCancelledByAnotherInstance(timerState); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isPersistent(TimerPrimaryKey timerId) throws FinderException {
        return super.isPersistent(timerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean isValidTimerForThisServer(TimerPrimaryKey timerId, RuntimeTimerState timerState) {
        return super.isValidTimerForThisServer(timerId, timerState); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String[] listTimers(String[] serverIds) {
        return super.listTimers(serverIds); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public int migrateTimers(String fromOwnerId) {
        return super.migrateTimers(fromOwnerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected Map<TimerPrimaryKey, Method> recoverAndCreateSchedules(long containerId, long applicationId, Map<Method, List<ScheduledTimerDescriptor>> schedules, boolean deploy) {
        return super.recoverAndCreateSchedules(containerId, applicationId, schedules, deploy); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean redeliverTimeout(RuntimeTimerState timerState) {
        return super.redeliverTimeout(timerState); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void resetLastExpiration(TimerPrimaryKey timerId, RuntimeTimerState timerState) {
        super.resetLastExpiration(timerId, timerState); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void resetEJBTimers(String target) {
        super.resetEJBTimers(target); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean stopOnFailure() {
        return super.stopOnFailure(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected boolean timerExists(TimerPrimaryKey timerId) {
        return super.timerExists(timerId); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void stopTimers(long containerId) {
        super.stopTimers(containerId); //To change body of generated methods, choose Tools | Templates.
    }
    

}
