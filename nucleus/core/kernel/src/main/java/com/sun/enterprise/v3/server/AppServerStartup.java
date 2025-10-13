/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 * Portions Copyright 2017-2025 Payara Foundation and/or its affiliates
 */

package com.sun.enterprise.v3.server;


import com.sun.appserv.server.util.Version;
import com.sun.enterprise.module.HK2Module;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.util.Result;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import fish.payara.internal.api.DeployPreviousApplicationsRunLevel;
import org.glassfish.api.FutureProvider;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.common.util.Constants;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.InstanceLifecycleEvent;
import org.glassfish.hk2.api.InstanceLifecycleEventType;
import org.glassfish.hk2.api.InstanceLifecycleListener;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.ChangeableRunLevelFuture;
import org.glassfish.hk2.runlevel.ErrorInformation;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.runlevel.RunLevelFuture;
import org.glassfish.hk2.runlevel.RunLevelListener;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.api.PostStartupRunLevel;
import org.glassfish.kernel.KernelLoggerInfo;
import org.glassfish.server.ServerEnvironmentImpl;
import org.jvnet.hk2.annotations.Service;


/**
 * Main class for Glassfish startup
 * This class spawns a non-daemon Thread when the start() is called.
 * Having a non-daemon thread allows us to control lifecycle of server JVM.
 * The thead is stopped when stop() is called.
 *
 * @author Jerome Dochez, sahoo@sun.com
 */
@Service
@Rank(Constants.DEFAULT_IMPLEMENTATION_RANK) // This should be the default impl if no name is specified
public class AppServerStartup implements PostConstruct, ModuleStartup {
    enum State {
        INITIAL, STARTING, STARTED, SHUTDOWN_REQUESTED, SHUTTING_DOWN, SHUT_DOWN;
    }
    
    StartupContext context;

    final static Logger logger = KernelLoggerInfo.getLogger();

    final static Level level = Level.FINE;

    @Inject
    ServerEnvironmentImpl env;

    @Inject
    ServiceLocator locator;

    @Inject
    ModulesRegistry systemRegistry;

    @Override
    @Inject
    public void setStartupContext(StartupContext context) {
        this.context = context;
    }

    @Inject
    Events events;

    @Inject
    CommonClassLoaderServiceImpl commonCLS;

    @Inject
    SystemTasks pidWriter;
    
    @Inject
    RunLevelController runLevelController;

    @Inject
    private AppInstanceListener appInstanceListener;
    
    private MasterRunLevelListener masterListener;
    
    private long platformInitTime;

    private String platform = System.getProperty("GlassFish_Platform");

    /**
     * A keep alive thread that keeps the server JVM from going down
     * as long as GlassFish kernel is up.
     */
    private Thread serverThread;
    private AtomicReference<State> state = new AtomicReference<>(State.INITIAL);
    
    private final static String THREAD_POLICY_PROPERTY = "org.glassfish.startupThreadPolicy";
    private final static String MAX_STARTUP_THREAD_PROPERTY = "org.glassfish.maxStartupThreads";

    private final static String POLICY_FULLY_THREADED = "FULLY_THREADED";
    private final static String POLICY_USE_NO_THREADS = "USE_NO_THREADS";

    /**
     * When set, SERVER_READY will fire once all `DeployPreviousApplicationsRunLevel` services
     * have initialised, and SERVER_STARTED will be fired in place of where SERVER_READY would normally.
     *
     * Enabled by default.
     *
     * This is to counter a side effect of us changing the run levels to apply a structured order to the post-boot and
     * deployment services in FISH-6588 (introduced in version 6.2022.2) to prevent a race between post-boot scripts
     * and previously deployed applications; all of these services used to run at the same `StartupRunLevel`
     * and so would've been initialised by the time the `SERVER_READY` event would've fired.
     */
    private final static String READY_AFTER_APPLICATIONS_PROPERTY = "fish.payara.ready-after-applications";

    private final static int DEFAULT_STARTUP_THREADS = 4;
    private final static String FELIX_PLATFORM = "Felix";
    private final static String STATIC_PLATFORM = "Static";
    
