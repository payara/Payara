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

import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.PaaSDeploymentException;
import org.glassfish.paas.orchestrator.ServiceOrchestratorImpl;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceUtil;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Jagadish Ramu
 */
@Service
public class EnableState extends AbstractPaaSDeploymentState {

    @Inject
    private ServiceUtil serviceUtil;

    @Inject
    private Habitat habitat;

    private static Logger logger = Logger.getLogger(ServiceOrchestratorImpl.class.getName());

    public void handle(PaaSDeploymentContext context) throws PaaSDeploymentException {
        startServices(context);
    }

    public Class getRollbackState() {
        return DisableState.class;
    }

    private Set<ProvisionedService> startServices(PaaSDeploymentContext context) throws PaaSDeploymentException {
        Set<ProvisionedService> appPSs = new HashSet<ProvisionedService>();
        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = orchestrator.getServiceMetadata(appName);

        List<ServiceDescription> provisionedSDs = new ArrayList<ServiceDescription>();
        for(ServiceDescription sd : appServiceMetadata.getServiceDescriptions()){
            try{
                ProvisionedService ps = startService(context, appName, sd);
                appPSs.add(ps);
                provisionedSDs.add(sd);
            }catch(Exception e){
                logger.log(Level.WARNING, "Exception while starting service " +
                        "[ "+sd.getName()+" ] for application [ "+appName+" ]", e);

                DisableState disableState = habitat.getComponent(DisableState.class);
                for(ServiceDescription provisionedSD : provisionedSDs){
                    try{
                        disableState.stopService(context, appName, provisionedSD);
                    }catch(Exception stopException){
                        logger.log(Level.WARNING, "Exception while stopping service " +
                                "[ "+sd.getName()+" ] for application [ "+appName+" ]", stopException);
                    }
                }
                throw new PaaSDeploymentException(e);
            }
        }
        orchestrator.addProvisionedServices(appName, appPSs);
        return appPSs;
    }

    public ProvisionedService startService(PaaSDeploymentContext context, String appName, ServiceDescription sd) {
        final ServiceOrchestratorImpl orchestrator = context.getOrchestrator();
        //final Set<Plugin> installedPlugins = orchestrator.getPlugins();
        Plugin<?> chosenPlugin = sd.getPlugin();
        logger.log(Level.INFO, "Retrieving provisioned Service for " + sd + " through " + chosenPlugin);
        ServiceInfo serviceInfo = serviceUtil.retrieveCloudEntry(sd.getName(), appName, null );
        if(serviceInfo != null){
            return chosenPlugin.startService(sd, serviceInfo);
        }else{
            logger.warning("unable to retrieve service-info for service : " + sd.getName() + " of application : " + appName);
            return null;
        }
    }
}
