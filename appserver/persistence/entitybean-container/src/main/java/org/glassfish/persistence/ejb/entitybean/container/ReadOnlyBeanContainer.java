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
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.EntityBean;
import javax.ejb.FinderException;
import javax.ejb.NoSuchEntityException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;

import com.sun.ejb.ComponentContext;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.containers.EJBContextImpl;
import com.sun.ejb.containers.EJBHomeInvocationHandler;
import com.sun.ejb.containers.EJBLocalHomeInvocationHandler;
import com.sun.ejb.containers.EJBLocalRemoteObject;
import com.sun.enterprise.security.SecurityManager;
import org.glassfish.persistence.ejb.entitybean.container.cache.EJBObjectCache;
import org.glassfish.persistence.ejb.entitybean.container.cache.FIFOEJBObjectCache;
import org.glassfish.persistence.ejb.entitybean.container.cache.UnboundedEJBObjectCache;
import com.sun.ejb.spi.container.BeanStateSynchronization;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbEntityDescriptor;
import org.glassfish.persistence.ejb.entitybean.container.distributed.DistributedEJBServiceFactory;
import org.glassfish.persistence.ejb.entitybean.container.distributed.DistributedReadOnlyBeanService;
import org.glassfish.persistence.ejb.entitybean.container.distributed.ReadOnlyBeanRefreshEventHandler;

import static com.sun.ejb.containers.EJBContextImpl.BeanState;

/**
 * The Container that manages instances of ReadOnly Beans. This container
 * blocks all calls to ejbStore() and selectively performs ejbLoad()
 *
 * @author Mahesh Kannan
 * @author Pramod Gopinath
 */

