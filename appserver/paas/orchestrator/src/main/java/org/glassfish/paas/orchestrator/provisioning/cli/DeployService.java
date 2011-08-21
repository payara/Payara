/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;

@Service(name = "cloud-deploy")
@Scoped(PerLookup.class)
@CommandLock(CommandLock.LockType.NONE)
public class DeployService implements AdminCommand {

    @Param(name = "servicename", optional = true)
    private String servicename;

    @Param(name = "application", optional = false, primary = true)
    private String application;

    // The orchestrator should do service lookup mechanism to lookup the plugins.
    @Inject
    private ServiceOrchestrator orchestrator;

    @Inject
    ArchiveFactory archiveFactory;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        try {
            orchestrator.setUsingDeployService(true);
            File app = new File(application);
            //TODO get app-name from deploy command
            String appName = app.getName().substring(0, app.getName().lastIndexOf("."));

            orchestrator.deployApplication(appName, archiveFactory.openArchive(app));
        } catch (Exception ex) {
            ex.printStackTrace();
            // As long as the command is synchronous we can propagate the failure back to the client,
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(ex.toString());
            report.setFailureCause(ex);
        }finally{
            orchestrator.setUsingDeployService(false);
        }

/*
        if (serviceUtil.isValidService(servicename)) {
            String das = serviceUtil.getDomainName(servicename);
            System.out.println("DAS : " + das);
            String dasIP = serviceUtil.getIPAddress(das);
            System.out.println("DAS IP : " + dasIP);

            String target = "server";
            if (serviceUtil.isCluster(servicename)) {
                target = serviceUtil.getClusterName(servicename);
            } else if (serviceUtil.isStandaloneInstance(servicename)) {
                target = serviceUtil.getStandaloneInstanceName(servicename);
            }
            System.out.println("target for deployment is : " + target);

            ApplicationServerProvisioner provisioner = service.getAppServerProvisioner(dasIP);
            String result = provisioner.deploy(dasIP, application, "--target=" + target);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            //report.setMessage("Application ["+appName+"] deployed successfully in target ["+target+"]");
            report.setMessage(result + " in target [" + target + "]");
            //report.setMessage(result);
        } else {
            //TODO throw exception
            throw new RuntimeException("invalid service : " + servicename);
        }
*/
    }

}
