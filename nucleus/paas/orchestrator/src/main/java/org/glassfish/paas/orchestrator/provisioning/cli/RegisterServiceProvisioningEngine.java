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
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngine;
import org.glassfish.paas.orchestrator.config.ServiceProvisioningEngines;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


/**
 * This command registers a service-provisioning engine..
 *
 * @author Sandhya Kripalani K
 */


@org.jvnet.hk2.annotations.Service(name = "register-service-provisioning-engine")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value = {CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "register-service-provisioning-engine", description = "Register Service Provisioning Engine")
})
public class RegisterServiceProvisioningEngine implements AdminCommand {

    @Param(name = "type", optional = false)
    private String type;

    @Param(name = "classname", primary = true)
    private String className;

    @Param(name = "defaultservice", defaultValue = "false", optional = true)
    private Boolean defaultService;

    @Param(name = "force", defaultValue = "false", optional = true)
    private Boolean force;

    @Param(name = "property", optional = true, separator = ':')
    private Properties properties;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        final ServiceProvisioningEngines serviceProvisioningEngines = serviceUtil.getServiceProvisioningEngines();

        for(ServiceProvisioningEngine serviceProvisioningEngine:serviceProvisioningEngines.getServiceProvisioningEngines()){
            if(serviceProvisioningEngine.getClassName().equals(className)){
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Duplicate key name. A service provisioning engine with name [ "+className+" ] is already registered.");
                return;
            }
        }

        if (defaultService) {
            if (!force) {
                for (ServiceProvisioningEngine serviceProvisioningEngine : serviceProvisioningEngines.getServiceProvisioningEngines()) {
                    if (Boolean.valueOf(serviceProvisioningEngine.getDefault()) && type.equalsIgnoreCase(serviceProvisioningEngine.getType())) {
                        //report.setMessage("A service provisioning engine named [" + serviceProvisioningEngine.getClassName() + "] is already marked as default service provisioning engine " +
                        //        "for type [" + type + "] , use --force=true to override the same");
                        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                        report.setFailureCause(new RuntimeException("A service provisioning engine named [" + serviceProvisioningEngine.getClassName() + "] is already marked as default service provisioning engine " +
                                "for type [" + type + "] , use --force=true to override the same"));
                        return;

                    }
                }
            }
        }

        try {
            if (ConfigSupport.apply(new SingleConfigCode<ServiceProvisioningEngines>() {
                public Object run(ServiceProvisioningEngines serviceProvisioningEngines) throws PropertyVetoException, TransactionFailure {
                    Locale locale=Locale.getDefault();
                    ServiceProvisioningEngine serviceProvisioningEngine = serviceProvisioningEngines.createChild(ServiceProvisioningEngine.class);
                    serviceProvisioningEngine.setClassName(className);
                    serviceProvisioningEngine.setType(type.toUpperCase(locale));
                    serviceProvisioningEngine.setDefault(defaultService);

                    if (properties != null) {
                        for (Map.Entry e : properties.entrySet()) {
                            Property prop = serviceProvisioningEngine.createChild(Property.class);
                            prop.setName((String) e.getKey());
                            prop.setValue((String) e.getValue());
                            serviceProvisioningEngine.getProperty().add(prop);
                        }
                    }
                    serviceProvisioningEngines.getServiceProvisioningEngines().add(serviceProvisioningEngine);

                    if (defaultService) {
                        if (force) {
                            for (final ServiceProvisioningEngine spe : serviceProvisioningEngines.getServiceProvisioningEngines()) {
                                if (spe.getDefault() && type.equalsIgnoreCase(spe.getType())) {

                                    Transaction t = Transaction.getTransaction(serviceProvisioningEngines);
                                    ServiceProvisioningEngine spe_w = t.enroll(spe);
                                    spe_w.setDefault(false);
                                    break;
                                }
                            }
                        }                                                                                                  []
                    }

                    return serviceProvisioningEngines;
                }
            }, serviceProvisioningEngines) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                //report.setMessage("Unable to register service provisioning service");
                report.setFailureCause(new RuntimeException("Unable to register service provisioning service"));
                return;
            } else {
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;
            }

        } catch (TransactionFailure transactionFailure) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            //report.setMessage("Unable to register service provisioning service due to : " + transactionFailure.getMessage());
            report.setFailureCause(transactionFailure);
            return;

        }


    }
}


