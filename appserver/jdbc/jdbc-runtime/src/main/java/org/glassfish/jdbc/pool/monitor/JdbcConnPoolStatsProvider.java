/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbc.pool.monitor;

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.pool.PoolLifeCycleListenerRegistry;
import com.sun.enterprise.resource.pool.PoolStatus;
import org.glassfish.external.probe.provider.annotations.ProbeListener;
import org.glassfish.external.probe.provider.annotations.ProbeParam;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.RangeStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * StatsProvider object for Jdbc pool monitoring.
 * 
 * Implements various events related to jdbc pool monitoring and provides 
 * objects to the calling modules that retrieve monitoring information.
 * 
 * @author Shalini M
 */
@AMXMetadata(type="jdbc-connection-pool-mon", group="monitoring")
@ManagedObject
@Description("JDBC Statistics")
public class JdbcConnPoolStatsProvider {

    private PoolInfo poolInfo;
    private Logger logger;

    //Registry that stores all listeners to this object
    private PoolLifeCycleListenerRegistry poolRegistry;

    
    //Objects that are exposed by this telemetry
    private CountStatisticImpl numConnFailedValidation = new CountStatisticImpl(
            "NumConnFailedValidation", StatisticImpl.UNIT_COUNT,
            "The total number of connections in the connection pool that failed " +
            "validation from the start time until the last sample time.");
    private CountStatisticImpl numConnTimedOut = new CountStatisticImpl(
            "NumConnTimedOut", StatisticImpl.UNIT_COUNT, "The total number of " +
            "connections in the pool that timed out between the start time and the last sample time.");
    private RangeStatisticImpl numConnFree = new RangeStatisticImpl(
            0, 0, 0,
            "NumConnFree", StatisticImpl.UNIT_COUNT, "The total number of free " +
            "connections in the pool as of the last sampling.",
            System.currentTimeMillis(), System.currentTimeMillis());
    private RangeStatisticImpl numConnUsed = new RangeStatisticImpl(
            0, 0, 0,
            "NumConnUsed", StatisticImpl.UNIT_COUNT, "Provides connection usage " +
            "statistics. The total number of connections that are currently being " +
            "used, as well as information about the maximum number of connections " +
            "that were used (the high water mark).",
            System.currentTimeMillis(), System.currentTimeMillis());
    private RangeStatisticImpl connRequestWaitTime = new RangeStatisticImpl(
            0, 0, 0, 
            "ConnRequestWaitTime", StatisticImpl.UNIT_MILLISECOND, 
            "The longest and shortest wait times of connection requests. The " +
            "current value indicates the wait time of the last request that was " +
            "serviced by the pool.", 
            System.currentTimeMillis(), System.currentTimeMillis());
    private CountStatisticImpl numConnDestroyed = new CountStatisticImpl(
            "NumConnDestroyed", StatisticImpl.UNIT_COUNT, 
            "Number of physical connections that were destroyed since the last reset.");
    private CountStatisticImpl numConnAcquired = new CountStatisticImpl(
            "NumConnAcquired", StatisticImpl.UNIT_COUNT, "Number of logical " +
            "connections acquired from the pool.");
    private CountStatisticImpl numConnReleased = new CountStatisticImpl(
            "NumConnReleased", StatisticImpl.UNIT_COUNT, "Number of logical " +
            "connections released to the pool.");
    private CountStatisticImpl numConnCreated = new CountStatisticImpl(
            "NumConnCreated", StatisticImpl.UNIT_COUNT, 
            "The number of physical connections that were created since the last reset.");
    private CountStatisticImpl numPotentialConnLeak = new CountStatisticImpl(
            "NumPotentialConnLeak", StatisticImpl.UNIT_COUNT, 
            "Number of potential connection leaks");
    private CountStatisticImpl numConnSuccessfullyMatched = new CountStatisticImpl(
            "NumConnSuccessfullyMatched", StatisticImpl.UNIT_COUNT,
            "Number of connections succesfully matched");
    private CountStatisticImpl numConnNotSuccessfullyMatched = new CountStatisticImpl(
            "NumConnNotSuccessfullyMatched", StatisticImpl.UNIT_COUNT,
            "Number of connections rejected during matching");
    private CountStatisticImpl totalConnRequestWaitTime = new CountStatisticImpl(
            "TotalConnRequestWaitTime", StatisticImpl.UNIT_MILLISECOND,
            "Total wait time per successful connection request");
    private CountStatisticImpl averageConnWaitTime = new CountStatisticImpl(
            "AverageConnWaitTime", StatisticImpl.UNIT_MILLISECOND,
            "Average wait-time-duration per successful connection request");
    private CountStatisticImpl waitQueueLength = new CountStatisticImpl(
            "WaitQueueLength", StatisticImpl.UNIT_COUNT, 
            "Number of connection requests in the queue waiting to be serviced.");
    private static final String JDBC_PROBE_LISTENER = "glassfish:jdbc:connection-pool:";


