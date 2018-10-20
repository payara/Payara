/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.ejb.persistent.timer;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.TimerConfig;
import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EjbContainerUtil;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.EJBTimerService;
import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.NonPersistentEJBTimerService;
import com.sun.ejb.containers.RuntimeTimerState;
import com.sun.ejb.containers.TimerPrimaryKey;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.config.EjbTimerService;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.ScheduledTimerDescriptor;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;
/**
 * Persistent support part of the EJBTimerService
 *
 * @author Marina Vatkina
 */
public class PersistentEJBTimerService extends NonPersistentEJBTimerService {

    private final TimerLocal timerLocal_;

    private static final Logger logger = EjbContainerUtilImpl.getLogger();

    // This boolean value would be set in PE to be a default value of false.
    // In case of EE the default value would be true. When set to true the
    // timer service would have maximim consistency with performance degration.
    private boolean performDBReadBeforeTimeout = false;

    private boolean removeOldTimers = false;

    private static final String strDBReadBeforeTimeout = "com.sun.ejb.timer.ReadDBBeforeTimeout";
    private boolean foundSysPropDBReadBeforeTimeout = false;

    EjbTimerService ejbt = null;
    private DataSource timerDataSource = null;

    private static final String TIMER_RESOURCE_JNDI = "jdbc/__TimerPool";
    private static final String TIMER_SERVICE_APP_NAME = "ejb-timer-service-app";
    private static final String TIMER_SERVICE_BEAN_NAME = "TimerBean";

    // Determines what needs to be done on connection failure
    private static final String ON_CONECTION_FAILURE = "operation-on-connection-failure";
    private static final String OP_REDELIVER = "redeliver";
    private static final String OP_STOP = "stop";

    // Possible values "redeliver" and "stop"
    private String operationOnConnectionFailure = null;

    private PersistentEJBTimerService(String ejbName, boolean removeOldTimers) throws Exception {
        super();

        timerLocal_ = (TimerLocal) ejbContainerUtil.getGlassfishNamingManager().
                getInitialContext().lookup(ejbName);

        this.removeOldTimers = removeOldTimers;

        initProperties();

        // Verify that the DataSource ref is correct and store it to check if connections can be aquired if
        // the timeout fails
        lookupTimerResource();

        // The default value for ReadDBBeforeTimeout in case of PE
        // is false.
        setPerformDBReadBeforeTimeout(!isDas);
    }

    private void initProperties() {

        try {

            // Check for property settings from domain.xml
            EjbContainer ejbc = ejbContainerUtil.getEjbContainer();
            ejbt = ejbc.getEjbTimerService();

            if( ejbt != null ) {

                // If the system property com.sun.ejb.timer.ReadDBBeforeTimeout
                // is defined by the user use that the value of the flag
                // performDBReadBeforeTimeout
                foundSysPropDBReadBeforeTimeout =
                    getDBReadBeforeTimeoutProperty();

                operationOnConnectionFailure = ejbt.getPropertyValue(ON_CONECTION_FAILURE);
            }

        } catch(Exception e) {
            logger.log(Level.FINE, "Exception converting timer service " +
               "domain.xml properties.  Defaults will be used instead.", e);
        }

    }

    /**
     *--------------------------------------------------------------
     * Methods to be implemented for Admin CLI
     *--------------------------------------------------------------
     */

    /**
     * Provide a count of timers owned by each server
     */
    public String[] listTimers( String[] serverIds ) {
        String[] totalTimers = null;
        try {
           totalTimers = timerLocal_.countTimersOwnedByServerIds(serverIds);
        } catch( Exception ex ) {
            logger.log( Level.SEVERE, "Exception in listTimers() : " , ex );

            //Propogate any exceptions caught
            EJBException ejbEx = createEJBException( ex );
            throw ejbEx;
        }
        return totalTimers;
    }

    /**
     * Take ownership of another server's timers.
     */
    public int migrateTimers(String fromOwnerId) {

        String ownerIdOfThisServer = getOwnerIdOfThisServer();

        if( fromOwnerId.equals(ownerIdOfThisServer) ) {
            /// Error. The server from which timers are being
            // migrated should never be up and running OR receive this
            // notification.
            logger.log(Level.WARNING, "Attempt to migrate timers from " +
                        "an active server instance " + ownerIdOfThisServer);
            throw new IllegalStateException("Attempt to migrate timers from " +
                                            " an active server instance " +
                                            ownerIdOfThisServer);
        }

        logger.log(Level.INFO, "Beginning timer migration process from " +
                   "owner " + fromOwnerId + " to " + ownerIdOfThisServer);

        TransactionManager tm = ejbContainerUtil.getTransactionManager();

        Set toRestore = null;
	int totalTimersMigrated = 0;

        try {

            tm.begin();

            toRestore = timerLocal_.findTimersOwnedBy(fromOwnerId);
            totalTimersMigrated = timerLocal_.migrateTimers(fromOwnerId, ownerIdOfThisServer);

            tm.commit();

        } catch(Exception e) {
            // Don't attempt to restore any timers since an error has
            // occurred.  This could be the expected result in the case that
            // multiple server instances attempted the migration at the same
            // time.
            logger.log(Level.FINE, "timer migration error", e);

            try {
                tm.rollback();
            } catch(Exception re) {
                logger.log(Level.FINE, "timer migration rollback error", re);
            }

            //Propagate the exception caught
            EJBException ejbEx = createEJBException( e );
            throw ejbEx;
        }

// XXX if( totalTimersMigrated  == toRestore.size() ) { XXX ???
        if( totalTimersMigrated > 0 ) {

            boolean success = false;
            try {

                logger.log(Level.INFO, "Timer migration phase 1 complete. " +
                           "Changed ownership of " + toRestore.size() +
                           " timers.  Now reactivating timers...");

                _notifyContainers(toRestore);

                tm.begin();
                _restoreTimers(toRestore);
                success = true;

            } catch(Exception e) {

                logger.log(Level.FINE, "timer restoration error", e);

                //Propogate any exceptions caught as part of the transaction
                EJBException ejbEx = createEJBException( e );
                throw ejbEx;

            } finally {
                // We're not modifying any state in this tx so no harm in
                // always committing.
                try {
                    tm.commit();
                } catch(Exception re) {
                    logger.log(Level.FINE, "timer migration error", re);

                    if( success ) {
                        //Propogate any exceptions caught when trying to commit
                        //the transaction
                        EJBException ejbEx = createEJBException( re );
                        throw ejbEx;
                    }
                }
            }
        } else {
            logger.log(Level.INFO, fromOwnerId + " has 0 timers in need " +
                       "of migration");
        }

        return totalTimersMigrated;

    } //migrateTimers()

