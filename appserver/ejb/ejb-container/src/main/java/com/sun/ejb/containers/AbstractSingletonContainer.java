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
 */
// Portions Copyright [2016-2023] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.cp.lock.FencedLock;
import com.hazelcast.map.IMap;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.Container;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.containers.util.pool.ObjectFactory;
import com.sun.ejb.monitoring.stats.EjbMonitoringStatsProvider;
import com.sun.ejb.monitoring.stats.SingletonBeanStatsProvider;
import com.sun.enterprise.admin.monitor.callflow.ComponentType;
import com.sun.enterprise.container.common.spi.ClusteredSingletonLookup;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.util.Utility;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBException;
import jakarta.ejb.NoSuchEJBException;
import jakarta.ejb.NoSuchObjectLocalException;
import jakarta.ejb.RemoveException;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.startup.SingletonLifeCycleManager;


public abstract class AbstractSingletonContainer extends BaseContainer {

    private static final byte[] singletonInstanceKey = {0, 0, 0, 1};

    // All Singleton EJBs have the same instanceKey
    //  Note: the first byte of instanceKey must be left empty.

    // Note : Singletons do not support the legacy EJB 2.x RemoteHome/LocalHome
    //        client views.


    private EJBLocalObjectImpl localBusinessObject;
    private EJBLocalObjectImpl optionalLocalBusinessObject;


    // Data members for Remote business view. Any objects representing the
    // Remote business interface are not subtypes of EJBObject.
    private EJBObjectImpl remoteBusinessObject;
    private final Map<String, java.rmi.Remote> remoteBusinessStubs = new HashMap<>();

    // Information about a web service ejb endpoint.  Used as a conduit
    // between webservice runtime and ejb container.  Contains a Remote
    // servant used by jaxrpc to call web service business method.
    //TODO private EjbRuntimeEndpointInfo webServiceEndpoint;

    protected ObjectFactory singletonCtxFactory;

    private SingletonLifeCycleManager lcm;

    protected AtomicBoolean singletonInitialized = new AtomicBoolean(false);

    // used to protect against synchronous loopback calls during Singleton init
    private boolean initializationInProgress;

    // Set to true if Singleton failed to complete its initialization successfully.
    // If true, Singleton is not accessible.
    protected boolean singletonInitializationFailed;

    protected volatile ComponentContext singletonCtx;

    private final InvocationInfo postConstructInvInfo;
    private final InvocationInfo preDestroyInvInfo;

    protected final ClusteredSingletonLookup clusteredLookup =
        new ClusteredSingletonLookupImpl(ejbDescriptor, componentId);

    protected FencedLock clusteredSingletonLock;

    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     * @exception Exception on error
     */