    @Override
    public void postConstruct() {
        masterListener = new MasterRunLevelListener(runLevelController);
        String threadPolicy = System.getProperty(THREAD_POLICY_PROPERTY);
        if (threadPolicy != null) {
            if (POLICY_FULLY_THREADED.equals(threadPolicy)) {
                logger.fine("Using startup thread policy FULLY_THREADED at behest of system property");
                runLevelController.setThreadingPolicy(RunLevelController.ThreadingPolicy.FULLY_THREADED);
            }
            else if (POLICY_USE_NO_THREADS.equals(threadPolicy)) {
                logger.fine("Using startup thread policy USE_NO_THREADS at behest of system property");
                runLevelController.setThreadingPolicy(RunLevelController.ThreadingPolicy.USE_NO_THREADS);
            }
            else {
                logger.warning("Unknown threading policy " + threadPolicy + ".  Will use the current policy of " +
                    runLevelController.getThreadingPolicy());
            }
        }
        else {
            if (platform == null || !(platform.equals(FELIX_PLATFORM) || platform.equals(STATIC_PLATFORM))) {
                runLevelController.setThreadingPolicy(RunLevelController.ThreadingPolicy.USE_NO_THREADS);
            }
        }
        
        int numThreads = Integer.getInteger(MAX_STARTUP_THREAD_PROPERTY, DEFAULT_STARTUP_THREADS);
        if (numThreads > 0) {
            logger.fine("Startup controller will use " + numThreads + " + threads");
            runLevelController.setMaximumUseableThreads(numThreads);
        }
        else {
            logger.fine("Startup controller will use infinite threads");
        }
        
    }

    @Override
    public synchronized void start() {
        ClassLoader origCL = Thread.currentThread().getContextClassLoader();
        try {
            // See issue #5596 to know why we set context CL as common CL.
            Thread.currentThread().setContextClassLoader(
                    commonCLS.getCommonClassLoader());
            if (state.compareAndSet(State.INITIAL, State.STARTING) || state.compareAndSet(State.SHUT_DOWN, State.STARTING)) {
                doStart();
            } else {
                throw new GlassFishException("Server cannot start, because it's in state "+state.get());
            }
        } catch (GlassFishException ex) {
            throw new RuntimeException (ex);
        } finally {
            if (!state.compareAndSet(State.STARTING, State.STARTED)) {
                // the state is no longer STARTING, therefore it has to be SHUTDOWN_REQUESTED
                stop();
            }
            // reset the context classloader. See issue GLASSFISH-15775
            Thread.currentThread().setContextClassLoader(origCL);
        }
    }

    private void doStart() throws GlassFishException {

        if (!run()){
            //startup failed
            logger.log(Level.SEVERE, "Failed to start, exiting");
            throw new GlassFishException("Server failed to start");
        }

        //if (appInstanceListener.)
        
        final CountDownLatch latch = new CountDownLatch(1);

        // spwan a non-daemon thread that waits indefinitely for shutdown request.
        // This stops the VM process from exiting.
        serverThread = new Thread("GlassFish Kernel Main Thread"){
            @Override
            public void run() {
                logger.logp(level, "AppServerStartup", "run",
                        "[{0}] started", new Object[]{this});

                // notify the other thread to continue now that a non-daemon
                // thread has started.
                latch.countDown();

                synchronized (this) {
                    while (state.get() != AppServerStartup.State.SHUT_DOWN ) {
                        try {
                            wait(); // Wait indefinitely until shutdown is requested
                        }
                        catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                
                logger.logp(level, "AppServerStartup", "run",
                        "[{0}] exiting", new Object[]{this});
            }
        };

        // by default a thread inherits daemon status of parent thread.
        // Since this method can be called by non-daemon threads (e.g.,
        // PackageAdmin service in case of an update of bundles), we
        // have to explicitly set the daemon status to false.
        serverThread.setDaemon(false);
        serverThread.start();

        // wait until we have spawned a non-daemon thread
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * @return True is startup succeeded, false if an error occurred preventing
     * the server from starting
     */
    public boolean run() {
        
        if (context==null) {
            System.err.println("Startup context not provided, cannot continue");
            return false;
        }

        if (platform==null) {
            platform = "Embedded";
        }

        platformInitTime = System.currentTimeMillis();

        if (logger.isLoggable(level)) {
            logger.log(level, "Startup class : {0}", getClass().getName());
        }

        // prepare the global variables
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();

        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(logger));
        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(masterListener));

        config.addUnbindFilter(BuilderHelper.createContractFilter(ProcessEnvironment.class.getName()));

        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(
                env.isEmbedded() ?
                new ProcessEnvironment(ProcessEnvironment.ProcessType.Embedded):
                new ProcessEnvironment(ProcessEnvironment.ProcessType.Server)));
        config.commit();
        
        

        // activate the run level services
        masterListener.reset();
        
        long initFinishTime = 0L;
        long startupFinishTime = 0L;
        
        if (!proceedTo(InitRunLevel.VAL)) {
            appInstanceListener.stopRecordingTimes();
            return false;
        }
        
        if (!logger.isLoggable(level)) {
            // Stop recording the times, no-one cares
            appInstanceListener.stopRecordingTimes();
        }
        else {
            initFinishTime = System.currentTimeMillis();
            logger.log(level, "Init level done in " +
                (initFinishTime - context.getCreationTime()) + " ms");
        }
        events.send(new Event(EventTypes.POST_SERVER_INIT), false);
        
        appInstanceListener.startRecordingFutures();
        if (!proceedTo(StartupRunLevel.VAL)) {
            appInstanceListener.stopRecordingTimes();
            return false;
        }

        boolean readyAfterApplications = Boolean.parseBoolean(System.getProperty(READY_AFTER_APPLICATIONS_PROPERTY, "true"));
        if (!postStartupJob(readyAfterApplications)) {
            appInstanceListener.stopRecordingTimes();
            return false;
        }
        
        if (logger.isLoggable(level)) {
            
            startupFinishTime = System.currentTimeMillis();
            logger.log(level, "Startup level done in " +
                (startupFinishTime - initFinishTime) + " ms");
        }

        if (readyAfterApplications) {
            if (!proceedTo(DeployPreviousApplicationsRunLevel.VAL)) {
                appInstanceListener.stopRecordingTimes();
                return false;
            }
            events.send(new Event(EventTypes.SERVER_READY), false);
        }
        
        if (!proceedTo(PostStartupRunLevel.VAL)) {
            appInstanceListener.stopRecordingTimes();
            return false;
        }
        
        if (logger.isLoggable(level)) {
            
            long postStartupFinishTime = System.currentTimeMillis();
            logger.log(level, "PostStartup level done in " +
                (postStartupFinishTime - startupFinishTime) + " ms");
        }
        return true;
    }

