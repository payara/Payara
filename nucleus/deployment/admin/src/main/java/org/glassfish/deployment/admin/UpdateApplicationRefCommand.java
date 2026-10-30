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

package org.glassfish.deployment.admin;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

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

import fish.payara.enterprise.config.serverbeans.DeploymentGroup;

@Service(name = "update-application-ref")
@I18n("update.application.ref.command")
@PerLookup
@ExecuteOn(value = {RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value = {CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.DEPLOYMENT_GROUP})
@RestEndpoints({
    @RestEndpoint(configBean = Cluster.class, opType = RestEndpoint.OpType.POST,
            path = "update-application-ref", description = "Update an Application Reference on a cluster target"),
    @RestEndpoint(configBean = Server.class, opType = RestEndpoint.OpType.POST,
            path = "update-application-ref", description = "Update an Application Reference on a server target"),
    @RestEndpoint(configBean = DeploymentGroup.class, opType = RestEndpoint.OpType.POST,
            path = "update-application-ref", description = "Update an Application Reference on a server target")
})
public class UpdateApplicationRefCommand implements AdminCommand {

    private final static LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(UpdateApplicationRefCommand.class);

    @Param(primary = true)
    private String name;

    @Param(optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    private String target;

    @Param(optional = true, alias = "virtual-servers")
    private String virtualservers = null;

    @Param(optional = true)
    private Boolean enabled = null;

    @Param(optional = true)
    private Boolean lbenabled = null;

    @Inject
    private Domain domain;

    /**
     * Execution method for updating the configuration of an ApplicationRef.
     * Will be replicated if the target is a cluster.
     *
     * @param context context for the command.
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        // Make a list of all ApplicationRefs that need to change
        List<ApplicationRef> applicationRefsToChange = new ArrayList<>();

        // Add the ApplicationRef which is being immediately targetted
        {
            ApplicationRef primaryApplicationRef = domain.getApplicationRefInTarget(name, target);
            if (primaryApplicationRef == null) {
                report.failure(logger, LOCAL_STRINGS.getLocalString("appref.not.exists", "Target {1} does not have a reference to application {0}.", name, target));
                return;
            }
            applicationRefsToChange.add(primaryApplicationRef);
        }

        // Add the implicitly targetted ApplicationRefs if the target is in a cluster or deployment group
        {
            Cluster cluster = domain.getClusterNamed(target);
            // if the target is a cluster
            if (cluster != null) {
                for (Server server : cluster.getInstances()) {
                    ApplicationRef instanceAppRef = server.getApplicationRef(name);
                    // if the server in the cluster contains the ApplicationRef
                    if (instanceAppRef != null) {
                        applicationRefsToChange.add(instanceAppRef);
                    }
                }
            }
            
            DeploymentGroup dg = domain.getDeploymentGroupNamed(target);
            if (dg != null)  {
                for (Server server: dg.getInstances()) {
                    ApplicationRef instanceAppRef = server.getApplicationRef(name);
                    // if the server in the dg contains the ApplicationRef
                    if (instanceAppRef != null) {
                        applicationRefsToChange.add(instanceAppRef);
                    }                    
                }
            }
        }

        // Apply the configuration to the listed ApplicationRefs
        try {
            ConfigSupport.apply(new ConfigCode() {
                @Override
                public Object run(ConfigBeanProxy... params) throws PropertyVetoException, TransactionFailure {
                    for (ConfigBeanProxy proxy : params) {
                        if (proxy instanceof ApplicationRef) {
                            ApplicationRef applicationRefProxy = (ApplicationRef) proxy;
                            if (enabled != null) {
                                applicationRefProxy.setEnabled(enabled.toString());
                            }
                            if (virtualservers != null) {
                                applicationRefProxy.setVirtualServers(virtualservers);
                            }
                            if (lbenabled != null) {
                                applicationRefProxy.setLbEnabled(lbenabled.toString());
                            }
                        }
                    }
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return true;
                }
            }, applicationRefsToChange.toArray(new ApplicationRef[]{}));
        } catch (TransactionFailure ex) {
            report.failure(logger, ex.getLocalizedMessage());
        }
    }

}
