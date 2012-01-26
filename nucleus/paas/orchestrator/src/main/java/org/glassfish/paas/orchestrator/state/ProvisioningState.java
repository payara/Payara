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

package org.glassfish.paas.orchestrator.state;

import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
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
        logger.log(Level.FINER, localStrings.getString("METHOD.provisionServices"));
        final Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = appInfoRegistry.getServiceMetadata(appName);


        // create one virtual cluster per deployment unit.
        String virtualClusterName = orchestrator.getVirtualClusterName(appServiceMetadata);
        if(virtualClusterName != null){
            CommandResult result = commandRunner.run("create-cluster", virtualClusterName);
            Object args[]=new Object[]{virtualClusterName,result.getOutput()};
            logger.log(Level.INFO,"create.cluster.exec.output",args);
            if (result.getExitStatus().equals(CommandResult.ExitStatus.FAILURE)) {
                throw new RuntimeException("Failure while provisioning services, " +
                        "Unable to create cluster [" + virtualClusterName + "]");
            }
        }

        Collection<ServiceDescription> serviceDescriptionsToProvision =
                orchestrator.getServiceDescriptionsToProvision(appName);
        boolean failed = false;
        Exception rootCause = null;
        if (isParallelProvisioningEnabled()) {
            List<Future<ProvisionedService>> provisioningFutures = new ArrayList<Future<ProvisionedService>>();
            for (final ServiceDescription sd : serviceDescriptionsToProvision) {
                Future<ProvisionedService> future = ServiceUtil.getThreadPool().submit(new Callable<ProvisionedService>() {
                    public ProvisionedService call() {
                        ServicePlugin<?> chosenPlugin = sd.getPlugin();
                        Object args[]=new Object[]{sd,chosenPlugin};
                        logger.log(Level.FINEST, localStrings.getString("started.provisioningservice.parallel",args));
                        return chosenPlugin.provisionService(sd, context);
                    }
                });
                provisioningFutures.add(future);
            }


            for (Future<ProvisionedService> future : provisioningFutures) {
                try {
                    ProvisionedService ps = future.get();
                    serviceUtil.registerService(appName, ps, null);
                    appPSs.add(ps);
                    logger.log(Level.FINEST, localStrings.getString("completed.provisioningservice.parallel",ps));
                } catch (Exception e) {
                    failed = true;
                    logger.log(Level.WARNING, "failure.provisioningservice.parallel", e);
                    if (rootCause == null) {
                        rootCause = e; //we are caching only the first failure and logging all failures
                    }
                }
            }
        } else {

            for (final ServiceDescription sd : serviceDescriptionsToProvision) {
                try {
                    ServicePlugin<?> chosenPlugin = sd.getPlugin();
                    Object args[]=new Object[]{sd,chosenPlugin};
                    logger.log(Level.FINEST, localStrings.getString("started.provisioningservice.serial",args));
                    ProvisionedService ps = chosenPlugin.provisionService(sd, context);
                    serviceUtil.registerService(appName, ps, null);
                    appPSs.add(ps);
                    logger.log(Level.FINEST, localStrings.getString("completed.provisioningservice.serial",ps));
                } catch (Exception e) {
                    Object args[]=new Object[]{sd.getName(),sd.getPlugin(),e};
                    failed = true;
                    logger.log(Level.WARNING, "failure.provisioningservice", args);
                    rootCause = e;
                    break; //since we are provisioning serially, we can abort
                }
            }
        }
        if(!failed){
            appInfoRegistry.registerProvisionedServices(context.getAppName(), appPSs);
            return appPSs;
        }else{
            if(isAtomicDeploymentEnabled()){
                for(ProvisionedService ps : appPSs){
                    try{
                        ServiceDescription sd = ps.getServiceDescription();
                        ServicePlugin<?> chosenPlugin = sd.getPlugin();
                        Object args[]=new Object[]{sd,chosenPlugin};
                        logger.log(Level.INFO, "rollingback.provisioningservice",args);
                        chosenPlugin.unprovisionService(sd, context); //TODO we could do unprovisioning in parallel.
                        serviceUtil.unregisterServiceInfo(sd.getName(), sd.getAppName());
                        logger.log(Level.INFO, "rolledback.provisioningservice",args);
                    }catch(Exception e){
                        Object args[]={ps,e};
                        logger.log(Level.WARNING, "failure.while.rollingback.ps",args);
                    }
                }

                // Clean up the virtual cluster config if is application-scoped.
                if(virtualClusterName != null){
                    orchestrator.removeVirtualCluster(virtualClusterName);
                }
                appInfoRegistry.removeProvisionedServices(appName);
                appInfoRegistry.removeServiceMetadata(appName);
                appInfoRegistry.removePluginsToHandleSDs(appName);
                appInfoRegistry.removeSRToSDMap(appName);
            }

            PaaSDeploymentException re = new PaaSDeploymentException("Failure while provisioning services");
            if(rootCause != null){
                re.initCause(rootCause);
            }

            throw re;
        }
    }
}
