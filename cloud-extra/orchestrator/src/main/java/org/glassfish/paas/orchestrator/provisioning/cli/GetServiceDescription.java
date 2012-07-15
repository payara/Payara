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

import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Utility CLI to get the service-description of a particular service in an application
 * @author ishan
 */
@Service(name = "_get-service-description")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.GET, path = "_get-service-description",
            description = "Get Service Description for a particular service of an application")
})
public class GetServiceDescription implements AdminCommand {

    @Param(name = "appname", optional = false)
    private String appName;

    @Param(name = "servicename", primary = true)
    private String service;

    @Inject
    private ServiceOrchestrator orchestrator;

    @Inject
    private Domain domain;

    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        if (domain.getApplications().getApplication(appName) == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("No such application [" + appName + "] is deployed");
            return;
        }

        ServiceDescription desc = orchestrator.getServiceDescription(appName, service);

        if (desc == null) {
            report.setMessage("Service by name [" + service + " ] does not exist for the given application [" + appName + " ]");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        
        Map<String, Object> descMap = getServiceDescriptionMap(desc);

        Properties extraProperties = new Properties();
        extraProperties.put("list", descMap);
        report.setExtraProperties(extraProperties);
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

    private Map<String, Object> getServiceDescriptionMap(ServiceDescription desc) {

        Map<String, Object> serviceDescription = new TreeMap<String, Object>();
        serviceDescription.put("init-type", desc.getInitType());
        serviceDescription.put("name", desc.getName());
        serviceDescription.put("service-type", desc.getServiceType());
        if (desc.getTemplateIdentifier() != null) {
            serviceDescription.put("template-id", desc.getTemplateIdentifier().getId());
        } else {
            Map<String, String> serviceCharacteristicsMap = new TreeMap<String, String>();
            if (desc.getServiceCharacteristics() != null) {
                ServiceCharacteristics characteristics = desc.getServiceCharacteristics();
                for (Property characteristic : characteristics.getServiceCharacteristics()) {
                    serviceCharacteristicsMap.put(characteristic.getName(), characteristic.getValue());
                }
            }
            serviceDescription.put("characteristics", serviceCharacteristicsMap);
        }

        Map<String, String> configuration = new TreeMap<String, String>();
        List<Property> configs = desc.getConfigurations();
        if (configs != null) {
            for (Property property : configs) {
                configuration.put(property.getName(), property.getValue());
            }
        }
        serviceDescription.put("configurations", configuration);

        return serviceDescription;
    }
}
