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

package com.sun.enterprise.connectors.work;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.ConnectorConstants;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.corba.ee.spi.orbutil.threadpool.ThreadPoolManager;
import com.sun.corba.ee.spi.orbutil.threadpool.ThreadPool;
import com.sun.corba.ee.spi.orbutil.threadpool.NoSuchThreadPoolException;
import com.sun.enterprise.connectors.work.monitor.WorkManagementProbeProvider;
import com.sun.enterprise.connectors.work.monitor.WorkManagementStatsProvider;
import com.sun.enterprise.connectors.work.context.WorkContextHandler;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import javax.resource.spi.work.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.enterprise.iiop.util.S1ASThreadPoolManager;


/**
 * WorkManager implementation.
 *
 * @author Binod P.G
 */

public final class CommonWorkManager implements WorkManager {

    private static WorkManager wm = null;

    private ThreadPoolManager tpm;
    private ThreadPool tp;

    private static final Logger logger =
            LogDomains.getLogger(CommonWorkManager.class, LogDomains.RSR_LOGGER);

    private WorkManagementProbeProvider probeProvider = null;
    private WorkManagementStatsProvider statsProvider = null;
    private String dottedNamesHierarchy;

    private StringManager localStrings = StringManager.getManager(
            CommonWorkManager.class);

    private ConnectorRuntime runtime;
	private String raName ;
    private ClassLoader rarClassLoader;

    /**
     * Private constructor.
     *
     * @param threadPoolId Id of the thread pool.
     * @throws ConnectorRuntimeException if thread pool is not accessible
     */
    public CommonWorkManager(String threadPoolId, ConnectorRuntime runtime, String raName, ClassLoader cl)
            throws ConnectorRuntimeException {

        if (runtime.isServer() || runtime.isEmbedded()) {
            this.runtime = runtime;
            this.raName = raName;
            this.rarClassLoader = cl;
            tpm = S1ASThreadPoolManager.getThreadPoolManager();

            if (threadPoolId == null || threadPoolId.isEmpty()) {
                tp = tpm.getDefaultThreadPool();
            } else {
                try {
                    tp = tpm.getThreadPool(threadPoolId);
                    if(logger.isLoggable(Level.FINEST)){
                        logger.finest("Got the thread pool [ "+threadPoolId+" ] for WorkManager of RAR [ "+raName+" ]");
                    }
                } catch (NoSuchThreadPoolException e) {
                    String msg = localStrings.getString("workmanager.threadpool_not_found", new Object[]{threadPoolId});
                    logger.log(Level.SEVERE,msg);
                    ConnectorRuntimeException cre = new ConnectorRuntimeException(e.getMessage());
                    cre.initCause(e);
                    throw cre;
                }
            }
            if (tp == null) {
                // in case the default thread-pool was not available.
                // Set the message appropriately.
                if(threadPoolId == null){
                    threadPoolId = "default thread-pool of server";
                }
                String msg = localStrings.getString("workmanager.threadpool_not_found", new Object[]{threadPoolId});
                logger.log(Level.SEVERE, msg);
                throw new ConnectorRuntimeException(msg);
            }
            registerWithMonitoringService();
        }
    }

