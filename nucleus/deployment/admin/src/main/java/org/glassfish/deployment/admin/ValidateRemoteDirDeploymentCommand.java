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

import com.sun.enterprise.admin.util.ClusterOperationUtil;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.deployment.common.DeploymentUtils;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * Makes sure that, if a deployment is a directory deployment to a non-DAS
 * target, that all targeted instances "see" the same files in the specified
 * deployment directory as the DAS sees.  If so, the DeployCommand runs as normal.  If not,
 * this supplemental command reports failure which prevents the DeployCommand
 * from running because it would have tried to deploy different files to
 * different instances.
 *
 * @author Tim Quinn
 */
@Service(name="_validateRemoteDirDeployment")
@Supplemental(value="deploy", on=Supplemental.Timing.Before, ifFailure=FailurePolicy.Error)
@PerLookup
@ExecuteOn(value={RuntimeType.DAS})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_validateRemoteDirDeployment", 
        description="_validateRemoteDirDeployment")
})
@AccessRequired(resource=DeploymentCommandUtils.APPLICATION_RESOURCE_NAME, action="write")
public class ValidateRemoteDirDeploymentCommand extends DeployCommandParameters
        implements AdminCommand {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(ValidateRemoteDirDeploymentCommand.class);

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServiceLocator habitat;

    @Inject
    private Deployment deployment;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();

        /*
         * This supplemental command should run only if the deployment
         * underway is a directory deployment, if the archive exists, and
         * only if the target includes a non-DAS target.
         */
        final ReadableArchive archive = archive(logger, report);
        if (archive == null) {
            /*
             * This is a little weird.  We cannot read the archive the user
             * specified.  Eventually, the deploy command will find this out
             * also.  But if we return a failure from here then the
             * command framework will report that a supplemental command has
             * failed and the deploy command will never have a chance to nicely
             * complain about the missing archive.  So, from this supplemental
             * command we'll report success so the deploy command will
             * be run.
             */
            reportSuccess(report);
            return;
        }

        final File source = new File(archive.getURI().getSchemeSpecificPart());
        try {
            archive.close();
        } catch (IOException ex) {
            report.failure(logger, ex.getLocalizedMessage(), ex);
        }
        if ( ! source.isDirectory()) {
            /*
             * This is not a directory deployment, so we're done.
             */
            reportSuccess(report);
            return;
        }

        if (target == null) {
            target = deployment.getDefaultTarget(name, origin, _classicstyle);
        }

        final TargetInfo targetInfo = new TargetInfo(target);
        if ( ! targetInfo.containsNonDAS()) {
            reportSuccess(report);
            return;
        }

        /*
         * There is at least one non-DAS target.  Compute the checksum as seen
         * here on the DAS.
         */
        final long checksum = DeploymentUtils.checksum(source);

        /*
         * Replicate the hidden validateRemoteDirDeployment command on the
         * targets, passing the URI for the directory and the checksum.
         */
        final ParameterMap paramMap = new ParameterMap();
        paramMap.add("checksum", Long.toString(checksum));
        paramMap.add("DEFAULT", path.toURI().getSchemeSpecificPart());

        ActionReport.ExitCode replicateResult = ClusterOperationUtil.replicateCommand(
                "_instanceValidateRemoteDirDeployment",
                FailurePolicy.Error,
                FailurePolicy.Ignore,
                FailurePolicy.Ignore,
                targetInfo.targetNames(),
                context,
                paramMap,
                habitat);

        report.setActionExitCode(replicateResult);

    }

    /**
     * Opens and returns an archive for the injected "path" parameter.  If there
     * is any problem the method updates the action report accordingly.
     * @param logger
     * @param report
     * @return
     */
    private ReadableArchive archive(final Logger logger, final ActionReport report) {
        try {
            return archiveFactory.openArchive(path, this);
        } catch (IOException e) {
            final String msg = localStrings.getLocalString("deploy.errOpeningArtifact",
                    "deploy.errOpeningArtifact", path.getAbsolutePath());
            if (logReportedErrors) {
                report.failure(logger, msg, e);
            } else {
                report.setMessage(msg + path.getAbsolutePath() + e.toString());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            }
            return null;
        }
    }

    private void reportSuccess(final ActionReport report) {
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        report.setMessage("");
    }

    /**
     * Collects some information about the target(s) specified by the
     * "target" parameter.
     */
    private static class TargetInfo {

        private boolean containsNonDAS = false;
        private final List<String> targetNames = new ArrayList<String>();

        private TargetInfo(final String targetExpr) {
            for (String targetName : targetExpr.split(",")) {
                targetNames.add(targetName);
                containsNonDAS |= ( ! DeploymentUtils.isDASTarget(targetName));
            }
        }

        private boolean containsNonDAS() {
            return containsNonDAS;
        }

        private List<String> targetNames() {
            return targetNames;
        }
    }
}
