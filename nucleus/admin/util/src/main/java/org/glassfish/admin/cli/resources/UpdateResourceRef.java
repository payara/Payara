/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2017-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2026 Payara Foundation and/or its affiliates

package org.glassfish.admin.cli.resources;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ResourceRef;
import com.sun.enterprise.config.serverbeans.Resources;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import jakarta.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigCode;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;

@Service(name = "update-resource-ref")
@I18n("update.resource.ref")
@PerLookup
@ExecuteOn(value = RuntimeType.DAS)
@TargetType(value = {CommandTarget.CONFIG, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Resources.class, opType = RestEndpoint.OpType.POST, path = "update-resource-ref", description = "Update a Resource Reference on a given target")
})
public class UpdateResourceRef implements AdminCommand {

    private static final LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(UpdateResourceRef.class);

    @Param(primary = true)
    private String name;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(optional = true)
    private Boolean enabled = null;

    @Inject
    private Domain domain;

    /**
     * Execution method for updating the configuration of a ResourceRef. Will be
     * replicated if the target is a cluster.
     *
     * @param context context for the command.
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        // Make a list of all ResourceRefs that need to change
        List<ResourceRef> resourceRefsToChange = new ArrayList<>();

        // Add the ResourceRef from a named server if the target is a server
        Server server = domain.getServerNamed(target);
        // if the target is a server
        if (server != null) {
            ResourceRef serverResourceRef = server.getResourceRef(name);
            // if the ResourceRef doesn't exist
            if (serverResourceRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(serverResourceRef);
        }

        // Add the ResourceRef from a named config if the target is a config
        Config config = domain.getConfigNamed(target);
        // if the target is a config
        if (config != null) {
            ResourceRef configResourceRef = config.getResourceRef(name);
            // if the ResourceRef doesn't exist
            if (configResourceRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(configResourceRef);
        }
        
        //Add the ResourceRefs from a named Deployment Group if the target is a Deployment Group
        DeploymentGroup dg = domain.getDeploymentGroupNamed(target);
        if (dg != null) {
            ResourceRef ref = dg.getResourceRef(name);
            if (ref == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("resource.ref.not.exists", "Target {1} does not have a reference to resource {0}.", name, target));
                return;
            }
            resourceRefsToChange.add(ref);
            for (Server instance : dg.getInstances()) {
                ResourceRef instanceResourceRef = instance.getResourceRef(name);
                // if the server in the dg contains the ResourceRef
                if (instanceResourceRef != null) {
                    resourceRefsToChange.add(instanceResourceRef);
                }
            }
        }

        // Apply the configuration to the listed ResourceRefs
        try {
            ConfigSupport.apply(new ConfigCode() {
                @Override
                public Object run(ConfigBeanProxy... params) throws PropertyVetoException, TransactionFailure {
                    for (ConfigBeanProxy proxy : params) {
                        if (proxy instanceof ResourceRef) {
                            ResourceRef resourceRefProxy = (ResourceRef) proxy;
                            if (enabled != null) {
                                resourceRefProxy.setEnabled(enabled.toString());
                            }
                        }
                    }
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return true;
                }
            }, resourceRefsToChange.toArray(new ResourceRef[]{}));
        } catch (TransactionFailure ex) {
            report.failure(logger, ex.getLocalizedMessage());
        }
    }

}