public class ReadOnlyBeanContainer
    extends EntityContainer
    implements ReadOnlyBeanRefreshEventHandler
{
    
    private long refreshPeriodInMillis = 0;

    // Sequence number incremented each time a bean-level refresh is requested.
    // PK-level data structure has a corresponding sequence number that is used
    // to determine when it needs updating due to bean-level refresh.
    private int beanLevelSequenceNum = 1;

    // Last time a bean-level timeout refresh event occurred. 
    private long beanLevelLastRefreshRequestedAt = 0;                 

    private volatile long currentTimeInMillis = System.currentTimeMillis();
    
    // timer task for refreshing or null if no refresh.
    private TimerTask refreshTask = null;

    private EJBObjectCache robCache;
    
    private DistributedReadOnlyBeanService distributedReadOnlyBeanService;

    private volatile Map<FinderResultsKey, FinderResultsValue> finderResultsCache =
        new ConcurrentHashMap<FinderResultsKey, FinderResultsValue>();
    
    private static final int FINDER_LOCK_SIZE = 8 * 1024;
    
    private Object[] finderLocks = new Object[FINDER_LOCK_SIZE];
   
    //Don't make this as a static. In future, we may want to
    //  support bean level flag for this
    private boolean RELATIVE_TIME_CHECK_MODE = false;

    protected ReadOnlyBeanContainer(EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
        throws Exception
    {
        //super(ContainerType.READ_ONLY, desc, loader);
        super(ContainerType.ENTITY, desc, loader, sm);
        
        EjbEntityDescriptor ed = (EjbEntityDescriptor)desc;
        refreshPeriodInMillis =
            ed.getIASEjbExtraDescriptors().getRefreshPeriodInSeconds() * 1000L;

        if( refreshPeriodInMillis > 0 ) {
            long timerFrequency = 1;
            String refreshRateStr =
            System.getProperty("com.sun.ejb.containers.readonly.timer.frequency", "1");
            try {
                timerFrequency = Integer.parseInt(refreshRateStr);
                if (timerFrequency < 0) {
                    timerFrequency = 1;
                }
            } catch (Exception ex) {
                _logger.log(Level.FINE, "Invalid timer frequency " + refreshRateStr);
            }

            try {
                RELATIVE_TIME_CHECK_MODE  = Boolean.valueOf(System.getProperty(
                        "com.sun.ejb.containers.readonly.relative.refresh.mode"));
                _logger.log(Level.FINE, "RELATIVE_TIME_CHECK_MODE: " + RELATIVE_TIME_CHECK_MODE);
            } catch (Exception ex) {
            	_logger.log(Level.FINE, "(Ignorable) Exception while initializing RELATIVE_TIME_CHECK_MODE", ex);
            }

            Timer timer = ejbContainerUtilImpl.getTimer();
            if (RELATIVE_TIME_CHECK_MODE) {
                refreshTask = new CurrentTimeRefreshTask ();
                timer.scheduleAtFixedRate(refreshTask, timerFrequency*1000L, timerFrequency*1000L);
            } else {
                refreshTask = new RefreshTask();
                timer.scheduleAtFixedRate(refreshTask, refreshPeriodInMillis, 
                                      refreshPeriodInMillis);
            }
        } else {
            refreshPeriodInMillis = 0;
        }
        
        for (int i=0; i<FINDER_LOCK_SIZE; i++) {
        	finderLocks[i] = new Object();
        }

        // Create read-only bean cache
        long idleTimeoutInMillis = (cacheProp.cacheIdleTimeoutInSeconds <= 0) ?
            -1 : (cacheProp.cacheIdleTimeoutInSeconds * 1000L);

        if( (cacheProp.maxCacheSize <= 0) && (idleTimeoutInMillis <= 0) ) {
            robCache = new UnboundedEJBObjectCache(ejbDescriptor.getName());
            robCache.init(DEFAULT_CACHE_SIZE, cacheProp.numberOfVictimsToSelect,
                          0L, 1.0F, null);
        } else {
            int cacheSize = (cacheProp.maxCacheSize <= 0) ?
                DEFAULT_CACHE_SIZE : cacheProp.maxCacheSize;
            robCache = new FIFOEJBObjectCache(ejbDescriptor.getName());
        
            robCache.init(cacheSize, 
                          cacheProp.numberOfVictimsToSelect, 
                          idleTimeoutInMillis, 1.0F, null);
            // .setEJBObjectCacheListener(
            //     new EJBObjectCacheVictimHandler());
        }
        
        this.distributedReadOnlyBeanService =
            DistributedEJBServiceFactory.getDistributedEJBService()
                .getDistributedReadOnlyBeanService();
        this.distributedReadOnlyBeanService.addReadOnlyBeanRefreshEventHandler(
                getContainerId(), getClassLoader(), this);
        

    }
    
    private void updateBeanLevelRefresh() {
               
        beanLevelSequenceNum++;
        beanLevelLastRefreshRequestedAt = this.currentTimeInMillis;

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "updating bean-level refresh for " + 
                        " read-only bean " + ejbDescriptor.getName() + 
                        " at " + new Date(beanLevelLastRefreshRequestedAt) + 
                        " beanLevelSequenceNum = " + beanLevelSequenceNum);

            
        }

        // Clear out bean-level finder results cache.
        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, "Clearing " + 
            		finderResultsCache.size() + " items from " + 
                        "finder results cache");
        }

        finderResultsCache =
            new ConcurrentHashMap<FinderResultsKey, FinderResultsValue>();

        
    }

    protected void callEJBStore(EntityBean ejb, EntityContextImpl context) {
        // this method in the ReadOnlyBean case should be a no-op 
        // and should not throw any exception.
    }
    
    protected ComponentContext _getContext(EjbInvocation inv) {
        ComponentContext ctx = super._getContext(inv);

        InvocationInfo info = inv.invocationInfo; // info cannot be null
        if (info.isTxRequiredLocalCMPField) {
            if (! inv.foundInTxCache) {
                EntityContextImpl entityCtx = (EntityContextImpl) ctx;
                super.afterBegin(entityCtx);
                inv.foundInTxCache = true;
            }
        } else {
            //TODO: We can still optimize NonTx access to CMP getters/setters
        }
        return ctx;
    }
    
    protected void callEJBLoad(EntityBean ejb, EntityContextImpl entityCtx,
                               boolean activeTx)
        throws Exception
    {
        // ReadOnlyContextImpl should always be used in conjunction with ReadOnlyBeanContainer
        assert entityCtx instanceof ReadOnlyContextImpl;
        ReadOnlyContextImpl context = (ReadOnlyContextImpl) entityCtx;
               
        ReadOnlyBeanInfo robInfo = context.getReadOnlyBeanInfo();

        // Grab the pk-specific lock before doing the refresh comparisons.
        // In the common-case, the lock will only be held for a very short
        // amount of time.  In the case where a pk-level refresh is needed,
        // we want to ensure that no concurrent refreshes for the same
        // pk can occur.

        int pkLevelSequenceNum = 0;
        long  pkLastRefreshedAt = 0;

        synchronized(robInfo) {
            
            int currentBeanLevelSequenceNum = beanLevelSequenceNum;

            if( robInfo.beanLevelSequenceNum != currentBeanLevelSequenceNum) { 

                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, "REFRESH DUE TO BEAN-LEVEL UPDATE:"
                                + " Bean-level sequence num = " + 
                                beanLevelSequenceNum + 
                                robInfo + " current time is " + new Date());
                }
                
                robInfo.refreshNeeded = true;
            } else if (RELATIVE_TIME_CHECK_MODE && (refreshPeriodInMillis > 0)) { // 0 implies no time based refresh
                if ((currentTimeInMillis - robInfo.lastRefreshedAt) > refreshPeriodInMillis) {
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "REFRESH DUE TO STALE PK:"
                                + " robInfo.lastRefreshedAt: " + robInfo.lastRefreshedAt
                                + "; current (approx) time is " + currentTimeInMillis);
                    }

                    robInfo.refreshNeeded = true;
                }
            } 
            
            // Refresh could be true EITHER because time-based refresh
            // occurred or programmatic refresh of this PK.
            if (robInfo.refreshNeeded) {
                
                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, " PK-LEVEL REFRESH : "
                                + robInfo + " current time is " + new Date());
                }
                
                try {

                    if( isContainerManagedPers ) {
                        BeanStateSynchronization beanStateSynch =
                            (BeanStateSynchronization) ejb;

                        beanStateSynch.ejb__refresh(entityCtx.getPrimaryKey());

                        if( _logger.isLoggable(Level.FINE) ) {
                            _logger.log(Level.FINE, " PK-LEVEL REFRESH DONE :"
                                + robInfo + " current time is " + new Date());
                        }

                    } else {

                        if( ejb instanceof BeanStateSynchronization ) {
                            // For debugging purposes, call into ejb__refresh
                            // if it's present on a BMP bean class
                            BeanStateSynchronization beanStateSynch =
                                (BeanStateSynchronization) ejb;
                            
                            beanStateSynch.ejb__refresh
                                (entityCtx.getPrimaryKey());
                        }

                    }
                    
                } finally {                    
                    // Always set refreshNeeded to false 
                    robInfo.refreshNeeded = false;
                }

                // Rob info only updated if no errors so far.

                updateAfterRefresh(robInfo);
            }                     
                        
            pkLevelSequenceNum = robInfo.pkLevelSequenceNum;
            pkLastRefreshedAt = robInfo.lastRefreshedAt;
            
        } // releases lock for pk's read-only bean info
        
        if ((entityCtx.isNewlyActivated())
                || (context.getPKLevelSequenceNum() != pkLevelSequenceNum)) {

            // Now do instance-level refresh check to see if 
            // ejbLoad is warranted.        
            callLoad(ejb, context, pkLevelSequenceNum, 
                    pkLastRefreshedAt, currentTimeInMillis);
        }
    }
        
    private void callLoad(EntityBean ejb, ReadOnlyContextImpl context,
                          int pkLevelSequenceNum, long pkLastRefreshedAt,
                          long currentTime) throws Exception {

        if( _logger.isLoggable(Level.FINE) ) {
            _logger.log(Level.FINE, 
                        "Calling ejbLoad for read-only bean " +
                        ejbDescriptor.getName() + " primary key " + 
                        context.getPrimaryKey() + " at " +
                        new Date(currentTime));
        }

        try {
            context.setInEjbLoad(true);
            ejb.ejbLoad();

            if( pkLevelSequenceNum > 0 ) {
                // Synch up pk-level sequence num after successful load
                context.setPKLevelSequenceNum(pkLevelSequenceNum);
            }

            // Set last refresh time after successful load
            context.setLastRefreshedAt(pkLastRefreshedAt);       
        } finally {
            context.setInEjbLoad(false);
        }        

    }
    
    protected void callEJBRemove(EntityBean ejb, EntityContextImpl context)
        throws Exception
    {

        // This will only be called for BMP read-only beans since AS 7
        // allowed the client to make this call.  Calls to remove 
        // CMP read-only beans result in a runtime exception.

        Object pk = context.getPrimaryKey();                
        robCache.removeAll(pk);
        
    }
    
    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {

        this.distributedReadOnlyBeanService.removeReadOnlyBeanRefreshEventHandler(
                getContainerId());
        
        if( refreshTask != null ) {
            refreshTask.cancel();
        }

        robCache.clear();

        super.doConcreteContainerShutdown(appBeingUndeployed);
    }
    
    // Called from BaseContainer just before invoking a business method
    // whose tx attribute is TX_NEVER / TX_NOT_SUPPORTED / TX_SUPPORTS 
    // without a client tx.
    protected void preInvokeNoTx(EjbInvocation inv) {
        EntityContextImpl context = (EntityContextImpl)inv.context;
        
        if ( context.isInState(BeanState.DESTROYED) )
            return;
        
        if ( !inv.invocationInfo.isCreateHomeFinder ) {
            // follow EJB2.0 section 12.1.6.1
            EntityBean e = (EntityBean)context.getEJB();
            try {
                callEJBLoad(e, context, false);
            } catch ( NoSuchEntityException ex ) {
                _logger.log(Level.FINE, "Exception in preInvokeNoTx()", ex);
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new NoSuchObjectLocalException(
                    "NoSuchEntityException thrown by ejbLoad, " +
                    "EJB instance discarded");
            } catch ( Exception ex ) {
                // Error during ejbLoad, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                
                throw new EJBException(ex);
            }
            context.setNewlyActivated(false);
        }
    }
    
    protected void afterNewlyActivated(EntityContextImpl context) {
        // In the case of ReadOnlyBean store the Context into the list
        ReadOnlyBeanInfo robInfo = addToCache(context.getPrimaryKey(), true);

        // Set the read-only bean info on the context so we can access it
        // without doing a cache lookup.
        // ReadOnlyContextImpl should always be used in conjunction with ReadOnlyBeanContainer
        assert context instanceof ReadOnlyContextImpl;
        ReadOnlyContextImpl readOnlyContext = (ReadOnlyContextImpl) context;
        readOnlyContext.setReadOnlyBeanInfo(robInfo);       
    }
    
    protected void addPooledEJB(EntityContextImpl ctx) {
        try {
            // ReadOnlyContextImpl should always be used in conjunction with ReadOnlyBeanContainer
            assert ctx instanceof ReadOnlyContextImpl;
            ReadOnlyContextImpl readOnlyCtx = (ReadOnlyContextImpl)ctx;
            if( readOnlyCtx.getReadOnlyBeanInfo() != null ) {

                readOnlyCtx.setReadOnlyBeanInfo(null);
                
                robCache.remove(ctx.getPrimaryKey(), true);
            }                                    
        } catch (Exception ex) {

            _logger.log(Level.SEVERE, "entitybean.container.addPooledEJB", ex);
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;

        } finally {
            super.addPooledEJB(ctx);
        }
    }
    
    protected void forceDestroyBean(EJBContextImpl context) {
        
        try {
            ReadOnlyContextImpl readOnlyCtx = (ReadOnlyContextImpl) context;
            if( readOnlyCtx.getReadOnlyBeanInfo() != null ) {

                readOnlyCtx.setReadOnlyBeanInfo(null);

                robCache.remove(readOnlyCtx.getPrimaryKey(), true);
            }
            
        } catch (Exception ex) {            

            _logger.log(Level.SEVERE, "entitybean.container.forceDestroyBean", ex);            
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;        
    
        } finally {
            super.forceDestroyBean(context);
        }
    }

    public void preInvoke(EjbInvocation inv) {

        // Overriding preInvoke is the best way to interpose on the 
        // create early enough to throw an exception or eat the
        // request before too much setup work is done by the container.
        // It's better to keep this logic in the Read-Only Bean container
        // than to put it in the InvocationHandlers.  Note that 
        // interposition for the remove operation is handled below
        // by overriding the removeBean method.
        if( (inv.invocationInfo != null) &&
            inv.invocationInfo.startsWithCreate ) {

            String msg = "Error for ejb " + ejbDescriptor.getName() +
                ". create is not allowed for read-only entity beans";

            if( isContainerManagedPers ) {
                // EJB team decided that throwing a runtime exception was more
                // appropriate in this case since creation is not a
                // supported operation for read-only beans.  If the application
                // is coded this way, it's best to throw a system exception
                // to signal that the application is broken.  NOTE that this 
                // only applies to the CMP 1.x and 2.x read-only bean 
                // functionality added starting with AS 8.1.  

                throw new EJBException(msg);
                                       
            } else {
                // Preserve AS 7 BMP ROB create behavior 
                CreateException ce = new CreateException(msg);
                throw new PreInvokeException(ce);
            }

        } else {
            super.preInvoke(inv);                
        }
    }

    protected Object invokeTargetBeanMethod(Method beanClassMethod, EjbInvocation inv, 
                                  Object target, Object[] params, 
                                  com.sun.enterprise.security.SecurityManager mgr) 
        throws Throwable {

        Object returnValue = null;

        if( inv.invocationInfo.startsWithFind ) {

            FinderResultsKey key = new FinderResultsKey(inv.method, params);

            FinderResultsValue value = finderResultsCache.get(key);
            if (value != null) {
                if (RELATIVE_TIME_CHECK_MODE && (refreshPeriodInMillis > 0)) {
                    long timeLeft = currentTimeInMillis - value.lastRefreshedAt;
                    if (timeLeft >=  refreshPeriodInMillis) {
                        returnValue = value.value;
                    }
                } else {
                	//Use even if !RELATIVE_MODE or if refreshTime == 0
                    returnValue = value.value;
                }
            }

            if (returnValue == null) {
            	int hashCode = key.getExtendedHC();
            	if (hashCode < 0) {
            		hashCode = -hashCode;
            	}
            	int index = hashCode & (FINDER_LOCK_SIZE - 1);
            	synchronized (finderLocks[index]) {
            	    value = finderResultsCache.get(key);
	                if (value == null) {
	                    returnValue = super.invokeTargetBeanMethod(
	                        beanClassMethod, inv, target, params, mgr);
	                    finderResultsCache.put(key, new FinderResultsValue(returnValue,
	                        currentTimeInMillis));
	                } else {
                            returnValue = value.value;
	                }
            	    }
                }

        } else {
            returnValue =  super.invokeTargetBeanMethod(beanClassMethod, inv,
                                                        target, params, mgr);
        }

        return returnValue;
    }

    protected void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod,
                              boolean local)
        throws RemoveException, EJBException, RemoteException
    {

        String msg = "Error for ejb " + ejbDescriptor.getName() +
            ". remove is not allowed for read-only entity beans";

        if( isContainerManagedPers ) {
            
            // EJB team decided that throwing a runtime exception was more
            // appropriate in this case since removal is not a
            // supported operation for read-only beans.  If the application
            // is coded this way, it's best to throw a system exception
            // to signal that the application is broken.  NOTE that this 
            // only applies to the CMP 1.x and 2.x read-only bean 
            // functionality added starting with AS 8.1.  
            
            // There's no post-invoke logic to convert local exceptions
            // to remote, so take care of that here.
            if (local) {                
                throw new EJBException(msg);
            } else {
                throw new RemoteException(msg);
            }

        } else {
            // Preserve AS 7 BMP ROB removal behavior.
            // Calls to ejbRemove on BMP read-only beans in AS 7
            // were silently "eaten" by the ejb container.   The
            // client didn't receive any exception, but ejbRemove
            // was not called on the container.
        }        
    }
    
    protected void initializeHome()
        throws Exception
    {
        super.initializeHome();

        if (isRemote) {
            ((ReadOnlyEJBHomeImpl) this.ejbHomeImpl).
                setReadOnlyBeanContainer(this);
        } 
        
        if (isLocal) {
            ReadOnlyEJBLocalHomeImpl readOnlyLocalHomeImpl =
                (ReadOnlyEJBLocalHomeImpl) ejbLocalHomeImpl;
            readOnlyLocalHomeImpl.setReadOnlyBeanContainer(this);
        }
    }

    @Override
    protected EJBHomeInvocationHandler getEJBHomeInvocationHandler(Class homeIntfClass) throws Exception {
        return new ReadOnlyEJBHomeImpl(ejbDescriptor, homeIntfClass);
    }

    @Override
    protected EJBLocalHomeInvocationHandler getEJBLocalHomeInvocationHandler(Class homeIntfClass) throws Exception {
        return new ReadOnlyEJBLocalHomeImpl(ejbDescriptor, homeIntfClass);
    }
    
    public void setRefreshFlag(Object primaryKey) {
        
        try {
            handleRefreshRequest(primaryKey);
        } finally {
            distributedReadOnlyBeanService.notifyRefresh(
                    getContainerId(), primaryKey);
        }
    }
        
    public void handleRefreshRequest(Object primaryKey) {    
        // Lookup the read-only bean info for this pk. 
        // If there is no entry for this pk, do nothing.
        // If there is a cache hit we *don't* want to increment the
        // ref count.
        ReadOnlyBeanInfo robInfo = (ReadOnlyBeanInfo) 
            robCache.get(primaryKey, false);
        if( robInfo != null ) {
           
            synchronized(robInfo) {
                
                robInfo.refreshNeeded = true;
                robInfo.lastRefreshRequestedAt = this.currentTimeInMillis;
                
                if( _logger.isLoggable(Level.FINE) ) {
                    _logger.log(Level.FINE, 
                        "Updating refresh time for read-only bean " +
                        ejbDescriptor.getName() + " primary key " + primaryKey 
                        + " at " + new Date(robInfo.lastRefreshRequestedAt) +
                        " pkLevelSequenceNum = " + robInfo.pkLevelSequenceNum);
                }
            }
        } else {
            _logger.log(Level.FINE,
                        "Refresh event for unknown read-only bean PK = " +
                        primaryKey + " at " + new Date());
        }
    }
    
    /**
     * invoked when application calls refreshAll()
     */
    void refreshAll() {
        try {
            handleRefreshAllRequest();
        } finally {
            distributedReadOnlyBeanService.notifyRefreshAll(getContainerId());
        }
    }
    
    public void handleRefreshAllRequest() {
    	_logger.log(Level.FINE, "Received refreshAll request...");
        updateBeanLevelRefresh();
    }

    protected EntityContextImpl createEntityContextInstance(EntityBean ejb,
        EntityContainer entityContainer)
    {
        return new ReadOnlyContextImpl(ejb, entityContainer);
    }

    private ReadOnlyBeanInfo addToCache(Object primaryKey, boolean incrementRefCount) {
        
        // Optimize for the cache where the cache item already
        // exists and we have a 2nd, 3rd, 4th, etc. context for
        // the same primary key.  If the item exists, the ref count
        // will be incremented.
        ReadOnlyBeanInfo robInfo = (ReadOnlyBeanInfo) 
            robCache.get(primaryKey, incrementRefCount);

        if( robInfo == null ) {

            // If the item doesn't exist, create a new one.  The cache
            // ensures that the ref count is correct in the face of concurrent
            // puts.

            ReadOnlyBeanInfo newRobInfo = new ReadOnlyBeanInfo();

            newRobInfo.primaryKey = primaryKey;

            // Initialize bean level sequence num so that the first time an
            // instance of this PK goes through callEJBLoad, it will force
            // a refresh.
            newRobInfo.beanLevelSequenceNum = -1;
            newRobInfo.refreshNeeded = true;

            newRobInfo.pkLevelSequenceNum = 1;

            newRobInfo.lastRefreshRequestedAt = 0;
            newRobInfo.lastRefreshedAt = 0;

            // Cache ejbObject/ejbLocalObject within ROB info.
            // This value is used by
            // findByPrimaryKey to avoid a DB access.  Caching here
            // ensures that there will be one DB access for the PK 
            // regardless of the order in which findByPrimaryKey is called
            // with respect to the business method call.  This also covers
            // the case where a business method is invoked through the
            // local view and findByPrimaryKey is invoked through the
            // Remote view (or vice versa).  
            if( ejbDescriptor.isLocalInterfacesSupported() ) {
                newRobInfo.cachedEjbLocalObject = 
                    getEJBLocalObjectForPrimaryKey(primaryKey);
            }
            if( ejbDescriptor.isRemoteInterfacesSupported() ) {
                newRobInfo.cachedEjbObject = 
                    getEJBObjectStub(primaryKey, null);
            }
            
            ReadOnlyBeanInfo otherRobInfo = (ReadOnlyBeanInfo)
                robCache.put(primaryKey, newRobInfo, incrementRefCount);

            // If someone else inserted robInfo for this pk before *our* put(),
            // use that as the pk's robInfo.  Otherwise, the new robInfo we
            // created is the "truth" for this pk.
            robInfo = (otherRobInfo == null) ? newRobInfo : otherRobInfo;
        } 

        return robInfo;
    }
    
    //Called from InvocationHandler for findByPrimaryKey
    //The super class (EntityContainer) also defines this method whcih is where
    //	the real work (of finding it from the database) is done.
    protected Object invokeFindByPrimaryKey(Method method, EjbInvocation inv,
		Object[] args)
	throws Throwable
    {
	Object returnValue = null;
	ReadOnlyBeanInfo robInfo = addToCache(args[0], false);
	synchronized (robInfo) {
	    returnValue = inv.isLocal
		? robInfo.cachedEjbLocalObject : robInfo.cachedEjbObject;

	    if ( robInfo.refreshNeeded ) {
		_logger.log(Level.FINE, "ReadOnlyBeanContainer calling ejb.ejbFindByPK... for pk=" + args[0]);
		returnValue = super.invokeFindByPrimaryKey(method, inv, args);
		robInfo.refreshNeeded = false;

		//set the seq numbers so that the subsequent business method calls 
		//  (if within expiration time) do not have to call ejb__refresh!!
		updateAfterRefresh(robInfo);

	    }
	}

	return returnValue;
    }

    public Object postFind(EjbInvocation inv, Object primaryKeys, 
                           Object[] findParams)
        throws FinderException
    {

        // Always call parent to convert pks to ejbobjects/ejblocalobjects.
        Object returnValue = super.postFind(inv, primaryKeys, findParams);

        // Only proceed if this is not a findByPK method.  FindByPK
        // processing is special since it's possible to actually
        // skip the db access for the query itself.  The caching requirements
        // to actually skip nonFindByPK queries are extremely complex, but
        // the next best thing to skipping the query is to populate the
        // RobInfo cache with an entry for each pk in the result set.  If
        // a PK is part of the result set for a nonFindByPK query before
        // it is accessed through some other means, no new refresh will be
        // required.  This will have the largest benefits for large result
        // sets since it's possible for a query to return N beans from one
        // db access, which would otherwise require N db accesses if the
        // refresh were done upon business method invocation or findByPK.
        // If a PK has been accessed before appearing in the result set of 
        // a nonFindByPK finder, there is no performance gain.
        if( !inv.method.getName().equals("findByPrimaryKey") ) {
            if ( primaryKeys instanceof Enumeration ) {
                 Enumeration e = (Enumeration) primaryKeys;
                 while ( e.hasMoreElements() ) {
                     Object primaryKey = e.nextElement();
                     if( primaryKey != null ) {
                         updateRobInfoAfterFinder(primaryKey);
                     }
                 }
            } else if ( primaryKeys instanceof Collection ) {
                Collection c = (Collection)primaryKeys;
                Iterator it = c.iterator();
                while ( it.hasNext() ) {
                    Object primaryKey = it.next();
                    if( primaryKey != null ) {
                        updateRobInfoAfterFinder(primaryKey);
                    }
                }
            } else {
                if( primaryKeys != null ) {
                    updateRobInfoAfterFinder(primaryKeys);
                }
            }
        }

        return returnValue;
    }

    private void updateRobInfoAfterFinder(Object primaryKey) {
        addToCache(primaryKey, false);

        /*
        ReadOnlyBeanInfo robInfo = addToCache(primaryKey, false);
        synchronized (robInfo) {
            if( robInfo.refreshNeeded ) {
                robInfo.refreshNeeded = false;
                updateAfterRefresh(robInfo);
            }
        }
        */
    }

    //Called after a sucessful ejb_refresh and
    //it is assumed that the caller has a lock on the robInfo
    private void updateAfterRefresh(ReadOnlyBeanInfo robInfo) {
	robInfo.beanLevelSequenceNum = beanLevelSequenceNum;
	robInfo.pkLevelSequenceNum++;
	robInfo.lastRefreshedAt = this.currentTimeInMillis;
    }

    private final class RefreshTask extends TimerTask {

        public void run() {            
            updateBeanLevelRefresh();
        }
    }

    private final class CurrentTimeRefreshTask extends TimerTask {

        public void run() {            
            currentTimeInMillis = System.currentTimeMillis();
        }
    }

    private static final class FinderResultsValue {
        long lastRefreshedAt;
        Object value;

        public FinderResultsValue(Object v, long time) {
            value = v;
            this.lastRefreshedAt = time;
        }
    }

    private static final class FinderResultsKey {

        private static final Object[] EMPTY_PARAMS = new Object[0];

        private Method finderMethod;

        private Object[] params;

        private int hc;
        
        private int extendedHC;

        public FinderResultsKey(Method method, Object[] params) {
            finderMethod = method;
            this.hc = finderMethod.hashCode();
            this.extendedHC = this.hc;

            this.params = (params == null) ? EMPTY_PARAMS : params;
            for (Object param : this.params) {
            	extendedHC += param.hashCode();
            }
        }
        
        public int hashCode() {
            return hc;
        }
        
        public int getExtendedHC() {
        	return this.extendedHC;
        }

        public boolean equals(Object o) {

            boolean equal = false;

            if( o instanceof FinderResultsKey ) {

                FinderResultsKey other = (FinderResultsKey) o;
                if ((params.length == other.params.length)
                		&& (finderMethod.equals(other.finderMethod))) {

                    equal = true;

                    for(int i = 0; i < params.length; i++) {
                        
                        Object nextParam = params[i];
                        Object nextParamOther  = other.params[i];
                        
                        if( nextParam instanceof EJBLocalObject ) {

                            equal = compareEJBLocalObject
                                (((EJBLocalObject)nextParam), nextParamOther);

                        } else if ( nextParam instanceof EJBObject ) {

                            equal = compareEJBObject
                                (((EJBObject)nextParam), nextParamOther);
                                                        
                        } else {
                            equal = nextParam.equals(nextParamOther);
                        }

                        if( !equal ) {
                            break;
                        }
                    }
                }
            }

            return equal;

        }

        private boolean compareEJBLocalObject(EJBLocalObject localObj1,
                                              Object other) {
            boolean equal = false;
            
            if( other instanceof EJBLocalObject ) {

                equal = localObj1.isIdentical((EJBLocalObject) other);

            }
            
            return equal;

        }

        private boolean compareEJBObject(EJBObject ejbObj1,
                                         Object other) {

            boolean equal = false;

            if( other instanceof EJBObject ) {
                
                // @@@ Might want to optimize to avoid EJBObject invocation
                // overhead.
                try {
                    equal = ejbObj1.isIdentical((EJBObject) other);
                } catch(RemoteException re) {
                    // ignore
                    equal = false;
                }

            }

            return equal;

        }

        

    }
    
}
