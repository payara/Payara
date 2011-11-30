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

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngine;
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngines;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.List;

/**
 * This command lists the set of service-provisioning engines that are configured.
 *
 * @author Sandhya Kripalani K
 */

@Service(name = "list-service-provisioning-engines")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "list-service-provisioning-engines", description = "List Service Provisioning Engines")
})
public class ListServiceProvisioningEngines implements AdminCommand {
    @Param(name = "type", optional = true)
    private String type;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;


    @Override
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();
        ActionReport.MessagePart messagePart = report.getTopMessagePart();

        String output;
        List<String> outputList = new ArrayList<String>();

        ServiceProvisioningEngines serviceProvisioningEngines = serviceUtil.getServiceProvisioningEngines();

        if (type == null) {
            for (ServiceProvisioningEngine serviceProvisioningEngine : serviceProvisioningEngines.getServiceProvisioningEngines()) {
                output = serviceProvisioningEngine.getType().toUpperCase() + "    < " + serviceProvisioningEngine.getClassName() + " >";
                if (serviceProvisioningEngine.getDefault()) {
                    output = output + "\t<default>";
                }
                outputList.add(output);
            }

        } else {
            for (ServiceProvisioningEngine serviceProvisioningEngine : serviceProvisioningEngines.getServiceProvisioningEngines()) {
                if (serviceProvisioningEngine.getType().equalsIgnoreCase(type)) {
                    output = type.toUpperCase() + "\t<" + serviceProvisioningEngine.getClassName() + ">";
                    if (serviceProvisioningEngine.getDefault()) {
                        output = output + "\t<default>";
                    }
                    outputList.add(output);
                }
            }
            if (outputList.isEmpty()) {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                report.setMessage("No service-provisioning-engine of type [" + type + "] is available.");
                return;
            }
        }

        for (String anOutputList : outputList) {
            ActionReport.MessagePart childPart = messagePart.addChild();
            childPart.setMessage(anOutputList);
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
