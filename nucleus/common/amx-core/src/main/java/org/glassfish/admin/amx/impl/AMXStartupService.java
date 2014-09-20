/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.amx.impl;

import org.glassfish.admin.amx.base.DomainRoot;
import org.glassfish.admin.amx.base.MBeanTracker;
import org.glassfish.admin.amx.base.MBeanTrackerMBean;
import org.glassfish.admin.amx.base.SystemInfo;
import org.glassfish.admin.amx.core.proxy.ProxyFactory;
import org.glassfish.admin.amx.impl.mbean.ComplianceMonitor;
import org.glassfish.admin.amx.impl.mbean.DomainRootImpl;
import org.glassfish.admin.amx.impl.mbean.SystemInfoFactory;
import org.glassfish.admin.amx.impl.mbean.SystemInfoImpl;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.impl.util.SingletonEnforcer;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.FeatureAvailability;
import org.glassfish.admin.amx.util.TimingDelta;
import org.glassfish.admin.amx.util.jmx.stringifier.StringifierRegistryIniter;
import org.glassfish.admin.amx.util.stringifier.StringifierRegistryImpl;
import org.glassfish.admin.amx.util.stringifier.StringifierRegistryIniterImpl;
import org.glassfish.admin.mbeanserver.AMXStartupServiceMBean;
import org.glassfish.api.amx.AMXLoader;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.external.amx.AMXUtil;
import org.glassfish.external.amx.MBeanListener;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.management.*;
import javax.management.remote.JMXServiceURL;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.admin.amx.util.AMXLoggerInfo;

/**
An {@link AMXLoader} responsible for loading core amx MBeans
 */
