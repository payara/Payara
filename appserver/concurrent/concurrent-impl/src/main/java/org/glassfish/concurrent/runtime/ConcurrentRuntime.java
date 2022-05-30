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
// Portions Copyright [2016-2022] [Payara Foundation and/or its affiliates]

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.Utility;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.concurrent.LogFacade;
import org.glassfish.concurrent.runtime.deployer.ContextServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedExecutorServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedScheduledExecutorServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedThreadFactoryConfig;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.jvnet.hk2.annotations.Service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService;
import org.glassfish.enterprise.concurrent.AbstractManagedThread;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedScheduledExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;

/**
 * This class provides API to create various Concurrency Utilities objects
 */
@Service
@Singleton
public class ConcurrentRuntime implements PostConstruct, PreDestroy {

    private static ConcurrentRuntime _runtime;

    private Map<String, ManagedExecutorServiceImpl> managedExecutorServiceMap;
    private Map<String, ManagedScheduledExecutorServiceImpl> managedScheduledExecutorServiceMap;
    private Map<String, ContextServiceImpl> contextServiceMap = new HashMap();
    private Map<String, ManagedThreadFactoryImpl> managedThreadFactoryMap;

    public static final String CONTEXT_INFO_CLASSLOADER = "Classloader";
    public static final String CONTEXT_INFO_JNDI = "JNDI";
    public static final String CONTEXT_INFO_SECURITY = "Security";
    public static final String CONTEXT_INFO_WORKAREA = "WorkArea";

    public static final String CONTEXT_INFO_ALL = CONTEXT_INFO_CLASSLOADER + "," + CONTEXT_INFO_JNDI + "," + CONTEXT_INFO_SECURITY + "," + CONTEXT_INFO_WORKAREA;

    private ScheduledExecutorService internalScheduler;
    private static final Logger logger  = LogFacade.getLogger();

    @Inject
    InvocationManager invocationManager;

    @Inject
    Deployment deployment;

    @Inject
    Applications applications;

    @Inject
    JavaEETransactionManager transactionManager;

    @Inject
    ApplicationRegistry applicationRegistry;

    @Inject
    ComponentEnvManager compEnvMgr;

    @Inject
    private ResourceNamingService resourceNamingService;

    /**
     * Returns the ConcurrentRuntime instance.
     * It follows singleton pattern and only one instance exists at any point
     * of time. External entities need to call this method to get
     * ConcurrentRuntime instance
     *
     * @return ConcurrentRuntime instance
     */
    public static ConcurrentRuntime getRuntime() {
        if (_runtime == null) {
            throw new RuntimeException("ConcurrentRuntime not initialized");
        }
        return _runtime;
    }

    private static void setRuntime(ConcurrentRuntime runtime) {
        _runtime = runtime;
    }

    /**
     * Constructor should be private to follow singleton pattern, but package access for unit testing.
     */
    ConcurrentRuntime() {
        setRuntime(this);
    }

    InvocationManager getInvocationManager() {
        return invocationManager;
    }

    Deployment getDeployment() {
        return deployment;
    }

    Applications getApplications() {
        return applications;
    }

    JavaEETransactionManager getTransactionManager() {
        return transactionManager;
    }

    ApplicationRegistry getApplicationRegistry() {
        return applicationRegistry;
    }

    public ComponentEnvManager getCompEnvMgr() {
        return compEnvMgr;
    }

    public synchronized ContextServiceImpl getContextService(ResourceInfo resource, ContextServiceConfig config) {
        String jndiName = config.getJndiName();
        if (contextServiceMap != null && contextServiceMap.containsKey(jndiName)) {
            return contextServiceMap.get(jndiName);
        }
        ContextServiceImpl contextService = createContextService(resource, config);
        if (contextServiceMap == null) {
            contextServiceMap = new HashMap();
        }
        contextServiceMap.put(jndiName, contextService);
        return contextService;
    }

    public synchronized ContextServiceImpl createContextService(ResourceInfo resource, ContextServiceConfig config) {
        ContextServiceImpl contextService = createContextServiceImpl(config.getJndiName(),
                config.isContextInfoEnabledBoolean(), config.getPropagatedContexts(),
                config.getClearedContexts(), config.getUchangedContexts());
        return contextService;
    }

    public void shutdownContextService(String jndiName) {
        synchronized(this) {
            if (contextServiceMap != null) {
                contextServiceMap.remove(jndiName);
            }
        }
    }

