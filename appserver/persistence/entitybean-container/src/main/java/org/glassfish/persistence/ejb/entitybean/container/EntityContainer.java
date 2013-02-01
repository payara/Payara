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

package org.glassfish.persistence.ejb.entitybean.container;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.CreateException;
import javax.ejb.EJBContext;
import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.sun.appserv.util.cache.BaseCache;
import com.sun.appserv.util.cache.Cache;
import com.sun.appserv.util.cache.CacheListener;
import com.sun.appserv.util.cache.Constants;
import com.sun.appserv.util.cache.LruCache;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.EJBContextImpl.BeanState;
import com.sun.ejb.containers.EJBLocalObjectImpl;
import com.sun.ejb.containers.EJBLocalRemoteObject;
import com.sun.ejb.containers.EJBObjectImpl;
import com.sun.ejb.containers.EJBHomeInvocationHandler;
import com.sun.ejb.containers.EJBLocalHomeInvocationHandler;
import org.glassfish.persistence.ejb.entitybean.container.cache.EJBObjectCache;
import org.glassfish.persistence.ejb.entitybean.container.cache.EJBObjectCacheListener;
import org.glassfish.persistence.ejb.entitybean.container.cache.FIFOEJBObjectCache;
import org.glassfish.persistence.ejb.entitybean.container.cache.UnboundedEJBObjectCache;
import com.sun.ejb.containers.util.pool.AbstractPool;
import com.sun.ejb.containers.util.pool.NonBlockingPool;
import com.sun.ejb.containers.util.pool.ObjectFactory;
import com.sun.ejb.monitoring.stats.EjbCacheStatsProvider;
import com.sun.ejb.monitoring.stats.EjbCacheStatsProviderDelegate;
import com.sun.ejb.monitoring.stats.EjbMonitoringStatsProvider;
import com.sun.ejb.monitoring.stats.EjbPoolStatsProvider;
import com.sun.ejb.portable.EJBMetaDataImpl;
import com.sun.ejb.portable.ObjrefEnumeration;
import com.sun.ejb.spi.container.BeanStateSynchronization;
import com.sun.enterprise.admin.monitor.callflow.ComponentType;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbCMPEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.BeanCacheDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;
import org.glassfish.persistence.ejb.entitybean.container.spi.ReadOnlyEJBHome;
import org.glassfish.persistence.ejb.entitybean.container.spi.ReadOnlyEJBLocalHome;
import org.glassfish.persistence.ejb.entitybean.container.stats.EntityBeanStatsProvider;

/**
 * This class implements the Container interface for EntityBeans.
 * It is responsible for instance & lifecycle management for BMP & CMP
 * EntityBeans.
 * The EntityContainer implements option B of the commit-time options
 * described in the EJB2.0 spec section 10.5.9
 * It also implements optimistic concurrency (i.e. multiple non-exclusive
 * bean instances per primary key) when there are multiple concurrent
 * transactions on a EntityBean.
 * <p>
 * The following sequence of actions happens for the EntityContainer,
 * for each EJB lifecycle stage (note: getEJBObject, getContext,
 * releaseContext, preInvokeTx, postInvokeTx are called from BaseContainer).
 * 1. EJB Creation
 * homeImpl.create, container.getContext,
 * container.preInvokeTx, ejb.ejbCreate, container.postCreate,
 * ejb.ejbPostCreate, container.postInvokeTx, container.releaseContext
 * 2. EJB Finding
 * homeImpl.find---, container.getContext, container.preInvokeTx,
 * ejb.ejbFind---, container.postFind, container.postInvokeTx,
 * container.releaseContext
 * 3. EJB Invocation
 * container.getEJBObject, ejbObject.someMethod, container.getContext,
 * container.preInvokeTx, ejb.someMethod, container.postInvokeTx,
 * container.releaseContext
 * <P>
 * State Management: The EntityContainer manages collections of EJBs
 * in different states.
 * The 5 states of an EntityBean (an EJB can be in only 1 state at a time):
 * <UL>
 * <LI> 1. POOLED : does not have identity. EJBs in the POOLED state are
 * all identical, hence are maintained in a java.util.Vector,
 * whose size is maintained below a HIGH_WATER_MARK (currently 100).
 * <LI> 2. READY : ready for invocations, no transaction in progress.
 * EJBs in the READY state are associated with a primary key.
 * To enhance reuse of EJB instances, only one READY
 * EJB per primary key is stored. READY EJBs are managed by the
 * ejbstore/EntityStore class. READY EJBs are looked up using a key consisting
 * of the primary key and a null transaction context.
 * <LI> 3. INVOKING : processing an invocation.
 * EJBs in the INVOKING state are not stored anywhere. Before transitioning
 * from READY or INCOMPLETE_TX to INVOKING, the EJB is removed from the
 * EntityStore.
 * <LI> 4. INCOMPLETE_TX : ready for invocations, transaction in progress.
 * EJBs in the INCOMPLETE_TX state are associated with a primary key.
 * INCOMPLETE_TX EJBs are managed by the ejbstore/EntityStore class.
 * INCOMPLETE_TX EJBs are looked up using a composite key consisting
 * of the primary key and the transaction context.
 * <LI> 5. DESTROYED : does not exist.
 * </UL>
 * All READY bean instances are stored in the readyStore.
 * All INCOMPLETE_TX bean instances are stored in the ActiveTxCache.
 * Beans in the READY state are stored with key = ejbObject.
 * Beans in the INCOMPLETE_TX state are stored with key = ejbObject+Tx.
 * Instances in INVOKING state which have transactions associated
 * with them are also in ActiveTxCache.
 * All POOLED instances are stored in the pooledEJBs vector.
 *
 * Note on locking order: if both ready/ActiveTxCache and context are to be
 * locked, always acquire the context lock first, then the Store lock.
 * Note on locking order: if both ready/ActiveTxCache and ejbObject need
 * locks, always acquire the ejbObject lock first, then the Store lock.
 *
 * @author Mahesh Kannan
 * @author Shanker N
 * @author Pramod Gopinath
 */

