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

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.AssocWithThreadResourceHandle;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.pool.datastructure.DataStructureFactory;
import com.sun.enterprise.resource.pool.resizer.AssocWithThreadPoolResizer;
import com.sun.enterprise.resource.pool.resizer.Resizer;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.transaction.Transaction;
import java.util.Hashtable;

/**
 * Associates a resource with the thread. When the same thread is used again,
 * it checks whether the resource associated with the thread can serve the request.
 *
 * @author Aditya Gore, Jagadish Ramu
 */
public class AssocWithThreadResourcePool extends ConnectionPool {

    private ThreadLocal<AssocWithThreadResourceHandle> localResource =
            new ThreadLocal<AssocWithThreadResourceHandle>();

    public AssocWithThreadResourcePool(PoolInfo poolInfo, Hashtable env)
            throws PoolingException {
        super(poolInfo, env);
    }

    @Override
    protected void initializePoolDataStructure() throws PoolingException {
        ds = DataStructureFactory.getDataStructure(
                "com.sun.enterprise.resource.pool.datastructure.ListDataStructure",
                dataStructureParameters,
                maxPoolSize, this, resourceSelectionStrategyClass);
    }


    /**
     * Prefetch is called to check whether there there is a free resource is already associated with the thread
     * Only when prefetch is unable to find a resource, normal routine (getUnenlistedResource) will happen.
     * @param spec ResourceSpec
     * @param alloc ResourceAllocator
     * @param tran Transaction
     * @return ResourceHandle resource associated with the thread, if any
     */
    protected ResourceHandle prefetch(ResourceSpec spec,
                                      ResourceAllocator alloc, Transaction tran) {
        AssocWithThreadResourceHandle ar = localResource.get();
        if (ar != null) {
            //synch on ar and do a quick-n-dirty check to see if the local
            //resource is usable at all
            synchronized (ar.lock) {
                if ((ar.getThreadId() != Thread.currentThread().getId()) ||
                        ar.hasConnectionErrorOccurred() ||
                        ar.isDirty() || !ar.isAssociated()) {
                    //we were associated with someone else or resource error 
                    //occurred or resource was disassociated and used by some one else. So evict
                    //NOTE: We do not setAssociated to false here since someone
                    //else has associated this resource to themself. Also, if
                    //the eviction is because of a resourceError, the resource is
                    //not going to be used anyway.

                    localResource.remove();
                    return null;
                }

                if (ar.getResourceState().isFree() &&
                        ar.getResourceState().isUnenlisted()) {
                    if (matchConnections) {
                        if (!alloc.matchConnection(ar)) {
                            //again, since the credentials of the caller don't match
                            //evict from ThreadLocal
                            //also, mark the resource as unassociated and make this resource
                            //potentially usable
                            localResource.remove();
                            ar.setAssociated(false);
                            if(poolLifeCycleListener != null){
                                poolLifeCycleListener.connectionNotMatched();
                            }
                            return null;
                        }
                        if(poolLifeCycleListener != null){
                            poolLifeCycleListener.connectionMatched();
                        }
                    }

                    if (!isConnectionValid(ar, alloc)) {
                        localResource.remove();
                        ar.setAssociated(false);
                        // disassociating the connection from the thread.
                        // validation failure will mark the connectionErrorOccurred flag
                        // and the connection will be removed whenever it is retrieved again
                        // from the pool.
                        return null;
                    }

                    setResourceStateToBusy(ar);
                    if (maxConnectionUsage_ > 0) {
                        ar.incrementUsageCount();
                    }
                    if(poolLifeCycleListener != null) {
                        poolLifeCycleListener.connectionUsed(ar.getId());
                        //Decrement numConnFree
                        poolLifeCycleListener.decrementNumConnFree();
                        
                    }
                    return ar;
                }
            }
        }

        return null;
    }

    @Override
    protected Resizer initializeResizer() {
        return new AssocWithThreadPoolResizer(poolInfo, ds, this, this,
                preferValidateOverRecreate);
    }

    /**
     * to associate a resource with the thread
     * @param h ResourceHandle
     */
    private void setInThreadLocal(AssocWithThreadResourceHandle h) {
        if (h != null) {
            synchronized (h.lock) {
                h.setThreadId(Thread.currentThread().getId());
                h.setAssociated(true);
                localResource.set(h);
            }
        }
    }

