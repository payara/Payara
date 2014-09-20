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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.ConcurrentAccessException;
import javax.ejb.ConcurrentAccessTimeoutException;
import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.EJBObject;
import javax.ejb.IllegalLoopbackException;
import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionSynchronization;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContextType;
import javax.persistence.SynchronizationType;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import com.sun.appserv.util.cache.CacheListener;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.Container;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.MethodLockInfo;
import com.sun.ejb.base.stats.HAStatefulSessionStoreMonitor;
import com.sun.ejb.base.stats.StatefulSessionStoreMonitor;
import com.sun.ejb.containers.util.cache.LruSessionCache;
import com.sun.ejb.monitoring.probes.EjbCacheProbeProvider;
import com.sun.ejb.monitoring.stats.EjbCacheStatsProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringStatsProvider;
import com.sun.ejb.monitoring.stats.EjbMonitoringUtils;
import com.sun.ejb.monitoring.stats.StatefulSessionBeanStatsProvider;
import com.sun.ejb.spi.container.SFSBContainerCallback;
import com.sun.ejb.spi.container.StatefulEJBContext;
import com.sun.ejb.spi.sfsb.util.SFSBUUIDUtil;
import com.sun.ejb.spi.sfsb.util.SFSBVersionManager;
import com.sun.enterprise.admin.monitor.callflow.ComponentType;
import com.sun.enterprise.container.common.impl.EntityManagerFactoryWrapper;
import com.sun.enterprise.container.common.impl.EntityManagerWrapper;
import com.sun.enterprise.container.common.impl.PhysicalEntityManagerWrapper;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;
import com.sun.enterprise.deployment.EntityManagerReferenceDescriptor;
import com.sun.enterprise.deployment.LifecycleCallbackDescriptor;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.security.SecurityManager;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbRemovalInfo;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.CheckpointAtEndOfMethodDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;
import org.glassfish.flashlight.provider.ProbeProviderFactory;
import org.glassfish.ha.store.api.BackingStore;
import org.glassfish.ha.store.api.BackingStoreException;
import org.glassfish.ha.store.util.SimpleMetadata;
import org.glassfish.logging.annotation.LogMessageInfo;

import static com.sun.ejb.containers.EJBContextImpl.BeanState;
import static com.sun.enterprise.deployment.LifecycleCallbackDescriptor.CallbackType;
import static javax.persistence.SynchronizationType.*;

/**
 * This class provides container functionality specific to stateful
 * SessionBeans.
 * At deployment time, one instance of the StatefulSessionContainer is created
 * for each stateful SessionBean type (i.e. deployment descriptor) in a JAR.
 * <p/>
 * The 5 states of a Stateful EJB (an EJB can be in only 1 state at a time):
 * 1. PASSIVE : has been passivated
 * 2. READY : ready for invocations, no transaction in progress
 * 3. INVOKING : processing an invocation
 * 4. INCOMPLETE_TX : ready for invocations, transaction in progress
 * 5. DESTROYED : does not exist
 *
 * @author Mahesh Kannan
 */

public final class StatefulSessionContainer
        extends BaseContainer
        implements CacheListener, SFSBContainerCallback {

    private static final Logger _logger  = LogFacade.getLogger();

    @LogMessageInfo(
        message = "[SFSBContainer] Exception while  initializing SessionSynchronization methods",
        level = "WARNING")
    private static final String EXCEPTION_WHILE_INITIALIZING_SESSION_SYNCHRONIZATION = "AS-EJB-00012";

    @LogMessageInfo(
        message = "[SFSBContainer] Exception while  loading checkpoint info",
        level = "WARNING")
    private static final String EXCEPTION_WHILE_LOADING_CHECKPOINT = "AS-EJB-00013";

    @LogMessageInfo(
        message = "Exception creating ejb object : [{0}]",
        level = "WARNING")
    private static final String CREATE_EJBOBJECT_EXCEPTION = "AS-EJB-00014";

    @LogMessageInfo(
        message = "Exception creating ejb local object [{0}]",
        level = "WARNING")
    private static final String CREATE_EJBLOCALOBJECT_EXCEPTION = "AS-EJB-00015";

    @LogMessageInfo(
        message = "Couldn't update timestamp for: [{0}]; Exception: [{1}]",
        level = "WARNING")
    private static final String COULDNT_UPDATE_TIMESTAMP_FOR_EXCEPTION = "AS-EJB-00016";

    @LogMessageInfo(
        message = "Cannot register bean for checkpointing",
        level = "WARNING")
    private static final String CANNOT_REGISTER_BEAN_FOR_CHECKPOINTING = "AS-EJB-00017";

    @LogMessageInfo(
        message = "Error  during checkpoint ([{0}]. Key: [{1}]) [{2}]",
        level = "WARNING")
    private static final String ERROR_DURING_CHECKPOINT_3PARAMs = "AS-EJB-00018";

    @LogMessageInfo(
        message = "sfsb checkpoint error. Name: [{0}]",
        level = "WARNING")
    private static final String SFSB_CHECKPOINT_ERROR_NAME = "AS-EJB-00019";

    @LogMessageInfo(
        message = "sfsb checkpoint error. Key: [{0}]",
        level = "WARNING")
    private static final String SFSB_CHECKPOINT_ERROR_KEY = "AS-EJB-00020";

    @LogMessageInfo(
        message = "Exception in afterCompletion : [{0}]",
        level = "INFO")
    private static final String AFTER_COMPLETION_EXCEPTION = "AS-EJB-00021";

    @LogMessageInfo(
        message = "1. passivateEJB() returning because containerState: [{0}]",
        level = "WARNING")
    private static final String PASSIVATE_EJB_RETURNING_BECAUSE_CONTAINER_STATE = "AS-EJB-00022";

    @LogMessageInfo(
        message = "Extended EM not serializable. Exception: [{0}]",
        level = "WARNING")
    private static final String EXTENDED_EM_NOT_SERIALIZABLE = "AS-EJB-00023";

    @LogMessageInfo(
        message = "Error during passivation: [{0}]; [{1}]",
        level = "WARNING")
    private static final String ERROR_DURING_PASSIVATION = "AS-EJB-00024";

    @LogMessageInfo(
        message = "Error during passivation of [{0}]",
        level = "WARNING")
    private static final String PASSIVATION_ERROR_1PARAM = "AS-EJB-00025";

    @LogMessageInfo(
        message = "sfsb passivation error. Key: [{0}]",
        level = "WARNING")
    private static final String SFSB_PASSIVATION_ERROR_1PARAM = "AS-EJB-00026";

    @LogMessageInfo(
        message = "Error during Stateful Session Bean activation for key [{0}]",
        level = "SEVERE",
        cause = "A problem occurred while the container was activating a stateful session bean.  " +
                "One possible cause is that the bean code threw a system exception from its ejbActivate method.",
        action = "Check the stack trace to see whether the exception was thrown from the ejbActivate method " +
                "and if so double-check the application code to determine what caused the exception.")
    private static final String SFSB_ACTIVATION_ERROR = "AS-EJB-00028";

    @LogMessageInfo(
        message = "[{0}]: Error during backingStore.shutdown()",
        level = "WARNING")
    private static final String ERROR_DURING_BACKING_STORE_SHUTDOWN = "AS-EJB-00029";

    @LogMessageInfo(
        message = "[{0}]: Error during  onShutdown()",
        level = "WARNING")
    private static final String ERROR_DURING_ON_SHUTDOWN = "AS-EJB-00030";

    @LogMessageInfo(
        message = "[{0}]: Error while  undeploying ctx. Key: [{1}]",
        level = "WARNING")
    private static final String ERROR_WHILE_UNDEPLOYING_CTX_KEY = "AS-EJB-00031";

    @LogMessageInfo(
        message = "Cannot add idle bean cleanup task",
        level = "WARNING")
    private static final String ADD_CLEANUP_TASK_ERROR = "AS-EJB-00032";

    @LogMessageInfo(
        message = "Got exception during removeExpiredSessions (but the reaper thread is still alive)",
        level = "WARNING")
    private static final String GOT_EXCEPTION_DURING_REMOVE_EXPIRED_SESSIONS = "AS-EJB-00033";

    @LogMessageInfo(
        message = "Error during checkpoint(, but session not destroyed)",
        level = "WARNING")
    private static final String ERROR_DURING_CHECKPOINT_SESSION_ALIVE = "AS-EJB-00034";

    @LogMessageInfo(
        message = "Error during checkpoint",
        level = "WARNING")
    private static final String ERROR_DURING_CHECKPOINT = "AS-EJB-00035";

    @LogMessageInfo(
        message = "Cache is shutting down, {0} stateful session beans will not be restored after restarting " +
                "since passivation is disabled",
        level = "INFO")
    private static final String SFSB_NOT_RESTORED_AFTER_RESTART = "AS-EJB-00050";
    
    // We do not want too many ORB task for passivation
    public static final int MIN_PASSIVATION_BATCH_COUNT = 8;

    private final static long CONCURRENCY_NOT_ALLOWED = 0;
    private final static long BLOCK_INDEFINITELY = -1;

    private long instanceCount = 1;

    private ArrayList passivationCandidates = new ArrayList();
    private Object asyncTaskSemaphore = new Object();


    private int asyncTaskCount = 0;
    private int asyncCummTaskCount = 0;

    private int passivationBatchCount
            = MIN_PASSIVATION_BATCH_COUNT;

    private int containerTrimCount = 0;

    private LruSessionCache sessionBeanCache;
    private BackingStore<Serializable, SimpleMetadata> backingStore;
    private SFSBUUIDUtil uuidGenerator;
    private ArrayList scheduledTimerTasks = new ArrayList();

    private int statMethodReadyCount = 0;

    private Level TRACE_LEVEL = Level.FINE;

    private String ejbName;

    private boolean isHAEnabled;
    private int removalGracePeriodInSeconds;

    private InvocationInfo postConstructInvInfo;
    private InvocationInfo preDestroyInvInfo;
    private InvocationInfo postActivateInvInfo;
    private InvocationInfo prePassivateInvInfo;

    private StatefulSessionStoreMonitor sfsbStoreMonitor;

    private final String traceInfoPrefix;

    private SFSBVersionManager sfsbVersionManager;

    private Method afterBeginMethod;
    private Method beforeCompletionMethod;
    private Method afterCompletionMethod;
    private boolean isPassivationCapable;
    
    /*
     * Cache for keeping ref count for shared extended entity manager.
     * The key in this map is the physical entity manager
     */

    private static final Map<EntityManager, EEMRefInfo> extendedEMReferenceCountMap
            = new HashMap<EntityManager, EEMRefInfo>();

    private static final Map<EEMRefInfoKey, EntityManager> eemKey2EEMMap
            = new HashMap<EEMRefInfoKey, EntityManager>();

    /**
     * This constructor is called from the JarManager when a Jar is deployed.
     *
     * @throws Exception on error
     */
    public StatefulSessionContainer(EjbDescriptor desc,
                                    ClassLoader loader,
                                    SecurityManager sm)
            throws Exception {
        this(ContainerType.STATEFUL, desc, loader, sm);
    }
    public StatefulSessionContainer(ContainerType conType, EjbDescriptor desc,
                                    ClassLoader loader, SecurityManager sm)
            throws Exception {
        super(conType, desc, loader, sm);
        super.createCallFlowAgent(ComponentType.SFSB);
        this.ejbName = desc.getName();

        this.traceInfoPrefix = "sfsb-" + ejbName + ": ";

        postConstructInvInfo = getLifecycleCallbackInvInfo(ejbDescriptor.getPostConstructDescriptors());
        preDestroyInvInfo = getLifecycleCallbackInvInfo(ejbDescriptor.getPreDestroyDescriptors());

        EjbSessionDescriptor sfulDesc = (EjbSessionDescriptor) ejbDescriptor;
        postActivateInvInfo = getLifecycleCallbackInvInfo(sfulDesc.getPostActivateDescriptors());
        prePassivateInvInfo = getLifecycleCallbackInvInfo(sfulDesc.getPrePassivateDescriptors());

        isPassivationCapable = sfulDesc.isPassivationCapable();
    }

    public boolean isPassivationCapable() {
        return isPassivationCapable;
    }
    
    private InvocationInfo getLifecycleCallbackInvInfo(
            Set<LifecycleCallbackDescriptor> lifecycleCallbackDescriptors) throws Exception {
        InvocationInfo inv = new InvocationInfo();
        inv.ejbName = ejbDescriptor.getName();
        inv.methodIntf = MethodDescriptor.LIFECYCLE_CALLBACK;
        inv.txAttr = getTxAttrForLifecycleCallback(lifecycleCallbackDescriptors, 
                -1, Container.TX_NOT_SUPPORTED, Container.TX_REQUIRES_NEW);

        return inv;
    }

    protected void initializeHome()
            throws Exception {
        super.initializeHome();

	    initSessionSyncMethods();

        loadCheckpointInfo();

        registerMonitorableComponents();

    }

    private void initSessionSyncMethods() throws Exception {

	    if( SessionSynchronization.class.isAssignableFrom(ejbClass) ) {

	        try {
		        afterBeginMethod = ejbClass.getMethod("afterBegin", null);
		        beforeCompletionMethod = ejbClass.getMethod("beforeCompletion", null);
		        afterCompletionMethod = ejbClass.getMethod("afterCompletion", Boolean.TYPE);
	        } catch(Exception e) {
		        _logger.log(Level.WARNING, EXCEPTION_WHILE_INITIALIZING_SESSION_SYNCHRONIZATION, e);
	        }
	    } else {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDescriptor;

	        MethodDescriptor afterBeginMethodDesc = sessionDesc.getAfterBeginMethod();
            if( afterBeginMethodDesc != null ) {
                afterBeginMethod = afterBeginMethodDesc.getDeclaredMethod(sessionDesc);

                processSessionSynchMethod(afterBeginMethod);
            }

	        MethodDescriptor beforeCompletionMethodDesc = sessionDesc.getBeforeCompletionMethod();
            if( beforeCompletionMethodDesc != null ) {
                beforeCompletionMethod = beforeCompletionMethodDesc.getDeclaredMethod(sessionDesc);

                processSessionSynchMethod(beforeCompletionMethod);
            }
            
	        MethodDescriptor afterCompletionMethodDesc = sessionDesc.getAfterCompletionMethod();
            if( afterCompletionMethodDesc != null ) {
                afterCompletionMethod = afterCompletionMethodDesc.getDeclaredMethod(sessionDesc);
                if( afterCompletionMethod == null ) {
                    afterCompletionMethod =
                        afterCompletionMethodDesc.getDeclaredMethod(sessionDesc, new Class[] { Boolean.TYPE });
                }
                processSessionSynchMethod(afterCompletionMethod);
            }

	    }

    }

    private void processSessionSynchMethod(Method sessionSynchMethod)
        throws Exception {

        final Method methodAccessible = sessionSynchMethod;

        // SessionSynch method defined through annotation or ejb-jar.xml
        // can have any access modifier so make sure we have permission
        // to invoke it.

        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedExceptionAction() {
                public java.lang.Object run() throws Exception {
                    if( !methodAccessible.isAccessible() ) {
                        methodAccessible.setAccessible(true);
                    }
                    return null;
                }
         });
      
    }

    // Called before invoking a bean with no Tx or with a new Tx.
    // Check if the bean is associated with an unfinished tx.
    protected void checkUnfinishedTx(Transaction prevTx, EjbInvocation inv) {
        try {
            if ( inv.invocationInfo.isBusinessMethod && prevTx != null &&
                prevTx.getStatus() != Status.STATUS_NO_TRANSACTION ) {
                // An unfinished tx exists for the bean.
                // so we cannot invoke the bean with no Tx or a new Tx.
                throw new IllegalStateException(
                    "Bean is associated with a different unfinished transaction");
            }
        } catch (SystemException ex) {
            _logger.log(Level.FINE, "Exception in checkUnfinishedTx", ex);
            throw new EJBException(ex);
        }
    }

    protected void loadCheckpointInfo() {
        try {
            if (isHAEnabled) {
                Iterator iter = invocationInfoMap.values().iterator();
                while (iter.hasNext()) {
                    InvocationInfo info = (InvocationInfo) iter.next();
                    info.checkpointEnabled = false;
                    MethodDescriptor md = new MethodDescriptor(
                            info.method, info.methodIntf);
                    IASEjbExtraDescriptors extraDesc =
                            ejbDescriptor.getIASEjbExtraDescriptors();
                    if (extraDesc != null) {
                        CheckpointAtEndOfMethodDescriptor cpDesc =
                                extraDesc.getCheckpointAtEndOfMethodDescriptor();
                        if (cpDesc != null) {
                            info.checkpointEnabled =
                                    cpDesc.isCheckpointEnabledFor(md);
                        }
                    }

                    if (info.checkpointEnabled) {
                        if (_logger.isLoggable(Level.FINE)) {
                            _logger.log(Level.FINE, "[SFSBContainer] "
                                    + info.method + " MARKED for "
                                    + "end-of-method-checkpoint");
                        }
                    }
                }
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, EXCEPTION_WHILE_LOADING_CHECKPOINT, ex);
        }
    }

    protected void registerMonitorableComponents() {
        super.registerMonitorableComponents();
        cacheProbeListener = new EjbCacheStatsProvider(sessionBeanCache,
                getContainerId(), containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        cacheProbeListener.register();

        try {
            ProbeProviderFactory probeFactory = ejbContainerUtilImpl.getProbeProviderFactory();
            String invokerId = EjbMonitoringUtils.getInvokerId(containerInfo.appName,
                    containerInfo.modName, containerInfo.ejbName);
            cacheProbeNotifier = probeFactory.getProbeProvider(EjbCacheProbeProvider.class, invokerId);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Got ProbeProvider: " + cacheProbeNotifier.getClass().getName());
            }
        } catch (Exception ex) {
            cacheProbeNotifier = new EjbCacheProbeProvider();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Error getting the EjbMonitoringProbeProvider");
            }
        }

        if (isHAEnabled) {
            sfsbStoreMonitor = new HAStatefulSessionStoreMonitor();
        } else {
            sfsbStoreMonitor = new StatefulSessionStoreMonitor();
        }
        sessionBeanCache.setStatefulSessionStoreMonitor(sfsbStoreMonitor);
        _logger.log(Level.FINE, "[SFSBContainer] registered monitorable");
    }

    public String getMonitorAttributeValues() {
        StringBuffer sbuf = new StringBuffer();
        //sbuf.append(storeHelper.getMonitorAttributeValues());
        sbuf.append(" { asyncTaskCount=").append(asyncTaskCount)
                .append("; asyncCummTaskCount=").append(asyncCummTaskCount)
                .append("; passivationBatchCount=").append(passivationBatchCount)
                .append("; passivationQSz=").append(passivationCandidates.size())
                .append("; trimEventCount=").append(containerTrimCount)
                .append(" }");
        return sbuf.toString();
    }

    protected EjbMonitoringStatsProvider getMonitoringStatsProvider(
            String appName, String modName, String ejbName) {
        return new StatefulSessionBeanStatsProvider(this, getContainerId(), appName, modName, ejbName);
    }


