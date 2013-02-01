/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;

import com.sun.appserv.util.cache.CacheListener;
import com.sun.ejb.Container;
import com.sun.ejb.ContainerFactory;
import com.sun.ejb.base.container.util.CacheProperties;
import com.sun.ejb.base.sfsb.util.ScrambledKeyGenerator;
import com.sun.ejb.base.sfsb.util.SimpleKeyGenerator;
import com.sun.ejb.containers.util.cache.FIFOSessionCache;
import com.sun.ejb.containers.util.cache.LruSessionCache;
import com.sun.ejb.containers.util.cache.NRUSessionCache;
import com.sun.ejb.containers.util.cache.UnBoundedSessionCache;
import com.sun.enterprise.config.serverbeans.AvailabilityService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.config.EjbContainerAvailability;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreConfiguration;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.api.BackingStoreFactory;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * A builder for StatefulSessionContainer. Takes care of
 * building / initializing the StatefulSessionContainer
 * with the following classes:
 * a) Cache (LRU / NRU / FIFO / UnBounded)
 * b) SFSBStoreManager (Using PersistenceStrategyBuilder)
 * c) Cache passivation task (if idle-timeout is greater than 0)
 * d) Passivated sessions removal task (if removal-timeout is greater than 0)
 * e) CheckpointPolicy (if ha enabled)
 * f) SFSBUUIDUtil
 * g) BeanLifecycleManager
 *
 * @author Mahesh Kannan
 */
