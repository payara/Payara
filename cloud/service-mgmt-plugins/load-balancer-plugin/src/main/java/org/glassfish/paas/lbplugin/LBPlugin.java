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
package org.glassfish.paas.lbplugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.paas.lbplugin.cli.GlassFishLBProvisionedService;
import org.glassfish.paas.lbplugin.logger.LBPluginLogger;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.HTTPLoadBalancerServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import org.glassfish.paas.lbplugin.util.LBServiceConfiguration;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.virtualization.spi.TemplateCondition;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@org.jvnet.hk2.annotations.Service
public class LBPlugin implements ServicePlugin {

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private org.glassfish.embeddable.CommandRunner embeddedCommandRunner;

    @Inject
    private LBServiceUtil lbServiceUtil;

    @Inject
    private ApplicationFactory applicationFactory;

    @Inject
    private Habitat habitat;

    @Inject(optional = true) // made it optional for non-virtual scenario to work
    private TemplateRepository templateRepository;

    private static final String APPLICATION_DOMAIN_NAME = "application-domain-name";

    private static Logger logger = Logger.getLogger(LBPlugin.class.getName());

    public HTTPLoadBalancerServiceType getServiceType() {
        return new HTTPLoadBalancerServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, LB Plugin has no role here.
        return true;
    }

    public boolean handles(ServiceDescription serviceDescription) {
        return false;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        LBPluginLogger.getLogger().log(Level.INFO,"Given referenceType : " + referenceType + " : " + Constants.LB.equalsIgnoreCase(referenceType));
        return Constants.LB.equalsIgnoreCase(referenceType);
    }