    /**
     * check whether the resource is unused
     * @param h ResourceHandle
     * @return boolean representing resource usefullness
     */
    protected boolean isResourceUnused(ResourceHandle h) {
        if(h instanceof AssocWithThreadResourceHandle){
            return h.getResourceState().isFree() && !((AssocWithThreadResourceHandle) h).isAssociated();
        }else{
            return h.getResourceState().isFree();
        }
    }


    // this is the RI getResource() with some modifications
    /**
     * return resource in free list. If none is found, returns null
     */
    protected ResourceHandle getUnenlistedResource(ResourceSpec spec,
                                                   ResourceAllocator alloc, Transaction tran) throws PoolingException {

        ResourceHandle result;
        result = super.getUnenlistedResource(spec, alloc, tran);

        //It is possible that Resizer might have marked the resource for recycle
        //and hence we should not use this resource.
        if(result != null) {
            synchronized(result.lock) {
                if(ds.getAllResources().contains(result) &&
                        ((AssocWithThreadResourceHandle)result).isDirty()) {
                    //Remove the resource and set to null
                    ds.removeResource(result);
                    result = null;
                }
            }            
        }
        //If we came here, that's because free doesn't have anything
        //to offer us. This could be because:
        //1. All free resources are associated
        //2. There are no free resources
        //3. We cannot create anymore free resources
        //Handle case 1 here

        //DISASSOCIATE
        if (result == null) {
            synchronized (this) {
                
                for (ResourceHandle resource : ds.getAllResources()) {
                    synchronized (resource.lock) {
                        //though we are checking resources from within the free list,
                        //we could have a situation where the resource was free upto
                        //this point, put just before we entered the synchronized block,
                        //the resource "h" got used by the thread that was associating it
                        //so we need to check for isFree also

                        if (resource.getResourceState().isUnenlisted() &&
                                resource.getResourceState().isFree() && 
                                !(((AssocWithThreadResourceHandle) resource).isDirty())) {
                            if (!matchConnection(resource, alloc)) {
                                continue;
                            }

                            if (resource.hasConnectionErrorOccurred()) {
                                continue;
                            }
                            result = resource;
                            setResourceStateToBusy(result);
                            ((AssocWithThreadResourceHandle) result).setAssociated(false);

                            break;
                        }
                    }
                }
            }
        }

        if (localResource.get() == null) {
            setInThreadLocal((AssocWithThreadResourceHandle) result);
        }

        return result;
    }

    /**
     * return the resource back to pool only if it is not associated with the thread.
     * @param h ResourceHandle
     */
    protected synchronized void freeUnenlistedResource(ResourceHandle h) {
        if (this.cleanupResource(h)) {
            if (h instanceof AssocWithThreadResourceHandle) {
                //Only when resource handle usage count is more than maxConnUsage
                if (maxConnectionUsage_ > 0 &&
                        h.getUsageCount() >= maxConnectionUsage_) {
                    performMaxConnectionUsageOperation(h);
                } else {

                    if (!((AssocWithThreadResourceHandle) h).isAssociated()) {
                        ds.returnResource(h);
                    }
                    //update monitoring data
                    if (poolLifeCycleListener != null) {
                        poolLifeCycleListener.decrementConnectionUsed(h.getId());
                        poolLifeCycleListener.incrementNumConnFree(false, steadyPoolSize);
                    }
                }
                //for both the cases of free.add and maxConUsageOperation, a free resource is added.
                // Hence notify waiting threads
                notifyWaitingThreads();
            }
        }
    }

    /**
     * destroys the resource
     * @param resourceHandle resource to be destroyed
     */
    public void deleteResource(ResourceHandle resourceHandle) {
        try {
            super.deleteResource(resourceHandle);
        } finally {
            //Note: here we are using the connectionErrorOccurred flag to indicate
            //that this resource is no longer usable. This flag would be checked while
            //getting from ThreadLocal
            //The main intention of marking this is to handle the case where 
            //failAllConnections happens
            //Note that setDirty only happens here - i.e during destroying of a 
            //resource

            if(resourceHandle instanceof AssocWithThreadResourceHandle){
                synchronized (resourceHandle.lock) {
                    ((AssocWithThreadResourceHandle) resourceHandle).setDirty();
                }
            }
        }
    }
}