    protected AbstractSingletonContainer(EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
        throws Exception {
        super(ContainerType.SINGLETON, desc, loader, sm);

        super.createCallFlowAgent(ComponentType.SLSB);

        // Tx attribute for PostConstruct/PreDestroy methods can only be specified using
        // a PostConstruct/PreDestroy method defined on the bean class.  If nothing is
        // specified, the CMT default is for the method to run within a transaction.  We
        // actually use TX_REQUIRES_NEW to force the transaction manager to always suspend
        // any existing transaction in the case that the Singleton instance is initialized
        // lazily as a side effect of an invocation.   Like timeout methods, from the
        // developer's perspective there is never an inflowing transaction to a Singleton
        // PostConstruct or PreDestroy method.

        postConstructInvInfo = new InvocationInfo();
        postConstructInvInfo.ejbName = ejbDescriptor.getName();
        postConstructInvInfo.methodIntf = MethodDescriptor.LIFECYCLE_CALLBACK;
        postConstructInvInfo.txAttr = getTxAttrForLifecycleCallback(ejbDescriptor.getPostConstructDescriptors());

        preDestroyInvInfo = new InvocationInfo();
        preDestroyInvInfo.ejbName = ejbDescriptor.getName();
        preDestroyInvInfo.methodIntf = MethodDescriptor.LIFECYCLE_CALLBACK;
        preDestroyInvInfo.txAttr = getTxAttrForLifecycleCallback(ejbDescriptor.getPreDestroyDescriptors());
    }

    public String getMonitorAttributeValues() {
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("SINGLETON ").append(ejbDescriptor.getName());
        sbuf.append("]");

        return sbuf.toString();
    }

    @Override
    protected EjbInvocation createEjbInvocation(Object ejb, ComponentContext ctx) {
        EjbInvocation inv = super.createEjbInvocation(ejb, ctx);
        setResourceHandler(inv);

        return inv;
    }

    @Override
    protected EjbInvocation createEjbInvocation() {
        EjbInvocation inv = super.createEjbInvocation();
        setResourceHandler(inv);

        return inv;
    }

    @Override
    protected ComponentContext _getContext(EjbInvocation invocation) throws EJBException {
        // initialize, serialize the Singleton and set to the session
        checkInit();
        if (clusteredLookup.isClusteredEnabled()) {
            AbstractSessionContextImpl sessionContext = (AbstractSessionContextImpl) singletonCtx;
            try {
                invocationManager.preInvoke(invocation);
                invocation.context = sessionContext;

                if (_logger.isLoggable(Level.FINE)) {
                    // Log all operations on the lock - note this Should™ also log the call to unlock in releaseContext
                    clusteredSingletonLock = (FencedLock) Proxy.newProxyInstance(loader, new Class<?>[]{FencedLock.class},
                            (proxy, method, args) -> {
                                FencedLock fencedLock = clusteredLookup.getDistributedLock();
                                _logger.log(
                                        Level.FINE,
                                        "DistributedLock, about to call {0}, Locked: {1}, Locked by Us: {2}, thread ID {3}",
                                        new Object[] {
                                                method.getName(),
                                                fencedLock.isLocked(),
                                                fencedLock.isLockedByCurrentThread(),
                                                Thread.currentThread().getId()});
                                Object rv = method.invoke(fencedLock, args);
                                _logger.log(
                                        Level.FINE,
                                        "DistributedLock, after call to {0}, Locked: {1}, Locked by Us: {2}, thread ID {3}",
                                        new Object[] {
                                                method.getName(),
                                                fencedLock.isLocked(),
                                                fencedLock.isLockedByCurrentThread(),
                                                Thread.currentThread().getId()});
                                return rv;
                    });
                } else {
                    clusteredSingletonLock = clusteredLookup.getDistributedLock();
                }

                /**
                 * Look up the clustered singleton and set it in the session context. Note that if this is an instance
                 * of {@link CMCSingletonContainer} the
                 * {@link CMCSingletonContainer#getClusteredSingleton(EjbInvocation)} will (by default)
                 * guard concurrent access via {@link java.util.concurrent.locks.Lock Locks}.
                 *
                 * This is done here so that when multiple concurrent threads are queued up to execute they will
                 * lock around the read & write of the EJB itself, rather than the method call.
                 */
                sessionContext.setEJB(getClusteredSingleton(invocation));

                if (isJCDIEnabled()) {
                    if (sessionContext.getJCDIInjectionContext() != null) {
                        sessionContext.getJCDIInjectionContext().cleanup(false);
                    }
                    sessionContext.setJCDIInjectionContext(_createJCDIInjectionContext(sessionContext, sessionContext.getEJB()));
                }
                if (sessionContext.getEJB() != null) {
                    injectEjbInstance(sessionContext);
                }
            } catch (Exception ex) {
                throw new EJBException(ex);
            } finally {
                invocation.context = null;
                invocationManager.postInvoke(invocation);
            }
        }
        return singletonCtx;
    }

    /**
     * Get the clustered singleton for this container.
     *
     * This method does not provide any concurrent access guards, but may be overridden to do so.
     *
     * @param invocation The {@link EjbInvocation} that prompted the lookup
     * @return The clustered singleton object
     */
    protected Object getClusteredSingleton(EjbInvocation invocation) {
        return clusteredLookup.getClusteredSingletonMap().get(clusteredLookup.getClusteredSessionKey());
    }

    @Override
    protected void releaseContext(EjbInvocation inv) throws EJBException {
        if (clusteredLookup.isClusteredEnabled()) {
            try {
                invocationManager.preInvoke(inv);
                if (clusteredLookup.getClusteredSingletonMap().containsKey(clusteredLookup.getClusteredSessionKey())) {
                    // serializes the Singleton into Hazelcast
                    clusteredLookup.getClusteredSingletonMap().set(clusteredLookup.getClusteredSessionKey(), inv.context.getEJB());
                }
            }
            finally {
                invocationManager.postInvoke(inv);
            }
        }
    }

    private void setResourceHandler(EjbInvocation inv) {
        // Singletons can not store the underlying resource list
        // in the context impl since that is shared across many
        // concurrent invocations.  Instead, set the resource
        // handler on the invocation to provide a different
        // resource List for each Singleton invocation.
        inv.setResourceHandler(SimpleEjbResourceHandlerImpl.getResourceHandler(transactionManager));
    }

    @Override
    protected void initializeHome() throws Exception {
        super.initializeHome();

        if (isRemote) {
            if (hasRemoteBusinessView) {
                remoteBusinessObject = instantiateRemoteBusinessObjectImpl();
                for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {
                    java.rmi.Remote stub = next.referenceFactory.createRemoteReference(singletonInstanceKey);
                    remoteBusinessStubs.put(next.generatedRemoteIntf.getName(), stub);
                    remoteBusinessObject.setStub(next.generatedRemoteIntf.getName(), stub);
                }
            }
        }

        if (isLocal) {
            if (hasLocalBusinessView) {
                localBusinessObject = instantiateEJBLocalBusinessObjectImpl();
            }
            if (hasOptionalLocalBusinessView) {
                optionalLocalBusinessObject = instantiateOptionalEJBLocalBusinessObjectImpl();
            }
        }

        createBeanPool();
        registerMonitorableComponents();
    }


    private void createBeanPool() {
        this.singletonCtxFactory = new SingletonContextFactory();
    }

    private int getTxAttrForLifecycleCallback(Set<LifecycleCallbackDescriptor> lifecycleCallbackDescriptors)
        throws Exception {
        return getTxAttrForLifecycleCallback(lifecycleCallbackDescriptors, Container.TX_REQUIRES_NEW,
            Container.TX_NOT_SUPPORTED);
    }

    @Override
    protected void registerMonitorableComponents() {
        super.registerMonitorableComponents();
        _logger.log(Level.FINE, "[Singleton Container] registered monitorable");
    }

    @Override
    protected EjbMonitoringStatsProvider getMonitoringStatsProvider(String appName, String modName, String ejbName) {
        return new SingletonBeanStatsProvider(getContainerId(), appName, modName, ejbName);
    }

    @Override
    public void onReady() {
    }

    @Override
    public EJBObjectImpl createRemoteBusinessObjectImpl() throws CreateException, RemoteException {
        // No access check since this is an internal operation.

        ejbProbeNotifier.ejbBeanCreatedEvent( //
            getContainerId(), containerInfo.appName, containerInfo.modName, containerInfo.ejbName);
        return remoteBusinessObject;
    }

    @Override
    public EJBObjectImpl createEJBObjectImpl() throws CreateException, RemoteException {
        throw new CreateException("EJB 2.x Remote view not supported on Singletons");
    }

    /**
     * Called during client creation request through EJB LocalHome view.
     */
    @Override
    public EJBLocalObjectImpl createEJBLocalObjectImpl() throws CreateException {
        throw new CreateException("EJB 2.x Local view not supported on Singletons");
    }

    /**
     * Called during internal creation of session bean
     */
    @Override
    public EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(boolean localBeanView) throws CreateException {
        // No access checks needed because this is called as a result
        // of an internal creation, not a user-visible create method.
        return localBeanView ? optionalLocalBusinessObject : localBusinessObject;
    }

    /**
     *  Doesn't apply to Singletons
     */
    @Override
    protected void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod, boolean local)
        throws RemoveException, EJBException, RemoteException {
        throw new EJBException("Not applicable to Singletons");
    }

