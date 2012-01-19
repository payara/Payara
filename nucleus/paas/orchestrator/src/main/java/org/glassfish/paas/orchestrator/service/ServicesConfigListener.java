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

package org.glassfish.paas.orchestrator.service;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.internal.api.PostStartup;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.*;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.paas.orchestrator.service.spi.ConfiguredService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PostConstruct;
import org.jvnet.hk2.component.PreDestroy;
import org.jvnet.hk2.component.Singleton;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@Scoped(Singleton.class)
@org.jvnet.hk2.annotations.Service
public class ServicesConfigListener implements ConfigListener, PostConstruct, PreDestroy, PostStartup {

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private ServiceOrchestratorImpl serviceOrchestrator;

    @Inject
    private CommandRunner commandRunner;

    private static final Logger logger =
            LogDomains.getLogger(ServiceOrchestratorImpl.class,LogDomains.PAAS_LOGGER);


    /**
     * Notification that @Configured objects that were injected have changed
     *
     * @param events list of changes
     */
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] events) {
        return ConfigSupport.sortAndDispatch(events, new PropertyChangeHandler(events), logger);
    }

    public void postConstruct() {
        addListenerToServices();
    }

    private void addListenerToServices() {
        Services services = serviceUtil.getServices();
        if (services != null) {
            ObservableBean bean = (ObservableBean) ConfigSupport.getImpl(services);
            bean.addListener(this);
        } else {
            logger.log(Level.WARNING,"unable.to.register.listenersto.servicesconfig");
        }
    }

    public void preDestroy() {

    }

    class PropertyChangeHandler implements Changed {

        PropertyChangeEvent[] events;

        private PropertyChangeHandler(PropertyChangeEvent[] events) {
            this.events = events;
        }

        public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> changedType, T changedInstance) {
            NotProcessed np = null;
            Object args[]= new Object[]{changedType.getName(),changedInstance};
            switch (type) {
                case ADD:
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,"added.instance",args);
                    }
                    np = handleAddEvent(changedInstance);
                    break;

                case CHANGE:
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,"changed.instance",args);
                    }
                    np = handleChangeEvent(changedInstance);
                    break;

                case REMOVE:
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE,"removed.instance",args);
                    }
                    np = handleRemoveEvent(changedInstance);
                    break;

                default:
                    np = new NotProcessed("Unrecognized type of change: " + type);
                    break;
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleAddEvent(T instance) {
            NotProcessed np = null;
            if (instance instanceof SharedService) {
                SharedService service = (SharedService) instance;
                //System.out.println("shared service [" + service.getServiceName() + "] added");
                logger.log(Level.INFO,"shared.service.added",service.getServiceName());


                //construct service-description.
                ServiceDescription sd = new ServiceDescription();
                sd.setName(service.getServiceName());

                if (service.getConfigurations() != null && service.getConfigurations().getConfiguration() != null) {
                    List<Property> configurationList = new ArrayList<Property>();
                    for (Configuration config : service.getConfigurations().getConfiguration()) {
                        Property property = new Property();
                        property.setName(config.getName());
                        property.setValue(config.getValue());
                        configurationList.add(property);
                    }
                    sd.setConfigurations(configurationList);

                }

                if (service.getTemplate() != null) {
                    TemplateIdentifier tid = new TemplateIdentifier();
                    tid.setId(service.getTemplate());
                    sd.setTemplateOrCharacteristics(tid);
                }

                if (service.getCharacteristics() != null && service.getCharacteristics().getCharacteristic() != null) {
                    List<Property> characteristicsList = new ArrayList<Property>();
                    for (Characteristic characteristic : service.getCharacteristics().getCharacteristic()) {
                        Property property = new Property();
                        property.setName(characteristic.getName());
                        property.setValue(characteristic.getValue());
                        characteristicsList.add(property);
                    }
                    ServiceCharacteristics serviceCharacteristics = new ServiceCharacteristics(characteristicsList);
                    sd.setTemplateOrCharacteristics(serviceCharacteristics);
                }

                //create virtual cluster for the shared-service.
                //TODO we need to see the impact of virtual-cluster for service-names
                //TODO as service-names are unique only per scope whereas virtual-cluster
                //TODO may not be.


                // create one virtual cluster per deployment unit.
                String virtualClusterName = service.getServiceName();
                CommandResult result = commandRunner.run("create-cluster", virtualClusterName);
                Object args[]=new Object[]{virtualClusterName,result.getOutput()};
                logger.log(Level.INFO,"create.cluster.exec.output",args);
                if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                    throw new RuntimeException("Failure while provisioning services, " +
                            "Unable to create cluster [" + virtualClusterName + "]");
                }
                //set the virtual-cluster name in the service-description.
                sd.setVirtualClusterName(virtualClusterName);
                ServicePlugin defaultPlugin = serviceOrchestrator.getPlugin(sd);
                sd.setPlugin(defaultPlugin);
                sd.setServiceScope(ServiceScope.SHARED);
                PaaSDeploymentContext pdc = new PaaSDeploymentContext(null, null);
                ProvisionedService ps = defaultPlugin.provisionService(sd, pdc);
                serviceUtil.registerService(null, ps);
                //serviceOrchestrator.addSharedService(sd.getName(), ps);
            }else if (instance instanceof ExternalService){
                ExternalService externalService = (ExternalService)instance;
                ConfiguredService configuredService = serviceUtil.getExternalService(externalService.getServiceName());
                serviceOrchestrator.addExternalService(externalService.getServiceName(), configuredService);
            }
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleRemoveEvent(final T instance) {
            NotProcessed np = null;
            return np;
        }

        private <T extends ConfigBeanProxy> NotProcessed handleChangeEvent(T instance) {
            NotProcessed np = null;
            return np;
        }
    }
}
