/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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
import org.glassfish.api.Param;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.OpsParams.Origin;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.config.support.TargetType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.internal.data.ApplicationInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.ApplicationRef;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.Transaction;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.io.File;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import org.glassfish.deployment.versioning.VersioningException;
import org.glassfish.deployment.versioning.VersioningService;

/**
 * Delete application ref command
 */
@Service(name="delete-application-ref")
@I18n("delete.application.ref.command")
@ExecuteOn(value={RuntimeType.DAS, RuntimeType.INSTANCE})
@Scoped(PerLookup.class)
@TargetType(value={CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER})
public class DeleteApplicationRefCommand implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(DeleteApplicationRefCommand.class);

    @Param(primary=true)
    public String name = null;

    @Param(optional=true)
    String target = "server";

    @Param(optional=true, defaultValue="false")
    public Boolean cascade;

    @Inject
    Deployment deployment;

    @Inject
    Domain domain;

    @Inject
    Applications applications;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    VersioningService versioningService;

    @Inject(name= ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    @Inject
    ServerEnvironment env;

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        // retrieve matched version(s) if exist
        List<String> matchedVersions = null;
        try {
            matchedVersions = versioningService.getMatchedVersions(name, target);
        } catch (VersioningException e) {
            report.failure(logger, e.getMessage());
            return;
        }

        // if matched list is empty and no VersioningException thrown,
        // this is an unversioned behavior and the given application is not registered
        if(matchedVersions.isEmpty()){
            if (env.isDas()) {
                // let's only do this check for DAS to be more
                // tolerable of the partial deployment case
                report.setMessage(localStrings.getLocalString("ref.not.referenced.target","Application {0} is not referenced by target {1}", name, target));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            return;
        }

        UndeployCommandParameters commandParams =
            new UndeployCommandParameters();

        if (server.isDas()) {
            commandParams.origin = Origin.unload;
        } else {
            // delete application ref on instance
            // is essentially an undeploy
            commandParams.origin = Origin.undeploy;
        }

        // for each matched version
        Iterator it = matchedVersions.iterator();
        while (it.hasNext()) {
            String appName = (String)it.next();

            Application application = applications.getApplication(appName);
            if (application == null) {
                if (env.isDas()) {
                    // let's only do this check for DAS to be more
                    // tolerable of the partial deployment case
                    report.setMessage(localStrings.getLocalString("application.notreg","Application {0} not registered", appName));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                }
                return;
            }

            ApplicationRef applicationRef = domain.getApplicationRefInTarget(appName, target);
            if (applicationRef == null) {
                if (env.isDas()) {
                    // let's only do this check for DAS to be more
                    // tolerable of the partial deployment case
                    report.setMessage(localStrings.getLocalString("appref.not.exists","Target {1} does not have a reference to application {0}.", appName, target));
                    report.setActionExitCode(ActionReport.ExitCode.WARNING);
                }
                return;
            }

            if (application.isLifecycleModule()) {
                try  {
                    deployment.unregisterAppFromDomainXML(appName, target, true);
                } catch(Exception e) {
                    report.failure(logger, e.getMessage());
                }
                return;
            }

            try {
                ReadableArchive source = null;
                ApplicationInfo appInfo = deployment.get(appName);
                if (appInfo != null) {
                    source = appInfo.getSource();
                } else {
                    File location = new File(new URI(application.getLocation()));
                    source = archiveFactory.openArchive(location);
                }

                commandParams.name = appName;
                commandParams.cascade = cascade;

                final ExtendedDeploymentContext deploymentContext =
                        deployment.getBuilder(logger, commandParams, report).source(source).build();
                deploymentContext.getAppProps().putAll(
                    application.getDeployProperties());
                deploymentContext.setModulePropsMap(
                    application.getModulePropertiesMap());

                if (domain.isCurrentInstanceMatchingTarget(target, appName, server.getName(), null)&& appInfo != null) {
                    // stop and unload application if it's the target and the
                    // the application is in enabled state
                    deployment.unload(appInfo, deploymentContext);
                }

                if (report.getActionExitCode().equals(
                    ActionReport.ExitCode.SUCCESS)) {
                    try {
                        if (server.isInstance()) {
                            // if it's on instance, we should clean up
                            // the bits
                            deployment.undeploy(appName, deploymentContext);
                            deploymentContext.clean();
                            if (!Boolean.valueOf(application.getDirectoryDeployed()) && source.exists()) {
                                FileUtils.whack(new File(source.getURI()));
                            }
                            deployment.unregisterAppFromDomainXML(appName, target);
                        } else {
                            deployment.unregisterAppFromDomainXML(appName, target, true);
                        }
                    } catch(TransactionFailure e) {
                        logger.warning("failed to delete application ref for " + appName);
                    }
                }
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Error during deleteing application ref ", e);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(e.getMessage());
            }
        }
    }
}