public class EntityContainer
    extends BaseContainer
    implements CacheListener 
{
    
    private ThreadLocal ejbServant = new ThreadLocal() {
        protected Object initialValue() {
            return null;
        }
    };

    static final Logger _logger =
        LogDomains.getLogger(EntityContainer.class, LogDomains.EJB_LOGGER);

    static final int POOLED=1, READY=2, INVOKING=3,
            INCOMPLETE_TX=4, DESTROYED=5;
    protected static final int HIGH_WATER_MARK=100;
    
    private static final int DEFAULT_TX_CACHE_BUCKETS = 16;

    // table of EJBObjects, indexed by primary key.
    // Note: Hashtable methods are synchronized.
    protected EJBObjectCache ejbObjectStore;
    
    // table of EJBLocalObjectImpls, indexed by primary key.
    // Note: Hashtable methods are synchronized.
    protected EJBObjectCache ejbLocalObjectStore;
    
    //protected  LIFOChannel channel = null;
    protected Stack			passivationCandidates = new Stack();
    
    // table of EJBs (Contexts) in READY state, key is primary key
    protected Cache readyStore;
    
    //Pool of free EntityContexts
    protected AbstractPool	entityCtxPool;
    
    protected boolean isReentrant;
    protected boolean isContainerManagedPers;
    
    protected final float DEFAULT_LOAD_FACTOR = 0.75f;
    protected final int DEFAULT_CACHE_SIZE = 8192;
    protected int _maxBuckets = 8;
    
    protected IASEjbExtraDescriptors iased = null;
    protected BeanCacheDescriptor beanCacheDes = null;
    protected BeanPoolDescriptor beanPoolDes = null;
    protected EjbContainer ejbContainer;
    boolean largeCache = false;
    
    CacheProperties cacheProp = null;
    PoolProperties poolProp = null;
    Object asyncTaskSemaphore = new Object();
    boolean addedASyncTask = false;
    
    // a timer task to trim the beans idle in readyStore
    protected IdleBeansPassivator	idleEJBObjectPassivator;
    protected IdleBeansPassivator	idleLocalEJBObjectPassivator;
    protected boolean				defaultCacheEJBO = true;
    
    IdleBeansPassivator idleBeansPassivator;
    boolean timerValid = true;
    long idleTimeout;
    
    
    protected int ejboRemoved;

    protected int	totalPassivations;
    protected int	totalPassivationErrors;

    private EntityCacheStatsProvider	cacheStatsProvider;

    static {
        _logger.log(Level.FINE," Loading Entitycontainer...");
    }
    
    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     * @exception Exception on error
     */
    protected EntityContainer(EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
    	throws Exception {    
    	this(ContainerType.ENTITY, desc, loader, sm);
    }
    
    protected EntityContainer(ContainerType containerType, EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
    	throws Exception {
        super(containerType, desc, loader, sm);
        EjbEntityDescriptor ed = (EjbEntityDescriptor)desc;
        isReentrant = ed.isReentrant();
        if ( ed.getPersistenceType().equals(
            EjbEntityDescriptor.BEAN_PERSISTENCE) ) {
            isContainerManagedPers = false;
        } else {
            isContainerManagedPers = true;
        }
        
        isBeanManagedTran = false;
        iased = ed.getIASEjbExtraDescriptors();
        if( iased != null) {
            beanCacheDes = iased.getBeanCache();
            beanPoolDes = iased.getBeanPool();
        }
        
        ejbContainer = ejbContainerUtilImpl.getEjbContainer();

        //TODO super.setMonitorOn(ejbContainer.isMonitoringEnabled());
        
        createCaches();
        
        super.createCallFlowAgent(
                isContainerManagedPers ? ComponentType.CMP : ComponentType.BMP);
        _logger.log(Level.FINE,"[EntityContainer] Created EntityContainer: "
                + logParams[0]);
    }
    
    protected void preInitialize(EjbDescriptor desc, ClassLoader loader) {
        EjbEntityDescriptor ed = (EjbEntityDescriptor)desc;
        isReentrant = ed.isReentrant();
        if ( ed.getPersistenceType().equals(
            EjbEntityDescriptor.BEAN_PERSISTENCE) ) {
            isContainerManagedPers = false;
        } else {
            isContainerManagedPers = true;
        }
        _logger.log(Level.FINE,"[EntityContainer] preInitialize==>isContainerManagedPers: "
                + isContainerManagedPers);
    }

    @Override
    protected void setEJBMetaData() throws Exception {
        EjbEntityDescriptor ed = (EjbEntityDescriptor)ejbDescriptor;
        Class primaryKeyClass = loader.loadClass(ed.getPrimaryKeyClassName());

        metadata = new EJBMetaDataImpl(ejbHomeStub, homeIntf, remoteIntf, primaryKeyClass);
    }

    @Override
    protected void validateTxAttr(MethodDescriptor md, int txAttr) throws EJBException {
        super.validateTxAttr(md, txAttr);

        // For EJB2.0 CMP EntityBeans, container is only required to support
        // REQUIRED/REQUIRES_NEW/MANDATORY, see EJB2.0 section 17.4.1.
        if (((EjbEntityDescriptor)ejbDescriptor).getPersistenceType().
                equals(EjbEntityDescriptor.CONTAINER_PERSISTENCE)) {
            EjbCMPEntityDescriptor e= (EjbCMPEntityDescriptor)ejbDescriptor;
            if ( !e.getIASEjbExtraDescriptors().isIsReadOnlyBean() &&
                     e.isEJB20() ) {
                if ( txAttr != TX_REQUIRED && txAttr != TX_REQUIRES_NEW
                        && txAttr != TX_MANDATORY ) {
                    throw new EJBException(
                            "Transaction attribute for EJB2.0 CMP EntityBeans" +
                            " must be Required/RequiresNew/Mandatory");
                }
            }
        }
    }

    @Override
    protected void adjustHomeTargetMethodInfo(InvocationInfo invInfo, String methodName, 
            Class[] paramTypes) throws NoSuchMethodException {
        if( invInfo.startsWithCreate ) {
            String extraCreateChars = methodName.substring("create".length());
            invInfo.targetMethod2 = ejbClass.getMethod
                        ("ejbPostCreate" + extraCreateChars, paramTypes);

        }
    }

    @Override
    protected EJBHomeInvocationHandler getEJBHomeInvocationHandler(
            Class homeIntfClass) throws Exception {
        return new EntityBeanHomeImpl(ejbDescriptor, homeIntfClass);
    }

    @Override
    protected EJBLocalHomeInvocationHandler getEJBLocalHomeInvocationHandler(
            Class homeIntfClass) throws Exception {
        return new EntityBeanLocalHomeImpl(ejbDescriptor, homeIntfClass);
    }

    /**
     * setup a timer task to trim timed out entries in the cache.
     * @param cache cache which is used to setup the timer task
     * @return the passivator object
     */
    public IdleBeansPassivator setupIdleBeansPassivator(Cache cache) 
        throws Exception {
        
        IdleBeansPassivator idleBeansPassivator =
            new IdleBeansPassivator(cache);

        ejbContainerUtilImpl.getTimer().
            scheduleAtFixedRate(idleBeansPassivator, idleTimeout, idleTimeout);
        
        return idleBeansPassivator;
    }
    
    /**
     * cancel a timer task to trim timed out entries in the cache.
     */
    public void cancelTimerTasks() {
        timerValid = false;
        if (idleBeansPassivator != null) {
            try {
                idleBeansPassivator.cancel();
                idleBeansPassivator.cache  = null;
            } catch (Exception e) {
                _logger.log(Level.FINE, "[EntityContainer] cancelTimerTask: " + 
                    e);
            }
        }
        
        if (idleEJBObjectPassivator != null) {
            try {
                idleEJBObjectPassivator.cancel();
                idleEJBObjectPassivator.cache  = null;
            } catch (Exception e) {
                _logger.log(Level.FINE, "[EntityContainer] cancelTimerTask: " +
                    e);
            }
        }
        
        if (idleLocalEJBObjectPassivator != null) {
            try {
                idleLocalEJBObjectPassivator.cancel();
                idleLocalEJBObjectPassivator.cache  = null;
            } catch (Exception e) {
                _logger.log(Level.FINE, "[EntityContainer] cancelTimerTask: " +
                    e);
            }
        }
        
        this.idleEJBObjectPassivator    = null;
        this.idleLocalEJBObjectPassivator    = null;
        this.idleBeansPassivator = null;
    }
    
    protected InvocationInfo postProcessInvocationInfo(
            InvocationInfo invInfo) {
        Method method = invInfo.method;
        boolean isCMPField = isContainerManagedPers && invInfo.isBusinessMethod
                && invInfo.methodIntf.equals(MethodDescriptor.EJB_LOCAL);
        if (isCMPField) {
            String methodName = method.getName();
            isCMPField = methodName.startsWith("get") 
                    || methodName.startsWith("set");
            if (isCMPField) {
                try {
                    //ejbClass is the container-generated implementation class.
                    //Need to get its superclass, which is provided by the bean provider.
                    Method methodInBeanClass = ejbClass.getSuperclass().getMethod(
                            methodName, method.getParameterTypes());
                    isCMPField = Modifier.isAbstract(methodInBeanClass.getModifiers());
                } catch (NoSuchMethodException ignore) {
                    isCMPField = false;
                }
            }
        }
        invInfo.isTxRequiredLocalCMPField = isCMPField
                && (invInfo.txAttr == TX_REQUIRED);
        return invInfo;
    }
    
    /**
     * Called from the ContainerFactory during initialization.
     */
    protected void initializeHome()
        throws Exception
    {
        ObjectFactory entityCtxFactory = new EntityContextFactory(this);
        
        int steadyPoolSize = 0;
        int resizeQuantity = 10;
        int idleTimeoutInSeconds = Integer.MAX_VALUE-1;
        poolProp = new PoolProperties(this);
        
        super.initializeHome();

        entityCtxPool = new NonBlockingPool(getContainerId(), ejbDescriptor.getName(),
        	entityCtxFactory, poolProp.steadyPoolSize,
            poolProp.poolResizeQuantity, poolProp.maxPoolSize,
            poolProp.poolIdleTimeoutInSeconds, loader);


	registerMonitorableComponents();
    }

    protected void registerMonitorableComponents() {
        super.registerMonitorableComponents();
	if (readyStore != null) {
	    int confMaxCacheSize = cacheProp.maxCacheSize;
	    if (confMaxCacheSize <= 0) {
		confMaxCacheSize = Integer.MAX_VALUE;
	    }
	    this.cacheStatsProvider = new EntityCacheStatsProvider(
		    (BaseCache) readyStore, confMaxCacheSize);
	    //registryMediator.registerProvider(cacheStatsProvider);
            cacheProbeListener = new EjbCacheStatsProvider(cacheStatsProvider,
                    getContainerId(), containerInfo.appName, containerInfo.modName,
                    containerInfo.ejbName);
            cacheProbeListener.register();
	}
        poolProbeListener = new EjbPoolStatsProvider(entityCtxPool, 
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        poolProbeListener.register();
        _logger.log(Level.FINE, "[Entity Container] registered monitorable");
    }

    protected EjbMonitoringStatsProvider getMonitoringStatsProvider(
            String appName, String modName, String ejbName) {
        return new EntityBeanStatsProvider(this, getContainerId(), appName, modName, ejbName);
    }
    
    public void onReady() {
    }
    
/** TODO
    public String getMonitorAttributeValues() {
        StringBuffer sbuf = new StringBuffer();
	appendStats(sbuf);
	return sbuf.toString();
    }

    public void appendStats(StringBuffer sbuf) {
	sbuf.append("\nEntityContainer: ")
	    .append("CreateCount=").append(statCreateCount).append("; ")
	    .append("RemoveCount=").append(statRemoveCount).append("; ")
	    .append("PassQSize=")
	    .append(passivationCandidates.size()).append("]");
        Map stats = null;
        if (readyStore != null) {
            stats = readyStore.getStats();
        }
        appendStat(sbuf, "ReadyStore", stats);
        
        appendStat(sbuf, "EJBObjectStore", ejbObjectStore.getStats());
        appendStat(sbuf, "EJBLocalObjectStore",ejbLocalObjectStore.getStats());
    }
**/

    /****************************/
    //Methods of EntityBeanStatsProvider

    public int getMaxCacheSize() {
	int maxSize = 0;
	if (readyStore != null) {
	    maxSize = (cacheProp.maxCacheSize <= 0)
		? Integer.MAX_VALUE
		: cacheProp.maxCacheSize;
	}

	return maxSize;
    }

    public int getSteadyPoolSize() {
	return entityCtxPool.getSteadyPoolSize();
    }

    public int getMaxPoolSize() {
	return entityCtxPool.getMaxPoolSize();
    }

    public long getPooledCount() {
	return entityCtxPool.getSize();
    }

    public long getReadyCount() {
	return (readyStore == null)
	    ? 0 
	    : readyStore.getEntryCount();
    }

    /**
     * Implementation of BaseContainer method. This is never called.
     */
    protected EJBObjectImpl createEJBObjectImpl()
        throws CreateException, RemoteException
    {
        throw new EJBException(
            "INTERNAL ERROR: EntityContainer.createEJBObject() called");
    }
    
    protected EJBLocalObjectImpl createEJBLocalObjectImpl()
        throws CreateException
    {
        throw new EJBException(
          "INTERNAL ERROR: EntityContainer.createEJBLocalObjectImpl() called");
    }
    
    
    /**
     * Called when a remote EjbInvocation arrives for an EJB.
     */
    protected EJBObjectImpl getEJBObjectImpl(byte[] streamKey) {
        // First get the primary key of the EJB
        Object primaryKey;
        try {
            primaryKey = EJBUtils.deserializeObject(streamKey, loader, false);
        } catch ( Exception ex ) {
            throw new EJBException(ex);
        }
        
        return internalGetEJBObjectImpl(primaryKey, streamKey);
    }
    
    
    /**
     * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
     * a local object reference.
     */
    protected EJBLocalObjectImpl getEJBLocalObjectImpl(Object key) {
        return internalGetEJBLocalObjectImpl(key);
    }
    
    /**
     * Called from BaseContainer.preInvoke which is called from the EJBObject
     * for local and remote invocations, and from the EJBHome for create/find.
     */
    protected ComponentContext _getContext(EjbInvocation inv) {
        if ( inv.invocationInfo.isCreateHomeFinder ) { 
            // create*, find*, home methods
            // Note: even though CMP finders dont need an instance,
            // we still return a pooled instance, so that the Tx demarcation
            // in BaseContainer.pre+postInvoke can work.
            
            // get any pooled EJB
            EntityContextImpl context = getPooledEJB();
            
            // we're sure that no concurrent thread can be using this
            // context, so no need to synchronize.
            context.setState(BeanState.INVOKING);
            
            if ( inv.invocationInfo.startsWithCreate )
                preCreate(inv, context);
            else if ( inv.invocationInfo.startsWithFind )
                preFind(inv, context);


            context.setLastTransactionStatus(-1);
            context.incrementCalls();
            
            return context;
        }
        
        // If we came here, it means this is a business method
        // and there is an EJBObject/LocalObject.
        
        // If we would invoke the EJB with the client's Tx,
        // try to get an EJB with that incomplete Tx.
        EntityContextImpl context = null;
        if ( willInvokeWithClientTx(inv) )
            context = getEJBWithIncompleteTx(inv);
        if ( context == null )
            context = getReadyEJB(inv);
        
        synchronized ( context ) {
            if ( context.isInState(BeanState.INVOKING) && !isReentrant )
                throw new EJBException(
                    "EJB is already executing another request");
            if (context.isInState(BeanState.POOLED) ||
                context.isInState(BeanState.DESTROYED)) {
                // somehow a concurrent thread must have changed state.
                // this is an internal error.
                throw new EJBException("Internal error: unknown EJB state");
            }
            
            context.setState(BeanState.INVOKING);
        }
        
        context.setLastTransactionStatus(-1);
        context.incrementCalls();
        // A business method may modify the bean's state
        context.setDirty(true);
        
        return context;
    }
    
    protected boolean willInvokeWithClientTx(EjbInvocation inv) {
        int status = Status.STATUS_UNKNOWN;
        try {
            Integer preInvokeTxStatus = inv.getPreInvokeTxStatus();
            status = (preInvokeTxStatus != null) ?
                preInvokeTxStatus.intValue() : transactionManager.getStatus();
        } catch ( SystemException ex ) {
            throw new EJBException(ex);
        }
        if ( status != Status.STATUS_NO_TRANSACTION ) {
            int txAttr = inv.invocationInfo.txAttr;
            switch (txAttr) {
                case TX_SUPPORTS:
                case TX_REQUIRED:
                case TX_MANDATORY:
                    return true;
            }
        }
        return false;
    }
    
    
    
    /**
     * This is called from BaseContainer.postInvoke after
     * EntityContainer.preInvokeTx has been called.
     */
    public void releaseContext(EjbInvocation inv) {
        EntityContextImpl context = (EntityContextImpl)inv.context;
        boolean decrementedCalls = false; // End of IAS 4661771
        
        if ( context.isInState(BeanState.DESTROYED) )
            return;
        
        try {
            if ( context.hasReentrantCall() ) {
                // For biz->biz or postCreate->biz, the bean instance will
                // remain in the incomplete-tx table.
                if ( containerStateManager.isRemovedEJBObject(inv) ) {
                    // biz -> remove case (biz method invoked reentrant remove)
                    // Remove from IncompleteTx table, to prevent further
                    // reentrant calls.
                    removeIncompleteTxEJB(context, true);
                    
                    containerStateManager.disconnectContext(context);
                } else {
                    if ( context.isInState(BeanState.INVOKING) )  {
                        doFlush( inv );
                    }
                }
                
                // Note: at this point context.getState() is INVOKING.
            } else if ( containerStateManager.isNullEJBObject(context)
                && containerStateManager.isNullEJBLocalObject(context) ) {
                // This can only happen if the method was ejbFind
                // OR if the method was ejbCreate which threw an application
                // exception (so postCreate was not called)
                // OR after a biz method which called a reentrant remove.
                // So bean instance goes back into pool.
                // We dont care if any Tx has completed or not.
                //context.setTransaction(null);
                decrementedCalls = true;
                context.decrementCalls();
                if (!(inv.invocationInfo.startsWithCreate)) {
                    context.setTransaction(null);
                    addPooledEJB(context);
                }else if(context.getTransaction() == null) {
                    addPooledEJB(context);
                } else {
                    // Set the state to incomplete as the transaction
                    // is not done still and afterCompletion will
                    // handle stuff
                    context.setState(BeanState.INCOMPLETE_TX);
                }
            } else if ( containerStateManager.isRemovedEJBObject(inv) ) {
                // EJBObject/LocalObject was removed, so bean instance
                // goes back into pool.
                // We dont care if any Tx has completed or not.
                removeIncompleteTxEJB(context, true);
                // unset the removed flag, in case the EJB(Local)Object
                // ref is held by the client and is used again
                containerStateManager.markObjectRemoved(context, false);

                decrementedCalls = true;
                context.decrementCalls();
                if(context.getTransaction() == null) {
                    addPooledEJB(context);
                } else {
                    // Set the state to incomplete as the transaction
                    // is not done still and afterCompletion will
                    // handle stuff
                    context.setState(BeanState.INCOMPLETE_TX);
                }
                
            } else if ( context.getTransaction() == null ) {
                // biz methods and ejbCreate
                // Either the EJB was called with no tx,
                // or it was called with a tx which finished,
                // so afterCompletion was already called.
                
                // If no tx or tx committed, then move the EJB to READY state
                // else pool the bean
                int status = context.getLastTransactionStatus();
                decrementedCalls = true;
                context.decrementCalls();
                context.setLastTransactionStatus(-1);
                if ( status == -1 || status == Status.STATUS_COMMITTED
                || status == Status.STATUS_NO_TRANSACTION )
                    addReadyEJB(context);
                else
                    passivateAndPoolEJB(context);
            } else {
                // biz methods and ejbCreate
                // The EJB is still associated with a Tx.
                // It will already be in the INCOMPLETE_TX table.
                context.setState(BeanState.INCOMPLETE_TX);

                doFlush( inv );
            }
        } catch ( Exception ex ) {
            _logger.log(Level.FINE, "entitybean.container.release_context_exception",
                        logParams);
            _logger.log(Level.FINE, "",ex);
            throw new EJBException(ex);
        } finally {
            if (decrementedCalls == false) {
                context.decrementCalls();
            }
            context.touch();
        }
    }
    
    
    /**
     * Called from getContext before the ejb.ejbCreate is called
     */
    protected void preCreate(EjbInvocation inv, EntityContextImpl context) {
	ejbProbeNotifier.ejbBeanCreatedEvent(
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
    }
    
    
    /**
     * This is called from the generated "HelloEJBHomeImpl" create* method,
     * after ejb.ejbCreate() has been called and before ejb.ejbPostCreate()
     * is called.
     * Note: postCreate will not be called if ejbCreate throws an exception
     */
    public void postCreate(EjbInvocation inv, Object primaryKey)
        throws CreateException
    {
        if ( primaryKey == null )
            throw new EJBException(
                "Null primary key returned by ejbCreate method");
        
        if ( (isRemote) && (!inv.isLocal) ) {
            // remote EjbInvocation: create EJBObject
            EJBObjectImpl ejbObjImpl = internalGetEJBObjectImpl(primaryKey, null, true);
            
            // associate the context with the ejbObject
            containerStateManager.attachObject(inv, (EJBContextImpl)inv.context, ejbObjImpl, null);
        }
        
        if ( isLocal ) {
            // create EJBLocalObject irrespective of local/remote EjbInvocation
            // this is necessary to make EntityContext.getPrimaryKey and
            // EntityContext.getEJBObject work.
            EJBLocalObjectImpl localObjImpl = internalGetEJBLocalObjectImpl(primaryKey, true);
            
            // associate the context with the ejbLocalObject
            containerStateManager.attachObject(inv, (EJBContextImpl)inv.context, null, localObjImpl);
        }
        
        EntityContextImpl context = (EntityContextImpl)inv.context;
        if ( context.getTransaction() != null ) {
            // Add EJB to INCOMPLETE_TX table so that concurrent/loopback
            // invocations will be correctly handled
            addIncompleteTxEJB(context);
        }
        
        context.setDirty(true); // ejbPostCreate could modify state
    }
    
    //Called from EJB(Local)HomeInvocationHandler
    //Note: preFind is already called from getContext
    protected Object invokeFindByPrimaryKey(Method method,
	    EjbInvocation inv, Object[] args)
	throws Throwable
    {
	Object pKeys = super.invokeTargetBeanMethod(method,
	    inv, inv.ejb, args, null);
	return postFind(inv, pKeys, null);
    }

    protected void authorizeLocalGetPrimaryKey(EJBLocalRemoteObject ejbObj) 
            throws EJBException {
        authorizeLocalMethod(BaseContainer.EJBLocalObject_getPrimaryKey);
        checkExists(ejbObj);
    }
   
    protected void authorizeRemoteGetPrimaryKey(EJBLocalRemoteObject ejbObj) 
            throws RemoteException {
        authorizeRemoteMethod(BaseContainer.EJBObject_getPrimaryKey);
    }
   
    /**
     * Called from getContext before the ejb.ejbFind* is called
     */
    protected void preFind(EjbInvocation inv, EntityContextImpl context) {
        // if the finder is being invoked with the client's transaction,
        // call ejbStore on all dirty bean instances associated with that
        // transaction. This ensures that the finder results will include
        // all updates done previously in the client's tx.
        if ( willInvokeWithClientTx(inv) &&
        !inv.method.getName().equals("findByPrimaryKey") ) {
            Transaction tx = null;
            try {
                tx = transactionManager.getTransaction();
            } catch ( SystemException ex ) {
                throw new EJBException(ex);
            }
            
            storeAllBeansInTx( tx );
        }
        
    }
    
    /**
     * Called from CMP PersistentManager
     */
    public void preSelect() 
      throws javax.ejb.EJBException {
	// if the ejbSelect is being invoked with the client's transaction,
        // call ejbStore on all dirty bean instances associated with that
        // transaction. This ensures that the select results will include
        // all updates done previously in the client's tx.
	_logger.fine(" inside preSelect...");
	Transaction tx = null;
	try {
	    _logger.fine("PRESELECT : getting transaction...");
	    tx = transactionManager.getTransaction();
	} catch ( SystemException ex ) {
	    throw new EJBException(ex);
	}
	_logger.fine("PRESELECT : calling storeAllBeansInTx()...");
	storeAllBeansInTx( tx );                
    }    

    /**
     * Convert a collection of primary keys to a collection of EJBObjects.
     * (special case: single primary key).
     * Note: the order of input & output collections must be maintained.
     * Null values are preserved in both the single primary key return
     * and collection-valued return cases.
     *
     * This is called from the generated "HelloEJBHomeImpl" find* method,
     * after ejb.ejbFind**() has been called.
     * Note: postFind will not be called if ejbFindXXX throws an exception
     */
    public Object postFind(EjbInvocation inv, Object primaryKeys, 
        Object[] findParams)
        throws FinderException
    {
                
        if ( primaryKeys instanceof Enumeration ) {
            // create Enumeration of objrefs from Enumeration of primaryKeys
            Enumeration e = (Enumeration)primaryKeys;
            // this is a portable Serializable Enumeration
            ObjrefEnumeration objrefs = new ObjrefEnumeration();
            while ( e.hasMoreElements() ) {
                Object primaryKey = e.nextElement();
                Object ref;
                if( primaryKey != null ) {
                    if ( inv.isLocal )
                        ref = getEJBLocalObjectForPrimaryKey(primaryKey);
                    else
                        ref = getEJBObjectStub(primaryKey, null);
                    objrefs.add(ref);
                } else {
                    objrefs.add(null);
                }
            }
            return objrefs;
        } else if ( primaryKeys instanceof Collection ) {
            // create Collection of objrefs from Collection of primaryKeys
            Collection c = (Collection)primaryKeys;
            Iterator it = c.iterator();
            ArrayList objrefs = new ArrayList();  // a Serializable Collection
            while ( it.hasNext() ) {
                Object primaryKey = it.next();
                Object ref;
                if( primaryKey != null ) {
                    if ( inv.isLocal )
                        ref = getEJBLocalObjectForPrimaryKey(primaryKey);
                    else
                        ref = getEJBObjectStub(primaryKey, null);
                    objrefs.add(ref);
                } else {
                    objrefs.add(null);
                }
            }
            return objrefs;
        } else {
            if( primaryKeys != null ) {
                if ( inv.isLocal )
                    return getEJBLocalObjectForPrimaryKey(primaryKeys);
                else
                    return getEJBObjectStub(primaryKeys, null);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Called only from the Persistence Manager for EJB2.0 CMP EntityBeans.
     * This is a private API between the PM and Container because there
     * is no standard API defined in EJB2.0 for the PM to get an EJBObject
     * for a primary key (home.findByPrimaryKey cant be used because it may
     * not run in the same tx).
     */
    public EJBObject getEJBObjectForPrimaryKey(Object pkey) {
        // create stub without creating EJBObject
        return getEJBObjectStub(pkey, null);
    }

    /**
     * Called only from the Persistence Manager for EJB2.0 CMP EntityBeans.
     * Called only during cascade delete......
     * This is a private API between the PM and Container because there
     * is no standard API defined in EJB2.0 for the PM to get an EJBLocalObject
     * for a primary key (findByPrimaryKey cant be used because it may
     * not run in the same tx).
     * 
     * Example 1:
     *  A cascadeDeletes B and B calls getA() (expected return value: null)
     *
     *  In the above case, getA() eventualy calls getEJBLocalObjectForPrimaryKey(PK_of_A, Ctx_of_B)
     *  We first check if B is in the process of being cascade deleted by checking the 
     *  cascadeDeleteBeforeEJBRemove flag. If this flag is true, only then we bother to check if
     *  the Context associated with the PK_of_A in this transaction is marked for cascade delete
     *  which can be figured out by checking isCascadeDeleteAfterSuperEJBRemove() in A's context.
     *  If A is marked for cascade delete then we return null else the EJBLocalObject associated
     *  with A.
     *  
     * Example 2:
     *  C cascadeDeletes B and B calls getA() (expected return value: EJBLocalObject for PK_of_A)
     *
     *  In the above case, getA() eventualy calls getEJBLocalObjectForPrimaryKey(PK_of_A, Ctx_of_B)
     *  We first check if B is in the process of being cascade deleted by checking the 
     *  cascadeDeleteBeforeEJBRemove flag. This flag will be true, and hence we check if
     *  the Context associated with the PK_of_A in this transaction is marked for cascade delete
     *  which can be figured out by checking isCascadeDeleteAfterSuperEJBRemove() in A's context.
     *  In this case this flag will be false and hcen we return the ejbLocalObject
     * Example 2:
     *  B is *NOT* cascade deleted and B calls getA() (expected return value: EJBLocalObject for PK_of_A)
     *
     *  In the above case, getA() eventualy calls getEJBLocalObjectForPrimaryKey(PK_of_A, Ctx_of_B)
     *  We first check if B is in the process of being cascade deleted by checking the 
     *  cascadeDeleteBeforeEJBRemove flag. This flag will be FALSE, and hence we do not make
     *  any further check and return the EJBLocalObject associated with A
     *
     * @param pkey The primary key for which the EJBLocalObject is required
     * @param ctx The context associated with the bean from which the accessor method is invoked
     * @return The EJBLocalObject associated with the PK or null if it is cascade deleted.
     *
     */
    public EJBLocalObject getEJBLocalObjectForPrimaryKey
        (Object pkey, EJBContext ctx) {
        // EntityContextImpl should always be used in conjunction with EntityContainer so we can always cast
        assert ctx instanceof EntityContextImpl;
        EntityContextImpl context = (EntityContextImpl) ctx;
        EJBLocalObjectImpl ejbLocalObjectImpl = 
            internalGetEJBLocalObjectImpl(pkey);

        if (context.isCascadeDeleteBeforeEJBRemove()) {
            JavaEETransaction current = null;
            try {
                current = (JavaEETransaction) transactionManager.getTransaction();
            } catch ( SystemException ex ) {
                throw new EJBException(ex);
            }
	    ActiveTxCache activeTxCache = (current == null) ? null :
		(ActiveTxCache) (ejbContainerUtilImpl.getActiveTxCache(current));
            if (activeTxCache != null) {
		EntityContextImpl ctx2 = (EntityContextImpl)
			activeTxCache.get(this, pkey);
		if ((ctx2 != null) && 
		    (ctx2.isCascadeDeleteAfterSuperEJBRemove())) {
		    return null;
		}
	    }
	    return (EJBLocalObject) ejbLocalObjectImpl.getClientObject();
        }

        return (EJBLocalObject) ejbLocalObjectImpl.getClientObject();
    }

    /**
     * Called only from the Persistence Manager for EJB2.0 CMP EntityBeans.
     * This is a private API between the PM and Container because there
     * is no standard API defined in EJB2.0 for the PM to get an EJBLocalObject
     * for a primary key (findByPrimaryKey cant be used because it may
     * not run in the same tx).
     */
    public EJBLocalObject getEJBLocalObjectForPrimaryKey(Object pkey) {
        EJBLocalObjectImpl localObjectImpl = 
            internalGetEJBLocalObjectImpl(pkey);
	return (localObjectImpl != null) ? 
            (EJBLocalObject) localObjectImpl.getClientObject() : null;
    }
    
    // Called from EJBHomeImpl.remove(primaryKey),
    // EJBLocalHomeImpl.remove(primaryKey)
    protected void doEJBHomeRemove(Object primaryKey, Method removeMethod,
        boolean local)
        throws RemoveException, EJBException, RemoteException
    {
        EJBLocalRemoteObject ejbo;
        if ( local ) {
            ejbo = internalGetEJBLocalObjectImpl(primaryKey, false, true);
        }
        else { // may be remote-only bean
            ejbo = internalGetEJBObjectImpl(primaryKey, null, false, true);
        }
        removeBean(ejbo, removeMethod, local);
    }
    
    // Called from EJBObjectImpl.remove, EJBLocalObjectImpl.remove,
    // and removeBean above.
    protected void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod,
            boolean local)
        throws RemoveException, EJBException, RemoteException
    {
        EjbInvocation i = super.createEjbInvocation();
        i.ejbObject = ejbo;
        i.isLocal = local;
        i.isRemote = !local;
        i.method = removeMethod;
        
        // Method must be a remove method defined on one of :
        // javax.ejb.EJBHome, javax.ejb.EJBObject, javax.ejb.EJBLocalHome,
        // javax.ejb.EJBLocalObject
        Class declaringClass = removeMethod.getDeclaringClass();
        i.isHome = ( (declaringClass == javax.ejb.EJBHome.class) ||
                     (declaringClass == javax.ejb.EJBLocalHome.class) );

        try {
            preInvoke(i);
            removeBean(i);
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"entitybean.container.preinvoke_exception",logParams);
            _logger.log(Level.SEVERE,"",e);
            i.exception = e;
        } finally {
            postInvoke(i);
        }
        
        if(i.exception != null) {
            if(i.exception instanceof RemoveException) {
                throw (RemoveException)i.exception;
            }
            else if(i.exception instanceof RuntimeException) {
                throw (RuntimeException)i.exception;
            }
            else if(i.exception instanceof Exception) {
                throw new EJBException((Exception)i.exception);
            }
            else {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(i.exception);
                throw ejbEx;
            }
        }
    }
    
    
    /**
     * container.preInvoke() must already be done.
     * So this will be called with the proper Tx context.
     * @exception RemoveException if an error occurs while removing the bean
     */
    protected void removeBean(EjbInvocation inv)
        throws RemoveException
    {
        try {
	    ejbProbeNotifier.ejbBeanDestroyedEvent(
                    getContainerId(), containerInfo.appName, containerInfo.modName,
                    containerInfo.ejbName);
            // Note: if there are concurrent invocations/transactions in
            // progress for this ejbObject, they will be serialized along with
            // this remove by the database. So we optimistically do ejbRemove.
            
            // call ejbRemove on the EJB
            // the EJB is allowed to veto the remove by throwing RemoveException
            EntityBean ejb = (EntityBean)inv.ejb;
            EntityContextImpl context = (EntityContextImpl)inv.context;
            callEJBRemove(ejb, context);
            
            // inv.ejbObject could be a EJBObject or a EJBLocalObject
            Object primaryKey = getInvocationKey(inv);
            if ( isRemote ) {
                removeEJBObjectFromStore(primaryKey);
            }
            
            if ( isLocal ) {
                // Remove the EJBLocalObject from ejbLocalObjectStore
                ejbLocalObjectStore.remove(primaryKey);
            }

            // Mark EJB as removed. Now releaseContext will add bean to pool
            containerStateManager.markObjectRemoved(context, true);
            
            // Remove any timers for this entity bean identity.
            cancelTimers(primaryKey);
        } catch ( RemoveException ex ) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"entitybean.container.local_remove_exception",logParams);
                _logger.log(Level.FINE,"",ex);
            }
            throw ex;
        }
        catch ( Exception ex ) {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"entitybean.container.remove_bean_exception",logParams);
                _logger.log(Level.FINE,"",ex);
            }
            throw new EJBException(ex);
        }
    }
    
    private void removeEJBObjectFromStore(Object primaryKey) {
        removeEJBObjectFromStore(primaryKey, true);
    }
    
    ComponentInvocation getCurrentInvocation() {
        return invocationManager.getCurrentInvocation();
    }
    
    private void removeEJBObjectFromStore(Object primaryKey, boolean decrementRefCount) {
        // Remove the EJBObject from ejbObjectStore so future lookups
        // in internalGetEJBObject will not get it.
        EJBObjectImpl ejbObjImpl = 
            (EJBObjectImpl)ejbObjectStore.remove(primaryKey, decrementRefCount);
                                                 
        if ( ejbObjImpl != null ) {
            synchronized ( ejbObjImpl ) {
                // disconnect the EJBObject from the ProtocolManager
                // so that no remote invocations can reach the EJBObject
                remoteHomeRefFactory.destroyReference(ejbObjImpl.getStub(), 
                                                  ejbObjImpl.getEJBObject());
            }
        }
    }
    
    /**
     * Remove a bean. Used by the PersistenceManager.
     * This is needed because the PM's remove must bypass tx/security checks.
     */
    public void removeBeanUnchecked(EJBLocalObject localObj) {
        // First convert client EJBLocalObject to EJBLocalObjectImpl
        EJBLocalObjectImpl localObjectImpl = 
            EJBLocalObjectImpl.toEJBLocalObjectImpl(localObj);
        internalRemoveBeanUnchecked(localObjectImpl, true);
    }
    
    
    /**
     * Remove a bean. Used by the PersistenceManager.
     * This is needed because the PM's remove must bypass tx/security checks.
     */
    public void removeBeanUnchecked(Object primaryKey) {
        EJBLocalRemoteObject ejbo;
        if ( isLocal ) {
            ejbo = internalGetEJBLocalObjectImpl(primaryKey);
            internalRemoveBeanUnchecked(ejbo, true);
        }
        else { // remote-only bean
            ejbo = internalGetEJBObjectImpl(primaryKey, null);
            internalRemoveBeanUnchecked(ejbo, false);
        }
    }
    
    /**
     * Remove a bean. Used by the PersistenceManager.
     * This is needed because the PM's remove must bypass tx/security checks.
     */
    private void internalRemoveBeanUnchecked(EJBLocalRemoteObject localRemoteObj, 
            boolean local) {
        EjbInvocation inv = super.createEjbInvocation();
        inv.ejbObject = localRemoteObj;
        inv.isLocal = local;
        inv.isRemote = !local;
        Method method=null;
        try {
            method = EJBLocalObject.class.getMethod("remove", NO_PARAMS);
        } catch ( NoSuchMethodException e ) {
            _logger.log(Level.FINE, 
                "Exception in internalRemoveBeanUnchecked()", e);
        }
        inv.method = method;
        
        inv.invocationInfo = (InvocationInfo) invocationInfoMap.get(method);
        
        try {
            // First get a bean instance on which ejbRemove can be invoked.
            // This code must be in sync with getContext().
            // Can't call getContext() directly because it does stuff
            // based on remove's txAttr.
            // Assume there is a tx on the current thread.
            EntityContextImpl context = getEJBWithIncompleteTx(inv);
            if ( context == null ) {
                context = getReadyEJB(inv);
            }
            
            synchronized ( context ) {
                if ( context.isInState(BeanState.INVOKING) && !isReentrant ) {
                    throw new EJBException(
                        "EJB is already executing another request");
                }
                if (context.isInState(BeanState.POOLED) ||
                    context.isInState(BeanState.DESTROYED)) {
                    // somehow a concurrent thread must have changed state.
                    // this is an internal error.
                    throw new EJBException("Internal error: unknown EJB state");
                }
                
                context.setState(BeanState.INVOKING);
            }
            inv.context = context;
            context.setLastTransactionStatus(-1);
            context.incrementCalls();
            
            inv.instance = inv.ejb = context.getEJB();
            inv.container = this;
            invocationManager.preInvoke(inv);
            
            // call ejbLoad if necessary
            useClientTx(context.getTransaction(), inv);
            
            try {
                context.setCascadeDeleteBeforeEJBRemove(true);
                removeBean(inv);
            } catch ( Exception ex ) {
                _logger.log(Level.FINE, 
                    "Exception in internalRemoveBeanUnchecked()", ex);
                // if system exception mark the tx for rollback
                inv.exception = checkExceptionClientTx(context, ex);
            }
            if ( inv.exception != null ) {
                throw inv.exception;
            }
        }
        catch ( RuntimeException ex ) {
            throw ex;
        }
        catch ( Exception ex ) {
            throw new EJBException(ex);
        }
        catch ( Throwable ex ) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;
        }
        finally {
            invocationManager.postInvoke(inv);
            releaseContext(inv);
        }
    }
    
    /**
     * Discard the bean instance. The bean's persistent state is not removed.
     * This is usually called when the bean instance throws a system exception,
     * from BaseContainer.postInvokeTx, getReadyEJB,
     * afterBegin, beforeCompletion, passivateEJB.
     */
    protected void forceDestroyBean(EJBContextImpl ctx) {
        // Something bad happened (such as a RuntimeException),
        // so kill the bean and let it be GC'ed
        // Note: EJB2.0 section 18.3.1 says that discarding an EJB
        // means that no methods other than finalize() should be invoked on it.
        
        EntityContextImpl context = (EntityContextImpl)ctx;
        if ( context.isInState(BeanState.DESTROYED) ) {
            entityCtxPool.destroyObject(null);
            return;
        }
        
        // Start of IAS 4661771
        synchronized ( context ) {
            try {
                Object primaryKey = context.getPrimaryKey();
                if ( primaryKey != null ) {
                    if ( context.getTransaction() != null ) {
                        Transaction txCurrent = context.getTransaction();
			ActiveTxCache activeTxCache = (ActiveTxCache) 
			    ejbContainerUtilImpl.getActiveTxCache(txCurrent);
                        if (activeTxCache !=  null) {
			    // remove the context from the store
			    activeTxCache.remove(this, primaryKey);
			}
                    }
                    
                    // remove the context from readyStore as well
                    removeContextFromReadyStore(primaryKey, context);
                    
                    if (!containerStateManager.isNullEJBObject(context)) {
                        removeEJBObjectFromStore(primaryKey);
                    }
                    if (!containerStateManager.isNullEJBLocalObject(context)) {
                        ejbLocalObjectStore.remove(primaryKey);
                    }
                    
                }
            } catch ( Exception ex ) {
                _logger.log(Level.FINE, "Exception in forceDestroyBean()", ex);
            } finally {
                try {
                    //Very importatnt to set the state as destroyed otherwise
                    //	the pool.destroy might wrongly call unsetEntityContext
                    context.setState(BeanState.DESTROYED);
                    entityCtxPool.destroyObject(context);
                } catch (Exception ex) {
                    _logger.log(Level.FINE, "Exception in forceDestroyBean()", 
                        ex);
                }
            }
        }
        // End of IAS 4661771
    }
    
    
    // Called before invoking a bean with no Tx or with a new Tx.
    // Check if the bean is associated with an unfinished tx.
    protected void checkUnfinishedTx(Transaction prevTx, EjbInvocation inv) {
                                     
        try {
            if ( (prevTx != null) &&
                 prevTx.getStatus() != Status.STATUS_NO_TRANSACTION ) {
                // An unfinished tx exists for the bean.
                // so we cannot invoke the bean with no Tx or a new Tx.
                throw new IllegalStateException(
                  "Bean is associated with a different unfinished transaction");
            }
        } catch (SystemException ex) {
            throw new EJBException(ex);
        }
    }
    
    
    /**
     * Check if the given EJBObject/LocalObject has been removed.
     * Called before executing non-business methods of EJBLocalObject.
     * @exception NoSuchObjectLocalException if the object has been removed.
     */
    protected void checkExists(EJBLocalRemoteObject ejbObj) {
        // Need to call ejbLoad to see if persistent state is removed.
        // However, the non-business methods dont have a transaction attribute.
        // So do nothing for now.
    }
    
    // Called from BaseContainer.SyncImpl
    protected void afterBegin(EJBContextImpl ctx) {
        EntityContextImpl context  = (EntityContextImpl)ctx;
        
        // Note: EntityBeans are not allowed to be TX_BEAN_MANAGED
        if ( context.isInState(BeanState.DESTROYED) )
            return;
        
        if ( !containerStateManager.isNullEJBObject(context)
             || !containerStateManager.isNullEJBLocalObject(context) ) {
            // ejbLoad needed only for business methods and removes
            
            // Add EJB to INCOMPLETE_TX table so that concurrent/loopback
            // invocations will be correctly handled
            if ( context.getTransaction() != null ) {
                addIncompleteTxEJB(context);
            }
            
            // need to call ejbLoad since there can be more than
            // one active EJB instance per primaryKey. (Option B in 9.11.5).
            EntityBean e = (EntityBean)context.getEJB();
            try {
                callEJBLoad(e, context, true);
            } catch ( NoSuchEntityException ex ) {
                _logger.log(Level.FINE, "Exception in afterBegin()", ex);
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new NoSuchObjectLocalException(
         "NoSuchEntityException thrown by ejbLoad, EJB instance discarded", ex);
            } catch ( Exception ex ) {
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new EJBException(ex);
            }
            
            context.setNewlyActivated(false);
        }
    }
    
    // Called from BaseContainer.SyncImpl.beforeCompletion, postInvokeNoTx
    protected void beforeCompletion(EJBContextImpl ctx) {
        EntityContextImpl context = (EntityContextImpl)ctx;
        if ( context.isInState(BeanState.DESTROYED) ) {
            return;
        }
        
        // Call ejbStore as required by diagram in EJB2.0 section 10.9.4
        // home methods, finders and remove dont need ejbStore
        if ( (!containerStateManager.isNullEJBObject(context) 
                  && !containerStateManager.isRemovedEJBObject(context))
             || (!containerStateManager.isNullEJBLocalObject(context) 
                  && !containerStateManager.isRemovedEJBLocalObject(context)) ) {
            if ( context.isDirty() ) {
                enlistResourcesAndStore(context);
            }
        }
    }
    
    
    // Called from beforeCompletion and preFind
    private void enlistResourcesAndStore(EntityContextImpl context) {
        EntityBean e = (EntityBean)context.getEJB();
        // NOTE : Use EjbInvocation instead of ComponentInvocation since
        // the context is available.  It is needed in case ejbStore/ejbLoad
        // makes use of EJB timer service in order to perform operations allowed
        // checks
        EjbInvocation inv = super.createEjbInvocation(e, context);
        invocationManager.preInvoke(inv);
        
        try {
            transactionManager.enlistComponentResources();
            
            callEJBStore(e, context);
            
        } catch ( NoSuchEntityException ex ) {
            // Error during ejbStore, so discard bean: EJB2.0 18.3.3
            forceDestroyBean(context);
            
            throw new NoSuchObjectLocalException(
        "NoSuchEntityException thrown by ejbStore, EJB instance discarded", ex);
        } catch ( Exception ex ) {
            // Error during ejbStore, so discard bean: EJB2.0 18.3.3
            forceDestroyBean(context);
            throw new EJBException(ex);
        } finally {
            invocationManager.postInvoke(inv);
        }
    }
    
    
    // Called from BaseContainer.SyncImpl.afterCompletion
    // at the end of a transaction.
    // Note: this can be called possibly asynchronously because
    // of transaction timeout
    // Note: this can be called before releaseContext (if container
    // completed the tx in BaseContainer.postInvokeTx), or it can
    // be called after releaseContext (if client completed the tx after
    // getting reply from bean). So whatever is done here *MUST* be
    // consistent with releaseContext, and the bean should end up in
    // the correct state.
    protected void afterCompletion(EJBContextImpl ctx, int status) {
        EntityContextImpl context = (EntityContextImpl)ctx;
        if ( context.isInState(BeanState.DESTROYED) ) {
            return;
        }

        if (super.isUndeployed()) {
	    transactionManager.componentDestroyed(ctx);
	    return;
	}

        // home methods, finders and remove dont need this
        if ( !containerStateManager.isRemovedEJBObject(context)
             || !containerStateManager.isRemovedEJBLocalObject(context) ) {
            // Remove bean from ActiveTxCache table if its there.
            // No need to remove it from txBeanTable because the table
            // gets updated in ContainerFactoryImpl.removeContainerSync.

	    //removeIncompleteTxEJB(context, false);
            
            context.setTransaction(null);
            context.setLastTransactionStatus(status);

            context.setCascadeDeleteAfterSuperEJBRemove(false);
            context.setCascadeDeleteBeforeEJBRemove(false);
            
            // Move context to ready state if tx commited, else to pooled state
            if ( !context.isInState(BeanState.INVOKING) ) {
                if ( (status == Status.STATUS_COMMITTED)
                     || (status == Status.STATUS_NO_TRANSACTION) ) {
                    addReadyEJB(context);
                } else {
                    passivateAndPoolEJB(context);
                }
            }
        } else if (containerStateManager.isNullEJBObject(context) && 
                containerStateManager.isNullEJBLocalObject(context)) {
            // This happens if an ejbcreate has an exception, in that case
            // we remove bean from ActiveTxCache table if its there.
            // and return it to the pool
            //removeIncompleteTxEJB(context, false);
            
            context.setTransaction(null);
            context.setLastTransactionStatus(status);

            context.setCascadeDeleteAfterSuperEJBRemove(false);
            context.setCascadeDeleteBeforeEJBRemove(false);
            
            if ( !context.isInState(BeanState.INVOKING) ) {
                addPooledEJB(context);
            }
	} else if ( containerStateManager.isRemovedEJBObject(context)
	    || containerStateManager.isRemovedEJBLocalObject(context) ) 
	{
	    //removeIncompleteTxEJB(context, false);
	    context.setTransaction(null);
	    context.setLastTransactionStatus(status);

	    if (context.isInState(BeanState.INCOMPLETE_TX)) {
		addPooledEJB(context);
	    }
	}
    }
    
    
    // Called from BaseContainer just before invoking a business method
    // whose tx attribute is TX_NEVER / TX_NOT_SUPPORTED / TX_SUPPORTS without
    // a client tx.
    @Override
    protected void preInvokeNoTx(EjbInvocation inv) {
        EntityContextImpl context = (EntityContextImpl)inv.context;
        
        if ( context.isInState(BeanState.DESTROYED) ) {
            return;
        }
        
        if ( context.isNewlyActivated() && 
            !inv.invocationInfo.isCreateHomeFinder ) {
            // follow EJB2.0 section 12.1.6.1
            EntityBean e = (EntityBean)context.getEJB();
            try {
                callEJBLoad(e, context, false);
            } catch ( NoSuchEntityException ex ) {
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new NoSuchObjectLocalException(
         "NoSuchEntityException thrown by ejbLoad, EJB instance discarded", ex);
            } catch ( Exception ex ) {
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new EJBException(ex);
            }
            
            context.setNewlyActivated(false);
        }
    }
    
    // Called from BaseContainer after invoking a method with tx attribute
    // NotSupported or Never or Supports without client tx.
    @Override
    protected void postInvokeNoTx(EjbInvocation inv) {
        // This calls ejbStore to allow bean to flush any state to database.
        // This is also sufficient for compliance with EJB2.0 section 12.1.6.1
        // (ejbStore must be called between biz method and ejbPassivate).
        beforeCompletion((EJBContextImpl)inv.context);
    }
    
    @Override
    protected void adjustInvocationInfo(InvocationInfo invInfo, Method method, int txAttr,
                                                boolean flushEnabled,
                                                String methodIntf,
                                                Class originalIntf)
            throws EJBException {

        invInfo.isHomeFinder = isHomeFinder(method);
    }

    // Check if a method is a finder / home method.
    // Note: this method object is of the EJB's remote/home/local interfaces,
    // not the EJB class.
    private final boolean isHomeFinder(Method method) {
        Class methodClass = method.getDeclaringClass();
        if ( isRemote ) {
            if ( (hasRemoteHomeView &&
                  methodClass.isAssignableFrom(homeIntf))
                 && (methodClass != EJBHome.class)
                 && (!method.getName().startsWith("create")) ) {
                return true;
            }
        }
        if ( isLocal ) {
            // No need to check LocalBusiness view b/c home/finder methods
            // only apply to entity beans.
            if ( (hasLocalHomeView &&
                  methodClass.isAssignableFrom(localHomeIntf))
                 && (methodClass != EJBLocalHome.class)
                 && (!method.getName().startsWith("create")) ) {
                return true;
            }
        }

        return false;
    }

    // CacheListener interface
    public void trimEvent(Object primaryKey, Object context) {
        synchronized (asyncTaskSemaphore) {
            passivationCandidates.add(context);
            if (addedASyncTask == true) {
                return;
            }
            addedASyncTask = true;
        }
        
        try {
            ASyncPassivator work = new ASyncPassivator();
           ejbContainerUtilImpl.addWork(work);
        } catch (Exception ex) {
            addedASyncTask = false;
            _logger.log(Level.WARNING, "entitybean.container.add_cleanup_task_error",ex);
        }
    }
    
    private class ASyncPassivator
        implements Runnable
    {
        
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = 
                currentThread.getContextClassLoader();
            final ClassLoader myClassLoader = loader;
            
            try {
                //We need to set the context class loader for this 
                //(deamon) thread!!      
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(myClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(myClassLoader);
                            return null;
                        }
                    }
                    );
                }
                
                ComponentContext ctx = null;
                do {
                    synchronized (asyncTaskSemaphore) {
                        int sz = passivationCandidates.size() - 1;
                        if (sz > 0) {
                            ctx = 
                          (ComponentContext) passivationCandidates.remove(sz-1);
                        } else {
                            return;
                        }
                    }
                    
                    if (ctx != null) {
                        passivateEJB(ctx);
			totalPassivations++;
                    }
                } while (ctx != null);
            } catch (Throwable th) {
		totalPassivationErrors++;
                th.printStackTrace();
            } finally {
                synchronized (asyncTaskSemaphore) {
                    addedASyncTask = false;
                }
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(previousClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(previousClassLoader);
                            return null;
                        }
                    }
                    );
                }
            }
        }
    }
    
    // Called from AbstractCache 
    protected boolean passivateEJB(ComponentContext ctx) {
        if (containerState != CONTAINER_STARTED) {
            return false;
        }
        
        EntityContextImpl context = (EntityContextImpl)ctx;
        
        if (!context.isInState(BeanState.READY)) {
            return false;
        }
        
        if(_logger.isLoggable(Level.FINEST)) {
            _logger.log(Level.FINEST,"EntityContainer.passivateEJB(): context = (" +
                ctx + ")");
        }
        EntityBean ejb = (EntityBean)context.getEJB();
        
        EjbInvocation inv = super.createEjbInvocation(ejb, context);
        inv.method = ejbPassivateMethod;
        
        Object pkey = context.getPrimaryKey();
        boolean wasPassivated = false;
        
        // check state after locking ctx
        if ( !context.isInState(BeanState.READY) )
            return false;
        try {
            invocationManager.preInvoke(inv);
            
            // remove EJB from readyStore
            removeContextFromReadyStore(pkey, context);
            
            // no Tx needed for ejbPassivate
            ejb.ejbPassivate();
            
            wasPassivated = true;
        } catch ( Exception ex ) {
            _logger.log(Level.FINE, "Exception in passivateEJB()", ex);
            // Error during ejbStore/Passivate, discard bean: EJB2.0 18.3.3
            forceDestroyBean(context);
            return false;
        } finally {
            invocationManager.postInvoke(inv);
        }
        
        // Remove the ejbObject/LocalObject from ejbObject/LocalObjectStore
        // If a future EjbInvocation arrives for them, they'll get recreated.
        if ( isRemote ) {
            removeEJBObjectFromStore(pkey);
        }
        if ( isLocal ) {
            ejbLocalObjectStore.remove(pkey);
        }
        
        // Note: ejbStore and ejbPassivate need the primarykey
        // so we should dissociate the context from EJBObject only
        // after calling ejbStore and ejbPassivate.
        synchronized (context) {
            addPooledEJB(context);
        }
        
        return wasPassivated;
    }
    
    
    
    /***************************************************************************
     * The following are private methods for implementing internal logic
     * for lifecyle and state management, in a reusable way.
     **************************************************************************/
    
    
    // called from postCreate, postFind,
    // getEJBLocalObjectForPrimaryKey, removeBean
    protected EJBLocalObjectImpl internalGetEJBLocalObjectImpl
        (Object primaryKey) {
        return internalGetEJBLocalObjectImpl(primaryKey, false, 
                                             defaultCacheEJBO);
    }
    
    protected EJBLocalObjectImpl internalGetEJBLocalObjectImpl
        (Object primaryKey, boolean incrementRefCount)
    {
        return internalGetEJBLocalObjectImpl(primaryKey, incrementRefCount, 
            defaultCacheEJBO);
    }
    
    protected EJBLocalObjectImpl internalGetEJBLocalObjectImpl
        (Object primaryKey, boolean incrementRefCount, boolean cacheEJBO) {
        // check if the EJBLocalObject exists in the store.
        try {
            EJBLocalObjectImpl localObjImpl = (EJBLocalObjectImpl)
                ejbLocalObjectStore.get(primaryKey, incrementRefCount);
            if ( localObjImpl == null ) {

                // and associate the EJBLocalObjectImpl with the primary key
                localObjImpl = instantiateEJBLocalObjectImpl(primaryKey);
                
                // add the EJBLocalObjectImpl to ejbLocalObjectStore
                if (incrementRefCount || cacheEJBO) {
                    ejbLocalObjectStore.put(primaryKey, localObjImpl, 
                        incrementRefCount);
                }
            }
            return localObjImpl;
        } catch ( Exception ex ) {
            _logger.log(Level.SEVERE,"entitybean.container.get_ejb_local_object_exception",
                        logParams);
            _logger.log(Level.SEVERE,"",ex);
            throw new EJBException(ex);
        }
    }
    
    // called from postFind, getEJBObjectForPrimaryKey, 
    // EntityContextImpl.getEJBObject()
    EJBObject getEJBObjectStub(Object primaryKey, byte[] streamKey) {	
        // primary key cant be null, streamkey may be null

        // check if the EJBObject exists in the store.
        try {
            EJBObjectImpl ejbObjImpl = 
                (EJBObjectImpl) ejbObjectStore.get(primaryKey);
            if ( (ejbObjImpl != null) && (ejbObjImpl.getStub() != null) ) {
                return (EJBObject) ejbObjImpl.getStub();
            }

            // create a new stub without creating the EJBObject itself
            if ( streamKey == null ) {
                streamKey = EJBUtils.serializeObject(primaryKey, false);
            }
            EJBObject ejbStub = (EJBObject)
                remoteHomeRefFactory.createRemoteReference(streamKey);
                                                           
            return ejbStub;
        } catch ( Exception ex ) {
            _logger.log(Level.FINE,"", ex);
            throw new EJBException(ex);
        }
    }

    // called from getEJBObject, postCreate, removeBean,
    //             postFind, getEJBObjectForPrimaryKey
    private EJBObjectImpl internalGetEJBObjectImpl(Object primaryKey, 
                                                     byte[] streamKey) {
        return internalGetEJBObjectImpl(primaryKey, streamKey, false, 
                                        defaultCacheEJBO);
    }
    
    private EJBObjectImpl internalGetEJBObjectImpl(Object primaryKey, 
            byte[] streamKey, boolean incrementRefCount)
    {
        return internalGetEJBObjectImpl
            (primaryKey, streamKey, incrementRefCount, defaultCacheEJBO);
    }
    
    
    // called from getEJBObject, postCreate, postFind,
    // getEJBObjectForPrimaryKey, removeBean
    private EJBObjectImpl internalGetEJBObjectImpl(Object primaryKey,
        byte[] streamKey, boolean incrementRefCount, boolean cacheEJBO) {
        // primary key cant be null, streamkey may be null
        
        // check if the EJBContext/EJBObject exists in the store.
        try {
            
            EJBObjectImpl ejbObjImpl = (EJBObjectImpl) 
                ejbObjectStore.get(primaryKey, incrementRefCount);

            if ( (ejbObjImpl != null) && (ejbObjImpl.getStub() != null) ) {
                return ejbObjImpl;
            }
            
            // check if the EJBContext/EJBObject exists in threadlocal
            // This happens if ejbo is in the process of being created.
            // This is necessary to prevent infinite recursion
            // because PRO.narrow calls is_a which calls the
            // ProtocolMgr which calls getEJBObject.
            ejbObjImpl = (EJBObjectImpl) ejbServant.get();
            if ( ejbObjImpl != null ) {
                return ejbObjImpl;
            }
            
            // set ejbo in thread local to help recursive calls find the ejbo
            ejbServant.set(ejbObjImpl);
            
            // "Connect" the EJBObject to the Protocol Manager
            
            if ( streamKey == null ) {
                streamKey = EJBUtils.serializeObject(primaryKey, false);
            }
            EJBObject ejbStub = (EJBObject)
                remoteHomeRefFactory.createRemoteReference(streamKey);
                                                           
            // create the EJBObject and associate it with the stub
            // and the primary key
            ejbObjImpl = instantiateEJBObjectImpl(ejbStub, primaryKey);
            
            ejbServant.set(null);
            
            if ((incrementRefCount || cacheEJBO)) {
                EJBObjectImpl ejbo1 = 
                    (EJBObjectImpl) ejbObjectStore.put(primaryKey, ejbObjImpl, 
                        incrementRefCount);
                if ((ejbo1 != null) && (ejbo1 != ejbObjImpl)) {
                    remoteHomeRefFactory.destroyReference(ejbObjImpl.getStub(), 
                                                      ejbObjImpl);
                    ejbObjImpl = ejbo1;
                }
            }
            
            return ejbObjImpl;
        }
        catch ( Exception ex ) {
            _logger.log(Level.FINE, "entitybean.container.get_ejb_context_exception", logParams);
            _logger.log(Level.FINE,"",ex);
            throw new EJBException(ex);
        }
    } //internalGetEJBObject(..)
    
    
    // called from getContext and getReadyEJB
    protected EntityContextImpl getPooledEJB() {
        try {
            return (EntityContextImpl) entityCtxPool.getObject(true, null);
        } catch (com.sun.ejb.containers.util.pool.PoolException inEx) {
            throw new EJBException(inEx);
        }
    }
    
    // called from passivateAndPoolEJB, releaseContext, passivateEJB
    // Note: addPooledEJB is idempotent: i.e. even if it is called multiple
    // times with the same context, the context is added only once.
    protected void addPooledEJB(EntityContextImpl context) {
        if ( context.isInState(BeanState.POOLED) ) {
            return;
        }
        // we're sure that no concurrent thread can be using this
        // context, so no need to synchronize.
        containerStateManager.clearContext(context);
        context.setState(BeanState.POOLED);
        context.clearCachedPrimaryKey();
        
        //context.cacheEntry = null;
        entityCtxPool.returnObject(context);
        
    }
    
    // called from addReadyEJB and afterCompletion
    protected void passivateAndPoolEJB(EntityContextImpl context) {
        if ( context.isInState(BeanState.DESTROYED) || context.isInState(BeanState.POOLED) )
            return;
        
        // if ( context.isPooled() ) {
        // context.isPooled(false);
        // return;
        // }
        EntityBean ejb = (EntityBean) context.getEJB();
        synchronized ( context ) {
            EjbInvocation inv = super.createEjbInvocation(ejb, context);
            inv.method = ejbPassivateMethod;
            invocationManager.preInvoke(inv);
            
            try {
                ejb.ejbPassivate();
            } catch ( Exception ex ) {
                _logger.log(Level.FINE,"Exception in passivateAndPoolEJB()",ex);
                forceDestroyBean(context);
                return;
            } finally {
                invocationManager.postInvoke(inv);
            }
            
            // remove EJB(Local)Object from ejb(Local)ObjectStore
            
            
            Object primaryKey = context.getPrimaryKey();
            if ( isRemote ) {
                removeEJBObjectFromStore(primaryKey);
            }
            if ( isLocal ) {
                ejbLocalObjectStore.remove(primaryKey);
            }
            
            addPooledEJB(context);
        }
    }
    
    
    /**
     * Called from getContext and getEJBWithIncompleteTx
     * Get an EJB in the ready state (i.e. which is not doing any
     * invocations and doesnt have any incomplete Tx), for the
     * ejbObject provided in the EjbInvocation.
     * Concurrent invocations should get *different* instances.
     */
    protected EntityContextImpl activateEJBFromPool(Object primaryKey, 
        EjbInvocation inv) {
        EntityContextImpl context = null;
        // get a pooled EJB and activate it.
        context = getPooledEJB();
        
        // we're sure that no concurrent thread can be using this
        // context, so no need to synchronize.
        
        // set EJBObject/LocalObject for the context
        if ( inv.isLocal ) {
            EJBLocalObjectImpl localObjImpl = 
                internalGetEJBLocalObjectImpl(primaryKey, true);
            containerStateManager.attachObject(inv, context, null, localObjImpl);
            // No need to create/set EJBObject if this EJB isRemote too.
		    // This saves remote object creation overhead.
		    // The EJBObject and stub will get created lazily if needed
		    // when EntityContext.getEJBObjectImpl is called.
        } else { // remote EjbInvocation
            EJBObjectImpl ejbObjImpl = 
                internalGetEJBObjectImpl(primaryKey, null, true);
            containerStateManager.attachObject(inv, context, ejbObjImpl, null);
            
            if ( isLocal ) {
                // Create EJBLocalObject so EntityContext methods work
                containerStateManager.attachObject(inv, context, null, 
                        internalGetEJBLocalObjectImpl(primaryKey, true));
            }
        }
        
        context.setState(BeanState.READY);
        
        EntityBean ejb = (EntityBean)context.getEJB();
        
        EjbInvocation inv2 = super.createEjbInvocation(ejb, context);
        inv2.method = ejbActivateMethod;
        invocationManager.preInvoke(inv2);
        
        try {
            ejb.ejbActivate();
            
            // Note: ejbLoad will be called during preInvokeTx
            // since this EJB instance is being associated with
            // a Tx for the first time.
            
        } catch ( Exception ex ) {
            // Error during ejbActivate, discard bean: EJB2.0 18.3.3
            forceDestroyBean(context);
            throw new EJBException(ex);
        } finally {
            invocationManager.postInvoke(inv2);
        }
        
        context.setNewlyActivated(true);
        //recycler.initSoftRef(context);
        
        afterNewlyActivated(context);
        
        return context;
    } //getReadyEJB(inv)
    
    
    // called from releaseContext, afterCompletion
    
    
    /**
     * Get an EJB instance for this EJBObject and current client Tx
     * Called only from getContext.
     * Return null if there no INCOMPLETE_TX bean for the pkey & tx.
     */
    private EntityContextImpl getEJBWithIncompleteTx(EjbInvocation inv) {
        // We need to make sure that two concurrent client
        // invocations with same primary key and same client tx
        // get the SAME EJB instance.
        // So we need to maintain exactly one copy of an EJB's state
        // per transaction.
        
        JavaEETransaction current = null;
        try {
            current = (JavaEETransaction) transactionManager.getTransaction();
        } catch ( SystemException ex ) {
            throw new EJBException(ex);
        }
        
        EntityContextImpl ctx = null;
	if (current != null) {
	    ActiveTxCache activeTxCache = (ActiveTxCache) 
		ejbContainerUtilImpl.getActiveTxCache(current);
	    ctx = (activeTxCache == null)
		    ? null : activeTxCache.get(this, getInvocationKey(inv));
        inv.foundInTxCache = (ctx != null);
	}
	
	return ctx;
    }
    
    
    /**
     * Called only from afterBegin.
     * This EJB is invoked either with client's tx (in which case
     * it would already be in table), or with new tx (in which case
     * it would not be in table).
     */
    private void addIncompleteTxEJB(EntityContextImpl context) {
    	JavaEETransaction current = (JavaEETransaction) context.getTransaction();
        if ( current == null ) {
            return;
        }
        if ( (containerStateManager.isNullEJBObject(context)) &&
             (containerStateManager.isNullEJBLocalObject(context)) ) {
            return;
        }

        // Its ok to add this context without checking if its already there.
	ActiveTxCache activeTxCache = (ActiveTxCache) ejbContainerUtilImpl.getActiveTxCache(current);
	if (activeTxCache == null) {
	    activeTxCache = new ActiveTxCache(DEFAULT_TX_CACHE_BUCKETS);
	    ejbContainerUtilImpl.setActiveTxCache(current, activeTxCache);
	}

	activeTxCache.add(context);
        
        Vector beans = ejbContainerUtilImpl.getBeans(current);
        beans.add(context);
    }
    
    /**
     * Called from releaseContext if ejb is removed, from afterCompletion,
     * and from passivateEJB.
     */
    protected void removeIncompleteTxEJB(EntityContextImpl context,
                                         boolean updateTxBeanTable) {
        JavaEETransaction current = (JavaEETransaction) context.getTransaction();

        if (current == null) {
            return;
        }
        if ( (containerStateManager.isNullEJBObject(context)) &&
             (containerStateManager.isNullEJBLocalObject(context)) ) {
            return;
        }
        
	ActiveTxCache activeTxCache = (ActiveTxCache) ejbContainerUtilImpl.getActiveTxCache(current);
	if (activeTxCache != null) {
	    activeTxCache.remove(this, context.getPrimaryKey());
	}

        if ( updateTxBeanTable ) {
            Vector beans = ejbContainerUtilImpl.getBeans(current);
            beans.remove(context); // this is a little expensive...
        }
    }
    
    /**
     * a TimerTask class to trim a given cache of timedout entries
     */
    private class IdleBeansPassivator
            extends java.util.TimerTask {
        Cache cache;
        
        IdleBeansPassivator(Cache cache) {
            this.cache = cache;
        }
        
        public void run() {
            if (timerValid) {
                cache.trimExpiredEntries(Integer.MAX_VALUE);
            }
        }

	public boolean cancel() {
	    cache = null;
	    return super.cancel();
	}
    }
    
    
    // Key for INCOMPLETE_TX beans which contains ejbObject + Tx
    private static class EJBTxKey {
        
        Transaction  tx; // may be null
        Object       primaryKey;
        int          pkHashCode;
        
        EJBTxKey(Object primaryKey, Transaction tx) {
            this.tx = tx;
            this.primaryKey = primaryKey;
            this.pkHashCode = primaryKey.hashCode();
        }
        
        public final int hashCode() {
            // Note: this hashcode need not be persistent across
            // activations of this process.
            // Return the primaryKey's hashCode. The Hashtable will
            // then search for the Tx through the bucket for
            // the primaryKey's hashCode.
            
            //return primaryKey.hashCode();
            return pkHashCode;
        }
        
        public final boolean equals(Object obj) {
            if ( !(obj instanceof EJBTxKey) ) {
                return false;
            }
            EJBTxKey other = (EJBTxKey) obj;
            try {
                // Note: tx may be null if the EJB is not associated with
                // an incomplete Tx.
                if ( primaryKey.equals(other.primaryKey) ) {
                    if ( (tx == null) && (other.tx == null) ) {
                        return true;
                    } else if ( (tx != null) && (other.tx != null)
                                && tx.equals(other.tx) ) {
                        return true;
                    } else  {
                        return false;
                    }
                } else {
                    return false;
                }
            } catch ( Exception ex ) {
                _logger.log(Level.FINE, "Exception in equals()", ex);
                return false;
            }
        }
        
    }
    
    protected static class CacheProperties {
        
        int maxCacheSize ;
        int numberOfVictimsToSelect ;
        int cacheIdleTimeoutInSeconds ;

        public CacheProperties(EntityContainer entityContainer) {
            numberOfVictimsToSelect = 
                Integer.parseInt(entityContainer.ejbContainer.getCacheResizeQuantity());
            maxCacheSize = Integer.parseInt(entityContainer.ejbContainer.getMaxCacheSize());
            cacheIdleTimeoutInSeconds = 
                Integer.parseInt(entityContainer.ejbContainer.getCacheIdleTimeoutInSeconds());

            if(entityContainer.beanCacheDes != null) {
                int temp = 0;
                if((temp = entityContainer.beanCacheDes.getResizeQuantity()) != -1) {
                    numberOfVictimsToSelect = temp;
                }
                
                if((temp = entityContainer.beanCacheDes.getMaxCacheSize()) != -1) {
                    maxCacheSize = temp;
                }
                
                if ((temp = entityContainer.beanCacheDes.getCacheIdleTimeoutInSeconds()) != -1)
                {
                    cacheIdleTimeoutInSeconds = temp;
                }
            }
        }
    } //CacheProperties
    
    private static class PoolProperties {
        int maxPoolSize;
        int poolIdleTimeoutInSeconds;
        //    	int maxWaitTimeInMillis;
        int poolResizeQuantity;
        int steadyPoolSize;
        
        public PoolProperties(EntityContainer entityContainer) {
            
            maxPoolSize = Integer.parseInt(entityContainer.ejbContainer.getMaxPoolSize());
            poolIdleTimeoutInSeconds = Integer.parseInt(
                entityContainer.ejbContainer.getPoolIdleTimeoutInSeconds());
            poolResizeQuantity = Integer.parseInt(
                entityContainer.ejbContainer.getPoolResizeQuantity());
            steadyPoolSize = Integer.parseInt(
                entityContainer.ejbContainer.getSteadyPoolSize());
            if(entityContainer.beanPoolDes != null) {
                int temp = 0;
                if ((temp = entityContainer.beanPoolDes.getMaxPoolSize()) != -1) {
                    maxPoolSize = temp;
                }
                if ((temp = entityContainer.beanPoolDes.getPoolIdleTimeoutInSeconds()) != -1) {
                    poolIdleTimeoutInSeconds = temp;
                }
                if ((temp = entityContainer.beanPoolDes.getPoolResizeQuantity()) != -1) {
                    poolResizeQuantity = temp;
                }
                if ((temp = entityContainer.beanPoolDes.getSteadyPoolSize()) != -1) {
                    steadyPoolSize = temp;
                }
            }
        }
    } //PoolProperties
    
    
    protected boolean isIdentical(EJBObjectImpl ejbObjImpl, EJBObject other)
            throws RemoteException {
        if ( other == ejbObjImpl.getStub() ) {
            return true;
        } else {
            try {
                // EJBObject may be a remote object.
                // Compare homes. See EJB2.0 spec section 9.8.
                if ( !getProtocolManager().isIdentical(ejbHomeStub,
                                              other.getEJBHome()))
                    return false;
                
                // Compare primary keys.
                if (!ejbObjImpl.getPrimaryKey().equals(other.getPrimaryKey())) {
                    return false;
                }
                
                return true;
            } catch ( Exception ex ) {
                _logger.log(Level.INFO, "entitybean.container.ejb_comparison_exception",
                            logParams);
                _logger.log(Level.INFO, "", ex);
                throw new RemoteException("Exception in isIdentical()", ex);
            }
        }
    }
    
    
    protected void callEJBLoad(EntityBean ejb, EntityContextImpl context,
                               boolean activeTx)
        throws Exception
    {
        try {
            context.setInEjbLoad(true);
            ejb.ejbLoad();
            // Note: no need to do context.setDirty(false) because ejbLoad is
            // called immediately before a business method.
        } catch(Exception e) {
            throw e;
        } finally {
            context.setInEjbLoad(false);
        }
    }
    
    protected void callEJBStore(EntityBean ejb, EntityContextImpl context)
        throws Exception
    {
        try {
            context.setInEjbStore(true);
            ejb.ejbStore();
        } finally {
            context.setInEjbStore(false);
            context.setDirty(false); // bean's state is in sync with DB
        }
    }
    
    protected void callEJBRemove(EntityBean ejb, EntityContextImpl context)
            throws Exception {
        Exception exc = null;
        try {
            // TODO - check if it is needed: context.setInEjbRemove(true);
            ejb.ejbRemove();
        } catch ( Exception ex ) {
            exc = ex;
            throw ex;
        } finally {
            // TODO - check if it is needed: context.setInEjbRemove(false);
            context.setDirty(false); // bean is removed so doesnt need ejbStore
            /* TODO
            if ( AppVerification.doInstrument() ) {
                AppVerification.getInstrumentLogger().doInstrumentForEjb(
                ejbDescriptor, ejbRemoveMethod, exc);
            }
            */
        }
    }
     
    protected void doTimerInvocationInit(EjbInvocation inv, Object primaryKey)
            throws Exception {
        if( isRemote ) {
            inv.ejbObject = internalGetEJBObjectImpl(primaryKey, null);
            inv.isRemote = true;
        } else {
            inv.ejbObject = internalGetEJBLocalObjectImpl(primaryKey);
            inv.isLocal = true;
        }
        if( inv.ejbObject == null ) {
            throw new Exception("Timed object identity (" + primaryKey +
                " ) no longer exists " );
        }
    }
    
    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {
        
        String ejbName = ejbDescriptor.getName();
        
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"[EntityContainer]: Undeploying " + ejbName +
                " ...");
        } 
        // destroy all EJBObject refs
        
        try {
            Iterator elements = ejbObjectStore.values();
            while ( elements.hasNext() ) {
                EJBObjectImpl ejbObjImpl = (EJBObjectImpl) elements.next();
                try {
                    if ( isRemote ) {
                        remoteHomeRefFactory.destroyReference
                            (ejbObjImpl.getStub(), ejbObjImpl.getEJBObject());
                                                    
                    }
                } catch ( Exception ex ) {
                    _logger.log(Level.FINE, "Exception in undeploy()", ex);
                }
            }
            
            ejbObjectStore.destroy();  //store must set the listern to null
            ejbObjectStore = null;
            
            ejbLocalObjectStore.destroy(); //store must set the listern to null
            ejbLocalObjectStore = null;
            
            // destroy all EJB instances in readyStore
            destroyReadyStoreOnUndeploy(); //cache must set the listern to null
            
            entityCtxPool.close();
            poolProbeListener.unregister();
            if (cacheProbeListener != null) {
                cacheProbeListener.unregister();
            }
            
            // stops the idle bean passivator and also removes the link
            // to the cache; note that cancel() method of timertask
            // does not remove the task from the timer's queue
            if (idleBeansPassivator != null) {
                try {
                    idleBeansPassivator.cancel();
                } catch (Exception e) {
                    _logger.log(Level.FINE,
                                "[EntityContainer] cancelTimerTask: ", e);
                }
                this.idleBeansPassivator.cache  = null;
            }
	    cancelTimerTasks();
        }
        finally {
            
            // helps garbage collection
            this.ejbObjectStore         = null;
            this.ejbLocalObjectStore    = null;
            this.passivationCandidates  = null;
            this.readyStore             = null;
            this.entityCtxPool          = null;
            this.iased                  = null;
            this.beanCacheDes           = null;
            this.beanPoolDes            = null;
            this.ejbContainer           = null;
            this.cacheProp              = null;
            this.poolProp               = null;
            this.asyncTaskSemaphore     = null;
            this.idleBeansPassivator    = null;
            
        }
        
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE," [EntityContainer]: Successfully Undeployed " +
                ejbName);
        }
    }
    
    protected void afterNewlyActivated(EntityContextImpl context) {
        //Noop for EntityContainer
    }
    
    protected EntityContextImpl createEntityContextInstance(EntityBean ejb,
            EntityContainer entityContainer)
    {
        return new EntityContextImpl(ejb, entityContainer);
    }
    
    private class EntityContextFactory
        implements ObjectFactory
    {
        private EntityContainer entityContainer;
        
        public EntityContextFactory(EntityContainer entityContainer) {
            this.entityContainer = entityContainer;
        }
        
        public Object create(Object param) {
            EntityContextImpl entityCtx = null;
            EjbInvocation ejbInv = null;
            try {
                // Create new bean. The constructor is not allowed
                // to do a JNDI access (see EJB2.0 section 10.5.5),
                // so no need to call invocationMgr before instantiation.
                EntityBean ejb = (EntityBean) ejbClass.newInstance();
                
                // create EntityContext
                entityCtx = createEntityContextInstance(ejb, entityContainer);

                ejbInv = entityContainer.createEjbInvocation(ejb, entityCtx);
                invocationManager.preInvoke(ejbInv);
                
                // setEntityContext may be called with or without a Tx
                // spec 9.4.2
                ejb.setEntityContext(entityCtx);

                // NOTE : Annotations are *not* supported for entity beans
                // so we do not invoke the injection manager for this instance.

            } catch (Exception ex ) {
                throw new EJBException("Could not create Entity EJB", ex);
            } finally {
                if ( ejbInv != null ) {
                    invocationManager.postInvoke(ejbInv);
                }
            }
            
            entityCtx.touch();
            return entityCtx;
        }
        
        
        public void destroy(Object object) {
            if (object == null) {
                //means that this is called through forceDestroyBean
                //So no need to anything, as we cannot call unsetEntityCtx etc..
                return;
            }
            
            EntityContextImpl context = (EntityContextImpl) object;
            EntityBean ejb = (EntityBean)context.getEJB();
            if (!context.isInState(BeanState.DESTROYED)) {
                EjbInvocation ci = entityContainer.createEjbInvocation(ejb, context);
                invocationManager.preInvoke(ci);
                
                // kill the bean and let it be GC'ed
                try {
                    synchronized ( context ) {
                        containerStateManager.clearContext(context);
                        context.setState(BeanState.DESTROYED);
                        //context.cacheEntry = null;
                        context.setInUnsetEntityContext(true);
                        
                        try {
                            ejb.unsetEntityContext();
                        } catch ( Exception ex ) {
                            _logger.log(Level.FINE, 
                                "Exception in ejb.unsetEntityContext()", ex);
                        }
                        
                        // tell the TM to release resources held by the bean
                        transactionManager.componentDestroyed(context);
                    }
                } finally {
                    invocationManager.postInvoke(ci);
                }
            } else {
                //Called from forceDestroyBean
                try {
                    synchronized ( context ) {
                        containerStateManager.clearContext(context);
                        context.setState(BeanState.DESTROYED);
                        //context.cacheEntry = null;
                        
                        // mark the context's transaction for rollback
                        Transaction tx = context.getTransaction();
                        if ( tx != null && tx.getStatus() != 
                            Status.STATUS_NO_TRANSACTION ) {
                            context.getTransaction().setRollbackOnly();
                        }
                        
                        // tell the TM to release resources held by the bean
                        transactionManager.componentDestroyed(context);
                        
                    }
                } catch (Exception ex) {
                    _logger.log(Level.FINE, "Exception in destroy()", ex);
                }
            }
        }
        
    } //class EntityContextFactory
    
    private void createCaches() throws Exception {

        cacheProp = new CacheProperties(this);
        
        int cacheSize = cacheProp.maxCacheSize;
        int numberOfVictimsToSelect = cacheProp.numberOfVictimsToSelect;
        float loadFactor = DEFAULT_LOAD_FACTOR;
        idleTimeout = cacheProp.cacheIdleTimeoutInSeconds * 1000L;
        
        createReadyStore(cacheSize, numberOfVictimsToSelect, loadFactor, 
                         idleTimeout);
        
        createEJBObjectStores(cacheSize, numberOfVictimsToSelect, 
                              idleTimeout);
        
    }
    
    protected void createReadyStore(int cacheSize, int numberOfVictimsToSelect,
            float loadFactor, long idleTimeout) throws Exception
    {
        idleTimeout = (idleTimeout <= 0) ? -1 : idleTimeout;
        if (cacheSize <= 0 && idleTimeout <= 0) {
            readyStore = new BaseCache();
            cacheSize = DEFAULT_CACHE_SIZE;
            readyStore.init(cacheSize, loadFactor, null);
        } else {
            cacheSize = (cacheSize <= 0) ? DEFAULT_CACHE_SIZE : cacheSize;
            LruCache lru = new LruCache(DEFAULT_CACHE_SIZE);
            if (numberOfVictimsToSelect >= 0) {
                loadFactor = (float) (1.0 - (1.0 *
                                             numberOfVictimsToSelect/cacheSize));
            }
            lru.init(cacheSize, idleTimeout, loadFactor, null);
            readyStore = lru;
            readyStore.addCacheListener(this);
        }
        
        if (idleTimeout > 0) {
            idleBeansPassivator = setupIdleBeansPassivator(readyStore);
        }
    }
    
    protected void createEJBObjectStores(int cacheSize,
        int numberOfVictimsToSelect, long idleTimeout) throws Exception {
        
        EJBObjectCache lru = null;
        String ejbName = ejbDescriptor.getName();
        idleTimeout = (idleTimeout <= 0) ? -1 : idleTimeout;
        
        if (cacheSize <= 0 && idleTimeout <= 0) {
            ejbObjectStore = new UnboundedEJBObjectCache(ejbName);
            ejbObjectStore.init(DEFAULT_CACHE_SIZE, numberOfVictimsToSelect, 0L, 
                (float)1.0, null);
            
            ejbLocalObjectStore = new UnboundedEJBObjectCache(ejbName);
            ejbLocalObjectStore.init(DEFAULT_CACHE_SIZE,
                numberOfVictimsToSelect, 0L, (float)1.0, null);
        } else {
            cacheSize = (cacheSize <= 0) ? DEFAULT_CACHE_SIZE : cacheSize;
            ejbObjectStore = new FIFOEJBObjectCache(ejbName);
            ejbObjectStore.init(cacheSize, numberOfVictimsToSelect, idleTimeout,
                (float)1.0, null);
            ejbObjectStore.setEJBObjectCacheListener(
                new EJBObjectCacheVictimHandler());
            
            ejbLocalObjectStore = new FIFOEJBObjectCache(ejbName);
            ejbLocalObjectStore.init(cacheSize, numberOfVictimsToSelect, 
                idleTimeout, (float)1.0, null);
            ejbLocalObjectStore.setEJBObjectCacheListener(
                new LocalEJBObjectCacheVictimHandler());
        }
        
        if (idleTimeout > 0) {
            idleEJBObjectPassivator = setupIdleBeansPassivator(ejbObjectStore);
            idleLocalEJBObjectPassivator = 
                setupIdleBeansPassivator(ejbLocalObjectStore);
        }
    }
    
    protected EntityContextImpl getReadyEJB(EjbInvocation inv) {
        Object primaryKey = getInvocationKey(inv);
        EntityContextImpl context = null;
        // Try and get an EJB instance for this primaryKey from the
        // readyStore
        context = (EntityContextImpl)readyStore.remove(primaryKey);
        if (context == null || !context.isInState(BeanState.READY)) {
            context = activateEJBFromPool(primaryKey, inv);
        }
        return context;
    } //getReadyEJB(inv)
    
    protected void addReadyEJB(EntityContextImpl context) {
        // add to the cache (can have multiple instances of beans per key)
        Object primaryKey = context.getPrimaryKey();
        context.setState(BeanState.READY);
        readyStore.add(primaryKey, context);
    }
    
    protected void destroyReadyStoreOnUndeploy() {
        if (readyStore == null) {
            return;
        }
        
        // destroy all EJB instances in readyStore
        synchronized ( readyStore ) {
            
            Iterator beans = readyStore.values();
            while ( beans.hasNext() ) {
                EJBContextImpl ctx = (EJBContextImpl)beans.next();
                transactionManager.componentDestroyed(ctx);
            }
        }
        readyStore.destroy();
        readyStore = null;
    }
    
    protected void removeContextFromReadyStore(Object primaryKey, 
        EntityContextImpl context) {
        readyStore.remove(primaryKey, context);
    }

    protected void addProxyInterfacesSetClass(Set proxyInterfacesSet, boolean local) {
        if( ejbDescriptor.getIASEjbExtraDescriptors().isIsReadOnlyBean() ) {
            if (local) {
                proxyInterfacesSet.add(ReadOnlyEJBLocalHome.class);
            } else {
                proxyInterfacesSet.add(ReadOnlyEJBHome.class);
            }
        }

    }

    protected void doFlush( EjbInvocation inv ) {
        if( !inv.invocationInfo.flushEnabled ||
            inv.exception != null )  {
            return;
        }

        if( !isContainerManagedPers ) {
            //NEED TO INTERNATIONALIZE THIS WARNING MESSAGE
            _logger.log(Level.WARNING, 
                "Cannot turn on flush-enabled-at-end-of-method for a bean with Bean Managed Persistence");
            return;
        }

        InvocationInfo invInfo = inv.invocationInfo;
        EntityContextImpl context = (EntityContextImpl)inv.context;
        Transaction tx = context.getTransaction();

        //Since postInvoke(Tx) has been called before the releaseContext, the transaction
        //could be committed or rolledback. In that case there is no point to call flush
        if( tx == null) {
            return;
        }

        //return w/o doing anything if the transaction is marked for rollback 
        try {
            if( context.getRollbackOnly() ) {
                return;
            }
        } catch( Throwable ex ) {
            _logger.log(Level.WARNING, "Exception when calling getRollbackOnly", ex);
            return;
        }

        if ( invInfo.isBusinessMethod ) {
            try {
                //Store the state of all the beans that are part of this transaction
                storeAllBeansInTx( tx );
            } catch( Throwable ex ) {
                inv.exception = ex;
                return;
            }
        }

        try {
            BeanStateSynchronization pmcontract = (BeanStateSynchronization)inv.ejb;
            pmcontract.ejb__flush();
        } catch( Throwable ex ) {
            //check the type of the method and create the corresponding exception
            if( invInfo.startsWithCreate ) {
                CreateException ejbEx = new CreateException();
                ejbEx.initCause(ex);
                inv.exception = ejbEx;
            } else if( invInfo.startsWithRemove ) {
                RemoveException ejbEx = new RemoveException();
                ejbEx.initCause(ex);
                inv.exception = ejbEx;
            } else {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(ex);
                inv.exception = ejbEx;
            }

            return;
        }

    } //doFlush(...)

    private void storeAllBeansInTx(Transaction tx) {
        // Call ejbStore on all entitybeans in tx for all EntityContainers
        Vector beans = ejbContainerUtilImpl.getBeans(tx);
        if ( beans.isEmpty() ) {
            // No beans associated with the current transaction
            return;
        }

        Iterator itr = beans.iterator();
        while ( itr.hasNext() ) {
            EntityContextImpl ctx = (EntityContextImpl)itr.next();
            if ( ctx.isInState(BeanState.INCOMPLETE_TX) && ctx.isDirty() ) {
                // Call ejbStore on the bean
                // Note: the bean may be in a different container instance
                EntityContainer cont = (EntityContainer)ctx.getContainer();
                cont.enlistResourcesAndStore(ctx);
            }
        }
    }


    protected class LocalEJBObjectCacheVictimHandler
        implements EJBObjectCacheListener, Runnable
    {
        
        protected Object lock = new Object();
        protected boolean addedTask = false;
        protected ArrayList keys = new ArrayList(16);
        
        protected LocalEJBObjectCacheVictimHandler() {
        }
        
        //EJBObjectCacheListener interface
        public void handleOverflow(Object key) {
            doCleanup(key);
        }
        
        public void handleBatchOverflow(ArrayList paramKeys) {
            int size = paramKeys.size();
            synchronized (lock) {
                for (int i=0; i<size; i++) {
                    keys.add(paramKeys.get(i));
                }
                if (addedTask == true) {
                    return;
                }
                addedTask = true;
            }
            
            try {
                ejbContainerUtilImpl.addWork(this);
            } catch (Exception ex) {
                _logger.log(Level.WARNING, "entitybean.container.entity_add_async_task", ex);
                synchronized (lock) {
                    addedTask = false;
                }
            }
        }
        
        public void run() {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader = 
                currentThread.getContextClassLoader();
            final ClassLoader myClassLoader = loader;
            
            try {
            //We need to set the context class loader for this (deamon) thread!!
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(myClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(myClassLoader);
                            return null;
                        }
                    }
                    );
                }
                
                ArrayList localKeys = null;
                do {
                    synchronized (lock) {
                        int size = keys.size();
                        if (size == 0) {
                            return;
                        }
                        
                        localKeys = keys;
                        keys = new ArrayList(16);
                    }
                    
                    int maxIndex = localKeys.size();
                    for (int i=0; i<maxIndex; i++) {
                        doCleanup(localKeys.get(i));
                    }
                } while (true);
                
            } catch (Throwable th) {
                th.printStackTrace();
            } finally {
                synchronized (lock) {
                    addedTask = false;
                }
                if(System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(previousClassLoader);
                } else {
                    java.security.AccessController.doPrivileged(
                            new java.security.PrivilegedAction() {
                        public java.lang.Object run() {
                            currentThread.setContextClassLoader(previousClassLoader);
                            return null;
                        }
                    }
                    );
                }
            }
        }
        
        protected void doCleanup(Object key) {
            ejbLocalObjectStore.remove(key, false);
        }
    } //LocalEJBObjectCacheVictimHandler{}
    
    protected class EJBObjectCacheVictimHandler
        extends LocalEJBObjectCacheVictimHandler
    {
        
        protected EJBObjectCacheVictimHandler() {
        }
        
        protected void doCleanup(Object key) {
            removeEJBObjectFromStore(key, false);
        }
    } //EJBObjectCacheVictimHandler{}
    


    class EntityCacheStatsProvider
	implements EjbCacheStatsProviderDelegate
    {
	private BaseCache cache;
	private int confMaxCacheSize;

	EntityCacheStatsProvider(BaseCache cache, int maxCacheSize) {
	    this.cache = cache;
	    this.confMaxCacheSize = maxCacheSize;
	}
    
	public int getCacheHits() {
	    return ((Integer) cache.getStatByName(
			Constants.STAT_BASECACHE_HIT_COUNT)).intValue();
	}
    
	public int getCacheMisses() {
	    return ((Integer) cache.getStatByName(
			Constants.STAT_BASECACHE_MISS_COUNT)).intValue();
	}
    
	public int getNumBeansInCache() {
	    return cache.getEntryCount();
	}
    
	public int getNumExpiredSessionsRemoved() {
	    return 0;
	}
    
	public int getNumPassivationErrors() {
	    return totalPassivationErrors;
	}
    
	public int getNumPassivations() {
	    return totalPassivations;
	}
    
	public int getNumPassivationSuccess() {
	    return totalPassivations - totalPassivationErrors;
	}

	public int getMaxCacheSize() {
	    return this.confMaxCacheSize;
	}

    public void appendStats(StringBuffer sbuf) {
	    sbuf.append("[Cache: ")
		.append("Size=").append(getNumBeansInCache()).append("; ")
		.append("HitCount=").append(getCacheHits()).append("; ")
		.append("MissCount=").append(getCacheMisses()).append("; ")
		.append("Passivations=").append(getNumPassivations()).append("; ]");
	}

    }//End of class EntityCacheStatsProvider
    
}

