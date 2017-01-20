/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.pool;

import com.sun.appserv.connectors.internal.api.ConnectorConstants.PoolType;
import com.sun.appserv.connectors.internal.api.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.appserv.connectors.internal.spi.MCFLifecycleListener;
import com.sun.enterprise.connectors.ConnectorConnectionPool;
import com.sun.enterprise.connectors.ConnectorRegistry;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.resource.ClientSecurityInfo;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.listener.PoolLifeCycle;
import com.sun.enterprise.resource.rm.*;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.ComponentInvocationHandler;
import org.glassfish.api.invocation.InvocationException;
import org.glassfish.resourcebase.resources.api.PoolInfo;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.RetryableUnavailableException;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tony Ng, Aditya Gore
 */
@Service
public class PoolManagerImpl extends AbstractPoolManager implements ComponentInvocationHandler {

    private final ConcurrentHashMap<PoolInfo, ResourcePool> poolTable;
    protected final static StringManager localStrings =
            StringManager.getManager(PoolManagerImpl.class);

    private ResourceManager resourceManager;
    private ResourceManager sysResourceManager;
    private ResourceManager noTxResourceManager;
    private LazyEnlistableResourceManagerImpl lazyEnlistableResourceManager;

    private static Logger _logger = null;

    @Inject
    private Provider<ConnectorRuntime> connectorRuntimeProvider;

    private ConnectorRuntime runtime;

    static {
        _logger = LogDomains.getLogger(PoolManagerImpl.class, LogDomains.RSR_LOGGER);
    }
    private PoolLifeCycle listener;

    public PoolManagerImpl() {
        this.poolTable = new ConcurrentHashMap<PoolInfo, ResourcePool>();
        resourceManager = new ResourceManagerImpl();
        sysResourceManager = new SystemResourceManagerImpl();
        noTxResourceManager = new NoTxResourceManagerImpl();
        lazyEnlistableResourceManager = new LazyEnlistableResourceManagerImpl();
    }

    public void createEmptyConnectionPool(PoolInfo poolInfo,
                                          PoolType pt, Hashtable env) throws PoolingException {
        //Create and initialise the connection pool
        createAndInitPool(poolInfo, pt, env);
        if (listener != null) {
            try {
               listener.poolCreated(poolInfo);
            } catch (Exception ex) {
                if(_logger.isLoggable(Level.FINE)) {
	            _logger.log(Level.FINE, "Exception thrown on pool listener");
                }
            }
        }
        //notify mcf-create
        ManagedConnectionFactory mcf = ConnectorRegistry.getInstance().getManagedConnectionFactory(poolInfo);
        if(mcf != null){
            if(mcf instanceof MCFLifecycleListener){
                ((MCFLifecycleListener)mcf).mcfCreated();
            }
        }
    }

