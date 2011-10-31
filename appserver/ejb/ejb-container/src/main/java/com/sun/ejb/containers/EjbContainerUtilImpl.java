/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import com.sun.ejb.base.sfsb.util.EJBServerConfigLookup;
import com.sun.enterprise.deployment.xml.RuntimeTagNames;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.glassfish.api.ActionReport;
import org.glassfish.deployment.common.DeploymentProperties;
import org.glassfish.ejb.spi.CMPDeployer;
import org.glassfish.ejb.api.DistributedEJBTimerService;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.persistence.common.Java2DBProcessorHelper;
import org.glassfish.persistence.common.DatabaseConstants;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.flashlight.provider.ProbeProviderFactory;

import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.Synchronization;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.io.File;
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

    private Logger _logger = LogDomains.getLogger(EjbContainerUtilImpl.class, LogDomains.EJB_LOGGER);

    private ThreadPoolExecutor defaultThreadPoolExecutor;
    
    @Inject
    private Habitat habitat;

    @Inject
    private ServerContext serverContext;

    // Flag that allows to load EJBTimerService on the 1st access and
    // distinguish between not available and not loaded
    private  volatile boolean _ejbTimerServiceVerified = false;

    // Flag that determines if timers cleanup is needed after upgrade
    private  boolean _ejbTimersCleanup = false;

    // If EJBTimerService is not yet loaded, keep the value to set it later.
    private  volatile boolean _doDBReadBeforeTimeout = false;

    private static Object lock = new Object();

    private  volatile EJBTimerService _ejbTimerService;

    private  Map<Long, BaseContainer> id2Container
            = new ConcurrentHashMap<Long, BaseContainer>();

    private  Timer _timer = new Timer(true);

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

    @Inject(name=ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private EjbContainer ejbContainer;

    @Inject
    private GlassFishORBHelper orbHelper;

    @Inject
    private ServerEnvironmentImpl env;

    @Inject(optional=true)
    private Agent callFlowAgent;

    @Inject //Note:- Not specific to any ejb descriptor
    private EJBServerConfigLookup ejbServerConfigLookup;

    @Inject
    private ProcessEnvironment processEnv;

    @Inject
    private EjbAsyncInvocationManager ejbAsyncInvocationManager;

    @Inject
    ProbeProviderFactory probeProviderFactory;

    @Inject
    Domain domain;

    private  static EjbContainerUtil _me;

    public String getHAPersistenceType() {
        return ejbServerConfigLookup.getSfsbHaPersistenceTypeFromConfig();
    }

    public void postConstruct() {
        if (callFlowAgent == null) {
            callFlowAgent = (Agent) Proxy.newProxyInstance(EjbContainerUtilImpl.class.getClassLoader(),
                    new Class[] {Agent.class},
                    new InvocationHandler() {
                        public Object invoke(Object proxy, Method m, Object[] args) {
                            return null;
                        }
                    });
        }

        defaultThreadPoolExecutor = createThreadPoolExecutor(DEFAULT_THREAD_POOL_NAME);
        
        if (!isDas()) {
            // On a clustered instance default is true
            _doDBReadBeforeTimeout = true;
        }
        _me = this;
    }

    public void preDestroy() {
        if( defaultThreadPoolExecutor != null ) {
            defaultThreadPoolExecutor.shutdown();
            defaultThreadPoolExecutor = null;
        }
    }

    public GlassFishORBHelper getORBHelper() {
        return orbHelper;
    }

    public Habitat getDefaultHabitat() {
        return habitat;
    }

    public static boolean isInitialized() {
        return (_me != null);        
    }

    public static EjbContainerUtil getInstance() {
        if (_me == null) {
            // This situation shouldn't happen. Print the error message 
            // and the stack trace to know how did we get here.

            // Create the instance first to access the logger.
            _me = Globals.getDefaultHabitat().getComponent(
                    EjbContainerUtilImpl.class);
            _me.getLogger().log(Level.WARNING, 
                    "Internal error: EJBContainerUtilImpl was null",
                    new Throwable());
        }
        return _me;
    }

    public  Logger getLogger() {
        return _logger;
    }

    public  void setEJBTimerService(EJBTimerService es) {
        _ejbTimerService = es;
    }

    public  void unsetEJBTimerService() {
        _ejbTimerServiceVerified = false;
        _ejbTimerService = null;
    }

    public  EJBTimerService getEJBTimerService() {
        return getEJBTimerService(null);
    }

    public  boolean isEJBTimerServiceLoaded() {
        return _ejbTimerServiceVerified;
    }

    public  void setEJBTimerServiceDBReadBeforeTimeout(boolean value) {
        _doDBReadBeforeTimeout = value;
        if (_ejbTimerService != null && _ejbTimerService instanceof PersistenceEJBTimerService) {
            ((PersistenceEJBTimerService)_ejbTimerService).setPerformDBReadBeforeTimeout(value);
        }
    }

    public  EJBTimerService getEJBTimerService(String target) {
        return getEJBTimerService(target, true);
    }

    public  EJBTimerService getEJBTimerService(String target, boolean force) {
        if (!_ejbTimerServiceVerified) {
            if (isEJBLite()) {
                if (_ejbTimerService == null) {
                    try {
                        _ejbTimerService = new EJBTimerService();
                    } catch (Exception e) {
                        _logger.log (Level.WARNING, "Cannot start EJBTimerService: ", e);
                    }
                }
            } else {
                deployEJBTimerService(target);

                // Do postprocessing if everything is OK
                if (_ejbTimerService != null) {
                    // load DistributedEJBTimerService 
                    habitat.getByContract(DistributedEJBTimerService.class);
                    if (_ejbTimersCleanup) {
                        _ejbTimerService.destroyAllTimers(0L);
                    } else if (target == null) {
                        // target is null when accessed from the BaseContainer on load, i.e. where timers are running
                        _logger.log(Level.INFO, "Setting DBReadBeforeTimeout to " + _doDBReadBeforeTimeout);
                        ((PersistenceEJBTimerService)_ejbTimerService).setPerformDBReadBeforeTimeout(_doDBReadBeforeTimeout);
                        _logger.log(Level.INFO, "==> Restoring Timers ... " );
                        if (_ejbTimerService.restoreEJBTimers()) {
                            _logger.log(Level.INFO, "<== ... Timers Restored.");
                        }
                    }
                } else if (!force) {
                    // If it was a request with force == false, and we failed to load the service,
                    // do not mark it as verified
                    _ejbTimerServiceVerified = false;
                }
            }
        }
        return _ejbTimerService;
    }

    public  void registerContainer(BaseContainer container) {
        id2Container.put(container.getContainerId(), container);
    }

    public  void unregisterContainer(BaseContainer container) {
        id2Container.remove(container.getContainerId());
    }

    public  BaseContainer getContainer(long id) {
        return id2Container.get(id);
    }

    public  EjbDescriptor getDescriptor(long id) {
        BaseContainer container = id2Container.get(id);
        return (container != null) ? container.getEjbDescriptor() : null;
    }

    public  ClassLoader getClassLoader(long id) {
        BaseContainer container = id2Container.get(id);
        return (container != null) ? container.getClassLoader() : null;
    }

    public  Timer getTimer() {
        return _timer;
    }

    public  void setInsideContainer(boolean bool) {
        _insideContainer = bool;
    }

    public  boolean isInsideContainer() {
        return _insideContainer;
    }

    public  InvocationManager getInvocationManager() {
        return _invManager;
    }

    public  InjectionManager getInjectionManager() {
        return _injectionManager;
    }

    public  GlassfishNamingManager getGlassfishNamingManager() {
        return _gfNamingManager;
    }

    public  ComponentEnvManager getComponentEnvManager() {
        return _compEnvManager;
    }

    public  ComponentInvocation getCurrentInvocation() {
        return _invManager.getCurrentInvocation();
    }

    public JavaEETransactionManager getTransactionManager() {
        return txMgr;
    }

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

    public void removeContainerSync(Transaction tx) {
        //No op
    }

    public void registerPMSync(Transaction jtx, Synchronization sync)
            throws RollbackException, SystemException {

        getContainerSync(jtx).addPMSynchronization(sync);
    }

    public EjbContainer getEjbContainer() {
        return ejbContainer;
    }

    public ServerEnvironmentImpl getServerEnvironment() {
        return env;
    }

    public  Vector getBeans(Transaction jtx) {
        JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);

        if( txData.beans == null ) {
            txData.beans = new Vector();
        }

        return txData.beans;

    }

    public Object getActiveTxCache(Transaction jtx) {
    	JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);
        
        return txData.activeTxCache;
    }

    public void setActiveTxCache(Transaction jtx, Object cache) {
    	JavaEETransaction tx = (JavaEETransaction) jtx;
        TxData txData = getTxData(tx);
        
        txData.activeTxCache = cache;
    }
    
    public Agent getCallFlowAgent() {
        return callFlowAgent;
    }

    public void addWork(Runnable task) {
        defaultThreadPoolExecutor.submit(task);
    }

    public EjbDescriptor ejbIdToDescriptor(long ejbId) {
        throw new RuntimeException("Not supported yet");
    }

    public boolean isEJBLite() {
        return (habitat.getByContract(CMPDeployer.class) == null);
    }

    public boolean isEmbeddedServer() {
        return processEnv.getProcessType().isEmbedded();
    }

    // Various pieces of data associated with a tx.  Store directly
    // in J2EETransaction to avoid repeated Map<tx, data> lookups.
    private static class TxData {
        ContainerSynchronization sync;
        Vector beans;
        Object activeTxCache;
    }
    
    private void deployEJBTimerService(String target) {
        synchronized (lock) {
            Deployment deployment = habitat.getByContract(Deployment.class);
            boolean isRegistered = deployment.isRegistered(EjbContainerUtil.TIMER_SERVICE_APP_NAME);

            if (isRegistered) {
                _logger.log (Level.WARNING, "EJBTimerService had been explicitly deployed.");
            } else {
                _logger.log (Level.INFO, "Loading EJBTimerService. Please wait.");

                File root = serverContext.getInstallRoot();
                File app = null;
                try {
                    app = FileUtils.getManagedFile(EjbContainerUtil.TIMER_SERVICE_APP_NAME + ".war",
                            new File(root, "lib/install/applications/"));
                } catch (Exception e) {
                    _logger.log (Level.WARNING, "Caught unexpected exception", e);
                }

                if (app == null || !app.exists()) {
                    _logger.log (Level.WARNING, "Cannot deploy or load EJBTimerService: " +
                            "required WAR file (" + 
                            EjbContainerUtil.TIMER_SERVICE_APP_NAME + ".war) is not installed");
                } else {
                    ActionReport report = habitat.getComponent(ActionReport.class, "plain");
                    DeployCommandParameters params = new DeployCommandParameters(app);
                    String appName = EjbContainerUtil.TIMER_SERVICE_APP_NAME;
                    params.name = appName;

                    File rootScratchDir = env.getApplicationStubPath();
                    File appScratchFile = new File(rootScratchDir, appName);
                    try {
                        String resourceName = getTimerResource(target);
                        if (resourceName != null) {
                            // appScratchFile is a marker file and needs to be created on Das on the 
                            // first access of the Timer Service application - so use & instead of &&
                            if (isDas() && (!isUpgrade(resourceName, target, appScratchFile.exists()) & appScratchFile.createNewFile())) {
                                params.origin = OpsParams.Origin.deploy;
                            } else {
                                params.origin = OpsParams.Origin.load;
                            }
                            params.target = env.getInstanceName();

                            ExtendedDeploymentContext dc = deployment.getBuilder(
                                    _logger, params, report).source(app).build();
                            dc.addTransientAppMetaData(DatabaseConstants.JTA_DATASOURCE_JNDI_NAME_OVERRIDE, resourceName);
                            Properties appProps = dc.getAppProps();
                            appProps.setProperty(ServerTags.OBJECT_TYPE, DeploymentProperties.SYSTEM_ALL);

                            deployment.deploy(dc);

                            if (report.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                                _logger.log (Level.WARNING, "Cannot deploy or load EJBTimerService: ",
                                        report.getFailureCause());
                            } else {
                                _logger.log(Level.INFO, "ejb.timer_service_started", new Object[] { resourceName } );
                            }
                        } else {
                            _logger.log (Level.WARNING, "Cannot start EJBTimerService: Timer resource for target " 
                                    + target + " is not available");
                        }

                    } catch (Exception e) {
                        _logger.log (Level.WARNING, "Cannot deploy or load EJBTimerService: ", e);
                    } finally {
                        if (_ejbTimerService == null && params.origin.isDeploy() && appScratchFile.exists()) {
                            // Remove marker file if deploy failed
                            appScratchFile.delete();
                        }
                    }
                }
            }
        }

        _ejbTimerServiceVerified = true;
    }

    private boolean isUpgrade(String resource, String target, boolean upgrade_with_load) {
        boolean upgrade = false;

        Property prop = null;
        EjbTimerService ejbt = getEjbTimerService(target);
        if (ejbt != null) {
            List<Property> properties = ejbt.getProperty();
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

        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("===> Upgrade? <==");
        }
        if (upgrade) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("===> Upgrade! <==");
            }
            boolean success = false;
            try {
                File root = serverContext.getInstallRoot();
                File dir = new File(root, "lib/install/databases/upgrade");

                if (!dir.exists()) {
                    _logger.log (Level.WARNING, "Cannot upgrade EJBTimerService: " +
                            "required directory is not available");
                } else {
                    Java2DBProcessorHelper h = new Java2DBProcessorHelper(
                            EjbContainerUtil.TIMER_SERVICE_APP_NAME);
                    success = h.executeDDLStatement(
                            dir.getCanonicalPath() + "/ejbtimer_upgrade_", resource);
                    _ejbTimersCleanup = !upgrade_with_load;
                    ConfigSupport.apply(new SingleConfigCode<Property>() {
                        public Object run(Property p) throws PropertyVetoException, TransactionFailure {
                            p.setValue("true");
                            return null;
                        }
                    }, prop);
                }
            } catch (Exception e) {
                _logger.log (Level.WARNING, "", e);
            }
            if (!success) {
                _logger.log (Level.SEVERE, "Failed to upgrade load EJBTimerService: " +
                            "see log for details");
            }
        }

        return upgrade;
    }

    public String getTimerResource() {
        return getTimerResource(null);
    }

    private String getTimerResource(String target) {
        String resource = null;
        EjbTimerService ejbt = getEjbTimerService(target);
        if (ejbt != null) {
            if (ejbt.getTimerDatasource() != null) {
                resource = ejbt.getTimerDatasource();
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Found Timer Service resource name " + resource);
                }
            } else {
                resource = EjbContainerUtil.TIMER_RESOURCE_JNDI;
            }
        }
        return resource;
    }

    private EjbTimerService getEjbTimerService(String target) {
        EjbTimerService ejbt = null;
        if (target == null) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Looking for current instance ejb-container config");
            }
            ejbt = getEjbContainer().getEjbTimerService();
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Looking for " + target + " ejb-container config");
            }
            ReferenceContainer rc =  domain.getReferenceContainerNamed(target);
            if (rc != null) {
                Config config = domain.getConfigNamed(rc.getReference());
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Found " + config);
                }
                if (config != null) {
                    ejbt = config.getExtensionByType(EjbContainer.class).getEjbTimerService();
                }
            }
        }

        return ejbt;
    }

    public ProbeProviderFactory getProbeProviderFactory() {
        return probeProviderFactory;
    }

   /**
    * Embedded is a single-instance like DAS
    */
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

        result = new EjbThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, workQueue, poolName);

        if(allowCoreThreadTimeout) {
            result.allowCoreThreadTimeOut(true);
        }
        if (preStartAllCoreThreads) {
            result.prestartAllCoreThreads();
        }

        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("Created " + result.toString());

        }
        return result;
    }
    
    public ThreadPoolExecutor getThreadPoolExecutor(String poolName) {
        if(poolName == null) {
            return defaultThreadPoolExecutor;
        } 
        return null;
//        TODO retrieve the named ThreadPoolExecutor
    }

}
