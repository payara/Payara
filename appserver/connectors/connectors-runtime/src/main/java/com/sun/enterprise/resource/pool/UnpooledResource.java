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
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.ResourceState;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.transaction.Transaction;
import java.util.Hashtable;

/**
 * This resource pool is created when connection pooling is switched off
 * Hence no pooling happens in this resource pool
 *
 * @author Kshitiz Saxena
 * @since 9.1
 */
public class UnpooledResource extends ConnectionPool{

    private int poolSize;

    /** Creates a new instance of UnpooledResourcePool */
    public UnpooledResource(PoolInfo poolInfo, Hashtable env) throws PoolingException {
        super(poolInfo, env);

        //No pool is being maintained, hence no pool cleanup is needed
        //in case of failure
        failAllConnections = false;
    }

    @Override
    protected synchronized void initPool(ResourceAllocator allocator)
            throws PoolingException{

        if (poolInitialized) {
            return;
        }

        //nothing needs to be done as pooling is disabled
        poolSize = 0;

        poolInitialized = true;
    }

    @Override
    protected ResourceHandle prefetch(ResourceSpec spec, ResourceAllocator alloc,
            Transaction tran) {
        return null;
    }

    @Override
    protected void reconfigureSteadyPoolSize(int oldSteadyPoolSize,
                                           int newSteadyPoolSize) throws PoolingException {
        //No-op as the steady pool size should not be reconfigured when connection
        //pooling is switched off
    }

    @Override
    protected ResourceHandle getUnenlistedResource(ResourceSpec spec, ResourceAllocator alloc,
            Transaction tran) throws PoolingException {
        ResourceHandle handle = null;

        if(incrementPoolSize()){
            try{
                handle = createSingleResource(alloc);
            }catch (PoolingException ex){
                decrementPoolSize();
                throw ex;
            }
            ResourceState state = new ResourceState();
            handle.setResourceState(state);
            state.setEnlisted(false);
            setResourceStateToBusy(handle);
            return handle;
        }
        String msg = localStrings.getStringWithDefault(
                "poolmgr.max.pool.size.reached",
                "In-use connections equal max-pool-size therefore cannot allocate any more connections.");
        throw new PoolingException(msg);
    }

    @Override
    public void resourceErrorOccurred(ResourceHandle resourceHandle) throws IllegalStateException {
        freeResource(resourceHandle);
    }

    @Override
    protected void freeResource(ResourceHandle resourceHandle){
        decrementPoolSize();
        deleteResource(resourceHandle);
    }

    private synchronized boolean incrementPoolSize(){
        if(poolSize >= maxPoolSize){
            _logger.info("Fail as poolSize : " + poolSize);
            return false;
        }
        poolSize++;
        return true;
    }

    private synchronized void decrementPoolSize(){
        poolSize--;
    }
}
