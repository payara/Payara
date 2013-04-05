/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.deployment.ApplicationLifecycleInterceptor;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import javax.inject.Inject;
import org.glassfish.api.admin.AccessRequired.AccessCheck;
import org.glassfish.api.admin.AdminCommandSecurity;

import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author Tim Quinn
 */
@Service
@PerLookup
public class PostStateCommand implements AdminCommand, 
        AdminCommandSecurity.Preauthorization, AdminCommandSecurity.AccessCheckProvider {
    
    @Inject
    protected ServiceLocator habitat;

    private DeployCommandSupplementalInfo suppInfo;
    private Collection<? extends AccessCheck> accessChecks;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        suppInfo = context.getActionReport().getResultType(DeployCommandSupplementalInfo.class);
        accessChecks = suppInfo.getAccessChecks();
        return true;
    }

    @Override
    public Collection<? extends AccessCheck> getAccessChecks() {
        return accessChecks;
    }
    
    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        final Logger logger = context.getLogger();
      try {
        logger.log(Level.INFO, "PostState starting: " + this.getClass().getName());
                
        final ExtendedDeploymentContext dc;
        if (suppInfo == null) {
            throw new IllegalStateException("Internal Error: suppInfo was not set. Insure that it is set properly.");
        } else {
            dc = suppInfo.deploymentContext();
        }

        if (dc == null) {
            return;
        }

        final InterceptorNotifier notifier = new InterceptorNotifier(habitat, dc);

        try {
            notifier.ensureAfterReported(ExtendedDeploymentContext.Phase.REPLICATION);
            logger.log(Level.INFO, "PostStateCommand: " + this.getClass().getName() + " finished successfully");
        } catch (Exception e) {
            report.failure(logger, e.getMessage());
            logger.log(Level.SEVERE, "Error during inner PostState: " + this.getClass().getName(), e);
        }
      } catch (Exception e) {
          logger.log(Level.SEVERE, "Error duirng outer PostState: " + this.getClass().getName(), e);
      }
    }
}