    /**
     * Create and initialize pool if not created already.
     *
     * @param poolInfo Name of the pool to be created
     * @param pt       - PoolType
     * @return ResourcePool - newly created pool
     * @throws PoolingException when unable to create/initialize pool
     */
    private ResourcePool createAndInitPool(final PoolInfo poolInfo, PoolType pt, Hashtable env)
            throws PoolingException {
        ResourcePool pool = getPool(poolInfo);
        if (pool == null) {
            pool = ResourcePoolFactoryImpl.newInstance(poolInfo, pt, env);
            addPool(pool);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Created connection  pool  and added it to PoolManager :" + pool);
            }
        }
        return pool;
    }


    // invoked by DataSource objects to obtain a connection
    public Object getResource(ResourceSpec spec, ResourceAllocator alloc, ClientSecurityInfo info)
            throws PoolingException, RetryableUnavailableException {

        Transaction tran = null;
        boolean transactional = alloc.isTransactional();

        if (transactional) {
            tran = getResourceManager(spec).getTransaction();
        }

        ResourceHandle handle =
                getResourceFromPool(spec, alloc, info, tran);

        if (!handle.supportsLazyAssociation()) {
            spec.setLazyAssociatable(false);
        }

        if (spec.isLazyAssociatable() &&
                spec.getConnectionToAssociate() != null) {
            //If getConnectionToAssociate returns a connection that means
            //we need to associate a new connection with it
            try {
                Object connection = spec.getConnectionToAssociate();
                ManagedConnection dmc
                        = (ManagedConnection) handle.getResource();
                dmc.associateConnection(connection);
            } catch (ResourceException e) {
                putbackDirectToPool(handle, spec.getPoolInfo());
                PoolingException pe = new PoolingException(
                        e.getMessage());
                pe.initCause(e);
                throw pe;
            }
        }

        //If the ResourceAdapter does not support lazy enlistment
        //we cannot either
        if (!handle.supportsLazyEnlistment()) {
            spec.setLazyEnlistable(false);
        }

        handle.setResourceSpec(spec);

        try {
            if (handle.getResourceState().isUnenlisted()) {
                //The spec being used here is the spec with the updated
                //lazy enlistment info
                //Here's the real place where we care about the correct 
                //resource manager (which in turn depends upon the ResourceSpec)
                //and that's because if lazy enlistment needs to be done
                //we need to get the LazyEnlistableResourceManager
                getResourceManager(spec).enlistResource(handle);
            }
        } catch (Exception e) {
            //In the rare cases where enlistResource throws exception, we
            //should return the resource to the pool
            putbackDirectToPool(handle, spec.getPoolInfo());
            _logger.log(Level.WARNING, "poolmgr.err_enlisting_res_in_getconn",
                    spec.getPoolInfo());
            logFine("rm.enlistResource threw Exception. Returning resource to pool");
            //and rethrow the exception
            throw new PoolingException(e);

        }

        return handle.getUserConnection();
    }

    public void putbackDirectToPool(ResourceHandle h, PoolInfo poolInfo) {
        // notify pool
        if (poolInfo != null) {
            ResourcePool pool = poolTable.get(poolInfo);
            if (pool != null) {
                pool.resourceClosed(h);
            }
        }
    }

    public ResourceHandle getResourceFromPool(ResourceSpec spec, ResourceAllocator alloc, ClientSecurityInfo info,
                                              Transaction tran) throws PoolingException, RetryableUnavailableException {
        ResourcePool pool = getPool(spec.getPoolInfo());
        // pool.getResource() has been modified to:
        //      - be able to create new resource if needed
        //      - block the caller until a resource is acquired or
        //              the max-wait-time expires
        return pool.getResource(spec, alloc, tran);
    }

    /**
     * Switch on matching in the pool.
     *
     * @param poolInfo Name of the pool
     */
    public boolean switchOnMatching(PoolInfo poolInfo) {
        ResourcePool pool = getPool(poolInfo);

        if (pool != null) {
            pool.switchOnMatching();
            return true;
        } else {
            return false;
        }
    }


