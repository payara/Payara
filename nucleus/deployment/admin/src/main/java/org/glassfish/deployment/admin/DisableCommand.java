/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.FailurePolicy;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.*;
import java.util.ArrayList;
import java.util.Collection;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.event.Events;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.DeploymentTargetResolver;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.glassfish.deployment.common.DeploymentUtils;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.deployment.DeploymentContext;

import org.glassfish.deployment.versioning.VersioningService;
import org.glassfish.deployment.versioning.VersioningException;
import org.glassfish.deployment.versioning.VersioningUtils;

/**
 * Disable command
 */
@Service(name="disable")
@I18n("disable.command")
@ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@PerLookup
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,opType=RestEndpoint.OpType.POST, path="disable", description="Disable",
        params={@RestParam(name="id", value="$parent")})
})
public class DisableCommand extends UndeployCommandParameters implements AdminCommand, 
        DeploymentTargetResolver, AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DisableCommand.class);
    
    final static String DISABLE_ACTION = "disable";

    @Param(optional=true, defaultValue="false")
    public Boolean isundeploy = false;

    @Inject
    ServerEnvironment env;

    @Inject
    Deployment deployment;

    @Inject
    Domain domain;

    @Inject
    Events events;

    @Inject
    Applications applications;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    @Inject
    VersioningService versioningService;

    @Inject
    ServiceLocator habitat;
    
    private ActionReport report;
    private Logger logger;
    private String appName;
    private Map<String,Set<String>> enabledVersionsInTargets;
    private boolean isVersionExpressionWithWildcard;
    private Set<String> enabledVersionsToDisable = Collections.EMPTY_SET;
    private List<String> matchedVersions;
    private final List<AccessCheck> accessChecks = new ArrayList<AccessCheck>();
        
    public DisableCommand() {
        origin = Origin.unload;
        command = Command.disable;
    }

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        report = context.getActionReport();
        logger = context.getLogger();
        
        appName = name();
        
        if (isundeploy) {
            origin = Origin.undeploy;
        }

        if (origin == Origin.unload && command == Command.disable) {
            // we need to set the default target for non-paas case first
            // so the versioned code could execute as expected
            if (target == null) {
                target = deployment.getDefaultTarget(_classicstyle);
            }
        }

        isVersionExpressionWithWildcard =
                VersioningUtils.isVersionExpressionWithWildCard(appName);

        if (env.isDas() && DeploymentUtils.isDomainTarget(target)) {

            enabledVersionsInTargets = Collections.EMPTY_MAP;

            if( isVersionExpressionWithWildcard ){
                enabledVersionsInTargets =
                        versioningService.getEnabledVersionInReferencedTargetsForExpression(appName);
            } else {
                enabledVersionsInTargets = new HashMap<String, Set<String>>();
                enabledVersionsInTargets.put(appName,
                        new HashSet<String>(domain.getAllReferencedTargetsForApplication(appName)));
            }
            enabledVersionsToDisable = enabledVersionsInTargets.keySet();
            return true;
        } else if ( isVersionExpressionWithWildcard ){

            try {
                matchedVersions = versioningService.getMatchedVersions(appName, target);
                if (matchedVersions == Collections.EMPTY_LIST) {
                    // no version matched by the expression
                    // nothing to do : success
                    return true;
                }
                String enabledVersion = versioningService.getEnabledVersion(appName, target);
                if (matchedVersions.contains(enabledVersion)) {
                    // the enabled version is matched by the expression
                    appName = enabledVersion;
                    return true;
                } else {
                    // the enabled version is not matched by the expression
                    // nothing to do : success
                    return true;
                }
            } catch (VersioningException e) {
                report.failure(logger, e.getMessage());
                return false;
            }
        }
        return true;
    } 

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        
        if (env.isDas() && DeploymentUtils.isDomainTarget(target)) {
            for (Map.Entry<String,Set<String>> entry : enabledVersionsInTargets.entrySet()) {
                for (String t : entry.getValue()) {
                    final String resourceForApp = DeploymentCommandUtils.getTargetResourceNameForExistingApp(domain, t, entry.getKey());
                    if (resourceForApp != null) {
                        accessChecks.add(new AccessCheck(resourceForApp, DISABLE_ACTION));
                    }
                }
            }
        } else if ( isVersionExpressionWithWildcard ){
            for (String appNm : matchedVersions) {
                final String resourceForApp = DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, target, appNm);
                accessChecks.add(new AccessCheck(resourceForApp, DISABLE_ACTION));
            }
        } else if (target == null) {
            final String resourceForApp = DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, 
                        deployment.getDefaultTarget(appName, origin, _classicstyle), appName);
            if (resourceForApp != null) {
                accessChecks.add(new AccessCheck(resourceForApp, DISABLE_ACTION));
            }
        } else {
            final String resourceForApp = DeploymentCommandUtils.getTargetResourceNameForExistingAppRef(domain, target, appName);
            if (resourceForApp != null) {
                accessChecks.add(new AccessCheck(resourceForApp, DISABLE_ACTION));
            }
        }
        final String resourceForApp = DeploymentCommandUtils.getResourceNameForExistingApp(domain, appName);
        if (resourceForApp != null) {
            accessChecks.add(new AccessCheck(resourceForApp, DISABLE_ACTION));
        }
        
        return accessChecks;
    }

    
    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        if (origin == Origin.unload && command == Command.disable) {
            // we should only validate this for the disable command
            deployment.validateSpecifiedTarget(target);
        }
        InterceptorNotifier notifier = new InterceptorNotifier(habitat, null);
        final DeployCommandSupplementalInfo suppInfo = new DeployCommandSupplementalInfo();
        suppInfo.setAccessChecks(accessChecks);
        report.setResultType(DeployCommandSupplementalInfo.class, suppInfo);

        if (env.isDas() && DeploymentUtils.isDomainTarget(target)) {

            // for each distinct enabled version in all known targets
            Iterator it = enabledVersionsInTargets.entrySet().iterator();
            while(it.hasNext()){

                Map.Entry entry = (Map.Entry)it.next();
                appName = (String)entry.getKey();

                List<String> targets =
                        new ArrayList<String>((Set<String>)entry.getValue());

                // replicate command to all referenced targets
                try {
                    ParameterMapExtractor extractor = new ParameterMapExtractor(this);
                    ParameterMap paramMap = extractor.extract(Collections.EMPTY_LIST);
                    paramMap.set("DEFAULT", appName);
                    notifier.ensureBeforeReported(ExtendedDeploymentContext.Phase.REPLICATION);
                    ClusterOperationUtil.replicateCommand("disable", FailurePolicy.Error, FailurePolicy.Warn, 
                            FailurePolicy.Ignore, targets, context, paramMap, habitat);
                } catch (Exception e) {
                    report.failure(logger, e.getMessage());
                    return;
                }
            }
        } else if ( isVersionExpressionWithWildcard ){

            try {
                if (matchedVersions == Collections.EMPTY_LIST) {
                    // no version matched by the expression
                    // nothing to do : success
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return;
                }
                String enabledVersion = versioningService.getEnabledVersion(appName, target);
                if (matchedVersions.contains(enabledVersion)) {
                    // the enabled version is matched by the expression
                    appName = enabledVersion;
                } else {
                    // the enabled version is not matched by the expression
                    // nothing to do : success
                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                    return;
                }
            } catch (VersioningException e) {
                report.failure(logger, e.getMessage());
                return;
            }
        }

        // now we resolved the version expression when applicable, we are
        // ready to do the real work

        if (target == null) {
            target = deployment.getDefaultTarget(appName, origin, _classicstyle);
        }

        if (env.isDas() || !isundeploy) {
            // we should let undeployment go through
            // on instance side for partial deployment case
            if (!deployment.isRegistered(appName)) {
                if (env.isDas()) {
                    // let's only do this check for DAS to be more
                    // tolerable of the partial deployment case
                    report.setMessage(localStrings.getLocalString("application.notreg","Application {0} not registered", appName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                }
                return;
            }

            if (!DeploymentUtils.isDomainTarget(target)) {
                ApplicationRef ref = domain.getApplicationRefInTarget(appName, target);
                if (ref == null) {
                    if (env.isDas()) {
                        // let's only do this check for DAS to be more
                        // tolerable of the partial deployment case
                        report.setMessage(localStrings.getLocalString("ref.not.referenced.target","Application {0} is not referenced by target {1}", appName, target));
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    }
                    return;
                }
            }
        }

        /*
         * If the target is a cluster instance, the DAS will broadcast the command
         * to all instances in the cluster so they can all update their configs.
         */
        if (env.isDas()) {
            try {
                notifier.ensureBeforeReported(ExtendedDeploymentContext.Phase.REPLICATION);
                DeploymentCommandUtils.replicateEnableDisableToContainingCluster(
                        "disable", domain, target, appName, habitat, context, this);

            } catch (Exception e) {
                report.failure(logger, e.getMessage());
                return;
            }
        }

        ApplicationInfo appInfo = deployment.get(appName);
        
        try {
            Application app = applications.getApplication(appName);
            this.name = appName;

            final DeploymentContext basicDC = deployment.disable(this, app, appInfo, report, logger);
            
            suppInfo.setDeploymentContext((ExtendedDeploymentContext)basicDC);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during disabling: ", e);
            if (env.isDas() || !isundeploy) {
                // we should let undeployment go through
                // on instance side for partial deployment case
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(e.getMessage());
            }
        }

        if( enabledVersionsToDisable == Collections.EMPTY_SET ) {
            enabledVersionsToDisable = new HashSet<String>();
            enabledVersionsToDisable.add(appName);
        }

        // iterating all the distinct enabled versions in all targets
        Iterator it = enabledVersionsToDisable.iterator();
        while (it.hasNext()) {

            appName = (String) it.next();
            if (!isundeploy && !report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                try {
                    deployment.updateAppEnabledAttributeInDomainXML(appName, target, false);
                } catch(TransactionFailure e) {
                    logger.warning("failed to set enable attribute for " + appName);
                }
            }
        }
    }        

    public String getTarget(ParameterMap parameters) {
        return DeploymentCommandUtils.getTarget(parameters, origin, deployment);
    }
}