/** TODO
    public void appendStats(StringBuffer sbuf) {
        sbuf.append("\nStatefulContainer: ")
                .append("CreateCount=").append(statCreateCount).append("; ")
                .append("RemoveCount=").append(statRemoveCount).append("; ")
                .append("Size=")
                .append(sessionBeanCache.getNumBeansInCache()).append("; ")
                .append("ReadyCount=")
                .append(statMethodReadyCount).append("; ");
        sbuf.append("]");
    }
**/

    private static final String convertCtxStateToString(
            SessionContextImpl sc) {
        switch (sc.getState()) {
            case PASSIVATED:
                return "PASSIVE";
            case READY:
                return "READY";
            case INVOKING:
                return "INVOKING";
            case INCOMPLETE_TX:
                return "INCOMPLETE_TX";
            case DESTROYED:
                return "DESTROYED";
        }
        return "UNKNOWN-STATE";
    }

    protected boolean isIdentical(EJBObjectImpl ejbo, EJBObject other)
            throws RemoteException {

        if (other == ejbo.getStub())
            return true;
        else {
            try {
                // other may be a stub for a remote object.
                if (getProtocolManager().isIdentical(ejbo.getStub(), other))
                    return true;
                else
                    return false;
            } catch (Exception ex) {
                _logger.log(Level.FINE,
                        "Exception while getting stub for ejb", ex);
                throw new RemoteException("Error during isIdentical.", ex);
            }
        }
    }

    /**
     * This is called from the generated "HelloEJBHomeImpl" create method
     * via EJBHomeImpl.createEJBObject.
     * Note: for stateful beans, the HelloEJBHomeImpl.create calls
     * ejbCreate on the new bean after createEJBObject() returns.
     * Return the EJBObject for the bean.
     */
    protected EJBObjectImpl createEJBObjectImpl()
            throws CreateException, RemoteException {
        try {
            SessionContextImpl context = createBeanInstance();
            EJBObjectImpl ejbObjImpl = createEJBObjectImpl(context);
            afterInstanceCreation(context);
            return ejbObjImpl;
        }
        catch (Exception ex) {

            _logger.log(Level.WARNING, CREATE_EJBOBJECT_EXCEPTION, new Object[]{ejbDescriptor.getName(), ex});

            if (ex instanceof EJBException)
                throw (EJBException) ex;
            else {
                CreateException ce =
                        new CreateException("ERROR creating stateful SessionBean");
                ce.initCause(ex);
                throw ce;
            }
        }
    }

    protected EJBObjectImpl createRemoteBusinessObjectImpl()
            throws CreateException, RemoteException {
        try {
            SessionContextImpl context = createBeanInstance();
            EJBObjectImpl ejbBusinessObjImpl =
                    createRemoteBusinessObjectImpl(context);
            afterInstanceCreation(context);
            return ejbBusinessObjImpl;
        }
        catch (Exception ex) {

            _logger.log(Level.WARNING, CREATE_EJBOBJECT_EXCEPTION, new Object[]{ejbDescriptor.getName(), ex});

            if (ex instanceof EJBException)
                throw (EJBException) ex;
            else {
                CreateException ce =
                        new CreateException("ERROR creating stateful SessionBean");
                ce.initCause(ex);
                throw ce;
            }
        }
    }


    /**
     * This is called from the generated "HelloEJBLocalHomeImpl" create method
     * via EJBLocalHomeImpl.createEJBObject.
     * Note: for stateful beans, the HelloEJBLocalHomeImpl.create calls
     * ejbCreate on the new bean after createEJBLocalObjectImpl() returns.
     * Return the EJBLocalObject for the bean.
     */
    protected EJBLocalObjectImpl createEJBLocalObjectImpl()
            throws CreateException {
        try {
            SessionContextImpl context = createBeanInstance();

            EJBLocalObjectImpl localObjImpl =
                    createEJBLocalObjectImpl(context);

            afterInstanceCreation(context);

            return localObjImpl;
        }
        catch (Exception ex) {

            _logger.log(Level.WARNING, CREATE_EJBLOCALOBJECT_EXCEPTION, new Object[]{ejbDescriptor.getName(), ex});

            if (ex instanceof EJBException)
                throw (EJBException) ex;
            else {
                CreateException ce =
                        new CreateException("ERROR creating stateful SessionBean");
                ce.initCause(ex);
                throw ce;
            }
        }
    }

    /**
     * Internal creation event for Local Business view of SFSB
     */
    EJBLocalObjectImpl createEJBLocalBusinessObjectImpl(boolean localBeanView)
            throws CreateException {
        try {

            
            SessionContextImpl context = createBeanInstance();

            EJBLocalObjectImpl localBusinessObjImpl = localBeanView ?
                createOptionalEJBLocalBusinessObjectImpl(context) :
                createEJBLocalBusinessObjectImpl(context);

            afterInstanceCreation(context);

            return localBusinessObjImpl;
        }
        catch (Exception ex) {

            _logger.log(Level.WARNING, CREATE_EJBLOCALOBJECT_EXCEPTION, new Object[]{ejbDescriptor.getName(), ex});

            if (ex instanceof EJBException)
                throw (EJBException) ex;
            else {
                CreateException ce =
                        new CreateException("ERROR creating stateful SessionBean");
                ce.initCause(ex);
                throw ce;
            }
        }
    }

   @Override
   protected EJBContextImpl _constructEJBContextImpl(Object instance) {
	return new SessionContextImpl(instance, this);
    }

    @Override
    protected Object _constructEJBInstance() throws Exception {
	return  (sfsbSerializedClass != null) ?
	    sfsbSerializedClass.newInstance() : ejbClass.newInstance();
    }

    @Override
    protected boolean suspendTransaction(EjbInvocation inv) throws Exception {
        SessionContextImpl sc = (SessionContextImpl) inv.context;
        return !(inv.invocationInfo.isBusinessMethod || sc.getInLifeCycleCallback());
    }

    @Override
    protected boolean resumeTransaction(EjbInvocation inv) throws Exception {
        SessionContextImpl sc = (SessionContextImpl) inv.context;
        return !(inv.invocationInfo.isBusinessMethod || sc.getInLifeCycleCallback());
    }

    /**
     * Create a new Session Bean and set Session Context.
     */
    private SessionContextImpl createBeanInstance()
            throws Exception {
        EjbInvocation ejbInv = null;
        try {

	    SessionContextImpl context = (SessionContextImpl) 
		createEjbInstanceAndContext();

            Object ejb = context.getEJB();

            Object sessionKey = uuidGenerator.createSessionKey();
            createExtendedEMs(context, sessionKey);

            // Need to do preInvoke because setSessionContext can access JNDI
            ejbInv = super.createEjbInvocation(ejb, context);
            invocationManager.preInvoke(ejbInv);

            // setSessionContext will be called without a Tx as required
            // by the spec, because the EJBHome.create would have been called
            // after the container suspended any client Tx.
            // setSessionContext is also called before createEJBObject because
            // the bean is not allowed to do EJBContext.getEJBObject here
            if (ejb instanceof SessionBean) {
                ((SessionBean) ejb).setSessionContext(context);
            }

            // Perform injection right after where setSessionContext
            // would be called.  This is important since injection methods
            // have the same "operations allowed" permissions as
            // setSessionContext.
            injectEjbInstance(context);
            
            // Set the timestamp before inserting into bean store, else
            // Recycler might go crazy and remove this bean!
            context.touch();

            // Add the EJB into the session store
            // and get the instanceKey for this EJB instance.
            // XXX The store operation could be avoided for local-only beans.

            sessionBeanCache.put(sessionKey, context);
            context.setInstanceKey(sessionKey);


            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, "[SFSBContainer] Created "
                        + "session: " + sessionKey);
            }

            return context;
        } catch (Exception ex) {
            throw ex;
        } catch (Throwable t) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(t);
            throw ejbEx;
        } finally {
            if (ejbInv != null) {
                invocationManager.postInvoke(ejbInv);
            }
        }
    }

    private void createExtendedEMs(SessionContextImpl ctx, Object sessionKey) {
        Set<EntityManagerReferenceDescriptor> emRefs
                = ejbDescriptor.getEntityManagerReferenceDescriptors();
        Iterator<EntityManagerReferenceDescriptor> iter = emRefs.iterator();
        Set<EEMRefInfo> eemRefInfos = new HashSet<EEMRefInfo>();
        while (iter.hasNext()) {
            EntityManagerReferenceDescriptor refDesc = iter.next();
            if (refDesc.getPersistenceContextType() ==
                    PersistenceContextType.EXTENDED) {
                String unitName = refDesc.getUnitName();
                EntityManagerFactory emf =
                        EntityManagerFactoryWrapper.lookupEntityManagerFactory(
                                ComponentInvocation.ComponentInvocationType.EJB_INVOCATION,
                                unitName, ejbDescriptor);
                if (emf != null) {
                    PhysicalEntityManagerWrapper physicalEntityManagerWrapper = findExtendedEMFromInvList(emf);

                    if (physicalEntityManagerWrapper == null) {
                        // We did not find an extended EM that we can inherit from. Create one
                        try {
                            EntityManager em = emf.createEntityManager(refDesc.getSynchronizationType(), refDesc.getProperties());
                            physicalEntityManagerWrapper = new PhysicalEntityManagerWrapper(em, refDesc.getSynchronizationType());
                        } catch (Throwable th) {
                            EJBException ejbEx = new EJBException
                                    ("Couldn't create EntityManager for"
                                            + " refName: " + refDesc.getName()
                                            + "; unitname: " + unitName);
                            ejbEx.initCause(th);
                            throw ejbEx;
                        }
                    } else {
                        // We found an extended EM we can inherit from. Validate that sync type matches
                        if(physicalEntityManagerWrapper.getSynchronizationType() != refDesc.getSynchronizationType()) {
                            throw new EJBException("The current invocation inherits a persistence context of synchronization type '" + physicalEntityManagerWrapper.getSynchronizationType() +
                                    "' where as it references a persistence context of synchronization type '" + refDesc.getSynchronizationType() +
                                    "' refName: " + refDesc.getName() +
                                    " unitName: " + unitName );
                        }
                    }
                    String emRefName = refDesc.getName();
                    long containerID = this.getContainerId();
                    EEMRefInfo refInfo = null;
                    synchronized (extendedEMReferenceCountMap) {
                        refInfo = extendedEMReferenceCountMap.get(physicalEntityManagerWrapper.getEM());
                        if (refInfo != null) {
                            refInfo.refCount++;
                        } else {
                            refInfo = new EEMRefInfo(emRefName, refDesc.getUnitName(), refDesc.getSynchronizationType(), containerID,
                                    sessionKey, physicalEntityManagerWrapper.getEM(), emf);
                            refInfo.refCount = 1;
                            extendedEMReferenceCountMap.put(physicalEntityManagerWrapper.getEM(), refInfo);
                            eemKey2EEMMap.put(refInfo.getKey(), refInfo.getEntityManager());
                        }
                    }
                    ctx.addExtendedEntityManagerMapping(emf, refInfo);
                    eemRefInfos.add(refInfo);
                } else {
                    throw new EJBException("EMF is null. Couldn't get extended EntityManager for"
                            + " refName: " + refDesc.getName()
                            + "; unitname: " + unitName);
                }
            }
        }

        if (eemRefInfos.size() > 0) {
            ctx.setEEMRefInfos(eemRefInfos);
        }
    }

    private PhysicalEntityManagerWrapper findExtendedEMFromInvList(EntityManagerFactory emf) {
        PhysicalEntityManagerWrapper em = null;

        ComponentInvocation compInv = (ComponentInvocation)
                invocationManager.getCurrentInvocation();
        if (compInv != null) {
            if (compInv.getInvocationType() == ComponentInvocation.ComponentInvocationType.EJB_INVOCATION) {
                EjbInvocation ejbInv = (EjbInvocation) compInv;
                if (ejbInv.context instanceof SessionContextImpl) {
                    SessionContextImpl ctxImpl = (SessionContextImpl) ejbInv.context;
                    if (ctxImpl.container instanceof StatefulSessionContainer) {
                        em = ctxImpl.getExtendedEntityManager(emf);
                    }
                }
            }
        }

        return em;
    }

    public EntityManager lookupExtendedEntityManager(EntityManagerFactory emf) {
        PhysicalEntityManagerWrapper physicalEntityManagerWrapper = findExtendedEMFromInvList(emf);
        return physicalEntityManagerWrapper == null ? null : physicalEntityManagerWrapper.getEM();
    }

    private void afterInstanceCreation(SessionContextImpl context)
            throws Exception {

        context.setState(BeanState.READY);

        EjbInvocation ejbInv = null;
        boolean inTx = false;
        try {
            // Need to do preInvoke because setSessionContext can access JNDI
            ejbInv = super.createEjbInvocation(context.getEJB(), context);
            invocationManager.preInvoke(ejbInv);

            // PostConstruct must be called after state set to something
                // other than CREATED
            inTx = callLifecycleCallbackInTxIfUsed(ejbInv, context, postConstructInvInfo, CallbackType.POST_CONSTRUCT);
        } catch (Throwable t) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(t);
            throw ejbEx;
        } finally {
            if (ejbInv != null) {
                try {
                    invocationManager.postInvoke(ejbInv);
                    if( inTx ) {
                        // Call directly to report exception
                        postInvokeTx(ejbInv);
                    }
                } catch(Exception pie) {
                    if (ejbInv.exception != null) {
                        _logger.log(Level.FINE, "Exception during SFSB startup postInvoke ", pie);
                    } else {
                        ejbInv.exception = pie;
                        CreateException creEx = new CreateException("Initialization failed for Stateful Session Bean " +
                                        ejbDescriptor.getName());
                        creEx.initCause(pie);
                        throw creEx;
                    }
                } finally {
                    context.setInLifeCycleCallback(false);
                }
            }

        }

        ejbProbeNotifier.ejbBeanCreatedEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
        incrementMethodReadyStat();


    }


    // called from createEJBObject and activateEJB and createEJBLocalObjectImpl
    private EJBLocalObjectImpl createEJBLocalObjectImpl
            (SessionContextImpl context) throws Exception {
        if (context.getEJBLocalObjectImpl() != null)
            return context.getEJBLocalObjectImpl();

        // create EJBLocalObject
        EJBLocalObjectImpl localObjImpl = instantiateEJBLocalObjectImpl(context.getInstanceKey());

        // introduce context and EJBLocalObject to each other
        context.setEJBLocalObjectImpl(localObjImpl);
        localObjImpl.setContext(context);

        if (hasLocalBusinessView) {
            createEJBLocalBusinessObjectImpl(context);
        }

        if (hasOptionalLocalBusinessView) {
            createOptionalEJBLocalBusinessObjectImpl(context);
        }

        if (hasRemoteHomeView) {
            createEJBObjectImpl(context);  // enable remote invocations too
        }

        if (hasRemoteBusinessView) {
            createRemoteBusinessObjectImpl(context);
        }

        return localObjImpl;
    }

    private EJBLocalObjectImpl createEJBLocalBusinessObjectImpl
            (SessionContextImpl context) throws Exception {
        if (context.getEJBLocalBusinessObjectImpl() != null)
            return context.getEJBLocalBusinessObjectImpl();

        EJBLocalObjectImpl localBusinessObjImpl =
                instantiateEJBLocalBusinessObjectImpl();

        context.setEJBLocalBusinessObjectImpl(localBusinessObjImpl);
        localBusinessObjImpl.setContext(context);
        localBusinessObjImpl.setKey(context.getInstanceKey());

        if (hasOptionalLocalBusinessView) {
            createOptionalEJBLocalBusinessObjectImpl(context);
        }

        if (hasLocalHomeView) {
            createEJBLocalObjectImpl(context);
        }

        if (hasRemoteHomeView) {
            createEJBObjectImpl(context);  // enable remote invocations too
        }

        if (hasRemoteBusinessView) {
            createRemoteBusinessObjectImpl(context);
        }

        return localBusinessObjImpl;
    }

    private EJBLocalObjectImpl createOptionalEJBLocalBusinessObjectImpl
            (SessionContextImpl context) throws Exception {
        if (context.getOptionalEJBLocalBusinessObjectImpl() != null)
            return context.getOptionalEJBLocalBusinessObjectImpl();

        EJBLocalObjectImpl optionalLocalBusinessObjImpl =
                instantiateOptionalEJBLocalBusinessObjectImpl();

        context.setOptionalEJBLocalBusinessObjectImpl(optionalLocalBusinessObjImpl);
        optionalLocalBusinessObjImpl.setContext(context);
        optionalLocalBusinessObjImpl.setKey(context.getInstanceKey());

        if (hasLocalBusinessView) {
            createEJBLocalBusinessObjectImpl(context);
        }
        
        if (hasLocalHomeView) {
            createEJBLocalObjectImpl(context);
        }

        if (hasRemoteHomeView) {
            createEJBObjectImpl(context);  // enable remote invocations too
        }

        if (hasRemoteBusinessView) {
            createRemoteBusinessObjectImpl(context);
        }

        return optionalLocalBusinessObjImpl;
    }

    // called from createEJBObject and activateEJB and createEJBLocalObjectImpl
    private EJBObjectImpl createEJBObjectImpl(SessionContextImpl context)
            throws Exception {

        if (context.getEJBObjectImpl() != null)
            return context.getEJBObjectImpl();

        // create EJBObject and associate it with the key
        Object sessionKey = context.getInstanceKey();
        EJBObjectImpl ejbObjImpl = instantiateEJBObjectImpl(null, sessionKey);

        // introduce context and EJBObject to each other
        context.setEJBObjectImpl(ejbObjImpl);
        ejbObjImpl.setContext(context);

        // connect the EJBObject to the ProtocolManager
        // (creates the client-side stub too)
        byte[] sessionOID = uuidGenerator.keyToByteArray(sessionKey);
        EJBObject ejbStub = (EJBObject)
                remoteHomeRefFactory.createRemoteReference(sessionOID);

        context.setEJBStub(ejbStub);
        ejbObjImpl.setStub(ejbStub);

        if (hasRemoteBusinessView) {
            createRemoteBusinessObjectImpl(context);
        }

        if (isLocal) {
            if (hasLocalHomeView) {
                // enable local home invocations too
                createEJBLocalObjectImpl(context);
            }
            if (hasLocalBusinessView) {
                // enable local business invocations too
                createEJBLocalBusinessObjectImpl(context);
            }
            if (hasOptionalLocalBusinessView) {
                createOptionalEJBLocalBusinessObjectImpl(context);
            }
        }

        return ejbObjImpl;
    }

    private EJBObjectImpl createRemoteBusinessObjectImpl
            (SessionContextImpl context) throws Exception {

        if (context.getEJBRemoteBusinessObjectImpl() != null)
            return context.getEJBRemoteBusinessObjectImpl();

        // create EJBObject
        EJBObjectImpl ejbBusinessObjImpl =
                instantiateRemoteBusinessObjectImpl();

        context.setEJBRemoteBusinessObjectImpl(ejbBusinessObjImpl);
        ejbBusinessObjImpl.setContext(context);
        Object sessionKey = context.getInstanceKey();
        ejbBusinessObjImpl.setKey(sessionKey);

        // connect the Remote object to the ProtocolManager
        // (creates the client-side stub too)
        byte[] sessionOID = uuidGenerator.keyToByteArray(sessionKey);
        for (RemoteBusinessIntfInfo next : remoteBusinessIntfInfo.values()) {

            java.rmi.Remote stub = next.referenceFactory.
                    createRemoteReference(sessionOID);

            ejbBusinessObjImpl.setStub(next.generatedRemoteIntf.getName(),
                    stub);
        }

        if (hasRemoteHomeView) {
            createEJBObjectImpl(context);
        }

        if (isLocal) {
            if (hasLocalHomeView) {
                // enable local home invocations too
                createEJBLocalObjectImpl(context);
            }
            if (hasLocalBusinessView) {
                // enable local business invocations too
                createEJBLocalBusinessObjectImpl(context);
            }
            if (hasOptionalLocalBusinessView) {
                createOptionalEJBLocalBusinessObjectImpl(context);
            }
        }

        return ejbBusinessObjImpl;
    }


    // Called from EJBObjectImpl.remove, EJBLocalObjectImpl.remove,
    // EJBHomeImpl.remove(Handle).
    protected void removeBean(EJBLocalRemoteObject ejbo, Method removeMethod,
                    boolean local)
            throws RemoveException, EJBException {
        EjbInvocation ejbInv = super.createEjbInvocation();
        ejbInv.ejbObject = ejbo;
        ejbInv.isLocal = local;
        ejbInv.isRemote = !local;
        ejbInv.method = removeMethod;

        // Method must be a remove method defined on one of :
        // javax.ejb.EJBHome, javax.ejb.EJBObject, javax.ejb.EJBLocalHome,
        // javax.ejb.EJBLocalObject
        Class declaringClass = removeMethod.getDeclaringClass();
        ejbInv.isHome = ((declaringClass == javax.ejb.EJBHome.class) ||
                (declaringClass == javax.ejb.EJBLocalHome.class));

        try {
            preInvoke(ejbInv);
            removeBean(ejbInv);
        } catch (Exception e) {
            _logger.log(Level.FINE, "Exception while running pre-invoke : ejbName = [{0}]", e);
            ejbInv.exception = e;
        } finally {
            /*TODO
            if (AppVerification.doInstrument()) {
                AppVerification.getInstrumentLogger().doInstrumentForEjb
                        (ejbDescriptor, removeMethod, i.exception);
            }
            */
            postInvoke(ejbInv);
        }

        if (ejbInv.exception != null) {
            if (ejbInv.exception instanceof RemoveException) {
                throw (RemoveException) ejbInv.exception;
            } else if (ejbInv.exception instanceof RuntimeException) {
                throw (RuntimeException) ejbInv.exception;
            } else if (ejbInv.exception instanceof Exception) {
                throw new EJBException((Exception) ejbInv.exception);
            } else {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(ejbInv.exception);
                throw ejbEx;
            }
        }
    }


    /**
     * Called from EJBObjectImpl.remove().
     * Note: preInvoke and postInvoke are called for remove().
     */
    private void removeBean(EjbInvocation inv)
            throws RemoveException {
        // At this point the EJB's state is always INVOKING
        // because EJBObjectImpl.remove() called preInvoke().

        try {
            ejbProbeNotifier.ejbBeanDestroyedEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
            SessionContextImpl sc = (SessionContextImpl) inv.context;

            Transaction tc = sc.getTransaction();
		
            if (tc != null && tc.getStatus() !=
                    Status.STATUS_NO_TRANSACTION) {
                // EJB2.0 section 7.6.4: remove must always be called without
                // a transaction.
                throw new RemoveException("Cannot remove EJB: transaction in progress");
            }

            // call ejbRemove on the EJB
            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, "[SFSBContainer] Removing "
                        + "session: " + sc.getInstanceKey());
            }

            sc.setInEjbRemove(true);
            try {
                destroyBean(inv, sc);
            } catch (Throwable t) {
                _logger.log(Level.FINE,
                        "exception thrown from SFSB PRE_DESTROY", t);
            } finally {
                sc.setInEjbRemove(false);
            }
            forceDestroyBean(sc);
        }
        catch (EJBException ex) {
            _logger.log(Level.FINE, "EJBException in removing bean", ex);
            throw ex;
        }
        catch (RemoveException ex) {
            _logger.log(Level.FINE, "Remove exception while removing bean", ex);
            throw ex;
        }
        catch (Exception ex) {
            _logger.log(Level.FINE, "Some exception while removing bean", ex);
            throw new EJBException(ex);
        }
    }


    /**
     * Force destroy the EJB and rollback any Tx it was associated with
     * Called from removeBean, timeoutBean and BaseContainer.postInvokeTx.
     * Note: EJB2.0 section 18.3.1 says that discarding an EJB
     * means that no methods other than finalize() should be invoked on it.
     */
    protected void forceDestroyBean(EJBContextImpl ctx) {
        SessionContextImpl sc = (SessionContextImpl) ctx;

        synchronized (sc) {
            if (sc.getState() == EJBContextImpl.BeanState.DESTROYED)
                return;

            // mark context as destroyed so no more invocations happen on it
            sc.setState(BeanState.DESTROYED);

            cleanupInstance(ctx);
            
            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, "[SFSBContainer] (Force)Destroying "
                        + "session: " + sc.getInstanceKey());
            }


            Transaction prevTx = sc.getTransaction();
            try {
                if (prevTx != null && prevTx.getStatus() !=
                        Status.STATUS_NO_TRANSACTION) {
                    prevTx.setRollbackOnly();
                }
            } catch (SystemException ex) {
                throw new EJBException(ex);
            } catch (IllegalStateException ex) {
                throw new EJBException(ex);
            }

            // remove the bean from the session store
            Object sessionKey = sc.getInstanceKey();
            sessionBeanCache.remove(sessionKey, sc.existsInStore());

            if (isRemote) {

                if (hasRemoteHomeView) {
                    // disconnect the EJBObject from the context and vice versa
                    EJBObjectImpl ejbObjImpl = sc.getEJBObjectImpl();
                    ejbObjImpl.clearContext();
                    ejbObjImpl.setRemoved(true);
                    sc.setEJBObjectImpl(null);

                    // disconnect the EJBObject from the ProtocolManager
                    // so that no remote invocations can reach the EJBObject
                    remoteHomeRefFactory.destroyReference
                            (ejbObjImpl.getStub(), ejbObjImpl.getEJBObject());
                }

                if (hasRemoteBusinessView) {

                    EJBObjectImpl ejbBusinessObjImpl =
                            sc.getEJBRemoteBusinessObjectImpl();
                    ejbBusinessObjImpl.clearContext();
                    ejbBusinessObjImpl.setRemoved(true);
                    sc.setEJBRemoteBusinessObjectImpl(null);

                    for (RemoteBusinessIntfInfo next :
                            remoteBusinessIntfInfo.values()) {
                        // disconnect from the ProtocolManager
                        // so that no remote invocations can get through
                        next.referenceFactory.destroyReference
                                (ejbBusinessObjImpl.getStub
                                        (next.generatedRemoteIntf.getName()),
                                        ejbBusinessObjImpl.getEJBObject
                                                (next.generatedRemoteIntf.getName()));
                    }
                }

            }

            if (isLocal) {
                if (hasLocalHomeView) {
                    // disconnect the EJBLocalObject from the context 
                    // and vice versa
                    EJBLocalObjectImpl localObjImpl =
                            (EJBLocalObjectImpl) sc.getEJBLocalObjectImpl();
                    localObjImpl.clearContext();
                    localObjImpl.setRemoved(true);
                    sc.setEJBLocalObjectImpl(null);
                }
                if (hasLocalBusinessView) {
                    // disconnect the EJBLocalObject from the context 
                    // and vice versa
                    EJBLocalObjectImpl localBusinessObjImpl =
                            (EJBLocalObjectImpl) sc.getEJBLocalBusinessObjectImpl();
                    localBusinessObjImpl.clearContext();
                    localBusinessObjImpl.setRemoved(true);
                    sc.setEJBLocalBusinessObjectImpl(null);
                }
                if (hasOptionalLocalBusinessView) {
                    EJBLocalObjectImpl optionalLocalBusinessObjImpl =
                            (EJBLocalObjectImpl) sc.getOptionalEJBLocalBusinessObjectImpl();
                    optionalLocalBusinessObjImpl.clearContext();
                    optionalLocalBusinessObjImpl.setRemoved(true);
                    sc.setOptionalEJBLocalBusinessObjectImpl(null);
                }
            }

            destroyExtendedEMsForContext(sc);

            // tell the TM to release resources held by the bean
            transactionManager.componentDestroyed(sc);

