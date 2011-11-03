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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.lbplugin.cli.GlassFishLBProvisionedService;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.ServiceOrchestrator.ReconfigAction;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.service.HTTPLoadBalancerServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import org.glassfish.paas.lbplugin.util.LBServiceConfiguration;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.spi.TemplateCondition;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@Service
public class LBPlugin implements Plugin {

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private LBServiceUtil lbServiceUtil;

    @Inject
    private ApplicationFactory applicationFactory;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    private static Logger logger = Logger.getLogger(LBPlugin.class.getName());

    public HTTPLoadBalancerServiceType getServiceType() {
        return new HTTPLoadBalancerServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, LB Plugin has no role here.
        return true;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        LBPluginLogger.getLogger().log(Level.INFO,"Given referenceType : " + referenceType + " : " + Constants.LB.equalsIgnoreCase(referenceType));
        return Constants.LB.equalsIgnoreCase(referenceType);
    }

    @Override
    public Set getServiceReferences(String appName, ReadableArchive cloudArchive) {
        HashSet<ServiceReference> serviceReferences = new HashSet<ServiceReference>();
        serviceReferences.add(new ServiceReference(cloudArchive.getName(), "JavaEE", null));
        return serviceReferences;
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        if (Constants.LB.equals(svcRef.getServiceRefType())) {
            TemplateInstance template = getLBTemplate();
            if(template == null){
                throw new RuntimeException("No LB template exists.");
            }
            // create default service description.
            String defaultServiceName = getDefaultServiceName(appName);
            TemplateIdentifier identifier = new TemplateIdentifier();
            identifier.setId(template.getConfig().getName());
            //List<Property> properties = getDefaultServiceProperties(template);
            List<Property> configurations = getDefaultServiceConfigurations(template);
            ServiceDescription sd = new ServiceDescription(defaultServiceName, appName,
                    "lazy", identifier, configurations);

            // Fill the required details in service reference.
            Properties svcRefProps = new Properties();//lbProvisioner.getDefaultConnectionProperties();
            svcRefProps.setProperty("serviceName", defaultServiceName);
            svcRef.setProperties(svcRefProps);

            return sd;
        } else {
            return null;
        }
    }

    private TemplateInstance getLBTemplate(){
        TemplateCondition condition =
                new org.glassfish.virtualization.util.ServiceType(
                Constants.ServiceTypeLB);
        for (TemplateInstance ti : templateRepository.all()) {
            if(ti.satisfies(condition)){
                return ti;
            }
        }
        return null;
    }

    private List<Property> getDefaultServiceProperties(TemplateInstance template) {
        List<Property> properties = new ArrayList<Property>();
        properties.add(new Property(Constants.SERVICE_TYPE_PROP_NAME,
                Constants.ServiceTypeLB));
        return properties;
    }

    private List<Property> getDefaultServiceConfigurations(TemplateInstance template) {
        return LBServiceConfiguration.getDefaultLBServiceConfigurations();
    }

    private String getDefaultServiceName(String appName){
        return appName + "-lb";
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
        String serviceName = serviceDescription.getName();
        LBPluginLogger.getLogger().log(Level.INFO,"Given serviceName : " + serviceName);
        logger.entering(getClass().getName(), "provisionService");
        ArrayList<String> params;
        String[] parameters;

        CommandResult result;// = commandRunner.run("_list-lb-services");
        //if (!result.getOutput().contains(serviceName)) {
            //_create-lb-service
            params = new ArrayList<String>();
            //params.add("--_ignore_appserver_association");
            //params.add("true");
            if(serviceDescription.getAppName() != null){
                params.add("--appname");
                params.add(serviceDescription.getAppName());
            }
            if (serviceDescription.getServiceCharacteristics() != null) {
                String serviceCharacteristics = formatArgument(serviceDescription
                        .getServiceCharacteristics().getServiceCharacteristics());
                params.add("--servicecharacteristics=" + serviceCharacteristics);
            }else if(serviceDescription.getTemplateIdentifier() != null){
                String templateID = serviceDescription.getTemplateIdentifier().getId();
                params.add("--templateid=" + templateID);
            }
            String serviceConfigurations =
                    formatArgument(serviceDescription.getConfigurations());
            params.add("--serviceconfigurations");
            params.add(serviceConfigurations);
            params.add("--waitforcompletion=true");
            params.add("--virtualcluster");
            params.add(serviceDescription.getVirtualClusterName());
            params.add(serviceName);
            parameters = new String[params.size()];
            parameters = params.toArray(parameters);

            result = commandRunner.run("_create-lb-service", parameters);
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                LBPluginLogger.getLogger().log(Level.INFO,"_create-lb-service [" + serviceName + "] failed");
            }
        //}

