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

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.TransactionFailure;

import javax.inject.Inject;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Create Resource Ref Command
 *
 * @author Jennifer Chou, Jagadish Ramu
 *
 */
@TargetType(value={CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.CLUSTER, CommandTarget.STANDALONE_INSTANCE })
@org.glassfish.api.admin.ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Service(name="create-resource-ref")
@PerLookup
@I18n("create.resource.ref")
public class CreateResourceRef implements AdminCommand {

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
    private ServerEnvironment environment;

    @Inject
    private ServiceLocator locator;

    @Inject
    private ConfigBeansUtilities configBeansUtilities;

    private String commandName = null;

    private CommandTarget targets[];

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        // check if the resource exists before creating a reference
        if (!isResourceExists(refName)) {
            report.setMessage(localStrings.getLocalString("create.resource.ref.resourceDoesNotExist",
                    "Resource {0} does not exist", refName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        boolean isBindableResource = isBindableResource(refName);
        boolean isServerResource = isServerResource(refName);
        
        Resource resource = getResourceByIdentity(refName);
        Class<?>[] allInterfaces = resource.getClass().getInterfaces();
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
                    if (name != null && name.equals(commandName))
                        return true;
                    return false;
                }
            });
            for (ServiceHandle<?> handle : serviceHandles) {
                ActiveDescriptor<?> descriptor = handle.getActiveDescriptor();
                if (descriptor.getName().equals(commandName)) {
                    AdminCommand service = locator.getService(descriptor.getImplementationClass());
                    if (service != null) {
                        TargetType targetType = descriptor.getImplementationClass().getAnnotation(TargetType.class);
                        targets = targetType.value();
                        break;
                    }
                }
            }

            if (!validateTarget(target, targets)) {
                report.setMessage(localStrings.getLocalString("create.resource.ref.resourceDoesNotHaveValidTarget",
                        "Resource {0} has Invalid target to create resource-ref on {1}.", refName, target));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;

            }

            try {
                Config config = domain.getConfigs().getConfigByName(target);
                if (config != null) {
                    if (config.isResourceRefExists(refName)) {
                        report.setMessage(localStrings.getLocalString(
                                "create.resource.ref.existsAlready",
                                "Resource ref {0} already exists for target {1}", 
                                refName, target));
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        return;
                    }
                    config.createResourceRef(enabled.toString(), refName);
                } else {
                	Server server = configBeansUtilities.getServerNamed(target);
	                if (server != null) {
	                    if (server.isResourceRefExists(refName)) {
	                        report.setMessage(localStrings.getLocalString(
	                        		"create.resource.ref.existsAlready",
	                                "Resource ref {0} already exists for target {1}", 
	                                refName, target));
	                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
	                        return;
	                    }
	                    // create new ResourceRef as a child of Server
	                    server.createResourceRef(enabled.toString(), refName);
	                } else {
	                    Cluster cluster = domain.getClusterNamed(target);
	                    if (cluster.isResourceRefExists(refName)) {
	                        report.setMessage(localStrings.getLocalString(
	                        		"create.resource.ref.existsAlready",
	                                "Resource ref {0} already exists for target {1}", 
	                                refName, target));
	                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
	                        return;
	                    }
	                    // create new ResourceRef as a child of Cluster
	                    cluster.createResourceRef(enabled.toString(), refName);

	                    // create new ResourceRef for all instances of Cluster
	                    if (isBindableResource) {
	                        Target tgt = locator.getService(Target.class);
		                    List<Server> instances = tgt.getInstances(target);
		                    for (Server svr : instances) {
		                        svr.createResourceRef(enabled.toString(), refName);
		                    }
	                    }
	                }
                }
            } catch (TransactionFailure tfe) {
                report.setMessage(localStrings.getLocalString("create.resource.ref.failed",
                        "Resource ref {0} creation failed", refName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(tfe);
                return;
            } catch (Exception e) {
                report.setMessage(localStrings.getLocalString("create.resource.ref.failed",
                        "Resource ref {0} creation failed", refName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }
            ActionReport.ExitCode ec = ActionReport.ExitCode.SUCCESS;
            report.setMessage(localStrings.getLocalString("create.resource.ref.success",
                    "resource-ref {0} created successfully.", refName));
            report.setActionExitCode(ec);
        } else {
            report.setMessage(localStrings.getLocalString("create.resource.ref.failed",
                    "Resource ref {0} creation failed", refName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
    }

    private boolean isBindableResource(String name) {
        return domain.getResources().getResourceByName(BindableResource.class, name) != null;
    }
    
    private boolean isServerResource(String name) {
        return domain.getResources().getResourceByName(ServerResource.class, name) != null;
    }
    
    private boolean isResourceExists(String jndiName) {
        return getResourceByIdentity(jndiName) != null;
    }

    private Resource getResourceByJndiName(String jndiName) {
        for (Resource resource : domain.getResources().getResources()) {
            if (resource instanceof BindableResource) {
                if (((BindableResource) resource).getJndiName().equals(jndiName)) {
                    return resource;
                }
            }
        }
        return null;
    }

    private Resource getResourceByIdentity(String id) {
        for (Resource resource : domain.getResources().getResources()) {
            if (resource.getIdentity().equals(id)) {
                return resource;
            }
        }
        return null;
    }

    private boolean validateTarget(String target, CommandTarget targets[]) {
        List validTarget = new ArrayList();

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