/*    private ConcurrentHashMap<PoolInfo, ResourcePool> getPoolTable() {
        return poolTable;
    }
*/

    private void addPool(ResourcePool pool) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine("Adding pool " + pool.getPoolInfo() + "to pooltable");
        }
        poolTable.put(pool.getPoolStatus().getPoolInfo(), pool);
    }


    private ResourceManager getResourceManager(ResourceSpec spec) {
        if (spec.isNonTx()) {
            logFine("Returning noTxResourceManager");
            return noTxResourceManager;
        } else if (spec.isPM()) {
            logFine("Returning sysResourceManager");
            return sysResourceManager;
        } else if (spec.isLazyEnlistable()) {
            logFine("Returning LazyEnlistableResourceManager");
            return lazyEnlistableResourceManager;
        } else {
            logFine("Returning resourceManager");
            return resourceManager;
        }
    }

    private void addSyncListener(Transaction tran) {
        Synchronization sync = new SynchronizationListener(tran);
        try {
            tran.registerSynchronization(sync);
        } catch (Exception ex) {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Error adding syncListener : "
                        + (ex.getMessage() != null ? ex.getMessage() : " "));
            }
        }
    }

    // called by EJB Transaction Manager
    public void transactionCompleted(Transaction tran, int status)
            throws IllegalStateException {

        Iterator iter = ((JavaEETransaction) tran).getAllParticipatingPools().iterator();
        while (iter.hasNext()) {
            ResourcePool pool = getPool((PoolInfo) iter.next());
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("calling transactionCompleted on " + pool.getPoolInfo());
            }
            pool.transactionCompleted(tran, status);
        }
    }

    public void resourceEnlisted(Transaction tran, com.sun.appserv.connectors.internal.api.ResourceHandle h)
            throws IllegalStateException {
        ResourceHandle res = (ResourceHandle) h;

        PoolInfo poolInfo = res.getResourceSpec().getPoolInfo();
        try {
            JavaEETransaction j2eeTran = (JavaEETransaction) tran;
            if (poolInfo != null && j2eeTran.getResources(poolInfo) == null) {
                addSyncListener(tran);
            }
        } catch (ClassCastException e) {
            addSyncListener(tran);
        }
        if (poolInfo != null) {
            ResourcePool pool = getPool(poolInfo);
            if (pool != null) {
                pool.resourceEnlisted(tran, res);
            }
        }
    }

    /**
     * This method gets called by the LazyEnlistableConnectionManagerImpl when
     * a connection needs enlistment, i.e on use of a Statement etc.
     */
    public void lazyEnlist(ManagedConnection mc) throws ResourceException {
        lazyEnlistableResourceManager.lazyEnlist(mc);
    }


    private ConnectorRuntime getConnectorRuntime() {
        if(runtime == null){
            runtime = connectorRuntimeProvider.get();
        }
        return runtime;
    }

    public void registerResource(com.sun.appserv.connectors.internal.api.ResourceHandle handle) throws PoolingException {
        ResourceHandle h = (ResourceHandle)handle;
        ResourceManager rm = getResourceManager(h.getResourceSpec());
        rm.registerResource(h);
    }

    public void registerPoolLifeCycleListener(PoolLifeCycle poolListener) {
        listener = poolListener;
    }

    public void unregisterPoolLifeCycleListener() {
        listener = null;
    }
    
    public void unregisterResource(com.sun.appserv.connectors.internal.api.ResourceHandle resource, int xaresFlag) {
        ResourceHandle h = (ResourceHandle)resource;
        ResourceManager rm = getResourceManager(h.getResourceSpec());
        rm.unregisterResource(h, xaresFlag);
    }

    public void resourceClosed(ResourceHandle resource) {
        ResourceManager rm = getResourceManager(resource.getResourceSpec());
        rm.delistResource(resource, XAResource.TMSUCCESS);
        putbackResourceToPool(resource, false);
    }

    public void badResourceClosed(ResourceHandle resource) {
        ResourceManager rm = getResourceManager(resource.getResourceSpec());
        rm.delistResource(resource, XAResource.TMSUCCESS);
        putbackBadResourceToPool(resource);
    }

    public void resourceErrorOccurred(ResourceHandle resource) {
        putbackResourceToPool(resource, true);
    }

    public void resourceAbortOccurred(ResourceHandle resource) {
        ResourceManager rm = getResourceManager(resource.getResourceSpec());
        rm.delistResource(resource, XAResource.TMSUCCESS);
        putbackResourceToPool(resource, true);
    }

    public void putbackBadResourceToPool(ResourceHandle h) {

        // notify pool
        PoolInfo poolInfo = h.getResourceSpec().getPoolInfo();
        if (poolInfo != null) {
            ResourcePool pool = poolTable.get(poolInfo);
            if (pool != null) {
                synchronized (pool) {
                    pool.resourceClosed(h);
                    h.setConnectionErrorOccurred();
                    pool.resourceErrorOccurred(h);
                }
            }
        }
    }

    public void putbackResourceToPool(ResourceHandle h,
                                      boolean errorOccurred) {

        // notify pool
        PoolInfo poolInfo = h.getResourceSpec().getPoolInfo();
        if (poolInfo != null) {
            ResourcePool pool = poolTable.get(poolInfo);
            if (pool != null) {
                if (errorOccurred) {
                    pool.resourceErrorOccurred(h);
                } else {
                    pool.resourceClosed(h);
                }
            }
        }
    }


    /**
     * Use this method if the string being passed does not <br>
     * involve multiple concatenations<br>
     * Avoid using this method in exception-catch blocks as they
     * are not frequently executed <br>
     *
     * @param msg String
     */
    private void logFine(String msg) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(msg);
        }
    }

    public ResourcePool getPool(PoolInfo poolInfo) {
        if (poolInfo == null) {
            return null;
        }
        return poolTable.get(poolInfo);
    }

    /**
     * Kill the pool with the specified pool name
     *
     * @param poolInfo - The name of the pool to kill
     */
    public void killPool(PoolInfo poolInfo) {

        //empty the pool
        //and remove from poolTable
        ResourcePool pool = poolTable.get(poolInfo);
        if (pool != null) {
            pool.cancelResizerTask();
            pool.emptyPool();
            if (_logger.isLoggable(Level.FINE)) {
                _logger.fine("Removing pool " + pool + " from pooltable");
            }
            poolTable.remove(poolInfo);
            if (listener != null){
                listener.poolDestroyed(poolInfo);
            }

            //notify mcf-destroy
            ManagedConnectionFactory mcf = ConnectorRegistry.getInstance().getManagedConnectionFactory(poolInfo);
            if(mcf != null){
                if(mcf instanceof MCFLifecycleListener){
                    ((MCFLifecycleListener)mcf).mcfDestroyed();
                }
            }
        }
    }

    public void killFreeConnectionsInPools() {
           Iterator pools = poolTable.values().iterator();
           logFine("Killing all free connections in pools");
           while (pools.hasNext()) {
               ResourcePool pool = (ResourcePool) pools.next();
               if (pool != null) {
                   PoolInfo poolInfo = pool.getPoolStatus().getPoolInfo();
                   try {
                       if (poolInfo != null) {
                           ResourcePool poolToKill = poolTable.get(poolInfo);
                           if (poolToKill != null) {
                               pool.emptyFreeConnectionsInPool();
                           }
                           if (_logger.isLoggable(Level.FINE)){
                               _logger.fine("Now killing free connections in pool : " + poolInfo);
                           }
                       }
                   } catch (Exception e) {
                       if (_logger.isLoggable(Level.FINE)) {
                           _logger.fine("Error killing pool : " + poolInfo + " :: "
                                   + (e.getMessage() != null ? e.getMessage() : " "));
                       }
                   }
               }
           }
       }
    
    public ResourceReferenceDescriptor getResourceReference(String jndiName, String logicalName) {
        Set descriptors = getConnectorRuntime().getResourceReferenceDescriptor();
        List matchingRefs = new ArrayList();

        if (descriptors != null) {
            for (Object descriptor : descriptors) {
                ResourceReferenceDescriptor ref =
                        (ResourceReferenceDescriptor) descriptor;
                String name = ref.getJndiName();
                if (jndiName.equals(name)) {
                     matchingRefs.add(ref);
                }
            }
        }
        if(matchingRefs.size()==1){
            return (ResourceReferenceDescriptor)matchingRefs.get(0);
        }else if(matchingRefs.size() > 1){
            Iterator it = matchingRefs.iterator();
            while(it.hasNext()){
                ResourceReferenceDescriptor rrd = (ResourceReferenceDescriptor)it.next();
                String refName = rrd.getName();
                if(refName != null && logicalName != null){
                    refName = getJavaName(refName);
                    if(refName.equals(getJavaName(logicalName))){
                        return rrd;
                    }
                }
            }
        }
        return null;
    }

    private static String getJavaName(String name){
        if(name == null || name.startsWith("java:")){
            return name;
        }else {
            //by default, scope is "comp"
            return "java:comp/env/" + name;
        }
    }

    public void beforePreInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv,
                                ComponentInvocation newInv) throws InvocationException {
        //no-op
    }

    public void afterPreInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv,
                               ComponentInvocation curInv) throws InvocationException {
        //no-op
    }

    public void beforePostInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv,
                                 ComponentInvocation curInv) throws InvocationException {
        //no-op
    }

    /*
    * Called by the InvocationManager at methodEnd. This method
    * will disassociate ManagedConnection instances from Connection
    * handles if the ResourceAdapter supports that.
    */
    public void afterPostInvoke(ComponentInvocation.ComponentInvocationType invType, ComponentInvocation prevInv,
                                ComponentInvocation curInv) throws InvocationException {
        postInvoke(curInv);
    }

    private void postInvoke(ComponentInvocation curInv){

        ComponentInvocation invToUse = curInv;
/*
        if(invToUse == null){
            invToUse = getConnectorRuntime().getInvocationManager().getCurrentInvocation();
        }
*/
        if (invToUse == null) {
            return;
        }

        Object comp = invToUse.getInstance();

        if (comp == null) {
            return;
        }

        handleLazilyAssociatedConnectionPools(comp, invToUse);
    }

    /**
     * If the connections associated with the component are lazily-associatable, dissociate them.
     * @param comp Component that acquired connections
     * @param invToUse component invocation
     */
    private void handleLazilyAssociatedConnectionPools(Object comp, ComponentInvocation invToUse) {
        JavaEETransactionManager tm = getConnectorRuntime().getTransactionManager();
        List list = tm.getExistingResourceList(comp, invToUse);
        if (list == null) {
            //For invocations of asadmin the ComponentInvocation does not
            //have any resources and hence the existingResourcesList is null
            return;
        }

        if (list.size() == 0) return;

        ResourceHandle[] handles = new ResourceHandle[list.size()];
        handles = (ResourceHandle[]) list.toArray(handles);
        for (ResourceHandle h : handles) {
            ResourceSpec spec = h.getResourceSpec();
            if (spec.isLazyAssociatable()) {
                //In this case we are assured that the managedConnection is
                //of type DissociatableManagedConnection
                javax.resource.spi.DissociatableManagedConnection mc =
                        (javax.resource.spi.DissociatableManagedConnection) h.getResource();
                if (h.isEnlisted()) {
                    getResourceManager(spec).delistResource(
                            h, XAResource.TMSUCCESS);
                }
                try {
                    mc.dissociateConnections();
                } catch (ResourceException re) {
                    InvocationException ie = new InvocationException(
                            re.getMessage());
                    ie.initCause(re);
                    throw ie;
                } finally {
                    if (h.getResourceState().isBusy()) {
                        putbackDirectToPool(h, spec.getPoolInfo());
                    }
                }
            }
        }
    }

    class SynchronizationListener implements Synchronization {

        private Transaction tran;

        SynchronizationListener(Transaction tran) {
            this.tran = tran;
        }

        public void afterCompletion(int status) {
            try {
                transactionCompleted(tran, status);
            } catch (Exception ex) {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.fine("Exception in afterCompletion : " +
                        (ex.getMessage() != null ? ex.getMessage() : " "));
                }
            }
        }

        public void beforeCompletion() {
            // do nothing
        }
    }

    public void reconfigPoolProperties(ConnectorConnectionPool ccp) throws PoolingException {
        PoolInfo poolInfo = ccp.getPoolInfo();
        ResourcePool pool = getPool( poolInfo );
        if (pool != null ) {
            pool.reconfigurePool( ccp );
        }        
    }

    /**
     * Flush Connection pool by reinitializing the connections 
     * established in the pool.
     * @param poolInfo
     * @throws com.sun.appserv.connectors.internal.api.PoolingException
     */
    public boolean flushConnectionPool(PoolInfo poolInfo) throws PoolingException {
        boolean result = false;
        ResourcePool pool = getPool( poolInfo );
        if(pool != null) {
            result = pool.flushConnectionPool();
        } else {
            _logger.log(Level.WARNING, "poolmgr.flush_noop_pool_not_initialized", poolInfo);
            String exString = localStrings.getString("poolmgr.flush_noop_pool_not_initialized",
                    poolInfo.toString());
            throw new PoolingException(exString);
        }
        return result;
    }

    /**
     * Get connection pool status.
     * @param poolInfo
     * @return
     */
    public PoolStatus getPoolStatus(PoolInfo poolInfo) {
        ResourcePool pool = poolTable.get(poolInfo);
        if(pool != null) {
            return pool.getPoolStatus();
        } else {
            //TODO log exception
            return null;
        }
    }
}
