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

package com.sun.enterprise.resource.pool;

import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.appserv.connectors.internal.api.PoolingException;

import javax.resource.ResourceException;
import java.util.Set;

/**
 * ResourceHandler to create/delete resource
 *
 * @author Jagadish Ramu
 */
public interface ResourceHandler {

    /**
     * destroys the given resource
     * @param resourceHandle resource to be destroyed
     */
    void deleteResource(ResourceHandle resourceHandle);

    /**
     * create a new resource using the given resource-allocator
     * @param allocator allocator to create a resource
     * @return newly created resource
     * @throws PoolingException when unable to create a resource
     */
    ResourceHandle createResource(ResourceAllocator allocator) throws PoolingException;

    /**
     * create a new resource and add it to pool (using default resource-allocator)
     * @throws PoolingException when unable to create a resource
     */
    void createResourceAndAddToPool() throws PoolingException;

    /**
     * gets the invalid connections from the given connections set
     * @param connections that need to be validated
     * @return invalid connections set
     * @throws ResourceException when unable to validate
     */
    Set getInvalidConnections(Set connections) throws ResourceException;

    /**
     * callback method to handle the case of invalid connection detected
     * @param h connection that is invalid
     */
    void invalidConnectionDetected(ResourceHandle h);
}
