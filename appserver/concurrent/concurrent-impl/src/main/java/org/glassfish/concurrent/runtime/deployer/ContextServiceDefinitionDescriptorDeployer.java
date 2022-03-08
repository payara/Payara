/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.concurrent.runtime.deployer;

import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Resource;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.deployment.ContextServiceDefinitionDescriptor;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.logging.LogHelper;
import org.glassfish.concurrent.LogFacade;
import org.glassfish.concurrent.runtime.ConcurrentRuntime;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
import org.glassfish.resources.naming.SerializableObjectRefAddr;
import org.jvnet.hk2.annotations.Service;

/**
 * Deployer for ContextServiceDefinitionDescriptor.
 *
 * @author Petr Aubrecht <aubrecht@asoftware.cz>
 */
@Service
@ResourceDeployerInfo(ContextServiceDefinitionDescriptor.class)
public class ContextServiceDefinitionDescriptorDeployer implements ResourceDeployer {

    private static final Logger logger = Logger.getLogger(ContextServiceDefinitionDescriptorDeployer.class.getName());

    @Inject
    private ResourceNamingService namingService;

    @Inject
    private InvocationManager invocationManager;

    @Inject
    ConcurrentRuntime concurrentRuntime;

    @Override
    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        //not implemented
    }

    @Override
    public void deployResource(Object resource) throws Exception {
        ContextServiceDefinitionDescriptor concurrentDefinitionDescriptor = (ContextServiceDefinitionDescriptor) resource;
        ContextServiceConfig contextServiceConfig
                = new ContextServiceConfig(concurrentDefinitionDescriptor.getName(), null, "true");
        String applicationName = invocationManager.getCurrentInvocation().getAppName();
        String customNameOfResource = ConnectorsUtil.deriveResourceName(concurrentDefinitionDescriptor.getResourceId(), concurrentDefinitionDescriptor.getName(), concurrentDefinitionDescriptor.getResourceType());
        ResourceInfo resourceInfo = new ResourceInfo(customNameOfResource, applicationName, null);
        javax.naming.Reference ref = new javax.naming.Reference(
                jakarta.enterprise.concurrent.ContextServiceDefinition.class.getName(),
                "org.glassfish.concurrent.runtime.deployer.ConcurrentObjectFactory",
                null);
        RefAddr addr = new SerializableObjectRefAddr(ContextServiceConfig.class.getName(), contextServiceConfig);
        ref.add(addr);
        RefAddr resAddr = new SerializableObjectRefAddr(ResourceInfo.class.getName(), resourceInfo);
        ref.add(resAddr);

        try {
            // Publish the object ref
            namingService.publishObject(resourceInfo, ref, true);
        } catch (NamingException ex) {
            LogHelper.log(logger, Level.SEVERE, LogFacade.UNABLE_TO_BIND_OBJECT, ex,
                    "ContextService", contextServiceConfig.getJndiName());
        }
    }

    @Override
    public void undeployResource(Object resource) throws Exception {
        ContextServiceDefinitionDescriptor concurrentDefinitionDescriptor = (ContextServiceDefinitionDescriptor) resource;
        throw new UnsupportedOperationException("undeployResource not supported yet.");
//        ResourceInfo resourceInfo = ResourceUtil.getResourceInfo(concurrentDefinitionDescriptor);
//        undeployResource(resource, resourceInfo.getApplicationName(), resourceInfo.getModuleName());
    }

    @Override
    public void undeployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ContextServiceDefinitionDescriptor concurrentDefinitionDescriptor = (ContextServiceDefinitionDescriptor) resource;
        String jndiName = concurrentDefinitionDescriptor.getName();
        ResourceInfo resourceInfo = new ResourceInfo(jndiName, applicationName, moduleName);
        namingService.unpublishObject(resourceInfo, jndiName);
        // stop the runtime object
        concurrentRuntime.shutdownContextService(jndiName);
    }

    @Override
    public void redeployResource(Object resource) throws Exception {
        undeployResource(resource);
        deployResource(resource);
    }

    @Override
    public void enableResource(Object resource) throws Exception {
        deployResource(resource);
    }

    @Override
    public void disableResource(Object resource) throws Exception {
        undeployResource(resource);
    }

    @Override
    public boolean handles(Object resource) {
        return resource instanceof ContextServiceDefinitionDescriptor;
    }

    @Override
    public boolean supportsDynamicReconfiguration() {
        return false;
    }

    @Override
    public Class[] getProxyClassesForDynamicReconfiguration() {
        return new Class[0];
    }

    @Override
    public boolean canDeploy(boolean postApplicationDeployment, Collection<Resource> allResources, Resource resource) {
        if (handles(resource)) {
            if (!postApplicationDeployment) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validatePreservedResource(Application oldApp, Application newApp, Resource resource, Resources allResources) throws ResourceConflictException {

    }
}
