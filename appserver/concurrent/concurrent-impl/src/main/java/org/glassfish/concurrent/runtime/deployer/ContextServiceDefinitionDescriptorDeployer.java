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
import jakarta.enterprise.concurrent.ContextServiceDefinition;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.concurrent.runtime.ConcurrentRuntime;
import org.glassfish.concurrent.runtime.ContextSetupProviderImpl;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.resourcebase.resources.api.ResourceConflictException;
import org.glassfish.resourcebase.resources.api.ResourceDeployer;
import org.glassfish.resourcebase.resources.api.ResourceDeployerInfo;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.glassfish.resourcebase.resources.naming.ResourceNamingService;
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
    public void deployResource(Object resource) throws Exception {
        String applicationName = invocationManager.getCurrentInvocation().getAppName();
        String moduleName = invocationManager.getCurrentInvocation().getModuleName();
        deployResource(resource, applicationName, moduleName);
    }

    @Override
    public void deployResource(Object resource, String applicationName, String moduleName) throws Exception {
        ContextServiceDefinitionDescriptor descriptor = (ContextServiceDefinitionDescriptor) resource;
        validateContextServiceDescriptor(descriptor);
        String propageContexts = renameBuiltinContexts(descriptor.getPropagated()).stream()
                .collect(Collectors.joining(", "));
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(
                descriptor.getName(), propageContexts, "true",
                renameBuiltinContexts(descriptor.getPropagated()),
                renameBuiltinContexts(descriptor.getCleared()),
                renameBuiltinContexts(descriptor.getUnchanged()));
        String customNameOfResource = ConnectorsUtil.deriveResourceName(descriptor.getResourceId(), descriptor.getName(), descriptor.getResourceType());
        ResourceInfo resourceInfo = new ResourceInfo(customNameOfResource, applicationName, moduleName);

        ContextServiceImpl contextService = concurrentRuntime.createContextService(resourceInfo, contextServiceConfig);
        namingService.publishObject(resourceInfo, customNameOfResource, contextService, true);
    }

    @Override
    public void undeployResource(Object resource) throws Exception {
        String applicationName = invocationManager.getCurrentInvocation().getAppName();
        String moduleName = invocationManager.getCurrentInvocation().getModuleName();
        ContextServiceDefinitionDescriptor concurrentDefinitionDescriptor = (ContextServiceDefinitionDescriptor) resource;
        undeployResource(resource, applicationName, moduleName);
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

    private void validateContextServiceDescriptor(ContextServiceDefinitionDescriptor descriptor) {

        if(descriptor.getCleared() == null) {
            HashSet<String> defaultSetCleared = new HashSet<>();
            defaultSetCleared.add(ContextServiceDefinition.TRANSACTION);
            descriptor.setCleared(defaultSetCleared);
        }

        if(descriptor.getPropagated() == null) {
            HashSet<String> defaultSetPropagated = new HashSet<>();
            defaultSetPropagated.add(ContextServiceDefinition.ALL_REMAINING);
            descriptor.setPropagated(defaultSetPropagated);
        }

        if(descriptor.getUnchanged() == null){
            descriptor.setUnchanged(new HashSet<>());
        }
    }

    private Set<String> renameBuiltinContexts(Set<String> definitions) {
        Set<String> contexts = new HashSet<>();
        Set<String> unusedDefinitions = new HashSet<>(definitions);
        if (unusedDefinitions.contains(ContextServiceDefinition.TRANSACTION)) {
            contexts.add(ContextSetupProviderImpl.CONTEXT_TYPE_WORKAREA);
            unusedDefinitions.remove(ContextServiceDefinition.TRANSACTION);
        }
        if (unusedDefinitions.contains(ContextServiceDefinition.SECURITY)) {
            contexts.add(ContextSetupProviderImpl.CONTEXT_TYPE_SECURITY);
            unusedDefinitions.remove(ContextServiceDefinition.SECURITY);
        }
        if (unusedDefinitions.contains(ContextServiceDefinition.APPLICATION)) {
            contexts.add(ContextSetupProviderImpl.CONTEXT_TYPE_CLASSLOADING);
            contexts.add(ContextSetupProviderImpl.CONTEXT_TYPE_NAMING);
            unusedDefinitions.remove(ContextServiceDefinition.APPLICATION);
        }
        // add all the remaining, custom definitions
        contexts.addAll(unusedDefinitions);
        return contexts;
    }
}