    /**
     * 
     * @return True if started successfully, false otherwise
     */
    private boolean postStartupJob(boolean readyAfterApplications) {
        LinkedList<Future<Result<Thread>>> futures = appInstanceListener.getFutures();

        env.setStatus(ServerEnvironment.Status.starting);
        events.send(new Event(EventTypes.SERVER_STARTUP), false);

        // finally let's calculate our starting times
        long nowTime = System.currentTimeMillis();
        logger.log(Level.INFO, KernelLoggerInfo.startupEndMessage,
                new Object[] { Version.getVersion(), Version.getBuildVersion(), platform,
                (platformInitTime - context.getCreationTime()),
                (nowTime - platformInitTime),
                nowTime - context.getCreationTime()});

        printModuleStatus(systemRegistry, level);

        String wallClockStart = System.getProperty("WALL_CLOCK_START");
        if (wallClockStart != null) {
            try {
                // it will only be set when called from AsadminMain and the env. variable AS_DEBUG is set to true
                long realstart = Long.parseLong(wallClockStart);
                logger.log(Level.INFO, KernelLoggerInfo.startupTotalTime, (System.currentTimeMillis() - realstart));
            }
            catch(Exception e) {
                // do nothing.
            }
        }

        for (Future<Result<Thread>> future : futures) {
            try {
                try {
                    // wait for an eventual status, otherwise ignore
                    if (future.get(3, TimeUnit.SECONDS).isFailure()) {
                        final Throwable t = future.get().exception();
                        logger.log(Level.SEVERE, KernelLoggerInfo.startupFatalException, t);
                        shutdown();
                        return false;
                    }
                } catch(TimeoutException e) {
                    logger.log(Level.WARNING, KernelLoggerInfo.startupWaitTimeout, e);
                }
            } catch(Throwable t) {
                logger.log(Level.SEVERE, KernelLoggerInfo.startupException, t);
            }
        }

        env.setStatus(ServerEnvironment.Status.started);
        if (readyAfterApplications) {
            events.send(new Event(EventTypes.SERVER_STARTED), false);
        } else {
            events.send(new Event(EventTypes.SERVER_READY), false);
        }
        pidWriter.writePidFile();
        
        return true;
    }

