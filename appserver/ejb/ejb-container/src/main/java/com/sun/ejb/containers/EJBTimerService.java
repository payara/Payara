/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Serializable;

import java.sql.Connection;

import com.sun.logging.LogDomains;

import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.CreateException;
import javax.ejb.TimerConfig;
import javax.ejb.Schedule;
import javax.ejb.ScheduleExpression;
import javax.sql.DataSource;

import com.sun.enterprise.admin.monitor.callflow.RequestType;
import com.sun.enterprise.admin.monitor.callflow.Agent;

import com.sun.enterprise.config.serverbeans.EjbContainer;
import com.sun.enterprise.config.serverbeans.EjbTimerService;
import com.sun.enterprise.config.serverbeans.ConfigBeansUtilities;
import org.glassfish.server.ServerEnvironmentImpl;

import org.glassfish.api.invocation.ComponentInvocation;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.ScheduledTimerDescriptor;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;

import javax.transaction.TransactionManager;
import javax.transaction.Transaction;
import javax.transaction.Synchronization;
import javax.transaction.Status;

/*
 * EJBTimerService is the central controller of the EJB timer service.  
 * There is one instance of EJBTimerService per VM. All operations and
 * state transitions on timers pass through EJB Timer Service.  This
 * reduces the overall complexity by encapsulating the timer logic
 * within this class and keeping the supporting classes simple.
 *
 * @author Kenneth Saks
 * @author Marina Vatkina
 */
