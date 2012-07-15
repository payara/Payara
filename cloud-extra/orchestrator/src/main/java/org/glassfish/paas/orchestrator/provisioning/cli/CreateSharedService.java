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

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.*;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.TemplateIdentifier;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualization;
import org.glassfish.virtualization.config.Virtualizations;
import javax.inject.Inject;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@org.jvnet.hk2.annotations.Service(name = "create-shared-service")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "create-shared-service", description = "Create a shared service")
})
public class CreateSharedService implements AdminCommand {

    @Param(name = "defaultService", defaultValue = "false", optional = true)
    private Boolean defaultService;

    @Param(name = "force", defaultValue = "false", optional = true)
    private Boolean force;

    @Param(name = "servicetype", optional = false)
    private String serviceType;

    @Param(name = "properties", optional = true, separator = ':')
    private Properties properties;

    @Param(name = "characteristics", optional = true, separator = ':')
    private Properties characteristics;

    @Param(name = "configuration", optional = true, separator = ':')
    private Properties configuration;

    @Param(name = "initmode", optional = true, acceptableValues = "eager,lazy", defaultValue = "eager")
    private String initMode;

    @Param(name = "template", optional = true)
    private String template;

    @Param(name = "servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    private boolean templateFound = false;

    @Inject
    private SharedServiceLazyInitializer sharedServiceLazyInitializer;

    private static Logger logger = LogDomains.getLogger(ServiceOrchestratorImpl.class, LogDomains.PAAS_LOGGER);

    @Inject
    private ServiceOrchestratorImpl serviceOrchestrator;

    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        if (template != null && characteristics != null) {
            report.setMessage("Provide either template or characteristics and not both while creating a shared service");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //Check if the template provided by the user exists
        if (template != null) {
            Virtualizations virtualizations = domain.getExtensionByType(Virtualizations.class);
            if (virtualizations != null) {
                for (Virtualization virtualization : virtualizations.getVirtualizations()) {
                    for (Template tmplt : virtualization.getTemplates()) {
                        if (template.equalsIgnoreCase(tmplt.getName())) {
                            templateFound = true;
                            break;
                        }
                    }
                }
                if (!templateFound) {
                    report.setMessage("A template named [ " + template + " ] does not exist.");
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    return;
                }
            }
        }

        //check whether any service by this name exist
        //For now, service-name is unique across all scopes (shared/external/app-scoped)
        for (Service service : serviceUtil.getServices().getServices()) {
            if (service.getServiceName().equals(serviceName)) {
                report.setMessage("Service by name [" + serviceName + "] already exist");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }

        if (defaultService && !force) {
            Services services = domain.getExtensionByType(Services.class);
            if (services != null) {
                for (org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
                    if (service instanceof SharedService) {
                        if (((SharedService) service).getDefault() && service.getType().equalsIgnoreCase(serviceType)) {
                            report.setMessage("A shared service by name [" + service.getServiceName() + "] is already marked as default service, " +
                                    " for service-type [" + serviceType + "] use --force=true to override the same");
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            return;
                        }
                    }
                }
            }
        }

        createConfig(report);

        if ("eager".equalsIgnoreCase(initMode)) {
            //TODO interact with Orchestrator to see whether this particular service-configuration can
            //TODO be really supported.

            ServiceDescription sd = new ServiceDescription();
            sd.setName(serviceName);

            List<org.glassfish.paas.orchestrator.service.metadata.Property> configurationList =
                    new ArrayList<org.glassfish.paas.orchestrator.service.metadata.Property>();
            if (configuration != null) {
                for (String name : configuration.stringPropertyNames()) {
                    org.glassfish.paas.orchestrator.service.metadata.Property property =
                            new org.glassfish.paas.orchestrator.service.metadata.Property();
                    property.setName(name);
                    property.setValue((String) configuration.get(name));
                    configurationList.add(property);
                }
                sd.setConfigurations(configurationList);
            }

            if (template != null) {
                TemplateIdentifier tid = new TemplateIdentifier();
                tid.setId(template);
                sd.setTemplateOrCharacteristics(tid);
            }

            if (characteristics != null) {
                List<org.glassfish.paas.orchestrator.service.metadata.Property> characteristicsList =
                        new ArrayList<org.glassfish.paas.orchestrator.service.metadata.Property>();
                for (String name : characteristics.stringPropertyNames()) {
                    org.glassfish.paas.orchestrator.service.metadata.Property property =
                            new org.glassfish.paas.orchestrator.service.metadata.Property();
                    property.setName(name);
                    property.setValue((String) characteristics.get(name));
                    characteristicsList.add(property);
                }
                ServiceCharacteristics serviceCharacteristics = new ServiceCharacteristics(characteristicsList);
                sd.setTemplateOrCharacteristics(serviceCharacteristics);
            }

            sharedServiceLazyInitializer.provisionService(sd, report);
        }
    }

    private void createConfig(ActionReport report) {
        Services services = serviceUtil.getServices();
        try {
            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                    SharedService sharedService = param.createChild(SharedService.class);
                    sharedService.setDefault(defaultService);
                    sharedService.setType(serviceType);
                    sharedService.setTemplate(template);
                    sharedService.setInitMode(initMode);
                    sharedService.setServiceName(serviceName);
                    sharedService.setState(ServiceStatus.UNINITIALIZED.toString());


                    //add the properties given in the command create-shared-service.
                    /*Properties mergedProperties = new Properties();
                    if (properties != null) {
                        mergedProperties.putAll(properties);
                    }*/
                    if (properties != null) {
                        for (Map.Entry e : properties.entrySet()) {
                            Property prop = sharedService.createChild(Property.class);
                            prop.setName((String) e.getKey());
                            prop.setValue((String) e.getValue());
                            sharedService.getProperty().add(prop);
                        }
                    }
                    if (characteristics != null) {
                        Characteristics chars
                                = sharedService.createChild(Characteristics.class);
                        for (Map.Entry e : characteristics.entrySet()) {
                            Characteristic characteristic = chars.createChild(Characteristic.class);
                            characteristic.setName((String) e.getKey());
                            characteristic.setValue((String) e.getValue());
                            chars.getCharacteristic().add(characteristic);
                        }
                        sharedService.setCharacteristics(chars);
                    }

                    if (configuration != null) {
                        Configurations configs
                                = sharedService.createChild(Configurations.class);
                        for (Map.Entry e : configuration.entrySet()) {
                            Configuration config = configs.createChild(Configuration.class);
                            config.setName((String) e.getKey());
                            config.setValue((String) e.getValue());
                            configs.getConfiguration().add(config);
                        }
                        sharedService.setConfigurations(configs);
                    }

                    //while creating a shared service created if --defaultservice=true and --force=true,
                    // any other existing default shared service,if any, is set as non-default service.
                    if (defaultService && force) {
                        Services services = domain.getExtensionByType(Services.class);
                        for (Service service : services.getServices()) {
                            if (service instanceof SharedService) {
                                SharedService existingSharedService = (SharedService) service;
                                if (existingSharedService.getDefault() && serviceType.equalsIgnoreCase(existingSharedService.getType())) {
                                    Transaction transaction = Transaction.getTransaction(param);
                                    SharedService wShService = transaction.enroll(existingSharedService);
                                    wShService.setDefault(false);
                                    break;
                                }
                            }
                        }

                    }
                    param.getServices().add(sharedService);
                    return sharedService;
                }
            }, services) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Unable to create shared service");
            }
        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to create shared service [" + serviceName + "] due to : " + e.getMessage());
            report.setFailureCause(e);
        }
        //TODO rollback in case of failure.
    }

}
