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

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBObject;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.transaction.Status;
import javax.transaction.Transaction;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.config.EjbContainer;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;

import com.sun.ejb.ComponentContext;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.containers.util.pool.AbstractPool;
import com.sun.ejb.containers.util.pool.NonBlockingPool;
import com.sun.ejb.containers.util.pool.ObjectFactory;
import com.sun.ejb.monitoring.stats.EjbMonitoringStatsProvider;
import com.sun.ejb.monitoring.stats.EjbPoolStatsProvider;
import com.sun.ejb.monitoring.stats.StatelessSessionBeanStatsProvider;
import com.sun.enterprise.admin.monitor.callflow.ComponentType;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import com.sun.enterprise.deployment.runtime.BeanPoolDescriptor;
import com.sun.enterprise.security.SecurityManager;

/** This class provides container functionality specific to stateless 
 *  SessionBeans.
 *  At deployment time, one instance of the StatelessSessionContainer is created
 *  for each stateless SessionBean type (i.e. deployment descriptor) in a JAR. 
 * <P>
 * The 3 states of a Stateless EJB (an EJB can be in only 1 state at a time):
 * 1. POOLED : ready for invocations, no transaction in progress
 * 2. INVOKING : processing an invocation
 * 3. DESTROYED : does not exist  
 * <P>
 * This container services invocations using a pool of EJB instances.
 * An instance is returned to the pool immediately after the invocation
 * completes, so the # of instances needed = # of concurrent invocations.
 * <P>
 * A Stateless Bean can hold open DB connections across invocations.
 * Its assumed that the Resource Manager can handle
 * multiple incomplete transactions on the same
 * connection.
 *
 */    