@Service
public final class AMXStartupService
        implements org.glassfish.hk2.api.PostConstruct,
        org.glassfish.hk2.api.PreDestroy,
        AMXStartupServiceMBean {

    private static void debug(final String s) {
        System.out.println(s);
    }

    @Inject
    ServiceLocator mHabitat;
    @Inject
    InjectedValues mInjectedValues;
    @Inject
    private MBeanServer mMBeanServer;
    @Inject
    Events mEvents;
    private volatile MBeanTracker mMBeanTracker;

    private static final Logger logger = AMXLoggerInfo.getLogger();

    public static MBeanTrackerMBean getMBeanTracker(final MBeanServer server) {
        return MBeanServerInvocationHandler.newProxyInstance(server, MBeanTrackerMBean.MBEAN_TRACKER_OBJECT_NAME, MBeanTrackerMBean.class, false);
    }


    public AMXStartupService() {
        new StringifierRegistryIniterImpl(StringifierRegistryImpl.DEFAULT);
        new StringifierRegistryIniter(StringifierRegistryImpl.DEFAULT);
    }


    private final class ShutdownListener implements EventListener {

        public void event(EventListener.Event event) {
            if (event.is(EventTypes.SERVER_SHUTDOWN)) {
                shutdown();
            }
        }
    }


    private void shutdown() {
        logger.fine("AMXStartupService: shutting down AMX MBeans");
        unloadAMXMBeans();

        final ObjectName allAMXPattern = AMXUtil.newObjectName(AMXGlassfish.DEFAULT.amxJMXDomain(), "*");
        final Set<ObjectName> remainingAMX = mMBeanServer.queryNames(allAMXPattern, null);
        if (remainingAMX.size() != 0) {
            logger.log(Level.WARNING, AMXLoggerInfo.shutdownNotUnregistered, remainingAMX);
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
            }
        }
        FeatureAvailability.getInstance().deRegisterFeatures();
        logger.log(Level.INFO,"amx.shutdown.unregistered",mMBeanServer.queryNames(allAMXPattern, null));
    }


    public void postConstruct() {
        final TimingDelta delta = new TimingDelta();

        SingletonEnforcer.register(this.getClass(), this);

        if (mMBeanServer == null) {
            throw new Error("AMXStartup: null MBeanServer");
        }

        try {
            // StandardMBean is required because interface and class are in different packages
            final StandardMBean mbean = new StandardMBean(this, AMXStartupServiceMBean.class);
            mMBeanServer.registerMBean(mbean, OBJECT_NAME);

            mMBeanTracker = new MBeanTracker(AMXGlassfish.DEFAULT.amxJMXDomain());

            mMBeanTracker.setEmitMBeanStatus(false);

            //final StandardMBean supportMBean = new StandardMBean(mMBeanTracker, MBeanTrackerMBean.class);
            mMBeanServer.registerMBean(mMBeanTracker, MBeanTrackerMBean.MBEAN_TRACKER_OBJECT_NAME);
        } catch (final Exception e) {
            logger.log(Level.INFO, "amx.fatal.error", e);
            throw new Error(e);
        }
        //debug( "AMXStartupService.postConstruct(): registered: " + OBJECT_NAME );
        logger.log(Level.INFO,"amx.startupService",new Object[] {delta.elapsedMillis(),OBJECT_NAME});

        mEvents.register(new ShutdownListener());
    }


    public void preDestroy() {
        logger.log(Level.INFO,"amx.preDestroy");
        unloadAMXMBeans();
    }


    public JMXServiceURL[] getJMXServiceURLs() {
        try {
            return (JMXServiceURL[]) mMBeanServer.getAttribute(AMXGlassfish.DEFAULT.getBootAMXMBeanObjectName(), "JMXServiceURLs");
        } catch (final JMException e) {
            throw new RuntimeException(e);
        }
    }


    /**
    Return a proxy to the AMXStartupService.
     */
    public static AMXStartupServiceMBean getAMXStartupServiceMBeanProxy(final MBeanServer mbs) {
        AMXStartupServiceMBean ss = null;

        if (mbs.isRegistered(OBJECT_NAME)) {
            ss = AMXStartupServiceMBean.class.cast(
                    MBeanServerInvocationHandler.newProxyInstance(mbs, OBJECT_NAME, AMXStartupServiceMBean.class, false));
        }
        return ss;
    }


    public synchronized ObjectName getDomainRoot() {
        try {
            // might not be ready yet
            return getDomainRootProxy().extra().objectName();
        } catch (Exception e) {
            // not there
        }
        return null;
    }


    DomainRoot getDomainRootProxy() {
        return ProxyFactory.getInstance(mMBeanServer).getDomainRootProxy(false);
    }


    public ObjectName loadAMXMBeans() {
        ObjectName objectName = AMXGlassfish.DEFAULT.domainRoot();
        if (!mMBeanServer.isRegistered(objectName)) {
            try {
                objectName = _loadAMXMBeans();
            } catch (final Exception e) {
                logger.log(Level.SEVERE,"amx.error.loadAMXBeans",e);
                throw new RuntimeException(e);
            }
        }
        return objectName;
    }

    /** also works as a loaded/not loaded flag: null if not yet loaded */
    private volatile ObjectName DOMAIN_ROOT_OBJECTNAME = null;

    private synchronized ObjectName loadDomainRoot() {        
        if (DOMAIN_ROOT_OBJECTNAME != null) {
            return DOMAIN_ROOT_OBJECTNAME;
        }

        final DomainRootImpl domainRoot = new DomainRootImpl();
        DOMAIN_ROOT_OBJECTNAME = AMXGlassfish.DEFAULT.domainRoot();
        try {
            DOMAIN_ROOT_OBJECTNAME = mMBeanServer.registerMBean(domainRoot, DOMAIN_ROOT_OBJECTNAME).getObjectName();
            loadSystemInfo();
        } catch (final Exception e) {
            final Throwable rootCause = ExceptionUtil.getRootCause(e);
            logger.log(Level.INFO, "amx.error.load.DomainRoot", rootCause);
            throw new RuntimeException(rootCause);
        }

        return DOMAIN_ROOT_OBJECTNAME;
    }


    protected final ObjectName loadSystemInfo()
            throws NotCompliantMBeanException, MBeanRegistrationException,
            InstanceAlreadyExistsException {
        final SystemInfoImpl systemInfo = SystemInfoFactory.createInstance(mMBeanServer);

        ObjectName systemInfoObjectName =
                ObjectNameBuilder.buildChildObjectName(mMBeanServer, DOMAIN_ROOT_OBJECTNAME, SystemInfo.class);
        
        systemInfoObjectName = mMBeanServer.registerMBean(systemInfo, systemInfoObjectName).getObjectName();
        
        return systemInfoObjectName;
    }


    /** run each AMXLoader in its own thread */
    private static final class AMXLoaderThread extends Thread {

        private final AMXLoader mLoader;
        private volatile ObjectName mTop;
        private final CountDownLatch mLatch;

        public AMXLoaderThread(final AMXLoader loader) {
            mLoader = loader;
            mLatch = new CountDownLatch(1);
        }

        public void run() {
            try {
                logger.fine("AMXStartupServiceNew.AMXLoaderThread: loading: " + mLoader.getClass().getName());
                mTop = mLoader.loadAMXMBeans();
            } catch (final Exception e) {
                logger.log(Level.INFO, AMXLoggerInfo.failToLoad, e);
            } finally {
                mLatch.countDown();
            }
        }

        public ObjectName waitDone() {
            try {
                mLatch.await();
            } catch (InterruptedException e) {
            }
            return mTop;
        }

        public ObjectName top() {
            return mTop;
        }
    }


    class MyListener extends MBeanListener.CallbackImpl {

        @Override
        public void mbeanRegistered(final ObjectName objectName, final MBeanListener listener) {
            super.mbeanRegistered(objectName, listener);
            // verification code, nothing more to do
            //debug( "MBean registered: " + objectName );
        }
    }


    public synchronized ObjectName _loadAMXMBeans() {
        // self-check important MBeans
        final AMXGlassfish amxg = AMXGlassfish.DEFAULT;
        final MBeanListener<MyListener> bootAMXListener = amxg.listenForBootAMX(mMBeanServer, new MyListener());

        final MBeanListener<MyListener> domainRootListener = amxg.listenForDomainRoot(mMBeanServer, new MyListener());

        // loads the high-level AMX MBeans, like DomainRoot, QueryMgr, etc
        loadDomainRoot();
        FeatureAvailability.getInstance().registerFeature(FeatureAvailability.AMX_CORE_READY_FEATURE, getDomainRoot());
        logger.fine("AMXStartupServiceNew: AMX core MBeans are ready for use, DomainRoot = " + getDomainRoot());

        try {
            // Find and load any additional AMX subsystems
            final Collection<AMXLoader> loaders = mHabitat.getAllServices(AMXLoader.class);
            logger.fine( "AMXStartupService._loadAMXMBeans(): found this many loaders: " + loaders.size() );
            final AMXLoaderThread[] threads = new AMXLoaderThread[loaders.size()];
            int i = 0;
            for (final AMXLoader loader : loaders) {
                logger.fine( "AMXStartupService._loadAMXMBeans(): found this many loaders: " + loader);
                threads[i] = new AMXLoaderThread(loader);
                threads[i].start();
                ++i;
            }
            // don't mark AMX ready until all loaders have finished
            for (final AMXLoaderThread thread : threads) {
                thread.waitDone();
            }
        } catch (Throwable t) {
            logger.log(Level.INFO, AMXLoggerInfo.fatalError, t);
        } finally {
            FeatureAvailability.getInstance().registerFeature(FeatureAvailability.AMX_READY_FEATURE, getDomainRoot());
            logger.log(Level.INFO, AMXLoggerInfo.startupServiceDomainRoot, getDomainRoot());
        }

        // sanity-check (self-test) our listeners
        if (bootAMXListener.getCallback().getRegistered() == null) {
            throw new IllegalStateException("BootAMX listener was not called");
        }
        if (domainRootListener.getCallback().getRegistered() == null) {
            throw new IllegalStateException("DomainRoot listener was not called");
        }

        return getDomainRoot();
    }
    

    public synchronized void unloadAMXMBeans() {
        if (getDomainRoot() != null) {
            final Collection<AMXLoader> loaders = mHabitat.getAllServices(AMXLoader.class);
            for (final AMXLoader loader : loaders) {
                if (loader == this) {
                    continue;
                }

                try {
                    loader.unloadAMXMBeans();
                } catch (final Exception e) {
                    logger.log(Level.INFO, AMXLoggerInfo.failToUnLoad, e);
                }
            }

            ImplUtil.unregisterAMXMBeans(getDomainRootProxy());
            // Need to set this to null for making this work in the same VM.
            this.DOMAIN_ROOT_OBJECTNAME = null;
            // here is where we have to reset the singletons
            ComplianceMonitor.removeInstance();
            SystemInfoFactory.removeInstance();
        }
    }
    // public Startup.Lifecycle getLifecycle() { return Startup.Lifecycle.SERVER; }
}










