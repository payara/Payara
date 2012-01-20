/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.mq;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

import com.sun.enterprise.deployment.*;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.javaee.core.deployment.JavaEEDeploymentUtils;
import org.glassfish.deployment.common.Descriptor;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.javaee.core.deployment.DolProvider;
import org.glassfish.paas.gfplugin.GlassFishProvisionedService;
import org.glassfish.paas.mq.logger.MQServicePluginLogger;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.MQServiceType;
import org.glassfish.paas.orchestrator.service.metadata.*;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceProvisioningException;
import org.glassfish.paas.spe.common.BasicProvisionedService;
import org.glassfish.paas.spe.common.ServiceProvisioningEngineBase;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import org.glassfish.paas.orchestrator.service.spi.Service;

import static org.glassfish.paas.mq.Constants.*;

/**
 * @author Jagadish Ramu
 */
@Scoped(PerLookup.class)
@org.jvnet.hk2.annotations.Service
public class MQServicePlugin extends ServiceProvisioningEngineBase<MQServiceType> {

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private Habitat habitat;

    @Inject
    private ApplicationFactory applicationFactory;

    @Inject
    private DolProvider dolProvider;


    public MQServiceType getServiceType() {
        return new MQServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return true;
    }

    public boolean handles(ServiceDescription serviceDescription) {
        return false;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        return serviceReferenceTypes.contains(referenceType);
    }

    public Set getServiceReferences(String appName, ReadableArchive cloudArchive, PaaSDeploymentContext dc) {
        Set<ServiceReference> serviceReferences = new LinkedHashSet<ServiceReference>();
        serviceReferences.add(new ServiceReference(cloudArchive.getName(), JAVAEE_SERVICE_REFERENCE, null));
        serviceReferences.addAll(discoverServiceReferences(cloudArchive, appName));
        return serviceReferences;
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        List<Property> properties = new ArrayList<Property>();
        properties.add(new Property("service-type", "MQ"));
        ServiceCharacteristics serviceCharacteristics = new ServiceCharacteristics(properties);

        List<Property> configurations = new ArrayList<Property>();

        return new ServiceDescription("default-mq-service", appName, null, serviceCharacteristics,
                configurations);
    }

    private Set<ServiceReference> discoverServiceReferences(ReadableArchive cloudArchive, String appName) {
        Set<ServiceReference> serviceReferences = new HashSet<ServiceReference>();

        Application application = null;
        try {
            application = dolProvider.processDeploymentMetaData(cloudArchive);
        } catch (Exception ex) {
            MQServicePluginLogger.getLogger().log(Level.INFO, "exception", ex);
        }

        if (!JavaEEDeploymentUtils.isJavaEE(cloudArchive, habitat)) {
            return serviceReferences;
        }

        if (application != null) {
            Set<BundleDescriptor> bundleDescriptors = application.getBundleDescriptors();
            for (BundleDescriptor descriptor : bundleDescriptors) {
                populateResourceRefsAsServiceReferences(descriptor, serviceReferences);

                if (descriptor instanceof EjbBundleDescriptor) {
                    EjbBundleDescriptor ejbDesc = (EjbBundleDescriptor) descriptor;
                    Set<EjbDescriptor> ejbDescriptors = ejbDesc.getEjbs();
                    for (EjbDescriptor ejbDescriptor : ejbDescriptors) {
                        populateResourceRefsAsServiceReferences(ejbDescriptor, serviceReferences);
                    }
                    //ejb interceptors
                    Set<EjbInterceptor> ejbInterceptors = ejbDesc.getInterceptors();
                    for (EjbInterceptor ejbInterceptor : ejbInterceptors) {
                        populateResourceRefsAsServiceReferences(ejbInterceptor, serviceReferences);
                    }
                }
                // managed bean descriptors
                Set<ManagedBeanDescriptor> managedBeanDescriptors = descriptor.getManagedBeans();
                for (ManagedBeanDescriptor mbd : managedBeanDescriptors) {
                    populateResourceRefsAsServiceReferences(mbd, serviceReferences);
                }
            }
        }
        return serviceReferences;
    }

