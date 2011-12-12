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
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.*;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


@org.jvnet.hk2.annotations.Service
public class ServiceUtil implements PostConstruct {

    private static ExecutorService threadPool = Executors.newCachedThreadPool();

    @Inject
    private Domain domain;

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    public void postConstruct() {
    }

    public boolean isValidService(String serviceName, String appName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        return entry != null;
    }

    // set a general property for application-scoped-service config element.
    public void setProperty(String serviceName, String appName,
                            final String propName, final String propValue) {
        Service matchingService = getService(serviceName, appName);
        if (matchingService != null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Service>() {
                    public Object run(Service serviceConfig) throws PropertyVetoException, TransactionFailure {
                        Property property = serviceConfig.getProperty(propName);
                        if (property != null) {
                            Transaction t = Transaction.getTransaction(serviceConfig);
                            Property p_w = t.enroll(property);
                            p_w.setValue(propValue);

                        } else {
                            Property prop = serviceConfig.createChild(Property.class);
                            prop.setName(propName);
                            prop.setValue(propValue);
                            serviceConfig.getProperty().add(prop);
                        }
                        return serviceConfig;
                    }
                }, matchingService) == null) {
                    String msg = "Unable to update property [" + propName + "] of service [" + serviceName + "]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        } else {
            throw new RuntimeException("Invalid service, no such service [" + serviceName + "] found");
        }
    }

    // set a general property for application-scoped-service config element.
    public void removeProperty(String serviceName, String appName,
                               final String propName) {
        Service matchingService = getService(serviceName, appName);
        if (matchingService != null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Service>() {
                    public Object run(Service serviceConfig) throws PropertyVetoException, TransactionFailure {
                        Property property = serviceConfig.getProperty(propName);
                        if (property != null) {
                            serviceConfig.getProperty().remove(property);
                        }
                        return serviceConfig;
                    }
                }, matchingService) == null) {
                    String msg = "Unable to remove property [" + propName + "] of service [" + serviceName + "]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        } else {
            throw new RuntimeException("Invalid service, no such service [" + serviceName + "] found");
        }
    }

    public void updateVMID(String serviceName, String appName, final String vmId, ServiceType type) {
        Service matchingService = getService(serviceName, appName);
        if (matchingService != null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Service>() {
                    public Object run(Service serviceConfig) throws PropertyVetoException, TransactionFailure {
                        Property property = serviceConfig.getProperty("vm-id");
                        if (property != null) {
                            Transaction t = Transaction.getTransaction(serviceConfig);
                            Property p_w = t.enroll(property);
                            p_w.setValue(vmId);

                        } else {
                            Property prop = serviceConfig.createChild(Property.class);
                            //TODO should this be changed to vm-id
                            prop.setName("vm-id");
                            prop.setValue(vmId);
                            serviceConfig.getProperty().add(prop);
                        }
                        return serviceConfig;
                    }
                }, matchingService) == null) {
                    String msg = "Unable to update vm-id [" + vmId + "] of service [" + serviceName + "]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        } else {
            throw new RuntimeException("Invalid service, no such service [" + serviceName + "] found");
        }
    }

    public void updateState(String serviceName, String appName, final String state, ServiceType type) {
        Service matchingService = getService(serviceName, appName);
        if (matchingService != null) {
            if (matchingService instanceof ApplicationScopedService) {
                ApplicationScopedService appScopedService = (ApplicationScopedService) matchingService;
                try {
                    if (ConfigSupport.apply(new SingleConfigCode<ApplicationScopedService>() {
                        public Object run(ApplicationScopedService serviceConfig) throws PropertyVetoException, TransactionFailure {
                            serviceConfig.setState(state);
                            return serviceConfig;
                        }
                    }, appScopedService) == null) {
                        String msg = "Unable to update state [" + state + "] of service [" + serviceName + "]";
                        System.out.println(msg);
                        throw new RuntimeException(msg);
                    }
                } catch (TransactionFailure transactionFailure) {
                    transactionFailure.printStackTrace();
                    throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
                }
            }

            if (matchingService instanceof SharedService) {
                SharedService sharedService = (SharedService) matchingService;
                try {
                    if (ConfigSupport.apply(new SingleConfigCode<SharedService>() {
                        public Object run(SharedService serviceConfig) throws PropertyVetoException, TransactionFailure {
                            serviceConfig.setState(state);
                            return serviceConfig;
                        }
                    }, sharedService) == null) {
                        String msg = "Unable to update state [" + state + "] of service [" + serviceName + "]";
                        System.out.println(msg);
                        throw new RuntimeException(msg);
                    }
                } catch (TransactionFailure transactionFailure) {
                    transactionFailure.printStackTrace();
                    throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
                }
            }

        } else {
            throw new RuntimeException("Invalid service, no such service [" + serviceName + "] found");
        }
    }

    public void updateIPAddress(String serviceName, String appName, final String IPAddress, ServiceType type) {
        Service matchingService = getService(serviceName, appName);
        if (matchingService != null) {
            try {

                if (ConfigSupport.apply(new SingleConfigCode<Service>() {
                    public Object run(Service serviceConfig) throws PropertyVetoException, TransactionFailure {
                        Property property = serviceConfig.getProperty("ip-address");
                        if (property != null) {
                            Transaction t = Transaction.getTransaction(serviceConfig);
                            Property p_w = t.enroll(property);
                            p_w.setValue(IPAddress);
                        } else {
                            Property prop = serviceConfig.createChild(Property.class);
                            //TODO should this be changed to vm-id
                            prop.setName("ip-address");
                            prop.setValue(IPAddress);
                            serviceConfig.getProperty().add(prop);
                        }
                        return serviceConfig;
                    }
                }, matchingService) == null) {
                    String msg = "Unable to update ip-address [" + IPAddress + "] of service [" + serviceName + "]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        } else {
            throw new RuntimeException("Invalid service, no such service [" + serviceName + "] found");
        }
    }

    public boolean isServiceAlreadyConfigured(String serviceName, String appName, ServiceType type) {
        Service matchingService = getService(serviceName, appName);
        return matchingService != null;
    }

    public String getServiceType(String serviceName, String appName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getServerType();
        } else {
            return null;
        }
    }

    public String getServiceState(String serviceName, String appName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getState();
        } else {
            return null;
        }
    }

    public String getIPAddress(String serviceName, String appName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getIpAddress();
        } else {
            return null;
        }
    }

    public String getInstanceID(String serviceName, String appName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getInstanceId();
        } else {
            return null;
        }
    }

    public String getProperty(String serviceName, String appName, String propertyName, ServiceType type) {
        ServiceInfo entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getProperties().get(propertyName);
        } else {
            return null;
        }
    }


    public ServiceInfo retrieveCloudEntry(String serviceName, String appName, ServiceType type) {
        Service matchingService = null;
        ServiceInfo cre = null;
        matchingService = getService(serviceName, appName);

        if (matchingService != null) {
            cre = new ServiceInfo();
            cre.setServiceName(matchingService.getServiceName());
            if (matchingService.getProperty() != null) {
                if (matchingService.getProperty("ip-address") != null) {
                    cre.setIpAddress(matchingService.getProperty("ip-address").getValue());
                }

                if (matchingService.getProperty("vm-id") != null) {
                    cre.setInstanceId(matchingService.getProperty("vm-id").getValue());
                }

                List<Property> properties = matchingService.getProperty();
                for (Property property : properties) {
                    cre.getProperties().put(property.getName(), property.getValue());
                }
            }

            //TODO need a "Stateful" service type ?
            if (matchingService instanceof ApplicationScopedService) {
                cre.setState(((ApplicationScopedService) matchingService).getState());
            } else if (matchingService instanceof SharedService) {
                cre.setState(((SharedService) matchingService).getState());
            }
            cre.setServerType(matchingService.getType());
        }
        return cre;
    }

    private Service getService(String serviceName, String appName) {
        Service matchingService = null;
        Services services = getServices();
        for (Service service : services.getServices()) {
            if (service.getServiceName().equals(serviceName)) {
                if (appName != null) {
                    if (service instanceof ApplicationScopedService) {
                        String applicationName = ((ApplicationScopedService) service).getApplicationName();
                        if (appName.equals(applicationName)) {
                            matchingService = service;
                            break;
                        }
                    }
                } else {
                    matchingService = service;
                    break;
                }
            }
        }
        return matchingService;
    }

    public Services getServices() {
        Services services = domain.getExtensionByType(Services.class);
        if (services == null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    public Object run(Domain param) throws PropertyVetoException, TransactionFailure {
                        Services services = param.createChild(Services.class);
                        param.getExtensions().add(services);
                        return services;
                    }
                }, domain) == null) {
                    System.out.println("Unable to create 'services' config");
                }
            } catch (TransactionFailure transactionFailure) {
                System.out.println("Unable to create 'services' config due to : " + transactionFailure.getMessage());
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        }

        services = domain.getExtensionByType(Services.class);
        return services;
    }


    public void unregisterCloudEntry(final String serviceName, final String appName) {
        Services services = getServices();
        try {
            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services servicesConfig) throws PropertyVetoException, TransactionFailure {
                    Service deletedService = null;
                    for (Service service : servicesConfig.getServices()) {
                        if (serviceName.equals(service.getServiceName())) {
                            if (service instanceof ApplicationScopedService) {
                                ApplicationScopedService appScopedService = (ApplicationScopedService) service;
                                if (appScopedService.getApplicationName().equals(appName)) {
                                    servicesConfig.getServices().remove(appScopedService);
                                    deletedService = appScopedService;
                                    break;
                                }
                            }else{
                                //shared or external service
                                servicesConfig.getServices().remove(service);
                                deletedService = service;
                                break;
                            }
                        }
                    }
                    return deletedService;
                }
            }, services) == null) {
                String msg = "Unable to remove service [" + serviceName + "]";
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
        } catch (TransactionFailure transactionFailure) {
            transactionFailure.printStackTrace();
            throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
        }
    }


    public void registerCloudEntry(final ServiceInfo entry) {
        Services services = getServices();
        try {
            //TODO for now, if app-name is null, check whether its a shared service and
            //TODO update instead of registering the entry.
            if (entry.getAppName() == null) {
                boolean serviceFound = false;
                for (Service service : services.getServices()) {
                    if (service.getServiceName().equals(entry.getServiceName())) {
                        serviceFound = true;
                        break;
                    }
                }

                if (serviceFound) {
                    ConfigSupport.apply(new SingleConfigCode<Services>() {
                        Service matchingService = null;
                        public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                            for (Service service : param.getServices()) {
                                if (service.getServiceName().equals(entry.getServiceName())) {
                                    if (service instanceof SharedService) {
                                        SharedService sharedService = (SharedService) service;
                                        Transaction t = Transaction.getTransaction(param);
                                        SharedService t_service = t.enroll(sharedService);
                                        t_service.setState(entry.getState());
                                        matchingService = service;
                                        break;
                                    }
                                }
                            }
                            return matchingService;
                        }
                    }, services);

                    Map<String, String> properties = entry.getProperties();
                    if (properties != null) {
                        for (Map.Entry<String, String> property : properties.entrySet()) {
                            setProperty(entry.getServiceName(), null, property.getKey(), property.getValue());
                        }
                    }
                } else {
                    throw new RuntimeException("unable to find a shared or" +
                            " external service by name [" + entry.getServiceName() + "]");
                }
            } else if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services servicesConfig) throws PropertyVetoException, TransactionFailure {
                    ApplicationScopedService service = servicesConfig.createChild(ApplicationScopedService.class);

                    service.setServiceName(entry.getServiceName());
                    service.setType(entry.getServerType());

                    if (entry.getAppName() != null) {
                        service.setApplicationName(entry.getAppName());
                    }
                    service.setState(entry.getState());

                    Map<String, String> properties = entry.getProperties();
                    if (properties != null) {
                        for (Map.Entry<String, String> entry : properties.entrySet()) {
                            Property prop = service.createChild(Property.class);
                            prop.setName(entry.getKey());
                            prop.setValue(entry.getValue());
                            service.getProperty().add(prop);
                        }
                    }

                    servicesConfig.getServices().add(service);
                    return service;
                }
            }, services) == null) {
                String msg = "Unable to service [" + entry.getServiceName() + "]";
                System.out.println(msg);
                throw new RuntimeException(msg);
            }
        } catch (TransactionFailure transactionFailure) {
            transactionFailure.printStackTrace();
            throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
        }
    }

    private void debug(String message) {
        System.out.println("[ServiceUtil] " + message);
    }

    public ServiceStatus getServiceStatus(ServiceInfo entry) {
        if (entry.getState() != null && entry.getState().equals(ServiceInfo.State.Running.toString())) {
            return ServiceStatus.RUNNING;
        }
        if (entry.getState() != null && entry.getState().equals(ServiceInfo.State.Start_in_progress.toString())) {
            return ServiceStatus.STARTING;
        }
        if (entry.getState() != null && entry.getState().equals(ServiceInfo.State.Stop_in_progress.toString())) {
            return ServiceStatus.STOPPED;
        }
        if (entry.getState() != null && entry.getState().equals(ServiceInfo.State.NotRunning.toString())) {
            return ServiceStatus.STOPPED;
        }
        //TODO handle delete in progress/create in progress later.

        return ServiceStatus.UNKNOWN;
    }

    public ServiceProvisioningEngines getServiceProvisioningEngines() {
        ServiceProvisioningEngines spes = domain.getExtensionByType(ServiceProvisioningEngines.class);
        if (spes == null) {
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    public Object run(Domain param) throws PropertyVetoException, TransactionFailure {
                        ServiceProvisioningEngines spes = param.createChild(ServiceProvisioningEngines.class);
                        param.getExtensions().add(spes);
                        return spes;
                    }
                }, domain) == null) {
                    logger.log(Level.SEVERE, "Unable to create 'service-provisioning-engines' config");
                }
            } catch (TransactionFailure transactionFailure) {
                logger.log(Level.SEVERE, "Unable to create 'service-provisioning-engines' config", transactionFailure);
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        }

        spes = domain.getExtensionByType(ServiceProvisioningEngines.class);
        return spes;
    }

    //TODO implementation for external and app-scoped-service also.
    public ServiceDescription getSharedServiceDescription(ServiceInfo serviceInfo){
        ServiceDescription sd = null;
        Service service = getService(serviceInfo.getServiceName(), serviceInfo.getAppName());
        if(service != null){
            if(service instanceof SharedService){
                SharedService sharedService = (SharedService)service;

                Object characteristicsOrTemplate = null;
                Characteristics characteristics = sharedService.getCharacteristics();
                if(characteristics != null){
                    if(characteristics.getCharacteristic() != null){
                        List<org.glassfish.paas.orchestrator.service.metadata.Property> serviceCharacteristicsList
                                = new ArrayList<org.glassfish.paas.orchestrator.service.metadata.Property>();
                        for(Characteristic characteristic : characteristics.getCharacteristic()){
                            org.glassfish.paas.orchestrator.service.metadata.Property property =
                                    new org.glassfish.paas.orchestrator.service.metadata.Property(characteristic.getName(), characteristic.getValue());
                            serviceCharacteristicsList.add(property);
                        }
                        ServiceCharacteristics serviceCharacteristics = new ServiceCharacteristics();
                        serviceCharacteristics.setServiceCharacteristics(serviceCharacteristicsList);
                        characteristicsOrTemplate = serviceCharacteristics;
                    }
                }else if(sharedService.getTemplate() != null){
                    TemplateIdentifier tid = new TemplateIdentifier();
                    tid.setId(sharedService.getTemplate());
                    characteristicsOrTemplate = tid;
                }

                List<org.glassfish.paas.orchestrator.service.metadata.Property> configuration = null;
                if(sharedService.getConfigurations() != null){
                    if(sharedService.getConfigurations().getConfiguration() != null){
                        configuration = new ArrayList<org.glassfish.paas.orchestrator.service.metadata.Property>();
                        for(Configuration config : sharedService.getConfigurations().getConfiguration()){
                            org.glassfish.paas.orchestrator.service.metadata.Property property =
                                    new org.glassfish.paas.orchestrator.service.metadata.Property(config.getName(), config.getValue());
                            configuration.add(property);
                        }
                    }
                }

                sd = new ServiceDescription(sharedService.getServiceName(), null,
                        null, characteristicsOrTemplate, configuration);
                sd.setVirtualClusterName(serviceInfo.getServiceName());
            }
        }else{
            throw new RuntimeException("No such service ["+serviceInfo.getServiceName()+"] is available");
        }
        return sd;
    }

}
