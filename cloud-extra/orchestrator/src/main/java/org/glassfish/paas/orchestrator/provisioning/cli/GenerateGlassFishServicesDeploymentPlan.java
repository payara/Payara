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
import org.glassfish.paas.orchestrator.service.metadata.*;
import org.glassfish.paas.orchestrator.provisioning.util.JSONUtil;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Given the list of service-descriptions, this command generates the glassfish-services.xml file and deployment plan
 * .jar file that includes the glassfish-services.xml
 * Returns the location of generated deployment plan .jar file.
 * This internal command is used by the GUI (cloud console) and the IDE
 * plugin to get the generated deployment plan.
 * <p/>
 * This command runs on CPAS and does not acquire any lock as there is
 * no state modified by the command.
 *
 * @author Jagadish Ramu
 */

@Service(name = "_generate-glassfish-services-deployment-plan")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
        @RestEndpoint(configBean = Applications.class, opType = RestEndpoint.OpType.POST,
                path = "_generate-glassfish-services-deployment-plan",
                description = "Generate glassfish-services.xml deployment plan from the list of services")
})
public class GenerateGlassFishServicesDeploymentPlan implements AdminCommand {

    @Param(optional = false)
    private File archive;

    @Param(optional = false)
    private String modifiedServiceDesc;

    @Inject
    private ArchiveFactory archiveFactory;

    @Inject
    private ServiceOrchestrator orchestrator;


    public void execute(AdminCommandContext context) {

        List<Map<String, Object>> serviceMetadata = (List<Map<String, Object>> ) JSONUtil.jsonToJava(modifiedServiceDesc);

        ActionReport report = context.getActionReport();

        List<ServiceDescription> serviceDescriptions = generateServiceMetadata(serviceMetadata);

        if (archive == null || !archive.exists()) {
            report.setMessage("Invalid archive");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }
        ReadableArchive readableArchive = null;
        try {
            readableArchive = archiveFactory.openArchive(archive);
            ServiceMetadata metadata = orchestrator.getServices(readableArchive);
            Set<ServiceDescription> serviceDescriptionsSet = metadata.getServiceDescriptions();
            serviceDescriptionsSet.clear();
            serviceDescriptionsSet.addAll(serviceDescriptions);

            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(ServiceMetadata.class);
                Marshaller marshaller = jaxbContext.createMarshaller();
                String fileName = System.getProperty("java.io.tmpdir") + File.separator + "glassfish-services.xml";
                File xmlFile = new File(fileName);
                xmlFile.deleteOnExit(); //remove on exit
                marshaller.marshal(metadata, xmlFile);

                File jarFile = new File(fileName);
                jarFile.deleteOnExit(); //remove on exit
                String jarFilepath = generateJarFile(jarFile);

                Properties properties = new Properties();
                properties.put("deployment-plan-file-path", jarFilepath);
                report.setExtraProperties(properties);

                report.setMessage("generated deployment plan jar file [ " + jarFilepath + " ]");

            } catch (Exception ex) {
                ex.printStackTrace();
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(ex);
                report.setMessage("Failure while generating the updated glassfish-services.xml : " + ex.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
            report.setMessage("Failure while generating the updated glassfish-services.xml : " + e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }catch (Exception e) {
                e.printStackTrace();
                report.setMessage("Failure while generating the updated glassfish-services.xml : " + e.getMessage());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
        } finally {
            try {
                if (readableArchive != null) {
                    readableArchive.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                report.setMessage("Failure while generating the updated glassfish-services.xml : " + e.getMessage());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
            }
        }
    }


    private String generateJarFile(File file) throws IOException {

        String jarFileName = file.getAbsolutePath() + ".jar";
        JarOutputStream jarOutputStream = null;
        BufferedInputStream bufferedInputStream = null;
        try {
            String fileName = file.getName();
            JarEntry jarEntry = new JarEntry(fileName);
            jarEntry.setTime(file.lastModified());
            jarOutputStream = new JarOutputStream(new FileOutputStream(jarFileName));
            jarOutputStream.putNextEntry(jarEntry);
            bufferedInputStream = new BufferedInputStream(new FileInputStream(file));

            byte[] buffer = new byte[1024];
            while (true) {
                int count = bufferedInputStream.read(buffer);
                if (count == -1)
                    break;
                jarOutputStream.write(buffer, 0, count);
            }
            jarOutputStream.closeEntry();
            return jarFileName;
        } finally {
            if (bufferedInputStream != null) {
                try {
                    bufferedInputStream.close();
                } catch (Exception e) {
                }
            }
            if (jarOutputStream != null) {
                try {
                    jarOutputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private List<ServiceDescription> generateServiceMetadata(List<Map<String, Object>> serviceMetadata) {

        List<ServiceDescription> metadata = new ArrayList<ServiceDescription>();

        for (Map<String, Object> sdMap : serviceMetadata) {
            ServiceDescription sd = new ServiceDescription();
            if (sdMap.get("name") != null) {
                sd.setName((String) sdMap.get("name"));
            } else {
               throw new IllegalArgumentException("Name not available for the ServiceDesciption");
            }
            if (sdMap.get("init-type") != null) {
                sd.setInitType((String) sdMap.get("init-type"));
            }
            if (sdMap.get("service-type") != null) {
                sd.setInitType((String) sdMap.get("service-type"));
            } else {
                throw new IllegalArgumentException("Service-type not available for the ServiceDesciption");
            }
            if (sdMap.get("template-id") != null) {
                String templateID = (String) sdMap.get("template-id");
                TemplateIdentifier tid = new TemplateIdentifier();
                tid.setId(templateID);
                sd.setTemplateOrCharacteristics(tid);
            } else if (sdMap.get("characteristics") != null) {
                HashMap characteristics = (HashMap) sdMap.get("characteristics");
                Set keySet = characteristics.keySet();
                Iterator keySetIterator = keySet.iterator();
                List<Property> characteristicsList = new ArrayList<Property>();
                while (keySetIterator.hasNext()) {
                    Property property = new Property();
                    String name = (String) keySetIterator.next();
                    String value = (String) characteristics.get(name);
                    property.setName(name);
                    property.setValue(value);
                    characteristicsList.add(property);
                }

                ServiceCharacteristics serviceCharacteristics = new ServiceCharacteristics();
                serviceCharacteristics.setServiceCharacteristics(characteristicsList);
                sd.setTemplateOrCharacteristics(serviceCharacteristics);

            } else {
                throw new IllegalArgumentException("Neither template-id nor Characteristics is available for the ServiceDesciption");
            }

            if (sdMap.get("configurations") != null) {
                HashMap configuration = (HashMap) sdMap.get("configurations");
                Set keySet = configuration.keySet();
                Iterator keySetIterator = keySet.iterator();
                List<Property> configurationList = new ArrayList<Property>();
                while (keySetIterator.hasNext()) {
                    Property property = new Property();
                    String name = (String) keySetIterator.next();
                    String value = (String) configuration.get(name);
                    property.setName(name);
                    property.setValue(value);
                    configurationList.add(property);
                }
                sd.setConfigurations(configurationList);
            }

            metadata.add(sd);
        }
        return metadata;
    }
}