    public Set<ServiceDescription> getImplicitServiceDescriptions(ReadableArchive cloudArchive, String appName) {
        HashSet<ServiceDescription> implicitServiceDescriptions = new HashSet<ServiceDescription>();

        if (!JavaEEDeploymentUtils.isJavaEE(cloudArchive, habitat)) {
            return implicitServiceDescriptions;
        }
        Set<ServiceReference> serviceReferences = discoverServiceReferences(cloudArchive, appName);

        boolean hasJMSReference = false;
        for (ServiceReference serviceReference : serviceReferences) {
            if (serviceReferenceTypes.contains(serviceReference.getType())) {
                hasJMSReference = true;
                break;
            }
        }
        if (hasJMSReference) {
            implicitServiceDescriptions.add(getDefaultServiceDescription(appName, null));
        }
        return implicitServiceDescriptions;
    }

    private boolean isDuplicate(ServiceReference serviceReference, Set<ServiceReference> serviceReferences){
        for(ServiceReference sr : serviceReferences){
            if(sr.getName().equals(serviceReference.getName()) && sr.getType().equals(serviceReference.getType())){
                return true;
            }
        }
        return false;
    }

    private void populateResourceRefsAsServiceReferences(Descriptor descriptor, Set<ServiceReference> serviceReferences) {
        if (descriptor instanceof JndiNameEnvironment) {
            JndiNameEnvironment jndiEnv = (JndiNameEnvironment) descriptor;

            // resource-ref
            for (Object resourceRef : jndiEnv.getResourceReferenceDescriptors()) {
                ResourceReferenceDescriptor resRefDesc = (ResourceReferenceDescriptor) resourceRef;
                if (serviceReferenceTypes.contains(resRefDesc.getType())) {
                    ServiceReference serviceRef = new ServiceReference(resRefDesc.getJndiName(), resRefDesc.getType(), null);
                    if(!isDuplicate(serviceRef, serviceReferences)){
                        serviceReferences.add(serviceRef);
                    }
                }
            }

            // resource-env-ref
            for (Object jmsDestRef : jndiEnv.getJmsDestinationReferenceDescriptors()) {
                JmsDestinationReferenceDescriptor jmsDestRefDesc = (JmsDestinationReferenceDescriptor) jmsDestRef;
                if (serviceReferenceTypes.contains(jmsDestRefDesc.getRefType())) {
                    ServiceReference serviceRef = new ServiceReference(jmsDestRefDesc.getJndiName(), jmsDestRefDesc.getRefType(), null);
                    if(!isDuplicate(serviceRef, serviceReferences)){
                        serviceReferences.add(serviceRef);
                    }
                }
            }

            for (Object jmsDestRef : jndiEnv.getMessageDestinationReferenceDescriptors()) {
                MessageDestinationReferenceDescriptor msgDestnRefDesc = (MessageDestinationReferenceDescriptor) jmsDestRef;
                if (serviceReferenceTypes.contains(msgDestnRefDesc.getDestinationType())) {
                    ServiceReference serviceRef = new ServiceReference(msgDestnRefDesc.getJndiName(), msgDestnRefDesc.getDestinationType(), null);
                    if(!isDuplicate(serviceRef, serviceReferences)){
                        serviceReferences.add(serviceRef);
                    }
                }
            }
        }
    }

    public void executeCommand(VirtualMachine virtualMachine, String ... args){

        List<String> commandArgs = Arrays.asList(args);

        try {
            String output = virtualMachine.executeOn(args);
            MQServicePluginLogger.getLogger().log(Level.FINEST, ("Command [" + commandArgs.toString() + "] output : " + output));
        } catch (Exception e) {
            MQServicePluginLogger.getLogger().log(Level.WARNING, "Unable to execute command ["+commandArgs.toString()+"]", e);
        }
    }


