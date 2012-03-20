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
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.component.Habitat;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Returns the service metadata associated with an application archive.
 * This internal command is used by the GUI (cloud console) and the IDE 
 * plugin to get the service-dependencies and detailed <code>ServiceDescription</code>s 
 * associated that the OE and SPEs have discovered for the 
 * user provided application archive.
 * 
 * This command runs on CPAS and does not acquire any lock as there is
 * no state modified by the command. 
 * 
 * @author Jagadish Ramu
 */
@Service(name = "_get-service-metadata")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value={CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Applications.class, opType= RestEndpoint.OpType.GET, path="_get-service-metadata", description="Get Service Metadata")
})
public class GetServiceMetadata implements AdminCommand {

    @Param(primary = true)
    private File archive ;

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServiceOrchestrator orchestrator;

    @Inject 
    private Habitat habitat;

    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        if(archive == null || !archive.exists()){
            report.setMessage("Invalid archive");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        ReadableArchive readableArchive = null;
        try {
            readableArchive = archiveFactory.openArchive(archive);
            ServiceMetadata metadata = orchestrator.getServices(readableArchive);
            List<Map<String,Object>> serviceMetadataList = new ArrayList<Map<String, Object>>();
            for(ServiceDescription desc : metadata.getServiceDescriptions()){
                Map<String, Object> descMap = getServiceDescriptionMap(desc);
                serviceMetadataList.add(descMap);
            }

            Properties extraProperties = new Properties();
            extraProperties.put("list", serviceMetadataList);
            report.setExtraProperties(extraProperties);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (IOException e) {
            report.setMessage("Failure while reading the archive");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        } catch (Exception e) {
            report.setMessage("Failure while getting service-metadata");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }finally{
            try{
                if(readableArchive != null){
                    readableArchive.close();
                }
            }catch(Exception e){}
        }
    }

    private Map<String, Object> getServiceDescriptionMap(ServiceDescription desc) {

            /*
            * List<Map>  -> each Map corresponds to a ServiceDescription.
            * Content of Map will be of the form :
            { init-type = "lazy", name="myservice", template-id = "my-template", service-type="JavaEE",
              service-characteristics = { name=value, name=value, name=value},
            configuration = {name = value, name=value, name=value}
            }
             */

        Map<String, Object> serviceDescription = new TreeMap<String, Object>();
        serviceDescription.put("init-type", desc.getInitType());
        serviceDescription.put("name", desc.getName());
        serviceDescription.put("service-type", desc.getServiceType());
        if(desc.getTemplateIdentifier() != null){
            serviceDescription.put("template-id", desc.getTemplateIdentifier().getId());
        }else{
            Map<String, String> serviceCharacteristicsMap = new TreeMap<String, String>();
            if(desc.getServiceCharacteristics() != null){
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
