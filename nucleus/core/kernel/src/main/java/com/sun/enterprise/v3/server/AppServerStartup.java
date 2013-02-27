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

package com.sun.enterprise.v3.server;


import com.sun.appserv.server.util.Version;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.bootstrap.ModuleStartup;
import com.sun.enterprise.module.bootstrap.StartupContext;
import com.sun.enterprise.util.Result;
import com.sun.enterprise.v3.common.DoNothingActionReporter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import org.glassfish.api.FutureProvider;
import org.glassfish.api.StartupRunLevel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.event.EventListener.Event;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.common.util.Constants;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Rank;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.runlevel.Activator;
import org.glassfish.hk2.runlevel.RunLevelController;
import org.glassfish.hk2.runlevel.RunLevelListener;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.internal.api.InitRunLevel;
import org.glassfish.internal.api.InternalSystemAdministrator;
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
public class AppServerStartup implements ModuleStartup {
    
    StartupContext context;

    final static Logger logger = KernelLoggerInfo.getLogger();

    final static Level level = Level.FINE;

    @Inject
    ServerEnvironmentImpl env;

    @Inject
    ServiceLocator locator;

    @Inject
    ModulesRegistry systemRegistry;

    @Inject
    public void setStartupContext(StartupContext context) {
        this.context = context;
    }

    @Inject
    ExecutorService executor;

    @Inject
    Events events;

    @Inject
    CommonClassLoaderServiceImpl commonCLS;

    @Inject
    SystemTasks pidWriter;
    
    @Inject
    RunLevelController runLevelController;

    @Inject
    Provider<CommandRunner> commandRunnerProvider;
    
    private final MasterRunLevelListener masterListener = new MasterRunLevelListener();
    
    private long platformInitTime;

    private String platform = System.getProperty("GlassFish_Platform");

    private final Map<Class<?>, Long> servicesTiming = new HashMap<Class<?>, Long>();

    /**
     * A keep alive thread that keeps the server JVM from going down
     * as long as GlassFish kernel is up.
     */
    private Thread serverThread;
    private boolean shutdownSignal = false;

    public synchronized void start() {
        ClassLoader origCL = Thread.currentThread().getContextClassLoader();
        try {
            // See issue #5596 to know why we set context CL as common CL.
            Thread.currentThread().setContextClassLoader(
                    commonCLS.getCommonClassLoader());
            doStart();
        } finally {
            // reset the context classloader. See issue GLASSFISH-15775
            Thread.currentThread().setContextClassLoader(origCL);
        }
    }

    private void doStart() {

        run();

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
                    while (!shutdownSignal) {
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

    public void run() {
        
        if (context==null) {
            System.err.println("Startup context not provided, cannot continue");
            return;
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

        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(this));
        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(masterListener));
        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(logger));

        config.addUnbindFilter(BuilderHelper.createContractFilter(ProcessEnvironment.class.getName()));

        config.addActiveDescriptor(BuilderHelper.createConstantDescriptor(
                env.isEmbedded() ?
                new ProcessEnvironment(ProcessEnvironment.ProcessType.Embedded):
                new ProcessEnvironment(ProcessEnvironment.ProcessType.Server)));
        config.commit();

        // activate the run level services
        if (proceedTo(InitRunLevel.VAL, new InitActivator())) {
            if (proceedTo(StartupRunLevel.VAL, new StartupActivator())) {
                proceedTo(PostStartupRunLevel.VAL, new PostStartupActivator());
            }
        }
    }

    public static void printModuleStatus(ModulesRegistry registry, Level level) {
    	
        if (!logger.isLoggable(level) || registry == null) {
            return;
        }
        
        StringBuilder sb = new StringBuilder("Module Status Report Begins\n");
        // first started :

        for (Module m : registry.getModules()) {
            if (m.getState()== ModuleState.READY) {
                sb.append(m).append("\n");
            }
        }
        sb.append("\n");
        // then resolved
        for (Module m : registry.getModules()) {
            if (m.getState()== ModuleState.RESOLVED) {
                sb.append(m).append("\n");
            }
        }
        sb.append("\n");
        // finally installed
        for (Module m : registry.getModules()) {
            if (m.getState()!= ModuleState.READY && m.getState()!=ModuleState.RESOLVED) {
                sb.append(m).append("\n");
            }
        }
        sb.append("Module Status Report Ends");
        logger.log(level, sb.toString());
    }

