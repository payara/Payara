/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.amx.impl.config;

import com.sun.enterprise.config.serverbeans.Domain;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.management.*;
import org.glassfish.admin.amx.config.AMXConfigConstants;
import org.glassfish.admin.amx.core.Util;
import org.glassfish.admin.amx.impl.util.ImplUtil;
import org.glassfish.admin.amx.impl.util.InjectedValues;
import org.glassfish.admin.amx.impl.util.ObjectNameBuilder;
import org.glassfish.admin.amx.impl.util.SingletonEnforcer;
import org.glassfish.admin.amx.util.AMXLoggerInfo;
import org.glassfish.admin.amx.util.ExceptionUtil;
import org.glassfish.admin.amx.util.FeatureAvailability;
import org.glassfish.admin.amx.util.MapUtil;
import org.glassfish.admin.amx.util.TypeCast;
import org.glassfish.admin.mbeanserver.PendingConfigBeanJob;
import org.glassfish.admin.mbeanserver.PendingConfigBeans;
import org.glassfish.external.amx.AMXGlassfish;
import org.glassfish.external.arc.Stability;
import org.glassfish.external.arc.Taxonomy;
import org.jvnet.hk2.config.*;

/**
Responsible for loading ConfigBeanProxy MBeans (com.sun.enterprise.config.serverbeans.*)
 * @author llc
 */