    public static void printModuleStatus(ModulesRegistry registry, Level level) {
    	
        if (!logger.isLoggable(level) || registry == null) {
            return;
        }
        
        StringBuilder sb = new StringBuilder("Module Status Report Begins\n");
        // first started :

        for (HK2Module m : registry.getModules()) {
            if (m.getState()== ModuleState.READY) {
                sb.append(m).append("\n");
            }
        }
        sb.append("\n");
        // then resolved
        for (HK2Module m : registry.getModules()) {
            if (m.getState()== ModuleState.RESOLVED) {
                sb.append(m).append("\n");
            }
        }
        sb.append("\n");
        // finally installed
        for (HK2Module m : registry.getModules()) {
            if (m.getState()!= ModuleState.READY && m.getState()!=ModuleState.RESOLVED) {
                sb.append(m).append("\n");
            }
        }
        sb.append("Module Status Report Ends");
        logger.log(level, sb.toString());
    }

    private void shutdown() {
        if (    state.compareAndSet(State.STARTING, State.SHUTDOWN_REQUESTED) ||
                state.compareAndSet(State.INITIAL, State.SHUT_DOWN)) {
            // SHUTDOWN_REQUESTED is handled at end of START
            // INITIAL --> SHUT_DOWN is trivial case of shutdown before we started
            return;
        }
        // state doesn't change on SHUT_DOWN, SHUTTING_DOWN and SHUTDOWN_REQUESTED
        if (state.compareAndSet(State.STARTED, State.SHUTDOWN_REQUESTED)) {
            // directly stop only if server already completed startup sequence
            stop();
        }
    }

    @Override
    public synchronized void stop() {
        state.set(State.SHUTTING_DOWN);
        if(env.getStatus() == ServerEnvironment.Status.stopped) {
            // During shutdown because of shutdown hooks, we can be stopped multiple times.
            // In such a case, ignore any subsequent stop operations.
            logger.fine("Already stopped, so just returning");
            return;
        }
        env.setStatus(ServerEnvironment.Status.stopping);
        try {
            events.send(new Event(EventTypes.PREPARE_SHUTDOWN), false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.exceptionDuringShutdown, e);
        }

        // deactivate the run level services
        try {
            proceedTo(InitRunLevel.VAL);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.exceptionDuringShutdown, e);
        }

