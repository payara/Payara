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

package org.glassfish.paas.orchestrator.service.spi;

import java.util.Set;

import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Contract;

/**
 * An SPI to allow the adding in of multiple service provider implementations
 * into the PaaS runtime. Each <code>Plugin</code> supports a Service of a 
 * particular <code>ServiceType</code>
 * <p/>
 * Each Plugin provides its own implementation of <code>ServiceDescription</code>
 * and <code>ProvisionedService</code>.
 * <p/>
 * A Plugin for a Service implementation performs the following functions:
 * <ul>
 * <b>Service Dependency Discovery</b> 
 * <li>provides explicit service dependencies from an application to a service<li>
 * <li>discovers implicit and discovered service dependencies of an application
 * and helps the orchestrator in finding out the effective set of service 
 * dependencies of the application<li>
 * <b>Service Provisioning and Service Association</b>
 * <li> provisions/decomissions a Service <li>
 * <li> associates/disassociates a target ProvisionedService from/to a 
 * source ProvisionedService <li>
 * </ul>
 * 
 * @author Sivakumar Thyagarajan
 */
@Contract
public interface Plugin<T extends ServiceType> {
    /**
     * Provides the <code>ServiceType</code> supported by this plugin
     *
     * @return the <code>ServiceType</code> supported by this plugin
     */
    public T getServiceType();

    /**
     * Checks if this plugin can process and handle the provided orchestration enabled
     * archive
     *
     * @return true if the plugin can handle service provisioning for this
     *         archive, false otherwise
     */
    public boolean handles(ReadableArchive cloudArchive);

    /**
     * Checks if a service reference of a particular ref-type
     * can be supported by a service that this plugin can provision.
     * <p/>
     * For instance, if a Java EE application required a javax.sql.Datasource
     * service reference, the DB plugin would indicate that it could support
     * that service ref-type.
     */
    public boolean isReferenceTypeSupported(String referenceType);

    /* SERVICE DEPENDENCY DISCOVERY */
    
    /**
     * Discover the implicit service references in the orchestration enabled archive.
     * For instance, a orchestration.xml may not explicitly provide a RDBMS ServiceType
     * ServiceDescription and a ServiceReference for a JDBC connection pool and
     * resource used by the application. The plugin could figure out that a
     * RDBMS ServiceType service reference is implicit in the application by
     * virtue of the application having a reference to a JDBC resource in
     * component deployment descriptors (web.xml, sun-web.xml)
     *
     * @param appName application-name as generated by deployment framework.
     *
     * @param cloudArchive the orchestration-enabled archive for which the CAS needs to determine
     *                     implicit ServiceReferences
     * @return A Set of ServiceReferences required to be satisfied for the
     *         proper functioning of the orchestration-enabled archive
     */
    public Set<ServiceReference> getServiceReferences(
            String appName, ReadableArchive cloudArchive);

    /**
     * For a discovered (implicit) ServiceReference, the application developer
     * may not have provided corresponding ServiceDefinitions to satisfy the
     * ServiceReference. Since the Service Provisioning layer works with
     * concrete ServiceDefinitions to provision a service, the CAS uses this
     * method to obtain a ServiceDescription, defaulted to known values, for a
     * ServiceReference.
     *
     * ServiceReference can also be modified to fill in additional details.
     * For eg., default jdbc-connection-pool properties.
     *
     * @return A valid ServiceDescription with defaults filled in.
     */
    public ServiceDescription getDefaultServiceDescription(
            String appName, ServiceReference svcRef);

