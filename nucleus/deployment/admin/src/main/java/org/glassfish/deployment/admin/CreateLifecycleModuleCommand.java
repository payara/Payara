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

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.ServerTags;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import java.util.ArrayList;
import java.util.Collection;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.I18n;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.Transaction;

import java.util.Properties;
import java.util.List;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.deployment.common.DeploymentUtils;

/**
 * Create lifecycle modules.
 *
 */
@Service(name="create-lifecycle-module")
@I18n("create.lifecycle.module")
@ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@PerLookup
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
@RestEndpoints({
    @RestEndpoint(configBean=Cluster.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-lifecycle-module", 
        description="Create Lifecycle Module",
        params={
            @RestParam(name="target", value="$parent")
        }),
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="create-lifecycle-module", 
        description="Create Lifecycle Module",
        params={
            @RestParam(name="target", value="$parent")
        })
})
public class CreateLifecycleModuleCommand implements AdminCommand, AdminCommandSecurity.AccessCheckProvider {

    @Param(primary=true)
    public String name = null;

    @Param(optional=false)
    public String classname = null;

    @Param(optional=true)
    public String classpath = null;

    @Param(optional=true)
    public String loadorder = null;

    @Param(optional=true, defaultValue="false")
    public Boolean failurefatal = false;

    @Param(optional=true, defaultValue="true")
    public Boolean enabled = true;

    @Param(optional=true)
    public String description = null;

    @Param(optional=true)
    public String target = "server";

    @Param(optional=true, separator=':')
    public Properties property = null;

    @Inject
    Deployment deployment;

    @Inject
    Domain domain;

    @Inject
    Applications apps;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CreateLifecycleModuleCommand.class);

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        /*
         * One check for the life cycle module itself.
         */
        accessChecks.add(new AccessCheck(DeploymentCommandUtils.APPLICATION_RESOURCE_NAME, "create"));
        
        /*
         * One check for the target.
         */
        accessChecks.add(new AccessCheck(
                DeploymentCommandUtils.getTargetResourceNameForNewAppRef(domain, target), "create"));
        return accessChecks;
    }
   
    
    public void execute(AdminCommandContext context) {
        
        ActionReport report = context.getActionReport();

        try {
            validateTarget(target, name);
        } catch (IllegalArgumentException ie) {
            report.setMessage(ie.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        } 

        DeployCommandParameters commandParams = new DeployCommandParameters();
        commandParams.name = name;
        commandParams.enabled = enabled;
        commandParams.description = description;
        commandParams.target = target;

        // create a dummy context to hold params and props
        ExtendedDeploymentContext deploymentContext = new DeploymentContextImpl(report, null, commandParams, null);

        Properties appProps = deploymentContext.getAppProps();

        if (property!=null) {
            appProps.putAll(property);
        }

        // set to default "user", deployers can override it
        appProps.setProperty(ServerTags.OBJECT_TYPE, "user");
        appProps.setProperty(ServerTags.CLASS_NAME, classname);
        if (classpath != null) {
            appProps.setProperty(ServerTags.CLASSPATH, classpath);
        }
        if (loadorder != null) {
            appProps.setProperty(ServerTags.LOAD_ORDER, loadorder);
        }
        appProps.setProperty(ServerTags.IS_FAILURE_FATAL, 
            failurefatal.toString());
 
        appProps.setProperty(ServerTags.IS_LIFECYCLE, "true");

        try  {
            Transaction t = deployment.prepareAppConfigChanges(deploymentContext);
            deployment.registerAppInDomainXML(null, deploymentContext, t);
        } catch(Exception e) {
            report.setMessage("Failed to create lifecycle module: " + e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void validateTarget(String target, String name) {
        List<String> referencedTargets = domain.getAllReferencedTargetsForApplication(name);
        Application app = apps.getApplication(name);
        if (app != null && !app.isLifecycleModule()){
             throw new IllegalArgumentException(localStrings.getLocalString("application_withsamename_exists", "Application with same name {0} already exists, please pick a different name for the lifecycle module.", name));	
        }
        if (referencedTargets.isEmpty()) {
            if (deployment.isRegistered(name)) {
                throw new IllegalArgumentException(localStrings.getLocalString("lifecycle.use.create_app_ref_2", "Lifecycle module {0} is already created in this domain. Please use create application ref to create application reference on target {1}", name, target));
            }
        } else {
            if (referencedTargets.contains(target)) {
                throw new IllegalArgumentException(localStrings.getLocalString("lifecycle.alreadyreg", "Lifecycle module {0} is already created on this target {1}", name, target));
            } else {
                throw new IllegalArgumentException(localStrings.getLocalString("lifecycle.use.create_app_ref", "Lifecycle module {0} is already referenced by other target(s). Please use create application ref to create application reference on target {1}", name, target));
            }
        }
    }
}
