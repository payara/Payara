/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.deployment.StateCommandParameters;
import org.glassfish.api.deployment.OpsParams;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.admin.util.ClusterOperationUtil;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.common.util.admin.ParameterMapExtractor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.glassfish.internal.deployment.DeploymentTargetResolver;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.TransactionFailure;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Collections;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.versioning.VersioningSyntaxException;

import org.glassfish.deployment.versioning.VersioningService;
import org.glassfish.internal.deployment.ExtendedDeploymentContext.Phase;

/**
 * Enable command
 */
@Service(name="enable")
@I18n("enable.command")
@ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Scoped(PerLookup.class)
@TargetType(value={CommandTarget.DOMAIN, CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE})
public class EnableCommand extends StateCommandParameters implements AdminCommand, DeploymentTargetResolver {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(EnableCommand.class);

    @Inject
    Deployment deployment;

    @Inject
    Habitat habitat;

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment env;

    @Inject
    Applications applications;

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    @Inject
    VersioningService versioningService;
    
    @Inject
    ArchiveFactory archiveFactory;

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        deployment.validateSpecifiedTarget(target);

        if (target == null) {
            target = deployment.getDefaultTarget(name(), OpsParams.Origin.load);
        }

        if (!deployment.isRegistered(name())) {
            report.setMessage(localStrings.getLocalString("application.notreg","Application {0} not registered", name()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        if (!DeploymentUtils.isDomainTarget(target)) {
            ApplicationRef applicationRef = domain.getApplicationRefInTarget(name(), target);
            if (applicationRef == null) {
                report.setMessage(localStrings.getLocalString("ref.not.referenced.target","Application {0} is not referenced by target {1}", name(), target));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        // return if the application is already in enabled state
        if (domain.isAppEnabledInTarget(name(), target)) {
            logger.fine("The application is already enabled");
            return;
        }

        InterceptorNotifier notifier = new InterceptorNotifier(habitat, null);
        DeployCommandSupplementalInfo suppInfo = new DeployCommandSupplementalInfo();
        suppInfo.setDeploymentContext(notifier.dc());
        report.setResultType(DeployCommandSupplementalInfo.class, suppInfo);
        if (env.isDas()) {
            // try to disable the enabled version, if exist
            try {
                versioningService.handleDisable(name(),target, report);
            } catch (VersioningSyntaxException e) {
                report.failure(logger, e.getMessage());
                return;
            }
            if (DeploymentUtils.isDomainTarget(target)) {
                List<String> targets = domain.getAllReferencedTargetsForApplication(name());
                // replicate command to all referenced targets
                try {
                    ParameterMapExtractor extractor = new ParameterMapExtractor(this);
                    ParameterMap paramMap = extractor.extract(Collections.EMPTY_LIST);
                    paramMap.set("DEFAULT", name());

                    notifier.ensureBeforeReported(Phase.REPLICATION);
                    ClusterOperationUtil.replicateCommand("enable", FailurePolicy.Error, FailurePolicy.Warn, 
                            FailurePolicy.Ignore, targets, context, paramMap, habitat);
                } catch (Exception e) {
                    report.failure(logger, e.getMessage());
                    return;
                }
            }

            /*
             * If the target is a cluster instance, the DAS will broadcast the command
             * to all instances in the cluster so they can all update their configs.
             */

            try {
                notifier.ensureBeforeReported(Phase.REPLICATION);
                DeploymentCommandUtils.replicateEnableDisableToContainingCluster(
                        "enable", domain, target, name(), habitat, context, this);
            } catch (Exception e) {
                report.failure(logger, e.getMessage());
                return;
            }
        }

        try {
            Application app = applications.getApplication(name()); 
            ApplicationRef appRef = domain.getApplicationRefInServer(server.getName(), name());

            DeploymentContext dc = deployment.enable(target, app, appRef, report, logger);
            suppInfo.setDeploymentContext((ExtendedDeploymentContext)dc);

            if (!report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
                // update the domain.xml
                try {
                    deployment.updateAppEnabledAttributeInDomainXML(name(), target, true);
                } catch(TransactionFailure e) {
                    logger.warning("failed to set enable attribute for " + name());
                }
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error during enabling: ", e);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
        } 
    }        

    public String getTarget(ParameterMap parameters) {
        return DeploymentCommandUtils.getTarget(parameters, OpsParams.Origin.load, deployment);
    }
}