@Service(name = "StatefulContainerFactory")
@PerLookup
public class StatefulContainerFactory extends BaseContainerFactory
        implements PostConstruct, ContainerFactory {
    protected static final Logger _logger  = LogFacade.getLogger();

    @LogMessageInfo(
        message = "TopLevel AvailabilityService.getAvailabilityEnabled: [{0}]",
        level = "INFO")
    private static final String SFSB_BUILDER_TOP_LEVEL_AVAILABILITY_SERVICE_ENABLED = "AS-EJB-00036";

    @LogMessageInfo(
        message = "TopLevel EjbAvailabilityService.getAvailabilityEnabled: [{0}]",
        level = "INFO")
    private static final String SFSB_BUILDER_EJB_AVAILABILITY_SERVICE_ENABLED = "AS-EJB-00037";

    @LogMessageInfo(
        message = "Global AvailabilityEnabled: [{0}], application AvailabilityEnabled: [{1}]",
        level = "INFO")
    private static final String SFSB_BUILDER_GLOBAL_AND_APP_AVAILABILITY_ENABLED = "AS-EJB-00038";

    @LogMessageInfo(
        message = "Exception while trying to determine availability-enabled settings for this app",
        level = "WARNING")
    private static final String SFSB_BUILDER_DETERMINE_AVAILABILITY_EXCEPTION = "AS-EJB-00039";

    @LogMessageInfo(
        message = "StatefulContainerBuilder AvailabilityEnabled [{0}] for this application",
        level = "INFO")
    private static final String SFSB_BUILDER_RESOLVED_AVAILABILITY_ENABLED = "AS-EJB-00040";

    @LogMessageInfo(
        message = "StatefulContainerBuilder.buildStoreManager() storeName: [{0}]",
        level = "INFO")
    private static final String SFSB_BUILDER_STORE_NAME = "AS-EJB-00041";

    @LogMessageInfo(
        message = "Could not instantiate backing store for type [{0}]",
        level = "WARNING")
    private static final String SFSB_BUILDER_INSTANTIATE_BACKING_STORE_EXCEPTION = "AS-EJB-00042";

    @LogMessageInfo(
        message = "StatefulContainerbuilder instantiated store: {0}, " +
                "with ha-enabled [{1}], and backing store configuration: {2}",
        level = "INFO")
    private static final String SFSB_BUILDER_INSTANTIATED_BACKING_STORE = "AS-EJB-00043";

    @LogMessageInfo(
        message = "Error while adding idle bean passivator task",
        level = "WARNING")
    private static final String SFSB_HELPER_ADD_IDLE_PASSIVATOR_TASK_FAILED = "AS-EJB-00044";

    @LogMessageInfo(
        message = "Error while adding idle bean removal task",
        level = "WARNING")
    private static final String SFSB_HELPER_ADD_REMOVE_PASSIVATOR_TASK_FAILED = "AS-EJB-00045";

    @LogMessageInfo(
        message = "Error while removing idle beans for [{0}]",
        level = "WARNING")
    static final String SFSB_HELPER_REMOVE_IDLE_BEANS_FAILED = "AS-EJB-00046";

    @LogMessageInfo(
        message = "Error while removing expired beans for [{0}]",
        level = "WARNING")
    static final String SFSB_HELPER_REMOVE_EXPIRED_BEANS_FAILED = "AS-EJB-00047";

    @LogMessageInfo(
        message = "Disabling high availability for the stateful session bean {0}, as its marked non passivatable",
        level = "WARNING")
    private static final String SFSB_HA_DISABLED_BY_PASSIVATION_SETTING = "AS-EJB-00051";

    private static final Level TRACE_LEVEL = Level.FINE;

    private EjbDescriptor		    ejbDescriptor;

    private StatefulSessionContainer sfsbContainer;

    @Inject
    private ServiceLocator services;

    @Inject
    private CacheProperties cacheProps;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME) @Optional
    private AvailabilityService availabilityService;

    @Inject @Optional
    private EjbContainerAvailability ejbAvailability;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config serverConfig;

    EjbContainer ejbContainerConfig;

    @Inject @Optional
    GMSAdapterService gmsAdapterService;
    
    private LruSessionCache sessionCache;

    private BackingStore<Serializable, SimpleMetadata> backingStore;

    private boolean HAEnabled = false;

    private boolean asyncReplication = true;

    private SimpleKeyGenerator keyGen;

    public void postConstruct() {
        ejbContainerConfig = serverConfig.getExtensionByType(EjbContainer.class);
    }

    public void buildComponents(byte[] ipAddress, int port,
                                DeploymentContext dc)
            throws Exception {
        if (availabilityService != null) {
            this.HAEnabled = Boolean.valueOf(availabilityService.getAvailabilityEnabled());
            _logger.log(Level.INFO, SFSB_BUILDER_TOP_LEVEL_AVAILABILITY_SERVICE_ENABLED, this.HAEnabled);
            if ((this.HAEnabled) && (ejbAvailability != null)) {
                this.HAEnabled = Boolean.valueOf(ejbAvailability.getAvailabilityEnabled());
                _logger.log(Level.INFO, SFSB_BUILDER_EJB_AVAILABILITY_SERVICE_ENABLED, this.HAEnabled);
            }

            boolean appLevelHAEnabled = false;
            try {
                if (HAEnabled) {
                    if (dc != null) {
                        DeployCommandParameters params = dc.getCommandParameters(DeployCommandParameters.class);
                        if (params != null) {
                            appLevelHAEnabled = params.availabilityenabled;
                            asyncReplication = params.asyncreplication;
                        }
                    }
                    
                    _logger.log(Level.INFO, SFSB_BUILDER_GLOBAL_AND_APP_AVAILABILITY_ENABLED,
                            new Object[] {this.HAEnabled, appLevelHAEnabled});
                }
            } catch (Exception ex) {
                _logger.log(Level.WARNING, SFSB_BUILDER_DETERMINE_AVAILABILITY_EXCEPTION, ex);
                appLevelHAEnabled = false;
            }

            HAEnabled = HAEnabled && appLevelHAEnabled;
            _logger.log(Level.INFO, SFSB_BUILDER_RESOLVED_AVAILABILITY_ENABLED, this.HAEnabled);
        }

        EjbSessionDescriptor sessionDescriptor = (EjbSessionDescriptor)ejbDescriptor;
        //When passivation is disabled, we should also forbid ha.
        if (!sessionDescriptor.isPassivationCapable() && HAEnabled) {
            if (_logger.isLoggable(Level.WARNING)) {
                _logger.log(Level.WARNING, SFSB_HA_DISABLED_BY_PASSIVATION_SETTING, ejbDescriptor.getEjbClassName());
            }
            HAEnabled = false;
        }

        buildCheckpointPolicy(this.HAEnabled);
        buildSFSBUUIDUtil(ipAddress, port);

        //First build BackingStore before Cache is built
        if (sessionDescriptor.isPassivationCapable()){
            buildStoreManager();
        } else{
            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, "Stateful session bean passivation is disabled, so do not create store manger");
            }
        }

        buildCache();
        scheduleTimerTasks(sfsbContainer);
    }

    /************************* Private Methods *************************/
    /**
     * ***************************************************************
     */

    private final void buildCheckpointPolicy(boolean haEnabled) {
        sfsbContainer.setHAEnabled(haEnabled);
    }

    private void buildSFSBUUIDUtil(byte[] ipAddress, int port) {
        //Just for debugging purpose,  we instantiate
        //  two different key generators
        keyGen = HAEnabled
                ? new ScrambledKeyGenerator(ipAddress, port)
                : new SimpleKeyGenerator(ipAddress, port);
        sfsbContainer.setSFSBUUIDUtil(keyGen);
    }

    private void buildStoreManager()
        throws BackingStoreException {

        String persistenceStoreType = "file";

        if (ejbAvailability != null) {
            persistenceStoreType = HAEnabled
                ? ejbAvailability.getSfsbHaPersistenceType() : ejbAvailability.getSfsbPersistenceType();
            if ("ha".equals(persistenceStoreType)) {
                persistenceStoreType = "replicated";
            } else if ("memory".equals(persistenceStoreType)) {
                persistenceStoreType = "file";
            }
        }



        BackingStoreConfiguration<Serializable, SimpleMetadata> conf = new BackingStoreConfiguration<Serializable, SimpleMetadata>();
        String storeName = ejbDescriptor.getName() + "-" + ejbDescriptor.getUniqueId() + "-BackingStore";

        _logger.log(Level.INFO, SFSB_BUILDER_STORE_NAME, storeName);
        
        String subDirName = "";

/*        if (ejbDescriptor.getApplication().isVirtual()) {
            String archURI = ejbDescriptor.getEjbBundleDescriptor().
                    getModuleDescriptor().getArchiveUri();
            subDirName += FileUtils.makeFriendlyFilename(archURI);
            subDirName += "_" + FileUtils.makeFriendlyFilename(ejbDescriptor.getName());
        } else {
            subDirName += FileUtils.makeFriendlyFilename(ejbDescriptor.getApplication().getRegistrationName());
            subDirName += "_" + FileUtils.makeFriendlyFilename(ejbDescriptor.getEjbBundleDescriptor().getName());
            subDirName += "_" + FileUtils.makeFriendlyFilename(ejbDescriptor.getName());
        }*/

        subDirName += ejbDescriptor.getName() + "-" + ejbDescriptor.getUniqueId();
        
        conf.setShortUniqueName(""+ejbDescriptor.getUniqueId()).setStoreName(storeName)
                .setStoreType(persistenceStoreType)
                .setBaseDirectory(new File(ejbContainerConfig.getSessionStore(), subDirName))
                .setKeyClazz(Serializable.class)
                .setValueClazz(SimpleMetadata.class)
                .setClassLoader(StatefulContainerFactory.class.getClassLoader());


        Map<String, Object> vendorMap = conf.getVendorSpecificSettings();
        vendorMap.put("local.caching", true);
        vendorMap.put("start.gms", false);
        vendorMap.put("async.replication", asyncReplication);
        vendorMap.put("broadcast.remove.expired", false);
        vendorMap.put("value.class.is.thread.safe", true);
        vendorMap.put("key.transformer", keyGen);

        if (gmsAdapterService != null) {
            GMSAdapter gmsAdapter = gmsAdapterService.getGMSAdapter();
            if (gmsAdapter != null) {
                conf.setClusterName(gmsAdapter.getClusterName());
                conf.setInstanceName(gmsAdapter.getModule().getInstanceName());
            }
        }
        
        BackingStoreFactory factory = null;
        try {
            factory = services.getService(BackingStoreFactory.class, persistenceStoreType);
        } catch (Exception ex) {
            _logger.log(Level.WARNING, SFSB_BUILDER_INSTANTIATE_BACKING_STORE_EXCEPTION,
                    new Object[]{persistenceStoreType, ex});
        }

        try {
            if (factory == null) {
                factory = services.getService(BackingStoreFactory.class, "noop");
            }
            this.backingStore = factory.createBackingStore(conf);
        } catch (Exception ex) {
            _logger.log(Level.WARNING, SFSB_BUILDER_INSTANTIATE_BACKING_STORE_EXCEPTION,
                    new Object[]{persistenceStoreType, ex});
            throw new BackingStoreException("Could not instantiate backing store for type [" +
                    persistenceStoreType + "]", ex);
        }
        _logger.log(Level.INFO, SFSB_BUILDER_INSTANTIATED_BACKING_STORE, new Object[]{backingStore, HAEnabled, conf});
    }

    private void buildCache() {
        String cacheName = ejbDescriptor.getEjbClassName();
        String victimPolicy = cacheProps.getVictimSelectionPolicy();

        if (cacheProps.getMaxCacheSize() <= 0) {
            sessionCache = new UnBoundedSessionCache(cacheName, sfsbContainer,
                    cacheProps.getCacheIdleTimeoutInSeconds(),
                    cacheProps.getRemovalTimeoutInSeconds());
        } else if ("lru".equalsIgnoreCase(victimPolicy)) {
            sessionCache = new LruSessionCache(cacheName, sfsbContainer,
                    cacheProps.getCacheIdleTimeoutInSeconds(),
                    cacheProps.getRemovalTimeoutInSeconds());
        } else if ("fifo".equalsIgnoreCase(victimPolicy)) {
            sessionCache = new FIFOSessionCache(cacheName, sfsbContainer,
                    cacheProps.getCacheIdleTimeoutInSeconds(),
                    cacheProps.getRemovalTimeoutInSeconds());
        } else {
            sessionCache = new NRUSessionCache(cacheName, sfsbContainer,
                    cacheProps.getCacheIdleTimeoutInSeconds(),
                    cacheProps.getRemovalTimeoutInSeconds());
        }


        float ratio = (float) (1.0 * cacheProps.getNumberOfVictimsToSelect()
                / cacheProps.getMaxCacheSize());
        float loadFactor = (float) (1.0 - ratio);
        if (loadFactor < 0 || loadFactor > 1) {
            loadFactor = 0.75f;
        }

        if (cacheProps.getMaxCacheSize() <= 0) {
            sessionCache.init(16 * 1024, loadFactor, null);
        } else {
            sessionCache.init(cacheProps.getMaxCacheSize(), loadFactor, null);
        }

        sessionCache.addCacheListener((CacheListener) sfsbContainer);

        sfsbContainer.setSessionCache(sessionCache);
        sessionCache.setBackingStore(backingStore);
        sfsbContainer.setBackingStore(this.backingStore);
        if (cacheProps.getNumberOfVictimsToSelect() >
                sfsbContainer.MIN_PASSIVATION_BATCH_COUNT) {
            sfsbContainer.setPassivationBatchCount(
                    cacheProps.getNumberOfVictimsToSelect());
        }

        if (_logger.isLoggable(TRACE_LEVEL)) {
            _logger.log(TRACE_LEVEL,
                    "Created cache for {0}; cache properties: {1}; loadFactor: {2}; backingStore: {3}",
                    new Object[]{ejbDescriptor.getName(), cacheProps, loadFactor, this.backingStore});
        }
    }

    private void scheduleTimerTasks(StatefulSessionContainer container) {
        String ejbName = ejbDescriptor.getEjbClassName();

        if (cacheProps.getCacheIdleTimeoutInSeconds() > 0) {
            long timeout = cacheProps.getCacheIdleTimeoutInSeconds() * 1000L;
            try {
                sfsbContainer.invokePeriodically(timeout, timeout,
                        new CachePassivatorTask(ejbName, sessionCache, _logger));
                if (_logger.isLoggable(TRACE_LEVEL)) {
                    _logger.log(TRACE_LEVEL, "Added CachePassivator for {0} to run after {1} milliseconds",
                            new Object[]{ejbName, timeout});
                }

            } catch (Throwable th) {
                _logger.log(Level.WARNING, SFSB_HELPER_ADD_IDLE_PASSIVATOR_TASK_FAILED, th);
            }
        }

        if (cacheProps.getRemovalTimeoutInSeconds() > 0 && container.isPassivationCapable()) {
            long timeout = cacheProps.getRemovalTimeoutInSeconds() * 1000L;
            try {
                sfsbContainer.invokePeriodically(timeout, timeout,
                        new ExpiredSessionsRemovalTask(ejbName,
                                this.sfsbContainer, _logger));
                if (_logger.isLoggable(TRACE_LEVEL)) {
                    _logger.log(TRACE_LEVEL, "Added StorePassivator for {0} to run after {1} milliseconds",
                            new Object[]{ejbName, timeout});
                }
            } catch (Throwable th) {
                _logger.log(Level.WARNING, SFSB_HELPER_ADD_REMOVE_PASSIVATOR_TASK_FAILED, th);
            }
        }

    }

  @Override
  public Container createContainer(EjbDescriptor ejbDescriptor,
                                   ClassLoader loader,
                                   DeploymentContext deployContext)
          throws Exception {
    this.ejbDescriptor = ejbDescriptor;

    //FIXME: Read from domain.xml iiop-service ip-addr
    byte[] ipAddress = new byte[4];
    try {
      ipAddress = InetAddress.getLocalHost().getAddress();
    } catch (Exception ex) {
      long val = System.identityHashCode(ipAddress)
              + System.currentTimeMillis();
      Utility.longToBytes(val, ipAddress, 0);
    }

    //FIXME: Read from domain.xml
    int port = 8080;

    cacheProps.init(ejbDescriptor);
    SecurityManager sm = getSecurityManager(ejbDescriptor);
    sfsbContainer = new StatefulSessionContainer(ejbDescriptor, loader, sm);
    buildComponents(ipAddress, port, deployContext);
    sfsbContainer.initializeHome();
    return sfsbContainer;
  }

}

class CachePassivatorTask
        implements Runnable {

    private LruSessionCache cache;
    private Logger logger;
    private String name;

    CachePassivatorTask(String name, LruSessionCache cache, Logger logger) {
        this.name = name;
        this.cache = cache;
        this.logger = logger;
    }

    public void run() {
        try {
            cache.trimTimedoutItems(Integer.MAX_VALUE);
        } catch (Exception ex) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, StatefulContainerFactory.SFSB_HELPER_REMOVE_IDLE_BEANS_FAILED,
                        new Object[]{name, ex});
            }
        }
    }
}

class ExpiredSessionsRemovalTask
        implements Runnable {
    private StatefulSessionContainer container;
    private Logger logger;
    private String name;

    ExpiredSessionsRemovalTask(String name,
                               StatefulSessionContainer container, Logger logger) {
        this.name = name;
        this.container = container;
        this.logger = logger;
    }

    public void run() {
        try {
            container.removeExpiredSessions();
        } catch (Exception ex) {
            if (logger.isLoggable(Level.WARNING)) {
                logger.log(Level.WARNING, StatefulContainerFactory.SFSB_HELPER_REMOVE_EXPIRED_BEANS_FAILED,
                        new Object[]{name, ex});
            }
        }
    }
}