    @Override
    public boolean isPersistent() {
        return true;
    }

    private void setPerformDBReadBeforeTimeout( boolean defaultDBReadValue ) {

        // If the system property com.sun.ejb.timer.ReadDBBeforeTimeout
        // has been defined by the user then use that value else use the default
        if ( !foundSysPropDBReadBeforeTimeout ) {
            performDBReadBeforeTimeout = defaultDBReadValue;

            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "EJB Timer Service property : " +
                           "\nread DB before timeout delivery = " +
                           performDBReadBeforeTimeout);
            }

        }
    }

    /**
     * Check to see if the user has defined a System property to specify if
     * we need to check the timer table in the database and confirm that the
     * timer is valid before delivering the ejbTimeout() for that timer.
     *
     * In case of PE - the default value is false
     * and for SE/EE - the default value is true
     *
     * But in all cases (PE/SE/EE) the user can set the System property
     * "READ_DB_BEFORE_EJBTIMEOUT" to change the behaviour
     */
    private boolean getDBReadBeforeTimeoutProperty() {

        boolean result = false;
        try{
            String str=System.getProperty( strDBReadBeforeTimeout );
            if( null != str) {
                performDBReadBeforeTimeout = Boolean.valueOf(str).booleanValue();

                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "EJB Timer Service property : " +
                               "\nread DB before timeout delivery = " +
                               performDBReadBeforeTimeout);
                }

                result = true;
            }
        } catch(Exception e) {
            logger.log(Level.INFO,
                "getDBReadBeforeTimeoutProperty(), " +
                " Exception when trying to " +
                "get the System property - ", e);
        }
        return result;
    }

    /**
     * Called at server startup *after* user apps have been re-activated
     * to restart any active EJB timers.
     */
    private boolean restoreEJBTimers() {
        boolean rc = false;
        try {
            if( totalTimedObjectsInitialized_ > 0 ) {
                restoreTimers();
                rc = true;
            } else {
                int s = (timerLocal_.findActiveTimersOwnedByThisServer()).size();
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

    private void restoreTimers() throws Exception {

        // Optimization.  Skip timer restoration if there aren't any
        // applications with timed objects deployed.
        if( totalTimedObjectsInitialized_ == 0 ) {
            return;
        }

        TransactionManager tm = ejbContainerUtil.getTransactionManager();
        try {
            // create a tx in which to do database access for all timers
            // needing restoration.  This gives us better performance that
            // doing individual transactions per timer.
            tm.begin();

            // This operation can take a while, since in some configurations
            // this will be the first time the connection to the database
            // is initialized.  In addition, there's an initialization
            // cost to generating the SQL for the underlying
            // jpql queries the first time any TimerBean query is called.
            _restoreTimers(timerLocal_.findActiveTimersOwnedByThisServer());

        } finally {
            // try to commit regardless of success or failure.
            try {
                tm.commit();
            } catch(Exception e) {
                logger.log(Level.WARNING, "ejb.timer_service_init_error", e);
            }
        }
    }

    /**
     * The portion of timer migration that notifies containers about
     * automatic timers being migrated to this instance
     */
    private void _notifyContainers(Set<TimerState> timers) {
        for(TimerState timer: timers) {
            EJBTimerSchedule ts = timer.getTimerSchedule();
            if (ts != null && ts.isAutomatic()) {
                addToSchedules(timer.getContainerId(), getPrimaryKey(timer), ts);
            }
        }
    }

    /**
     * The portion of timer restoration that deals with registering the
     * JDK timer tasks and checking for missed expirations.
     * @return the Set of restored timers
     */
    private Set<TimerState> _restoreTimers(Set<TimerState> timersEligibleForRestoration) {

        // Do timer restoration in two passes.  The first pass updates
        // the timer cache with each timer.  The second pass schedules
        // the JDK timer tasks.

        Map timersToRestore = new HashMap();
        Set timerIdsToRemove = new HashSet();
        Set<TimerState> result = new HashSet<TimerState>();

        for(TimerState timer: timersEligibleForRestoration) {

            TimerPrimaryKey timerId = getPrimaryKey(timer);
            if (getTimerState(timerId) != null) {
                // Already restored. Add it to the result but do nothing else.
                logger.log(Level.FINE, "@@@ Timer already restored: " + timer);
                result.add(timer);
                continue;
            }

            long containerId = timer.getContainerId();

            // Timer might refer to an obsolete container.
            BaseContainer container = getContainer(containerId);
            if( container != null ) {

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
                if( container.getContainerType() == BaseContainer.ContainerType.ENTITY) {
                    timedObjectPrimaryKey = timer.getTimedObjectPrimaryKey();
                }

                RuntimeTimerState timerState = new RuntimeTimerState
                    (timerId, initialExpiration,
                     timer.getIntervalDuration(), container,
                     timedObjectPrimaryKey,
                     timer.getTimerSchedule(),
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

                if( timerState.isPeriodic() ) {
                    // lastExpiration time, or null if we either aren't
                    // tracking last expiration or an expiration hasn't
                    // occurred yet for this timer.
                    Date lastExpiration = timer.getLastExpiration();
                    EJBTimerSchedule ts = timer.getTimerSchedule();

                    // @@@ need to handle case where last expiration time
                    // is not stored in database.  This will be the case
                    // when we add configuration for update-db-on-delivery.
                    // However, for now assume we do update the db on each
                    // ejbTimeout.  Therefore, if (lastExpirationTime == null),
                    // it means the timer didn't successfully complete any
                    // timer expirations.

                    if( (lastExpiration == null) &&
                        now.after(initialExpiration) ) {

                        if (!timerState.isExpired()) {
                            // This timer didn't even expire one time.
                            logger.log(Level.INFO,
                                   "Rescheduling missed expiration for " +
                                   "periodic timer " +
                                   timerState + ". Timer expirations should " +
                                   " have been delivered starting at " +
                                   initialExpiration);
                        }

                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.

                    } else if ( (lastExpiration != null) &&
                            ((ts != null && ts.getNextTimeout(lastExpiration).getTimeInMillis()
                                   < now.getTime())
                            || ((ts == null) && now.getTime() - lastExpiration.getTime()
                                   > timer.getIntervalDuration()) ) ) {

                        // Schedule-based timer is periodic

                        logger.log(Level.INFO,
                                   "Rescheduling missed expiration for " +
                                   "periodic timer " +
                                   timerState + ".  Last timer expiration " +
                                   "occurred at " + lastExpiration);

                        // Timer expired at least once and at least one
                        // missed expiration has occurred.

                        // keep expiration time at initialExpiration.  That
                        // will force an ejbTimeout almost immediately. After
                        // that the timer will return to fixed rate expiration.

                    } else {

                        // In this case, at least one expiration has occurred
                        // but that was less than one period ago so there were
                        // no missed expirations.
                        expirationTime =
                            calcNextFixedRateExpiration(timerState);
                    }

                } else {  // single-action timer

                    if( now.after(initialExpiration) ) {
                        logger.log(Level.INFO,
                                   "Rescheduling missed expiration for " +
                                   "single-action timer " +
                                   timerState + ". Timer expiration should " +
                                   " have been delivered at " +
                                   initialExpiration);
                    }
                }

                if (expirationTime == null) {
                    // Schedule-based timer will never expire again - remove it.
                    logger.log(Level.INFO,
                            "Removing schedule-based timer " + timerState +
                                   " that will never expire again");
                    timerIdsToRemove.add(timerId);
                } else {
                    timersToRestore.put(timerState, expirationTime);
                    result.add(timer);
                }

            } else {
                // Timed object's container no longer exists - remember its id.
                logger.log(Level.FINE,
                        "Skipping timer " + timerId +
                               " for container that is not up: " + containerId);
            }
        } // End -- for each active timer

        if (timerIdsToRemove.size() > 0) {
            timerLocal_.remove(timerIdsToRemove);
        }

        for(Iterator entries = timersToRestore.entrySet().iterator();
            entries.hasNext(); ) {
            Map.Entry next  = (Map.Entry) entries.next();
            RuntimeTimerState nextTimer = (RuntimeTimerState) next.getKey();
            TimerPrimaryKey timerId    = nextTimer.getTimerId();
            Date expiration = (Date) next.getValue();
            scheduleTask(timerId, expiration);
            logger.log(Level.FINE,
                       "EJBTimerService.restoreTimers(), scheduling timer " +
                       nextTimer);
        }

        logger.log(Level.FINE, "DONE EJBTimerService.restoreTimers()");
        return result;
    }

    @Override
    protected void cancelTimersByKey(long containerId, Object primaryKey) {
        try {

            // Get *all* timers for this entity bean identity.  This includes
            // even timers *not* owned by this server instance, but that
            // are associated with the same entity bean and primary key.
            Collection<TimerState> timers = getTimers(containerId, primaryKey);
            if( logger.isLoggable(Level.FINE) ) {
                if( timers.isEmpty() ) {
                    logger.log(Level.FINE, "0 cancelEntityBeanTimers for " +
                               containerId + ", " + primaryKey);
                }
            }
// XXX check if we need a FinderException XXX
            // Called by EntityContainer.removeBean and will be
            // called with the proper Tx context

            // Non-persistent timers are not supported for entity beans
            timerLocal_.cancelTimers(timers);
        } catch(Exception e) {
            logger.log(Level.WARNING, "ejb.cancel_entity_timers",
                       new Object[] { String.valueOf(containerId), primaryKey });
            logger.log(Level.WARNING, "", e);
        }
    }

    @Override
    protected void stopTimers(long containerId) {
        super.stopTimers(containerId);
        stopTimers(timerLocal_.findTimerIdsByContainer(containerId));
    }

    @Override
    protected void _destroyTimers(long id, boolean all) {
        Set<TimerPrimaryKey> timerIds = null;

        int count = ((all)? timerLocal_.countTimersByApplication(id) :
                timerLocal_.countTimersByContainer(id));

        if (count == 0) {
            if( logger.isLoggable(Level.INFO) ) {
                logger.log(Level.INFO, "No timers to be deleted for id: " + id);
            }
            return;
        }

        try {

            // Remove *all* timers for this ejb or this app. When an app is being undeployed
            // it will be called only once for all server instances.
            int deleted = ((all)? timerLocal_.deleteTimersByApplication(id) :
                    timerLocal_.deleteTimersByContainer(id));
            if( logger.isLoggable(Level.INFO) ) {
                logger.log(Level.INFO, "[" + deleted + "] timers deleted for id: " + id);
            }
        } catch(Exception ex) {
            logger.log(Level.WARNING, "ejb.destroy_timers_error",
                       new Object[] { String.valueOf(id) });
            logger.log(Level.WARNING, "", ex);
        }

        return;
    }

    /**
     * Create persistent timer.
     */
    @Override
    protected void _createTimer(TimerPrimaryKey timerId, long containerId, long applicationId,
                                Object timedObjectPrimaryKey, String server_name,
                                Date initialExpiration, long intervalDuration,
                                EJBTimerSchedule schedule, TimerConfig timerConfig)
                                throws Exception {
        try {
            if (timerConfig.isPersistent()) {
                timerLocal_.createTimer(timerId.getTimerId(), containerId,
                                   applicationId, server_name, timedObjectPrimaryKey,
                                   initialExpiration, intervalDuration,
                                   schedule, timerConfig);
            } else {
                addTimerSynchronization(null,
                        timerId.getTimerId(), initialExpiration,
                        containerId, ownerIdOfThisServer_, false);
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "ejb.create_timer_failure",
                       new Object[] { String.valueOf(containerId),
                                      timedObjectPrimaryKey,
                                      timerConfig.getInfo() });
            logger.log(Level.SEVERE, "", e);
            // Since timer was never created, remove it from cache.
            timerCache_.removeTimer(timerId);
            if( e instanceof CreateException ) {
                throw ((CreateException)e);
            } else {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(e);
                throw ejbEx;
            }
        }
    }

    /**
     * Recover pre-existing timers associated with the Container identified
     * by the containerId, and create automatic timers defined by the @Schedule
     * annotation on the EJB bean.
     *
     * If it is called from deploy on a non-clustered instance, both
     * persistent and non-persistent timers will be created.
     * Otherwise only non-persistent timers are created by this method.
     *
     * @return a Map of both, restored and created timers, where the key is TimerPrimaryKey
     * and the value is the Method to be executed by the container when the timer with
     * this PK times out.
     */
    @Override
    protected Map<TimerPrimaryKey, Method> recoverAndCreateSchedules(
            long containerId, long applicationId,
            Map<Method, List<ScheduledTimerDescriptor>> schedules,
            boolean deploy) {

        Map<TimerPrimaryKey, Method> result = new HashMap<TimerPrimaryKey, Method>();

        TransactionManager tm = ejbContainerUtil.getTransactionManager();
        try {
            tm.begin();

            Set<TimerState> timers = _restoreTimers(
                    (Set<TimerState>)timerLocal_.findActiveTimersOwnedByThisServerByContainer(containerId));

            if (timers.size() > 0) {
                logger.log(Level.FINE, "Found " + timers.size() +
                        " persistent timers for containerId: " + containerId);
            }

            boolean schedulesExist = (schedules.size() > 0);
            for (TimerState timer : timers) {
                EJBTimerSchedule ts = timer.getTimerSchedule();
                if (ts != null && ts.isAutomatic() && schedulesExist) {
                    for (Map.Entry<Method, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
                        Method m = entry.getKey();
                        if (m.getName().equals(ts.getTimerMethodName()) &&
                                m.getParameterTypes().length == ts.getMethodParamCount()) {
                            result.put(new TimerPrimaryKey(timer.getTimerId()), m);
                            if( logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "@@@ FOUND existing schedule: " +
                                        ts.getScheduleAsString() + " FOR method: " + m);
                            }
                        }
                    }
                }
            }

            createSchedules(containerId, applicationId, schedules, result, ownerIdOfThisServer_, true,
                    (deploy && isDas));

            tm.commit();

        } catch(Exception e) {
            recoverAndCreateSchedulesError(e, tm);
        }

        return result;
    }

    /**
     * Called in a clustered environment to eagerly create automatic persistent timers
     * on the specific server instance.
     */
    public void createSchedulesOnServer(EjbDescriptor ejbDescriptor, String server_name) {
        Map<MethodDescriptor, List<ScheduledTimerDescriptor>> schedules =
                new HashMap<MethodDescriptor, List<ScheduledTimerDescriptor>>();
        for (ScheduledTimerDescriptor schd : ejbDescriptor.getScheduledTimerDescriptors()) {
            MethodDescriptor method = schd.getTimeoutMethod();
            if (method != null && schd.getPersistent()) {
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "... processing " + method );
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
            logger.log( Level.FINE, "EJBTimerService - creating schedules for " + ejbDescriptor.getUniqueId());
        }
        createSchedules(ejbDescriptor.getUniqueId(), ejbDescriptor.getApplication().getUniqueId(), schedules, server_name);

        if (logger.isLoggable(Level.FINE)) {
            logger.log( Level.FINE, "EJBTimerService - finished processing schedules for BEAN ID: " + ejbDescriptor.getUniqueId());
        }
    }


    /**
     * Create automatic timers defined by the @Schedule annotation on the EJB bean during
     * deployment to a cluster or the first create-application-ref call after deployment
     * to DAS only.
     *
     * Only persistent schedule based timers for the containerId that has no timers associated
     * with it, will be created. And no timers will be scheduled.
     */
    public void createSchedules(long containerId, long applicationId,
            Map<MethodDescriptor, List<ScheduledTimerDescriptor>> methodDescriptorSchedules, String server_name) {
        TransactionManager tm = ejbContainerUtil.getTransactionManager();
        try {
            tm.begin();
            int count = timerLocal_.countTimersByContainer(containerId);

            if (count == 0) {
                // No timers owned by this EJB
                createSchedules(containerId, applicationId, methodDescriptorSchedules, null, server_name, false, true);
            }

            tm.commit();

        } catch(Exception e) {
            recoverAndCreateSchedulesError(e, tm);
        }
    }

    /**
     * Common code for exception processing in recoverAndCreateSchedules
     */
    private void recoverAndCreateSchedulesError(Exception e, TransactionManager tm) {
        logger.log(Level.WARNING, "Timer restore or schedule creation error", e);

        try {
            tm.rollback();
        } catch(Exception re) {
            logger.log(Level.FINE, "Timer restore or schedule creation rollback error", re);
        }

        //Propagate the exception caught as an EJBException
        EJBException ejbEx = createEJBException( e );
        throw ejbEx;
    }

    /**
     * Use database query to retrieve all active timers.  Results must
     * be transactionally consistent. E.g.,  a client calling
     * getTimers within a transaction where a timer has been
     * created but not committed "sees" the timer but a client
     * in a different transaction doesn't.
     *
     * @param timedObjectPrimaryKey can be null if not entity bean
     *
     * @return Collection of TimerState objects.
     */
    private Collection<TimerState> getTimers(long containerId,
                                 Object timedObjectPrimaryKey) {

        // The results should include all timers for the given ejb
        // and/or primary key, including timers owned by other server instances.

        // @@@ Might want to consider cases where we can use
        // timer cache to avoid some database access in PE/SE, or
        // even in EE with the appropriate consistency tradeoff.

        Collection<TimerState> activeTimers =
            timerLocal_.findActiveTimersByContainer(containerId);

        Collection<TimerState> timersForTimedObject = activeTimers;

        if( timedObjectPrimaryKey != null ) {

            // Database query itself can't do equality check on primary
            // key of timed object so perform check ourselves.

            timersForTimedObject = new HashSet();

            for(TimerState timer : activeTimers) {
                Object nextTimedObjectPrimaryKey =
                    timer.getTimedObjectPrimaryKey();
                if( nextTimedObjectPrimaryKey.equals(timedObjectPrimaryKey) ) {
                    timersForTimedObject.add(timer);
                }
            }
        }

        return timersForTimedObject;
    }

    /**
     * Use database query to retrieve persistrent timer ids of all active
     * timers.  Results must be transactionally consistent. E.g.,
     * a client calling getTimerIds within a transaction where a
     * timer has been created but not committed "sees" the timer
     * but a client in a different transaction doesn't. Called by
     * EJBTimerServiceWrapper when caller calls getTimers.
     *
     * @param timedObjectPrimaryKey can be null if not entity bean
     * @return Collection of Timer Ids.
     */
    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(long containerId, Object timedObjectPrimaryKey) {

        // The results should include all timers for the given ejb
        // and/or primary key, including timers owned by other server instances.

        // @@@ Might want to consider cases where we can use
        // timer cache to avoid some database access in PE/SE, or
        // even in EE with the appropriate consistency tradeoff.

        Collection<TimerPrimaryKey> timerIdsForTimedObject =
                new HashSet<TimerPrimaryKey>();

        if( timedObjectPrimaryKey == null ) {

            timerIdsForTimedObject =
                timerLocal_.findActiveTimerIdsByContainer(containerId);

        } else {

            // Database query itself can't do equality check on primary
            // key of timed object so perform check ourselves.

            Collection<TimerState> timersForTimedObject = getTimers(containerId,
                                                        timedObjectPrimaryKey);

            for(TimerState timer : timersForTimedObject) {
                timerIdsForTimedObject.add(getPrimaryKey(timer));
            }
        }

        // Add active non-persistent timer ids
        timerIdsForTimedObject.addAll(super.getTimerIds(containerId, null));

        return timerIdsForTimedObject;
    }

    /**
     * Return the Ids of active timers owned by EJBs.
     * Primary key of entity bean is unnecessary because all of the active
     * timers are expected
     *
     * @return Collection of Timer Ids.
     */
    @Override
    protected Collection<TimerPrimaryKey> getTimerIds(Collection<Long> containerIds) {
        Collection<TimerPrimaryKey> timerIds = new HashSet<TimerPrimaryKey>(super.getTimerIds(containerIds));
        timerIds.addAll(timerLocal_.findActiveTimerIdsByContainers(containerIds));
        return timerIds;
    }

    //
    // Logic used by TimerWrapper for javax.ejb.Timer methods.
    //

    @Override
    protected void cancelTimer(TimerPrimaryKey timerId)
            throws FinderException, Exception {

        // Check non-persistent timers first
        if (!cancelNonPersistentTimer(timerId)) {
            // @@@ We can't assume this server instance owns the timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.

            // Look up timer bean from database.  Throws FinderException if
            // timer no longer exists, which is converted by the caller into a
            // NoSuchObjectLocalException.
            timerLocal_.cancel(timerId);
        }

    }

    private TimerPrimaryKey getPrimaryKey(TimerState timer) {
        return new TimerPrimaryKey(timer.getTimerId());
    }

    @Override
    protected Date getNextTimeout(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            return _getNextTimeout(rt);
        }

        // It's a persistent timer

        // @@@ We can't assume this server instance owns the persistent timer
        // so always ask the database.  Investigate possible use of
        // timer cache for optimization.

        TimerState timer = getPersistentTimer(timerId);
        Date initialExpiration = timer.getInitialExpiration();
        long intervalDuration = timer.getIntervalDuration();
        EJBTimerSchedule ts = timer.getTimerSchedule();

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
    protected Serializable getInfo(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers first
        if (!super.isPersistent(timerId)) {
            return super.getInfo(timerId);
        }

        // @@@ We can't assume this server instance owns the persistent timer
        // so always ask the database.  Investigate possible use of
        // timer cache for optimization.

        TimerState timer = getPersistentTimer(timerId);
        return timer.getInfo();
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
    protected boolean timerExists(TimerPrimaryKey timerId) {
        boolean exists = super.timerExists(timerId);

        // Check persistent timers only if non-persistent is not found
        if (!exists) {

            // @@@ We can't assume this server instance owns the persistent timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.

            TimerState timer = timerLocal_.findTimer(timerId);
            if (timer != null) {
                // Make sure timer hasn't been cancelled within the current tx.
                exists = timer.isActive();
            }
        }

        return exists;
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

            TimerState timer = getPersistentTimer(timerId);
            ts = timer.getTimerSchedule();
        }

        return ts;
    }

    private void removeTimerBean(TimerPrimaryKey timerId) {
        try {
            timerLocal_.remove(timerId);
        } catch(Throwable t) {
            logger.log(Level.WARNING, "ejb.remove_timer_failure",
                       new Object[] { timerId });
            logger.log(Level.WARNING, "", t);
        }
    }

    private TimerState getPersistentTimer(TimerPrimaryKey timerId)
            throws FinderException {
        TimerState timer = timerLocal_.findTimer(timerId);
        if( timer == null || timer.isCancelled() ) {
            // The timer has been cancelled within this tx.
            throw new FinderException("timer " + timerId + " does not exist");
        }

        return timer;
    }



    /**
     * Check if another server instance cancelled this timer.
     */
    @Override
    protected boolean isCancelledByAnotherInstance(RuntimeTimerState timerState) {
        if( timerState.isPersistent() && performDBReadBeforeTimeout) {

            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "For Timer :" + timerState.getTimerId() +
                        ": check the database to ensure that the timer is still " +
                        " valid, before delivering the ejbTimeout call" );
            }

            if( ! checkForTimerValidity(timerState.getTimerId()) ) {
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
    protected boolean redeliverTimeout(RuntimeTimerState timerState) {
         return ( timerState.getNumFailedDeliveries() <
                            getMaxRedeliveries() || redeliverOnFailedConnection());
    }

    /**
     * Persistent timers can be cancelled from another server instance
     */
    @Override
    protected boolean isValidTimerForThisServer(TimerPrimaryKey timerId,
                                          RuntimeTimerState timerState) {
        if (timerState.isPersistent()) {
            if( getValidTimerFromDB( timerId ) == null ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Update database for a persistent timer
     */
    @Override
    protected void resetLastExpiration(TimerPrimaryKey timerId,
                                          RuntimeTimerState timerState) {
        if (timerState.isPersistent()) {
            TimerState timer = getValidTimerFromDB( timerId );
            if( null == timer ) {
                return;
            }

            Date now = new Date();
            timer.setLastExpiration(now);

            // Since timer was successfully delivered, update
            // last delivery time in database if that option is
            // enabled.
            // @@@ add configuration for update-db-on-delivery
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE,
                       "Setting last expiration " +
                       " for periodic timer " + timerState +
                       " to " + now);
            }
        }
    }

    /**
     * This method is called to check if the timer is still valid.
     * In the SE/EE case the timer might be cancelled by any other
     * server instance (other than the owner server instance)
     * that is part of the same cluster. Until we have a messaging
     * system in place we would have to do a database query to
     * check if the timer is still valid.
     * Also check that the timer is owned by the current server instance
     *
     *  @return false if the timer record is not found in the database,
     *          true  if the timer is still valid.
     */
    private boolean checkForTimerValidity(TimerPrimaryKey timerId) {

        boolean result = true;

        TimerState timer = getValidTimerFromDB( timerId );
        if( null == timer) {
            result = false;
        }

        return result;
    }

    private TimerState getValidTimerFromDB(TimerPrimaryKey timerId) {

        boolean result       = true;
        TimerState timer = timerLocal_.findTimer(timerId);

        try {
            if (timer != null) {
                // There is a possibility that the same timer might be
                // migrated across to a different server. Hence check
                // that the ownerId of the timer record is the same as
                // the current server
                if( ! ( timer.getOwnerId().equals(
                        ownerIdOfThisServer_) ) ) {
                    logger.log(Level.WARNING,
                            "The timer (" + timerId + ") is not owned by " +
                            "server (" + ownerIdOfThisServer_ + ") that " +
                            "initiated the ejbTimeout. This timer is now " +
                            "owned by (" + timer.getOwnerId() + "). \n" +
                            "Hence delete the timer from " +
                            ownerIdOfThisServer_ + "'s cache.");

                    result = false;
                }

            } else {
                // The timer does not exist in the database
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "Timer :" + timerId +
                            ": has been cancelled by another server instance. " +
                            "Expunging the timer from " + ownerIdOfThisServer_ +
                            "'s cache.");
                }

                result = false;
            }

        } finally {
            if( !result ) {
                // The timer is either not present in the database or it is now
                // owned by some other server instance, hence remove the cache
                //entry for the timer from the current server
                expungeTimer(timerId, false);
                timer = null;
            } else {
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE,
                        "The Timer :" + timerId +
                        ": is a valid timer for the server (" +
                        ownerIdOfThisServer_ + ")");
                }
            }
        }

        return timer;
    }

    @Override
    protected void expungeTimer(TimerPrimaryKey timerId,
                              boolean removeTimerBean) {
        // First remove timer bean.  Don't update cache until
        // afterwards, since accessing of timer bean might require
        // access to timer state(e.g. timer application classloader)
        if( removeTimerBean ) {
            removeTimerBean(timerId);
        }
        // And finish in the superclass...
        super.expungeTimer(timerId, removeTimerBean);
    }


    // Used by TimerBean.testTimer
    TimerLocal getTimerLocal() {
        return timerLocal_;
    }

    @Override
    protected boolean stopOnFailure() {
        return stopOnFailedConnection();
    }

    private boolean redeliverOnFailedConnection() {
        return operationOnConnectionFailure != null && operationOnConnectionFailure.equalsIgnoreCase(OP_REDELIVER) &&
            failedConnection();
    }

    private boolean stopOnFailedConnection() {
        return operationOnConnectionFailure != null && operationOnConnectionFailure.equalsIgnoreCase(OP_STOP) &&
            failedConnection();
    }

    private boolean failedConnection() {
        boolean failed = false;
        Connection c = null;
        try {
            if (timerDataSource == null) {
                lookupTimerResource();
            }
            c = timerDataSource.getConnection();
            failed = !c.isValid(0);
        } catch (Exception e) {
            failed = true;
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {}
        }

        if (failed) {
            logger.log(Level.WARNING, "Cannot acquire a connection from the database used by the EJB Timer Service");
        } else {
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Connection ok");
            }
        }

        return failed;
    }

    private void lookupTimerResource() throws Exception {
        String resource_name = getTimerResource();
        ConnectorRuntime connectorRuntime = ejbContainerUtil.getServices().getService(ConnectorRuntime.class);
        timerDataSource = DataSource.class.cast(connectorRuntime.lookupNonTxResource(resource_name, false));
    }

    static void initEJBTimerService(String target) {
        EJBTimerService timerService = null;

        EjbContainerUtil _ejbContainerUtil = EjbContainerUtilImpl.getInstance();
        EjbTimerService _ejbt = _ejbContainerUtil.getEjbTimerService(target);
        String resourceName = getTimerResource(_ejbt);

        File root = _ejbContainerUtil.getServerContext().getInstallRoot();
        boolean is_upgrade = isUpgrade(resourceName, _ejbt, root);

        File rootScratchDir = _ejbContainerUtil.getServerEnvironment().getApplicationStubPath();
        File appScratchFile = new File(rootScratchDir, TIMER_SERVICE_APP_NAME);

        // Remember the value before the file is created during deploy
        boolean removeOldTimers = is_upgrade && !appScratchFile.exists();

        boolean available = _ejbContainerUtil.getDeployment().isRegistered(TIMER_SERVICE_APP_NAME);
        if (available) {
            logger.log (Level.WARNING, "EJBTimerService had been explicitly deployed.");
        } else {
            if (resourceName != null) {
                available = deployEJBTimerService(root, appScratchFile, resourceName, is_upgrade);
            } else {
                logger.log (Level.WARNING, "Cannot deploy EJBTimerService: Timer resource for target "
                        + target + " is not available");
            }
        }

        if (available) {
            try {
                timerService = new PersistentEJBTimerService("java:global/" +
                        TIMER_SERVICE_APP_NAME + "/" + TIMER_SERVICE_BEAN_NAME,
                        removeOldTimers);

                logger.log(Level.INFO, "ejb.timer_service_started", new Object[] { resourceName } );
            } catch (Exception ex) {
                logger.log(Level.WARNING, "ejb.timer_service_init_error", ex);
            }
        }

        EJBTimerService.setPersistentTimerService(timerService);
    }

    @Override
    protected void resetEJBTimers(String target) {
        if (removeOldTimers) {
            // Destroy all previous timers
            destroyAllTimers(0L);
        } else if (target == null) {
            // target is null when accessed from the BaseContainer on load, i.e. where timers are running
            logger.log(Level.INFO, "==> Restoring Timers ... " );
            if (restoreEJBTimers()) {
                logger.log(Level.INFO, "<== ... Timers Restored.");
            }
        }
    }

    private String getTimerResource() {
        return getTimerResource(ejbt);
    }

    private static String getTimerResource(EjbTimerService _ejbt) {
        String resource = null; // EjbTimerService is not available for domain deployment
        if (_ejbt != null) {
            if (_ejbt.getTimerDatasource() != null) {
                resource = _ejbt.getTimerDatasource();
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Found Timer Service resource name " + resource);
                }
            } else {
                resource = TIMER_RESOURCE_JNDI;
            }
        }
        return resource;
    }

    private static boolean deployEJBTimerService(File root, File appScratchFile,
            String resourceName, boolean is_upgrade) {
        boolean deployed = false;
        logger.log (Level.INFO, "Loading EJBTimerService. Please wait.");
        File app = null;
        try {
            app = FileUtils.getManagedFile(TIMER_SERVICE_APP_NAME + ".war",
                    new File(root, "lib/install/applications/"));
        } catch (Exception e) {
            logger.log (Level.WARNING, "Caught unexpected exception", e);
        }

        if (app == null || !app.exists()) {
            logger.log (Level.WARNING, "Cannot deploy or load persistent EJBTimerService: " +
                    "required WAR file (" + TIMER_SERVICE_APP_NAME + ".war) is not installed");
        } else {
            DeployCommandParameters params = new DeployCommandParameters(app);
            params.name = TIMER_SERVICE_APP_NAME;

            try {
                EjbContainerUtil _ejbContainerUtil = EjbContainerUtilImpl.getInstance();
                // appScratchFile is a marker file and needs to be created on Das on the
                // first access of the Timer Service application
                if (_ejbContainerUtil.isDas() && appScratchFile.createNewFile() && !is_upgrade) {
                    params.origin = OpsParams.Origin.deploy;
                } else {
                    params.origin = OpsParams.Origin.load;
                }
                params.target = _ejbContainerUtil.getServerEnvironment().getInstanceName();

                ActionReport report = _ejbContainerUtil.getServices().
                        getService(ActionReport.class, "plain");
                Deployment deployment = _ejbContainerUtil.getDeployment();
                ExtendedDeploymentContext dc = deployment.getBuilder(
                        logger, params, report).source(app).build();
                dc.addTransientAppMetaData(DatabaseConstants.JTA_DATASOURCE_JNDI_NAME_OVERRIDE, resourceName);
                Properties appProps = dc.getAppProps();
                appProps.setProperty(ServerTags.OBJECT_TYPE, DeploymentProperties.SYSTEM_ALL);

                deployment.deploy(dc);

                if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                    logger.log (Level.WARNING, "Cannot deploy or load EJBTimerService: ",
                            report.getFailureCause());
                } else {
                    deployed = true;
                }
            } catch (Exception e) {
                logger.log (Level.WARNING, "Cannot deploy or load EJBTimerService: ", e);
            } finally {
                if (!deployed && params.origin.isDeploy() && appScratchFile.exists()) {
                    // Remove marker file if deploy failed
                    if (!appScratchFile.delete()) {
                        logger.log (Level.WARNING, "Failed to remove the marker file " + appScratchFile);
                    }
                }
            }
        }

        return deployed;
    }

    private static boolean isUpgrade(String resource, EjbTimerService _ejbt, File root) {
        boolean upgrade = false;
        Property prop = null;
        if (_ejbt != null) {
            List<Property> properties = _ejbt.getProperty();
            if (properties != null) {
                for (Property p : properties) {
                    if (p.getName().equals(EjbContainerUtil.TIMER_SERVICE_UPGRADED)) {
                        String value = p.getValue();
                        if (value != null && "false".equals(value)) {
                            upgrade = true;
                            prop = p;
                            break;
                        }
                    }
                }
            }
        }

        if (logger.isLoggable(Level.FINE)) {
            logger.fine("===> Upgrade? <==");
        }
        if (upgrade) {
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("===> Upgrade! <==");
            }
            boolean success = false;
            try {
                File dir = new File(root, "lib/install/databases/upgrade");

                if (!dir.exists()) {
                    logger.log (Level.WARNING, "Cannot upgrade EJBTimerService: " +
                            "required directory is not available");
                } else {
                    Java2DBProcessorHelper h = new Java2DBProcessorHelper(TIMER_SERVICE_APP_NAME);
                    success = h.executeDDLStatement(
                            dir.getCanonicalPath() + "/ejbtimer_upgrade_", resource);
                    ConfigSupport.apply(new SingleConfigCode<Property>() {
                        public Object run(Property p) throws PropertyVetoException, TransactionFailure {
                            p.setValue("true");
                            return null;
                        }
                    }, prop);
                }
            } catch (Exception e) {
                logger.log (Level.WARNING, "", e);
            }
            if (!success) {
                logger.log (Level.SEVERE, "Failed to upgrade EJBTimerService: see log for details");
            }
        }
        return upgrade;
    }
}
