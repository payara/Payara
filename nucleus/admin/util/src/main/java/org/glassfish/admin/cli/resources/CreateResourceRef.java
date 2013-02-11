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

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyVetoException;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
//import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;

/**
 * Create Resource Ref Command
 *
 * @author Jennifer Chou, Jagadish Ramu
 *
 */
@TargetType(value={CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@RestEndpoints({
        @RestEndpoint(configBean=Resources.class,
                opType=RestEndpoint.OpType.POST,
                path="create-resource-ref",
                description="create-resource-ref")
})
@org.glassfish.api.admin.ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Service(name="create-resource-ref")
@PerLookup
@I18n("create.resource.ref")
public class CreateResourceRef implements AdminCommand, AdminCommandSecurity.Preauthorization,
        AdminCommandSecurity.AccessCheckProvider {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateResourceRef.class);

    @Param(optional=true, defaultValue="true")
    private Boolean enabled;

    @Param(optional=true)
    private String target = SystemPropertyConstants.DAS_SERVER_NAME;

    @Param(name="reference_name", primary=true)
    private String refName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceLocator locator;

    @Inject
    private ConfigBeansUtilities configBeansUtilities;

    private String commandName = null;

    private CommandTarget targets[];
    
    private boolean isTargetValid = false;
    
    private RefContainer refContainer = null;
    
    @AccessRequired.To("refer")
    private Resource resourceOfInterest = null;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        resourceOfInterest = getResourceByIdentity(refName);
        if (resourceOfInterest == null) {
            report.setMessage(localStrings.getLocalString("create.resource.ref.resourceDoesNotExist",
                    "Resource {0} does not exist", refName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        
        refContainer = chooseRefContainer(context); // also sets isTargetValid
        if ( ! isTargetValid) {
            report.setMessage(localStrings.getLocalString("create.resource.ref.resourceDoesNotHaveValidTarget",
                            "Resource {0} has Invalid target to create resource-ref on {1}.", refName, target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        
        return refContainer != null;
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final Collection<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        accessChecks.add(new AccessCheck(
                AccessRequired.Util.resourceNameFromConfigBeanType(refContainer, null /* collection name */, ResourceRef.class), 
                "create"));
        return accessChecks;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        
        if (isResourceRefAlreadyPresent()) {
            report.setMessage(localStrings.getLocalString(
                    "create.resource.ref.existsAlready",
                    "Resource ref {0} already exists for target {1}", 
                    refName, target));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        try {
            createResourceRef();
            // create new ResourceRef for all instances of Cluster, if it's a cluster
            if (refContainer instanceof Cluster && isElegibleResource(refName)) {
                Target tgt = locator.getService(Target.class);
                List<Server> instances = tgt.getInstances(target);
                for (Server svr : instances) {
                    svr.createResourceRef(enabled.toString(), refName);
                }
            }
            ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
            report.setMessage(localStrings.getLocalString("create.resource.ref.success",
                    "resource-ref {0} created successfully.", refName));
            report.setActionExitCode(ec);
        } catch (TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("create.resource.ref.failed",
                    "Resource ref {0} creation failed", refName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        }
    }
    
    private boolean isElegibleResource(String refName) {
        return isBindableResource(refName) || isServerResource(refName);
    }

    private boolean isResourceRefAlreadyPresent() {
        for (ResourceRef rr : refContainer.getResourceRef()) {
            if (rr.getRef().equals(refName)) {
                return true;
            }
        }
        return false;
    }
    
    private void createResourceRef() throws TransactionFailure {
        ConfigSupport.apply(new SingleConfigCode<RefContainer>() {

                public Object run(RefContainer param) throws PropertyVetoException, TransactionFailure {

                    ResourceRef newResourceRef = param.createChild(ResourceRef.class);
                    newResourceRef.setEnabled(enabled.toString());
                    newResourceRef.setRef(refName);
                    param.getResourceRef().add(newResourceRef);
                    return newResourceRef;
                }
            }, refContainer);
    }
    
    private RefContainer chooseRefContainer(final AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        Class<?>[] allInterfaces = resourceOfInterest.getClass().getInterfaces();
        for (Class<?> resourceInterface : allInterfaces) {
            ResourceConfigCreator resourceConfigCreator = (ResourceConfigCreator) resourceInterface.getAnnotation(ResourceConfigCreator.class);
            if (resourceConfigCreator != null) {
                commandName = resourceConfigCreator.commandName();
            }
        }

        if (commandName != null) {
            List<ServiceHandle<?>> serviceHandles = locator.getAllServiceHandles(new Filter() {
                @Override
                public boolean matches(Descriptor arg0) {
                    String name = arg0.getName();
                    if (name != null && name.equals(commandName)) {
                        return true;
                    }
                    return false;
                }
            });
            for (ServiceHandle<?> handle : serviceHandles) {
                ActiveDescriptor<?> descriptor = handle.getActiveDescriptor();
                if (descriptor.getName().equals(commandName)) {
                    if ( ! descriptor.isReified()) {
                        locator.reifyDescriptor(descriptor);
                    }
                    AdminCommand service = locator.<AdminCommand>getService(descriptor.getImplementationClass());
                    if (service != null) {
                        TargetType targetType = descriptor.getImplementationClass().getAnnotation(TargetType.class);
                        targets = targetType.value();
                        break;
                    }
                }
            }

            if ( ! (isTargetValid = validateTarget(target, targets))) {
                return null;
            }

            Config config = domain.getConfigs().getConfigByName(target);
            if (config != null) {
                return config;
            }
            Server server = configBeansUtilities.getServerNamed(target);
            if (server != null) {
                return server;
            }
            Cluster cluster = domain.getClusterNamed(target);
            return cluster;
        } else {
            report.setMessage(localStrings.getLocalString("create.resource.ref.failed",
                    "Resource ref {0} creation failed", refName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return null;
        }
    }

    private boolean isBindableResource(String name) {
        return domain.getResources().getResourceByName(BindableResource.class, name) != null;
    }
    
    private boolean isServerResource(String name) {
        return domain.getResources().getResourceByName(ServerResource.class, name) != null;
    }
    
//    private Resource getResourceByJndiName(String jndiName) {
//        for (Resource resource : domain.getResources().getResources()) {
//            if (resource instanceof BindableResource) {
//                if (((BindableResource) resource).getJndiName().equals(jndiName)) {
//                    return resource;
//                }
//            }
//        }
//        return null;
//    }

    private Resource getResourceByIdentity(String id) {
        for (Resource resource : domain.getResources().getResources()) {
            if (resource.getIdentity().equals(id)) {
                return resource;
            }
        }
        return null;
    }

    private boolean validateTarget(String target, CommandTarget targets[]) {
        List<String> validTarget = new ArrayList<String>();

        for (CommandTarget commandTarget : targets) {
            validTarget.add(commandTarget.name());
        }

        if (target.equals("domain")) {
            return validTarget.contains(CommandTarget.DOMAIN.name());
        } else if (target.equals("server")) {
            return validTarget.contains(CommandTarget.DAS.name());
        } else if (domain.getConfigNamed(target) != null) {
            return validTarget.contains(CommandTarget.CONFIG.name());
        } else if (domain.getClusterNamed(target) != null) {
            return validTarget.contains(CommandTarget.CLUSTER.name());
        } else if (domain.getServerNamed(target) != null) {
            return validTarget.contains(CommandTarget.STANDALONE_INSTANCE.name());
        } else if (domain.getClusterForInstance(target) != null) {
            return validTarget.contains(CommandTarget.CLUSTERED_INSTANCE.name());
        } else if (domain.getNodeNamed(target) != null) {
            return validTarget.contains(CommandTarget.NODE.name());
        }

        return false;
    }
}