    public JdbcConnPoolStatsProvider(PoolInfo poolInfo, Logger logger) {
        this.poolInfo = poolInfo;
        this.logger = logger;
    }
    
    /**
     * Whenever connection leak happens, increment numPotentialConnLeak
     * @param pool JdbcConnectionPool that got a connLeakEvent
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "potentialConnLeakEvent")
    public void potentialConnLeakEvent(@ProbeParam("poolName") String poolName,
                                       @ProbeParam("appName") String appName,
                                       @ProbeParam("moduleName") String moduleName
                                       ) {
	// handle the conn leak probe event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection Leak event received - poolName = " + 
                             poolName);
            }
            //TODO V3: Checking if this is a valid event
            //Increment counter
            numPotentialConnLeak.increment();
        }
    }

    /**
     * Whenever connection timed-out event occurs, increment numConnTimedOut
     * @param pool JdbcConnectionPool that got a connTimedOutEvent
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionTimedOutEvent")
    public void connectionTimedOutEvent(@ProbeParam("poolName") String poolName,
                                        @ProbeParam("appName") String appName,
                                        @ProbeParam("moduleName") String moduleName
                                        ) {
	// handle the conn timed out probe event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connection Timed-out event received - poolName = " + 
                             poolName);
            }
            //Increment counter
            numConnTimedOut.increment();
        }        
    }
    
    /**
     * Decrement numconnfree event
     * @param poolName
     * @param steadyPoolSize
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "decrementNumConnFreeEvent")
    public void decrementNumConnFreeEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {
	// handle the num conn free decrement event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Decrement Num Connections Free event received - poolName = " + 
                             poolName);
            }
            //Decrement counter
            synchronized(numConnFree) {
                numConnFree.setCurrent(numConnFree.getCurrent() - 1);
            }
        }
    }
    
    /**
     * Increment numconnfree event
     * @param poolName
     * @param steadyPoolSize
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "incrementNumConnFreeEvent")
    public void incrementNumConnFreeEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("beingDestroyed") boolean beingDestroyed,
            @ProbeParam("steadyPoolSize") int steadyPoolSize) {
	// handle the num conn free increment event

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Increment Num Connections Free event received - poolName = " + 
                             poolName);
            }
            if(beingDestroyed) {
                //if pruned by resizer thread
                synchronized(numConnFree) {
                    synchronized(numConnUsed) {
                        if(numConnFree.getCurrent() + numConnUsed.getCurrent() < steadyPoolSize) {
                            numConnFree.setCurrent(numConnFree.getCurrent() + 1);    
                        }
                    }
                }
            } else {
                synchronized(numConnFree) {
                    numConnFree.setCurrent(numConnFree.getCurrent() + 1);
                }
            }            
        }
    }

    /**
     * Decrement numConnUsed event
     * @param poolName
     * @param beingDestroyed if the connection is destroyed due to error
     * @param steadyPoolSize
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "decrementConnectionUsedEvent")
    public void decrementConnectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {
	// handle the num conn used decrement event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Decrement Num Connections Used event received - poolName = " + 
                             poolName);
            }
            //Decrement numConnUsed counter
            synchronized(numConnUsed) {
                numConnUsed.setCurrent(numConnUsed.getCurrent() - 1);
            }
        }
    }
    
    /**
     * Connections freed event
     * @param poolName 
     * @param count number of connections freed to the pool
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionsFreedEvent")
    public void connectionsFreedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("count") int count) {
	// handle the connections freed event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connections Freed event received - poolName = " + 
                             poolName);
                logger.finest("numConnUsed =" + numConnUsed.getCurrent() + 
                    " numConnFree=" + numConnFree.getCurrent() + 
                    " Number of connections freed =" + count);
            }
            //set numConnFree to the count value
            synchronized(numConnFree) {
                numConnFree.setCurrent(count);
            }
        }
    }

    /**
     * Connection used event
     * @param poolName
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionUsedEvent")
    public void connectionUsedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {
	// handle the connection used event
        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connection Used event received - poolName = " + 
                             poolName);
            }
            //increment numConnUsed
            synchronized(numConnUsed) {
                numConnUsed.setCurrent(numConnUsed.getCurrent() + 1);
            }
        }
    }

    /**
     * Whenever connection leak happens, increment numConnFailedValidation
     * @param pool JdbcConnectionPool that got a failed validation event
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionValidationFailedEvent")
    public void connectionValidationFailedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("increment") int increment) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connection Validation Failed event received - " +
                    "poolName = " + poolName);
            }
            //TODO V3 : add support in CounterImpl for addAndGet(increment)
            numConnFailedValidation.increment(increment);
        }
        
    }
    
    /**
     * Event that a connection request is served in timeTakenInMillis.
     * 
     * @param poolName
     * @param timeTakenInMillis
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionRequestServedEvent")
    public void connectionRequestServedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName,
            @ProbeParam("timeTakenInMillis") long timeTakenInMillis) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connection request served event received - " +
                    "poolName = " + poolName);
            }
            connRequestWaitTime.setCurrent(timeTakenInMillis);
            totalConnRequestWaitTime.increment(timeTakenInMillis);
        }        
    }  
    
    /**
     * When connection destroyed event is got increment numConnDestroyed.
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionDestroyedEvent")
    public void connectionDestroyedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection destroyed event received - " +
                    "poolName = " + poolName);
            }
            numConnDestroyed.increment();
        }                
    }
    
    /**
     * When a connection is acquired increment counter
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionAcquiredEvent")
    public void connectionAcquiredEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection acquired event received - " +
                    "poolName = " + poolName);
            }
            numConnAcquired.increment();
        }                        
    }

    /**
     * When a connection is released increment counter
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionReleasedEvent")
    public void connectionReleasedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection released event received - " +
                    "poolName = " + poolName);
            }
            numConnReleased.increment();
        }                                
    }

    /**
     * When a connection is created increment counter
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionCreatedEvent")
    public void connectionCreatedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection created event received - " +
                    "poolName = " + poolName);
            }
            numConnCreated.increment();
        }                                        
    }

    /**
     * Reset pool statistics.
     * When annotated with @Reset, this method is invoked whenever monitoring
     * is turned to HIGH from OFF, thereby setting the statistics to 
     * appropriate values.
     */
    @Reset
    public void reset() {
        if(logger.isLoggable(Level.FINEST)) {        
            logger.finest("Reset event received - poolInfo = " + poolInfo);
        }
        PoolStatus status = ConnectorRuntime.getRuntime().getPoolManager().getPoolStatus(poolInfo);
        numConnUsed.setCurrent(status.getNumConnUsed());
        numConnFree.setCurrent(status.getNumConnFree());
        numConnCreated.reset();
        numConnDestroyed.reset();
        numConnFailedValidation.reset();
        numConnTimedOut.reset();
        numConnAcquired.reset();
        numConnReleased.reset();
        connRequestWaitTime.reset();
        numConnSuccessfullyMatched.reset();
        numConnNotSuccessfullyMatched.reset();
        numPotentialConnLeak.reset();
        averageConnWaitTime.reset();
        totalConnRequestWaitTime.reset();
        waitQueueLength.reset();        
    }
    