    /**
     * Force destroy the EJB should be a no-op for singletons.
     * After Initialization completes successfully, runtime exceptions
     * during invocations on the Singleton do not result in the instance
     * being destroyed.
     */
    @Override
    protected void forceDestroyBean(EJBContextImpl sc) {
    }

    /**
     * Not applicable to Singletons
     */
    @Override
    protected EJBObjectImpl getEJBObjectImpl(byte[] instanceKey) {
        return null;
    }

    @Override
    EJBObjectImpl getEJBRemoteBusinessObjectImpl(byte[] instanceKey) {
        return remoteBusinessObject;
    }

    /**
     * Not applicable to Singletons
     */
    @Override
    protected EJBLocalObjectImpl getEJBLocalObjectImpl(Object key) {
        return null;
    }

    /**
    * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
    * a local business object reference.
    */
    @Override
    EJBLocalObjectImpl getEJBLocalBusinessObjectImpl(Object key) {
        return localBusinessObject;
    }

    /**
    * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
    * a local business object reference.
    */
    @Override
    EJBLocalObjectImpl getOptionalEJBLocalBusinessObjectImpl(Object key) {
        return optionalLocalBusinessObject;
    }


    public void setSingletonLifeCycleManager(SingletonLifeCycleManager lcm) {
        this.lcm = lcm;
    }

