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

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.paas.orchestrator.config.Service;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.jvnet.hk2.annotations.Contract;

import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.service.ServiceType;
import org.glassfish.api.deployment.ApplicationContainer;

/**
 * An SPI to allow the plugging in of multiple service provider implementations.
 * Each Plugin supports a Service of a particular <code>ServiceType</code>
 * <p/>
 * Each Plugin provides its own implementation of <code>ServiceDescription</code>
 * and <code>ProvisionedService</code>.
 * <p/>
 * XXX: Parsing orchestration.xml
 * XXX: find out how schema.service requirements could become ServiceDefinitions
 * XXX: Implement GlassFishPlugin and learn what would go into GlassFishProvisionedService
 * XXX: Start implementing other plugins(GF, DB, HTTP LB, JMS?)
 * XXX: Orchestrator to use HK2 Services mechanism to discover deployed Plugins.
 * XXX: How can we ensure Cloud sniffer and deployer alone is used in CAS?
 * XXX: Support service-requirements (a lighter-weight vendor-neutral mechanism
 * to specify requirements of an application)
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

    /**
     * Discover the implicit service references in the orchestration enabled archive.
     * For instance, a orchestration.xml may not explicitly provide a RDBMS ServiceType
     * ServiceDescription and a ServiceReference for a JDBC connection pool and
     * resource used by the application. The plugin could figure out that a
     * RDBMS ServiceType service reference is implicit in the application by
     * virtue of the application having a reference to a JDBC resource in
     * component deployment descriptors (web.xml, sun-web.xml)
     *
     * @param cloudArchive the orchestration-enabled archive for which the CAS needs to determine
     *                     implicit ServiceReferences
     * @return A Set of ServiceReferences required to be satisfied for the
     *         proper functioning of the orchestration-enabled archive
     */
    public Set<ServiceReference> getServiceReferences(
            ReadableArchive cloudArchive);

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
     * Once the CAS merges all discovered and explicit
     * <code>ServiceDefinitions</code>s, it provisions the required Services
     * through the <code>Plugin</code>.
     *
     * @return a Set of <code>ProvisionedService</code>s
     */
    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc);

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc);

    public void dissociateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                   ProvisionedService serviceProvider, boolean beforeUndeploy,
                                  DeploymentContext dc);

    /**
     * A <code>ProvisionedService</code> for a <code>ServiceReference</code> is
     * associated with each other through this method.
     *
     * @param beforeDeployment indicates if this association is happening before the
     *                         deployment of the application.
     */
    public void associateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                  ProvisionedService serviceProvider, boolean beforeDeployment,
                                  DeploymentContext dc);

    /**
     * Deploy the orchestration-enabled archive
     */
    public ApplicationContainer deploy(ReadableArchive cloudArchive);

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

    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive cloudArchive, String appName);

}