public class EJBTimerService 
        /**implements com.sun.ejb.spi.distributed.DistributedEJBTimerService **/ {

    private EjbContainerUtil ejbContainerUtil = EjbContainerUtilImpl.getInstance();

    private long nextTimerIdMillis_ = 0;
    private long nextTimerIdCounter_ = 0;
    private String domainName_;
    private boolean isDas;

    // @@@ Double-check that the individual server id, domain name, 
    // and cluster name cannot contain the TIMER_ID_SEP 
    // characters.      

    // separator between components that make up timer id and owner id
    private static final String TIMER_ID_SEP = "@@";

    // Owner id of the server instance in which we are currently running.
    private String ownerIdOfThisServer_;        

    // A cache of timer info for all timers *owned* by this server instance. 
    private TimerCache timerCache_;

    private TimerLocal timerLocal_;
    private boolean shutdown_;

    // Total number of ejb components initialized as timed objects between the
    // start and end of a single server instance.  This value is used during
    // restoreTimers() as an optimization to avoid initialization overhead in
    // the common case that there are no applications with timed objects.  
    // It is NOT intended to be a consistent count of the current number of 
    // timed objects, so there is no need to decrement the number when a 
    // container is undeployed.
    private long totalTimedObjectsInitialized_ = 0;

    private static final Logger logger =
        LogDomains.getLogger(EJBTimerService.class, LogDomains.EJB_LOGGER);

    // Defaults for configurable timer service properties.

    private static final int MAX_REDELIVERIES = 1;
    private static final long REDELIVERY_INTERVAL = 5000;

    private String appID;

    // minimum amount of time between either a timer creation and its first 
    // expiration or between subsequent timer expirations.  
    private long minimumDeliveryInterval_ = EjbContainerUtil.MINIMUM_TIMER_DELIVERY_INTERVAL;

    // maximum number of times the container will attempt to retry a 
    // timer delivery before giving up.
    private long maxRedeliveries_         = MAX_REDELIVERIES;

    // amount of time the container waits between timer redelivery attempts.
    private long redeliveryInterval_      = REDELIVERY_INTERVAL;

    private static final String TIMER_SERVICE_FILE =
        "__timer_service_shutdown__.dat";
    private static final String TIMER_SERVICE_DOWNTIME_FORMAT =
        "yyyy/MM/dd HH:mm:ss";

    // This boolean value would be set in PE to be a default value of false.
    // In case of EE the default value would be true. When set to true the 
    // timer service would have maximim consistency with performance degration.
    private boolean performDBReadBeforeTimeout = false;
    
    private static final String strDBReadBeforeTimeout = "com.sun.ejb.timer.ReadDBBeforeTimeout";
    private boolean foundSysPropDBReadBeforeTimeout = false;

    private Agent agent = ejbContainerUtil.getCallFlowAgent();

    private DataSource timerDataSource = null;

    // Determines what needs to be done on connection failure
    private static final String ON_CONECTION_FAILURE = "operation-on-connection-failure";
    private static final String OP_REDELIVER = "redeliver";
    private static final String OP_STOP = "stop";

    // Possible values "redeliver" and "stop"
    private String operationOnConnectionFailure = null;

    // Allow to reschedule a failed timer for the next delivery
    private static final String RESCHEDULE_FAILED_TIMER = "reschedule-failed-timer";
    private boolean rescheduleFailedTimer = false;

    public EJBTimerService(String appID, TimerLocal timerLocal) throws Exception {
        timerLocal_ = timerLocal;
        timerCache_     = new TimerCache();
        shutdown_       = false;
        this.appID = appID;

        // Verify that the DataSource ref is correct and store it to check if connections can be aquired if
        // the timeout fails
        lookupTimerResource();

        ServerEnvironmentImpl env = ejbContainerUtil.getServerEnvironment();

        // Compose owner id for all timers created with this 
        // server instance.  
        ownerIdOfThisServer_ = env.getInstanceName();

        domainName_ = env.getDomainName();
        isDas = env.isDas() || env.isEmbedded();

        initProperties();
    }

    private void initProperties() {

        try {
            
            // Check for property settings from domain.xml
            EjbContainer ejbc = ejbContainerUtil.getEjbContainer();
            EjbTimerService ejbt = ejbc.getEjbTimerService();

            if( ejbt != null ) {

                String valString = ejbt.getMinimumDeliveryIntervalInMillis();
                long val = (valString != null) ? 
                    Long.parseLong(valString) : -1;
                    
                if( val > 0 ) {
                    minimumDeliveryInterval_ = val;
                }

                valString = ejbt.getMaxRedeliveries();
                val = (valString != null) ? Long.parseLong(valString) : -1;
                // EJB 2.1 specification minimum is 1
                if( val > 0 ) {
                    maxRedeliveries_ = val;
                }

                valString = ejbt.getRedeliveryIntervalInternalInMillis();
                val = (valString != null) ? Long.parseLong(valString) : -1;
                if( val > 0 ) {
                    redeliveryInterval_ = val;
                }

                // If the system property com.sun.ejb.timer.ReadDBBeforeTimeout
                // is defined by the user use that the value of the flag
                // performDBReadBeforeTimeout 
                foundSysPropDBReadBeforeTimeout = 
                    getDBReadBeforeTimeoutProperty();

                // The default value for ReadDBBeforeTimeout in case of PE 
                // is false. For SE/EE the correct default would set when the 
                // EJBLifecyleImpl gets created as part of the EE lifecycle module
                setPerformDBReadBeforeTimeout( false );

                operationOnConnectionFailure = ejbt.getPropertyValue(ON_CONECTION_FAILURE);
                rescheduleFailedTimer = Boolean.valueOf(ejbt.getPropertyValue(RESCHEDULE_FAILED_TIMER));

                // Load confing listener
                ejbContainerUtil.getDefaultHabitat().getComponent(EJBTimerServiceConfigListener.class);
            }

        } catch(Exception e) {
            logger.log(Level.FINE, "Exception converting timer service " +
               "domain.xml properties.  Defaults will be used instead.", e);
        }

        logger.log(Level.FINE, "EJB Timer Service properties : " +
                   "min delivery interval = " + minimumDeliveryInterval_ +
                   "\nmax redeliveries = " + maxRedeliveries_ +
                   "\nredelivery interval = " + redeliveryInterval_);
    }

    synchronized void timedObjectCount() {
        totalTimedObjectsInitialized_++;
    }

    /**
     * Return the ownerId of the server instance in
     * which we are running.
     */
    String getOwnerIdOfThisServer() {
        return ownerIdOfThisServer_;
    }

    /**
     *--------------------------------------------------------------
     * Methods to be implemented for DistributedEJBTimerService
     *--------------------------------------------------------------
     */

    /**
     * Create EJBException using the exception that is passed in
     */
    private EJBException createEJBException( Exception ex ) {
        EJBException ejbEx = new EJBException();
        ejbEx.initCause(ex);
        return ejbEx;
    }

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

    public void setPerformDBReadBeforeTimeout( boolean defaultDBReadValue ) {

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
                "ContainerFactoryImpl.getDebugMonitoringDetails(), " +
                " Exception when trying to " + 
                "get the System property - ", e);
        }
        return result;
    }

    /**
     * Called at server startup *after* user apps have been re-activated
     * to restart any active EJB timers.  
     */
    public boolean restoreEJBTimers() {
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
            ejbContainerUtil.setEJBTimerService(null);

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
            TimerSchedule ts = timer.getTimerSchedule();
            if (ts != null && ts.isAutomatic()) {
                long containerId = timer.getContainerId();
                BaseContainer container = getContainer(containerId);
                if( container != null ) {
                    TimerPrimaryKey timerId = getPrimaryKey(timer);
                    container.addSchedule(timerId, ts);
                }
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
                if( container.containerType == BaseContainer.ContainerType.ENTITY) {
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
                    TimerSchedule ts = timer.getTimerSchedule();

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

    void shutdown() {
        // Set flag to prevent any new timer expirations.
        shutdown_ = true;
    }

    /**
     * Cancel all timers associated with a particular entity bean 
     * identity.  This is typically called when an entity bean is 
     * removed.  Note that this action falls under the normal EJB 
     * Timer removal semantics, which means it can be rolled back 
     * if the transaction rolls back.
     */
    void cancelEntityBeanTimers(long containerId, Object primaryKey) {
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

    /**
     * Remove from the cache and stop all timers associated with a particular ejb container
     * and known to this server instance.
     * This is typically called when an ejb is disabled or on a server shutdown
     * to avoid accidentally removing a valid timer.  
     * This is also called when an ejb is disabled as part of an undeploy. Removal
     * of the associated timer from the database is done as the last step of undeployment.
     */
    void stopTimers(long containerId) {
        Set<TimerPrimaryKey> timerIds = null;

        timerIds = timerLocal_.findTimerIdsByContainer(containerId);
        timerIds.addAll(timerCache_.getNonPersistentTimerIdsForContainer(containerId));

        for(TimerPrimaryKey nextTimerId: timerIds) {
            RuntimeTimerState nextTimerState = null;
            try {
                nextTimerState = getTimerState(nextTimerId);
                if( nextTimerState != null ) {
                    synchronized(nextTimerState) {
                        if( nextTimerState.isScheduled() ) {
                            EJBTimerTask timerTask = 
                                nextTimerState.getCurrentTimerTask();
                            timerTask.cancel();
                        } 
                    }
                }
            } catch(Exception e) {
                logger.log(Level.WARNING, "ejb.destroy_timer_error",
                           new Object[] { nextTimerId });
                logger.log(Level.WARNING, "", e);
            } finally {
                if( nextTimerState != null ) {
                    timerCache_.removeTimer(nextTimerId);
                }
            }
        }
    }

    /**
     * Destroy all timers associated with a particular ejb container
     * This is typically called when an ejb is undeployed.  It expunges
     * all timers whose timed object matches the given container.  In
     * the case of an entity bean container, all timers associated with
     * any of that container's entity bean identities will be destroyed.
     * This action *can not* be rolled back.  
     */
    public void destroyTimers(long containerId) {
        _destroyTimers(containerId, false);
    }

    /**
     * Destroy all timers associated with a particular application.
     * This is called when an application is undeployed.  It expunges
     * all timers whose timed object matches the given application.  In
     * the case of an entity bean container, all timers associated with
     * any of that container's entity bean identities will be destroyed.
     * This action *can not* be rolled back.  
     */
    public void destroyAllTimers(long applicationId) {
        _destroyTimers(applicationId, true);
    }

    private void _destroyTimers(long id, boolean all) {
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

    void rescheduleTask(TimerPrimaryKey timerId, Date expiration) {
        scheduleTask(timerId, expiration, true);
    }

    void scheduleTask(TimerPrimaryKey timerId, Date expiration) {
        scheduleTask(timerId, expiration, false);
    }

    void scheduleTask(TimerPrimaryKey timerId, Date expiration, 
                      boolean rescheduled) {
    
        RuntimeTimerState timerState = getTimerState(timerId);

        if( timerState != null ) {
            synchronized(timerState) {

                Date timerExpiration = expiration;
                if( !rescheduled ) {
                    // Guard against very small timer intervals. The EJB Timer 
                    // service is defined in units of milliseconds, but it is
                    // intended for coarse-grained events.  Very small timer
                    // intervals (e.g. 1 millisecond) are likely to overload 
                    // the server, so compensate by adjusting to a configurable
                    // minimum interval. 
                    Date cutoff = new Date(new Date().getTime() + 
                                           getMinimumDeliveryInterval());
                    if( expiration.before(cutoff) ) {
                        timerExpiration = cutoff;
                    }
                }

                EJBTimerTask timerTask = 
                    new EJBTimerTask(timerExpiration, timerId, this);
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, (rescheduled ? "RE-" : "") + 
                               "Scheduling " + timerState + 
                               " for timeout at " + timerExpiration);
                }
                if( rescheduled ) {
                    timerState.rescheduled(timerTask);
                } else {
                    timerState.scheduled(timerTask);
                }

                java.util.Timer jdkTimer = ejbContainerUtil.getTimer();
                jdkTimer.schedule(timerTask, timerExpiration);            
            }
        } else {
            
            logger.log(Level.FINE, "No timer state found for " +
                       (rescheduled ? "RE-schedule" : "schedule") +
                       " request of " + timerId + 
                       " for timeout at " + expiration);            
        }
    }


    /**
     * Called by #cancelTimerSynchronization() to cancel the next scheduled expiration 
     * for a timer.
     * @return (initialExpiration time) if state is CREATED or
     *         (time that expiration would have occurred) if state=SCHEDULED or
     *         (null) if state is BEING_DELIVERED or timer id not found
     */
    Date cancelTask(TimerPrimaryKey timerId) {
       
        Date timeout = null;

        RuntimeTimerState timerState = getTimerState(timerId);
        if( timerState != null ) {
            synchronized(timerState) {

                if( timerState.isCreated() ) {
                    timeout = timerState.getInitialExpiration();
                } else if( timerState.isScheduled() ) {
                    EJBTimerTask timerTask = timerState.getCurrentTimerTask();
                    timeout = timerTask.getTimeout();
                    timerTask.cancel();
                }
                timerState.cancelled();

            }
        } else {
            logger.log(Level.FINE, "No timer state found for " +
                       "cancelTask request of " + timerId);                   
        }

        return timeout;
    }

    /**
     * Called from TimerBean in case where Timer is cancelled from within
     * its own ejbTimeout method and then rolled back.
     */
    void restoreTaskToDelivered(TimerPrimaryKey timerId) {
        
        RuntimeTimerState timerState = getTimerState(timerId);
        if( timerState != null ) {
            synchronized(timerState) {
                timerState.restoredToDelivered();
            }
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Restoring " + timerId + 
                   " to delivered state after it was cancelled and " +
                   " rolled back from within its own ejbTimeout method");
            }
        } else {
            logger.log(Level.FINE, "No timer state found for " +
                       "restoreTaskToDelivered request of " + timerId);       
        }
    }

    void expungeTimer(TimerPrimaryKey timerId) {
        // Expunge timer from timer cache without removing timer associated
        // timer bean.
        expungeTimer(timerId, false);
    }


    private Date calcInitialFixedRateExpiration(long timerServiceWentDownAt,
            RuntimeTimerState timerState)
    {
        if (!timerState.isPeriodic()) {
            throw new IllegalStateException();
        }
        Date now = new Date();

        long nowMillis = now.getTime();
        long initialExpiration = timerState.getInitialExpiration().getTime();

        long now2initialDiff = nowMillis - initialExpiration;
        long count = now2initialDiff / timerState.getIntervalDuration();
        long previousExpiration = 
            initialExpiration  + (count * timerState.getIntervalDuration());

        if ((previousExpiration >= timerServiceWentDownAt)
                && (previousExpiration <= nowMillis))
        {
            //We certainly missed this one while the server was down
            logger.log(Level.INFO, "ejb.deliver_missed_timer",
                       new Object[] { timerState.getTimerId(),
                                      new Date(previousExpiration) });
            return now;
        } else {
            //Calculate the new expiration time
            return calcNextFixedRateExpiration(timerState);
        }

    }

    private Date calcNextFixedRateExpiration(RuntimeTimerState timerState) {

        if( !timerState.isPeriodic() ) {
            throw new IllegalStateException("Timer " + timerState + " is " +
                                            "not a periodic timer");
        }

        TimerSchedule ts = timerState.getTimerSchedule();
        if (ts != null) {
            return getNextScheduledTimeout(ts);
        }

        Date initialExpiration = timerState.getInitialExpiration();
        long intervalDuration  = timerState.getIntervalDuration();

        return calcNextFixedRateExpiration(initialExpiration, intervalDuration);
    }
    
    private Date calcNextFixedRateExpiration(Date initialExpiration,
                                             long intervalDuration) {       

        Date now = new Date();
        long nowMillis = now.getTime();

        // In simplest case, initial expiration hasn't even occurred yet.
        Date nextExpirationTime = initialExpiration;
        
        if( now.after(initialExpiration) ) {
            long timeSinceInitialExpire = 
                (nowMillis - initialExpiration.getTime());
                                
            // number of time intervals since initial expiration.
            // intervalDuration is guaranteed to be >0 since this is a 
            // periodic timer.
            long numIntervals = 
                (timeSinceInitialExpire / intervalDuration);
            
            // Increment the number of intervals and multiply by the interval 
            // duration to calculate the next fixed-rate boundary.
            nextExpirationTime = new Date(initialExpiration.getTime() +
                ((numIntervals + 1) * intervalDuration));
        }

        return nextExpirationTime;
    }

    /**
     * Remove all traces of a timer.  This should be written defensively
     * so that if expunge is called multiple times for the same timer id,
     * the second, third, fourth, etc. calls will not cause exceptions.
     */
    private void expungeTimer(TimerPrimaryKey timerId, 
                              boolean removeTimerBean) {
        // First remove timer bean.  Don't update cache until
        // afterwards, since accessing of timer bean might require
        // access to timer state(e.g. timer application classloader)
        if( removeTimerBean ) {
            removeTimerBean(timerId);
        }
        timerCache_.removeTimer(timerId);
    }

    /**
     * @param primaryKey can be null if timed object is not an entity bean.
     * @return Primary key of newly created timer
     */
    TimerPrimaryKey createTimer(long containerId, long applicationId, Object timedObjectPrimaryKey,
                                long initialDuration, long intervalDuration, 
                                TimerConfig timerConfig) throws CreateException {

        Date now = new Date();

        Date initialExpiration = new Date(now.getTime() + initialDuration);

        return createTimer(containerId, applicationId, timedObjectPrimaryKey, 
                           initialExpiration, intervalDuration, timerConfig);
    }

    /**
     * @param primaryKey can be null if timed object is not an entity bean.
     * @return Primary key of newly created timer
     */
    TimerPrimaryKey createTimer(long containerId, long applicationId, Object timedObjectPrimaryKey,
                                Date initialExpiration, long intervalDuration,
                                TimerConfig timerConfig) throws CreateException {
        return createTimer(containerId, applicationId, timedObjectPrimaryKey, 
                           initialExpiration, intervalDuration, null, timerConfig);
    }

    /**
     * @return Primary key of newly created timer
     */
    TimerPrimaryKey createTimer(long containerId, long applicationId, TimerSchedule schedule, 
                                TimerConfig timerConfig, String server_name) 
                                throws CreateException {

        return createTimer(containerId, applicationId, null, null, 0, schedule, timerConfig, server_name);
    }

    /**
     * @param primaryKey can be null if timed object is not an entity bean.
     * @return Primary key of newly created timer
     */
    TimerPrimaryKey createTimer(long containerId, long applicationId, Object timedObjectPrimaryKey,
                                TimerSchedule schedule, TimerConfig timerConfig) 
                                throws CreateException {

        return createTimer(containerId, applicationId, timedObjectPrimaryKey, 
                           null, 0, schedule, timerConfig, ownerIdOfThisServer_);
    }

    /**
     * @param primaryKey can be null if timed object is not an entity bean.
     * @return Primary key of newly created timer
     */
    private TimerPrimaryKey createTimer(long containerId, long applicationId, Object timedObjectPrimaryKey,
                                Date initialExpiration, long intervalDuration,
                                TimerSchedule schedule, TimerConfig timerConfig) 
                                throws CreateException {

        return createTimer(containerId, applicationId, timedObjectPrimaryKey, initialExpiration,
                intervalDuration, schedule, timerConfig, ownerIdOfThisServer_);
    }

    /**
     * @param primaryKey can be null if timed object is not an entity bean.
     * @return Primary key of newly created timer
     */
    private TimerPrimaryKey createTimer(long containerId, long applicationId, Object timedObjectPrimaryKey,
                                Date initialExpiration, long intervalDuration,
                                TimerSchedule schedule, TimerConfig timerConfig,
                                String server_name) 
                                throws CreateException {

        BaseContainer container = getContainer(containerId);
        boolean startTimers = ownerIdOfThisServer_.equals(server_name);
        if (startTimers) {
            if( container == null ) {
                throw new CreateException("invalid container id " + containerId +
                                      " in createTimer request");
            }
        
            Class ejbClass = container.getEJBClass();
            if( !container.isTimedObject() ) {
                throw new CreateException
                        ("Attempt to create an EJB Timer from a bean that is " +
                         "not a Timed Object.  EJB class " + ejbClass + 
                         " must implement javax.ejb.TimedObject or " +
                         " annotation a timeout method with @Timeout");
            }
        }

        TimerPrimaryKey timerId = new TimerPrimaryKey(getNextTimerId());

        if (schedule != null) {
            Calendar next = schedule.getNextTimeout();
            if( !schedule.isValid(next) ) {
                initialExpiration = new Date();
            } else {
                initialExpiration = next.getTime();
            }
        }

        if( logger.isLoggable(Level.FINE) ) {
            logger.log(Level.FINE, "@@@ Created timer [" + timerId + 
                    "] with the first expiration set to: " + initialExpiration);
        }

        if (timerConfig == null) {
            // Easier create one than check everywhere for null...
            timerConfig = new TimerConfig();
        }

        RuntimeTimerState timerState = 
            new RuntimeTimerState(timerId, initialExpiration, 
                                  intervalDuration, containerId, container, 
                                  timedObjectPrimaryKey,
                                  schedule, timerConfig.getInfo(),
                                  timerConfig.isPersistent());

        synchronized(timerState) {
            // Add timer entry before calling TimerBean.create, since 
            // create() actions might call back on EJBTimerService and 
            // need access to timer cache.
            if (startTimers) {
                timerCache_.addTimer(timerId, timerState);
            }

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

        return timerId;
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
    Map<TimerPrimaryKey, Method> recoverAndCreateSchedules(
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
                TimerSchedule ts = timer.getTimerSchedule();
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

            createSchedules(containerId, applicationId, schedules, result, ownerIdOfThisServer_, true, (deploy && isDas));

            tm.commit();

        } catch(Exception e) {
            recoverAndCreateSchedulesError(e, tm);
        }

        return result;
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
     * Create automatic timers defined by the @Schedule annotation on the EJB bean.
     * 
     * If this method is called on a deploy in a clustered deployment, only persistent schedule
     * based timers will be created. And no timers will be scheduled.
     * If it is called from deploy on a non-clustered instance, both
     * persistent and non-persistent timers will be created.
     * Otherwise only non-persistent timers are created by this method.
     */
    private void createSchedules(long containerId, long applicationId,
            Map<?, List<ScheduledTimerDescriptor>> schedules,
            Map<TimerPrimaryKey, Method> result,
            String server_name, boolean startTimers, boolean deploy) throws Exception {

        for (Map.Entry<?, List<ScheduledTimerDescriptor>> entry : schedules.entrySet()) {
            Object key = entry.getKey();
            String mname = null;
            int args_length = 0;
            if (key instanceof Method) {
                mname = ((Method)key).getName();
                args_length = ((Method)key).getParameterTypes().length;
            } else {
                mname = ((MethodDescriptor)key).getName();
                args_length = ((MethodDescriptor)key).getJavaParameterClassNames().length;
            }

            for (ScheduledTimerDescriptor sch : entry.getValue()) {
                boolean persistent = sch.getPersistent();
                if ((persistent && !deploy) || (!persistent && !startTimers)) {
                    // Do not recreate schedule-based timers on restart or create
                    // non-persistent timers on a clustered deploy
                    continue;
                }

                TimerSchedule ts = new TimerSchedule(sch, mname, args_length);
                TimerConfig tc = new TimerConfig();
                String info = sch.getInfo();
                if (info != null && !info.equals("")) {
                    tc.setInfo(info);
                }
                tc.setPersistent(persistent);
                TimerPrimaryKey tpk = createTimer(containerId, applicationId, ts, tc, server_name);
                if( logger.isLoggable(Level.FINE) ) {
                        logger.log(Level.FINE, "@@@ CREATED new schedule: " + ts.getScheduleAsString() + " FOR method: " + key);
                }

                if (startTimers && result != null) {
                    result.put(tpk, (Method)key);
                }
            }
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
     * @param primaryKey can be null if not entity bean
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
     * @param primaryKey can be null if not entity bean
     * @return Collection of Timer Ids.
     */
    Collection<TimerPrimaryKey> getTimerIds(long containerId, Object timedObjectPrimaryKey) {

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
        timerIdsForTimedObject.addAll(
                timerCache_.getNonPersistentActiveTimerIdsForContainer(containerId));

        return timerIdsForTimedObject;
    }
    
    /**
     * Get the application class loader for the timed object
     * that created a given timer.
     */
    ClassLoader getTimerClassLoader(long containerId) {       
        BaseContainer container = getContainer(containerId);        
        return (container != null) ? container.getClassLoader() : null;
    }

    private RuntimeTimerState getTimerState(TimerPrimaryKey timerId) {
        return timerCache_.getTimerState(timerId);
    }

    private RuntimeTimerState getNonPersistentTimerState(TimerPrimaryKey timerId) {
        return timerCache_.getNonPersistentTimerState(timerId);
    }

    // Returns a Set of active non-persistent timer ids for this server
    Set<TimerPrimaryKey> getActiveTimerIdsByThisServer() {
        return timerCache_.getActiveTimerIdsByThisServer();
    }

    //
    // Logic used by TimerWrapper for javax.ejb.Timer methods.
    //

    void cancelTimer(TimerPrimaryKey timerId) 
            throws FinderException, Exception {

        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            if( rt.isCancelled()) {
                // already cancelled or removed
                return;
            }

            cancelTimerSynchronization(null, timerId,
                rt.getContainerId(), ownerIdOfThisServer_, false);
        } else {

            // @@@ We can't assume this server instance owns the timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.

            // Look up timer bean from database.  Throws FinderException if
            // timer no longer exists, which is converted by the caller into a
            // NoSuchObjectLocalException.
            timerLocal_.cancel(timerId);            
        }
        
    }

    /**
     * Return next planned timeout for this timer.  We have a fair amount
     * of leeway regarding the consistency of this information.  We should
     * strive to detect the case where the timer no longer exists.  However, 
     * since the current timer instance may not even own this timer, 
     * it's difficult to know the exact time of delivery in another server 
     * instance.  In the case of single-action timers, we return the 
     * expiration time that was provided upon timer creation.  For 
     * periodic timers, we can derive the next scheduled fixed rate
     * expiration based on the initial expiration and the interval.  
     */
    Date getNextTimeout(TimerPrimaryKey timerId) throws FinderException {

        Date initialExpiration = null;
        long intervalDuration = 0;
        TimerSchedule ts = null;

        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            initialExpiration = rt.getInitialExpiration();
            intervalDuration = rt.getIntervalDuration();
            ts = rt.getTimerSchedule();
        } else {

            // @@@ We can't assume this server instance owns the persistent timer
            // so always ask the database.  Investigate possible use of
            // timer cache for optimization.

            TimerState timer = getPersistentTimer(timerId);
            initialExpiration = timer.getInitialExpiration();
            intervalDuration = timer.getIntervalDuration();
            ts = timer.getTimerSchedule();
        }

        Date nextTimeout = initialExpiration;
        if (ts != null) {
            nextTimeout = getNextScheduledTimeout(ts);
            // The caller is responsible to return 0 or -1 for the time remaining....
            
        } else if (intervalDuration > 0) {
            nextTimeout = calcNextFixedRateExpiration(initialExpiration, 
                               intervalDuration);
        }

        return nextTimeout;
    }

    Serializable getInfo(TimerPrimaryKey timerId) throws FinderException {
        
        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            return rt.getInfo();
        }

        // @@@ We can't assume this server instance owns the persistent timer
        // so always ask the database.  Investigate possible use of
        // timer cache for optimization.

        TimerState timer = getPersistentTimer(timerId);
        return timer.getInfo();
    }

    ScheduleExpression getScheduleExpression(TimerPrimaryKey timerId) throws FinderException {
        
        TimerSchedule ts = getTimerSchedule(timerId);
        return (ts == null)? null : ts.getScheduleExpression();
    }

    boolean isCalendarTimer(TimerPrimaryKey timerId) throws FinderException {
        
        TimerSchedule ts = getTimerSchedule(timerId);
        return (ts != null);
    }

    boolean isPersistent(TimerPrimaryKey timerId) throws FinderException {
        
        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
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
    
    boolean timerExists(TimerPrimaryKey timerId) {
        boolean exists = false;

        // Check non-persistent timers first
        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            exists = rt.isActive();
        }

        // @@@ We can't assume this server instance owns the persistent timer
        // so always ask the database.  Investigate possible use of
        // timer cache for optimization.
        
        TimerState timer = timerLocal_.findTimer(timerId);
        if (timer != null) {
            // Make sure timer hasn't been cancelled within the current tx.
            exists = timer.isActive();
        } 
        
        return exists;
    }

    /**
     * Called by #getScheduleExpression and #isCalendarTimer
     */
    private TimerSchedule getTimerSchedule(TimerPrimaryKey timerId) throws FinderException {

        // Check non-persistent timers first
        TimerSchedule ts = null;

        RuntimeTimerState rt = getNonPersistentTimer(timerId);
        if (rt != null) {
            ts = rt.getTimerSchedule();
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

    private RuntimeTimerState getNonPersistentTimer(TimerPrimaryKey timerId) 
            throws FinderException {
        RuntimeTimerState rt = getNonPersistentTimerState(timerId);
        if (rt != null) {
            if (rt.isCancelled()) {
                // The timer has been cancelled within this tx.
                throw new FinderException("Non-persistent timer " + timerId 
                        + " does not exist");
            }
        }

        return rt;
    }

    private BaseContainer getContainer(long containerId) {
        return ejbContainerUtil.getContainer(containerId);
    }

    /**
     * Called from timer thread.  Used to deliver ejb timeout.
     */
    private void deliverTimeout(TimerPrimaryKey timerId) {

        if( logger.isLoggable(Level.FINE) ) {
            logger.log(Level.FINE, "EJBTimerService.deliverTimeout(): work " 
                       + 
                       "thread is processing work for timerId = " + timerId);
        }

        if( shutdown_ ) { 
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Cancelling timeout for " + timerId + 
                           " due to server shutdown.  Expiration " +
                           " will occur when server is restarted.");
            }
            return; 
        }
    
        RuntimeTimerState timerState = getTimerState(timerId);

        //
        // Make some defensive state checks.  It's possible that the
        // timer state changed between the time that the JDK timer task expired
        // and we got called on this thread.
        //

        if( timerState == null ) { 
            logger.log(Level.FINE, "Timer state is NULL for timer " + timerId +
                       " in deliverTimeout");
            return; 
        }

        BaseContainer container = getContainer(timerState.getContainerId());

        synchronized(timerState) {
            if( container == null ) {
                logger.log(Level.FINE, "Unknown container for timer " + 
                           timerId + " in deliverTimeout.  Expunging timer.");
                expungeTimer(timerId, true);
                return;
            } else if ( !timerState.isBeingDelivered() ) {
                logger.log(Level.FINE, "Timer state = " + 
                           timerState.stateToString() + 
                           "for timer " + timerId + " before callEJBTimeout");
                return;
            } else {
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "Calling ejbTimeout for timer " +
                               timerState);
                }
            }
        }
         
        try {
                    
            agent.requestStart(RequestType.TIMER_EJB);
            container.onEnteringContainer();
            // Need to address the case that another server instance 
            // cancelled this timer.  For maximum consistency, we will need 
            // to do a database read before each delivery.  This can have 
            // significant performance implications, so investigate possible 
            // reduced consistency tradeoffs.  
            if( timerState.isPersistent() && performDBReadBeforeTimeout) {

                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "For Timer :" + timerId + 
                    ": check the database to ensure that the timer is still " +
                    " valid, before delivering the ejbTimeout call" );
                }

                if( ! checkForTimerValidity(timerId) ) {
                    // The timer for which a ejbTimeout is about to be delivered
                    // is not present in the database. This could happen in the 
                    // SE/EE case as other server instances (other than the owner)
                    // could call a cancel on the timer - deleting the timer from 
                    // the database.
                    // Also it is possible that the timer is now owned by some other
                    // server instance
                    return;
                } 
            }

            ///
            // Call container to invoke ejbTimeout on the bean instance.
            // The remaining actions are divided up into two categories :
            //
            // 1) Actions that should be done within the same transaction
            //    context as the ejbTimeout call itself.  These are 
            //    handled by having the ejb container call back on the
            //    postEjbTimeout method after it has invoked bean.ejbTimeout
            //    but *before* it has called postInvoke.  That way any
            //    transactional operations like setting the last update time
            //    for periodic timers or removing a successfully delivered
            //    single-action timer can be done within the same tx as
            //    the ejbTimeout itself.  Note that there is no requirement
            //    that the ejbTimeout will be configured for CMT/RequiresNew.
            //    If there isn't a container-managed transaction,
            //    postEjbTimeout will still be called, and the database
            //    operations will be done in their own transaction.  While
            //    partitioning the actions like this adds some complexity,
            //    it's preferable to pushing this detailed timer semantics
            //    into the container's callEJBTimeout logic.
            //
            // 2) Post-processing for setting up next timer delivery and
            //    other redelivery conditions.  
            // 

            // Do not deliver bogus timeout, but continue processing and 
            // cancel such timer

            boolean redeliver = (timerState.isExpired())? false :
                    container.callEJBTimeout(timerState, this);

            if( shutdown_ ) {
                // Server is shutting down so we can't finish processing 
                // the timer expiration.  
                if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "Cancelling timeout for " + timerId
                               +
                               " due to server shutdown. Expiration will " +
                               " occur on server restart");
                }
                return;
            }

            // Resynchronize on timer state since a state change could have
            // happened either within ejbTimeout or somewhere else             

            timerState = getTimerState(timerId);

            if( timerState == null ) { 
                // This isn't an error case.  The most likely reason is that
                // the ejbTimeout method itself cancelled the timer or the bean was disabled.
                logger.log(Level.FINE, "Timer no longer exists for " + 
                           timerId + " after callEJBTimeout");
                return; 
            }


            synchronized(timerState) {
                Date now = new Date();
                boolean reschedule = false;

                if( timerState.isCancelled() ) {
                    // nothing more to do.
                } else if (timerState.isExpired()) {
                    // schedule-based timer without valid expiration
                    cancelTimer(timerId);
                } else if( redeliver ) {
                    int numDeliv = timerState.getNumFailedDeliveries() + 1;
                    if( timerState.getNumFailedDeliveries() <
                            getMaxRedeliveries() || redeliverOnFailedConnection()) {
                        Date redeliveryTimeout = new Date
                            (now.getTime() + getRedeliveryInterval());
                        if( logger.isLoggable(Level.FINE) ) {
                            logger.log(Level.FINE,"Redelivering " + timerState);
                        }
                        rescheduleTask(timerId, redeliveryTimeout);
                    } else if (stopOnFailedConnection()) {
                        // stop Timer Service
                        shutdown();
                    } else if (rescheduleFailedTimer) {
                        logger.log(Level.INFO, "ejb.timer_reschedule_after_max_deliveries",
                           new Object[] { timerState.toString(), numDeliv});
                        reschedule = true;
                    } else {
                        logger.log(Level.INFO, "ejb.timer_exceeded_max_deliveries",
                           new Object[] { timerState.toString(), numDeliv});
                        expungeTimer(timerId, true);
                    }
                } else {
                    reschedule = true;
                }
                if( reschedule && (redeliver || timerState.isPeriodic()) ) {

                    // Any necessary transactional operations would have
                    // been handled in postEjbTimeout callback.  Here, we
                    // just schedule the JDK timer task for the next ejbTimeout
                    
                    Date expiration = calcNextFixedRateExpiration(timerState);
                    if (expiration != null) {
                        scheduleTask(timerId, expiration);
                    } else {
                        // schedule-based timer ended.
                        cancelTimer(timerId);
                    }
                } else {
                   
                    // Any necessary transactional operations would have
                    // been handled above or in postEjbTimeout callback.  Nothing
                    // more to do for this single-action timer that was
                    // successfully delivered.                 
                }
            }

        } catch(Exception e) {
            logger.log(Level.FINE, "callEJBTimeout threw exception " +
                       "for timer id " + timerId , e);
            expungeTimer(timerId, true);
        } finally {
            container.onLeavingContainer();
            agent.requestEnd();
        }
    }

    /**
     *  Called from BaseContainer during callEJBTimeout after bean.ejbTimeout
     *  but before postInvoke.  NOTE that this method is called whether or not
     *  the ejbTimeout method is configured for container-managed transactions.
     *  This method is *NOT* called if the container has already determined
     *  that a redelivery is necessary.
     *
     *  @return true if successful , false otherwise.  
     */
    boolean postEjbTimeout(TimerPrimaryKey timerId) {    

        boolean success = true;

        if( shutdown_ ) {
            // Server is shutting down so we can't finish processing 
            // the timer expiration.  
            return success;
        }

        // Resynchronize on timer state since a state change could have
        // happened either within ejbTimeout or somewhere else             

        RuntimeTimerState timerState = getTimerState(timerId);

        if( timerState != null ) { 
        
            // Since the ejbTimeout was called successfully increment the
            // delivery count
            BaseContainer container = getContainer(timerState.getContainerId());
            container.incrementDeliveredTimedObject();
                                  
            synchronized(timerState) {
                
                if( timerState.isCancelled() ) {
                    // nothing more to do. 
                } else {
                    
                    try {
                        TimerState timer = null;
                        if (timerState.isPersistent()) {
                            timer = getValidTimerFromDB( timerId );
                            if( null == timer ) {
                                return false;
                            }
                        }

                        if( timerState.isPeriodic() ) {
                            Date now = new Date();
                            if (timerState.isPersistent()) {
                                timer.setLastExpiration(now);   
                            }
                            
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

                        } else {
                                                        
                            if( logger.isLoggable(Level.FINE) ) {
                                logger.log(Level.FINE, "Single-action timer " + 
                                   timerState + " was successfully delivered. "
                                   + " Removing...");
                            }

                            // Timer has expired sucessfully, so remove it.
// XXX the original logic relied on the tx being executed on CMP if needed
// XXX the original logic didn't need a FinderException as it assumed that it's in the same tx XXX
                            cancelTimer(timerId);
                        }
                    } catch(Exception e) {
                        
                        // @@@ i18N
                        logger.log(Level.WARNING, "Error in post-ejbTimeout " +
                                   "timer processing for " + timerState, e);
                        success = false;
                    }                                       
                }
            }
        } 

        return success;
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

    /**
     * This method is called back from the EJBTimerTask object 
     * on the JDK Timer Thread.  Work performed in this callback 
     * should be short-lived, so do a little bookkeeping and then
     * launch a separate thread to invoke ejbTimeout, etc.
     */

    void taskExpired(TimerPrimaryKey timerId) {
        RuntimeTimerState timerState = getTimerState(timerId);

        if( timerState != null ) {
            synchronized(timerState) {
                if( timerState.isScheduled() ) {
                    timerState.delivered();

                    if( logger.isLoggable(Level.FINE) ) {
                        logger.log(Level.FINE, 
                           "Adding work pool task for timer " + timerId);
                    }

                    TaskExpiredWork work = new TaskExpiredWork(this, timerId);
                    ejbContainerUtil.addWork(work);
                } else {
                    logger.log(Level.FINE, "Timer " + timerId + 
                               " is not in scheduled state.  Current state = "
                               + timerState.stateToString());
                }
            }
        } else {
            logger.log(Level.FINE, "null timer state for timer id " + timerId);
        }

        return;
    }

    /**
     * Generate a unique key for the persistent timer object.
     * Key must be unique across server shutdown and startup, and
     * within all server instances sharing the same timer table.
     */
    private synchronized String getNextTimerId() {

        if( nextTimerIdCounter_ <= 0 ) {
            nextTimerIdMillis_  = System.currentTimeMillis();
            nextTimerIdCounter_ = 1;
        } else {
            nextTimerIdCounter_++;
        }

        // @@@ Add cluster ID

        return "" + nextTimerIdCounter_ +
                          TIMER_ID_SEP + nextTimerIdMillis_ +
                          TIMER_ID_SEP + ownerIdOfThisServer_ + 
                          TIMER_ID_SEP + domainName_;
    }

    //
    // Accessors for timer service properties.
    //
    private long getMinimumDeliveryInterval() {
        return minimumDeliveryInterval_;
    }

    private long getMaxRedeliveries() {
        return maxRedeliveries_;
    }

    private long getRedeliveryInterval() {
        return redeliveryInterval_;
    }

    private TimerPrimaryKey getPrimaryKey(TimerState timer) {
        return new TimerPrimaryKey(timer.getTimerId());
    }

    // Used by TimerBean.testTimer
    TimerLocal getTimerLocal() {
        return timerLocal_;
    }

    void addTimerSynchronization(EJBContextImpl context_, String timerId, 
            Date initialExpiration, long containerId, String ownerId) 
            throws Exception {

        addTimerSynchronization(context_, timerId, initialExpiration,
                containerId, ownerId, true);
    }

    void addTimerSynchronization(EJBContextImpl context_, String timerId, 
            Date initialExpiration, long containerId, String ownerId, 
            boolean persistent) throws Exception {

        // context_ is null for a non-persistent timer as it's not called 
        // from the TimerBean.
        if (context_ == null || timerOwnedByThisServer(ownerId)) {
            TimerPrimaryKey pk = new TimerPrimaryKey(timerId);

            ContainerSynchronization containerSynch = getContainerSynch(context_, timerId, persistent);
            if (containerSynch == null) {
                // null ContainerSynchronization for persistent timer would've
                // caused exception in #getContainerSynch(). For non-persistent 
                // timer schedule it right away
                scheduleTask(pk, initialExpiration);
                ejbContainerUtil.getContainer(containerId).incrementCreatedTimedObject();

            } else {

                // Register a synchronization object to handle the commit/rollback
                // semantics and ejbTimeout notifications.
                Synchronization timerSynch =
                        new TimerSynch(pk, TimerState.ACTIVE, initialExpiration,
                                   ejbContainerUtil.getContainer(containerId),
                                   this);

                containerSynch.addTimerSynchronization(pk, timerSynch);
            }

        }
    }

    void cancelTimerSynchronization(EJBContextImpl context_, 
            TimerPrimaryKey timerId, long containerId, String ownerId) 
            throws Exception {

        cancelTimerSynchronization(context_, timerId, containerId, ownerId, true);
    }

    void cancelTimerSynchronization(EJBContextImpl context_, 
            TimerPrimaryKey timerId, long containerId, String ownerId, 
            boolean persistent) throws Exception {

        // Only proceed with JDK timer task cancellation if this timer
        // is owned by the current server instance.
        if( context_ == null || timerOwnedByThisServer(ownerId) ) {

            // Cancel existing timer task.  Save time at which task would
            // have executed in case cancellation is rolled back.  The
            // nextTimeout can be null if the timer is currently being
            // delivered.
            Date nextTimeout = cancelTask(timerId);

            ContainerSynchronization containerSynch = 
                    getContainerSynch(context_, timerId.getTimerId(), persistent);

            if (containerSynch == null) {
                // null ContainerSynchronization for persistent timer would've
                // caused exception in #getContainerSynch(). For non-persistent 
                // timer remove it right away
                expungeTimer(timerId);
                ejbContainerUtil.getContainer(containerId).incrementRemovedTimedObject();
            } else {

                Synchronization timerSynch =
                        containerSynch.getTimerSynchronization(timerId);

                if( timerSynch != null ) {
                    // This timer was created and cancelled within the
                    // same transaction.  No tx synchronization actions
                    // are needed, since whether tx commits or rolls back,
                    // timer will not exist.
                    containerSynch.removeTimerSynchronization(timerId);
                    expungeTimer(timerId);
                } else {
                    // Set tx synchronization action to handle timer cancellation.
                    timerSynch = new TimerSynch(timerId, TimerState.CANCELLED, nextTimeout,
                                        ejbContainerUtil.getContainer(containerId),
                                        this);
                    containerSynch.addTimerSynchronization(timerId, timerSynch);
                }
            }
        }
    }

    /**
     * Checks whether this timer is owned by the server instance in
     * which we are running.
     */
    private boolean timerOwnedByThisServer(String ownerId) {
        String ownerIdOfThisServer = getOwnerIdOfThisServer();
        return ( (ownerIdOfThisServer != null) &&
                 (ownerIdOfThisServer.equals(ownerId)) );
    }

    /** 
     * Returns next schedule-based timeout or null if such schedule will
     * not expire again.
     */
    private Date getNextScheduledTimeout(TimerSchedule ts) {
        Calendar next = ts.getNextTimeout();
        if( ts.isValid(next) ) {
            return next.getTime();
        } else { // Expired or no timeouts available
            return null;
        }
    }

    private ContainerSynchronization getContainerSynch(EJBContextImpl context_, 
            String timerId, boolean persistent) throws Exception {

        Transaction transaction = null;
        if (context_ != null) {
            transaction = context_.getTransaction();
        }

        if( transaction == null ) {
            ComponentInvocation i = ejbContainerUtil.getCurrentInvocation();
            if (i == null) {
                // This should happen only when creating timers for methods annotated with @Schedule
                transaction = ejbContainerUtil.getTransactionManager().getTransaction();
            } else {
                transaction = (Transaction) i.getTransaction();
                if (context_ != null && transaction != null) {
                    // Need to know when it happens
                    logger.log(Level.WARNING, "Context transaction in TimerBean = null. Using " +
                           "invocation instead."); // , new Throwable());
                }
            }
        }

        if( transaction == null ) {
            if (persistent) {
                // Shouldn't happen
                throw new Exception("transaction = null in getContainerSynch " +
                                "for timerId = " + timerId);
            } else {
                // null ContainerSynchronization for non-persistent timer will
                // schedule or cancel it right away
                return null;
            }
        }

        return ejbContainerUtil.getContainerSync(transaction);
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
        String resource_name = ejbContainerUtil.getTimerResource();
        ConnectorRuntime connectorRuntime = ejbContainerUtil.getDefaultHabitat().getByContract(ConnectorRuntime.class);
        timerDataSource = DataSource.class.cast(connectorRuntime.lookupNonTxResource(resource_name, false));
    }

    static String txStatusToString(int txStatus) {
        String txStatusStr = "UNMATCHED TX STATUS";

        switch(txStatus) {
            case Status.STATUS_ACTIVE :
                txStatusStr = "TX_STATUS_ACTIVE";
                break;
            case Status.STATUS_COMMITTED :
                txStatusStr = "TX_STATUS_COMMITTED";
                break;
            case Status.STATUS_COMMITTING :
                txStatusStr = "TX_STATUS_COMMITTING";
                break;
            case Status.STATUS_MARKED_ROLLBACK :
                txStatusStr = "TX_STATUS_MARKED_ROLLBACK";
                break;
            case Status.STATUS_NO_TRANSACTION :
                txStatusStr = "TX_STATUS_NO_TRANSACTION";
                break;
            case Status.STATUS_PREPARED :
                txStatusStr = "TX_STATUS_PREPARED";
                break;
            case Status.STATUS_PREPARING :
                txStatusStr = "TX_STATUS_PREPARING";
                break;
            case Status.STATUS_ROLLEDBACK :
                txStatusStr = "TX_STATUS_ROLLEDBACK";
                break;
            case Status.STATUS_ROLLING_BACK :
                txStatusStr = "TX_STATUS_ROLLING_BACK";
                break;
            case Status.STATUS_UNKNOWN :
                txStatusStr = "TX_STATUS_UNKNOWN";
                break;
            default :
                txStatusStr = "UNMATCHED TX STATUS";
                break;
        }

        return txStatusStr;
    }


    // 
    // This is a global cache of timer data *only for timers owned by
    // this server instance*.  It is not transactionally
    // consistent.  Operations requiring those semantics should query
    // the database for TimerBean info.  Any timer for which there is an
    // active JDK timer task must be contained within this cache. 
    //
    // Note : this class supports concurrent access.
    //
    private static class TimerCache {

        // Maps timer id to timer state.
        private Map timers_;

        // Map of timer information per container.
        //
        // For stateless session beans and message-driven beans, 
        // container id is mapped to a Long value representing the 
        // number of timers.
        // 
        //
        // For entity beans, container id is mapped to a list of
        // primary keys.  NOTE : This list can contain duplicate primary keys
        // in the case where the same entity bean identity has more
        // than one associated timer.
        
        private Map containerTimers_;

        // Map of non-persistent timer id to timer state.
        private Map<TimerPrimaryKey, RuntimeTimerState> nonpersistentTimers_;

        public TimerCache() {
            // Create unsynchronized collections.  TimerCache will 
            // provide concurrency control.
            timers_ = new HashMap();
            containerTimers_ = new HashMap();
            nonpersistentTimers_ = new HashMap<TimerPrimaryKey, RuntimeTimerState>();
        }

        public synchronized void addTimer(TimerPrimaryKey timerId, 
                                          RuntimeTimerState timerState) {
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Adding timer " + timerState);
            }

            timers_.put(timerId, timerState);
            if (!timerState.isPersistent()) {
                nonpersistentTimers_.put(timerId, timerState);
            }

            Long containerId = timerState.getContainerId();

            Object containerInfo = containerTimers_.get(containerId);

            if( timerState.timedObjectIsEntity() ) {
                Collection entityBeans;
                if( containerInfo == null ) {
                    // NOTE : This list *can* contain duplicates, since
                    // the same entity bean can be the timed object for 
                    // multiple timers.
                    entityBeans = new ArrayList();
                    containerTimers_.put(containerId, entityBeans);
                } else {
                    entityBeans = (Collection) containerInfo;
                }
                entityBeans.add(timerState.getTimedObjectPrimaryKey());
            } else {
                Long timerCount = (containerInfo == null) ? 1 :
                    ((Long) containerInfo).longValue() + 1;
                containerTimers_.put(containerId, timerCount);
            }

        }

        /**
         * Remove a timer from the cache. This should be coded 
         * defensively since it's possible it will be called multiple
         * times for the same timer.
         */
        public synchronized void removeTimer(TimerPrimaryKey timerId) {
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Removing timer " + timerId);
            }

            RuntimeTimerState timerState = (RuntimeTimerState) 
                timers_.remove(timerId);

            if( timerState == null) {
                return;
            }

            if (!timerState.isPersistent()) {
                nonpersistentTimers_.remove(timerId);
            }
            Long containerId = timerState.getContainerId();
            Object containerInfo = containerTimers_.get(containerId);
                
            if( containerInfo != null ) {
                if( timerState.timedObjectIsEntity() ) {
                    Collection entityBeans = (Collection) containerInfo;
                    if( entityBeans.size() == 1 ) {
                        // Only one left -- blow away the container.
                        containerTimers_.remove(containerId);
                    } else {
                        // Remove a single instance of this primary key
                        // from the list.  There could still be other
                        // instances of the same primary key.
                        entityBeans.remove
                            (timerState.getTimedObjectPrimaryKey());
                    }
                } else {
                    long timerCount = ((Long) containerInfo).longValue();
                    if( timerCount == 1 ) {
                        // Only one left -- blow away the container
                        containerTimers_.remove(containerId);
                    } else {
                        Long newCount = timerCount - 1;
                        containerTimers_.put(containerId, newCount);
                    }                         
                }
            }
        }

        public synchronized RuntimeTimerState getTimerState(TimerPrimaryKey 
                                                            timerId) {
            return (RuntimeTimerState) timers_.get(timerId);
        }

        public synchronized RuntimeTimerState getNonPersistentTimerState(
                              TimerPrimaryKey timerId) {
            return (RuntimeTimerState) nonpersistentTimers_.get(timerId);
        }

        // True if the given entity bean has any timers and false otherwise.
        public synchronized boolean entityBeanHasTimers(long containerId, 
                                                        Object pkey) {
            Object containerInfo = containerTimers_.get(containerId);
            return (containerInfo != null) ?
                ((Collection) containerInfo).contains(pkey) : false;
        }

        // True if the ejb represented by this container id has any timers
        // and false otherwise.  
        public synchronized boolean containerHasTimers(long containerId) {
            return containerTimers_.containsKey(containerId);
        }

        // Placeholder for logic to ensure timer cache consistency.
        public synchronized void validate() {
        }

        // Returns a Set of non-persistent timer ids for this container
        public synchronized Set<TimerPrimaryKey> getNonPersistentTimerIdsForContainer(
                                        long containerId_) {
            Set<TimerPrimaryKey> result = new HashSet<TimerPrimaryKey>();
            for (Map.Entry<TimerPrimaryKey, RuntimeTimerState> entry : nonpersistentTimers_.entrySet()) {
                TimerPrimaryKey key = entry.getKey();
                RuntimeTimerState rt = entry.getValue();
                if ((rt.getContainerId() == containerId_)) {
                    result.add(key);
                }
            }

            return result;
        }

        // Returns a Set of active non-persistent timer ids for this container
        public synchronized Set<TimerPrimaryKey> getNonPersistentActiveTimerIdsForContainer(
                                        long containerId_) {
            Set<TimerPrimaryKey> result = new HashSet<TimerPrimaryKey>();
            for (Map.Entry<TimerPrimaryKey, RuntimeTimerState> entry : nonpersistentTimers_.entrySet()) {
                TimerPrimaryKey key = entry.getKey();
                RuntimeTimerState rt = entry.getValue();
                if ((rt.getContainerId() == containerId_) && rt.isActive()) {
                    result.add(key);
                }
            }

            return result;
        }

        // Returns a Set of active non-persistent timer ids for this server
        public synchronized Set<TimerPrimaryKey> getActiveTimerIdsByThisServer() {
            Set<TimerPrimaryKey> result = new HashSet<TimerPrimaryKey>();
            for (Map.Entry<TimerPrimaryKey, RuntimeTimerState> entry : nonpersistentTimers_.entrySet()) {
                TimerPrimaryKey key = entry.getKey();
                RuntimeTimerState rt = entry.getValue();
                if (rt.isActive()) {
                    result.add(key);
                }
            }

            return result;
        }

    } //TimerCache{}

    private File getTimerServiceShutdownFile() throws Exception {
        File timerServiceShutdownFile = null;

        String j2eeAppPath = 
                ConfigBeansUtilities.getLocation(appID);

        File timerServiceShutdownDirectory = new File(j2eeAppPath + File.separator);
        if (!timerServiceShutdownDirectory.mkdirs()) {
            if( logger.isLoggable(Level.FINE) ) {
                logger.log(Level.FINE, "Failed to create timerServiceShutdownDirectory");
            }
        }
        timerServiceShutdownFile = new File(j2eeAppPath + File.separator
                    + TIMER_SERVICE_FILE);

        return timerServiceShutdownFile;
    }

    private long getTimerServiceDownAt() {
        long timerServiceWentDownAt = -1;
        BufferedReader br = null;
        try {
            File timerServiceShutdownFile  = getTimerServiceShutdownFile();

            if (timerServiceShutdownFile.exists()) {
                DateFormat dateFormat =  
                    new SimpleDateFormat(TIMER_SERVICE_DOWNTIME_FORMAT);
        
                FileReader fr = new FileReader(timerServiceShutdownFile);
                br = new BufferedReader(fr, 128);
                String line = br.readLine();

                if (line != null) {
                    Date myDate = dateFormat.parse(line);
                    timerServiceWentDownAt = myDate.getTime();
                    logger.log(Level.INFO, "ejb.timer_service_last_shutdown",
                               new Object[] { line });
                } else {
                    logger.log(Level.WARNING, "ejb.timer_service_shutdown_unknown",
                               new Object[] { timerServiceShutdownFile });
                }
            } else {
                logger.log(Level.WARNING, "ejb.timer_service_shutdown_unknown",
                           new Object[] { timerServiceShutdownFile });
            }
        } catch (Throwable th) {
            logger.log(Level.WARNING, "ejb.timer_service_shutdown_unknown",
                       new Object[] { "" });
            logger.log(Level.WARNING, "", th);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ex) {
                    logger.log(Level.FINE, "Error closing timer service shutdown file", ex);
                }
            }
        }
        return timerServiceWentDownAt;
    }


    /**
     * Called from TimerBeanContainer
     */
    public void onShutdown() {
        shutdown();
        try {
            DateFormat dateFormat =  
                new SimpleDateFormat(TIMER_SERVICE_DOWNTIME_FORMAT);
            String downTimeStr = dateFormat.format(new Date());

            File timerServiceShutdownFile  = getTimerServiceShutdownFile();
            if (timerServiceShutdownFile.exists()) {
                FileWriter fw = new FileWriter(timerServiceShutdownFile);
                PrintWriter pw = new PrintWriter(fw);

                pw.println(downTimeStr);

                pw.flush();
                pw.close();
                fw.close();
                logger.log(Level.INFO, "ejb.timer_service_shutdown_msg",
                       new Object[] { downTimeStr });
            } else {
                logger.log(Level.WARNING, "ejb.timer_service_shutdown_unknown",
                       new Object[] { TIMER_SERVICE_FILE });
            } 
        } catch (Throwable th) {
            logger.log(Level.WARNING, "ejb.timer_service_shutdown_unknown",
                       new Object[] { TIMER_SERVICE_FILE });
            logger.log(Level.WARNING, "", th);
        }
    }


    /**
     * This class gets a callback on a worker thread where the actual
     * ejbTimeout invocation will be made.  
     */
    private static class TaskExpiredWork implements Runnable {
        private EJBTimerService timerService_;
        private TimerPrimaryKey timerId_;

        public TaskExpiredWork(EJBTimerService timerService, 
                               TimerPrimaryKey timerId) {
            timerService_ = timerService;
            timerId_ = timerId;
        }

        public void run() {
            // Delegate to Timer Service.
            timerService_.deliverTimeout(timerId_);
        } 

    } // TaskExpiredWork

    private static class TimerSynch implements Synchronization {

        private TimerPrimaryKey timerId_;
        private int state_;
        private Date timeout_;
        private BaseContainer container_;
        private EJBTimerService timerService_;

        public TimerSynch(TimerPrimaryKey timerId, int state, Date timeout,
                          BaseContainer container, EJBTimerService timerService) {
            timerId_   = timerId;
            state_     = state;
            timeout_   = timeout;
            container_ = container;
            timerService_ = timerService;
        }

        public void afterCompletion(int status) {
            if( logger.isLoggable(Level.FINE) ) {
                    logger.log(Level.FINE, "TimerSynch::afterCompletion. " +
                               "timer state = " + TimerState.stateToString(state_) +
                               " , " + "timer id = " +
                               timerId_ + " , JTA TX status = " +
                               txStatusToString(status) + " , " +
                               "timeout = " + timeout_);
            }

            switch(state_) {
            case TimerState.ACTIVE :
                if( status == Status.STATUS_COMMITTED ) {
                    timerService_.scheduleTask(timerId_, timeout_);
                    container_.incrementCreatedTimedObject();
                } else {
                    timerService_.expungeTimer(timerId_);
                }
                break;
            case TimerState.CANCELLED :
                if( status == Status.STATUS_ROLLEDBACK ) {
                    if( timeout_ != null ) {
                        // Timer was cancelled while in the SCHEDULED state.
                        // Just schedule it again with the original timeout.
                        timerService_.scheduleTask(timerId_, timeout_);
                    } else {
                        // Timer was cancelled from within its own ejbTimeout
                        // and then rolledback.
                        timerService_.restoreTaskToDelivered(timerId_);
                    }
                } else {
                    timerService_.expungeTimer(timerId_);
                    container_.incrementRemovedTimedObject();
                }
                break;
            default :
                // do nothing if the state is not one of the above
                break;
            }
        }

        public void beforeCompletion() {}

    }

}