    private void registerWithMonitoringService() {

        if(ConnectorsUtil.belongsToSystemRA(raName)){
            if(!ConnectorsUtil.isJMSRA(raName)){
                return ;    
            }
        }
        probeProvider = new WorkManagementProbeProvider();
        String monitoringModuleName = ConnectorConstants.MONITORING_CONNECTOR_SERVICE_MODULE_NAME;

        if(ConnectorsUtil.isJMSRA(raName)){
            dottedNamesHierarchy = ConnectorConstants.MONITORING_JMS_SERVICE +
                ConnectorConstants.MONITORING_SEPARATOR + ConnectorConstants.MONITORING_WORK_MANAGEMENT;
            monitoringModuleName =  ConnectorConstants.MONITORING_JMS_SERVICE_MODULE_NAME;
        }else{
            dottedNamesHierarchy = ConnectorConstants.MONITORING_CONNECTOR_SERVICE +
                    ConnectorConstants.MONITORING_SEPARATOR + raName + ConnectorConstants.MONITORING_SEPARATOR +
                    ConnectorConstants.MONITORING_WORK_MANAGEMENT;

        }
        statsProvider = new WorkManagementStatsProvider(raName);

        StatsProviderManager.register(monitoringModuleName,PluginPoint.SERVER, dottedNamesHierarchy, statsProvider);

        logger.log(Level.FINE, "Registered work-monitoring stats [ "+dottedNamesHierarchy+" ]  " +
                "for [ " + raName + " ] with monitoring-stats-registry.");
    }

    private void deregisterFromMonitoringService(){

        if(ConnectorsUtil.belongsToSystemRA(raName)){
            if(!ConnectorsUtil.isJMSRA(raName)){
                return ;
            }
        }
        if (statsProvider != null) {
            StatsProviderManager.unregister(statsProvider);
            logger.log(Level.FINE, "De-registered work-monitoring stats [ "+dottedNamesHierarchy+" ]" +
                    "  for [ " + raName + " ] from monitoring-stats-registry.");
        }
    }

    public void cleanUp(){
        if (runtime != null && runtime.isServer()) {
            deregisterFromMonitoringService();
        }
    }

    /**
     * Executes the work instance.
     *
     * @param work work instance from resource adapter
     * @throws WorkException if there is an exception while executing work.
     */
    public void doWork(Work work)
            throws WorkException {
        doWork(work, -1, null, null);
    }

    /**
     * Executes the work instance. The calling thread will wait until the
     * end of work execution.
     *
     * @param work         work instance from resource adapter
     * @param startTimeout Timeout for the work.
     * @param execContext  Execution context in which the work will be executed.
     * @param workListener Listener from RA that will listen to work events.
     * @throws WorkException if there is an exception while executing work.
     */
    public void doWork(Work work, long startTimeout,
                       ExecutionContext execContext, WorkListener workListener)
            throws WorkException {
        WorkContextHandler contextHandler = createWorkContextHandler();
        validateWork(work, WorkCoordinator.getExecutionContext(execContext, work), contextHandler);

        if (logger.isLoggable(Level.FINEST)) {
            String msg = "doWork for [" + work.toString() + "] START";
            logger.log(Level.FINEST, debugMsg(msg));
        }

        WorkCoordinator wc = new WorkCoordinator
                (work, startTimeout, execContext, tp.getAnyWorkQueue(), workListener,
                        this.probeProvider, runtime, raName, contextHandler);
        wc.submitWork(WorkCoordinator.WAIT_UNTIL_FINISH);
        wc.lock();

        WorkException we = wc.getException();
        if (we != null) {
            throw we;
        }

        if (logger.isLoggable(Level.FINEST)) {
            String msg = "doWork for [" + work.toString() + "] END";
            logger.log(Level.FINEST, debugMsg(msg));
        }
    }

    /**
     * Executes the work instance. The calling thread will wait until the
     * start of work execution.
     *
     * @param work work instance from resource adapter
     * @throws WorkException if there is an exception while executing work.
     */
    public long startWork(Work work) // startTimeout = INDEFINITE
            throws WorkException {
        //block the current application thread
        //find a thread to run work
        //notify the application thread when done

        return startWork(work, -1, null, null);
    }