//No need to sync...
class ActiveTxCache {

    private EntityContextImpl[]	    buckets;
    private int			    bucketMask;

    ActiveTxCache(int numBuckets) {
	this.bucketMask = numBuckets - 1;
	initialize();
    }

    EntityContextImpl get(BaseContainer container, Object pk) {
	int pkHashCode = pk.hashCode();
	int index = getIndex(pkHashCode);

	EntityContextImpl ctx = buckets[index];
	while (ctx != null) {
	    if (ctx.doesMatch(container, pkHashCode, pk)) {
		return ctx;
	    }
	    ctx = ctx._getNext();
	}

	return null;
    }

    void add(EntityContextImpl ctx) {
	ctx.cachePrimaryKey();
	int index = getIndex(ctx._getPKHashCode());
	ctx._setNext(buckets[index]);
	buckets[index] = ctx;
    }

    EntityContextImpl remove(BaseContainer container, Object pk) {
	int pkHashCode = pk.hashCode();
	int index = getIndex(pkHashCode);

	EntityContextImpl ctx = buckets[index];
	for (EntityContextImpl prev = null; ctx != null; ctx = ctx._getNext()) {
	    if (ctx.doesMatch(container, pkHashCode, pk)) {
		if (prev == null) {
		    buckets[index] = ctx._getNext();
		} else {
		    prev._setNext(ctx._getNext());
		}
		ctx._setNext(null);
		break;
	    }
	    prev = ctx;
	}

	return ctx;
    }

    //One remove method is enough
    EntityContextImpl remove(Object pk, EntityContextImpl existingCtx) {
	int pkHashCode = pk.hashCode();
	int index = getIndex(pkHashCode);

	EntityContextImpl ctx = buckets[index];
	for (EntityContextImpl prev = null; ctx != null; ctx = ctx._getNext()) {
	    if (ctx == existingCtx) {
		if (prev == null) {
		    buckets[index] = ctx._getNext();
		} else {
		    prev._setNext(ctx._getNext());
		}
		ctx._setNext(null);
		break;
	    }
	    prev = ctx;
	}

	return ctx;
    }

    private void initialize() {
	buckets = new EntityContextImpl[bucketMask+1];
    }

    private final int getIndex(int hashCode) {
	return (hashCode & bucketMask);
    }

}