    protected void checkInit() {
        if (singletonInitializationFailed) {
            throw new NoSuchEJBException("Singleton " + ejbDescriptor.getName() + " is unavailable "
                + "because its original initialization failed.");
        }

        if (!singletonInitialized.get()) {
            //Note: NEVER call instantiateSingletonInstance() directly from here
            // The following starts all dependent beans as well
            //
            //Also, it is OK to call the following by concurrent threads
            lcm.initializeSingleton(this);
        }
    }

    // Called from SingletonLifeCycleManager to initialize a Singleton as part of the
    // eager loading and @DependsOn sequence
    public ComponentContext instantiateSingletonInstance() {
        if (!singletonInitialized.get()) {
            synchronized (this) {
                if (!singletonInitialized.get()) {

                    // All other locks must be grabbed first.  This check prevents
                    // synchronous loopback attempts during Singleton PostConstruct
                    if (initializationInProgress) {
                        throw new EJBException("Illegal synchronous loopback call during Singleton "
                            + ejbDescriptor.getName() + " initialization would have resulted in deadlock");
                    }

                    initializationInProgress = true;
                    ClassLoader originalCCL = null;
                    try {
                        // This may be happening on the base container initialization thread
                        // rather than on an invocation thread so set the CCL
                        originalCCL = Utility.setContextClassLoader(loader);

                        //The following may throw exception
                        singletonCtx = (ComponentContext) singletonCtxFactory.create(null);
                        //this allows _getContext() to proceed
                        singletonInitialized.set(true);
                    } finally {
                        if ( originalCCL != null ) {
                            Utility.setContextClassLoader(originalCCL);
                        }
                    }
                }
            }
        }

        return singletonCtx;
    }

    @Override
    protected SingletonContextImpl _constructEJBContextImpl(Object instance) {
        return new SingletonContextImpl(instance, this);
    }

    private EjbInvocation createInvocationAndPreInvoke(EjbInvocation ejbInv, Object ejb, SingletonContextImpl context) {
        // this allows JNDI lookups from setSessionContext, ejbCreate
        if (ejbInv == null) {
            ejbInv = createEjbInvocation(ejb, context);
            invocationManager.preInvoke(ejbInv);
        }
        return ejbInv;
    }