    @Override
    public Set getServiceReferences(String appName, ReadableArchive cloudArchive, PaaSDeploymentContext dc) {
        LinkedHashSet<ServiceReference> serviceReferences = new LinkedHashSet<ServiceReference>();
        //LB will optionally associate with DNS service if present
        //Since association is bidirectional, this ref will ensure DNS service will associate with LB
        ServiceReference dnsServiceReference = 
                new ServiceReference(cloudArchive.getName(), "DNS", null);
        dnsServiceReference.setOptional(true);
        serviceReferences.add(dnsServiceReference);
        serviceReferences.add(new ServiceReference(cloudArchive.getName(), "JavaEE", null));
        return serviceReferences;
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        if (Constants.LB.equals(svcRef.getType())) {
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

    @Override
    public ProvisionedService provisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc) {
        String serviceName = serviceDescription.getName();
        LBPluginLogger.getLogger().log(Level.INFO,"Given serviceName : " + serviceName);
        logger.entering(getClass().getName(), "provisionService");
        //ArrayList<String> params;
        //String[] parameters;

        //CommandResult result;// = commandRunner.run("_list-lb-services");
        //if (!result.getOutput().contains(serviceName)) {
            //_create-lb-service
            //params = new ArrayList<String>();
            //params.add("--_ignore_appserver_association");
            //params.add("true");

        ActionReport report = habitat.getComponent(ActionReport.class);
        org.glassfish.api.admin.CommandRunner.CommandInvocation invocation = commandRunner.getCommandInvocation("_create-lb-service", report);
        ParameterMap parameterMap=new ParameterMap();

        if(serviceDescription.getAppName() != null){
            parameterMap.add("appname",serviceDescription.getAppName());
        }
        if (serviceDescription.getServiceCharacteristics() != null) {
            String serviceCharacteristics = formatArgument(serviceDescription
                    .getServiceCharacteristics().getServiceCharacteristics());
            parameterMap.add("servicecharacteristics",serviceCharacteristics);
        }else if(serviceDescription.getTemplateIdentifier() != null){
            String templateID = serviceDescription.getTemplateIdentifier().getId();
            parameterMap.add("templateid",templateID);
        }
        String serviceConfigurations =
                formatArgument(serviceDescription.getConfigurations());
        parameterMap.add("serviceconfigurations",serviceConfigurations);
        parameterMap.add("waitforcompletion","true");
        parameterMap.add("virtualcluster",serviceDescription.getVirtualClusterName());

        String domainName = System.getProperty(Constants.DOMAIN_NAME_SYSTEM_PROPERTY);
        if(domainName != null){
           parameterMap.add("domainname",domainName);
        }
        parameterMap.add("DEFAULT", serviceName);
        invocation.parameters(parameterMap).execute();

        if(report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)){
            LBPluginLogger.getLogger().log(Level.INFO,"_create-lb-service [" + serviceName + "] failed");
        }

        Properties serviceProperties = report.getExtraProperties();

        parameterMap = new ParameterMap();
        if (serviceDescription.getAppName() != null) {
            parameterMap.add("appname", serviceDescription.getAppName());
        }
        parameterMap.add("virtualcluster", serviceDescription.getVirtualClusterName());
        parameterMap.add("_vmid", serviceProperties.getProperty("vm-id"));
        parameterMap.add("DEFAULT", serviceName);

        report = habitat.getComponent(ActionReport.class);
        invocation = commandRunner.getCommandInvocation("_start-lb-service", report);
        invocation.parameters(parameterMap).execute();

        if(report.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)){
            LBPluginLogger.getLogger().log(Level.WARNING,"_start-lb-service [" + serviceName + "] failed", report.getFailureCause());
        }

        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, serviceProperties);
        ps.setStatus(ServiceStatus.RUNNING);
        return ps;
    }

    @Override
    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        //ServiceInfo entry = lbServiceUtil.retrieveCloudEntry(serviceDescription.getName(), serviceDescription.getAppName(), ServiceType.LB);
        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, new Properties());
        ps.setStatus(lbServiceUtil.getServiceStatus(serviceInfo));
        return (ProvisionedService) ps;
    }
   
    @Override
    public void associateServices(Service serviceConsumer, ServiceReference svcRef,
                                  Service serviceProvider, boolean beforeDeployment, PaaSDeploymentContext dc) {
        if(beforeDeployment){
            return;
        }

        if (!("JavaEE".equals(svcRef.getType())
                && serviceConsumer.getServiceType().toString().equals("LB")
                && serviceProvider.getServiceType().toString().equals("JavaEE"))){
            return;
        }
        
        String appDomainName = dc.getTransientAppMetaData
                (APPLICATION_DOMAIN_NAME, String.class);
        callAssociateService(serviceConsumer, serviceProvider, false, dc.getAppName(), appDomainName);
        
    }

    private void callAssociateService(Service serviceConsumer,
            Service serviceProvider, boolean isReconfig, String appName, String appDomainName) {
        ServiceDescription serviceDescription = serviceConsumer.getServiceDescription();
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "provisionService");
        ArrayList<String> params;
        String[] parameters;
        params = new ArrayList<String>();
        params.add("--appname");
        params.add(appName);
        params.add("--reconfig="+isReconfig);
        params.add("--clustername");
        params.add(serviceProvider.getServiceDescription().getName());
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        if (appDomainName != null) {
            params.add("--domainname");
            params.add(appDomainName);
        }
        ServiceScope scope = serviceDescription.getServiceScope();
        if(scope != null && scope.equals(ServiceScope.SHARED)){
            if (lbServiceUtil.getApplicationsUsingSharedService(
                    serviceName).size() > 1){
                params.add("--first=false");
            }
        }
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);
        CommandResult result = embeddedCommandRunner.run("_associate-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO, "_associate-lb-service [" + serviceName + "] failed");
        }
    }

   /**
     * {@inheritDoc}
     */
    public boolean deploy(PaaSDeploymentContext dc, Service service){
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean undeploy(PaaSDeploymentContext dc, Service service){
        return true;
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

        CommandResult result = embeddedCommandRunner.run("_start-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_start-lb-service [" + serviceName + "] failed");
        }

        GlassFishLBProvisionedService ps = new GlassFishLBProvisionedService(serviceDescription, new Properties());
        ps.setStatus(ServiceStatus.RUNNING);
        return ps;
    }

    public boolean stopService(ProvisionedService ps, ServiceInfo serviceInfo) {
        ServiceDescription serviceDescription = ps.getServiceDescription();
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

        CommandResult result = embeddedCommandRunner.run("_stop-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_stop-lb-service [" + serviceName + "] failed");
        }

        ps.setStatus(ServiceStatus.STOPPED);
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

    public boolean unprovisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc){
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

        CommandResult result = embeddedCommandRunner.run("_stop-lb-service", parameters);
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

        result = embeddedCommandRunner.run("_delete-lb-service", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            LBPluginLogger.getLogger().log(Level.INFO,"_delete-lb-service [" + serviceName + "] failed");
            return false;
        }
        return true;
    }

    public void dissociateServices(Service serviceConsumer, ServiceReference svcRef,
                                   Service serviceProvider, boolean beforeUndeploy, PaaSDeploymentContext dc){
        if(!beforeUndeploy){
            return;
        }

        if (!(svcRef.getType().equals("JavaEE")
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
        params.add("--appname");
        params.add(dc.getAppName());
        params.add("--clustername");
        params.add(serviceProvider.getServiceDescription().getName());
        params.add("--virtualcluster");
        params.add(serviceDescription.getVirtualClusterName());
        ServiceScope scope = serviceDescription.getServiceScope();
        if(scope != null && scope.equals(ServiceScope.SHARED)){
            if (lbServiceUtil.getApplicationsUsingSharedService(
                    serviceName).size() > 1){
                params.add("--last=false");
            }
        }
        params.add(serviceName);
        parameters = new String[params.size()];
        parameters = params.toArray(parameters);
        CommandResult result = embeddedCommandRunner.run("_dissociate-lb-service", parameters);
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
    public ProvisionedService scaleService(ProvisionedService provisionedService,
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
    public boolean reassociateServices(Service serviceConsumer,
            Service oldServiceProvider,
            Service newServiceProvider,
            ServiceOrchestrator.ReconfigAction reason) {
        callAssociateService(serviceConsumer, newServiceProvider, true, null, null);
        return true;
    }
    
}
