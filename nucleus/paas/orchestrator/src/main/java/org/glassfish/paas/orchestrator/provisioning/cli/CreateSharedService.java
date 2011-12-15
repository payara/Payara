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
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.paas.orchestrator.config.*;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Service(name = "create-shared-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@CommandLock(CommandLock.LockType.NONE)
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

    //TODO logging
    //TODO java-doc
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        if (defaultService) {
            if (force) {
                //TODO unset default=true in any other shared service that already exists.
            } else {
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
        }

        if(initMode.equalsIgnoreCase("lazy")){
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(new UnsupportedOperationException("Init type 'lazy' not supported. Set init-mode of service as 'eager'"));
            return;
        }

        //TODO interact with Orchestrator to see whether this particular service-configuration can
        //TODO be really supported.

        //TODO accept either template-id or characteristics but not both.

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
                        Transaction t = Transaction.getTransaction(chars);
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
                            config.setName((String)e.getKey());
                            config.setValue((String)e.getValue());
                            configs.getConfiguration().add(config);
                        }
                        sharedService.setConfigurations(configs);
                    }

                    param.getServices().add(sharedService);
                    return sharedService;
                }
            }, services) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Unable to create shared service");
            } else {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to create shared service [" + serviceName + "] due to : " + e.getMessage());
            report.setFailureCause(e);
        }
    }
}
