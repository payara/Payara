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

package org.glassfish.paas.orchestrator.state;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.orchestrator.*;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
public class ProvisioningState extends AbstractPaaSDeploymentState {

    @Inject
    private CommandRunner commandRunner;

    public void handle(PaaSDeploymentContext context) throws PaaSDeploymentException {
        try{
            provisionServices(context);
        }catch(Exception e){
            if(e instanceof PaaSDeploymentException){
                throw (PaaSDeploymentException)e;
            }else{
                throw new PaaSDeploymentException(e);
            }
        }
    }

    public Class getRollbackState() {
        return UnprovisioningState.class;
    }

    private Set<ProvisionedService> provisionServices(final PaaSDeploymentContext context) throws PaaSDeploymentException {
        //TODO refactor such that rollback is done in a different state.
        //TODO add exception handling for the entire task
        //TODO pass the exception via the PaaSDeploymentContext ?
        logger.entering(getClass().getName(), "provisionServices");
        final Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = orchestrator.getServiceMetadata(appName);


        // create one virtual cluster per deployment unit.
        String virtualClusterName = orchestrator.getVirtualClusterName(appServiceMetadata);
        if(virtualClusterName != null){
            CommandResult result = commandRunner.run("create-cluster", virtualClusterName);
            logger.info("Command create-cluster [" + virtualClusterName + "] executed. " +
                    "Command Output [" + result.getOutput() + "]");
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                throw new RuntimeException("Failure while provisioning services, " +
                        "Unable to create cluster [" + virtualClusterName + "]");
            }
        }

        Collection<ServiceDescription> serviceDescriptionsToProvision =
                orchestrator.getServiceDescriptionsToProvision(appName);
        boolean failed = false;
        Exception rootCause = null;
        if (Boolean.getBoolean("org.glassfish.paas.orchestrator.parallel-provisioning")) {
            List<Future<ProvisionedService>> provisioningFutures = new ArrayList<Future<ProvisionedService>>();
            for (final ServiceDescription sd : serviceDescriptionsToProvision) {
                Future<ProvisionedService> future = ServiceUtil.getThreadPool().submit(new Callable<ProvisionedService>() {
                    public ProvisionedService call() {
                        Plugin<?> chosenPlugin = sd.getPlugin();
                        logger.log(Level.INFO, "Started Provisioning Service in parallel for " + sd + " through " + chosenPlugin);
                        return chosenPlugin.provisionService(sd, context);
                    }
                });
                provisioningFutures.add(future);
            }


            for (Future<ProvisionedService> future : provisioningFutures) {
                try {
                    ProvisionedService ps = future.get();
                    appPSs.add(ps);
                    logger.log(Level.INFO, "Completed Provisioning Service in parallel " + ps);
                } catch (Exception e) {
                    failed = true;
                    logger.log(Level.WARNING, "Failure while provisioning service", e);
                    if (rootCause == null) {
                        rootCause = e; //we are caching only the first failure and logging all failures
                    }
                }
            }
        } else {

            for (final ServiceDescription sd : serviceDescriptionsToProvision) {
                try {
                    Plugin<?> chosenPlugin = sd.getPlugin();
                    logger.log(Level.INFO, "Started Provisioning Service serially for " + sd + " through " + chosenPlugin);
                    ProvisionedService ps = chosenPlugin.provisionService(sd, context);
                    appPSs.add(ps);
                    logger.log(Level.INFO, "Completed Provisioning Service serially " + ps);
                } catch (Exception e) {
                    failed = true;
                    logger.log(Level.WARNING, "Failure while provisioning service", e);
                    rootCause = e;
                    break; //since we are provisioning serially, we can abort
                }
            }
        }
        if(!failed){
            orchestrator.registerProvisionedServices(context.getAppName(), appPSs);
            return appPSs;
        }else{
            for(ProvisionedService ps : appPSs){
                try{
                    ServiceDescription sd = ps.getServiceDescription();
                    Plugin<?> chosenPlugin = sd.getPlugin();
                    logger.log(Level.INFO, "Rolling back provisioned-service for " + sd + " through " + chosenPlugin );
                    chosenPlugin.unprovisionService(sd, context); //TODO we could do unprovisioning in parallel.
                    logger.log(Level.INFO, "Rolled back provisioned-service for " + sd + " through " + chosenPlugin );
                }catch(Exception e){
                    logger.log(Level.FINEST, "Failure while rolling back provisioned service " + ps, e);
                }
            }

            // Clean up the glassfish cluster, virtual cluster config, etc..
            // TODO :: assuming app-scoped virtual cluster. fix it when supporting shared/external service.
            if(virtualClusterName != null){
                orchestrator.removeVirtualCluster(virtualClusterName);
            }

            PaaSDeploymentException re = new PaaSDeploymentException("Failure while provisioning services");
            if(rootCause != null){
                re.initCause(rootCause);
            }
            orchestrator.removeProvisionedServices(appName);
            orchestrator.removeServiceMetadata(appName);

            throw re;
        }
    }
}
