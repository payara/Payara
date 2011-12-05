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

package org.glassfish.paas.orchestrator;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.service.metadata.ServiceMetadata;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Contract;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;

/**
 * The Orchestration Engine (OE) component in a PaaS runtime 
 * performs the following functions during deployment of an archive.
 * 
 * <ul>
 * <li>Service Dependency Discovery</li>
 * <li>Service Provisioning</li>
 * <li>Service Association/Binding</li>
 * <li>Application Deployment</li>
 * </ul>
 * 
 * The Orchestrator performs these functions through the use of Service implementation
 * specific Service Provisioning Engines (SPEs)
 */

@Contract
public interface ServiceOrchestrator {
    
    /**
     * Deploys an application archive into the PaaS runtime.
     * 
     * XXX: This is currently used by the cloud-deploy command. The deploy
     * command integration is through <code>ApplicationLifecycleInterceptor</code>
     * and hence this can be removed later once we move to the deploy 
     * command fully.
     * 
     * @param appName the name of the application as it should be referenced
     * in the PaaS console
     * @param cloudArchive the application archive 
     */
    public void deployApplication(String appName, ReadableArchive cloudArchive);
    
    /**
     * Provides the <code>ServiceMetadata</code> associated with an application
     * archive. This is used by GUI and the IDE plugin to get the service
     * dependencies and default <code>ServiceDescription</code>s associated
     * that the OE and SPEs have discovered for the provided application archive.
     * 
     * @param archive Application archive
     * @return The <code>ServiceMetadata</code> of the application discovered
     * by OE and SPEs.
     */
    public ServiceMetadata getServices(ReadableArchive archive) throws Exception;
    
    /**
     * Scales the size of a Service up or down as per the provided scalingFactor.
     * The Cloud Elasticity Manager(CEM) component uses this method to perform
     * auto-scaling of Services (GlassFish Cluster etc) based on user-defined
     * alerts and alarms.
     * 
     * @param appName Name of the application
     * @param svcName Names of the service to be scaled
     * @param scaleCount Number of units of the Service that needs to be scaled.
     * A positive number for scaling up and a negative number for scaling down.
     * @param allocStrategy The allocationStrategy that needs to be utilized
     * to scale the Service. The allocationStrategy implementation that is 
     * provided could be used to spawn a new instance in a less-loaded/underutilized
     * machine in the <code>ServerPool</code>. This could be null, if the default
     * allocation strategy needs to be employed.
     * @return true if the scaling operation was successful, and false otherwise
     */
    public boolean scaleService(String appName, String svcName, 
            int scaleCount, AllocationStrategy allocStrategy);
    
    public static enum ReconfigAction {AUTO_SCALING, RESTART};
    
    /**
     * given the application-name and service-name, retrieve the service-description
     * info. </BR>
     * Useful for GUI/Tooling modules to retrieve the service-description.
     * @param appName application-name
     * @param service service-name
     * @return ServiceDescription
     */
    public ServiceDescription getServiceDescription(String appName, String service);

}
