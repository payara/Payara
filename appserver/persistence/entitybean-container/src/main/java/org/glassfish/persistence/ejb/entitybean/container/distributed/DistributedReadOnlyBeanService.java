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

package org.glassfish.persistence.ejb.entitybean.container.distributed;

/**
 * DistributedReadOnlyBeanService defines the methods that can be used to 
 *  implement a distributed ReadOnly beans. An instance of 
 *  ReadOnlyBeanRefreshEventHandler is used to handle requests received from
 *  other server instances. An instance of  DistributedReadOnlyBeanNotifier is used to
 *  notify other server instances.
 *  
 * @author Mahesh Kannan
 * @see ReadOnlyBeanRefreshEventHandler
 */
public interface DistributedReadOnlyBeanService {

    /**
     * This is typically done during appserver startup time. One of the LifeCycle
     *  listeners will create an instance of DistributedReadOnlyBeanNotifier and
     *  register that instance with DistributedReadOnlyBeanService 
     *   
     * @param notifier the notifier who is responsible for notifying refresh 
     *  event(s) to other instances
     */
    public void setDistributedReadOnlyBeanNotifier(
            DistributedReadOnlyBeanNotifier notifier);
    
    /**
     * Called from ReadOnlyBeanContainer to register itself as a 
     *  ReadOnlyBeanRefreshEventHandler. 
     *  
     * @param ejbID the ejbID that uniquely identifies the container
     * @param loader the class loader that will be used to serialize/de-serialize
     *  the primary key 
     * @param handler The handler that is responsible for
     *  correctly refresing the state of a RO bean
     */
    public void addReadOnlyBeanRefreshEventHandler(
            long ejbID, ClassLoader loader,
            ReadOnlyBeanRefreshEventHandler handler);
    
    /**
     * Called from ReadOnlyBeanContainer to unregister itself as a 
     *  ReadOnlyBeanRefreshEventHandler. Typically called during undeployment.
     *  
     * @param ejbID
     */
    public void removeReadOnlyBeanRefreshEventHandler(long ejbID);
    
    /**
     * Called by the container after it has refreshed the RO bean
     * 
     * @param ejbID the ejbID that uniquely identifies the container
     * @param pk the primary key to be refreshed
     */
    public void notifyRefresh(long ejbID, Object pk);
    
    /**
     * Called by the container after it has refreshed all RO beans
     * 
     * @param ejbID the ejbID that uniquely identifies the container
     */
    public void notifyRefreshAll(long ejbID);
    
    /**
     * Called from the DistributedReadOnlyBeanNotifier when it receives a (remote) 
     *  request to refresh a RO bean
     *  
     * @param ejbID the ejbID that uniquely identifies the container
     * @param pk the primary key to be refreshed
     */
    public void handleRefreshRequest(long ejbID, byte[] pkData);
    
    /**
     * Called from the DistributedReadOnlyBeanNotifier when it receives a (remote) 
     *  request to refresh all RO beans
     *  
     * @param ejbID the ejbID that uniquely identifies the container
     */
    public void handleRefreshAllRequest(long ejbID);
    
}
