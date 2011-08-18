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
import org.glassfish.paas.orchestrator.config.ApplicationScopedService;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.SharedService;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryEntry;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;

import static org.glassfish.paas.orchestrator.provisioning.CloudRegistryService.*;


import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.beans.PropertyVetoException;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@org.jvnet.hk2.annotations.Service
public class ServiceUtil implements PostConstruct {

    private static ExecutorService threadPool = Executors.newFixedThreadPool(1);

    @Inject
    private Domain domain;

    public static ExecutorService getThreadPool() {
        return threadPool;
    }

    public void postConstruct() {
    }

    public boolean isValidService(String serviceName, String appName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, appName, type);
        return entry != null;
    }

    public void updateInstanceID(String serviceName, String appName, final String instanceID, ServiceType type) {
        updateInstanceIDThroughConfig(serviceName, appName, instanceID);
    }

    private void updateInstanceIDThroughConfig(String serviceName, String appName, final String instanceID) {
        Service matchingService = getService(serviceName, appName);
        if(matchingService != null){
            try {
                if (ConfigSupport.apply(new SingleConfigCode<Service>() {
                    public Object run(Service serviceConfig) throws PropertyVetoException, TransactionFailure {
                        Property property = serviceConfig.getProperty("instance-id");
                        if (property != null) {
                            Transaction t = Transaction.getTransaction(serviceConfig);
                            Property p_w = t.enroll(property);
                            p_w.setValue(instanceID);

                        } else {
                            Property prop = serviceConfig.createChild(Property.class);
                            //TODO should this be changed to vm-id
                            prop.setName("instance-id");
                            prop.setValue(instanceID);
                            serviceConfig.getProperty().add(prop);
                        }
                        return serviceConfig;
                    }
                }, matchingService) == null) {
                    String msg = "Unable to update instance-id ["+instanceID+"] of service ["+serviceName+"]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        }else{
            throw new RuntimeException("Invalid service, no such service ["+serviceName+"] found");
        }
    }

    public void updateState(String serviceName, String appName, final String state, ServiceType type) {
        updateStateThroughConfig(serviceName, appName, state);
    }

    private void updateStateThroughConfig(String serviceName, String appName, final String state) {
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
        updateIPAddressThroughConfig(serviceName, appName, IPAddress);
    }

    private void updateIPAddressThroughConfig(String serviceName, String appName, final String IPAddress) {
        Service matchingService = getService(serviceName, appName);
        if(matchingService != null){
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
                    String msg = "Unable to update ip-address ["+IPAddress+"] of service ["+serviceName+"]";
                    System.out.println(msg);
                    throw new RuntimeException(msg);
                }
            } catch (TransactionFailure transactionFailure) {
                transactionFailure.printStackTrace();
                throw new RuntimeException(transactionFailure.getMessage(), transactionFailure);
            }
        }else{
            throw new RuntimeException("Invalid service, no such service ["+serviceName+"] found");
        }
    }

    private PreparedStatement prepareStatement(Connection con, final String query)
            throws SQLException {
        return con.prepareStatement(query);
    }

    public boolean isServiceAlreadyConfigured(String serviceName, String appName, ServiceType type) {
        return isServiceAlreadyConfiguredThroughConfig(serviceName, appName);
    }

    private boolean isServiceAlreadyConfiguredThroughConfig(String serviceName, String appName) {
        Service matchingService = getService(serviceName, appName);
        return matchingService != null;
    }

    public String getServiceType(String serviceName, String appName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getServerType();
        } else {
            return null;
        }
    }

    public String getServiceState(String serviceName, String appName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getState();
        } else {
            return null;
        }
    }

    public String getIPAddress(String serviceName, String appName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getIpAddress();
        } else {
            return null;
        }
    }

    public String getInstanceID(String serviceName, String appName, ServiceType type) {
        CloudRegistryEntry entry = retrieveCloudEntry(serviceName, appName, type);
        if (entry != null) {
            return entry.getInstanceId();
        } else {
            return null;
        }
    }


    public CloudRegistryEntry retrieveCloudEntry(String serviceName, String appName, ServiceType type) {
        return retrieveCloudEntryThroughConfig(serviceName, appName);
    }

    private CloudRegistryEntry retrieveCloudEntryThroughConfig(String serviceName, String appName) {
        Service matchingService = null;
        CloudRegistryEntry cre = null;
        matchingService = getService(serviceName, appName);

        if(matchingService != null){
            cre = new CloudRegistryEntry();
            cre.setCloudName(matchingService.getServiceName());
            if(matchingService.getProperty() != null){
                if(matchingService.getProperty("ip-address") != null){
                    cre.setIpAddress(matchingService.getProperty("ip-address").getValue());
                }

                if(matchingService.getProperty("instance-id") != null){
                    cre.setInstanceId(matchingService.getProperty("instance-id").getValue());
                }
            }

            //TODO need a "Stateful" service type ?
            if(matchingService instanceof ApplicationScopedService){
                cre.setState(((ApplicationScopedService)matchingService).getState());
            }else if(matchingService instanceof SharedService){
                cre.setState(((SharedService)matchingService).getState());
            }
            cre.setServerType(matchingService.getType());
        }
        return cre;
    }

    private Service getService(String serviceName, String appName) {
        Service matchingService = null;
        Services services = getServices();
        for(Service service : services.getServices()){
            if(service.getServiceName().equals(serviceName)){
                if(appName != null){
                    if(service instanceof ApplicationScopedService){
                        String applicationName = ((ApplicationScopedService)service).getApplicationName();
                        if(appName.equals(applicationName)){
                            matchingService = service;
                            break;
                        }
                    }
                }else{
                    matchingService = service;
                    break;
                }
            }
        }
        return matchingService;
    }

    public Services getServices(){
        Services services = domain.getExtensionByType(Services.class);
        if(services == null){
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


    public void registerCloudEntry(final CloudRegistryEntry entry, String tableName, String type) {
        registerCloudEntryThroughConfig(entry);
    }

    private void registerCloudEntryThroughConfig(final CloudRegistryEntry entry) {
        Services services = getServices();
        try {
            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services servicesConfig) throws PropertyVetoException, TransactionFailure {
                    ApplicationScopedService service = servicesConfig.createChild(ApplicationScopedService.class);

                    service.setServiceName(entry.getCloudName());
                    service.setType(entry.getServerType());

                    if (entry.getAppName() != null) {
                        service.setApplicationName(entry.getAppName());
                    }
                    service.setState(entry.getState());

                    {
                        Property prop = service.createChild(Property.class);
                        //TODO should this be changed to vm-id
                        prop.setName("instance-id");
                        prop.setValue(entry.getInstanceId());
                        service.getProperty().add(prop);
                    }

                    {
                        //TODO remove ip-address once vm-id is sufficient
                        Property prop = service.createChild(Property.class);
                        prop.setName("ip-address");
                        prop.setValue(entry.getIpAddress());
                        service.getProperty().add(prop);
                    }
                    servicesConfig.getServices().add(service);
                    return service;
                }
            }, services) == null) {
                String msg = "Unable to service ["+entry.getCloudName()+"]";
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
}