public class StatelessSessionContainer
    extends BaseContainer 
{
    private static final byte[] statelessInstanceKey = {0, 0, 0, 1};

    // All stateless EJBs have the same instanceKey, since all stateless EJBs
    // are identical. Note: the first byte of instanceKey must be left empty.

    // All stateless EJB instances of a particular class (i.e. all bean 
    // instances created by this container instance) have the same 
    // EJBObject/EJBLocalObject instance since they are all identical.
    private EJBLocalObjectImpl theEJBLocalObjectImpl = null;
    private EJBLocalObjectImpl theEJBLocalBusinessObjectImpl = null;
    private EJBLocalObjectImpl theOptionalEJBLocalBusinessObjectImpl = null;

    // Data members for RemoteHome view
    private EJBObjectImpl theEJBObjectImpl = null;
    private EJBObject theEJBStub = null;

    // Data members for Remote business view. Any objects representing the
    // Remote business interface are not subtypes of EJBObject.
    private EJBObjectImpl theRemoteBusinessObjectImpl = null;
    private Map<String, java.rmi.Remote> theRemoteBusinessStubs =
        new HashMap<String, java.rmi.Remote>();

    protected AbstractPool pool;

    private IASEjbExtraDescriptors iased 	 = null;
    private BeanPoolDescriptor beanPoolDes   = null;
    private EjbContainer ejbContainer 		 = null;

    private PoolProperties poolProp 		 = null;

    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     * @exception Exception on error
     */
    StatelessSessionContainer(EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
	throws Exception
    {
        this(ContainerType.STATELESS, desc, loader, sm);
    }

    protected StatelessSessionContainer(ContainerType conType, EjbDescriptor desc, ClassLoader loader, SecurityManager sm)
        throws Exception
        {
            super(conType, desc, loader, sm);

        ejbContainer = ejbContainerUtilImpl.getEjbContainer();

        super.createCallFlowAgent(ComponentType.SLSB);
    }

    public String getMonitorAttributeValues() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("STATELESS ").append(ejbDescriptor.getName());
        sbuf.append(pool.getAllAttrValues());
        sbuf.append("]");

        return sbuf.toString();
    }

    @Override
    protected boolean suspendTransaction(EjbInvocation inv) throws Exception {
        // EJB2.0 section 7.5.7 says that ejbCreate/ejbRemove etc are called
        // without a Tx. So suspend the client's Tx if any.

        // Note: ejbRemove cannot be called when EJB is associated with
        // a Tx, according to EJB2.0 section 7.6.4. This check is done in
        // the container's implementation of removeBean().

        return !inv.invocationInfo.isBusinessMethod;
    }

    @Override
    protected boolean resumeTransaction(EjbInvocation inv) throws Exception {
        return !inv.invocationInfo.isBusinessMethod;
    }

    @Override
    public boolean scanForEjbCreateMethod() {
        return true;
    }

    protected EjbMonitoringStatsProvider getMonitoringStatsProvider(
            String appName, String modName, String ejbName) {
        return new StatelessSessionBeanStatsProvider(this, getContainerId(), appName, modName, ejbName);
    }

    protected void initializeHome()
        throws Exception
    {

        super.initializeHome();

        if ( isRemote ) {
            
            if( hasRemoteHomeView ) {
                // Create theEJBObjectImpl
                theEJBObjectImpl = instantiateEJBObjectImpl();

                // connect the EJBObject to the ProtocolManager 
                // (creates the stub 
                // too). Note: cant do this in constructor above because 
                // beanId is not set at that time.
                theEJBStub = (EJBObject) 
                    remoteHomeRefFactory.createRemoteReference
                       (statelessInstanceKey);
                
                theEJBObjectImpl.setStub(theEJBStub);
            }

            if( hasRemoteBusinessView ) {

                theRemoteBusinessObjectImpl = 
                    instantiateRemoteBusinessObjectImpl();

                for(RemoteBusinessIntfInfo next :
                        remoteBusinessIntfInfo.values()) {
                    java.rmi.Remote stub = next.referenceFactory.
                        createRemoteReference(statelessInstanceKey);
                    theRemoteBusinessStubs.put
                        (next.generatedRemoteIntf.getName(), stub);
                    theRemoteBusinessObjectImpl.setStub
                        (next.generatedRemoteIntf.getName(), stub);
                }

            }

        }

        if ( isLocal ) {
            if( hasLocalHomeView ) {
                theEJBLocalObjectImpl = instantiateEJBLocalObjectImpl();
            }
            if( hasLocalBusinessView ) {
                theEJBLocalBusinessObjectImpl = 
                    instantiateEJBLocalBusinessObjectImpl();
            }
            if (hasOptionalLocalBusinessView) {
                theOptionalEJBLocalBusinessObjectImpl =
                    instantiateOptionalEJBLocalBusinessObjectImpl();
            }
        }

        
        createBeanPool();

        registerMonitorableComponents();
    }

    protected void createBeanPool() {
        ObjectFactory sessionCtxFactory = new SessionContextFactory();

                iased = ejbDescriptor.getIASEjbExtraDescriptors();
        if( iased != null) {
            beanPoolDes = iased.getBeanPool();
        }

        poolProp = new PoolProperties(ejbContainer, beanPoolDes);
        String val = ejbDescriptor.getEjbBundleDescriptor().getEnterpriseBeansProperty(SINGLETON_BEAN_POOL_PROP);
        pool= new NonBlockingPool(getContainerId(), ejbDescriptor.getName(),
           sessionCtxFactory, poolProp.steadyPoolSize,
           poolProp.poolResizeQuantity, poolProp.maxPoolSize,
           poolProp.poolIdleTimeoutInSeconds, loader, Boolean.parseBoolean(val));
    }

    protected void registerMonitorableComponents() {
        super.registerMonitorableComponents();

        poolProbeListener = new EjbPoolStatsProvider(pool,
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        poolProbeListener.register();

        _logger.log(Level.FINE, "[SLSB Container] registered monitorable");
    }

    public void onReady() {
    }

    public EJBObjectImpl createRemoteBusinessObjectImpl()
        throws CreateException, RemoteException
    {
        // No access check since this is an internal operation.

	ejbBeanCreatedEvent();
        return theRemoteBusinessObjectImpl;
    }

    private void ejbBeanCreatedEvent() {
	ejbProbeNotifier.ejbBeanCreatedEvent(
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
    }
	
    /**
     *
     */
    public EJBObjectImpl createEJBObjectImpl()
        throws CreateException, RemoteException
    {
        // Need to do access control check here because BaseContainer.preInvoke
        // is not called for stateless sessionbean creates.
        authorizeRemoteMethod(EJBHome_create);
        /*TODO
        if ( AppVerification.doInstrument() ) {
            AppVerification.getInstrumentLogger().doInstrumentForEjb(
                ejbDescriptor, homeCreateMethod, null);
        }
        */
	ejbBeanCreatedEvent();

        // For stateless EJBs, EJB2.0 Section 7.8 says that 
        // Home.create() need not do any real creation.
        // If necessary, a stateless bean is created below during getContext().
        return theEJBObjectImpl;
    }

    /**
     * Called during client creation request through EJB LocalHome view.
     */
    public EJBLocalObjectImpl createEJBLocalObjectImpl()
        throws CreateException
    {	
        // Need to do access control check here because BaseContainer.preInvoke
        // is not called for stateless sessionbean creates.
        authorizeLocalMethod(EJBLocalHome_create);
        /*TODO
        if ( AppVerification.doInstrument() ) {
            AppVerification.getInstrumentLogger().doInstrumentForEjb(
                ejbDescriptor, localHomeCreateMethod, null);
        }
        */
	ejbBeanCreatedEvent();

        // For stateless EJBs, EJB2.0 Section 7.8 says that 
        // Home.create() need not do any real creation.
        // If necessary, a stateless bean is created below during getContext().
        return theEJBLocalObjectImpl;
    }

    /**
     * Called during internal creation of session bean
     */
    public EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(boolean localBeanView)
        throws CreateException
    {	
	ejbBeanCreatedEvent();

        // No access checks needed because this is called as a result
        // of an internal creation, not a user-visible create method.
        return (localBeanView)
                ? theOptionalEJBLocalBusinessObjectImpl
                : theEJBLocalBusinessObjectImpl;
    }


    // Called from EJBObjectImpl.remove, EJBLocalObjectImpl.remove,
    // EJBHomeImpl.remove(Handle).
    protected void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod,
	    boolean local)
	throws RemoveException, EJBException, RemoteException
    {
        if( local ) {
            authorizeLocalMethod(BaseContainer.EJBLocalObject_remove);
        } else {
            authorizeRemoteMethod(BaseContainer.EJBObject_remove);
        }
	ejbProbeNotifier.ejbBeanDestroyedEvent(
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
    }

    /**
     * Force destroy the EJB. Called from postInvokeTx.
     * Note: EJB2.0 section 18.3.1 says that discarding an EJB
     * means that no methods other than finalize() should be invoked on it.
     */
    protected void forceDestroyBean(EJBContextImpl sc) {
        if ( sc.getState() == EJBContextImpl.BeanState.DESTROYED )
                return;

        // mark context as destroyed
        sc.setState(EJBContextImpl.BeanState.DESTROYED);

        //sessionCtxPool.destroyObject(sc);
        pool.destroyObject(sc);
    }


    /**
     * Called when a remote invocation arrives for an EJB.
     */
    protected EJBObjectImpl getEJBObjectImpl(byte[] instanceKey) {
        return theEJBObjectImpl;
    }
    
    EJBObjectImpl getEJBRemoteBusinessObjectImpl(byte[] instanceKey) {
        return theRemoteBusinessObjectImpl;
    }

    /**
    * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
    * a local object reference.
    */
    protected EJBLocalObjectImpl getEJBLocalObjectImpl(Object key) {
        return theEJBLocalObjectImpl;
    }

    /**
    * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
    * a local business object reference.
    */
    EJBLocalObjectImpl getEJBLocalBusinessObjectImpl(Object key) {
        return theEJBLocalBusinessObjectImpl;
    }

    /**
    * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
    * a local business object reference.
    */
    EJBLocalObjectImpl getOptionalEJBLocalBusinessObjectImpl(Object key) {
        return theOptionalEJBLocalBusinessObjectImpl;
    }


    /**
    * Called from preInvoke which is called from the EJBObject
    * for local and remote invocations.
    */
    protected ComponentContext _getContext(EjbInvocation inv) {
        try {
            SessionContextImpl sessionCtx = 
                (SessionContextImpl) pool.getObject(null);
            sessionCtx.setState(EJBContextImpl.BeanState.INVOKING);
            return sessionCtx;
        } catch (Exception ex) {
            throw new EJBException(ex);
        }
    }

    protected EJBContextImpl _constructEJBContextImpl(Object instance) {
	return new SessionContextImpl(instance, this);
    }

    /**
    * called when an invocation arrives and there are no instances
    * left to deliver the invocation to.
    * Called from SessionContextFactory.create() !
    */
    private SessionContextImpl createStatelessEJB()
        throws CreateException
    { 
        EjbInvocation ejbInv = null;
        SessionContextImpl context;

        try {

	    context = (SessionContextImpl) createEjbInstanceAndContext();
	    
            Object ejb = context.getEJB();

            // this allows JNDI lookups from setSessionContext, ejbCreate
            ejbInv = super.createEjbInvocation(ejb, context);
            invocationManager.preInvoke(ejbInv);

            // setSessionContext will be called without a Tx as required
            // by the spec, because the EJBHome.create would have been called
            // after the container suspended any client Tx.
            // setSessionContext is also called before context.setEJBStub
            // because the bean is not allowed to do EJBContext.getEJBObject
            setSessionContext(ejb, context);

            // Perform injection right after where setSessionContext
            // would be called.  This is important since injection methods
            // have the same "operations allowed" permissions as
            // setSessionContext.
            injectEjbInstance(context);

            if ( isRemote ) {

                if( hasRemoteHomeView ) {
                    context.setEJBObjectImpl(theEJBObjectImpl);
                    context.setEJBStub(theEJBStub);
                }
                if( hasRemoteBusinessView ) {
                    context.setEJBRemoteBusinessObjectImpl
                        (theRemoteBusinessObjectImpl);
                }

            }
            if ( isLocal ) {
                if( hasLocalHomeView ) {
                    context.setEJBLocalObjectImpl(theEJBLocalObjectImpl);
                }
                if( hasLocalBusinessView ) {
                    context.setEJBLocalBusinessObjectImpl
                        (theEJBLocalBusinessObjectImpl);
                }
                if( hasOptionalLocalBusinessView ) {
                    context.setOptionalEJBLocalBusinessObjectImpl
                        (theOptionalEJBLocalBusinessObjectImpl);
                }
            }

            // all stateless beans have the same id and same InstanceKey
            context.setInstanceKey(statelessInstanceKey); 

            //Call ejbCreate() or @PostConstruct method
            intercept(CallbackType.POST_CONSTRUCT, context);

            // Set the state to POOLED after ejbCreate so that 
            // EJBContext methods not allowed will throw exceptions
            context.setState(EJBContextImpl.BeanState.POOLED);
        } catch ( Throwable th ) {
            _logger.log(Level.SEVERE, "ejb.stateless_ejbcreate_exception", logParams);
            CreateException creEx = new CreateException("Could not create stateless EJB");
            creEx.initCause(th);
            throw creEx;
        } finally {
            if (ejbInv != null) {
                invocationManager.postInvoke(ejbInv);
            }
        }
        context.touch();
        return context;
    }

    private void setSessionContext(Object ejb, SessionContextImpl context)
            throws Exception {
        if( ejb instanceof SessionBean ) {
            ((SessionBean)ejb).setSessionContext(context);
        }
    }

    protected void doTimerInvocationInit(EjbInvocation inv, Object primaryKey)
            throws Exception {
        // TODO I don't understand this check.  What is ejbObject used for?
        if( isRemote ) {
            //TODO inv.ejbObject = theEJBObjectImpl;
            inv.isLocal = false;
        } else {
            inv.ejbObject = theEJBLocalObjectImpl;
            inv.isLocal = true;
        }
    }

    public boolean userTransactionMethodsAllowed(ComponentInvocation inv) {
        boolean utMethodsAllowed = false;
        if( isBeanManagedTran ) {
            if( inv instanceof EjbInvocation ) {
                EjbInvocation ejbInv = (EjbInvocation) inv;
                EJBContextImpl sc = (EJBContextImpl) ejbInv.context;
                // If Invocation, only ejbRemove not allowed.
                utMethodsAllowed = !sc.isInEjbRemove();
            } else {
                // This will prevent setSessionContext/ejbCreate access
                utMethodsAllowed = false;
            }
        }
        return utMethodsAllowed;
    }

    /**
     * Called from preInvoke which is called from the EJBObject
     * for local and remote invocations.
     */
    public void releaseContext(EjbInvocation inv) {
        SessionContextImpl sc = (SessionContextImpl)inv.context;

        // check if the bean was destroyed
        if ( sc.getState() == EJBContextImpl.BeanState.DESTROYED )
            return;

            sc.setState(EJBContextImpl.BeanState.POOLED);

            // Stateless beans cant have transactions across invocations
            sc.setTransaction(null);
            sc.touch();

            pool.returnObject(sc);
    }


    protected boolean isIdentical(EJBObjectImpl ejbo, EJBObject other)
        throws RemoteException
    {

        if ( other == ejbo.getStub() ) {
            return true;
        }else {
            try {
                // other may be a stub for a remote object.
                // Although all stateless sessionbeans for a bean type
                // are identical, we dont know whether other is of the
                // same bean type as ejbo.
                if ( getProtocolManager().isIdentical(ejbo.getStub(), other) )
                        return true;
                else
                        return false;
            } catch ( Exception ex ) {
                if(_logger.isLoggable(Level.SEVERE)) {
                    _logger.log(Level.SEVERE,"ejb.ejb_getstub_exception",
                        logParams);
                    _logger.log(Level.SEVERE,"",ex);
                }
                throw new RemoteException("Error during isIdentical.", ex);
            }
        }

    }

    /**
    * Check if the given EJBObject/LocalObject has been removed.
    * @exception NoSuchObjectLocalException if the object has been removed.
    */
    protected void checkExists(EJBLocalRemoteObject ejbObj) 
    {
        // For stateless session beans, EJBObject/EJBLocalObj are never removed.
        // So do nothing.
    }


    protected void afterBegin(EJBContextImpl context) {
        // Stateless SessionBeans cannot implement SessionSynchronization!!
        // EJB2.0 Spec 7.8.
    }

    protected void beforeCompletion(EJBContextImpl context) {
        // Stateless SessionBeans cannot implement SessionSynchronization!!
        // EJB2.0 Spec 7.8.
    }

    protected void afterCompletion(EJBContextImpl ctx, int status) {
        // Stateless SessionBeans cannot implement SessionSynchronization!!
        // EJB2.0 Spec 7.8.

        // We dissociate the transaction from the bean in releaseContext above
    }

    // default
    public boolean passivateEJB(ComponentContext context) {
        return false;
    }   
    
    // default
    public void activateEJB(Object ctx, Object instanceKey) {}

/** TODO ???
    public void appendStats(StringBuffer sbuf) {
	sbuf.append("\nStatelessContainer: ")
	    .append("CreateCount=").append(statCreateCount).append("; ")
	    .append("RemoveCount=").append(statRemoveCount).append("; ")
	    .append("]");
    }
**/

    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {

        try {

            if ( hasRemoteHomeView ) {
                    // destroy EJBObject refs
                    // XXX invocations still in progress will get exceptions ??
                remoteHomeRefFactory.destroyReference
                    (theEJBObjectImpl.getStub(), 
                     theEJBObjectImpl.getEJBObject());
            }
            if ( hasRemoteBusinessView ) {
                for(RemoteBusinessIntfInfo next : 
                        remoteBusinessIntfInfo.values()) {
                    next.referenceFactory.destroyReference
                        (theRemoteBusinessObjectImpl.getStub
                            (next.generatedRemoteIntf.getName()),
                         theRemoteBusinessObjectImpl.getEJBObject
                            (next.generatedRemoteIntf.getName()));
                }
            }


            if (pool != null) {
                pool.close();
                poolProbeListener.unregister();
            }

        } catch(Throwable t) {
            _logger.log(Level.FINE, "Exception during conrete StatelessSessionBean cleanup", t);
        }
    }

    public long getMethodReadyCount() {
	return pool.getSize();
    }

    protected class SessionContextFactory
        implements ObjectFactory
    {

        public Object create(Object param) {
            try {
                    return createStatelessEJB();
            } catch (CreateException ex) {
                    throw new EJBException(ex);
            }
        }

        public void destroy(Object obj) {
            SessionContextImpl sessionCtx = (SessionContextImpl) obj;
            // Note: stateless SessionBeans cannot have incomplete transactions
            // in progress. So it is ok to destroy the EJB.

            Object sb = sessionCtx.getEJB();
            if (sessionCtx.getState() != EJBContextImpl.BeanState.DESTROYED) {
                //Called from pool implementation to reduce the pool size.
                //So need to call ejb.ejbRemove()
                // mark context as destroyed
                sessionCtx.setState(EJBContextImpl.BeanState.DESTROYED);

                EjbInvocation ejbInv = null;
                try {
                    // NOTE : Context class-loader is already set by Pool
                    ejbInv = createEjbInvocation(sb, sessionCtx);
                    invocationManager.preInvoke(ejbInv);
                    sessionCtx.setInEjbRemove(true);        
   
                    intercept(CallbackType.PRE_DESTROY, sessionCtx);

                } catch ( Throwable t ) {
                     _logger.log(Level.FINE, "ejbRemove exception", t);
                } finally {

                    sessionCtx.setInEjbRemove(false);
                    if( ejbInv != null ) {
                        invocationManager.postInvoke(ejbInv);
                    }
                }
            } else {
                //Called from forceDestroyBean
                //So NO need to call ejb.ejbRemove()
                // mark the context's transaction for rollback
                Transaction tx = sessionCtx.getTransaction();
                try {
                    if ( (tx != null) && 
                        (tx.getStatus() != Status.STATUS_NO_TRANSACTION ) )  {
                        tx.setRollbackOnly();
                    }	
                } catch ( Exception ex ) {
                     _logger.log(Level.FINE,"forceDestroyBean exception", ex);
                }
            }

            cleanupInstance(sessionCtx);

            // tell the TM to release resources held by the bean
            transactionManager.componentDestroyed(sessionCtx);   

            sessionCtx.setTransaction(null);

            sessionCtx.deleteAllReferences();
        }
    } // SessionContextFactory{}

    private static class PoolProperties {
        int maxPoolSize;
        int poolIdleTimeoutInSeconds;
        int poolResizeQuantity;
        int steadyPoolSize;

        public PoolProperties(EjbContainer ejbContainer, BeanPoolDescriptor beanPoolDes) {

            maxPoolSize = Integer.parseInt(ejbContainer.getMaxPoolSize());
            poolIdleTimeoutInSeconds = Integer.parseInt(
                ejbContainer.getPoolIdleTimeoutInSeconds());
            poolResizeQuantity = Integer.parseInt(
                ejbContainer.getPoolResizeQuantity());
            steadyPoolSize = Integer.parseInt(
                ejbContainer.getSteadyPoolSize());
            if(beanPoolDes != null) {
                int temp = 0;
                if (( temp = beanPoolDes.getMaxPoolSize()) != -1) {
                        maxPoolSize = temp;
                }
                if (( temp = beanPoolDes.getPoolIdleTimeoutInSeconds()) != -1) {
                        poolIdleTimeoutInSeconds = temp;
                }

                if (( temp = beanPoolDes.getPoolResizeQuantity()) != -1) {
                        poolResizeQuantity = temp;
                }
                if (( temp = beanPoolDes.getSteadyPoolSize()) != -1) {
                        steadyPoolSize = temp;
                }
            }
        }
    } // PoolProperties{}

    //Methods for StatelessSessionBeanStatsProvider
    public int getMaxPoolSize() {
        return (poolProp.maxPoolSize <= 0)
	    ? Integer.MAX_VALUE
	    : poolProp.maxPoolSize;
    }

    public int getSteadyPoolSize() {
        return (poolProp.steadyPoolSize <= 0)
	    ? 0
	    : poolProp.steadyPoolSize;
    }


} // StatelessSessionContainer.java