@Taxonomy(stability = Stability.NOT_AN_INTERFACE)
public final class AMXConfigLoader 
        implements TransactionListener {

    private static void debug(final String s) {
        System.out.println(s);
    }
    private volatile AMXConfigLoaderThread mLoaderThread;
    private final Transactions mTransactions;
    private final Logger mLogger = AMXLoggerInfo.getLogger();
    private final PendingConfigBeans mPendingConfigBeans;
    private final ConfigBeanRegistry mRegistry = ConfigBeanRegistry.getInstance();
    private final MBeanServer mServer;
    
    /**
     * Detects illegal characters in MBean name values.  (Used instead of
     * individual character searches for efficiency)
     */
    private static final Pattern ILLEGAL_JMX_NAME_PATTERN = Pattern.compile(".*[=:"
            + Pattern.quote("\"") + Pattern.quote("*") + Pattern.quote("?") + "].*");
    
    public AMXConfigLoader(
            final MBeanServer mbeanServer,
            final PendingConfigBeans pending,
            final Transactions transactions) {
        if (transactions == null) {
            throw new IllegalStateException("AMXConfigLoader.AMXConfigLoader: null Transactions");
        }

        mServer = mbeanServer;
        mTransactions = transactions;
        mPendingConfigBeans = pending;
    }

    public void registerConfigured(final Class<? extends ConfigBeanProxy> intf) {
        // cache it
        ConfigBeanJMXSupportRegistry.getInstance(intf);
    }

    public Map<String, String> getConfiguredTypes() {
        final List<Class<? extends ConfigBeanProxy>> classes = ConfigBeanJMXSupportRegistry.getConfiguredClasses();
        final Map<String, String> types = MapUtil.newMap();

        for (final Class<? extends ConfigBeanProxy> clazz : classes) {
            final String classname = clazz.getName();
            final String elementType = Util.typeFromName(classname);
            types.put(elementType, classname);
        }
        return types;
    }

    private void configBeanRemoved(final ConfigBean cb) {
        final ObjectName objectName = mRegistry.getObjectName(cb);
        if (objectName != null) {
            ImplUtil.unregisterAMXMBeans(mServer, objectName);
            mRegistry.remove(objectName);
        } else {
            // might or might not be there, but make sure it's gone!
            mPendingConfigBeans.remove(cb);
        }
    }

    private void issueAttributeChange(
            final ConfigBean cb,
            final String xmlAttrName,
            final Object oldValue,
            final Object newValue,
            final long whenChanged) {
        final ObjectName objectName = mRegistry.getObjectName(cb);
        if (objectName == null) {
            throw new IllegalArgumentException("Can't issue attribute change for null ObjectName for ConfigBean " + cb.getProxyType().getName());
        }

        boolean changed = false;
        if (oldValue != null) {
            changed = !oldValue.equals(newValue);
        } else if (newValue != null) {
            changed = true;
        }

        if (changed) {
            //debug( "AMXConfigLoader.issueAttributeChange(): " + xmlAttrName + ": {" + oldValue + " => " + newValue + "}");
            final Object impl = mRegistry.getImpl(cb);
            if (!(impl instanceof AMXConfigImpl)) {
                throw new IllegalStateException("impossible");
            }
            final AMXConfigImpl amx = (AMXConfigImpl) impl;
            final String message = cb.getProxyType().getName() + "." + xmlAttrName + ": " + oldValue + " => " + newValue;
            amx.issueAttributeChangeForXmlAttrName(xmlAttrName, message, oldValue, newValue, whenChanged);
        }
    }

    private void sortAndDispatch(
            final List<PropertyChangeEvent> events,
            final long whenChanged) {
        //debug( "AMXConfigLoader.sortAndDispatch: " + events.size() + " events" );
        final List<ConfigBean> newConfigBeans = new ArrayList<ConfigBean>();
        final List<PropertyChangeEvent> remainingEvents = new ArrayList<PropertyChangeEvent>();

        //
        // Process all ADD and REMOVE events first, placing leftovers into 'remainingEvents'
        // We do this even if AMX is *not* running, because they new ConfigBeans need to go
        // into the queue for when and if AMX starts running.
        // 
        for (final PropertyChangeEvent event : events) {
            final Object oldValue = event.getOldValue();
            final Object newValue = event.getNewValue();
            
            if (oldValue == null && newValue instanceof ConfigBeanProxy) {
                // ADD: a new ConfigBean was added
                final ConfigBeanProxy cbp = (ConfigBeanProxy) newValue;
                final ConfigBean cb = asConfigBean(ConfigBean.unwrap(cbp));
                final boolean doWait = amxIsRunning();
                if (handleConfigBean(cb, doWait)) // wait until registered
                {
                    newConfigBeans.add(cb);
                }
            } else if (newValue == null && oldValue instanceof ConfigBeanProxy && amxIsRunning()) {
                // REMOVE
                final ConfigBeanProxy cbp = (ConfigBeanProxy) oldValue;
                final ConfigBean cb = asConfigBean(ConfigBean.unwrap(cbp));
                //debug( "AMXConfigLoader.sortAndDispatch: remove (recursive) ConfigBean: " + mRegistry.getObjectName(cb) );
                configBeanRemoved(cb);
            } else {
                remainingEvents.add(event);
            }
        }

        // we can't issue events if AMX is not running!
        if (amxIsRunning()) {
            for (final PropertyChangeEvent event : remainingEvents) {
                final Object oldValue = event.getOldValue();
                final Object newValue = event.getNewValue();
                final Object source = event.getSource();
                final String propertyName = event.getPropertyName();
                //final String sourceString = (source instanceof ConfigBeanProxy) ? ConfigSupport.proxyType((ConfigBeanProxy)source).getName() : "" + source;

                //debug( "AMXConfigLoader.sortAndDispatch (ATTR change): name = " + propertyName +
                //        ", oldValue = " + oldValue + ", newValue = " + newValue + ", source = " + sourceString );
                if (source instanceof ConfigBeanProxy) {
                    // CHANGE
                    final ConfigBeanProxy cbp = (ConfigBeanProxy) source;
                    final ConfigBean cb = asConfigBean(ConfigBean.unwrap(cbp));
                    //final Class<? extends ConfigBeanProxy> proxyClass = ConfigSupport.proxyType(cbp);

                    // change events without prior add
                    // we shouldn't have to check for this, but it's a bug in the caller: no even for
                    // new ConfigBean, but changes come along anyway

                    if (mRegistry.getObjectName(cb) == null) {
                        if (!newConfigBeans.contains(cb)) {
                            //debug( "AMXConfigLoader.sortAndDispatch: process new ConfigBean (WORKAROUND): " + proxyClass.getNameProp() );
                            if (handleConfigBean(cb, false)) {
                                newConfigBeans.add(cb);
                            }
                        }
                    } else {
                        issueAttributeChange(cb, propertyName, oldValue, newValue, whenChanged);
                    }
                } else {
                    debug("AMXConfigLoader.sortAndDispatch: WARNING: source is not a ConfigBean");
                }
            }
        }
    }

    public void transactionCommited(final List<PropertyChangeEvent> changes) {
        //final PropertyChangeEvent[] changesArray = new PropertyChangeEvent[changes.size()];
        //changes.toArray( changesArray );
        sortAndDispatch(changes, System.currentTimeMillis());
    }

    public void unprocessedTransactedEvents(List<UnprocessedChangeEvents> changes) {
        // not interested...
    }

    public void handleNotification(final Notification notif, final Object handback) {
    }

    public void stop() {
        mTransactions.removeTransactionsListener(this);
        SingletonEnforcer.deregister(AMXConfigLoader.class, this);
    }

    /**
    No items will be processd until {@link #start} is called.
     */
    boolean handleConfigBean(final ConfigBean cb, final boolean waitDone) {
        boolean processed = true;

        if (mRegistry.getObjectName(cb) == null) {
            final PendingConfigBeanJob job = mPendingConfigBeans.add(cb, waitDone);

            // a job could come back null for a bogus ConfigBean
            if (job == null) {
                mLogger.log(Level.INFO, AMXLoggerInfo.configBeanNotProcessed, cb.getProxyType().getName());
                processed = false;
            } else if (waitDone) {
                try {
                    job.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } else {
            // ok
        }
        return processed;
    }

    /**
     */
    private ConfigBean getActualParent(final ConfigBean configBean) {
        ConfigBean parent = asConfigBean(configBean.parent());
        if (parent == null) {
            if (!configBean.getProxyType().getName().endsWith("Domain")) {
                throw new IllegalStateException("WARNING: parent is null for " + configBean.getProxyType().getName() + ",  see issue #10528");
            }
        }

        return parent;
    }

    private ObjectName getActualParentObjectName(final ConfigBean configBean) {
        ObjectName parentObjectName = null;

        final ConfigBean parent = getActualParent(configBean);
        if (parent != null) {
            parentObjectName = mRegistry.getObjectName(parent);
        }

        return parentObjectName;
    }

    /**
    @return a ConfigBean, or null if it's not a ConfigBean
     */
    @SuppressWarnings("unchecked")
    static ConfigBean asConfigBean(final Object o) {
        if (o == null) {
            return null;
        }

        if (!(o instanceof ConfigBean)) {
            throw new IllegalArgumentException("Not a ConfigBean: " + o.getClass().getName());
        }

        return (ConfigBean) o;
    }

    /**
    Enable registration of MBeans, queued until now.
     */
    public synchronized ObjectName start() {
        mLogger.log(Level.INFO,AMXLoggerInfo.inAMXConfigLoader, mLoaderThread );
        if (mLoaderThread == null) {
            FeatureAvailability.getInstance().waitForFeature(FeatureAvailability.AMX_CORE_READY_FEATURE, "AMXConfigLoader.start");

            mLoaderThread = new AMXConfigLoaderThread(mPendingConfigBeans);
            mLoaderThread.setDaemon(true);
            mLoaderThread.start();

            mPendingConfigBeans.swapTransactionListener(this);
            SingletonEnforcer.register(AMXConfigLoader.class, this);

            // wait until config beans have been loaded as MBeans
            mLoaderThread.waitInitialQueue();

            // Now the Config subsystem is ready: after the first queue of ConfigBeans are registered as MBeans
            // and after the above MBeans are registered.
            final ObjectName domainObjectName = ConfigBeanRegistry.getInstance().getObjectNameForProxy(getDomain());
            mLogger.log(Level.INFO,"amx.domain.config.registered", domainObjectName);
            FeatureAvailability.getInstance().registerFeature(AMXConfigConstants.AMX_CONFIG_READY_FEATURE, domainObjectName);
        }
        return null;
    }

    private synchronized boolean amxIsRunning() {
        return mLoaderThread != null;
    }

    private final class AMXConfigLoaderThread extends Thread {

        private final PendingConfigBeans mPending;
        volatile boolean mQuit = false;
        private volatile CountDownLatch mInitalQueueLatch = new CountDownLatch(1);

        AMXConfigLoaderThread(final PendingConfigBeans pending) {
            super("AMXConfigLoader.AMXConfigLoaderThread");
            mPending = pending;
        }

        void quit() {
            mQuit = true;
        }

        private ObjectName registerOne(final PendingConfigBeanJob job) {
            final ConfigBean cb = job.getConfigBean();

            ObjectName objectName = mRegistry.getObjectName(cb);
            try {
                // If the ObjectName is null, then it hasn't been registered
                // Due to recursive registration of parents, we could encounter beans
                // that are parents, and thus already registered.
                if (objectName == null) {
                    objectName = registerConfigBeanAsMBean(cb);
                    //debug( "AMXConfigLoaderThread.registerOne(): " + objectName);
                }
            } catch (final Throwable t) {
                mLogger.log(Level.WARNING, AMXLoggerInfo.cantRegister, new Object[]{getType(cb), getKey(cb), t});
            } finally {
                job.releaseLatch();
            }

            return objectName;
        }

        public void run() {
            try {
                doRun();
            } catch (final Throwable t) {
                mLogger.log(Level.SEVERE, AMXLoggerInfo.unexpectedDeath, t);
            }
        }

        /** wait until the initial queue of MBeans has been processed */
        public void waitInitialQueue() {
            final CountDownLatch latch = mInitalQueueLatch;
            if (latch != null) {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                mInitalQueueLatch = null;
            }
        }

        protected void doRun() throws Exception {
            /*
            First pass *only*:
            Note when we initially empty the queue; this signifies that
            AMX is "ready" for callers that just started it.
             */
            PendingConfigBeanJob job = mPending.take();  // block until first item is ready
            while ((!mQuit) && job != null) {
                registerOne(job);
                job = mPending.peek();  // don't block, loop exits when queue is first emptied
                if (job != null) {
                    job = mPending.take();
                }
            }
            mInitalQueueLatch.countDown();

            // ongoing processing once initial queue has been emptied: blocking behavior
            while (!mQuit) {
                job = mPending.take();
                registerOne(job);
            }
        }
    }

    /**
    Register the ConfigBean, first registering its parent, parent's parent, etc if not
    already present.
     */
    private ObjectName registerConfigBeanAsMBean(final ConfigBean cb) {
        ObjectName objectName = null;

        //debug( "registerConfigBeanAsMBean: " + cb.getProxyType().getName()  + ", object = " + cb );
        final ConfigBean parentCB = getActualParent(cb);
        if (parentCB != null && mRegistry.getObjectName(parentCB) == null) {
            //debug( "REGISTER parent first: " + parentCB.getProxyType().getName() );
            registerConfigBeanAsMBean(parentCB);
            //debug( "REGISTERED parent: " + parentCB.getProxyType().getName() + " as " + JMXUtil.toString(parentCB.getObjectName()) );
        }
        objectName = _registerConfigBeanAsMBean(cb, parentCB);
        assert objectName == null || mRegistry.getObjectName(cb) != null;
        return objectName;
    }

    /**
    Parent must have been registered already.
     */
    private ObjectName _registerConfigBeanAsMBean(
            final ConfigBean cb,
            final ConfigBean parentCB) {
        final Class<? extends ConfigBeanProxy> cbClass = cb.getProxyType();

        //debug( "_registerConfigBeanAsMBean: " + cb.getProxyType().getName() );

        ObjectName objectName = mRegistry.getObjectName(cb);
        if (objectName != null) {
            throw new IllegalArgumentException("ConfigBean " + cbClass.getName() + " already registered as " + objectName);
        }
        if (parentCB != null && mRegistry.getObjectName(parentCB) == null) {
            throw new IllegalArgumentException("ConfigBean parent " + parentCB.getProxyType().getName()
                    + " must be registered first before child = " + cbClass.getName());
        }

        // debug( "Preparing ConfigBean for registration with ObjectNameInfo = " + objectNameInfo.toString() + ", AMXMBeanMetaData = " + metadata );

        objectName = buildObjectName(cb);

        objectName = createAndRegister(cb, objectName);
        if (objectName != null) {
            mLogger.fine("REGISTERED MBEAN: " + objectName);
        }

        return objectName;
    }

    private ObjectName createAndRegister(
            final ConfigBean cb,
            final ObjectName objectNameIn) {
        ObjectName objectName;

        ObjectName parentObjectName = getActualParentObjectName(cb);

        if (parentObjectName == null) {
            parentObjectName = AMXGlassfish.DEFAULT.domainRoot();
        }

        final AMXConfigImpl impl = new AMXConfigImpl(parentObjectName, cb);

        try {
            final ObjectInstance instance = mServer.registerMBean(impl, objectNameIn);
            objectName = instance.getObjectName();
            mRegistry.add(cb, objectName, impl);

            //System.out.println( "AMXConfigLoader.createAndRegister(): REGISTERED: " + objectName + " at " + System.currentTimeMillis() );
            //System.out.println( JMXUtil.toString( mServer.getMBeanInfo(objectName) ) );
        } catch (final JMException e) {
            debug(ExceptionUtil.toString(e));

            objectName = null;
        }
        return objectName;
    }

    private String getType(final ConfigBean cb) {
        final ConfigBeanJMXSupport spt = ConfigBeanJMXSupportRegistry.getInstance(cb);
        return spt.getTypeString();
    }

    /** Get the key value eg the name to be used in an ObjectName */
    static String getKey(final ConfigBean cb) {
        final ConfigBeanJMXSupport spt = ConfigBeanJMXSupportRegistry.getInstance(cb);

        if (spt.isSingleton()) {
            return null;
        }

        String name = null;

        final String nameHint = spt.getNameHint();

        if (nameHint == null) {
            name = "MISSING_NAME__KEY_MUST_BE_SPECIFIED_IN_INTERFACE";
        } else if (spt.nameHintIsElement()) {
            final List<?> leaf = cb.leafElements(nameHint);
            if (leaf != null) {
                // verify that it is List<String> -- no other types are supported in this way
                final List<String> items = TypeCast.checkList(leaf, String.class);
                if (items.size() != 1) {
                    throw new IllegalArgumentException("Can't find sub-element of type " + nameHint + " in " + cb.getProxyType().getName());
                }
                name = items.get(0);
            }
        } else {
            name = cb.rawAttribute(nameHint);
        }

        return name;
    }

    public Domain getDomain() {
    	return InjectedValues.getInstance().getHabitat().getService(Domain.class);
    }

    private static final AtomicLong sCounter = new AtomicLong(1);

    private ObjectName buildObjectName(final ConfigBean cb) {
        ObjectName parentObjectName;
        final ConfigBean parent = getActualParent(cb);

        if (parent == null) {
            parentObjectName = AMXGlassfish.DEFAULT.domainRoot();
        } else {
            parentObjectName = mRegistry.getObjectName(parent);
        }

        final String type = getType(cb);
        String name = getKey(cb);

        final ConfigBeanJMXSupport spt = ConfigBeanJMXSupportRegistry.getInstance(cb);
        if ((!spt.isSingleton()) && (name == null || name.length() == 0)) {
            name = "MISSING_NAME-" + sCounter.getAndIncrement();
            mLogger.log(Level.WARNING, AMXLoggerInfo.nonsingletonConfigbean, new Object[] {cb.getProxyType().getName() ,name});
        }

        //debug( "Type/name for " + cb.getProxyType().getName() + " = " + type + " = " + name );

        final ObjectName objectName = ObjectNameBuilder.buildChildObjectName(mServer, parentObjectName, type, quoteIfNeeded(name));

        //debug( "ObjectName for " + cb.getProxyType().getName() + " = " + objectName + " of parent " + parentObjectName );

        return objectName;
    }
    
    /**
     * Quotes the name string if it contains any characters that are illegal
     * in MBean names.
     * 
     * @param name the string to examine
     * @return quoted string if it contains illegal characters; the string otherwise
     */
    private static String quoteIfNeeded(final String name) {
        /*
         * JMX names cannot include =  or , or : or * or ? unless they are part of
         * the value and they are quoted.
         */
        
        if (name != null && ILLEGAL_JMX_NAME_PATTERN.matcher(name).matches()) {
            return "\"" + name + "\"";
        } else {
            return name;
        }
    }
}
