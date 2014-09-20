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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.io.File;
import java.util.logging.Level;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.deployment.common.DeploymentUtils;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Instance-only command which makes sure that a deployment directory seems to
 * be the same when viewed from this instance as when viewed from the DAS.
 * <p>
 * The DAS computes a checksum for the deployment directory as it sees it and
 * passes it as a parameter to this command.  This command (on each instance)
 * computes a checksum for the path passed to it.  If the checksums agree
 * then we conclude that the DAS and this instance saw the same files in the
 * directory and this command reports success; otherwise this command reports
 * failure.
 *
 * @author Tim Quinn
 */
@Service(name="_instanceValidateRemoteDirDeployment")
@PerLookup
@ExecuteOn(value={RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_instanceValidateRemoteDirDeployment", 
        description="_instanceValidateRemoteDirDeployment")
})
@AccessRequired(resource="domain", action="read")
public class InstanceValidateRemoteDirDeploymentCommand implements AdminCommand {

    private static final LocalStringsImpl localStrings =
            new LocalStringsImpl(InstanceValidateRemoteDirDeploymentCommand.class);

    @Param(primary=true)
    private File path;

    @Param
    private String checksum;

    @Override
    public void execute(AdminCommandContext context) {
        context.getLogger().log(Level.FINE,
                "Running _instanceValidateRemoteDirDeployment with directory {0} and expected checksum {1}",
                new Object[]{path.getAbsolutePath(), checksum});
        final ActionReport report = context.getActionReport();

        try {
            final long myChecksum = DeploymentUtils.checksum(path);
            final long dasChecksum = Long.parseLong(checksum);
            if (dasChecksum == myChecksum) {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            } else {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.getTopMessagePart().setMessage(
                        localStrings.get("deploy.remoteDirDeployChecksumMismatch",
                        path.getAbsolutePath()));
            }
        } catch (IllegalArgumentException ex) {
            /*
             * If the path is not a directory then DeploymentUtils.checksum
             * throws an IllegalArgumentException.
             */
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.getTopMessagePart().setMessage(ex.getMessage());
        }
    }
}
