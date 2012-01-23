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
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Service;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author Jagadish Ramu
 */
public abstract class DissociationState extends AbstractPaaSDeploymentState {

    protected void dissociateProvisionedServices(PaaSDeploymentContext context, boolean beforeUndeploy) {
        String state="after";
        if(beforeUndeploy){
            state="before";
        }
        String appName = context.getAppName();
        final ServiceMetadata appServiceMetadata = appInfoRegistry.getServiceMetadata(appName);
        Set<org.glassfish.paas.orchestrator.service.spi.Service> allServices =
                orchestrator.getServicesForDissociation(appName);
        logger.log(Level.FINEST,localStrings.getString("dissociate.provisioned.services.preundeployment",state));
        boolean failed = false;
        Exception failureCause = null;

        for (Service serviceProvider : allServices) {
        ServiceDescription sd = serviceProvider.getServiceDescription();
            Set<ServiceReference> appSRs = appServiceMetadata.getServiceReferences();
            for(ServiceReference serviceRef : appSRs){
                Map<ServiceReference, ServiceDescription> srToSDMap = appInfoRegistry.getSRToSDMap(appName);
                for(Map.Entry<ServiceReference, ServiceDescription> entry : srToSDMap.entrySet()){
                    ServiceReference ref = entry.getKey();
                    if(ref.equals(serviceRef) && sd.equals(entry.getValue())){
                        ServicePlugin requestingPlugin = ref.getRequestingPlugin();
                        Collection<Service> serviceConsumers =
                                    orchestrator.getServicesManagedByPlugin(requestingPlugin, allServices);
                        for (Service serviceConsumer : serviceConsumers) {
                            try {
                                Object args[]=new Object[]{serviceProvider,serviceRef,requestingPlugin};
                                logger.log(Level.INFO, "dissociate.provisionedservice",args);
                                requestingPlugin.dissociateServices(serviceConsumer, serviceRef, serviceProvider,
                                        beforeUndeploy, context);
                            } catch (Exception e) {
                                    //TODO need to handle exception or continue ?

                                   failed = true;
                                   failureCause = e;
                            }
                        }
                    }
                }
            }
        }
    }
}