    /**
     * When connection under test matches the current request ,
     * increment numConnSuccessfullyMatched.
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionMatchedEvent")
    public void connectionMatchedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {
                logger.finest("Connection matched event received - " +
                    "poolName = " + poolName);
            }
            numConnSuccessfullyMatched.increment();
        }                
    }

    /**
     * When a connection under test does not match the current request,
     * increment numConnNotSuccessfullyMatched.
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionNotMatchedEvent")
    public void connectionNotMatchedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Connection not matched event received - " +
                    "poolName = " + poolName);
            }
            numConnNotSuccessfullyMatched.increment();
        }                
    }
    
    /**
     * When an object is added to wait queue, increment the waitQueueLength.
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionRequestQueuedEvent")
    public void connectionRequestQueuedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Wait Queue length modified event received - " +
                    "poolName = " + poolName);
            }
            waitQueueLength.increment();
        }                        
    }
    
    /**
     * When an object is removed from the wait queue, decrement the waitQueueLength.
     */
    @ProbeListener(JDBC_PROBE_LISTENER + "connectionRequestDequeuedEvent")
    public void connectionRequestDequeuedEvent(
            @ProbeParam("poolName") String poolName,
            @ProbeParam("appName") String appName,
            @ProbeParam("moduleName") String moduleName
            ) {

        PoolInfo poolInfo = new PoolInfo(poolName, appName, moduleName);
        if(this.poolInfo.equals(poolInfo)) {
            if(logger.isLoggable(Level.FINEST)) {            
                logger.finest("Wait Queue length modified event received - " +
                    "poolName = " + poolName);
            }
            waitQueueLength.decrement();
        }                        
    }