    public synchronized ManagedExecutorServiceImpl getManagedExecutorService(ResourceInfo resourceInfo, ManagedExecutorServiceConfig config) {
        String jndiName = config.getJndiName();

        if (managedExecutorServiceMap != null && managedExecutorServiceMap.containsKey(jndiName)) {
            return managedExecutorServiceMap.get(jndiName);
        }

        ContextServiceImpl contextService = prepareContextService(
                createContextServiceName(config.getContext(), config.getJndiName()),
                config.getContextInfo(),
                config.isContextInfoEnabledBoolean(), true);

        ManagedExecutorServiceImpl mes = createManagedExecutorService(resourceInfo, config, contextService);
        if (managedExecutorServiceMap == null) {
            managedExecutorServiceMap = new HashMap();
        }

        managedExecutorServiceMap.put(jndiName, mes);
        return mes;
    }

    public synchronized ManagedExecutorServiceImpl createManagedExecutorService(ResourceInfo resourceInfo, ManagedExecutorServiceConfig config, ContextServiceImpl contextService) {
        ManagedThreadFactoryImpl managedThreadFactory = new ThreadFactoryWrapper(
                config.getJndiName() + "-managedThreadFactory",
                null,
                config.getThreadPriority());
        ManagedExecutorServiceImpl mes = new ManagedExecutorServiceImpl(config.getJndiName(),
                managedThreadFactory,
                config.getHungAfterSeconds() * 1_000L, // in milliseconds
                config.isLongRunningTasks(),
                config.getCorePoolSize(),
                config.getMaximumPoolSize(),
                config.getKeepAliveSeconds(), TimeUnit.SECONDS,
                config.getThreadLifeTimeSeconds(),
                config.getTaskQueueCapacity(), 
                contextService,
                AbstractManagedExecutorService.RejectPolicy.ABORT);

        if (config.getHungAfterSeconds() > 0L && !config.isLongRunningTasks()) {
            scheduleInternalTimer();
        }

        return mes;
    }

    public void shutdownManagedExecutorService(String jndiName) {
        ManagedExecutorServiceImpl mes = null;
        synchronized(this) {
            if (managedExecutorServiceMap != null) {
                mes = managedExecutorServiceMap.remove(jndiName);
            }
        }
        if (mes != null) {
            mes.shutdownNow();
        }
    }

    public synchronized ManagedScheduledExecutorServiceImpl getManagedScheduledExecutorService(ResourceInfo resource,
                                                                                               ManagedScheduledExecutorServiceConfig config) {
        String jndiName = config.getJndiName();
        if (managedScheduledExecutorServiceMap != null && managedScheduledExecutorServiceMap.containsKey(jndiName)) {
            return managedScheduledExecutorServiceMap.get(jndiName);
        }
        ContextServiceImpl contextService = prepareContextService(createContextServiceName(config.getContext(), config.getJndiName()),
                config.getContextInfo(), config.isContextInfoEnabledBoolean(), true);

        ManagedScheduledExecutorServiceImpl mes = createManagedScheduledExecutorService(resource, config, contextService);

        if (managedScheduledExecutorServiceMap == null) {
            managedScheduledExecutorServiceMap = new HashMap();
        }
        managedScheduledExecutorServiceMap.put(jndiName, mes);
        if (config.getHungAfterSeconds() > 0L && !config.isLongRunningTasks()) {
            scheduleInternalTimer();
        }
        return mes;
    }

    public ManagedScheduledExecutorServiceImpl createManagedScheduledExecutorService(ResourceInfo resource,
            ManagedScheduledExecutorServiceConfig config, ContextServiceImpl contextService) {
        ManagedThreadFactoryImpl managedThreadFactory = new ThreadFactoryWrapper(
                config.getJndiName() + "-managedThreadFactory",
                null,
                config.getThreadPriority());
        ManagedScheduledExecutorServiceImpl mes = new ManagedScheduledExecutorServiceImpl(config.getJndiName(),
                managedThreadFactory,
                config.getHungAfterSeconds() * 1000L, // in millseconds
                config.isLongRunningTasks(),
                config.getCorePoolSize(),
                config.getKeepAliveSeconds(), TimeUnit.SECONDS,
                config.getThreadLifeTimeSeconds(),
                contextService,
                AbstractManagedExecutorService.RejectPolicy.ABORT);
        return mes;
    }

