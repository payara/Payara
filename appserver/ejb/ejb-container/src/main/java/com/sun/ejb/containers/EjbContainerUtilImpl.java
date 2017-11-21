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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import com.sun.ejb.base.io.EJBObjectInputStreamHandler;
import com.sun.ejb.base.io.EJBObjectOutputStreamHandler;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;
import org.glassfish.ejb.spi.CMPDeployer;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.flashlight.provider.ProbeProviderFactory;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.PreDestroy;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.Synchronization;

import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.config.EjbTimerService;

/**
 * @author Mahesh Kannan
 *         Date: Feb 10, 2008
 */
@Service
public class EjbContainerUtilImpl
    implements PostConstruct, PreDestroy, EjbContainerUtil {

    private static Logger _logger = LogDomains.getLogger(EjbContainerUtilImpl.class, LogDomains.EJB_LOGGER);

    private ThreadPoolExecutor defaultThreadPoolExecutor;
    
    @Inject
    private ServiceLocator services;

    @Inject
    private ServerContext serverContext;
    
    @Inject
    JavaEEIOUtils javaEEIOUtils;

    private  Map<Long, BaseContainer> id2Container
            = new ConcurrentHashMap<Long, BaseContainer>();

    private  Timer _timer;

    private  boolean _insideContainer = true;

    @Inject
    private  InvocationManager _invManager;

    @Inject
    private  InjectionManager _injectionManager;

    @Inject
    private  GlassfishNamingManager _gfNamingManager;

    @Inject
    private  ComponentEnvManager _compEnvManager;

    @Inject
    private JavaEETransactionManager txMgr;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;

    private EjbContainer ejbContainer;

    @Inject
    private GlassFishORBHelper orbHelper;

    @Inject
    private ServerEnvironmentImpl env;

    @Inject @Optional
    private Agent callFlowAgent;

    @Inject
    private ProcessEnvironment processEnv;

    @Inject
    private EjbAsyncInvocationManager ejbAsyncInvocationManager;

    @Inject
    ProbeProviderFactory probeProviderFactory;

    @Inject
    Domain domain;

    @Inject
    Provider<Deployment> deploymentProvider;

    @Inject
    private Provider<CMPDeployer> cmpDeployerProvider;

    private  static EjbContainerUtil _me;

    @Override
    public void postConstruct() {
        ejbContainer = serverConfig.getExtensionByType(EjbContainer.class);

        ClassLoader ejbImplClassLoader = EjbContainerUtilImpl.class.getClassLoader();
        if (callFlowAgent == null) {
            callFlowAgent = (Agent) Proxy.newProxyInstance(ejbImplClassLoader,
                    new Class[] {Agent.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method m, Object[] args) {
                            return null;
                        }
                    });
        }

        defaultThreadPoolExecutor = createThreadPoolExecutor(DEFAULT_THREAD_POOL_NAME);
        
        //avoid starting JDK timer in application class loader.  The life of _timer
        //field is longer than deployed apps, and any reference to app class loader
        //in JDK timer thread will cause class loader leak.  Issue 17468 
        ClassLoader originalClassLoader = null;
        try {
            originalClassLoader = Utility.setContextClassLoader(ejbImplClassLoader);
            _timer = new Timer("EJB Container Timer", true);
        } finally {
            if (originalClassLoader != null) {
                Utility.setContextClassLoader(originalClassLoader);
            }
        }

        EJBObjectOutputStreamHandler.setJavaEEIOUtils(javaEEIOUtils);
        javaEEIOUtils.addGlassFishOutputStreamHandler(new EJBObjectOutputStreamHandler());
        javaEEIOUtils.addGlassFishInputStreamHandler(new EJBObjectInputStreamHandler());
        _me = this;
    }

    @Override
    public void preDestroy() {
        if( defaultThreadPoolExecutor != null ) {
            defaultThreadPoolExecutor.shutdown();
            defaultThreadPoolExecutor = null;
        }
        EJBTimerService.onShutdown();
        EJBTimerService.unsetEJBTimerService();
    }

    @Override
    public GlassFishORBHelper getORBHelper() {
        return orbHelper;
    }

    @Override
    public ServiceLocator getServices() {
        return services;
    }

    public static boolean isInitialized() {
        return (_me != null);        
    }

    public static EjbContainerUtil getInstance() {
        if (_me == null) {
            // This situation shouldn't happen. Print the error message 
            // and the stack trace to know how did we get here.

            // Create the instance first to access the logger.
            _logger.log(Level.WARNING, 
                    "Internal error: EJBContainerUtilImpl is null, creating ...",
                    new Throwable());
            _me = Globals.getDefaultHabitat().getService(
                    EjbContainerUtilImpl.class);
        }
        return _me;
    }

    public  static Logger getLogger() {
        return _logger;
    }

    @Override
    public  void registerContainer(BaseContainer container) {
        id2Container.put(container.getContainerId(), container);
    }

    @Override
    public  void unregisterContainer(BaseContainer container) {
        id2Container.remove(container.getContainerId());
    }

    @Override
    public BaseContainer getContainer(long id) {
        return getContainer(id, 0L);
    }

    @Override
    public BaseContainer getContainer(long id, long appUniqueId) {
        BaseContainer rv = id2Container.get(id);

        // try to find by using just the offset from the application
        if(rv == null && appUniqueId != 0L) {
            _logger.log(Level.FINE, "Not Found ID: {0}, looking again with appId: {1}", new Object[] { id, appUniqueId } );
            try {
                long offset = id & 0xFFFFL;
                long derivedId = appUniqueId | offset;
                rv = id2Container.get(derivedId);
            }
            catch(Exception e) {
                _logger.log(Level.FINE, "Cannot derive application ID from context", e);
            }
        }
        return rv;
    }

    @Override
    public  EjbDescriptor getDescriptor(long id) {
        BaseContainer container = id2Container.get(id);
        return (container != null) ? container.getEjbDescriptor() : null;
    }

    @Override
    public  ClassLoader getClassLoader(long id) {
        BaseContainer container = id2Container.get(id);
        return (container != null) ? container.getClassLoader() : null;
    }

    @Override
    public  Timer getTimer() {
        return _timer;
    }

    @Override
    public  void setInsideContainer(boolean bool) {
        _insideContainer = bool;
    }

    @Override
    public  boolean isInsideContainer() {
        return _insideContainer;
    }

    @Override
    public  InvocationManager getInvocationManager() {
        return _invManager;
    }

    @Override
    public  InjectionManager getInjectionManager() {
        return _injectionManager;
    }

    @Override
    public  GlassfishNamingManager getGlassfishNamingManager() {
        return _gfNamingManager;
    }

    @Override
    public  ComponentEnvManager getComponentEnvManager() {
        return _compEnvManager;
    }

    @Override
    public  ComponentInvocation getCurrentInvocation() {
        return _invManager.getCurrentInvocation();
    }

    @Override
    public JavaEETransactionManager getTransactionManager() {
        return txMgr;
    }

    @Override
    public ServerContext getServerContext() {
        return serverContext;
    }

    public EjbAsyncInvocationManager getEjbAsyncInvocationManager() {
        return ejbAsyncInvocationManager;
    }

    private TxData getTxData(JavaEETransaction tx) {
        TxData txData = tx.getContainerData();

        if ( txData == null ) {
            txData = new TxData();
            tx.setContainerData(txData);
        }
        
        return txData;
    }
    
    @Override
    public  ContainerSynchronization getContainerSync(Transaction jtx)
        throws RollbackException, SystemException
    {
        JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);

        if( txData.sync == null ) {
            txData.sync = new ContainerSynchronization(tx, this);
            tx.registerSynchronization(txData.sync);
        }

        return txData.sync;
    }

    @Override
    public void removeContainerSync(Transaction tx) {
        //No op
    }

    @Override
    public void registerPMSync(Transaction jtx, Synchronization sync)
            throws RollbackException, SystemException {

        getContainerSync(jtx).addPMSynchronization(sync);
    }

    @Override
    public EjbContainer getEjbContainer() {
        return ejbContainer;
    }

    @Override
    public ServerEnvironmentImpl getServerEnvironment() {
        return env;
    }

    @Override
    public  Vector getBeans(Transaction jtx) {
        JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);

        if( txData.beans == null ) {
            txData.beans = new Vector();
        }

        return txData.beans;

    }

    @Override
    public Object getActiveTxCache(Transaction jtx) {
    	JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);
        
        return txData.activeTxCache;
    }

    @Override
    public void setActiveTxCache(Transaction jtx, Object cache) {
    	JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);
        
        txData.activeTxCache = cache;
    }
    
    @Override
    public Agent getCallFlowAgent() {
        return callFlowAgent;
    }

    @Override
    public void addWork(Runnable task) {
        if (defaultThreadPoolExecutor != null) {
            defaultThreadPoolExecutor.submit(task);
        }
    }

    @Override
    public EjbDescriptor ejbIdToDescriptor(long ejbId) {
        throw new RuntimeException("Not supported yet");
    }

    @Override
    public boolean isEJBLite() {
        return (cmpDeployerProvider.get() == null);
    }

    @Override
    public boolean isEmbeddedServer() {
        return processEnv.getProcessType().isEmbedded();
    }

    @Override
    public Deployment getDeployment() {
        return deploymentProvider.get();
    }

    // Various pieces of data associated with a tx.  Store directly
    // in J2EETransaction to avoid repeated Map<tx, data> lookups.
    private static class TxData {
        ContainerSynchronization sync;
        Vector beans;
        Object activeTxCache;
    }
    
    @Override
    public EjbTimerService getEjbTimerService(String target) {
        EjbTimerService ejbt = null;
        if (target == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Looking for current instance ejb-container config");
            }
            ejbt = getEjbContainer().getEjbTimerService();
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Looking for {0} ejb-container config", target);
            }
            ReferenceContainer rc =  domain.getReferenceContainerNamed(target);
            if (rc != null) {
                Config config = domain.getConfigNamed(rc.getReference());
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "Found {0}", config);
                }
                if (config != null) {
                    ejbt = config.getExtensionByType(EjbContainer.class).getEjbTimerService();
                }
            }
        }

        return ejbt;
    }

    @Override
    public ProbeProviderFactory getProbeProviderFactory() {
        return probeProviderFactory;
    }

   /**
    * Embedded is a single-instance like DAS
    * @return
    */
    @Override
    public boolean isDas() {
        return env.isDas() || env.isEmbedded();
    }

    private ThreadPoolExecutor createThreadPoolExecutor(String poolName) {
        ThreadPoolExecutor result = null;
        String val = ejbContainer.getPropertyValue(RuntimeTagNames.THREAD_CORE_POOL_SIZE);
        int corePoolSize = val != null ? Integer.parseInt(val.trim())
                : EjbContainer.DEFAULT_THREAD_CORE_POOL_SIZE;

        val = ejbContainer.getPropertyValue(RuntimeTagNames.THREAD_MAX_POOL_SIZE);
        int maxPoolSize = val != null ? Integer.parseInt(val.trim())
                : EjbContainer.DEFAULT_THREAD_MAX_POOL_SIZE;

        val = ejbContainer.getPropertyValue(RuntimeTagNames.THREAD_KEEP_ALIVE_SECONDS);
        long keepAliveSeconds = val != null ? Long.parseLong(val.trim())
                : EjbContainer.DEFAULT_THREAD_KEEP_ALIVE_SECONDS;

        val = ejbContainer.getPropertyValue(RuntimeTagNames.THREAD_QUEUE_CAPACITY);
        int queueCapacity = val != null ? Integer.parseInt(val.trim())
                : EjbContainer.DEFAULT_THREAD_QUEUE_CAPACITY;

        val = ejbContainer.getPropertyValue(RuntimeTagNames.ALLOW_CORE_THREAD_TIMEOUT);
        boolean allowCoreThreadTimeout = val != null ? Boolean.parseBoolean(val.trim())
                : EjbContainer.DEFAULT_ALLOW_CORE_THREAD_TIMEOUT;

        val = ejbContainer.getPropertyValue(RuntimeTagNames.PRESTART_ALL_CORE_THREADS);
        boolean preStartAllCoreThreads = val != null ? Boolean.parseBoolean(val.trim())
                : EjbContainer.DEFAULT_PRESTART_ALL_CORE_THREADS;

        BlockingQueue workQueue = queueCapacity > 0
                ? new LinkedBlockingQueue<Runnable>(queueCapacity)
                : new SynchronousQueue(true);

        // PAYARA-405 validates attributes of the thread pool to ensure no problems      
        if (corePoolSize < 0) {
            _logger.log(Level.WARNING, "Core Pool Size configured to be less than 0. Resetting to 0");
            corePoolSize = 0;
        }
        
        if (maxPoolSize < 1) {
            _logger.log(Level.WARNING, "Max Pool Size configured to be less than 1. Resetting to 1");
            maxPoolSize = 1;
        }
                
        if (corePoolSize > maxPoolSize) {
            _logger.log(Level.WARNING, "Core Pool Size configured to be greater than maxPoolSize. Resetting to maxPoolSize {0}", maxPoolSize);
            corePoolSize = maxPoolSize;
        }
        
        if (keepAliveSeconds < 0) {
            _logger.log(Level.WARNING, "Keep Alive Seconds configured to be less than 0. Resetting to 0");
            keepAliveSeconds = 0;            
        }
        
        result = new EjbThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, workQueue, poolName);

        if(allowCoreThreadTimeout) {
            result.allowCoreThreadTimeOut(true);
        }
        if (preStartAllCoreThreads) {
            result.prestartAllCoreThreads();
        }

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "Created {0}", result.toString());

        }
        return result;
    }
    
    @Override
    public ThreadPoolExecutor getThreadPoolExecutor(String poolName) {
        if(poolName == null) {
            return defaultThreadPoolExecutor;
        } 
        return null;
//        TODO retrieve the named ThreadPoolExecutor
    }

    @Override
    public JavaEEIOUtils getJavaEEIOUtils() {
        return javaEEIOUtils;
    }
}