    public void startMQ(final VirtualMachine virtualMachine){
        if (virtualMachine.getMachine() == null) {
            return;
        }
        final String fileName = "~/mq.plugin.broker.password.txt";
        //we are creating the file in the remote machine. Assumption being its a linux machine.
        executeCommand(virtualMachine, "echo \"imq.imqcmd.password=admin\" > " + fileName);

        try {

        Thread myThread = new Thread(){
            public void run(){
                try{
                    String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
                    String[] args = {installDir + File.separator + "mq" +
                            File.separator + "bin" + File.separator + "imqbrokerd", "-passfile", fileName,
                            "-port", Constants.MQ_PORT, "-force", "-name", Constants.MQ_BROKER_NAME};
                    executeCommand(virtualMachine, args);
                }catch(Exception e){
                    e.printStackTrace();
                }finally{
                    executeCommand(virtualMachine, "rm " + fileName);
                }
            }
        };
            myThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopMQ(VirtualMachine virtualMachine){

        if (virtualMachine.getMachine() == null) {
            return;
        }
        String fileName = "~/mq.plugin.broker.password.txt";
        executeCommand(virtualMachine, "echo \"imq.imqcmd.password=admin\" > " + fileName);

        String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
        String[] args = {installDir + File.separator + "mq" +
                File.separator + "bin" + File.separator + "imqcmd", "shutdown","bkr", "-u", "admin", "-f", "-passfile", fileName,
                "-b", "localhost:"+Constants.MQ_PORT};
        executeCommand(virtualMachine, args);
        executeCommand(virtualMachine, "rm " + fileName);
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc) {
        ProvisionedService provisionedService = createService(serviceDescription).get();

        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        startMQ(vm);
        String ipAddress = properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
        properties.putAll(getServiceProperties(ipAddress, provisionedService.getName()));
        return provisionedService;
    }

    protected Properties getServiceProperties(String ipAddress, String serviceName) {
        Properties serviceProperties = new Properties();
        serviceProperties.put("user", "admin");
        serviceProperties.put("password", "admin");
        serviceProperties.put("host", ipAddress);
        serviceProperties.put("port", MQ_PORT);
        serviceProperties.put("imqDestinationName", serviceName);

        return serviceProperties;
    }


    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription) {
        try {
            String serviceName = serviceDescription.getName();
            String appName = serviceDescription.getAppName();
            ServiceInfo serviceInfo = serviceUtil.getServiceInfo(
                    serviceName, appName, null);
            Properties properties = new Properties();
            properties.putAll(serviceInfo.getProperties());
            properties.putAll(getServiceProperties(
                    properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS), serviceName));

            return new BasicProvisionedService(serviceDescription, properties,
                    serviceUtil.getServiceStatus(serviceInfo));
        } catch (Exception ex) {
            throw new ServiceProvisioningException(ex);
        }
    }


    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        return getProvisionedService(serviceDescription);
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc) {
        Properties properties = getProvisionedService(serviceDescription).getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        String ipAddress = properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
        stopMQ(vm);
        return deleteService(serviceDescription);
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        return null;
    }

    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        ProvisionedService provisionedService = startService(serviceDescription);

        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        startMQ(vm);
        properties.putAll(getServiceProperties(
                properties.getProperty(VIRTUAL_MACHINE_IP_ADDRESS), serviceInfo.getServiceName()));
        return provisionedService;
    }

    public boolean stopService(ProvisionedService provisionedService, ServiceInfo serviceInfo) {
        ServiceDescription serviceDescription = provisionedService.getServiceDescription();
        Properties properties = provisionedService.getProperties();
        VirtualMachine vm = getVmByID(serviceDescription.getVirtualClusterName(),
                properties.getProperty(VIRTUAL_MACHINE_ID));
        stopMQ(vm);
        return stopService(serviceDescription);
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        throw new UnsupportedOperationException("Status check of MQ Service " +
                "not supported in this release");
    }

    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public ProvisionedService scaleService(ProvisionedService provisionedService,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        //no-op
        throw new UnsupportedOperationException("Scaling of MQ Service " +
                "not supported in this release");
    }

    public boolean reconfigureServices(ProvisionedService oldPS,
                                       ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of MQ Service " +
                "not supported in this release");
    }

    public void dissociateServices(Service serviceConsumer, ServiceReference svcRef,
                                   Service serviceProvider, boolean beforeUndeploy, PaaSDeploymentContext dc) {
        if (beforeUndeploy) {
            return;
        }

        if ((serviceReferenceTypes.contains(svcRef.getType()))
                && serviceConsumer.getServiceType().toString().equals(MQ_SERVICE_TYPE)
                && serviceProvider.getServiceType().toString().equals(JAVAEE_SERVICE_TYPE)) {

                CommandRunner commandRunner = getCommandRunner(serviceProvider);
            String target = serviceProvider.getServiceDescription().getName();

            if (svcRef.getType().equals(QUEUE) ||
                    svcRef.getType().equals(TOPIC) ||
                    svcRef.getType().equals(QCF) ||
                    svcRef.getType().equals(TCF)) {
                ArrayList<String> params = new ArrayList<String>();
                params.add("--target=" + target);
                params.add(svcRef.getName()); //resource-name

                String[] parameters = new String[params.size()];
                parameters = params.toArray(parameters);

                deleteResource(commandRunner, parameters);

                resetJMSService(commandRunner, serviceConsumer, serviceProvider);
            }
        }
    }

    public void associateServices(Service serviceConsumer, ServiceReference svcRef,
                                  Service serviceProvider, boolean beforeDeployment, PaaSDeploymentContext dc) {

        if (!beforeDeployment) {
            return;
        }

        if ((serviceReferenceTypes.contains(svcRef.getType()))
                && serviceConsumer.getServiceType().toString().equals(MQ_SERVICE_TYPE)
                && serviceProvider.getServiceType().toString().equals(JAVAEE_SERVICE_TYPE)) {


            CommandRunner commandRunner = getCommandRunner(serviceProvider);
            String target = serviceProvider.getServiceDescription().getName();

            configureJMSService(commandRunner, serviceConsumer, serviceProvider);

            if (svcRef.getType().equals(QUEUE) ||
                    svcRef.getType().equals(TOPIC)) {
                ArrayList<String> params = new ArrayList<String>();
                params.add("--restype=" + svcRef.getType());
                params.add("--target=" + target);
                String destinationName = svcRef.getName().replaceAll("/","_");
                params.add("--property=" + "imqDestinationName=" + destinationName + ":" + "Name=" + destinationName);
                params.add(svcRef.getName()); //resource-name

                String[] parameters = new String[params.size()];
                parameters = params.toArray(parameters);

                createResource(commandRunner, parameters);
            } else if (svcRef.getType().equals(QCF) ||
                    svcRef.getType().equals(TCF)) {
                ArrayList<String> params = new ArrayList<String>();
                params.add("--restype=" + svcRef.getType());
                params.add("--target=" + target);
                params.add(svcRef.getName()); //resource-name

                String[] parameters = new String[params.size()];
                parameters = params.toArray(parameters);

                createResource(commandRunner, parameters);
            }
        }
    }

    private void resetJMSService(CommandRunner commandRunner, Service serviceConsumer, Service serviceProvider) {

        String clusterName = serviceProvider.getName();

        boolean hasNoJmsResources = hasNoJmsResources(commandRunner, clusterName);

        if(hasNoJmsResources){
            String serviceTypeSetCommand = "configs.config."+clusterName+"-config.jms-service.type=EMBEDDED";
            String defaultJMSHost = "configs.config."+clusterName+"-config.jms-service.jms-host.default_JMS_host.host="+"localhost";
            //${JMS_PROVIDER_PORT} cannot be set due to constraint violation in JMS Host (not allowing the property is a bug)
            String defaultJMSPort = "configs.config."+clusterName+"-config.jms-service.jms-host.default_JMS_host.port=" + "'${JMS_PROVIDER_PORT}'";

            executeSetCommand(commandRunner, serviceTypeSetCommand);
            executeSetCommand(commandRunner, defaultJMSHost);
            executeSetCommand(commandRunner, defaultJMSPort);
        }
    }

    private boolean hasNoJmsResources(CommandRunner commandRunner, String clusterName) {
        CommandResult result = commandRunner.run("list-jms-resources",clusterName);
        MQServicePluginLogger.getLogger().log(Level.FINEST, "list-jms-resources output : " + result.getOutput());

        boolean noJmsResourcesFound = false;
        if(result.getOutput().contains("Nothing to list")){
            noJmsResourcesFound = true;
        }
        return noJmsResourcesFound;
    }


    private void configureJMSService(CommandRunner commandRunner, Service serviceConsumer, Service serviceProvider) {

        String clusterName = serviceProvider.getName();

        CommandResult result = commandRunner.run("list-jms-resources",clusterName);
        MQServicePluginLogger.getLogger().log(Level.FINEST, "list-jms-resources output : " + result.getOutput());

        boolean hasNoJmsResources = hasNoJmsResources(commandRunner, clusterName);

        if(hasNoJmsResources){
            String ipAddress = serviceConsumer.getProperties().getProperty(VIRTUAL_MACHINE_IP_ADDRESS);
            String serviceTypeSetCommand = "configs.config."+clusterName+"-config.jms-service.type=REMOTE";
            String defaultJMSHost = "configs.config."+clusterName+"-config.jms-service.jms-host.default_JMS_host.host="+ipAddress;
            String defaultJMSPort = "configs.config."+clusterName+"-config.jms-service.jms-host.default_JMS_host.port=" + Constants.MQ_PORT;

            executeSetCommand(commandRunner, serviceTypeSetCommand);
            executeSetCommand(commandRunner, defaultJMSHost);
            executeSetCommand(commandRunner, defaultJMSPort);
        }
    }

    private void executeSetCommand(CommandRunner commandRunner, String command) {
        CommandResult result = commandRunner.run("set", command);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            MQServicePluginLogger.getLogger().log(Level.WARNING, "failed to execute command [ set "+command+" ] : "
                    + result.getOutput(), result.getFailureCause());
            throw new RuntimeException(result.getFailureCause());
        }
    }

    private CommandRunner getCommandRunner(Service serviceProvider) {
        CommandRunner commandRunner = null;
        try {
            if (serviceProvider instanceof GlassFishProvisionedService) {
                GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceProvider;
                GlassFish gf = gfps.getProvisionedGlassFish();
                commandRunner = gf.getCommandRunner();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return commandRunner;
    }

    private void createResource(CommandRunner commandRunner, String[] parameters) {
        CommandResult result = commandRunner.run("create-jms-resource", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            MQServicePluginLogger.getLogger().log(Level.WARNING, "failed to create jms resource : "
                    + result.getOutput(), result.getFailureCause());
            throw new RuntimeException(result.getFailureCause());
        }
    }

    private void deleteResource(CommandRunner commandRunner, String[] parameters) {
        CommandResult result = commandRunner.run("delete-jms-resource", parameters);
        if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
            MQServicePluginLogger.getLogger().log(Level.WARNING, "failed to delete jms resource : "
                    + result.getOutput(), result.getFailureCause());
            throw new RuntimeException(result.getFailureCause());
        }
    }

    public boolean reassociateServices(Service serviceConsumer,
                                       Service oldServiceProvider,
                                       Service newServiceProvider,
                                       ServiceOrchestrator.ReconfigAction reason) {
        return true;
    }
}