    public void shutdownScheduledManagedExecutorService(String jndiName) {
        ManagedScheduledExecutorServiceImpl mses = null;
        synchronized(this) {
            if (managedScheduledExecutorServiceMap != null) {
                mses = managedScheduledExecutorServiceMap.remove(jndiName);
            }
        }
        if (mses != null) {
            mses.shutdownNow();
        }
    }

    public synchronized ManagedThreadFactoryImpl getManagedThreadFactory(ResourceInfo resource, ManagedThreadFactoryConfig config) {
        String jndiName = config.getJndiName();
        if (managedThreadFactoryMap != null && managedThreadFactoryMap.containsKey(jndiName)) {
            return managedThreadFactoryMap.get(jndiName);
        }
        String context = config.getContext();
        ContextServiceImpl contextService = prepareContextService(createContextServiceName(context, config.getJndiName()),
                config.getContextInfo(), config.isContextInfoEnabledBoolean(), true);

        ManagedThreadFactoryImpl managedThreadFactory = createManagedThreadFactory(resource, config, contextService);

        if (managedThreadFactoryMap == null) {
            managedThreadFactoryMap = new HashMap();
        }
        managedThreadFactoryMap.put(jndiName, managedThreadFactory);
        return managedThreadFactory;
    }

    public ManagedThreadFactoryImpl createManagedThreadFactory(ResourceInfo resource, ManagedThreadFactoryConfig config, ContextServiceImpl contextService) {
        ManagedThreadFactoryImpl managedThreadFactory = new ThreadFactoryWrapper(config.getJndiName(), contextService,
                config.getThreadPriority());
        return managedThreadFactory;
    }

    public void shutdownManagedThreadFactory(String jndiName) {
        ManagedThreadFactoryImpl mtf = null;
        synchronized(this) {
            if (managedThreadFactoryMap != null) {
                mtf = managedThreadFactoryMap.remove(jndiName);
            }
        }
        if (mtf != null) {
            mtf.stop();
        }
    }

    /**
     * Load cached context service or create a new one with default setup (propagate all).
     */
    private ContextServiceImpl prepareContextService(String contextServiceJndiName, String contextInfo, boolean contextInfoEnabled, boolean cleanupTransaction) {
        ContextServiceImpl contextService = contextServiceMap.get(contextServiceJndiName);
        if (contextService == null) {
            // if the context service is not known, create it
            Set<String> propagated = ContextServiceConfig.parseContextInfo(contextInfo, contextInfoEnabled);
            Set<String> cleared = Collections.EMPTY_SET;
            if (cleanupTransaction && !propagated.contains(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA)) {
                // pass the cleanup transaction in list of cleared handlers
                cleared = Set.of(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA);
            }
            contextService = createContextServiceImpl(contextServiceJndiName, contextInfoEnabled, propagated, cleared, Collections.EMPTY_SET);
            contextServiceMap.put(contextServiceJndiName, contextService);
        }
        return contextService;
    }

    private ContextServiceImpl createContextServiceImpl(String jndiName, boolean isContextInfoEnabled, Set<String> propagated, Set<String> cleared, Set<String> unchanged) {
        ContextSetupProviderImpl contextSetupProvider
                = new ContextSetupProviderImpl(
                        invocationManager,
                        deployment,
                        compEnvMgr,
                        applicationRegistry,
                        applications,
                        transactionManager,
                        propagated,
                        cleared,
                        unchanged
                );

        ContextServiceImpl contextService
                = new ContextServiceImpl(
                        jndiName,
                        contextSetupProvider,
                        new TransactionSetupProviderImpl(transactionManager,
                                unchanged.contains(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA),
                                cleared.contains(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA))
                );
        return contextService;
    }

    private void scheduleInternalTimer() {
        if (internalScheduler == null) {
            String name = "glassfish-internal";
            ManagedThreadFactoryImpl managedThreadFactory = new ThreadFactoryWrapper(
                    name + "-managedThreadFactory",
                    null,
                    Thread.NORM_PRIORITY);
            internalScheduler = new ManagedScheduledExecutorServiceImpl(name,
                    managedThreadFactory,
                    0L,
                    false,
                    1,
                    60, TimeUnit.SECONDS,
                    0L,
                    prepareContextService(createContextServiceName(null, name),
                            CONTEXT_INFO_CLASSLOADER, true, false),
                    AbstractManagedExecutorService.RejectPolicy.ABORT);
            internalScheduler.scheduleAtFixedRate(new HungTasksLogger(), 1L, 1L, TimeUnit.MINUTES);
        }
    }