    private SingletonContextImpl createSingletonEJB() throws CreateException {
        EjbInvocation ejbInv = null;
        SingletonContextImpl context;
        Object ejb;

        // Track whether initialization got as far as preInvokeTx.
        // Needed for adequate error handling in the face of an initialization
        // exception.
        boolean initGotToPreInvokeTx = false;
        boolean doPostConstruct = true;

        try {
            String sessionKey = clusteredLookup.getClusteredSessionKey();
            EjbSessionDescriptor sessDesc = (EjbSessionDescriptor)ejbDescriptor;
            if (clusteredLookup.isClusteredEnabled()) {
                IMap<String, Object> singletonMap = clusteredLookup.getClusteredSingletonMap();
                if (!singletonMap.containsKey(sessionKey)) {
                    context = (SingletonContextImpl) createEjbInstanceAndContext();
                    ejb = singletonMap.putIfAbsent(sessionKey, context.getEJB());
                    if ((ejb != null) && (ejb != context.getEJB()) && sessDesc.dontCallPostConstructOnAttach()) {
                        doPostConstruct = false;
                    }
                }
                else {
                    context = _constructEJBContextImpl(singletonMap.get(sessionKey));
                    ejb = context.getEJB();
                    ejbInv = createInvocationAndPreInvoke(ejbInv, ejb, context);
                    createEmptyContextAndInterceptors(context);
                    if (isJCDIEnabled()) {
                        _createJCDIInjectionContext(context, ejb, context.getJCDIInjectionContext());
                    }
                    if (sessDesc.dontCallPostConstructOnAttach()) {
                        doPostConstruct = false;
                    }
                }
                clusteredLookup.getClusteredUsageCount().incrementAndGet();
            }
            else {
                if (sessDesc.isClustered() && !clusteredLookup.getHazelcastCore().isEnabled()) {
                    _logger.log(Level.WARNING, "Clustered Singleton {0} not available - Hazelcast is Disabled", sessionKey);
                }
                // a dummy invocation will be created by the BaseContainer to support
                // possible AroundConstruct interceptors
                context = (SingletonContextImpl) createEjbInstanceAndContext();
                ejb = context.getEJB();
            }

            ejbInv = createInvocationAndPreInvoke(ejbInv, ejb, context);

            // Perform injection right after where setSessionContext
            // would be called.  This is important since injection methods
            // have the same "operations allowed" permissions as
            // setSessionContext.
            injectEjbInstance(context);

            if (isRemote) {
                if (hasRemoteBusinessView) {
                    context.setEJBRemoteBusinessObjectImpl(remoteBusinessObject);
                }
            }
            if (isLocal) {
                if (hasLocalBusinessView) {
                    context.setEJBLocalBusinessObjectImpl(localBusinessObject);
                }
                if (hasOptionalLocalBusinessView) {
                    context.setOptionalEJBLocalBusinessObjectImpl(optionalLocalBusinessObject);
                }
            }

            // Call preInvokeTx directly.  InvocationInfo containing tx
            // attribute must be set prior to calling preInvoke
            ejbInv.transactionAttribute = postConstructInvInfo.txAttr;
            ejbInv.invocationInfo = postConstructInvInfo;
            initGotToPreInvokeTx = true;
            preInvokeTx(ejbInv);

            context.setInstanceKey(singletonInstanceKey);

            if (doPostConstruct) {
                intercept(CallbackType.POST_CONSTRUCT, context);
                // Make sure to update Object stored in Map
                if (clusteredLookup.isClusteredEnabled()) {
                    clusteredLookup.getClusteredSingletonMap().set(sessionKey, context.getEJB());
                }
            }

        } catch (Throwable th) {
            if (ejbInv != null) {
                ejbInv.exception = th;
            }
            singletonInitializationFailed = true;
            CreateException creEx = new CreateException(
                "Initialization failed for Singleton " + ejbDescriptor.getName());
            creEx.initCause(th);
            throw creEx;
        } finally {
            initializationInProgress = false;
            if (ejbInv != null) {
                try {
                    invocationManager.postInvoke(ejbInv);
                    if (initGotToPreInvokeTx) {
                        postInvokeTx(ejbInv);
                    }
                } catch (Exception pie) {
                    if (ejbInv.exception != null) {
                        _logger.log(Level.WARNING, "Exception during Singleton startup postInvoke ", pie);
                    } else {
                        ejbInv.exception = pie;
                        singletonInitializationFailed = true;
                        CreateException creEx = new CreateException(
                            "Initialization failed for Singleton " + ejbDescriptor.getName());
                        creEx.initCause(pie);
                        throw creEx;
                    }
                }
            }
        }

        // Set the state to POOLED after ejbCreate so that
        // EJBContext methods not allowed will throw exceptions
        context.setState(EJBContextImpl.BeanState.POOLED);
        context.touch();
        return context;
    }

    @Override
    protected void doTimerInvocationInit(EjbInvocation inv, Object primaryKey) throws Exception {
        if ( isRemote ) {

            // @@@ Revisit setting ejbObject in invocation.
            // What about if bean doesn't expose a remote or local view?
            // How is inv.ejbObject used in timer invocation ?

            //TODO inv.ejbObject = theEJBObjectImpl;
            inv.isLocal = false;
        } else {
            // inv.ejbObject =
            inv.isLocal = true;
        }
    }

    @Override
    public boolean userTransactionMethodsAllowed(ComponentInvocation inv) {
        boolean utMethodsAllowed = false;
        if ( isBeanManagedTran ) {
            if ( inv instanceof EjbInvocation ) {

                EjbInvocation ejbInv = (EjbInvocation) inv;
                AbstractSessionContextImpl sc = (AbstractSessionContextImpl) ejbInv.context;

                // Allowed any time after dependency injection
                utMethodsAllowed = (sc.getInstanceKey() != null);
            }
        }
        return utMethodsAllowed;
    }


    /**
    * Check if the given EJBObject/LocalObject has been removed.
    * @exception NoSuchObjectLocalException if the object has been removed.
    */
    @Override
    protected void checkExists(EJBLocalRemoteObject ejbObj) {
        // Doesn't apply to Singletons
    }

    @Override
    protected void afterBegin(EJBContextImpl context) {
        // Singleton SessionBeans cannot implement SessionSynchronization!!
        // EJB2.0 Spec 7.8.
    }