    public PoolInfo getPoolInfo() {
        return poolInfo;
    }

    public void setPoolRegistry(PoolLifeCycleListenerRegistry poolRegistry) {
        this.poolRegistry = poolRegistry;
    }

    public PoolLifeCycleListenerRegistry getPoolRegistry() {
        return poolRegistry;
    }
    
    /**
     * When a connection leak is observed, the monitoring statistics are displayed
     * to the server.log. This method helps in segregating the statistics based
     * on LOW/HIGH monitoring levels and displaying them.
     * 
     * @param poolName
     * @param stackTrace
     */
    //TODO V3 need this?
    /*@ProbeListener("glassfish:connector:jdbc-connection-pool:toString")
    public void toString(@ProbeParam("poolName") String poolName,
            @ProbeParam("stackTrace") StringBuffer stackTrace) {
        logger.finest("toString(poolName) event received. " +
                "poolName = " + poolName);
        if((poolName != null) && (poolName.equals(this.jdbcPoolName))) {
            //If level is not OFF then print the stack trace.
            if(jdbcPoolStatsProviderBootstrap.getEnabledValue(monitoringLevel)) {
                if("LOW".equals(monitoringLevel)) {
                    lowLevelLog(stackTrace);
                } else if("HIGH".equals(monitoringLevel)) {
                    highLevelLog(stackTrace);                    
                }
            }
        }    
    }*/
    
/*    private void lowLevelLog(StringBuffer stackTrace) {
        stackTrace.append("\n curNumConnUsed = " + numConnUsed.getCurrent());
        stackTrace.append("\n curNumConnFree = " + numConnFree.getCurrent());
        stackTrace.append("\n numConnCreated = " + numConnCreated.getCount());
        stackTrace.append("\n numConnDestroyed = " + numConnDestroyed.getCount());        
    }
    
    private void highLevelLog(StringBuffer stackTrace) {
        lowLevelLog(stackTrace);
        stackTrace.append("\n numConnFailedValidation = " + numConnFailedValidation.getCount());
        stackTrace.append("\n numConnTimedOut = " + numConnTimedOut.getCount());

        stackTrace.append("\n numConnAcquired = " + numConnAcquired.getCount());
        stackTrace.append("\n numConnReleased = " + numConnReleased.getCount());

        //TODO V3 : enabling other counters.
        stackTrace.append("\n currConnectionRequestWait = " + currConnectionRequestWait);
        stackTrace.append("\n minConnectionRequestWait = " + minConnectionRequestWait);
        stackTrace.append("\n maxConnectionRequestWait = " + maxConnectionRequestWait);
        stackTrace.append("\n totalConnectionRequestWait = " + totalConnectionRequestWait);

        stackTrace.append("\n numConnSuccessfullyMatched = " + this.numConnSuccessfullyMatched);
        stackTrace.append("\n numConnNotSuccessfullyMatched = " + numConnNotSuccessfullyMatched);
        stackTrace.append("\n numPotentialConnLeak = " + numPotentialConnLeak.getCount());
    }
*/
    @ManagedAttribute(id="numpotentialconnleak")
    public CountStatistic getNumPotentialConnLeakCount() {
        return numPotentialConnLeak.getStatistic();
    }

