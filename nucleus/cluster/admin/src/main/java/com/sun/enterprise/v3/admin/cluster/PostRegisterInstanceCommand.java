/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2023 [Payara Foundation and/or its affiliates]

package com.sun.enterprise.v3.admin.cluster;

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.util.InstanceRegisterInstanceCommandParameters;
import com.sun.enterprise.config.util.RegisterInstanceCommandParameters;
import com.sun.enterprise.config.serverbeans.Server;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.internal.api.Target;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import jakarta.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Causes InstanceRegisterInstanceCommand executions on the correct remote instances.
 *
 * @author Jennifer Chou
 */
@Service(name="_post-register-instance")
@Supplemental(value="_register-instance", ifFailure=FailurePolicy.Warn)
@PerLookup
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_post-register-instance", 
        description="_post-register-instance")
})
public class PostRegisterInstanceCommand extends RegisterInstanceCommandParameters implements AdminCommand {

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Target target;

    @Inject
    private Domain domain;

    @Inject
    private ConfigModularityUtils configModularityUtils;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        final InstanceRegisterInstanceCommandParameters suppInfo =
                context.getActionReport().getResultType(InstanceRegisterInstanceCommandParameters.class);

        if (suppInfo != null && (clusterName != null || deploymentGroup != null)) {
            try {
                ParameterMapExtractor pme = new ParameterMapExtractor(suppInfo, this);
                final ParameterMap paramMap = pme.extract();

                List<String> targets = new ArrayList<String>();
                List<Server> instances = target.getInstances(this.clusterName != null ? clusterName : deploymentGroup);
                for (Server s : instances) {
                    targets.add(s.getName());
                }

                // Deployment-group members each have their own config, unlike cluster members
                // who share one, so a running member has to be given the new member's config or
                // the config-ref carried by the server registered just below would dangle until
                // the member is restarted. Push it first, only for the deployment-group case.
                if (clusterName == null && deploymentGroup != null) {
                    copyConfigToRunningMembers(instanceName, targets, context);
                }

                ClusterOperationUtil.replicateCommand(
                        "_register-instance-at-instance",
                        FailurePolicy.Warn,
                        FailurePolicy.Warn,
                        FailurePolicy.Ignore,
                        targets,
                        context,
                        paramMap,
                        habitat);
            } catch (Exception e) {
                report.failure(logger, e.getMessage());
            }
        }
    }

    /**
     * Recreates the newly registered instance's config on the group's running members through
     * {@code _copy-config-at-instance}, by serializing it on the DAS and having each member
     * materialize it in its own configuration. The instance being created is not started yet, so
     * it will pick up the other members' configs from the DAS on its first startup; only the
     * existing running members need to be told about the new one's config here.
     */
    private void copyConfigToRunningMembers(String instanceName, List<String> targets, AdminCommandContext context) {
        if (targets.isEmpty()) {
            return;
        }
        Server server = domain.getServerNamed(instanceName);
        if (server == null) {
            return;
        }
        Config config = domain.getConfigNamed(server.getConfigRef());
        if (config == null) {
            return;
        }
        String configXml = configModularityUtils.serializeConfigBean(config);
        if (configXml == null || configXml.isEmpty()) {
            return;
        }
        ParameterMap parameters = new ParameterMap();
        // The command declares the config name as its primary operand, so it travels under the
        // "DEFAULT" key; see InstanceCopyConfigCommand.
        parameters.add("DEFAULT", config.getName());
        parameters.add("configxml", configXml);
        ClusterOperationUtil.replicateCommand(
                "_copy-config-at-instance",
                FailurePolicy.Warn,
                FailurePolicy.Ignore,
                FailurePolicy.Ignore,
                targets,
                context,
                parameters,
                habitat);
    }
}
