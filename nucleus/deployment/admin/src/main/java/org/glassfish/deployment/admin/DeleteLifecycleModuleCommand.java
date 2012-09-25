/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.ArrayList;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.internal.deployment.Deployment;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Server;
import java.util.Collection;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.logging.Logger;
import java.util.Collections;
import java.util.List;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;

/**
 * Delete lifecycle modules.
 *
 */
@Service(name="delete-lifecycle-module")
@I18n("delete.lifecycle.module")
@PerLookup
@ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-lifecycle-module", 
        description="Delete Lifecycle Module",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.DELETE, 
        path="delete-lifecycle-module", 
        description="Delete Lifecycle Module",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class DeleteLifecycleModuleCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(primary=true)
    public String name = null;

    @Param(optional=true)
    public String target = "server";

    @Inject
    Deployment deployment;

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment env;

    @Inject
    ServiceLocator habitat;
    
    private List<String> targets = null;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteLifecycleModuleCommand.class);

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        if (env.isDas() && DeploymentUtils.isDomainTarget(target)) {
            targets = domain.getAllReferencedTargetsForApplication(name);
            for (String t : targets) {
                final String resourceName = DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, t, name);
                if (resourceName != null) {
                    accessChecks.add(new AccessCheck(resourceName, "delete"));
                }
            }
        } else {
            final String resourceName = DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, target, name);
            if (resourceName != null) {
                accessChecks.add(new AccessCheck(resourceName, "delete"));
            }
        }
        return accessChecks;
    }
   
    public void execute(AdminCommandContext context) {
        
        ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        if (!deployment.isRegistered(name)) {
            report.setMessage(localStrings.getLocalString("lifecycle.notreg","Lifecycle module {0} not registered", name)); 
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!DeploymentUtils.isDomainTarget(target)) {
            ApplicationRef ref = domain.getApplicationRefInTarget(name, target);
            if (ref == null) {
                report.setMessage(localStrings.getLocalString("lifecycle.not.referenced.target","Lifecycle module {0} is not referenced by target {1}", name, target));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        deployment.validateUndeploymentTarget(target, name);

        if (env.isDas() && DeploymentUtils.isDomainTarget(target)) {
            // replicate command to all referenced targets
            try {
                ParameterMapExtractor extractor = new ParameterMapExtractor(this);
                ParameterMap paramMap = extractor.extract(Collections.EMPTY_LIST);
                paramMap.set("DEFAULT", name);

                ClusterOperationUtil.replicateCommand("delete-lifecycle-module", FailurePolicy.Error, 
                        FailurePolicy.Ignore, FailurePolicy.Warn, targets, context, paramMap, habitat);
            } catch (Exception e) {
                report.failure(logger, e.getMessage());
                return;
            }
        }

        try {
            deployment.unregisterAppFromDomainXML(name, target);
        } catch(Exception e) {
            report.setMessage("Failed to delete lifecycle module: " + e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