//            if (checkpointPolicy.isHAEnabled()) {
                //Remove any SFSBClientVersions
                //TODO SFSBClientVersionManager.removeClientVersion(getContainerId(), sessionKey);
//            }
        }
    }

    private void destroyExtendedEMsForContext(SessionContextImpl sc) {
        for (PhysicalEntityManagerWrapper emWrapper : sc.getExtendedEntityManagers()) {
            synchronized (extendedEMReferenceCountMap) {
                EntityManager em = emWrapper.getEM();
                if (extendedEMReferenceCountMap.containsKey(em)) {
                    EEMRefInfo refInfo = extendedEMReferenceCountMap.get(em);
                    if (refInfo.refCount > 1) {
                        refInfo.refCount--;
                        _logger.log(Level.FINE,
                                "Decremented RefCount ExtendedEM em: " + em);
                    } else {
                        _logger.log(Level.FINE, "DESTROYED ExtendedEM em: "
                                + em);
                        refInfo = extendedEMReferenceCountMap.remove(em);
                        eemKey2EEMMap.remove(refInfo.getKey());
                        try {
                            em.close();
                        } catch (Throwable th) {
                            _logger.log(Level.FINE,
                                    "Exception during em.close()", th);
                        }
                    }
                }
            }
        }
    }

    public boolean userTransactionMethodsAllowed(ComponentInvocation inv) {
        boolean utMethodsAllowed = false;

        if (isBeanManagedTran) {
            if (inv instanceof EjbInvocation) {
                SessionContextImpl sc = (SessionContextImpl) ((EjbInvocation) inv).context;
                // This will prevent setSessionContext access to
                // UserTransaction methods.
                utMethodsAllowed = (sc.getInstanceKey() != null);
            } else {
                utMethodsAllowed = true;
            }
        }

        return utMethodsAllowed;
    }


    public void removeTimedoutBean(EJBContextImpl ctx) {
        // check if there is an invocation in progress for
        // this instance.
        synchronized (ctx) {
            if (ctx.getState() != BeanState.INVOKING) {
                try {
                    // call ejbRemove on the bean
                    ctx.setInEjbRemove(true);
                    destroyBean(null, ctx);
                } catch (Throwable t) {
                    _logger.log(Level.FINE, "ejbRemove exception", t);
                } finally {
                    ctx.setInEjbRemove(false);
                }

                if (_logger.isLoggable(TRACE_LEVEL)) {
                    SessionContextImpl sc = (SessionContextImpl) ctx;
                    _logger.log(TRACE_LEVEL, "[SFSBContainer] Removing TIMEDOUT "
                            + "session: " + sc.getInstanceKey());
                }

                forceDestroyBean(ctx);
            }
        }
    }

    /**
     * Called when a remote invocation arrives for an EJB.
     *
     * @throws NoSuchObjectLocalException if the target object does not exist
     */
    private SessionContextImpl _getContextForInstance(byte[] instanceKey) {

        Serializable sessionKey = (Serializable) uuidGenerator.byteArrayToKey(instanceKey, 0, -1);

        if (_logger.isLoggable(TRACE_LEVEL)) {
            _logger.log(TRACE_LEVEL, "[SFSBContainer] Got request for: "
                    + sessionKey);
        }
        while (true) {
            SessionContextImpl sc = (SessionContextImpl)
                    sessionBeanCache.lookupEJB(sessionKey, this, null);

            if (sc == null) {
                // EJB2.0 section 7.6
                // Note: the NoSuchObjectLocalException gets converted to a
                // remote exception by the protocol manager.
                throw new NoSuchObjectLocalException(
                        "Invalid Session Key ( " + sessionKey + ")");
            }

            synchronized (sc) {
                switch (sc.getState()) {
                    case PASSIVATED:      //Next cache.lookup() == different_ctx
                    case DESTROYED:    //Next cache.lookup() == null
                        break;
                    default:

                        return sc;
                }
            }
        }
    }


    protected EJBObjectImpl getEJBObjectImpl(byte[] instanceKey) {
        SessionContextImpl sc = _getContextForInstance(instanceKey);
        return sc.getEJBObjectImpl();
    }

    EJBObjectImpl getEJBRemoteBusinessObjectImpl(byte[] instanceKey) {
        SessionContextImpl sc = _getContextForInstance(instanceKey);
        return sc.getEJBRemoteBusinessObjectImpl();
    }

    /**
     * Called from EJBLocalObjectImpl.getLocalObject() while deserializing
     * a local object reference.
     */
    protected EJBLocalObjectImpl getEJBLocalObjectImpl(Object sessionKey) {

        // Create an EJBLocalObject reference which
        // is *not* associated with a SessionContext.  That way, the
        // session bean context lookup will be done lazily whenever
        // the reference is actually accessed.  This avoids I/O in the
        // case that the reference points to a passivated session bean.
        // It's also consistent with the deserialization approach used
        // throughout the container.  e.g. a timer reference is deserialized
        // from its handle without checking it against the timer database.

        EJBLocalObjectImpl localObjImpl;

        try {
            localObjImpl = instantiateEJBLocalObjectImpl(sessionKey);
        } catch (Exception ex) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;
        }

        return localObjImpl;
    }

    EJBLocalObjectImpl getEJBLocalBusinessObjectImpl(Object sessionKey) {

        // Create an EJBLocalObject reference which
        // is *not* associated with a SessionContext.  That way, the
        // session bean context lookup will be done lazily whenever
        // the reference is actually accessed.  This avoids I/O in the
        // case that the reference points to a passivated session bean.
        // It's also consistent with the deserialization approach used
        // throughout the container.  e.g. a timer reference is deserialized
        // from its handle without checking it against the timer database.

        EJBLocalObjectImpl localBusinessObjImpl;

        try {
            localBusinessObjImpl = instantiateEJBLocalBusinessObjectImpl();

            localBusinessObjImpl.setKey(sessionKey);

        } catch (Exception ex) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;
        }

        return localBusinessObjImpl;
    }

    EJBLocalObjectImpl getOptionalEJBLocalBusinessObjectImpl(Object sessionKey) {

        // Create an EJBLocalObject reference which
        // is *not* associated with a SessionContext.  That way, the
        // session bean context lookup will be done lazily whenever
        // the reference is actually accessed.  This avoids I/O in the
        // case that the reference points to a passivated session bean.
        // It's also consistent with the deserialization approach used
        // throughout the container.  e.g. a timer reference is deserialized
        // from its handle without checking it against the timer database.

        EJBLocalObjectImpl localBusinessObjImpl;

        try {
            localBusinessObjImpl = instantiateOptionalEJBLocalBusinessObjectImpl();

            localBusinessObjImpl.setKey(sessionKey);

        } catch (Exception ex) {
            EJBException ejbEx = new EJBException();
            ejbEx.initCause(ex);
            throw ejbEx;
        }

        return localBusinessObjImpl;
    }

    /**
     * Check if the given EJBObject/LocalObject has been removed.
     *
     * @throws NoSuchObjectLocalException if the object has been removed.
     */
    protected void checkExists(EJBLocalRemoteObject ejbObj) {
        if (ejbObj.isRemoved())
            throw new NoSuchObjectLocalException("Bean has been removed");
    }

    private final void logTraceInfo(EjbInvocation inv, Object key, String message) {
        _logger.log(TRACE_LEVEL, traceInfoPrefix + message
                + " for " + inv.method.getName() + "; key: " + key);
    }

    private final void logTraceInfo(SessionContextImpl sc, String message) {
        _logger.log(TRACE_LEVEL, traceInfoPrefix + message
                + " for key: " + sc.getInstanceKey()
                + "; " + System.identityHashCode(sc));
    }

    /**
     * Called from preInvoke which is called from the EJBObject
     * for local and remote invocations.
     */
    public ComponentContext _getContext(EjbInvocation inv) {
        EJBLocalRemoteObject ejbo = inv.ejbObject;
        SessionContextImpl sc = ejbo.getContext();
        Serializable sessionKey = (Serializable) ejbo.getKey();

        if (_logger.isLoggable(TRACE_LEVEL)) {
            logTraceInfo(inv, sessionKey, "Trying to get context");
        }

        if (sc == null) {
            // This is possible if the EJB was destroyed or passivated.
            // Try to activate it again.
            sc = (SessionContextImpl) sessionBeanCache.lookupEJB(
                    sessionKey, this, ejbo);
        }

        if ((sc == null) || (sc.getState() == BeanState.DESTROYED)) {
            if (_logger.isLoggable(TRACE_LEVEL)) {
                logTraceInfo(inv, sessionKey, "Context already destroyed");
            }
            // EJB2.0 section 7.6
            throw new NoSuchObjectLocalException("The EJB does not exist."
                    + " session-key: " + sessionKey);
        }

        MethodLockInfo lockInfo = inv.invocationInfo.methodLockInfo;
        boolean allowSerializedAccess = 
                (lockInfo == null) || (lockInfo.getTimeout() != CONCURRENCY_NOT_ALLOWED);

        if( allowSerializedAccess ) {

            boolean blockWithTimeout =
                    (lockInfo != null) && (lockInfo.getTimeout() != BLOCK_INDEFINITELY);

            if( blockWithTimeout ) {
                try {
                    boolean acquired = sc.getStatefulWriteLock().tryLock(lockInfo.getTimeout(),
                            lockInfo.getTimeUnit());
                    if( !acquired ) {
                        String msg = "Serialized access attempt on method " + inv.beanMethod +
                            " for ejb " + ejbDescriptor.getName() + " timed out after " +
                             + lockInfo.getTimeout() + " " + lockInfo.getTimeUnit();
                        throw new ConcurrentAccessTimeoutException(msg);
                    }

                } catch(InterruptedException ie)  {
                    String msg = "Serialized access attempt on method " + inv.beanMethod +
                            " for ejb " + ejbDescriptor.getName() + " was interrupted within " +
                             + lockInfo.getTimeout() + " " + lockInfo.getTimeUnit();
                    ConcurrentAccessException cae = new ConcurrentAccessTimeoutException(msg);
                    cae.initCause(ie);
                    throw cae;
                }
            } else {
                sc.getStatefulWriteLock().lock();
            }

            // Explicitly set state to track that we're holding the lock for this invocation.
            // No matter what we need to ensure that the lock is released.   In some
            // cases releaseContext() isn't called so for safety we'll have more than one
            // place that can potentially release the lock.  The invocation state will ensure
            // we don't accidently unlock too many times. 
            inv.setHoldingSFSBSerializedLock(true);
        }

        SessionContextImpl context = null;

        try {

            synchronized (sc) {

                SessionContextImpl newSC = sc;
                if (sc.getState() == BeanState.PASSIVATED) {
                    // This is possible if the EJB was passivated after
                    // the last lookupEJB. Try to activate it again.
                    newSC = (SessionContextImpl) sessionBeanCache.lookupEJB(
                            sessionKey, this, ejbo);
                    if (newSC == null) {
                        if (_logger.isLoggable(TRACE_LEVEL)) {
                            logTraceInfo(inv, sessionKey, "Context does not exist");
                        }
                        // EJB2.0 section 7.6
                        throw new NoSuchObjectLocalException(
                                "The EJB does not exist. key: " + sessionKey);
                    }
                    // Swap any stateful lock that was set on the original sc
                    newSC.setStatefulWriteLock(sc);
                }
                // acquire the lock again, in case a new sc was returned.
                synchronized (newSC) { //newSC could be same as sc
                    // Check & set the state of the EJB
                    if (newSC.getState() == BeanState.DESTROYED) {
                        if (_logger.isLoggable(TRACE_LEVEL)) {
                            logTraceInfo(inv, sessionKey, "Got destroyed context");
                        }
                        throw new NoSuchObjectLocalException
                                ("The EJB does not exist. session-key: " + sessionKey);
                    } else if (newSC.getState() == BeanState.INVOKING) {
                        handleConcurrentInvocation(allowSerializedAccess, inv, newSC, sessionKey);
                    }
                    if (newSC.getState() == BeanState.READY) {
                        decrementMethodReadyStat();
                    }
                    if (isHAEnabled) {
                        doVersionCheck(inv, sessionKey, sc);
                    }
                    newSC.setState(BeanState.INVOKING);
                    context = newSC;
                }
            }

            // touch the context here so timestamp is set & timeout is prevented
            context.touch();

            if ((context.existsInStore()) && (removalGracePeriodInSeconds > 0)) {
                long now = System.currentTimeMillis();
                long threshold = now - (removalGracePeriodInSeconds * 1000L);
                if (context.getLastPersistedAt() <= threshold) {
                    try {
                        backingStore.updateTimestamp(sessionKey, now);
                        context.setLastPersistedAt(System.currentTimeMillis());
                    } catch (BackingStoreException sfsbEx) {
                        _logger.log(Level.WARNING, COULDNT_UPDATE_TIMESTAMP_FOR_EXCEPTION,
                                new Object[]{sessionKey, sfsbEx});
                        _logger.log(Level.FINE,
                                "Couldn't update timestamp for: " + sessionKey, sfsbEx);
                    }
                }
            }

            if (_logger.isLoggable(TRACE_LEVEL)) {
                logTraceInfo(inv, context, "Got Context!!");
            }
        } catch(RuntimeException t) {

            // releaseContext isn't called if this method throws an exception,
            // so make sure to release any sfsb lock
            releaseSFSBSerializedLock(inv, sc);    

            throw t;
        }

        return context;
    }

    public boolean isHAEnabled() {
        return isHAEnabled;
    }

    private void doVersionCheck(EjbInvocation inv, Object sessionKey,
                                SessionContextImpl sc) {
        EJBLocalRemoteObject ejbLRO = inv.ejbObject;
        long clientVersion = SFSBVersionManager.NO_VERSION;
        if ((!inv.isLocal) && (sfsbVersionManager != null)) {
            clientVersion = sfsbVersionManager.getRequestClientVersion();
            sfsbVersionManager.clearRequestClientVersion();
            sfsbVersionManager.clearResponseClientVersion();
        }

        if (ejbLRO != null) {
            if (clientVersion ==
                    sfsbVersionManager.NO_VERSION) {
                clientVersion = ejbLRO.getSfsbClientVersion();
            }

            long ctxVersion = sc.getVersion();
            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, "doVersionCheck(): for: {" + ejbDescriptor.getName()
                        + "." + inv.method.getName() + " <=> " + sessionKey + "} clientVersion: "
                        + clientVersion + " == " + ctxVersion);
            }
            if (clientVersion > ctxVersion) {
                throw new NoSuchObjectLocalException(
                        "Found only a stale version " + " clientVersion: "
                                + clientVersion + " contextVersion: "
                                + ctxVersion);
            }
        }
    }

    private void handleConcurrentInvocation(boolean allowSerializedAccess,
                                            EjbInvocation inv, SessionContextImpl sc, Object sessionKey) {
        if (_logger.isLoggable(TRACE_LEVEL)) {
            logTraceInfo(inv, sessionKey, "Another invocation in progress");
        }

        if( allowSerializedAccess ) {

            // Check for loopback call to avoid deadlock.
            if( sc.getStatefulWriteLock().getHoldCount() > 1 ) {

                throw new IllegalLoopbackException("Illegal Reentrant Access : Attempt to make " +
                        "a loopback call on method '" + inv.beanMethod + " for stateful session bean " +
                        ejbDescriptor.getName());
            }

        } else {

            String errMsg = "Concurrent Access attempt on method " +
                    inv.beanMethod + " of SessionBean " + ejbDescriptor.getName() +
                    " is prohibited.  SFSB instance is executing another request. "
                + "[session-key: " + sessionKey + "]";
            ConcurrentAccessException conEx = new ConcurrentAccessException(errMsg);

            if (inv.isBusinessInterface) {
                throw conEx;
            } else {
                // there is an invocation in progress for this instance
                // throw an exception (EJB2.0 section 7.5.6).
                throw new EJBException(conEx);
            }
        }
    }

    protected void postInvokeTx(EjbInvocation inv) throws Exception {

        // Intercept postInvokeTx call to perform any @Remove logic
        // before tx commits.  super.postInvokeTx() must *always*
        // be called. 

        // If this was an invocation of a remove-method
        if (inv.invocationInfo.removalInfo != null) {

            InvocationInfo invInfo = inv.invocationInfo;
            EjbRemovalInfo removeInfo = invInfo.removalInfo;

            if (retainAfterRemoveMethod(inv, removeInfo)) {
                // Do nothing
            } else {

                // If there is a tx, remove bean from ContainerSynch so it 
                // won't receive any SessionSynchronization callbacks.
                // We delay the PreDestroy callback and instance destruction
                // until releaseContext so that PreDestroy won't run within
                // the business method's tx.  

                SessionContextImpl sc = (SessionContextImpl) inv.context;
                Transaction tx = sc.getTransaction();

                if (tx != null) {
                    ContainerSynchronization sync =
                            ejbContainerUtilImpl.getContainerSync(tx);
                    sync.removeBean(sc);
                }

            }
        }

        super.postInvokeTx(inv);

    }

    /**
     * Should only be called when a method is known to be a remove method.
     *
     * @return true if the removal should be skipped, false otherwise.
     */
    private boolean retainAfterRemoveMethod(EjbInvocation inv,
                                            EjbRemovalInfo rInfo) {

        boolean retain =
                (rInfo.getRetainIfException() &&
                        (inv.exceptionFromBeanMethod != null) &&
                        (isApplicationException(inv.exceptionFromBeanMethod)));

        return retain;

    }

    /**
     * Called from preInvoke which is called from the EJBObject for local and
     * remote invocations.
     */
    public void releaseContext(EjbInvocation inv) {
        SessionContextImpl sc = (SessionContextImpl) inv.context;

        // Make sure everything is within try block so we can be assured that
        // any instance lock is released in the finally block.
        try {
            // check if the bean was destroyed
            if (sc.getState() == BeanState.DESTROYED)
                return;

            // we're sure that no concurrent thread can be using this
            // context, so no need to synchronize.
            Transaction tx = sc.getTransaction();


            // If this was an invocation of a remove-method
            if (inv.invocationInfo.removalInfo != null) {

                InvocationInfo invInfo = inv.invocationInfo;
                EjbRemovalInfo removeInfo = invInfo.removalInfo;

                if (retainAfterRemoveMethod(inv, removeInfo)) {
                    _logger.log(Level.FINE, "Skipping destruction of SFSB "
                            + invInfo.ejbName + " after @Remove method "
                            + invInfo.method + " due to (retainIfException"
                            + " == true) and exception " + inv.exception);
                } else {
                    try {
                        destroyBean(inv, sc);
                    } catch (Throwable t) {
                        _logger.log(Level.FINE, "@Remove.preDestroy exception",
                                t);
                    }

                    // Explicitly null out transaction association in bean's context.
                    // Otherwise, forceDestroyBean() will mark that tx for rollback,
                    // which could incorrectly rollback a client-propagated transaction.
                    sc.setTransaction(null);

                    forceDestroyBean(sc);

                    // The bean has been detroyed so just skip any remaining processing.
                    return;
                }
            }

            if (tx == null || tx.getStatus() == Status.STATUS_NO_TRANSACTION) {
                // The Bean executed with no tx, or with a tx and
                // container.afterCompletion() was already called.
                if (sc.getState() != BeanState.READY) {
                    if (sc.isAfterCompletionDelayed()) {
                        // ejb.afterCompletion was not called yet
                        // because of container.afterCompletion may have
                        // been called concurrently with this invocation.
                        if (_logger.isLoggable(TRACE_LEVEL)) {
                            logTraceInfo(inv, sc,
                                    "Calling delayed afterCompletion");
                        }
                        callEjbAfterCompletion(sc, sc.getCompletedTxStatus());
                    }

                    if (sc.getState() != BeanState.DESTROYED) {
                        // callEjbAfterCompletion could make state as DESTROYED
                        sc.setState(BeanState.READY);
                        handleEndOfMethodCheckpoint(sc, inv);
                    }
                }
                if ((sc.getState() != BeanState.DESTROYED)
                        && isHAEnabled) {
                    syncClientVersion(inv, sc);
                }
            } else {
                if ((sc.getState() != BeanState.DESTROYED)
                        && isHAEnabled) {
                    syncClientVersion(inv, sc);
                }
                sc.setState(BeanState.INCOMPLETE_TX);
                if (_logger.isLoggable(TRACE_LEVEL)) {
                    logTraceInfo(inv, sc, "Marking state == INCOMPLETE_TX");
                }
            }

        } catch (SystemException ex) {
            throw new EJBException(ex);
        } finally {

            releaseSFSBSerializedLock(inv, sc);
        }
    }

    private void releaseSFSBSerializedLock(EjbInvocation inv, SessionContextImpl sc) {


        if( inv.holdingSFSBSerializedLock() ) {
            inv.setHoldingSFSBSerializedLock(false);
            sc.getStatefulWriteLock().unlock();
        }

    }

    protected void afterBegin(EJBContextImpl context) {
        // TX_BEAN_MANAGED EJBs cannot implement SessionSynchronization
        // Do not call afterBegin if it is a transactional lifecycle callback
        if (isBeanManagedTran || ((SessionContextImpl) context).getInLifeCycleCallback()) {
            return;
        }

        // Note: this is only called for business methods.
        // For SessionBeans non-business methods are never called with a Tx.
        Object ejb = context.getEJB();
        if (afterBeginMethod != null ) {
            try {
		        afterBeginMethod.invoke(ejb, null);
            } catch (Exception ex) {
           
                // Error during afterBegin, so discard bean: EJB2.0 18.3.3
                forceDestroyBean(context);
                throw new EJBException("Error during SessionSynchronization." +
                        ".afterBegin(), EJB instance discarded", ex);
                       
            }
        }

        //Register CMT Beans for end of Tx Checkpointing
        //Note:- We will never reach here for TX_BEAN_MANAGED
        if (isHAEnabled) {
            ContainerSynchronization cSync = null;
            try {
                cSync = ejbContainerUtilImpl.
                        getContainerSync(context.getTransaction());
                cSync.registerForTxCheckpoint(
                        (SessionContextImpl) context);
            } catch (javax.transaction.RollbackException rollEx) {
                _logger.log(Level.WARNING, CANNOT_REGISTER_BEAN_FOR_CHECKPOINTING, rollEx);
            } catch (javax.transaction.SystemException sysEx) {
                _logger.log(Level.WARNING, CANNOT_REGISTER_BEAN_FOR_CHECKPOINTING, sysEx);
            }
        }
    }


    protected void beforeCompletion(EJBContextImpl context) {
        // SessionSync calls on TX_BEAN_MANAGED SessionBeans
        // are not allowed
        // Do not call beforeCompletion if it is a transactional lifecycle callback
        if( isBeanManagedTran || beforeCompletionMethod == null || 
                ((SessionContextImpl) context).getInLifeCycleCallback() ) {
            return;
	}

        Object ejb = context.getEJB();

        // No need to check for a concurrent invocation
        // because beforeCompletion can only be called after
        // all business methods are completed.

        EjbInvocation inv = super.createEjbInvocation(ejb, context);
        invocationManager.preInvoke(inv);
        try {
            transactionManager.enlistComponentResources();
	   
	    beforeCompletionMethod.invoke(ejb, null);

        } catch (Exception ex) {

            // Error during beforeCompletion, so discard bean: EJB2.0 18.3.3
            try {
                forceDestroyBean(context);
            } catch (Exception e) {
                _logger.log(Level.FINE, "error destroying bean", e);
            }
            throw new EJBException("Error during SessionSynchronization." +
                    "beforeCompletion, EJB instance discarded", ex);

        } finally {
            invocationManager.postInvoke(inv);
        }
    }


    // Called from SyncImpl.afterCompletion
    // May be called asynchronously during tx timeout
    // or on the same thread as tx.commit
    protected void afterCompletion(EJBContextImpl context, int status) {
        if (context.getState() == BeanState.DESTROYED) {
            return;
        }

        SessionContextImpl sc = (SessionContextImpl) context;
        boolean committed = (status == Status.STATUS_COMMITTED)
                || (status == Status.STATUS_NO_TRANSACTION);

        sc.setTransaction(null);

        // Do not call afterCompletion if it is a transactional lifecycle callback
        if (sc.getInLifeCycleCallback()) {
            return;
        }

        // SessionSync calls on TX_BEAN_MANAGED SessionBeans
        // are not allowed.
        if (!isBeanManagedTran && (afterCompletionMethod != null)) {

            // Check for a concurrent invocation
            // because afterCompletion can be called asynchronously
            // during rollback because of transaction timeout
            if ((sc.getState() == BeanState.INVOKING) && (!sc.isTxCompleting())) {
                // Cant invoke ejb.afterCompletion now because there is
                // already some invocation in progress on the ejb.
                sc.setAfterCompletionDelayed(true);
                sc.setCompletedTxStatus(committed);
                if (_logger.isLoggable(TRACE_LEVEL)) {
                    logTraceInfo(sc, "AfterCompletion delayed");
                }
                return;
            }

            callEjbAfterCompletion(sc, committed);
        }

        //callEjbAfterCompletion can set state as  DESTROYED
        if (sc.getState() != BeanState.DESTROYED) {
            if (isHAEnabled) {
                if (isBeanManagedTran) {
                    sc.setTxCheckpointDelayed(true);
                    if (_logger.isLoggable(TRACE_LEVEL)) {
                        logTraceInfo(sc, "(BMT)Checkpoint delayed");
                    }
                }
            } else {
                if (!isBeanManagedTran) {
                    if (_logger.isLoggable(TRACE_LEVEL)) {
                        logTraceInfo(sc, "Released context");
                    }
                    sc.setState(BeanState.READY);
                    incrementMethodReadyStat();
                }
            }
        }
    }

    SimpleMetadata getSFSBBeanState(SessionContextImpl sc) {
        //No need to synchronize
        SimpleMetadata simpleMetadata = null;
        try {

            if ((containerState != CONTAINER_STARTED) && (containerState != CONTAINER_STOPPED)) {
                _logger.log(Level.FINE, "getSFSBBeanState() returning because "
                        + "containerState: " + containerState);
                return null;
            }

            if (sc.getState() == BeanState.DESTROYED) {
                return null;
            }

            Object ejb = sc.getEJB();

            EjbInvocation ejbInv = createEjbInvocation(ejb, sc);
            invocationManager.preInvoke(ejbInv);
            boolean needToDoPostInvokeTx = false;
            boolean destroyBean = false;

            synchronized (sc) {
                try {
                    needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                            prePassivateInvInfo, CallbackType.PRE_PASSIVATE);
                    sc.setLastPersistedAt(System.currentTimeMillis());
                    long newCtxVersion = sc.incrementAndGetVersion();
                    byte[] serializedState = serializeContext(sc);
                    simpleMetadata = new SimpleMetadata(sc.getVersion(),
                            System.currentTimeMillis(),
                            removalGracePeriodInSeconds*1000L, serializedState);
                    simpleMetadata.setVersion(newCtxVersion);
                    needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                            postActivateInvInfo, CallbackType.POST_ACTIVATE);
                    //Do not set sc.setExistsInStore() here
                } catch (java.io.NotSerializableException serEx) {
                    _logger.log(Level.WARNING, ERROR_DURING_CHECKPOINT_3PARAMs,
                            new Object[]{ejbDescriptor.getName(), sc.getInstanceKey(), serEx});
                    _logger.log(Level.FINE, "sfsb checkpoint error. Key: "
                            + sc.getInstanceKey(), serEx);
                    destroyBean = true;
                } catch (Throwable ex) {
                    _logger.log(Level.WARNING, SFSB_CHECKPOINT_ERROR_NAME, new Object[]{ejbDescriptor.getName()});
                    _logger.log(Level.WARNING, SFSB_CHECKPOINT_ERROR_KEY, new Object[]{sc.getInstanceKey(), ex});
                    destroyBean = true;
                } finally {
                    invocationManager.postInvoke(ejbInv);
                    completeLifecycleCallbackTxIfUsed(ejbInv, sc, needToDoPostInvokeTx);
                    if (destroyBean) {
                        try {
                            forceDestroyBean(sc);
                        } catch (Exception e) {
                            _logger.log(Level.FINE, "error destroying bean", e);
                        }
                    }
                }
            } //synchronized

        } catch (Throwable th) {
            _logger.log(Level.WARNING, SFSB_CHECKPOINT_ERROR_NAME, new Object[]{ejbDescriptor.getName(), th});
        }

        return simpleMetadata;
    }

    void txCheckpointCompleted(SessionContextImpl sc) {
        if (sc.getState() != BeanState.DESTROYED) {
            //We did persist this ctx in the store
            sc.setExistsInStore(true);
            sc.setState(BeanState.READY);
            incrementMethodReadyStat();
        }
    }

    private void callEjbAfterCompletion(SessionContextImpl context,
                                        boolean status) {
	if( afterCompletionMethod != null ) {
	    Object ejb = context.getEJB();
	    EjbInvocation ejbInv = createEjbInvocation(ejb, context);
	    invocationManager.preInvoke(ejbInv);
	    try {
		context.setInAfterCompletion(true);
		afterCompletionMethod.invoke(ejb, status);

		// reset flags
		context.setAfterCompletionDelayed(false);
		context.setTxCompleting(false);
	    }
	    catch (Exception ex) {
		    Throwable realException = ex;
		    if( ex instanceof InvocationTargetException ) {
		        realException = ((InvocationTargetException)ex).getTargetException();
		    }
		// Error during afterCompletion, so discard bean: EJB2.0 18.3.3
                try {
                    forceDestroyBean(context);
                } catch (Exception e) {
                    _logger.log(Level.FINE, "error destroying bean", e);
                }
		
		_logger.log(Level.INFO, AFTER_COMPLETION_EXCEPTION, realException);
		
		// No use throwing an exception here, since the tx has already
		// completed, and afterCompletion may be called asynchronously
		// when there is no client to receive the exception.
	    }
	    finally {
		context.setInAfterCompletion(false);
		invocationManager.postInvoke(ejbInv);
	    }
	}
    }

    public final boolean canPassivateEJB(ComponentContext context) {
        SessionContextImpl sc = (SessionContextImpl) context;
        return (sc.getState() == BeanState.READY);
    }

    // called asynchronously from the Recycler
    public final boolean passivateEJB(ComponentContext context) {

        SessionContextImpl sc = (SessionContextImpl) context;

        boolean success = false;

        try {

            if (ejbDescriptor.getApplication().getKeepStateResolved() == false) {
                if ((containerState != CONTAINER_STARTED) && (containerState != CONTAINER_STOPPED)) {
                    _logger.log(Level.WARNING, PASSIVATE_EJB_RETURNING_BECAUSE_CONTAINER_STATE, containerState);
                    return false;
                }
            }

            if (sc.getState() == BeanState.DESTROYED)
                return false;

            if (_logger.isLoggable(TRACE_LEVEL)) {
                _logger.log(TRACE_LEVEL, traceInfoPrefix + "Passivating context "
                        + sc.getInstanceKey() + "; current-state = "
                        + convertCtxStateToString(sc));
            }

            Object ejb = sc.getEJB();


            long passStartTime = -1;
            /* TODO 
            if (sfsbStoreMonitor.isMonitoringOn()) {
                passStartTime = System.currentTimeMillis();
            }
            */

            EjbInvocation ejbInv = createEjbInvocation(ejb, sc);
            invocationManager.preInvoke(ejbInv);

            boolean failed = false;

            success = false;
            boolean needToDoPostInvokeTx = false;
            boolean destroyBean = false;
            synchronized (sc) {
                try {
                    // dont passivate if there is a Tx/invocation in progress
                    // for this instance.
                    if (!sc.canBePassivated()) {
                        return false;
                    }
                    
                    Serializable instanceKey = (Serializable) sc.getInstanceKey();
                    if (sessionBeanCache.eligibleForRemovalFromCache(sc, instanceKey)) {
                        // remove the EJB since removal-timeout has elapsed
                        sc.setState(BeanState.DESTROYED);
                        needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                                preDestroyInvInfo, CallbackType.PRE_DESTROY);
                        sessionBeanCache.remove(instanceKey, sc.existsInStore());
                    } else {
                        // passivate the EJB
                        sc.setState(BeanState.PASSIVATED);
                        decrementMethodReadyStat();
                        needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                                prePassivateInvInfo, CallbackType.PRE_PASSIVATE);
                        sc.setLastPersistedAt(System.currentTimeMillis());
                        boolean saved = false;
                        try {
                            saved = sessionBeanCache.passivateEJB(sc, instanceKey);
                        } catch (EMNotSerializableException emNotSerEx) {
                            _logger.log(Level.WARNING, EXTENDED_EM_NOT_SERIALIZABLE, emNotSerEx);
                            _logger.log(Level.FINE, "Extended EM not serializable", emNotSerEx);
                            saved = false;
                        }
                        if (!saved) {
                            // TODO - add a flag to reactivate in the same tx
                            // Complete previous tx
                            completeLifecycleCallbackTxIfUsed(ejbInv, sc, needToDoPostInvokeTx);

                            needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                                    postActivateInvInfo, CallbackType.POST_ACTIVATE);
                            sc.setState(BeanState.READY);
                            incrementMethodReadyStat();
                            return false;
                        }
                    }
                    
                    // V2: sfsbStoreMonitor.incrementPassivationCount(true);
                    cacheProbeNotifier.ejbBeanPassivatedEvent(getContainerId(),
                            containerInfo.appName, containerInfo.modName,
                            containerInfo.ejbName, true);
                    transactionManager.componentDestroyed(sc);

                    decrementRefCountsForEEMs(sc);

                    if (isRemote) {

                        if (hasRemoteHomeView) {
                            // disconnect the EJBObject from the EJB
                            EJBObjectImpl ejbObjImpl = sc.getEJBObjectImpl();
                            ejbObjImpl.clearContext();
                            sc.setEJBObjectImpl(null);

                            // disconnect the EJBObject from ProtocolManager
                            // so that no state is held by ProtocolManager
                            remoteHomeRefFactory.destroyReference
                                    (ejbObjImpl.getStub(),
                                            ejbObjImpl.getEJBObject());
                        }
                        if (hasRemoteBusinessView) {
                            // disconnect the EJBObject from the EJB
                            EJBObjectImpl ejbBusinessObjImpl =
                                    sc.getEJBRemoteBusinessObjectImpl();
                            ejbBusinessObjImpl.clearContext();
                            sc.setEJBRemoteBusinessObjectImpl(null);

                            for (RemoteBusinessIntfInfo next :
                                    remoteBusinessIntfInfo.values()) {
                                next.referenceFactory.destroyReference
                                        (ejbBusinessObjImpl.getStub(),
                                                ejbBusinessObjImpl.getEJBObject
                                                        (next.generatedRemoteIntf.getName()));
                            }
                        }

                    }
                    if (isLocal) {
                        long version = sc.getVersion();

                        if (hasLocalHomeView) {
                            // disconnect the EJBLocalObject from the EJB
                            EJBLocalObjectImpl localObjImpl =
                                    sc.getEJBLocalObjectImpl();
                            localObjImpl.setSfsbClientVersion(version);
                            localObjImpl.clearContext();
                            sc.setEJBLocalObjectImpl(null);
                        }
                        if (hasLocalBusinessView) {
                            EJBLocalObjectImpl localBusinessObjImpl =
                                    sc.getEJBLocalBusinessObjectImpl();
                            localBusinessObjImpl.setSfsbClientVersion(version);
                            localBusinessObjImpl.clearContext();
                            sc.setEJBLocalBusinessObjectImpl(null);
                        }
                        if (hasOptionalLocalBusinessView ) {
                            EJBLocalObjectImpl optLocalBusObjImpl =
                                        sc.getOptionalEJBLocalBusinessObjectImpl();
                            optLocalBusObjImpl.setSfsbClientVersion(version);
                            optLocalBusObjImpl.clearContext();
                            sc.setOptionalEJBLocalBusinessObjectImpl(null);
                        }
                    }
                    if (_logger.isLoggable(TRACE_LEVEL)) {
                        logTraceInfo(sc, "Successfully passivated");
                    }
                } catch (java.io.NotSerializableException nsEx) {
                    // V2: sfsbStoreMonitor.incrementPassivationCount(false);
                    cacheProbeNotifier.ejbBeanPassivatedEvent(getContainerId(),
                            containerInfo.appName, containerInfo.modName,
                            containerInfo.ejbName, false);
                    _logger.log(Level.WARNING, ERROR_DURING_PASSIVATION, new Object[]{sc, nsEx});
                    _logger.log(Level.FINE, "sfsb passivation error", nsEx);
                    // Error during passivate, so discard bean: EJB2.0 18.3.3
                    destroyBean = true;
                } catch (Throwable ex) {
                    // V2: sfsbStoreMonitor.incrementPassivationCount(false);
                    cacheProbeNotifier.ejbBeanPassivatedEvent(getContainerId(),
                            containerInfo.appName, containerInfo.modName,
                            containerInfo.ejbName, false);
                    _logger.log(Level.WARNING, PASSIVATION_ERROR_1PARAM,
                            new Object[]{ejbDescriptor.getName() + " <==> " + sc});
                    _logger.log(Level.WARNING, SFSB_PASSIVATION_ERROR_1PARAM, new Object[]{sc.getInstanceKey(), ex});
                    // Error during passivate, so discard bean: EJB2.0 18.3.3
                    destroyBean = true;
                } finally {
                    invocationManager.postInvoke(ejbInv);
                    completeLifecycleCallbackTxIfUsed(ejbInv, sc, needToDoPostInvokeTx);
                    if (destroyBean) {
                        try {
                            forceDestroyBean(sc);
                        } catch (Exception e) {
                            _logger.log(Level.FINE, "error destroying bean", e);
                        }
                    }
                    if (passStartTime != -1) {
                        long timeSpent = System.currentTimeMillis()
                                - passStartTime;
                        // V2: sfsbStoreMonitor.setPassivationTime(timeSpent);
                    }
                }
            } //synchronized

        } catch (Exception ex) {
            _logger.log(Level.WARNING, PASSIVATION_ERROR_1PARAM, new Object[]{ejbDescriptor.getName(), ex});
        }
        return success;

    }

    public final int getPassivationBatchCount() {
        return this.passivationBatchCount;
    }

    public final void setPassivationBatchCount(int count) {
        this.passivationBatchCount = count;
    }

    // called asynchronously from the Recycler
    public final boolean passivateEJB(StatefulEJBContext sfsbCtx) {
        return passivateEJB((ComponentContext) sfsbCtx.getSessionContext());
    }

    public long getMethodReadyCount() {
        return statMethodReadyCount;
    }

    public long getPassiveCount() {
        return (sfsbStoreMonitor == null)
                ? 0 : sfsbStoreMonitor.getNumPassivations();
    }

    // called from StatefulSessionStore
    public void activateEJB(Object sessionKey, StatefulEJBContext sfsbCtx,
                            Object cookie) {
        SessionContextImpl context = (SessionContextImpl)
                sfsbCtx.getSessionContext();

        if (_logger.isLoggable(TRACE_LEVEL)) {
            logTraceInfo(context, "Attempting to activate");
        }

        EJBLocalRemoteObject ejbObject = (EJBLocalRemoteObject) cookie;
        Object ejb = context.getEJB();

        EjbInvocation ejbInv = createEjbInvocation(ejb, context);
        invocationManager.preInvoke(ejbInv);
            boolean needToDoPostInvokeTx = false;
        try {
            // we're sure that no concurrent thread can be using this bean
            // so no need to synchronize.

            // No need to call enlistComponentResources here because
            // ejbActivate executes in unspecified tx context (spec 6.6.1)

            // Set the timestamp here, else Recycler might remove this bean!
            context.touch();

            context.setContainer(this);
            context.setState(BeanState.READY);
            incrementMethodReadyStat();
            context.setInstanceKey(sessionKey);
            context.setExistsInStore(true);

            context.initializeStatefulWriteLock();


            if (ejbObject == null) {

                // This MUST be a remote invocation
                if (hasRemoteHomeView) {
                    createEJBObjectImpl(context);
                } else {
                    createRemoteBusinessObjectImpl(context);
                }

            } else if (ejbObject instanceof EJBObjectImpl) {

                EJBObjectImpl eo = (EJBObjectImpl) ejbObject;
                ejbObject.setContext(context);
                ejbObject.setKey(sessionKey);

                byte[] sessionOID = uuidGenerator.keyToByteArray(sessionKey);

                if (eo.isRemoteHomeView()) {

                    // introduce context and EJBObject to each other
                    context.setEJBObjectImpl(eo);

                    EJBObject ejbStub = (EJBObject)
                            remoteHomeRefFactory.createRemoteReference
                                    (sessionOID);
                    eo.setStub(ejbStub);
                    context.setEJBStub(ejbStub);

                    if (hasRemoteBusinessView) {
                        createRemoteBusinessObjectImpl(context);
                    }

                } else {

                    context.setEJBRemoteBusinessObjectImpl(eo);

                    for (RemoteBusinessIntfInfo next :
                            remoteBusinessIntfInfo.values()) {
                        java.rmi.Remote stub = next.referenceFactory
                                .createRemoteReference(sessionOID);

                        eo.setStub(next.generatedRemoteIntf.getName(), stub);
                    }

                    if (hasRemoteHomeView) {
                        createEJBObjectImpl(context);
                    }

                }

                if (isLocal) { // create localObj too
                    if (hasLocalHomeView) {
                        createEJBLocalObjectImpl(context);
                    }
                    if (hasLocalBusinessView) {
                        createEJBLocalBusinessObjectImpl(context);
                    }
                    if (hasOptionalLocalBusinessView) {
                        createOptionalEJBLocalBusinessObjectImpl(context);
                    }
                }
            } else if (ejbObject instanceof EJBLocalObjectImpl) {

                EJBLocalObjectImpl elo = (EJBLocalObjectImpl) ejbObject;
                ejbObject.setContext(context);
                ejbObject.setKey(sessionKey);

                if (elo.isLocalHomeView()) {
                    context.setEJBLocalObjectImpl(elo);
                    if (hasLocalBusinessView) {
                        createEJBLocalBusinessObjectImpl(context);
                    }
                    if (hasOptionalLocalBusinessView) {
                        createOptionalEJBLocalBusinessObjectImpl(context);
                    }
                } else if( elo.isOptionalLocalBusinessView() ) {
                    context.setOptionalEJBLocalBusinessObjectImpl(elo);
                    if (hasLocalBusinessView) {
                        createEJBLocalBusinessObjectImpl(context);
                    }
                    if (hasLocalHomeView) {
                        createEJBLocalObjectImpl(context);
                    }
                } else {
                    context.setEJBLocalBusinessObjectImpl(elo);
                    if (hasLocalHomeView) {
                        createEJBLocalObjectImpl(context);
                    }
                    if (hasOptionalLocalBusinessView) {
                        createOptionalEJBLocalBusinessObjectImpl(context);
                    }
                }

                if (hasRemoteHomeView) { // create remote obj too
                    createEJBObjectImpl(context);
                }
                if (hasRemoteBusinessView) {
                    createRemoteBusinessObjectImpl(context);
                }
            }

            //Now populate the EEM maps in this context
            repopulateEEMMapsInContext(sessionKey, context);

            try {
                needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, context, 
                        postActivateInvInfo, CallbackType.POST_ACTIVATE);
            } catch (Throwable th) {
                EJBException ejbEx = new EJBException("Error during activation"
                        + sessionKey);
                ejbEx.initCause(th);
                throw ejbEx;
            }
            long now = System.currentTimeMillis();
            try {
                backingStore.updateTimestamp((Serializable) sessionKey, now);
                context.setLastPersistedAt(now);
            } catch (BackingStoreException sfsbEx) {
                _logger.log(Level.WARNING, COULDNT_UPDATE_TIMESTAMP_FOR_EXCEPTION, new Object[]{sessionKey, sfsbEx});
                _logger.log(Level.FINE,
                        "Couldn't update timestamp for: " + sessionKey, sfsbEx);
            }


            if (_logger.isLoggable(TRACE_LEVEL)) {
                logTraceInfo(context, "Successfully activated");
            }
            _logger.log(Level.FINE, "Activated: " + sessionKey);
        }
        catch (Exception ex) {
            if (_logger.isLoggable(TRACE_LEVEL)) {
                logTraceInfo(context, "Failed to activate");
            }
            _logger.log(Level.SEVERE, SFSB_ACTIVATION_ERROR, new Object[]{sessionKey, ex});
            _logger.log(Level.SEVERE, "", ex);

            throw new EJBException("Unable to activate EJB for key: "
                    + sessionKey, ex);
        }
        finally {
            invocationManager.postInvoke(ejbInv);
            completeLifecycleCallbackTxIfUsed(ejbInv, context, needToDoPostInvokeTx);
        }
    }

    public byte[] serializeContext(StatefulEJBContext ctx) throws IOException {
        return serializeContext((SessionContextImpl)ctx.getSessionContext());
    }

    public Object deserializeData(byte[] data) throws Exception {
        Object o = ejbContainerUtilImpl.getJavaEEIOUtils().deserializeObject(data, true, getClassLoader());
        if (o instanceof SessionContextImpl) {
            SessionContextImpl ctx = (SessionContextImpl)o;
            Object ejb = ctx.getEJB();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "StatefulSessionContainer.deserializeData: " + ((ejb == null)? null : ejb.getClass()));
            }

            if (ejb instanceof SerializableEJB) {
                SerializableEJB sejb = (SerializableEJB)ejb;
                try (ByteArrayInputStream bis = new ByteArrayInputStream(sejb.serializedFields);
                    ObjectInputStream ois = ejbContainerUtilImpl.getJavaEEIOUtils().createObjectInputStream(bis, true, getClassLoader());) {

                    ejb = ejbClass.newInstance();
                    EJBUtils.deserializeObjectFields(ejb, ois, o, false);
                    ctx.setEJB(ejb);
                }

            }
        }

        return o;
    }

    /*********************************************************************/
    /***********  END SFSBContainerCallback methods    *******************/
    /**
     * *****************************************************************
     */

    private byte[] serializeContext(SessionContextImpl ctx) throws IOException {
        Object ejb = ctx.getEJB();
        if (!(ejb instanceof Serializable || 
                ejb.getClass().getName().equals(EJBUtils.getGeneratedSerializableClassName(ejbName)))) {

            ctx.setEJB(null);
            ctx.setEJB(new SerializableEJB(ejb));
        }
        return ejbContainerUtilImpl.getJavaEEIOUtils().serializeObject(ctx, true);
    }

    private void decrementRefCountsForEEMs(SessionContextImpl context) {
        Collection<EEMRefInfo> allRefInfos = context.getAllEEMRefInfos();
        for (EEMRefInfo refInfo : allRefInfos) {
            EEMRefInfoKey key = refInfo.getKey();
            synchronized (extendedEMReferenceCountMap) {
                EEMRefInfo cachedRefInfo = extendedEMReferenceCountMap.get(
                        refInfo.eem);
                if (cachedRefInfo != null) {
                    cachedRefInfo.refCount--;
                    if (cachedRefInfo.refCount == 0) {
                        extendedEMReferenceCountMap.remove(refInfo.eem);
                        eemKey2EEMMap.remove(key);
                    }
                }
            }
        }
    }

    private void repopulateEEMMapsInContext(Object sessionKey,
                                            SessionContextImpl context) {
        Collection<EEMRefInfo> allRefInfos = context.getAllEEMRefInfos();
        for (EEMRefInfo refInfo : allRefInfos) {
            EEMRefInfoKey key = refInfo.getKey();
            synchronized (extendedEMReferenceCountMap) {
                EntityManager eMgr = eemKey2EEMMap.get(key);
                EEMRefInfo newRefInfo = null;
                if (eMgr != null) {
                    EEMRefInfo cachedRefInfo = extendedEMReferenceCountMap.get(eMgr);
                    //cachedRefInfo cannot be null
                    context.addExtendedEntityManagerMapping(
                            cachedRefInfo.getEntityManagerFactory(),
                            cachedRefInfo);
                    cachedRefInfo.refCount++;
                    newRefInfo = cachedRefInfo;
                } else {
                    //Deserialize em from the byte[]
                    String emRefName = key.emRefName;
                    String unitName = refInfo.getUnitName();
                    EntityManagerFactory emf = EntityManagerFactoryWrapper
                            .lookupEntityManagerFactory(ComponentInvocation.ComponentInvocationType.EJB_INVOCATION,
                                    unitName, ejbDescriptor);
                    if (emf != null) {
                        try (ByteArrayInputStream bis = new ByteArrayInputStream(refInfo.serializedEEM);
                            ObjectInputStream ois = new ObjectInputStream(bis);) {
                            eMgr = (EntityManager) ois.readObject();
                            newRefInfo = new EEMRefInfo(emRefName, unitName, refInfo.getSynchronizationType(), super.getContainerId(),
                                    sessionKey, eMgr, emf);
                            newRefInfo.refCount = 1;
                            extendedEMReferenceCountMap.put(eMgr, newRefInfo);
                            eemKey2EEMMap.put(newRefInfo.getKey(),
                                    newRefInfo.getEntityManager());
                        } catch (Throwable th) {
                            EJBException ejbEx = new EJBException(
                                    "Couldn't create EntityManager for"
                                            + " refName: " + emRefName);
                            ejbEx.initCause(th);
                            throw ejbEx;
                        }
                    } else {
                        throw new EJBException(
                                "EMF is null. Couldn't get extended EntityManager for"
                                        + " refName: " + emRefName);
                    }
                }
                context.addExtendedEntityManagerMapping(
                        newRefInfo.getEntityManagerFactory(), newRefInfo);
            }
        }
    }

    @Override
    protected void validateEMForClientTx(EjbInvocation inv, JavaEETransaction clientJ2EETx) 
            throws EJBException {
        SessionContextImpl sessionCtx = (SessionContextImpl) inv.context;
        Map<EntityManagerFactory, PhysicalEntityManagerWrapper> entityManagerMap =
        sessionCtx.getExtendedEntityManagerMap();

        for (Map.Entry<EntityManagerFactory, PhysicalEntityManagerWrapper> entry :
                entityManagerMap.entrySet()) {
            EntityManagerFactory emf = entry.getKey();

            // Make sure there is no Transactional persistence context
            // for the same EntityManagerFactory as this SFSB's
            // Extended persistence context for the propagated transaction.
            if( clientJ2EETx.getTxEntityManagerResource(emf) != null ) {
                throw new EJBException("There is an active transactional persistence context for the same EntityManagerFactory as the current stateful session bean's extended persistence context");
            }

            // Now see if there's already a *different* extended
            // persistence context within this transaction for the
            // same EntityManagerFactory.
            PhysicalEntityManagerWrapper physicalEM = (PhysicalEntityManagerWrapper) clientJ2EETx.getExtendedEntityManagerResource(emf);
            if( (physicalEM != null) && entry.getValue().getEM() != physicalEM.getEM() ) {
                throw new EJBException("Detected two different extended persistence contexts for the same EntityManagerFactory within a transaction");
            }

        }

    }

    @Override
    protected void enlistExtendedEntityManagers(ComponentContext ctx) {
        if (ctx.getTransaction() != null) {
            JavaEETransaction j2eeTx = (JavaEETransaction) ctx.getTransaction();
            SessionContextImpl sessionCtx = (SessionContextImpl) ctx;
            Map<EntityManagerFactory, PhysicalEntityManagerWrapper> entityManagerMap =
                sessionCtx.getExtendedEntityManagerMap();

            for (Map.Entry<EntityManagerFactory, PhysicalEntityManagerWrapper> entry :
                     entityManagerMap.entrySet()) {
                EntityManagerFactory emf = entry.getKey();
                PhysicalEntityManagerWrapper extendedEm = entry.getValue();

                PhysicalEntityManagerWrapper extendedEmAssociatedWithTx = EntityManagerWrapper.getExtendedEntityManager(j2eeTx, emf);

                // If there's not already an EntityManager registered for
                // this extended EntityManagerFactory within the current tx
                if (extendedEmAssociatedWithTx == null) {
                    j2eeTx.addExtendedEntityManagerMapping(emf, extendedEm);
                    sessionCtx.setEmfRegisteredWithTx(emf, true);

                    // Tell persistence provider to associate the extended
                    // entity manager with the transaction.
                    if(extendedEm.getSynchronizationType() == SYNCHRONIZED) {
                        extendedEm.getEM().joinTransaction();
                    }
                }
            }
        }
    }

    @Override
    protected void delistExtendedEntityManagers(ComponentContext ctx) {
        if ( ctx.getTransaction() != null ) {
            SessionContextImpl sessionCtx = (SessionContextImpl) ctx;
            JavaEETransaction j2eeTx = (JavaEETransaction) sessionCtx.getTransaction();

            Map<EntityManagerFactory, PhysicalEntityManagerWrapper> entityManagerMap = sessionCtx
                    .getExtendedEntityManagerMap();
            for (Map.Entry<EntityManagerFactory, PhysicalEntityManagerWrapper> entry :
                    entityManagerMap.entrySet()) {
                EntityManagerFactory emf = entry.getKey();

                if (sessionCtx.isEmfRegisteredWithTx(emf)) {
                    j2eeTx.removeExtendedEntityManagerMapping(emf);
                    sessionCtx.setEmfRegisteredWithTx(emf, false);
                }
            }
        }
    }

    public void invokePeriodically(long delay, long periodicity, Runnable target) {
        java.util.Timer timer = ejbContainerUtilImpl.getTimer();

        TimerTask timerTask = new PeriodicTask(super.loader, target, ejbContainerUtilImpl);
        timer.scheduleAtFixedRate(timerTask, delay, periodicity);
        scheduledTimerTasks.add(timerTask);
    }

    //Called from Cache implementation through ContainerCallback
    //  when cache.undeploy() is invoked
    public void onUndeploy(StatefulEJBContext sfsbCtx) {
        undeploy((SessionContextImpl) sfsbCtx.getSessionContext());
    }

    protected String[] getPre30LifecycleMethodNames() {
        return new String[]{
                null, null, "ejbRemove", "ejbPassivate", "ejbActivate"
        };
    }

    ;

    protected void doConcreteContainerShutdown(boolean appBeingUndeployed) {

        cancelAllTimerTasks();
        if( appBeingUndeployed && (ejbDescriptor.getApplication().getKeepStateResolved() == false)) {

            removeBeansOnUndeploy();

        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "StatefulSessionContainer.doConcreteContainerShutdown() called with --keepstate="
                    + ejbDescriptor.getApplication().getKeepStateResolved());
            }
            passivateBeansOnShutdown();

        }
    }

    private void passivateBeansOnShutdown() {

        ClassLoader origLoader = Utility.setContextClassLoader(loader);

        try {

            _logger.log(Level.FINE, "Passivating SFSBs before container shutdown");

            if (!isPassivationCapable() && _logger.isLoggable(Level.INFO)) {
                _logger.log(Level.INFO, SFSB_NOT_RESTORED_AFTER_RESTART);
            }
            
            sessionBeanCache.shutdown();

            while (true) {
                ComponentContext ctx = null;
                synchronized (asyncTaskSemaphore) {
                    int sz = passivationCandidates.size();
                    if (sz > 0) {
                        ctx = (ComponentContext)
                                passivationCandidates.remove(sz - 1);
                    } else {
                        break;
                    }
                }
                passivateEJB(ctx);
            }



            sessionBeanCache.destroy();
            cacheProbeListener.unregister();


            try {
                // backingStore will be null when passivation-capable is false
                if (backingStore != null) {
                    backingStore.close();
                }
            } catch (BackingStoreException sfsbEx) {
                _logger.log(Level.WARNING, ERROR_DURING_BACKING_STORE_SHUTDOWN, new Object[]{ejbName, sfsbEx});
            }
        } catch (Throwable th) {
            _logger.log(Level.WARNING, ERROR_DURING_ON_SHUTDOWN, new Object[]{ejbName, th});
        } finally {
            Utility.setContextClassLoader(origLoader);
        }
    }

    private void removeBeansOnUndeploy() {

        ClassLoader origLoader = Utility.setContextClassLoader(loader);

        long myContainerId = 0;

        try {

            myContainerId = getContainerId();

            _logger.log(Level.FINE, "Removing SFSBs during application undeploy");

            sessionBeanCache.setUndeployedState();

            Iterator iter = sessionBeanCache.values();
            while (iter.hasNext()) {
                SessionContextImpl ctx = (SessionContextImpl) iter.next();
                invokePreDestroyAndUndeploy(ctx);
            }

            while (true) {
                SessionContextImpl ctx = null;
                synchronized (asyncTaskSemaphore) {
                    int sz = passivationCandidates.size();
                    if (sz > 0) {
                        ctx = (SessionContextImpl) passivationCandidates
                                .remove(sz - 1);
                        invokePreDestroyAndUndeploy(ctx);
                    } else {
                        break;
                    }
                }
            }

            sessionBeanCache.destroy();
            
            try {
                // backingStore will be null when passivation-capable is false
                if (backingStore != null) {
                    backingStore.destroy();
                }
            } catch (BackingStoreException sfsbEx) {
                _logger.log(Level.WARNING, ERROR_DURING_BACKING_STORE_SHUTDOWN, new Object[]{ejbName, sfsbEx});
            }
        } finally {

            if (sfsbVersionManager != null) {
                sfsbVersionManager.removeAll(myContainerId);
            }

            if (origLoader != null) {
                Utility.setContextClassLoader(origLoader);
            }
        }

    }

    private void invokePreDestroyAndUndeploy(SessionContextImpl ctx) {
        try {
            ctx.setInEjbRemove(true);
            destroyBean(null, ctx);
        } catch (Throwable t) {
            _logger.log(Level.FINE,
                    "exception thrown from SFSB PRE_DESTROY", t);
        } finally {
            ctx.setInEjbRemove(false);
        }

        try {
            this.undeploy(ctx);
        } catch (Exception ex) {
            _logger.log(Level.WARNING, ERROR_WHILE_UNDEPLOYING_CTX_KEY, new Object[]{ejbName, ctx.getInstanceKey()});
            _logger.log(Level.FINE, "[" + ejbName + "]: Error while "
                    + " undeploying ctx. Key: " + ctx.getInstanceKey(),
                    ex);
        }
    }

    private void cancelAllTimerTasks() {
        try {
            int size = scheduledTimerTasks.size();
            for (int i = 0; i < size; i++) {
                TimerTask task = (TimerTask) scheduledTimerTasks.get(i);
                task.cancel();
            }
        } catch (Exception ex) {
        }  finally {
            scheduledTimerTasks.clear();
        }
    }

    private void destroyBean(EjbInvocation ejbInv, EJBContextImpl ctx) {
        if (ejbInv == null) {
            ejbInv = createEjbInvocation(ctx.getEJB(), ctx);
        }

        boolean inTx = false;
        try {
            invocationManager.preInvoke(ejbInv);

            inTx = callLifecycleCallbackInTxIfUsed(ejbInv, ctx, preDestroyInvInfo, CallbackType.PRE_DESTROY);
        } catch (Throwable t) {
            _logger.log(Level.FINE,
                    "exception thrown from SFSB PRE_DESTROY", t);
        } finally {
            invocationManager.postInvoke(ejbInv);
            completeLifecycleCallbackTxIfUsed(ejbInv, ctx, inTx);
        }
    }

    /**
     * Start transaction if necessary and invoke lifecycle callback
     */
    private boolean callLifecycleCallbackInTxIfUsed(EjbInvocation ejbInv, EJBContextImpl ctx, 
            InvocationInfo invInfo, CallbackType callbackType)  throws Throwable {
        boolean inTx = (invInfo.txAttr != -1 && invInfo.txAttr != Container.TX_BEAN_MANAGED);
        if (inTx) {
            ((SessionContextImpl)ctx).setInLifeCycleCallback(true);

            // Call preInvokeTx directly.  InvocationInfo containing tx
            // attribute must be set prior to calling preInvoke
            ejbInv.transactionAttribute = invInfo.txAttr;
            ejbInv.invocationInfo = invInfo;
            preInvokeTx(ejbInv);
            enlistExtendedEntityManagers(ctx);
        }

        intercept(callbackType, ctx);

        return inTx;
    }

    /**
     * Complete transaction if necessary after lifecycle callback
     */
    private void completeLifecycleCallbackTxIfUsed(EjbInvocation ejbInv, EJBContextImpl ctx, boolean usedTx)  {
        if (usedTx) {
            delistExtendedEntityManagers(ctx);
            try {
                postInvokeTx(ejbInv);
            } catch(Exception pie) {
                _logger.log(Level.FINE, "SFSB postInvokeTx exception", pie);
            }
            ((SessionContextImpl)ctx).setInLifeCycleCallback(false);
        }
    }

    public void undeploy(SessionContextImpl ctx) {
        if (ctx.getContainer() == this) {

            if (hasRemoteHomeView) {
                EJBObjectImpl ejbObjectImpl = ctx.getEJBObjectImpl();
                if (ejbObjectImpl != null) {
                    remoteHomeRefFactory.destroyReference
                            (ejbObjectImpl.getStub(),
                                    ejbObjectImpl.getEJBObject());
                }
            }
            if (hasRemoteBusinessView) {
                EJBObjectImpl ejbBusinessObjectImpl =
                        ctx.getEJBRemoteBusinessObjectImpl();
                if (ejbBusinessObjectImpl != null) {
                    for (RemoteBusinessIntfInfo next :
                            remoteBusinessIntfInfo.values()) {
                        next.referenceFactory.destroyReference
                                (ejbBusinessObjectImpl.getStub
                                        (next.generatedRemoteIntf.getName()),
                                        ejbBusinessObjectImpl.getEJBObject
                                                (next.generatedRemoteIntf.getName()));
                    }
                }
            }
            
            sessionBeanCache.remove(ctx.getInstanceKey(), ctx.existsInStore());
            destroyExtendedEMsForContext(ctx);
            transactionManager.componentDestroyed(ctx);
        }
    }

    // CacheListener interface
    public void trimEvent(Object primaryKey, Object context) {
        boolean addTask = false;
        synchronized (asyncTaskSemaphore) {
            containerTrimCount++;
            passivationCandidates.add(context);
            int requiredTaskCount =
                    (passivationCandidates.size() / passivationBatchCount);
            addTask = (asyncTaskCount < requiredTaskCount);

            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "qSize: " + passivationCandidates.size()
                        + "; batchCount: " + passivationBatchCount
                        + "; asyncTaskCount: " + asyncTaskCount
                        + "; requiredTaskCount: " + requiredTaskCount
                        + "; ADDED TASK ==> " + addTask);
            }

            if (addTask == false) {
                return;
            }
            asyncTaskCount++;
            asyncCummTaskCount++;
        }


        try {
            ASyncPassivator work = new ASyncPassivator();
            ejbContainerUtilImpl.addWork(work);
        } catch (Exception ex) {
            synchronized (asyncTaskSemaphore) {
                asyncTaskCount--;
            }
            _logger.log(Level.WARNING, ADD_CLEANUP_TASK_ERROR, ex);
        }

    }

    private class ASyncPassivator implements Runnable {

        public void run() {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader previousClassLoader =
                    currentThread.getContextClassLoader();
            final ClassLoader myClassLoader = loader;

            boolean decrementedTaskCount = false;
            try {
                // We need to set the context class loader for
                // this (deamon) thread!!
                if (System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(myClassLoader);
                } else {
                    java.security.AccessController.doPrivileged
                            (new java.security.PrivilegedAction() {
                                public java.lang.Object run() {
                                    currentThread.setContextClassLoader(myClassLoader);
                                    return null;
                                }
                            });
                }
                ComponentContext ctx = null;

                do {
                    synchronized (asyncTaskSemaphore) {
                        int sz = passivationCandidates.size();
                        if (sz > 0) {
                            ctx = (ComponentContext)
                                    passivationCandidates.remove(sz - 1);
                        } else {
                            return;
                        }
                    }
                    passivateEJB(ctx);
                } while (true);

            } catch (Throwable th) {
                th.printStackTrace();
            } finally {
                if (!decrementedTaskCount) {
                    synchronized (asyncTaskSemaphore) {
                        asyncTaskCount--;
                    }
                }

                if (System.getSecurityManager() == null) {
                    currentThread.setContextClassLoader(previousClassLoader);
                } else {
                    java.security.AccessController.doPrivileged
                            (new java.security.PrivilegedAction() {
                                public java.lang.Object run() {
                                    currentThread.setContextClassLoader
                                            (previousClassLoader);
                                    return null;
                                }
                            });
                }
            }
        }
    }

    public void setSFSBUUIDUtil(SFSBUUIDUtil util) {
        this.uuidGenerator = util;
    }

    public void setHAEnabled(boolean isHAEnabled) {
        this.isHAEnabled = isHAEnabled;
    }

    public void setSessionCache(LruSessionCache cache) {
        this.sessionBeanCache = cache;
    }

    public void setRemovalGracePeriodInSeconds(int val) {
        this.removalGracePeriodInSeconds = val;
    }

    public void removeExpiredSessions() {
        try {
            _logger.log(Level.FINE, "StatefulContainer Removing expired sessions....");
            long val = 0;
            if (backingStore != null) {
                val = backingStore.removeExpired(this.removalGracePeriodInSeconds * 1000L);
            }

            if (cacheProbeNotifier != null) {
                cacheProbeNotifier.ejbExpiredSessionsRemovedEvent(getContainerId(),
                        containerInfo.appName, containerInfo.modName,
                        containerInfo.ejbName, val);
            }
            _logger.log(Level.FINE, "StatefulContainer Removed " + val + " sessions....");

        } catch (Exception sfsbEx) {
            _logger.log(Level.WARNING, GOT_EXCEPTION_DURING_REMOVE_EXPIRED_SESSIONS, sfsbEx);
        }
    }

    public void setSFSBVersionManager(SFSBVersionManager sfsbVersionManager) {
        this.sfsbVersionManager = sfsbVersionManager;
    }

    ///////////////////////////////////////////////////////////

    private void handleEndOfMethodCheckpoint(SessionContextImpl sc,
                                             EjbInvocation inv) {
        int txAttr = inv.invocationInfo.txAttr;
        switch (txAttr) {
            case TX_NEVER:
            case TX_SUPPORTS:
            case TX_NOT_SUPPORTED:
                if (inv.invocationInfo.checkpointEnabled) {
                    checkpointEJB(sc);
                }
                break;
            case TX_BEAN_MANAGED:
                if (sc.isTxCheckpointDelayed()
                        || inv.invocationInfo.checkpointEnabled) {
                    checkpointEJB(sc);
                    sc.setTxCheckpointDelayed(false);
                }
                break;
            default:
                if (inv.invocationInfo.isCreateHomeFinder) {
                    if (inv.invocationInfo.checkpointEnabled) {
                        checkpointEJB(sc);
                    }
                }
                break;
        }

        if (sc.getState() != BeanState.DESTROYED) {
            sc.setState(BeanState.READY);
            incrementMethodReadyStat();
            if (_logger.isLoggable(TRACE_LEVEL)) {
                logTraceInfo(inv, sc.getInstanceKey(), "Released context");
            }
        }
    }

    private void syncClientVersion(EjbInvocation inv, SessionContextImpl sc) {
        EJBLocalRemoteObject ejbLRO = inv.ejbObject;
        if (ejbLRO != null) {
            ejbLRO.setSfsbClientVersion(sc.getVersion());
        }

        if ((!inv.isLocal) && isHAEnabled) {
            long version = sc.getVersion();
            //TODO sfsbVersionManager.setResponseClientVersion(version);
            //TODO SFSBClientVersionManager.setClientVersion(getContainerId(),
                    //TODO sc.getInstanceKey(), version);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Added [synced] version: "
                        + version + " for key: " + sc.getInstanceKey());
            }
        }
    }


    //methods for StatefulSessionBeanStatsProvider
    public int getMaxCacheSize() {
        return sessionBeanCache.getMaxCacheSize();
    }

    public BackingStore<Serializable, SimpleMetadata> getBackingStore() {
        return backingStore;
    }

    public void setBackingStore(BackingStore<Serializable, SimpleMetadata> store) {
        this.backingStore = store;
    }

    private boolean checkpointEJB(SessionContextImpl sc) {

        boolean checkpointed = false;
        try {

            if ((containerState != CONTAINER_STARTED) && (containerState != CONTAINER_STOPPED)) {
                _logger.log(Level.FINE, "passivateEJB() returning because "
                        + "containerState: " + containerState);
                return false;
            }

            if (sc.getState() == BeanState.DESTROYED) {
                return false;
            }

            Object ejb = sc.getEJB();

            long checkpointStartTime = -1;
            if ((sfsbStoreMonitor != null) && sfsbStoreMonitor.isMonitoringOn()) {
                checkpointStartTime = System.currentTimeMillis();
            }

            EjbInvocation ejbInv = createEjbInvocation(ejb, sc);
            invocationManager.preInvoke(ejbInv);
            boolean needToDoPostInvokeTx = false;
            boolean destroyBean = false;

            synchronized (sc) {
                try {
                    // dont passivate if there is a Tx/invocation in progress
                    // for this instance.
                    if (sc.getState() != BeanState.READY) {
                        return false;
                    }

                    // passivate the EJB
                    sc.setState(BeanState.PASSIVATED);
                    decrementMethodReadyStat();
                    needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                            prePassivateInvInfo, CallbackType.PRE_PASSIVATE);
                    sc.setLastPersistedAt(System.currentTimeMillis());
                    byte[] serializedState = null;
                    try {
                        long newCtxVersion = sc.incrementAndGetVersion();
                        serializedState = serializeContext(sc);
                        SimpleMetadata beanState =
                               new SimpleMetadata(
                                        sc.getVersion(), sc.getLastAccessTime(),
                                        removalGracePeriodInSeconds*1000L, serializedState);
                        beanState.setVersion(newCtxVersion);
                        backingStore.save((Serializable) sc.getInstanceKey(), beanState, !sc.existsInStore());

                        //Now that we have successfully stored.....

                        sc.setLastPersistedAt(System.currentTimeMillis());
                        sc.setExistsInStore(true);
                        checkpointed = true;
                    } catch (EMNotSerializableException emNotSerEx) {
                        _logger.log(Level.WARNING, ERROR_DURING_CHECKPOINT_SESSION_ALIVE, emNotSerEx);
                    } catch (NotSerializableException notSerEx) {
                        throw notSerEx;
                    } catch (Exception ignorableEx) {
                        _logger.log(Level.WARNING, ERROR_DURING_CHECKPOINT, ignorableEx);
                    }

                    // TODO - add a flag to reactivate in the same tx
                    // Complete previous tx
                    completeLifecycleCallbackTxIfUsed(ejbInv, sc, needToDoPostInvokeTx);

                    needToDoPostInvokeTx = callLifecycleCallbackInTxIfUsed(ejbInv, sc, 
                            postActivateInvInfo, CallbackType.POST_ACTIVATE);
                    sc.setState(BeanState.READY);
                    incrementMethodReadyStat();
                    if( sfsbStoreMonitor != null ) {
                        sfsbStoreMonitor.setCheckpointSize(serializedState.length);
                        sfsbStoreMonitor.incrementCheckpointCount(true);
                    }
                } catch (Throwable ex) {
                    if( sfsbStoreMonitor != null ) {
                        sfsbStoreMonitor.incrementCheckpointCount(false);
                    }
                    _logger.log(Level.WARNING, SFSB_CHECKPOINT_ERROR_NAME, new Object[]{ejbDescriptor.getName()});
                    _logger.log(Level.WARNING, SFSB_CHECKPOINT_ERROR_KEY, new Object[]{sc.getInstanceKey(), ex});
                    destroyBean = true;
                } finally {
                    invocationManager.postInvoke(ejbInv);
                    completeLifecycleCallbackTxIfUsed(ejbInv, sc, needToDoPostInvokeTx);
                    if (destroyBean) {
                        try {
                            forceDestroyBean(sc);
                        } catch (Exception e) {
                            _logger.log(Level.FINE, "error destroying bean", e);
                        }
                    }
                    if (checkpointStartTime != -1) {
                        long timeSpent = System.currentTimeMillis()
                                - checkpointStartTime;
                        if( sfsbStoreMonitor != null ) {
                            sfsbStoreMonitor.setCheckpointTime(timeSpent);
                        }
                    }
                }
            } //synchronized

        } catch (Exception ex) {
            _logger.log(Level.WARNING, PASSIVATION_ERROR_1PARAM, new Object[]{ejbDescriptor.getName(), ex});
        }

        return checkpointed;
    }

    public void incrementMethodReadyStat() {
        statMethodReadyCount++;
        ejbProbeNotifier.methodReadyAddEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
    }

    public void decrementMethodReadyStat() {
        statMethodReadyCount--;
        ejbProbeNotifier.methodReadyRemoveEvent(getContainerId(),
                containerInfo.appName, containerInfo.modName,
                containerInfo.ejbName);
    }


    static class EEMRefInfoKey
            implements Serializable {

        private String emRefName;

        private long containerID;

        private Object instanceKey;

        private int hc;

        EEMRefInfoKey(String en, long cid, Object ikey) {
            this.emRefName = en;
            this.containerID = cid;
            this.instanceKey = ikey;

            this.hc = instanceKey.hashCode();
        }

        public int hashCode() {
            return hc;
        }

        public boolean equals(Object obj) {
            boolean result = false;
            if (obj instanceof EEMRefInfoKey) {
                EEMRefInfoKey other = (EEMRefInfoKey) obj;
                result = ((this.containerID == other.containerID)
                        && (this.emRefName.equals(other.emRefName))
                        && (this.instanceKey.equals(other.instanceKey))
                );
            }

            return result;
        }

        public String toString() {
            return "<" + instanceKey + ":" + emRefName + ":" + containerID + ">";
        }
    }

    static class EEMRefInfo
            implements IndirectlySerializable, SerializableObjectFactory {

        private transient int refCount = 0;

        private String unitName;

        private SynchronizationType synchronizationType;

        private EEMRefInfoKey eemRefInfoKey;

        private byte[] serializedEEM;

        private transient EntityManager eem;

        private transient EntityManagerFactory emf;

        private int hc;

        EEMRefInfo(String emRefName, String uName, SynchronizationType synchronizationType, long containerID,
                   Object instanceKey, EntityManager eem,
                   EntityManagerFactory emf) {

            this.eemRefInfoKey = new EEMRefInfoKey(emRefName,
                    containerID, instanceKey);
            this.eem = eem;
            this.emf = emf;
            this.unitName = uName;
            this.synchronizationType = synchronizationType;
        }

        EntityManager getEntityManager() {
            return eem;
        }

        EntityManagerFactory getEntityManagerFactory() {
            return this.emf;
        }

        EEMRefInfoKey getKey() {
            return eemRefInfoKey;
        }

        Object getSessionKey() {
            return eemRefInfoKey.instanceKey;
        }

        String getUnitName() {
            return unitName;
        }

        SynchronizationType getSynchronizationType() {
            return synchronizationType;
        }

        //Method of IndirectlySerializable
        public SerializableObjectFactory getSerializableObjectFactory()
                throws IOException {

            //Serialize the eem into the serializedEEM
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);) {
                oos.writeObject(eem);
                oos.flush();
                bos.flush();
                serializedEEM = bos.toByteArray();
            } catch (NotSerializableException notSerEx) {
                throw new EMNotSerializableException(
                        notSerEx.toString(), notSerEx);
            } catch (IOException ioEx) {
                throw new EMNotSerializableException(
                        ioEx.toString(), ioEx);
            }

            return this;
        }

        //Method of SerializableObjectFactory
        public Object createObject()
                throws IOException {

            return this;
        }

    }

    static class EMNotSerializableException
            extends NotSerializableException {

        public EMNotSerializableException(String className, Throwable th) {
            super(className);
            super.initCause(th);
        }

    }

    static class SerializableEJB
            implements IndirectlySerializable, SerializableObjectFactory {

        private byte[] serializedFields;

        SerializableEJB(Object ejb) throws IOException {
            //Serialize the ejb fields into the serializedFields
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = EjbContainerUtilImpl.getInstance().getJavaEEIOUtils().createObjectOutputStream(bos, true);) {
                EJBUtils.serializeObjectFields(ejb, oos, false);
                oos.flush();
                bos.flush();
                serializedFields = bos.toByteArray();
            }

        }

        //Method of IndirectlySerializable
        public SerializableObjectFactory getSerializableObjectFactory()
                throws IOException {

            return this;
        }

        //Method of SerializableObjectFactory
        public Object createObject() throws IOException {
            return this;
        }
    }
}

class PeriodicTask
        extends java.util.TimerTask {
    AsynchronousTask task;
    EjbContainerUtil ejbContainerUtil;

    PeriodicTask(ClassLoader classLoader, Runnable target, EjbContainerUtil ejbContainerUtil) {
        this.task = new AsynchronousTask(classLoader, target);
        this.ejbContainerUtil = ejbContainerUtil;
    }

    public void run() {
        if (!task.isExecuting()) {
            ejbContainerUtil.addWork(task);
        }
    }

    public boolean cancel() {
        boolean cancelled = super.cancel();

        this.task = null;

        return cancelled;
    }
}

class AsynchronousTask
        implements Runnable {
    ClassLoader loader;
    Runnable target;
    boolean executing;

    AsynchronousTask(ClassLoader cloassLoader, Runnable target) {
        this.loader = cloassLoader;
        this.target = target;
        this.executing = false;
    }

    boolean isExecuting() {
        return executing;
    }

    //This will be called with the correct ClassLoader
    public void run() {
        ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
        try {
            Utility.setContextClassLoader(loader);
            target.run();
        } finally {
            Utility.setContextClassLoader(prevCL);
            executing = false;
        }
    } // end run
}

