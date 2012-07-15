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

package org.glassfish.paas.orchestrator.provisioning.cli;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.config.Services;
import org.glassfish.paas.orchestrator.config.SharedService;
import org.glassfish.paas.orchestrator.provisioning.ServiceScope;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import javax.inject.Inject;

import org.jvnet.hk2.component.Habitat;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.Transaction;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: Sandhya Kripalani K
 */

@org.jvnet.hk2.annotations.Service
@PerLookup
public class SharedServiceLazyInitializer {

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private Domain domain;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private Habitat habitat;

    @Inject
    private ServiceOrchestratorImpl serviceOrchestrator;

    private static Logger logger = LogDomains.getLogger(ServiceOrchestratorImpl.class, LogDomains.PAAS_LOGGER);

    final static StringManager localStrings = StringManager.getManager(ServiceOrchestratorImpl.class);

    public ProvisionedService provisionService(ServiceDescription sd, ActionReport report) {

        if (report == null) {
            report = habitat.getComponent(ActionReport.class);
        }

        //create virtual cluster for the shared-service.
        //TODO we need to see the impact of virtual-cluster for service-names
        //TODO as service-names are unique only per scope whereas virtual-cluster
        //TODO may not be.

        // create one virtual cluster per shared-service.
        String virtualClusterName = sd.getName();
        ActionReport actionReport = habitat.getComponent(ActionReport.class);
        CommandRunner.CommandInvocation commandInvocation = commandRunner.getCommandInvocation("create-cluster", actionReport);
        ParameterMap parameterMap = new ParameterMap();
        parameterMap.add("DEFAULT", virtualClusterName);
        commandInvocation.parameters(parameterMap).execute();

        Object args[] = new Object[]{virtualClusterName, actionReport.getMessage()};
        logger.log(Level.INFO, "create.cluster.exec.output", args);
        if (actionReport.getActionExitCode().equals(ActionReport.ExitCode.FAILURE)) {
            if (actionReport.getFailureCause() != null) {
                report.setFailureCause(actionReport.getFailureCause());
            }
            report.setMessage(actionReport.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            logger.log(Level.WARNING, localStrings.getString("unable.create.cluster", virtualClusterName), report.getFailureCause());
            return null;
        }

        //set the virtual-cluster name in the service-description.
        sd.setVirtualClusterName(virtualClusterName);
        ServicePlugin defaultPlugin = serviceOrchestrator.getPlugin(sd);
        sd.setPlugin(defaultPlugin);
        sd.setServiceScope(ServiceScope.SHARED);
        DeploymentContext dc = null;
        PaaSDeploymentContext pdc = new PaaSDeploymentContext(null, dc);
        ProvisionedService ps = defaultPlugin.provisionService(sd, pdc);
        Properties properties = ps.getServiceProperties();
        serviceUtil.updateState(sd.getName(),null,ps.getStatus().toString());
        for(Map.Entry e:properties.entrySet()){
            serviceUtil.setProperty(sd.getName(),null,(String)e.getKey(),(String)e.getValue());

        }
        /*try {
            Services services = serviceUtil.getServices();
            if (ConfigSupport.apply(new SingleConfigCode<Services>() {
                public Object run(Services services) throws PropertyVetoException, TransactionFailure {
                    for (org.glassfish.paas.orchestrator.config.Service service : services.getServices()) {
                        if (service instanceof SharedService && service.getServiceName().equalsIgnoreCase(virtualClusterName)) {
                            Transaction transaction = Transaction.getTransaction(services);
                            SharedService w_sharedService = transaction.enroll((SharedService) service);
                            //Set the state of the shared service to that of the state of the corresponding provisioned service
                            w_sharedService.setState(ps.getStatus().toString());
                            //Add all the properties of the provisioned service to then config of the corresponding shared service
                            for (Map.Entry e : properties.entrySet()) {
                                org.jvnet.hk2.config.types.Property prop = w_sharedService.createChild(org.jvnet.hk2.config.types.Property.class);
                                prop.setName((String) e.getKey());
                                prop.setValue((String) e.getValue());
                                w_sharedService.getProperty().add(prop);
                            }
                        }
                    }
                    return services;
                }
            }, services) == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(new RuntimeException("Unable to create shared service"));
                logger.log(Level.WARNING, localStrings.getString("unable.create.shared.service"), report.getFailureCause());
                return null;
            }
        } catch (TransactionFailure transactionFailure) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(transactionFailure);
            return null;

        }*/
        //TODO if there is a failure, delete virtual cluster ?
        //TODO rollback in case of failure.
        registerSharedService(ps);
        return ps;
    }

    public ProvisionedService provisionService(String serviceName) {
        ServiceDescription sd = serviceOrchestrator.getSharedServiceDescription(serviceName);
        return provisionService(sd, null);
    }

    private void registerSharedService(final ProvisionedService ps) {
        //check whether provisioned-service has child nodes and register them.

        //we are not passing the parent as its already persisted in the previous steps.
        //we persist only necessary information of child services (eg: type, state, service-properties and
        //not service-description/characteristics/configuration.
        if (ps.getChildServices() != null && ps.getChildServices().size() > 0) {
            for (org.glassfish.paas.orchestrator.service.spi.Service childPS : ps.getChildServices()) {
                serviceUtil.registerService(null, childPS, ps);
            }
        }
        serviceUtil.fireServiceChangeEvent(ServiceChangeEvent.Type.CREATED, ps);
    }
}
