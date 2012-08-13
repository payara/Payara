/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.FailurePolicy;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.Supplemental;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired;
import org.jvnet.hk2.annotations.Service;

/**
 * Runs after any replications of the undeploy command have been sent to
 * instances.
 * 
 * @author Tim Quinn
 */
@Service(name="_postundeploy")
@Supplemental(value="undeploy", ifFailure=FailurePolicy.Warn, on= Supplemental.Timing.AfterReplication)
@PerLookup
@ExecuteOn(value={RuntimeType.DAS})
@AccessRequired(resource=DeploymentCommandUtils.APPLICATION_RESOURCE_NAME, action="write")

public class PostUndeployCommand extends UndeployCommandParameters implements AdminCommand {
    
    @Inject
    private ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {
        final Logger logger = context.getLogger();
        logger.log(Level.INFO, "PostUndeployCommand starting");
      try {
        ActionReport report = context.getActionReport();

        final DeployCommandSupplementalInfo suppInfo =
                context.getActionReport().getResultType(DeployCommandSupplementalInfo.class);
        /*
         * If the user undeployed by specifying the target as "domain" then
         * the undeploy command has already explicitly sent the undeploy command
         * to the instances and has notified the after(replication) listeners.
         * In that case, the suppInfo won't have been set in the deployment
         * context.
         */
        if (suppInfo == null) {
            return;
        }
        final ExtendedDeploymentContext dc = suppInfo.deploymentContext();
        
        final InterceptorNotifier notifier = new InterceptorNotifier(habitat, dc);

        try {
            notifier.ensureAfterReported(ExtendedDeploymentContext.Phase.REPLICATION);
            logger.log(Level.INFO, "PostUndeployCommand done successfully");
        } catch (Exception e) {
            report.failure(logger, e.getMessage());
            logger.log(Level.SEVERE, "Error in inner PostUndeployCommand", e);
        }
      }
      catch (Exception e) {
          logger.log(Level.SEVERE, "Error in outer PostUndeployCommand", e);
      }
    }
}