    @Override
    protected void beforeCompletion(EJBContextImpl context) {
        // Singleton SessionBeans cannot implement SessionSynchronization!!
        // EJB2.0 Spec 7.8.
    }

    @Override
    protected void afterCompletion(EJBContextImpl ctx, int status) {
        // Singleton SessionBeans cannot implement SessionSynchronization!!
    }

    // default
    @Override
    public boolean passivateEJB(ComponentContext context) {
        return false;
    }

    /**
     * @deprecated not called and not used in Payara 5
     */
    @Deprecated
    // default
    public void activateEJB(Object ctx, Object instanceKey) {
        // dead code
    }

    @Override
    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {

        ClassLoader originalCCL = null;
        try {

            originalCCL = Utility.setContextClassLoader(loader);

            // Shutdown the singleton instance if it hasn't already been shutdown.
            if (singletonCtxFactory != null) {
                singletonCtxFactory.destroy(singletonCtx);
            }

            if (hasRemoteBusinessView) {
                for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {
                    next.referenceFactory.destroyReference(
                        remoteBusinessObject.getStub(next.generatedRemoteIntf.getName()),
                        remoteBusinessObject.getEJBObject(next.generatedRemoteIntf.getName()));
                }
            }

        } catch (Throwable t) {
            _logger.log(Level.FINE, "Exception during Singleton shutdown", t);
        } finally {
            singletonCtxFactory = null;
            Utility.setContextClassLoader(originalCCL);
        }
    }

    public long getMethodReadyCount() {
        return 0;
    }

    protected class SingletonContextFactory implements ObjectFactory {

        @Override
        public Object create(Object param) {
            try {
                return createSingletonEJB();
            } catch (CreateException ex) {
                throw new EJBException(ex);
            }
        }

        @Override
        public void destroy(Object obj) {
            if (obj == null) {
                return;
            }
            SingletonContextImpl singletonCtx = (SingletonContextImpl) obj;
            // Note: Singletons cannot have incomplete transactions
            // in progress. So it is ok to destroy the EJB.

            // Only need to cleanup and destroy bean (andd call @PreDestroy)
            // if it successfully completed initialization.

            Object sb = singletonCtx.getEJB();

            // Called from pool implementation to reduce the pool size.
            // So we need to call @PreDestroy and mark context as destroyed
            boolean doPreDestroy = true;

            if (clusteredLookup.isClusteredEnabled()) {
                EjbSessionDescriptor sessDesc = (EjbSessionDescriptor) ejbDescriptor;
                IAtomicLong count = clusteredLookup.getClusteredUsageCount();
                if (count.decrementAndGet() <= 0) {
                    clusteredLookup.destroy();
                } else if (sessDesc.dontCallPreDestroyOnDetach()) {
                    doPreDestroy = false;
                }
            }

            singletonCtx.setState(EJBContextImpl.BeanState.DESTROYED);
            EjbInvocation ejbInv = null;
            try {
                // NOTE : Context class-loader is already set by Pool
                ejbInv = createEjbInvocation(sb, singletonCtx);
                invocationManager.preInvoke(ejbInv);
                singletonCtx.setInEjbRemove(true);

                // Call preInvokeTx directly.  InvocationInfo containing tx
                // attribute must be set prior to calling preInvoke
                ejbInv.transactionAttribute = preDestroyInvInfo.txAttr;
                ejbInv.invocationInfo = preDestroyInvInfo;
                preInvokeTx(ejbInv);

                if (doPreDestroy) {
                    intercept(CallbackType.PRE_DESTROY, singletonCtx);
                }

            } catch (Throwable t) {
                if (ejbInv != null) {
                    ejbInv.exception = t;
                }
                _logger.log(Level.FINE, "ejbRemove exception", t);
            } finally {
                singletonCtx.setInEjbRemove(false);
                if ( ejbInv != null ) {
                    invocationManager.postInvoke(ejbInv);
                    try {
                        postInvokeTx(ejbInv);
                    } catch(Exception pie) {
                        _logger.log(Level.FINE, "singleton postInvokeTx exception", pie);
                    }
                }
            }

            cleanupInstance(singletonCtx);
            singletonCtx.deleteAllReferences();
        }
    } // SessionContextFactory{}


    //Methods for StatelessSessionBeanStatsProvider
    public int getMaxPoolSize() {
        return 1;
    }

    public int getSteadyPoolSize() {
        return 1;
    }
}

