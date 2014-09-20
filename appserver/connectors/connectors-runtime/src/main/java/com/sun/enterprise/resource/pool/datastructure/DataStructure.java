/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.resource.pool.datastructure;

import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.appserv.connectors.internal.api.PoolingException;
import java.util.ArrayList;

/**
 * Represents a pool datastructure. Helps to plug-in various implementations
 * that the pool can use.<br> Datastructure need to synchronize the operations.
 *
 * @author Jagadish Ramu
 */
public interface DataStructure {

    String DS_TYPE_DEFAULT = "LIST";
    String DS_TYPE_CIRCULAR_LIST = "CIRCULAR_LIST";
    String DS_TYPE_PARTITIONED = "PARTITIONED";

    /**
     * Set maxSize based on the new max pool size set on the connection pool 
     * during a reconfiguration. 

     * @param maxSize
     */
    void setMaxSize(int maxSize);
    
    /**
     * creates a new resource and adds to the datastructure.
     *
     * @param allocator ResourceAllocator
     * @param count     Number (units) of resources to create
     * @return int number of resources added.
     * @throws PoolingException when unable to create a resource
     */
    int addResource(ResourceAllocator allocator, int count) throws PoolingException;

    /**
     * get a resource from the datastructure
     *
     * @return ResourceHandle
     */
    ResourceHandle getResource();

    /**
     * remove the specified resource from the datastructure
     *
     * @param resource ResourceHandle
     */
    void removeResource(ResourceHandle resource);

    /**
     * returns the resource to the datastructure
     *
     * @param resource ResourceHandle
     */
    void returnResource(ResourceHandle resource);

    /**
     * get the count of free resources in the datastructure
     *
     * @return int count
     */
    int getFreeListSize();

    /**
     * remove & destroy all resources from the datastructure.
     */
    void removeAll();

    /**
     * get total number of resources in the datastructure
     *
     * @return int count
     */
    int getResourcesSize();

    /**
     * Get all resources in the datastructure
     * Note : do not use this for normal usages as it
     * can potentially represent all resources
     * (including the ones in use).
     * This is used under special circumstances where there is a need
     * to process all resources.
     * @return ArrayList<ResourceHandle>
     */
    ArrayList<ResourceHandle> getAllResources();
}