    // TODO(Sahoo): Revisit this method after discussing with Jerome.
    private void shutdown() {
        CommandRunner runner = commandRunnerProvider.get();

        if (runner!=null) {
           final ParameterMap params = new ParameterMap();
            // By default we don't want to shutdown forcefully, as that will cause the VM to exit and that's not
            // a very good behavior for a code known to be embedded in other processes.
        final boolean noForcedShutdown =
                Boolean.parseBoolean(context.getArguments().getProperty(
                        com.sun.enterprise.glassfish.bootstrap.Constants.NO_FORCED_SHUTDOWN, "true"));
            if (noForcedShutdown) {
                params.set("force", "false");
            }
            final InternalSystemAdministrator kernelIdentity = locator.getService(InternalSystemAdministrator.class);
            if (env.isDas()) {
                runner.getCommandInvocation("stop-domain", new DoNothingActionReporter(), kernelIdentity.getSubject()).parameters(params).execute();
            } else {
                runner.getCommandInvocation("_stop-instance", new DoNothingActionReporter(), kernelIdentity.getSubject()).parameters(params).execute();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public synchronized void stop() {
        if(env.getStatus() != ServerEnvironment.Status.started) {
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
            proceedTo(InitRunLevel.VAL, new AppServerActivator());
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

        // notify the server thread that we are done, so that it can come out.
        if (serverThread!=null) {
            synchronized (serverThread) {
                shutdownSignal = true;
                
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
     * Proceed to the given run level using the given {@link AppServerActivator}.
     *
     * @param runLevel   the run level to proceed to
     * @param activator  an {@link AppServerActivator activator} used to
     *                   activate/deactivate the services
     * @return false if an error occurred that required server shutdown; true otherwise
     */
    private boolean proceedTo(int runLevel, AppServerActivator activator) {
        
        masterListener.setChild(activator);
        try {
            runLevelController.proceedTo(runLevel, activator);
        } catch (Exception e) {
            logger.log(Level.SEVERE, KernelLoggerInfo.shutdownRequired, e);
            shutdown();
            return false;
        }
        finally {
            masterListener.setChild(null);
        }
        
        return !activator.isShutdown();
    }


    // ----- AppServerActivator inner class ------------------------

    /**
     * Implementation of {@link Activator} used to activate/deactivate the application server
     * run level services.  Also implements {@link RunLevelListener} to receive notifications during
     * startup and shutdown.  If there are any problems (i.e., exceptions) during startup we set a
     * flag that the app server should shutdown.
     */
    private class AppServerActivator
            implements Activator {

        // ----- data members --------------------------------------------

        /**
         * Indicates whether or not a problem occurred that required a shutdown.
         */
        protected boolean shutdown;


        // ----- Activator -------------------------------------

        @Override
        public void activate(ActiveDescriptor<?> activeDescriptor) {
            runLevelController.getDefaultActivator().activate(activeDescriptor);
        }

        @Override
        public void deactivate(ActiveDescriptor<?> activeDescriptor) {
            if (activeDescriptor.isReified()) {
                try {
                    if (logger.isLoggable(level)) {
                        logger.log(level, "Releasing services {0}", activeDescriptor);
                    }
                    runLevelController.getDefaultActivator().deactivate(activeDescriptor);
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void awaitCompletion() throws ExecutionException, InterruptedException, TimeoutException {
        }

        @Override
        public void awaitCompletion(long timeout, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {
            awaitCompletion();
        }

        // ----- accessors -----------------------------------------------

        /**
         * Determine whether or not a problem occurred that required a shutdown.
         *
         * @return true if a problem occurred that required a shutdown; false otherwise
         */
        public boolean isShutdown() {
            return shutdown;
        }

        // ----- helper methods ------------------------------------------

        /**
         * Force a shutdown of this {@link AppServerStartup} instance.
         */
        protected void forceShutdown() {
            shutdown = true;
            shutdown();
        }
    }


    // ----- InitActivator inner class -----------------------------

    /**
     * Inhabitant activator for the init services.
     */
    private class InitActivator extends AppServerActivator {

        @Override
        public void activate(ActiveDescriptor<?> activeDescriptor) {
            long start = System.currentTimeMillis();

            super.activate(activeDescriptor);

            if (logger.isLoggable(level)) {
                long finish = System.currentTimeMillis();
                logger.log(level, activeDescriptor + " Init done in " +
                        (finish - context.getCreationTime()) + " ms");
                servicesTiming.put(activeDescriptor.getImplementationClass(), (finish - start));
            }
        }
    }


    // ----- StartupActivator inner class --------------------------

    /**
     * Inhabitant activator for the startup services.
     */
    private class StartupActivator extends AppServerActivator {

        /**
         * List of {@link Future futures} for {@link AppServerActivator#awaitCompletion}.
         */
        private ArrayList<Future<Result<Thread>>> futures = new ArrayList<Future<Result<Thread>>>();

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public void activate(ActiveDescriptor<?> activeDescriptor) {

            locator.reifyDescriptor(activeDescriptor);
            Class<?> type = activeDescriptor.getImplementationClass();

            long start = System.currentTimeMillis();
            try {
                if (logger.isLoggable(level)) {
                    logger.log(level, "Running Startup services " + type);
                }

                Object startup = locator.getServiceHandle(activeDescriptor).getService();

                if (logger.isLoggable(level)) {
                    logger.log(level, "Startup services finished" + startup);
                }
                
                // the synchronous service was started successfully,
                // let's check that it's not in fact a FutureProvider
                if (startup instanceof FutureProvider) {
                    futures.addAll(((FutureProvider) startup).getFutures());
                }
            } catch(RuntimeException e) {
                logger.log(Level.SEVERE, KernelLoggerInfo.startupFailure, e);
                events.send(new Event(EventTypes.SERVER_SHUTDOWN), false);
                forceShutdown();
                return;
            }
            if (logger.isLoggable(level)) {
                servicesTiming.put(type, (System.currentTimeMillis() - start));
            }
        }

        @Override
        public void awaitCompletion() throws ExecutionException, InterruptedException, TimeoutException {
            awaitCompletion(3, TimeUnit.SECONDS);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        public void awaitCompletion(long timeout, TimeUnit unit)
                throws ExecutionException, InterruptedException, TimeoutException {

            if (runLevelController.getCurrentRunLevel() < StartupRunLevel.VAL - 1 || isShutdown()) {
                return;
            }

            env.setStatus(ServerEnvironment.Status.starting);
            events.send(new Event(EventTypes.SERVER_STARTUP), false);

            // finally let's calculate our starting times
            logger.log(Level.INFO, KernelLoggerInfo.startupEndMessage,
                    new Object[] { Version.getVersion(), Version.getBuildVersion(), platform,
                    (platformInitTime - context.getCreationTime()),
                    (System.currentTimeMillis() - platformInitTime),
                    System.currentTimeMillis() - context.getCreationTime()});

            printModuleStatus(systemRegistry, level);

            try {
                // it will only be set when called from AsadminMain and the env. variable AS_DEBUG is set to true
                long realstart = Long.parseLong(System.getProperty("WALL_CLOCK_START"));
                logger.log(Level.INFO, KernelLoggerInfo.startupTotalTime, (System.currentTimeMillis() - realstart));
            }
            catch(Exception e) {
                // do nothing.
            }

            if (logger.isLoggable(level)) {
                for (Map.Entry<Class<?>, Long> service : servicesTiming.entrySet()) {
                    logger.log(level, "Service : " + service.getKey() + " took " + service.getValue() + " ms");
                }
            }

            for (Future<Result<Thread>> future : futures) {
                try {
                    try {
                        // wait for an eventual status, otherwise ignore
                        if (future.get(timeout, unit).isFailure()) {
                            final Throwable t = future.get().exception();
                            logger.log(Level.SEVERE, KernelLoggerInfo.startupFatalException, t);
                            events.send(new Event(EventTypes.SERVER_SHUTDOWN), false);
                            forceShutdown();
                            return;
                        }
                    } catch(TimeoutException e) {
                        logger.log(Level.WARNING, KernelLoggerInfo.startupWaitTimeout, e);
                    }
                } catch(Throwable t) {
                    logger.log(Level.SEVERE, KernelLoggerInfo.startupException, t);
                }
            }

            env.setStatus(ServerEnvironment.Status.started);
            events.send(new Event(EventTypes.SERVER_READY), false);
            pidWriter.writePidFile();
        }
    }


    // ----- PostStartupActivator inner class ----------------------

    /**
     * Inhabitant activator for the post startup services.
     */
    private class PostStartupActivator extends AppServerActivator {

        @Override
        public void awaitCompletion() throws ExecutionException, InterruptedException, TimeoutException {
            if (runLevelController.getCurrentRunLevel() < PostStartupRunLevel.VAL - 1 || isShutdown()) {
                return;
            }

            printModuleStatus(systemRegistry, level);
        }
    }
    
    @Singleton
    private static class MasterRunLevelListener implements RunLevelListener {
        private AppServerActivator child;
        
        private void setChild(AppServerActivator child) {
            this.child = child;
        }

        @Override
        public void onCancelled(RunLevelController controller,
                int previousProceedTo, boolean isInterrupt) {
            logger.log(Level.INFO, KernelLoggerInfo.shutdownRequested);
            
            if (child == null) return;
            child.forceShutdown();
        }

        @Override
        public void onError(RunLevelController controller, Throwable error,
                boolean willContinue) {
            logger.log(Level.INFO, KernelLoggerInfo.shutdownRequested, error);
            
            if (child == null) return;
            child.forceShutdown();
        }

        @Override
        public void onProgress(RunLevelController controller) {
            logger.log(level, "progress event: {0}", controller);
        }
        
    }
}
