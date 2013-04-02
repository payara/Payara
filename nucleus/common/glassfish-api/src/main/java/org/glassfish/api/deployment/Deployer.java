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

package org.glassfish.api.deployment;

import org.glassfish.api.container.Container;

/**
 * A deployer is capable of deploying one type of applications.
 *
 * Deployers should use the ArchiveHandler to get a ClassLoader capable of 
 * loading classes and resources from the archive type that is being deployed.
 *
 * In all cases the ApplicationContainer subclass must return the class loader 
 * associated with the application. In case the application is deployed to more 
 * than one container the class loader can be shared and therefore should be 
 * retrieved from the ArchiveHandler 
 *
 * @param <T> is the container type associated with this deployer
 * @param <U> is the ApplicationContainer implementation for this deployer
 * @author Jerome Dochez
 */
public interface Deployer<T extends Container, U extends ApplicationContainer> {

    /**
     * Returns the meta data associated with this Deployer
     *
     * @return the meta data for this Deployer
     */
    public MetaData getMetaData();    

    /**
     * Loads the meta date associated with the application.
     *
     * @param type type of meta-data that this deployer has declared providing.
     * @return the meta-data of type V
     */
    public <V> V loadMetaData(Class<V> type, DeploymentContext context);

    /**
     * Prepares the application bits for running in the application server. 
     * For certain cases, this is generating non portable artifacts and
     * other application specific tasks. 
     * Failure to prepare should throw an exception which will cause the overall
     * deployment to fail.
     *
     * @param context of the deployment
     * @return true if the prepare phase executed successfully
     */
    public boolean prepare(DeploymentContext context);
    
    /**
     * Loads a previously prepared application in its execution environment and 
     * return a ContractProvider instance that will identify this environment in
     * future communications with the application's container runtime.
     * @param container in which the application will reside
     * @param context of the deployment
     * @return an ApplicationContainer instance identifying the running application
     */
    public U load(T container, DeploymentContext context);
    
    /** 
     * Unload or stop a previously running application identified with the 
     * ContractProvider instance. The container will be stop upon return from this
     * method. 
     * @param appContainer instance to be stopped
     * @param context of the undeployment
     */
    public void unload(U appContainer, DeploymentContext context);
    
    /**
     * Clean any files and artifacts that were created during the execution 
     * of the prepare method. 
     * @param context deployment context
     */
    public void clean(DeploymentContext context);
}