    /**
     * OE gets implicit <code>ServiceDescription</code> associated with an 
     * application from the <code>Plugin</code> through this method, 
     * if the application does not have explicit <code>ServiceDescription<code>s 
     * in its services metadata deployment descriptor, for the type supported
     * by the <code>Plugin</code>.
     * 
     * @param cloudArchive The application archive
     * @param appName The application Name
     * @return A <code>Set</code> of implicit <code>ServiceDescription</code>s
     * that this Plugin "implies" for the provided application archive. 
     */
    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive cloudArchive, String appName);
    
    /* SERVICE PROVISIONING */
    
    /**
     * Once the CPAS merges all discovered and explicit
     * <code>ServiceDefinitions</code>s, it provisions the required Services
     * through the <code>Plugin</code>.
     *
     * @return a Set of <code>ProvisionedService</code>s
     */
    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc);

    /**
     * When CPAS is restarted, the CPAS uses this method to get the 
     * <code>ProvisionedService</code> that were provisioned earlier 
     * through the plugin.
     * 
     * @param serviceDescription <code>ServiceDescription</code> for the
     * service dependency.
     * @param serviceInfo The <code>ServiceInfo</code> persisted in the 
     * configuration store for the <code>ProvisionedService</code>
     * 
     * @return A <code>ProvisionedService</code> instance that represents
     * details about the provisioned service.
     */
    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo);

    /**
     * During undeployment of an application, OE decommissions
     * the Services that are scoped to the application. The Orchestrator uses
     * this method to let the plugin decommission a Service provisioned by it.
     * The plugin stops the Service and then performs any cleanup required
     * for a service to be decomissioned. It should be noted that the state of
     * the Service may be lost as part of the decomissioning process. [for 
     * instance the data in a Database may be lost during application 
     * undeployment unless the user explicitly indicates that undeployment
     * must retain prior state or the Service provider implementation provides
     * a mechanism to persist the State before destroying the Service.]
     *  
     * @param serviceDescription The <code>ServiceDescription</code> associated
     * with the provisioned service that needs to be decommissioned.
     * @param dc The <code>DeploymentContext</code> associated with the
     * undeployment operation that initiated this decomissioning process.
     * 
     * @return true if the Service was successful unprovisioned, false otherwise.
     */
    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc);

    /**
     * Scales the size of a Service up or down as per the provided scalingFactor.
     * 
     * @param serviceDesc The original ServiceDescription of the Service
     * @param scaleCount Number of units of the Service that needs to be scaled.
     * A positive number for scaling up and a negative number for scaling down.
     * @param allocStrategy The allocationStrategy that needs to be utilized
     * to scale the Service. The allocationStrategy implementation that is 
     * provided could be used to spawn a new instance in a less-loaded/underutilized
     * machine in the <code>ServerPool</code>. This could be null, if the default
     * allocation strategy needs to be employed.
     * 
     * @return the new ProvisionedService scaling operation was successful
     */
    public ProvisionedService scaleService(ServiceDescription serviceDesc, 
            int scaleCount, AllocationStrategy allocStrategy);


    /* SERVICE ASSOCIATION/BINDING */
    
    /**
     * A <code>ProvisionedService</code> for a <code>ServiceReference</code> is
     * associated with another <code>ProvisionedService</code> through this method.
     * See the section on "Service Association/Binding" at 
     * http://wikis.sun.com/display/GlassFish/3.2+Service+Orchestration+One+Pager
     * for more information.
     *
     * @param serviceConsumer The "target" <code>ProvisionedService</code>
     * @param svcRef The <code>ServiceReference</code> that binds the "source"
     * and "target" <code>ServiceReference</code>s.
     * @param serviceProvider The "source" <code>ProvisionedService</code>
     * @param beforeDeployment Indicates if this association is happening before
     * the deployment of the application
     * @param dc The <code>DeploymentContext</code> associated with the application
     * deployment or enablement that caused this association
     *                         
     */
    public void associateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                  ProvisionedService serviceProvider, boolean beforeDeployment,
                                  DeploymentContext dc);

    /**
     * A <code>ProvisionedService</code> for a <code>ServiceReference</code> is
     * dis-associated from another <code>ProvisionedService</code> through this method.
     * 
     * See the section on "Service Association/Binding" at 
     * http://wikis.sun.com/display/GlassFish/3.2+Service+Orchestration+One+Pager
     * for more information.

     * @param serviceConsumer The "target" <code>ProvisionedService</code>
     * @param svcRef The <code>ServiceReference</code> that bound the "source"
     * and "target" <code>ServiceReference</code>s.
     * @param serviceProvider The "source" <code>ProvisionedService</code>
     * @param beforeUndeploy Indicates if this dis-association is happening before
     * the deployment of the application
     * @param dc The <code>DeploymentContext</code> associated with the application
     * undeployment or disablement that caused this dis-association
     */
    public void dissociateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                   ProvisionedService serviceProvider, boolean beforeUndeploy,
                                   DeploymentContext dc);
    
    /**
     * Deploy the orchestration-enabled archive. 
     * 
     * At present, the deployment to the provisioned GlassFish Service
     * is performed by the deployment infrastructure in CPAS/DAS. In the future,
     * when we support non-Java EE archives that the deployment infrastructure
     * cannot control, OE would invoke this method in the plugin to initiate
     * deployment of the archive in that container service.
     */
    public ApplicationContainer deploy(ReadableArchive cloudArchive);

    /* SERVICE LIFECYCLE MANAGEMENT */
    
    /**
     * Start a Service that had been provisioned earlier and is now Stopped.
     * 
     * @param serviceDescription The <code>ServiceDescription</code> of the 
     * Service that needs to be started
     * @param serviceInfo The <code>ServiceInfo</code> that captures the prior 
     * provisioning state of the Service 
     * 
     * @return A reference to the started Service.
     */
    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo);

    /**
     * Stop a <code>ProvisionedService</code>.
     * 
     * @param serviceDescription The <code>ServiceDescription</code> of the 
     * Service that needs to be stopped
     * @param serviceInfo The <code>ServiceInfo</code> that captures the 
     * provisioned state of the Service 
     * 
     * @return True if the Service was successfully stopped, False otherwise.
     */
    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo);

    // The methods that follow are not relevant for the first prototype
    // Capturing these here for completeness

    /**
     * Checks if a ProvisionedService is still running (ie ping service). This
     * could be used by CAS, in scenarios where the CAS is restarted, but the
     * Provisioned Services are still available for an application to be used
     * and no fresh provisioning of Services needs to be done.
     */
    public boolean isRunning(ProvisionedService provisionedSvc);

    /**
     * Given a <code>ServiceReference</code>, find if a Service has already been
     * provisioned for it.
     */
    public ProvisionedService match(ServiceReference svcRef);

    /**
     * When a Service has been re-provisioned, and a prior deployment has
     * already been bound to the earlier ProvisionedService, CAS uses this
     * method to reassociate resources to point to the newly
     * <code>ProvisionedService</code>
     */
    public boolean reconfigureServices(ProvisionedService oldPS,
                                       ProvisionedService newPS);

    /**
     * When a Service has been re-provisioned, and a prior deployment has
     * already been bound to the earlier ProvisionedService, CPAS uses this
     * method to reassociate resources of the "Service Consumer" 
     * <code>ProvisionedService</code> to point to the new "Service Provider"
     * <code>ProvisionedService</code>.
     * 
     * Some of the reasons reconfiguration may occur are auto-scaling
     * of Services, CPAS or VM restarts.
     * 
     * @param svcConsumer The Service Consumer ProvisionedService
     * @param oldSvcProvider The old Service Provider ProvisionedService
     * @param newSvcProvider The new Service Provider ProvisionedService
     * @param reason The reason for the re-configuration.
     */
    public boolean reassociateServices(ProvisionedService svcConsumer, ProvisionedService oldSvcProvider,
            ProvisionedService newSvcProvider, 
            ServiceOrchestrator.ReconfigAction reason);
}