        // first send the shutdown event synchronously
        env.setStatus(ServerEnvironment.Status.stopped);
        try {
            events.send(new Event(EventTypes.SERVER_SHUTDOWN), false);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.exceptionDuringShutdown, e);
        }

        try {
            runLevelController.proceedTo(0);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.exceptionDuringShutdown, e);
        }

        logger.info(KernelLoggerInfo.shutdownFinished);

        state.set(State.SHUT_DOWN);
        // notify the server thread that we are done, so that it can come out.
        if (serverThread!=null) {
            synchronized (serverThread) {
                serverThread.notify();
            }
            try {
                serverThread.join(0);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Proceed to the given run level.
     *
     * @param runLevel   the run level to proceed to
     * @return false if an error occurred that required server shutdown; true otherwise
     */
    private boolean proceedTo(int runLevel) {
        
        try {
            runLevelController.proceedTo(runLevel);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.shutdownRequired, e);
            shutdown();
            return false;
        }
        
        return !masterListener.isForcedShutdown();
    }
    
    @Service
    public static class AppInstanceListener implements InstanceLifecycleListener {
        private static final Filter FILTER = new Filter() {

            @Override
            public boolean matches(Descriptor d) {
                if (d.getScope() != null && d.getScope().equals(RunLevel.class.getName())) return true;
                
                return false;
            }
            
        };
        
        @Inject
        private Provider<RunLevelController> controllerProvider;
        
        private volatile RunLevelController controller;
        
        private Map<String, Long> startTimes = new HashMap<String, Long>();
        private LinkedHashMap<String, Long> recordedTimes = new LinkedHashMap<String, Long>();
        private LinkedList<Future<Result<Thread>>> futures = null;

        @Override
        public Filter getFilter() {
            return FILTER;
        }

        @Override
        public void lifecycleEvent(InstanceLifecycleEvent lifecycleEvent) {
            if (InstanceLifecycleEventType.PRE_PRODUCTION.equals(lifecycleEvent.getEventType())) {
                doPreProduction(lifecycleEvent.getActiveDescriptor());
                return;
            }
            
            if (InstanceLifecycleEventType.POST_PRODUCTION.equals(lifecycleEvent.getEventType())) {
                doPostProduction(lifecycleEvent);
                return;
            }
            
            if (InstanceLifecycleEventType.PRE_DESTRUCTION.equals(lifecycleEvent.getEventType())) {
                doPreDestruction(lifecycleEvent.getActiveDescriptor());
                return;
            }
            
            // All other events ignored
        }
        
        private RunLevelController getController() {
            if (controller != null) return controller;
            
            synchronized (this) {
                if (controller != null) return controller;
                
                controller = controllerProvider.get();
                return controller;
            }
        }
        
        private void stopRecordingTimes() {
            startTimes = null;
            recordedTimes = null;
        }
        
        private void startRecordingFutures() {
            futures = new LinkedList<Future<Result<Thread>>>();
        }
        
        private LinkedList<Future<Result<Thread>>> getFutures() {
            LinkedList<Future<Result<Thread>>> retVal = futures;
            futures = null;
            return retVal;
        }
        
        private void doPreProduction(ActiveDescriptor<?> descriptor) {
            if (startTimes != null) {
                startTimes.put(descriptor.getImplementation(), System.currentTimeMillis());
            }
            
            if ((getController().getCurrentRunLevel() > InitRunLevel.VAL) && logger.isLoggable(level)) {
                logger.log(level, "Running service " + descriptor.getImplementation());
            }
        }
        
        @SuppressWarnings("unchecked")
        private void doPostProduction(InstanceLifecycleEvent event) {
            ActiveDescriptor<?> descriptor = event.getActiveDescriptor();
            
            if (startTimes != null && recordedTimes != null) {
            
                Long startupTime = startTimes.remove(descriptor.getImplementation());
                if (startupTime == null) return;
            
                recordedTimes.put(descriptor.getImplementation(),
                    (System.currentTimeMillis() - startupTime));
            }
            
            if ((getController().getCurrentRunLevel() > InitRunLevel.VAL) && logger.isLoggable(level)) {
                logger.log(level, "Service " + descriptor.getImplementation() + " finished " +
                    event.getLifecycleObject());
            }
            
            if (futures != null) {
                Object startup = event.getLifecycleObject();
                if (startup instanceof FutureProvider) {
                    FutureProvider<Result<Thread>> futureProvider = (FutureProvider<Result<Thread>>) startup;
                
                    futures.addAll(futureProvider.getFutures());
                }
            }
        }
        
        private void doPreDestruction(ActiveDescriptor<?> descriptor) {
            if (logger.isLoggable(level)) {
                logger.log(level, "Releasing service {0}", descriptor.getImplementation());
            }
        }
        
        private LinkedHashMap<String, Long> getAllRecordedTimes() {
            LinkedHashMap<String, Long> retVal = recordedTimes;
            
            stopRecordingTimes();  // Do not hold onto data that will never be needed again
            
            return retVal;
        }
        
    }
    
    @Singleton
    private class MasterRunLevelListener implements RunLevelListener {
        private final RunLevelController controller;
        
        private volatile boolean forcedShutdown = false;
        
        private MasterRunLevelListener(RunLevelController controller) {
            this.controller = controller;
        }
        
        private void reset() {
            forcedShutdown = false;
        }
        
        private boolean isForcedShutdown() { return forcedShutdown; }

        @Override
        public void onCancelled(RunLevelFuture future,
                int previousProceedTo) {
            logger.log(Level.INFO, KernelLoggerInfo.shutdownRequested);
            
            if (future.isDown()) return;
            
            forcedShutdown = true;
            shutdown();
        }

        @Override
        public void onError(RunLevelFuture future, ErrorInformation info) {
            if (future.isDown()) {
                // TODO: Need a log message
                logger.log(Level.WARNING, "An error occured when the system was coming down", info.getError());
                return;
            }
            
            logger.log(Level.INFO, KernelLoggerInfo.shutdownRequested, info.getError());
            
            if (controller.getCurrentRunLevel() >= InitRunLevel.VAL) {
                logger.log(Level.SEVERE, KernelLoggerInfo.startupFailure, info.getError());
            }
            
            forcedShutdown = true;
            shutdown();
        }

        @Override
        public void onProgress(ChangeableRunLevelFuture future, int achievedLevel) {
            if (achievedLevel == PostStartupRunLevel.VAL) {
                if (logger.isLoggable(level)) {
                    printModuleStatus(systemRegistry, level);
                    
                    LinkedHashMap<String, Long> recordedTimes = appInstanceListener.getAllRecordedTimes();
                    
                    int lcv = 0;
                    if (recordedTimes != null) {
                        for (Map.Entry<String, Long> service : recordedTimes.entrySet()) {
                            logger.log(level, "Service(" + lcv++ +") : " + service.getKey() + " took " + service.getValue() + " ms");
                        }
                    }
                }
            }
        }
        
    }
}
