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

import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.PaaSDeploymentState;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.jvnet.hk2.annotations.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
@Service
public class UnprovisioningState extends AbstractPaaSDeploymentState {

    public void handle(PaaSDeploymentContext context) throws PaaSDeploymentException {
        unprovisionServices(context);
    }

    private void unprovisionServices(final PaaSDeploymentContext context) throws PaaSDeploymentException {
        final String appName = context.getAppName();

        Collection<ServiceDescription> serviceDescriptionsToUnprovision = orchestrator.getServiceDescriptionsToUnprovision(appName);
        List<Future> unprovisioningFutures = new ArrayList<Future>();

        for (final ServiceDescription sd : serviceDescriptionsToUnprovision) {
            Future future = ServiceUtil.getThreadPool().submit(new Runnable() {
                public void run() {
                    ServicePlugin<?> chosenPlugin = sd.getPlugin();
                    Object args[]=new Object[]{sd, chosenPlugin};
                    logger.log(Level.FINEST, localStrings.getString("unprovision.service",args));
                    chosenPlugin.unprovisionService(sd, context);
                    Collection<ProvisionedService> servicesToUnprovision = orchestrator.getServicesToUnprovision(appName);
                    for(ProvisionedService ps : servicesToUnprovision){
                        if(sd.getName().equals(ps.getName())){
                            serviceUtil.unregisterService(appName, ps);
                        }
                    }
                }
            });
            unprovisioningFutures.add(future);
        }

        boolean failed = false;
        for(Future future : unprovisioningFutures){
            try {
                future.get();
            } catch (InterruptedException e) {
                failed = true;
                logger.log(Level.WARNING, "failure.while.unprovisioning.service", e);
            } catch (ExecutionException e) {
                failed = true;
                logger.log(Level.WARNING, "failure.while.unprovisioning.service", e);
            }
        }
        // Clean up the glassfish cluster, virtual cluster config, etc.. if they are application scoped.
        final ServiceMetadata appServiceMetadata = orchestrator.getServiceMetadata(appName);
        String virtualClusterName = orchestrator.getVirtualClusterName(appServiceMetadata);
        if(virtualClusterName != null){
            orchestrator.removeVirtualCluster(virtualClusterName);
        }

        orchestrator.removeProvisionedServices(appName);
        orchestrator.removeServiceMetadata(appName);

        if(failed){
            throw new PaaSDeploymentException("Failure while unprovisioning services, refer server.log for more details");
        }
    }

    public Class<PaaSDeploymentState> getRollbackState() {
        return null;
    }
}