    /**
     * Executes the work instance. The calling thread will wait until the
     * start of work execution.
     *
     * @param work         work instance from resource adapter
     * @param startTimeout Timeout for the work.
     * @param execContext  Execution context in which the work will be executed.
     * @param workListener Listener from RA that will listen to work events.
     * @throws WorkException if there is an exception while executing work.
     */
    public long startWork(Work work, long startTimeout,
                          ExecutionContext execContext, WorkListener workListener)
            throws WorkException {

        WorkContextHandler contextHandler = createWorkContextHandler();
        validateWork(work, WorkCoordinator.getExecutionContext(execContext, work), contextHandler);

        if (logger.isLoggable(Level.FINEST)) {
            String msg = "startWork for [" + work.toString() + "] START";
            logger.log(Level.FINEST, debugMsg(msg));
        }

        long acceptanceTime = System.currentTimeMillis();

        WorkCoordinator wc = new WorkCoordinator
                (work, startTimeout, execContext, tp.getAnyWorkQueue(), workListener,
                        this.probeProvider, runtime, raName, contextHandler);
        wc.submitWork(WorkCoordinator.WAIT_UNTIL_START);
        wc.lock();

        WorkException we = wc.getException();
        if (we != null) {
            throw we;
        }

        if (logger.isLoggable(Level.FINEST)) {
            String msg = "startWork for [" + work.toString() + "] END";
            logger.log(Level.FINEST, debugMsg(msg));
        }
        long startTime = System.currentTimeMillis();

        return (startTime - acceptanceTime);
    }

    /**
     * prvides work-context-handler to handle the submitted work-contexts
     * @return work-context-handler
     */
    private WorkContextHandler createWorkContextHandler() {
        WorkContextHandler contextHandler = new WorkContextHandler(runtime, raName, rarClassLoader);
        return contextHandler;
    }

    /**
     * Executes the work instance. Calling thread will continue after scheduling
     * the work
     *
     * @param work work instance from resource adapter
     * @throws WorkException if there is an exception while executing work.
     */
    public void scheduleWork(Work work) // startTimeout = INDEFINITE
            throws WorkException {
        scheduleWork(work, -1, null, null);
        return;
    }

    /**
     * Executes the work instance. Calling thread will continue after scheduling
     * the work
     *
     * @param work         work instance from resource adapter
     * @param startTimeout Timeout for the work.
     * @param execContext  Execution context in which the work will be executed.
     * @param workListener Listener from RA that will listen to work events.
     * @throws WorkException if there is an exception while executing work.
     */
    public void scheduleWork(Work work, long startTimeout,
                             ExecutionContext execContext, WorkListener workListener)
            throws WorkException {

        WorkContextHandler contextHandler = createWorkContextHandler();
        validateWork(work, WorkCoordinator.getExecutionContext(execContext, work), contextHandler);
        if (logger.isLoggable(Level.FINEST)) {
            String msg = "scheduleWork for [" + work.toString() + "] START";
            logger.log(Level.FINEST, debugMsg(msg));
        }

        WorkCoordinator wc = new WorkCoordinator
                (work, startTimeout, execContext, tp.getAnyWorkQueue(), workListener,
                        this.probeProvider, runtime, raName, contextHandler);
        wc.submitWork(WorkCoordinator.NO_WAIT);
        wc.lock();

        WorkException we = wc.getException();
        if (we != null) {
            throw we;
        }

        if (logger.isLoggable(Level.FINEST)) {
            String msg = "scheduleWork for [" + work.toString() + "] END";
            logger.log(Level.FINEST, debugMsg(msg));
        }
        return;
    }

    /**
     * validates the work-contexts provided in the work
     * @param workToBeValidated work instance
     * @param context execution-context (if present)
     * @param contextHandler work-context-handler
     * @throws WorkCompletedException when work processing fails
     * @throws WorkRejectedException when work cannot be processed
     */
    private void validateWork(Work workToBeValidated, ExecutionContext context, WorkContextHandler contextHandler)
            throws WorkCompletedException, WorkRejectedException {
        contextHandler.validateWork(workToBeValidated, context);
    }

    private String debugMsg(String message) {
        String msg = "[Thread " + Thread.currentThread().getName()
                + "] -- " + message;
        return msg;
    }
}
