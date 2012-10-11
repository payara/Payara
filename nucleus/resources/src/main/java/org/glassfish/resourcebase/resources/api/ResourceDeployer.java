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

package org.glassfish.resourcebase.resources.api;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import org.jvnet.hk2.annotations.Contract;

import java.util.Collection;

/**
 * Interface to be implemented by different resource types (eg. jms-resource)
 * to deploy/undeploy a resource to the server's runtime naming context.
 * <p/>
 * The methods can potentially be called concurrently, therefore implementation
 * need to do synchronization if necessary.
 */
@Contract
public interface ResourceDeployer {

    /**
     * Deploy the resource into the server's runtime naming context
     * This API is used in cases where the "config" bean is not
     * yet persisted in domain.xml and is part of the "config" transaction.
     *
     * @param resource a resource object (eg. JmsResource)
     * @param applicationName application-name
     * @param moduleName module-name
     * @throws Exception thrown if fail
     */
    void deployResource(Object resource, String applicationName, String moduleName) throws Exception; 

    /**
     * Deploy the resource into the server's runtime naming context
     *
     * @param resource a resource object (eg. JmsResource)
     * @throws Exception thrown if fail
     */
    void deployResource(Object resource) throws Exception;

    /**
     * Undeploy the resource from the server's runtime naming context
     *
     * @param resource a resource object (eg. JmsResource)
     * @throws Exception thrown if fail
     */
    void undeployResource(Object resource) throws Exception;

    /**
     * Undeploy the resource from the server's runtime naming context
     *
     * @param resource a resource object (eg. JmsResource)
     * @param applicationName application-name
     * @param moduleName module-name
     * @throws Exception thrown if fail
     */
    void undeployResource(Object resource, String applicationName, String moduleName) throws Exception;
    
    /**
     * Redeploy the resource into the server's runtime naming context
     *
     * @param resource a resource object
     * @throws Exception thrown if fail
     */
    void redeployResource(Object resource) throws Exception;

    /**
     * Enable the resource in the server's runtime naming context
     *
     * @param resource a resource object (eg. JmsResource)
     * @exception Exception thrown if fail
     */
	void enableResource(Object resource) throws Exception;

    /**
     * Disable the resource in the server's runtime naming context
     *
     * @param resource a resource object (eg. JmsResource)
     * @exception Exception thrown if fail
     */
	void disableResource(Object resource) throws Exception;

    /**
     * Indicates whether a particular resource deployer can handle the
     * resource in question
     * @param resource resource that need to be handled
     * @return boolean
     */
    boolean handles(Object resource);

    /**
     * Indicates whether the resource deployer can handle
     * transparent-dynamic-reconfiguration of resource
     * @return boolean indicating whether transparent-dynamic-reconfiguration is supported.
     */
    boolean supportsDynamicReconfiguration();

    /**
     * List of classes which need to be proxied for dynamic-reconfiguration
     * @return list of classes
     */
    Class[] getProxyClassesForDynamicReconfiguration();


    /**
     * A deployer can indicate whether a particular resource can be deployed before
     * application deployment</br>
     * Used in case of application-scoped-resources </br>
     * eg: Embedded RAR resources are created after application (that has embedded .rar)
     * deployment.
     * @param postApplicationDeployment post-application-deployment
     * @param allResources resources collection in which the resource being validated is present.
     * @param resource resource to be validated
     * @return boolean
     */
    boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource);


    void validatePreservedResource(Application oldApp, Application newApp, Resource resource, Resources allResources)
            throws org.glassfish.resourcebase.resources.api.ResourceConflictException;
}
