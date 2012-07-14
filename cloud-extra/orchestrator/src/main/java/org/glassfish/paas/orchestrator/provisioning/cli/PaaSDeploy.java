/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.logging.LogDomains;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.PaasApplication;
import org.glassfish.paas.orchestrator.config.PaasApplications;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@org.jvnet.hk2.annotations.Service(name = "paas-deploy")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "paas-deploy", description = "Deploy a PaaS enabled application")
})
public class PaaSDeploy implements AdminCommand {

    @Param(name = "file", primary = true)
    private File archive;

    @Inject
    private ServiceOrchestratorImpl orchestrator;

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServiceUtil serviceUtil;

    protected final static Logger logger = LogDomains.getLogger(ServiceOrchestratorImpl.class,LogDomains.PAAS_LOGGER);

    public void execute(AdminCommandContext context) {

        ReadableArchive fileArchive = null;
        try {
            fileArchive = archiveFactory.openArchive(archive);
        } catch (IOException e) {
            e.printStackTrace();
        }

        PaaSDeploymentContext dc = new PaaSDeploymentContext(archive.getName(), fileArchive);
        final String appName=dc.getAppName();

        final ActionReport report = context.getActionReport();
        PaasApplications paasApplications=serviceUtil.getPaasApplications();

        //Validation to check that an application with same name is not already deployed.
        for(PaasApplication paasApplication:paasApplications.getPaasApplications()){
            if(paasApplication.getAppName().equalsIgnoreCase(archive.getName())){
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("A paas-enabled application with name [ "+archive.getName()+" ] is already deployed.");
                return;
            }
        }

        orchestrator.preDeploy(archive.getName(), dc);
        orchestrator.deploy(archive.getName(), dc);
        orchestrator.postDeploy(archive.getName(), dc);

        //Creating a config in CPAS' domain.xml
                try {

                    if (ConfigSupport.apply(new SingleConfigCode<PaasApplications>() {
                        public Object run(PaasApplications paasApplications) throws PropertyVetoException, TransactionFailure {
                            PaasApplication paasApplication = paasApplications.createChild(PaasApplication.class);
                            paasApplication.setAppName(appName);
                            paasApplication.setEnabled(true);
                            paasApplications.getPaasApplications().add(paasApplication);
                            return paasApplication;
                        }
                    }, paasApplications) == null) {
                        //  handle this
                        logger.log(Level.SEVERE, "Error while persisting config during paas-deploy of application "+appName);
                        //TODO - ideally a Rollback should happen.
                    }

                } catch (TransactionFailure transactionFailure) {
                    //handle this
                    logger.log(Level.SEVERE, "Error while persisting config during paas-deploy of application "+appName+". Exception : "+ transactionFailure.getMessage());
                    //TODO - ideally a Rollback should happen.
                    return;

                }
    }
}