        ServiceInfo entry = lbServiceUtil.retrieveCloudEntry(serviceName, serviceDescription.getAppName(), ServiceType.LOAD_BALANCER);
        if (entry == null) {
            throw new RuntimeException("unable to get LB service : " + serviceName);
        }

        
        params = new ArrayList<String>();
        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);

        result = commandRunner.run("_start-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_start-lb-service [" + serviceName + "] failed");
        }

        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, new Properties());
        ps.setStatus(ServiceStatus.STARTED);
        return ps;
    }

    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        ServiceInfo entry = lbServiceUtil.retrieveCloudEntry(serviceDescription.getName(), serviceDescription.getAppName(), ServiceType.LOAD_BALANCER);
        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, new Properties());
        ps.setStatus(lbServiceUtil.getServiceStatus(entry));
        return ps;
    }

    public void associateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                  ProvisionedService serviceProvider, boolean beforeDeployment, DeploymentContext dc) {
        if(beforeDeployment){
            return;
        }

        if (!(svcRef.getServiceRefType().equals("JavaEE")
                && serviceConsumer.getServiceType().toString().equals("LB")
                && serviceProvider.getServiceType().toString().equals("JavaEE"))){
            return;
        }
        callAssociateService(serviceConsumer, serviceProvider, false);
    }

    private void callAssociateService(ProvisionedService serviceConsumer,
            ProvisionedService serviceProvider, boolean isReconfig) {
        ServiceDescription serviceDescription = serviceConsumer.getServiceDescription();
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "provisionService");
        ArrayList<String> params;
        String[] parameters;
        params = new ArrayList<String>();
        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--reconfig="+isReconfig);
        params.add("--clustername");
        params.add(serviceProvider.getServiceDescription().getName());
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);
        CommandResult result = commandRunner.run("_associate-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "_associate-lb-service [" + serviceName + "] failed");
        }
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        ArrayList params = new ArrayList<String>();
        
        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--startvm=true");
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        String[] parameters = new String[params.size()];
        parameters = (String[]) params.toArray(parameters);

        CommandResult result = commandRunner.run("_start-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_start-lb-service [" + serviceName + "] failed");
        }

        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, new Properties());
        ps.setStatus(ServiceStatus.STARTED);
        return ps;
    }

    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        ArrayList params = new ArrayList<String>();

        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--stopvm=true");
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        String[] parameters = new String[params.size()];
        parameters = (String[]) params.toArray(parameters);

        CommandResult result = commandRunner.run("_stop-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_stop-lb-service [" + serviceName + "] failed");
        }

        return true;
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.STARTED);
    }

    public ProvisionedService match(ServiceReference svcRef) {
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
            LBPluginLogger.getLogger().log(Level.INFO,"exception",ex);
        }

        if(application != null ) {
            boolean isWebApp = (application.getBundleDescriptors(
                    WebBundleDescriptor.class).size() > 0);
            if(isWebApp) {
                TemplateInstance template = getLBTemplate();
                if(template == null){
                    LBPluginLogger.getLogger().log(Level.SEVERE,
                            "No LB template exists, so LB service cannot be provisioned.");
                    return defs;
                }
                //List<Property> properties = getDefaultServiceProperties(template);
                List<Property> configurations = getDefaultServiceConfigurations(template);
                TemplateIdentifier identifier = new TemplateIdentifier();
                identifier.setId(template.getConfig().getName());
                // TODO :: check if the cloudArchive.getName() is okay.
                ServiceDescription sd = new ServiceDescription(
                        getDefaultServiceName(cloudArchive.getName()), appName, "lazy",
                        identifier, configurations);
                defs.add(sd);
            }
        }
        return defs;
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc){
        String serviceName = serviceDescription.getName();
        ArrayList params = new ArrayList<String>();

        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        String[] parameters = new String[params.size()];
        parameters = (String[]) params.toArray(parameters);

        CommandResult result = commandRunner.run("_stop-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_stop-lb-service [" + serviceName + "] failed");
        }

        params.clear();
        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = (String[]) params.toArray(parameters);

        result = commandRunner.run("_delete-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_delete-lb-service [" + serviceName + "] failed");
            return false;
        }
        return true;
    }

    public void dissociateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                   ProvisionedService serviceProvider, boolean beforeUndeploy, DeploymentContext dc){
        if(!beforeUndeploy){
            return;
        }

        if (!(svcRef.getServiceRefType().equals("JavaEE")
                && serviceConsumer.getServiceType().toString().equals("LB")
                && serviceProvider.getServiceType().toString().equals("JavaEE"))){
            return;
        }

        ServiceDescription serviceDescription = serviceConsumer.getServiceDescription();
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "provisionService");
        ArrayList<String> params;
        String[] parameters;
        params = new ArrayList<String>();
        if (serviceDescription.getAppName() != null) {
            params.add("--appname");
            params.add(serviceDescription.getAppName());
        }
        params.add("--clustername");
        params.add(serviceProvider.getServiceDescription().getName());
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);
        CommandResult result = commandRunner.run("_dissociate-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "_dissociate-lb-service [" + serviceName + "] failed");
        }
    }

    // TODO :: move this utility method to plugin-common module.
    private String formatArgument(List<Property> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (Property p : properties) {
                sb.append(p.getName() + "=" + p.getValue() + ":");
            }
        }
        // remove the last ':'
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public ProvisionedService scaleService(ServiceDescription serviceDesc,
            int scaleCount, AllocationStrategy allocStrategy) {
        //no-op
        throw new UnsupportedOperationException("Scaling of LB Service " +
                "not supported in this release");
    }
    
    @Override
    public boolean reconfigureServices(ProvisionedService oldPS,
            ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of Service " +
                "not supported in this release");
    }

    @Override
    public boolean reassociateServices(ProvisionedService serviceConsumer,
            ProvisionedService oldServiceProvider,
            ProvisionedService newServiceProvider,
            ServiceOrchestrator.ReconfigAction reason) {
        callAssociateService(serviceConsumer, newServiceProvider, true);
        return true;
    }
    
}
