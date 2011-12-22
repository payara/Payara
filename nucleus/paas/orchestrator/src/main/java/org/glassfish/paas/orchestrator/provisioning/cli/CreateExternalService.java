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
import org.glassfish.paas.orchestrator.config.Configuration;
import org.glassfish.paas.orchestrator.config.Configurations;
import org.glassfish.paas.orchestrator.config.ExternalService;
import org.glassfish.paas.orchestrator.config.Services;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;

/**
 * @author Jagadish Ramu
 */
@Service(name = "create-external-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(value={CommandTarget.DAS})
@RestEndpoints({
        @RestEndpoint(configBean = Domain.class, opType = RestEndpoint.OpType.GET, path = "create-external-service", description = "Create an external service")
})
public class CreateExternalService implements AdminCommand {

    @Param(name="defaultService", defaultValue = "false", optional = true)
    private Boolean defaultService;

    @Param(name="force", defaultValue = "false", optional = true)
    private Boolean force;

    @Param(name="servicetype", optional = false)
    private String serviceType;

    @Param(name="property", optional=true, separator=':')
    private Properties properties;

    @Param(name = "configuration", optional = true, separator = ':')
    private Properties configuration;

    @Param(name="servicename", primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

    @Inject
    private ServiceUtil serviceUtil;

    //TODO logging
    //TODO java-doc
    public void execute(AdminCommandContext context) {

        final ActionReport report = context.getActionReport();

        Services services = serviceUtil.getServices();
        if(defaultService){
            if(force){
                //TODO unset default=true in any other external service that already exists.
            }else{

                for(org.glassfish.paas.orchestrator.config.Service service : services.getServices()){
                    if(service instanceof ExternalService){
                        if(((ExternalService) service).getDefault()){
                            report.setMessage("An external service named ["+service.getServiceName()+"] is already marked as default service, " +
                                    "use --force=true to override the same");
                            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                            return;
                        }
                    }
                }
            }
        }

        try {
            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services param) throws PropertyVetoException, TransactionFailure {
                    ExternalService externalService = param.createChild(ExternalService.class);
                    externalService.setDefault(defaultService);
                    externalService.setType(serviceType);
                    externalService.setServiceName(serviceName);

                    if (configuration != null) {
                        Configurations configs
                                = externalService.createChild(Configurations.class);
                        for (Map.Entry e : configuration.entrySet()) {
                            Configuration config = configs.createChild(Configuration.class);
                            config.setName((String)e.getKey());
                            config.setValue((String)e.getValue());
                            configs.getConfiguration().add(config);
                        }
                        externalService.setConfigurations(configs);
                    }

                    if (properties != null) {
                        for (Map.Entry e : properties.entrySet()) {
                            Property prop = externalService.createChild(Property.class);
                            prop.setName((String) e.getKey());
                            prop.setValue((String) e.getValue());
                            externalService.getProperty().add(prop);
                        }
                    }
                    param.getServices().add(externalService);
                    return externalService;
                }
            }, services) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("Unable to create external service");
            }else{
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            }
        } catch (TransactionFailure transactionFailure) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Unable to create external service due to : " + transactionFailure.getMessage());
            report.setFailureCause(transactionFailure);
        }
    }
}
