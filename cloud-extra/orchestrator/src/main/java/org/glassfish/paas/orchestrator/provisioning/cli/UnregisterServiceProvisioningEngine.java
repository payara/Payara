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

package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngine;
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngines;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;


/**
 * This command unregisters a service-provisioning engine.
 *
 * @author  Sandhya Kripalani K
 */

@Service(name = "unregister-service-provisioning-engine")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
    @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "unregister-service-provisioning-engine", description = "Unregister Service Provisioning Engine")
})
public class UnregisterServiceProvisioningEngine implements AdminCommand{

    @Param(name = "classname",primary = true)
    private String classname;


    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;


    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport report=context.getActionReport();

        ServiceProvisioningEngines serviceProvisioningEngines=serviceUtil.getServiceProvisioningEngines();
        boolean found=false;
        for(final ServiceProvisioningEngine serviceProvisioningEngine: serviceProvisioningEngines.getServiceProvisioningEngines()){
            if(serviceProvisioningEngine.getClassName().equals(classname)){
                 found=true;
                 try {
                            if (ConfigSupport.apply(new SingleConfigCode<ServiceProvisioningEngines>() {
                                public Object run(ServiceProvisioningEngines param) throws PropertyVetoException, TransactionFailure {
                                    param.getServiceProvisioningEngines().remove(serviceProvisioningEngine);
                                    report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                                    report.setMessage("Deleted service-provisioning-engine [" + classname + "]");
                                    return serviceProvisioningEngine;
                                }
                            }, serviceProvisioningEngines) == null) {
                                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                                    //report.setMessage("Deleting service-provisioning engine [" + classname + "] failed ");
                                    report.setFailureCause(new RuntimeException("Deleting service-provisioning engine [" + classname + "] failed"));
                                    return;
                            }
                 } catch (TransactionFailure transactionFailure) {
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    //report.setMessage("Deleting service-provisioning-engine [" + classname + "] failed due to: " + transactionFailure.getMessage());
                    report.setFailureCause(transactionFailure);
                    return;
                 }
            }

        }
        if(!found){
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(new RuntimeException("No service-provisioning-engine by name [" + classname + "] is available"));
                return;
        }

    }
}
