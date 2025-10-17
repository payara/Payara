/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.http.admin;

import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CLUSTERED_INSTANCE;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.DEPLOYMENT_GROUP;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;
import static org.glassfish.deployment.autodeploy.AutoDeployer.getNameFromFilePath;

import java.nio.file.Path;

import jakarta.inject.Inject;

import com.sun.enterprise.config.serverbeans.Domain;
import static fish.payara.ejb.http.admin.Constants.EJB_INVOKER_APP;
import static fish.payara.ejb.http.admin.Constants.ENDPOINTS_DIR;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.deployment.autodeploy.AutoDeployer.AutodeploymentStatus;
import org.glassfish.deployment.autodeploy.AutoUndeploymentOperation;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * This command disables the EJB invoker endpoint.
 *
 * <p>
 * This happens by undeploying the small application in
 * <code>/domains/[domain]/endpoints/__ejb-invoker</code>. Note that by default
 * this application is not deployed. The default context root of this
 * application is <code>/ejb-invoker</code>, but it may have been set to a
 * different root by the {@link EnableEjbInvokerCommand} command.
 *
 * @author Arjan Tijms
 *
 */
@Deprecated
@Service(name = "disable-ejb-invoker")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType({DAS, STANDALONE_INSTANCE, CLUSTER, CLUSTERED_INSTANCE, CONFIG, DEPLOYMENT_GROUP})
public class DisableEjbInvokerCommand implements AdminCommand {

    @Param(optional = true)
    public String target;

    @Inject
    private ServerEnvironment serverEnvironment;

    @Inject
    private ServiceLocator serviceLocator;

    @Inject
    private Domain domain;

    @Override
    public void execute(AdminCommandContext context) {

        Path endPointsPath = serverEnvironment.getInstanceRoot().toPath().resolve(ENDPOINTS_DIR);
        Path ejbInvokerPath = endPointsPath.resolve(EJB_INVOKER_APP);

        AutoUndeploymentOperation autoUndeploymentOperation = AutoUndeploymentOperation.newInstance(
                serviceLocator,
                ejbInvokerPath.toFile(),
                getNameFromFilePath(endPointsPath.toFile(), ejbInvokerPath.toFile()),
                target);

        AutodeploymentStatus undeploymentStatus = autoUndeploymentOperation.run();

        ActionReport report = context.getActionReport();
        report.setActionExitCode(undeploymentStatus.getExitCode());

        if (undeploymentStatus.getExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            if (domain.getApplications().getApplication(EJB_INVOKER_APP) == null) {
                report.appendMessage("\nEJB Invoker is not enabled on any target");
            } else {
                report.appendMessage("\nFailed to disable Ejb Invoker - was it enabled on the specified target?");
            }
        }
        context.getActionReport().setMessage("\ndisable-ejb-invoker command is deprecated."
                + "\nPlease use the 'set-ejb-invoker-configuration --enabled=false' asadmin command to disable the service.");
    }

}