    @ManagedAttribute(id="numconnfailedvalidation")
    public CountStatistic getNumConnFailedValidation() {
        return numConnFailedValidation.getStatistic();
    }

    @ManagedAttribute(id="numconntimedout")
    public CountStatistic getNumConnTimedOut() {
        return numConnTimedOut.getStatistic();
    }

    @ManagedAttribute(id="numconnused")
    public RangeStatistic getNumConnUsed() {
        return numConnUsed.getStatistic();
    }

    @ManagedAttribute(id="numconnfree")
    public RangeStatistic getNumConnFree() {
        return numConnFree.getStatistic();
    }

    @ManagedAttribute(id="connrequestwaittime")
    public RangeStatistic getConnRequestWaitTime() {
        return connRequestWaitTime.getStatistic();
    }

    @ManagedAttribute(id="numconndestroyed")
    public CountStatistic getNumConnDestroyed() {
        return numConnDestroyed.getStatistic();
    }

    @ManagedAttribute(id="numconnacquired")
    public CountStatistic getNumConnAcquired() {
        return numConnAcquired.getStatistic();
    }

    @ManagedAttribute(id="numconncreated")
    public CountStatistic getNumConnCreated() {
        return numConnCreated.getStatistic();
    }

    @ManagedAttribute(id="numconnreleased")
    public CountStatistic getNumConnReleased() {
        return numConnReleased.getStatistic();
    }
    
    @ManagedAttribute(id="numconnsuccessfullymatched")
    public CountStatistic getNumConnSuccessfullyMatched() {
        return numConnSuccessfullyMatched;
    }
    
    @ManagedAttribute(id="numconnnotsuccessfullymatched")
    public CountStatistic getNumConnNotSuccessfullyMatched() {
        return numConnNotSuccessfullyMatched;
    }
    
    @ManagedAttribute(id="averageconnwaittime")
    public CountStatistic getAverageConnWaitTime() {
       //Time taken by all connection requests divided by total number of 
       //connections acquired in the sampling period.
       long averageWaitTime = 0;        
       if (numConnAcquired.getCount() != 0) {
           averageWaitTime = totalConnRequestWaitTime.getCount()/ 
                   numConnAcquired.getCount();
       } else {
           averageWaitTime = 0;
       }

       averageConnWaitTime.setCount(averageWaitTime);
       return averageConnWaitTime;
    }
    
    @ManagedAttribute(id="waitqueuelength") 
    public CountStatistic getWaitQueueLength() {
        return waitQueueLength;
    }
}