    /**
     * Decide JNDI name of context service. Either it is specified or it is created from the owning object.
     *
     * @param configuredContextJndiName JNDI name specified (typically context parameter in annotation)
     * @param parentObjectJndiName JNDI nam of ManagedExecutorService, ManagedThreadFactory etc. using the context service
     * @return JNDI name for the context service to be stored in the JNDI tree.
     */
    public static String createContextServiceName(String configuredContextJndiName, String parentObjectJndiName) {
        String contextServiceJndiName = configuredContextJndiName; 
        if (contextServiceJndiName == null) {
            contextServiceJndiName = parentObjectJndiName + "-contextservice";
        }
        return contextServiceJndiName;
    }

    public ContextServiceImpl findOrCreateContextService(String configuredContextJndiName, String parentObjectJndiName, String applicationName, String moduleName) {
        String contextOfResource = ConcurrentRuntime.createContextServiceName(
                configuredContextJndiName,
                parentObjectJndiName);
        ResourceInfo contextResourceInfo = new ResourceInfo(contextOfResource, applicationName, moduleName);
        ContextServiceImpl contextService;
        try {
            contextService = (ContextServiceImpl) resourceNamingService.lookup(contextResourceInfo, contextOfResource);
        } catch (NamingException e) {
            // not found, create a default one
            contextService = prepareContextService(contextOfResource, CONTEXT_INFO_ALL, true, true);
        }
        return contextService;
    }

    /**
     * context loader propagation to threads causes memory leaks on redeploy
     */
    private static final class ThreadFactoryWrapper extends ManagedThreadFactoryImpl {
        public ThreadFactoryWrapper(String string, ContextServiceImpl contextService, int threadPriority) {
            super(string, contextService, threadPriority);
        }

        @Override
        protected AbstractManagedThread createThread(Runnable r, ContextHandle contextHandleForSetup) {
            ClassLoader appClassLoader = Utility.getClassLoader();
            Utility.setContextClassLoader(null);
            try {
                return super.createThread(r, contextHandleForSetup);
            } finally {
                Utility.setContextClassLoader(appClassLoader);
            }
        }
    }

    @Override
    public void postConstruct() {
    }

    @Override
    public void preDestroy() {
        // TODO shut down objects here
    }

    class HungTasksLogger implements Runnable {

        public void run() {
            ArrayList<ManagedExecutorServiceImpl> executorServices = new ArrayList();
            ArrayList<ManagedScheduledExecutorServiceImpl> scheduledExecutorServices = new ArrayList();
            synchronized (ConcurrentRuntime.this) {
                if (managedExecutorServiceMap != null) {
                    Collection<ManagedExecutorServiceImpl> mesColl = managedExecutorServiceMap.values();
                    executorServices.addAll(mesColl);
                }
            }
            synchronized (ConcurrentRuntime.this) {
                if (managedScheduledExecutorServiceMap != null) {
                    Collection<ManagedScheduledExecutorServiceImpl> msesColl = managedScheduledExecutorServiceMap.values();
                    scheduledExecutorServices.addAll(msesColl);
                }
            }
            for (ManagedExecutorServiceImpl mes: executorServices) {
                Collection<AbstractManagedThread> hungThreads = mes.getHungThreads();
                logHungThreads(hungThreads, mes.getManagedThreadFactory(), mes.getName());
            }
            for (ManagedScheduledExecutorServiceImpl mses: scheduledExecutorServices) {
                Collection<AbstractManagedThread> hungThreads = mses.getHungThreads();
                logHungThreads(hungThreads, mses.getManagedThreadFactory(), mses.getName());
            }
        }

        private void logHungThreads(Collection<AbstractManagedThread> hungThreads, ManagedThreadFactoryImpl mtf, String mesName) {
            if (hungThreads != null) {
                for (AbstractManagedThread hungThread: hungThreads) {
                    Object[] params = {hungThread.getTaskIdentityName(),
                                       hungThread.getName(),
                                       hungThread.getTaskRunTime(System.currentTimeMillis()) / 1000,
                                       mtf.getHungTaskThreshold() / 1000,
                                       mesName};
                    logger.log(Level.WARNING, LogFacade.UNRESPONSIVE_TASK, params);
                }
            }
        }
    }

}
