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
package org.glassfish.paas.lbplugin;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.lbplugin.cli.GlassFishLBProvisionedService;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.service.HTTPLoadBalancerServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@Service
public class LBPlugin implements Plugin<HTTPLoadBalancerServiceType> {

    @Inject
    private ProvisionerUtil registryService;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private LBServiceUtil lbServiceUtil;

    @Inject
    private ApplicationFactory applicationFactory;

    private static final String LB = "HTTP_LOAD_BALANCER";

    public static final String GLASSFISH_LB = "GLASSFISH_LB";
    public static final String LB_ServiceType = "LB";

    public HTTPLoadBalancerServiceType getServiceType() {
        return new HTTPLoadBalancerServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, LB Plugin has no role here.
        return true;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        return LB.equalsIgnoreCase(referenceType);
    }

    public Set<ServiceReference> getServiceReferences(ReadableArchive cloudArchive) {
        //LB plugin does not scan anything for prototype
        return new HashSet<ServiceReference>();
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        if (LB.equals(svcRef.getServiceRefType())) {
            LBProvisioner lbProvisioner = registryService.getLBProvisioner(GLASSFISH_LB);

            // create default service description.
            String defaultServiceName = lbProvisioner.getDefaultServiceName();
            List<Property> properties = new ArrayList<Property>();
            properties.add(new Property("service-type", LB_ServiceType));
            properties.add(new Property("os-name", System.getProperty("os.name"))); // default OS will be same as that of what Orchestrator is running on.
            ServiceDescription sd = new ServiceDescription(defaultServiceName, appName,
                    "lazy", new ServiceCharacteristics(properties), null);

            // Fill the required details in service reference.
            Properties svcRefProps = lbProvisioner.getDefaultConnectionProperties();
            svcRefProps.setProperty("serviceName", defaultServiceName);
            svcRef.setProperties(svcRefProps);

            return sd;
        } else {
            return null;
        }
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
        String serviceName = serviceDescription.getName();

        ArrayList<String> params;
        String[] parameters;

        CommandResult result = commandRunner.run("_list-lb-services");
        if (!result.getOutput().contains(serviceName)) {
            //_create-lb-service
            params = new ArrayList<String>();
            params.add("--_ignore_appserver_association");
            params.add("true");
            if(serviceDescription.getAppName() != null){
                params.add("appname="+serviceDescription.getAppName());
            }
            params.add(serviceName);
            parameters = new String[params.size()];
            parameters = params.toArray(parameters);

            result = commandRunner.run("_create-lb-service", parameters);
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                System.out.println("_create-lb-service [" + serviceName + "] failed");
            }
        }

        ServiceInfo entry = lbServiceUtil.retrieveCloudEntry(serviceName, serviceDescription.getAppName(), ServiceType.LOAD_BALANCER);
        if (entry == null) {
            throw new RuntimeException("unable to get LB service : " + serviceName);
        }

        params = new ArrayList<String>();
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        result = commandRunner.run("_start-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            System.out.println("_start-lb-service [" + serviceName + "] failed");
        }

        return new GlassFishLBProvisionedService(serviceDescription, new Properties(), ServiceStatus.STARTED);
    }

    public void associateServices(ProvisionedService provisionedSvc, ServiceReference svcRef, boolean beforeDeployment,
                                  DeploymentContext dc) {
        //no-op
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.STARTED);
    }

    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public boolean reconfigureServices(ProvisionedService oldPS, ProvisionedService newPS) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public Set<ServiceDescription> getImplicitServiceDescriptions(ReadableArchive cloudArchive, String appName) {
        //no-op. Just by looking at a orchestration archive
        //the LB plugin cannot say that an LB needs to be provisioned.
        HashSet<ServiceDescription> defs = new HashSet<ServiceDescription>();

        Application application = null;
        try {
            application = applicationFactory.openArchive(cloudArchive.getURI());
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if(application != null ) {
            boolean isDistributable = false;
            for (WebBundleDescriptor descriptor : application.getBundleDescriptors(
                    WebBundleDescriptor.class)) {
                if (descriptor.isDistributable()) {
                    isDistributable = true;
                    break;
                }
            }
            if(isDistributable) {
                List<Property> properties = new ArrayList<Property>();
                properties.add(new Property("service-type", LB_ServiceType));
                // TODO :: check if the cloudArchive.getName() is okay.
                ServiceDescription sd = new ServiceDescription(
                        cloudArchive.getName(), appName, "lazy",
                        new ServiceCharacteristics(properties), null);
                defs.add(sd);
            }
        }
        return defs;
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc){
        //TODO impl.
        return true;
    }

    public void dissociateServices(ProvisionedService provisionedSvc,
                                  ServiceReference svcRef, boolean beforeUndeploy, DeploymentContext dc){
        //no-op
    }
}
