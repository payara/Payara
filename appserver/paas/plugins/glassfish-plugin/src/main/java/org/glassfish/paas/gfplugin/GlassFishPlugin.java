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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.UndeployCommandParameters;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.service.JavaEEServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceProvisioningException;
import org.glassfish.paas.spe.common.ServiceProvisioningEngineBase;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
@Service
@Scoped(PerLookup.class)
public class GlassFishPlugin extends ServiceProvisioningEngineBase
        implements Plugin<JavaEEServiceType> {

    @Inject
    private GlassFishCloudArchiveProcessor archiveProcessor;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    @Inject
    private ProvisionerUtil provisionerUtil;

    @Inject
    private Domain domain;

    @Inject
    private Habitat habitat;

    public static final String JAVAEE_SERVICE_TYPE = "JavaEE";

    public static final String MIN_CLUSTER_PROPERTY_NAME = "min.clustersize";
    public static final String MAX_CLUSTER_PROPERTY_NAME = "max.clustersize";

    private static Logger logger = Logger.getLogger(GlassFishPlugin.class.getName());

    public JavaEEServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return DeploymentUtils.isJavaEE(cloudArchive, habitat);
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        if(referenceType.equals(JAVAEE_SERVICE_TYPE)){
            return true;
        }
        //GlassFish plugin would not be able to support any other reference types
        return false;
    }

    public Set<ServiceReference> getServiceReferences(File archive, String appName) {
        try {
            return getServiceReferences(appName, archiveFactory.openArchive(archive));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<ServiceReference> getServiceReferences(String appName, ReadableArchive cloudArchive) {
        // Parse the archive and figure out resource references.
        return archiveProcessor.getServiceReferences(cloudArchive, appName);
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        return generateDefaultServiceDescription(appName);
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Delete all the service instances.
         */
        String clusterName = serviceDescription.getVirtualClusterName();
        boolean deleteSuccessful = false;
        for (int currentClusterSize = domain.getClusterNamed(clusterName).
                getInstances().size(); currentClusterSize > 0 ; currentClusterSize--) {
            serviceDescription.setName(serviceName + "." + currentClusterSize);
            // perform operation on VM before deleting??
            deleteSuccessful = super.deleteService(serviceDescription) || deleteSuccessful;
        }
        serviceDescription.setName(serviceName); // reset the name.

        /**
         * Step 2. Delete the DAS/cluster.
         * For now, nothing needs to be done for cluster, just delete the service entry corresponding to cluster.
         */
        deleteService(serviceDescription);

        /**
         * Step 3. Delete elastic service.
          */
        commandRunner.run("_delete-elastic-service", serviceName);
        
        return deleteSuccessful;
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription,
                                               DeploymentContext dc) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Provision DAS and cluster
         * For now, just create provisioned service object pointing to the local DAS
         */
        String dasIPAddress = "localhost";
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty("min.clustersize",
                serviceDescription.getConfiguration("min.clustersize"));
        serviceProperties.setProperty("max.clustersize",
                serviceDescription.getConfiguration("max.clustersize"));
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        GlassFishProvisionedService gfps = new GlassFishProvisionedService(
                serviceDescription, serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);

        /**
         * Step 2. Create as many GlassFish service instances as min.clustersize
         */
        int minClusterSize = Integer.parseInt(
                serviceDescription.getConfiguration("min.clustersize"));
        for (int i = 0; i < minClusterSize; i++) {
            serviceDescription.setName(serviceName + "." + (i+1));
            ProvisionedService provisionedService = createService(serviceDescription);
            // further customize the provisioned service
        }
        serviceDescription.setName(serviceName); // reset to original value.

        /**
         * Step 3. Set the target to the newly created cluster.
         * TODO what if someone requests for multiple GlassFish services ? As of now, the last one is considered as deployment target.
         */
        String clusterName = serviceName;
        DeployCommandParameters dcp = dc.getCommandParameters(
                DeployCommandParameters.class);
        dcp.target = clusterName;

        /**
         * Step 4. Create elastic service.
         */
        commandRunner.run("_create-elastic-service",
                "--min=" + serviceDescription.getConfiguration("min.clustersize"),
                "--max=" + serviceDescription.getConfiguration("max.clustersize"),
                serviceName);

        // Return the ProvisionedService object that represents the DAS/Cluster.
        return gfps;
    }

    public ProvisionedService startService(ServiceDescription serviceDescription,
                                           ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Start all the instances.
         */
        // Start all the instances
        String clusterName = serviceDescription.getVirtualClusterName();
        for (int currentClusterSize = domain.getClusterNamed(clusterName).
                getInstances().size(); currentClusterSize > 0 ; currentClusterSize--) {
            serviceDescription.setName(serviceName + "." + currentClusterSize);
            ProvisionedService provisionedService = super.startService(serviceDescription);
            // customize the VM after start???
        }
        serviceDescription.setName(serviceName); // reset the name.

        /**
         * Step 2. Start cluster. TODO :: remove start-cluster call once LocalGlassFishTemplateCustomizer's start is fixed.
         */
        commandRunner.run("start-cluster", serviceDescription.getVirtualClusterName());

        /**
         * Step 3. Enable elastic service.
         */
        commandRunner.run("enable-auto-scaling", serviceName);

        /**
         * Step 4. Create a ProvisionedService object representing the DAS/cluster.
         */
        Properties serviceProperties = new Properties();
        String dasIPAddress = "localhost";
        serviceProperties.setProperty("host", dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        return new GlassFishProvisionedService(serviceDescription,
                serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);
    }

    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Stop all the instances.
         */
        boolean stopSuccessful = false;
        String clusterName = serviceDescription.getVirtualClusterName();
        for (int currentClusterSize = domain.getClusterNamed(clusterName).
                getInstances().size(); currentClusterSize > 0 ; currentClusterSize--) {
        serviceDescription.setName(serviceName + "." + currentClusterSize);
            // customize the VM before stop???
            stopSuccessful =  super.stopService(serviceDescription) || stopSuccessful;
        }
        serviceDescription.setName(serviceName); // reset the service name.

        /**
         * Step 2. Stop the cluster/DAS.
         * 
         * For now, stop local cluster and jjust create a new ProvisionedService object
         * representing cluster so that State gets updated correctly.
         */
        commandRunner.run("stop-cluster", serviceDescription.getVirtualClusterName());
        new GlassFishProvisionedService(serviceDescription,
                new Properties(), ServiceStatus.STOPPED, null);

        /**
         * Step 3. Disable elastic service.
          */
        commandRunner.run("disable-auto-scaling", serviceName);

        return stopSuccessful;
    }


    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceDescription.getName());
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty("host", dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);

        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        serviceDescription.setVirtualClusterName(serviceDescription.getName());
        GlassFishProvisionedService gfps =new GlassFishProvisionedService(
                serviceDescription, serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);
        return gfps;
    }

    /**
     * @param serviceConsumer   Service that consumes the service provided by serviceProvider referred via service-reference
     * @param serviceProvider   Provisioned service like DB service or JMS service.
     * @param svcRef           Service Reference from GlassFish to that service.
     * @param beforeDeployment indicates if this association is happening before the
     */
    public void associateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                  ProvisionedService serviceProvider, boolean beforeDeployment, DeploymentContext dc) {
//        if (provisionedSvc instanceof DerbyProvisionedService) {
        if (svcRef.getServiceRefType().equals("javax.sql.DataSource") &&
	        serviceProvider.getServiceType().toString().equals("Database")  &&
            serviceConsumer.getServiceType().toString().equals("JavaEE")) {

            if (!beforeDeployment) return;

            // JDBC connection properties
            ServiceDescription serviceDescription = serviceProvider.getServiceDescription();
//                        (SimpleServiceDefinition) derbyProvisionedService.getServiceDefinition();
            Properties dbProperties = new Properties();
            if(svcRef.getProperties() != null){
                dbProperties.putAll(svcRef.getProperties());
            }
            if(serviceProvider.getServiceProperties() != null){
                dbProperties.putAll(serviceProvider.getServiceProperties());
                String serverName = serviceProvider.getServiceProperties().getProperty("host");
                String url = serviceProvider.getServiceProperties().getProperty("URL");
                if(serverName != null) {
                    dbProperties.setProperty("serverName", serverName);
                }
                if(url != null) {
                    dbProperties.setProperty("URL", url);
                }
            }
//                serviceDescription.getProperties();

            // Get the domain and cluster names.
//                SimpleServiceDefinition serviceDefinition =
//                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDesription();
            String serviceName = serviceConsumer.getServiceDescription().getName();
//                String domainName = glassfishProvisionedService.getServiceProperties().getProperty("domainName"); // serviceUtil.getDomainName(serviceName);
            //String clusterName = gfServiceUtil.getClusterName(serviceName);
            //String dasIPAddress = glassfishProvisionedService.getServiceProperties().getProperty("host"); // serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            String clusterName = gfServiceUtil.getClusterName(serviceName, serviceDescription.getAppName());
            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceConsumer.getServiceDescription().getName());

            String poolName = svcRef.getServiceRefName();
            String resourceName = svcRef.getServiceRefName();

            // Create JDBC resource and pool.
            // TODO :: delegate the pool creation to deployment backend.
            // TODO :: Decorate the archive with modified/newly_created META-INF/glassfish-resources.xml
            GlassFishProvisioner glassFishProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);
            glassFishProvisioner.createJdbcConnectionPool(dasIPAddress, clusterName,
                    dbProperties, poolName);
            glassFishProvisioner.createJdbcResource(dasIPAddress, clusterName,
                    poolName, resourceName);
        }
//        }

//        if (provisionedSvc instanceof GlassFishLBProvisionedService) {
        if (svcRef.getServiceRefType().equals("HTTP_LOAD_BALANCER")) {

//                SimpleServiceDefinition gfServiceDefinition =
//                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDesription();
            String appServerServiceName = serviceConsumer.getServiceDescription().getName();//gfServiceDefinition.getProperties().getProperty("servicename");
            //String domainName = glassfishProvisionedService.getServiceProperties().getProperty("domainName");//serviceUtil.getDomainName(appServerServiceName);
            //String clusterName = gfServiceUtil.getClusterName(appServerServiceName);
            //String dasIPAddress = glassfishProvisionedService.getServiceProperties().getProperty("host"); //serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            String clusterName = gfServiceUtil.getClusterName(appServerServiceName, serviceConsumer.getServiceDescription().getAppName());
            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceConsumer.getServiceDescription().getName());

            ApplicationServerProvisioner appServerProvisioner = provisionerUtil.getAppServerProvisioner(dasIPAddress);

//                GlassFishLBProvisionedService gfLBProvisionedService =
//                        (GlassFishLBProvisionedService) provisionedSvc;
//                SimpleServiceDefinition lbServiceDefinition = (SimpleServiceDefinition)
//                        gfLBProvisionedService.getServiceDescription();
            String lbServiceName = serviceProvider.getServiceDescription().getName();

            String domainName = domain.getProperty(Domain.DOMAIN_NAME_PROPERTY).getValue();
            if (beforeDeployment) {
                LBProvisioner lbProvisioner = provisionerUtil.getLBProvisioner();
                String lbIPAddress = gfServiceUtil.getIPAddress(lbServiceName, serviceConsumer.getServiceDescription().getAppName(), ServiceType.LOAD_BALANCER);
                lbProvisioner.associateApplicationServerWithLB(lbIPAddress, dasIPAddress, domainName);

                //restart
                lbProvisioner.stopLB(lbIPAddress);
                lbProvisioner.startLB(lbIPAddress);

                appServerProvisioner.associateLBWithApplicationServer(dasIPAddress, clusterName, lbIPAddress, lbServiceName);
            } else {
                appServerProvisioner.refreshLBConfiguration(dasIPAddress, lbServiceName);
            }
//            }

            }

            //if (svcRef.getServiceRefType().equals(JAVAEE_SERVICE_TYPE)) {
                //if (serviceConsumer instanceof GlassFishProvisionedService) {
                    if (beforeDeployment) {
                        GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceConsumer;
                        String clusterServiceName = gfServiceUtil.getClusterName(serviceConsumer.getName(), gfps.getServiceDescription().getAppName());
                        if (dc != null) { //TODO remove once "deploy-service" is made obselete
                            DeployCommandParameters ucp = dc.getCommandParameters(DeployCommandParameters.class);
                            ucp.target = clusterServiceName;
                        }
                  //  }
              //  }

        }
    }

    public void dissociateServices(ProvisionedService serviceConsumer, ServiceReference svcRef,
                                   ProvisionedService serviceProvider, boolean beforeUndeploy, DeploymentContext dc) {
        if (beforeUndeploy) {
            //if (serviceConsumer instanceof GlassFishProvisionedService) {
               // if (svcRef.getServiceRefType().equals(JAVAEE_SERVICE_TYPE)) {
                    GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceConsumer;
                    String serviceName = gfps.getServiceDescription().getName();
                    String clusterName = gfServiceUtil.getClusterName(serviceName, gfps.getServiceDescription().getAppName());

                    if (dc != null) { //TODO remove once "deploy-service" is made obselete
                        UndeployCommandParameters ucp = dc.getCommandParameters(UndeployCommandParameters.class);
                        ucp.target = clusterName;
                    }
                //}
            //}
        } else {
            if (svcRef.getServiceRefType().equals("javax.sql.DataSource") &&
                serviceProvider.getServiceType().toString().equals("Database") &&
                serviceConsumer.getServiceType().toString().equals("JavaEE")) {
                //if (serviceProvider instanceof GlassFishProvisionedService) {
                    GlassFishProvisionedService glassfishProvisionedService = (GlassFishProvisionedService) serviceConsumer;
                    String serviceName = glassfishProvisionedService.getServiceDescription().getName();
                    String clusterName = gfServiceUtil.getClusterName(serviceName, glassfishProvisionedService.getServiceDescription().getAppName());
                    String poolName = svcRef.getServiceRefName();
                    String resourceName = svcRef.getServiceRefName();

                    //TODO once glassfish-resources.xml is used, deleting resources and pools explicitly is not required.
                    String dasIPAddress = gfServiceUtil.getDASIPAddress(glassfishProvisionedService.getServiceDescription().getName());
                    GlassFishProvisioner glassFishProvisioner = (GlassFishProvisioner)
                            provisionerUtil.getAppServerProvisioner(dasIPAddress);
                    glassFishProvisioner.deleteJdbcResource(dasIPAddress, clusterName, resourceName);
                    glassFishProvisioner.deleteJdbcConnectionPool(dasIPAddress, poolName);
                //}
            }
        }
    }


    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
/*
        GlassFish provisionedGlassFish =
                glassfishProvisionedService.getProvisionedGlassFish();
//        SimpleServiceDefinition serviceDefinition =
//                (SimpleServiceDefinition) glassfishProvisionedService.getServiceDesription();
        ServiceDescription serviceDescription = glassfishProvisionedService.getServiceDescription();
        String serviceName = serviceDescription.getName();
        String clusterName = gfServiceUtil.getClusterName(serviceName, serviceDescription.getAppName());

        URI archive = cloudArchive.getURI();
        try {
            Deployer deployer = provisionedGlassFish.getDeployer();
            String appName = deployer.deploy(archive,
                    clusterName == null ? "" : "--target=" + clusterName); // TODO :: check this for standalone instances case.
            System.out.println(appName);
        } catch (GlassFishException e) {
            e.printStackTrace();
        }
        return null;
*/
        return null;
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        return provisionedSvc.getStatus().equals(ServiceStatus.RUNNING);
    }

    public ProvisionedService match(ServiceReference svcRef) {
        return null;
    }


    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive readableArchive, String appName) {
        HashSet<ServiceDescription> defs = new HashSet<ServiceDescription>();

        if (DeploymentUtils.isJavaEE(readableArchive, habitat)) {
            ServiceDescription sd = generateDefaultServiceDescription(appName);
            defs.add(sd);
        }
        return defs;
    }

    private ServiceDescription generateDefaultServiceDescription(String appName) {
        List<Property> characteristics = new ArrayList<Property>();
        characteristics.add(new Property("service-type", JAVAEE_SERVICE_TYPE));
        List<Property> configurations = new ArrayList<Property>();
        configurations.add(new Property(MIN_CLUSTER_PROPERTY_NAME, "2"));
        configurations.add(new Property(MAX_CLUSTER_PROPERTY_NAME, "4"));
        return new ServiceDescription(
                "gf-service-"+appName, appName, "lazy",
                new ServiceCharacteristics(characteristics), configurations);
    }

    @Override
    public ProvisionedService scaleService(ServiceDescription serviceDescription,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        if(scaleCount > 0) {
            scaleUpService(serviceDescription, scaleCount, allocStrategy);
        } else {
            scaleDownService(serviceDescription, -scaleCount);
        }
        // TODO :: Return the provisioned service with the new cluster shape.
        String dasIPAddress = "localhost"; // TODO :: change it when DAS is also provisioned separately.
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty("host", dasIPAddress);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        return new GlassFishProvisionedService(serviceDescription, serviceProperties,
                ServiceStatus.RUNNING, provisionedGlassFish);
    }

    private void scaleUpService(ServiceDescription serviceDescription,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        // Check for max.clustersize bound.
        String serviceName = serviceDescription.getName();
        String clusterName = serviceDescription.getVirtualClusterName();
        int currentClusterSize = domain.getClusterNamed(clusterName).getInstances().size();
        int maxClusterSize = Integer.parseInt(
                serviceDescription.getConfiguration("max.clustersize"));
        if (currentClusterSize + scaleCount > maxClusterSize) {
            String errMsg = "\nUnable to scale the service beyond the " +
                    "maximum size [" + maxClusterSize + "], " +
                    "current size is [" + currentClusterSize + "]";
            throw new ServiceProvisioningException(errMsg);
        }
        for (int i = currentClusterSize+1; scaleCount > 0 ; scaleCount--, i++) {
            serviceDescription.setName(serviceName + "." + i);
            ProvisionedService scaledService = super.createService(serviceDescription,
                    allocStrategy, null);
            // configure the VM after creating.
        }
        serviceDescription.setName(serviceName);
    }

    private void scaleDownService(ServiceDescription serviceDescription,
                                           int scaleCount) throws ServiceProvisioningException {
        // Check for max.clustersize bound.
        // make sure we don't scale down below min.clustersize.
        String serviceName = serviceDescription.getName();
        String clusterName = serviceDescription.getVirtualClusterName();
        int currentClusterSize = domain.getClusterNamed(clusterName).getInstances().size();
        int minClusterSize = Integer.parseInt(
                serviceDescription.getConfiguration("min.clustersize"));
        if (currentClusterSize - scaleCount < minClusterSize) {
            String errMsg = "\nUnable to scale the service below the " +
                    "minimum required size [" + minClusterSize + "], " +
                    "current size is [" + currentClusterSize + "]";
            throw new ServiceProvisioningException(errMsg);
        }
        for (int i = currentClusterSize; scaleCount > 0 ; scaleCount--, i--) {
            serviceDescription.setName(serviceName + "." + i);
            // perform operation on VM before deleting??
            boolean deleteSuccessful = super.deleteService(serviceDescription);
        }
        serviceDescription.setName(serviceName);
    }

    @Override
    public boolean reconfigureServices(ProvisionedService oldPS,
            ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of Service " +
                "not supported in this release");
    }

    @Override
    public boolean reassociateServices(ProvisionedService svcConsumer,
            ProvisionedService oldSvcProvider, ProvisionedService newSvcProvider,
            ServiceOrchestrator.ReconfigAction reason) {
        //no-op
        throw new UnsupportedOperationException("Reassociation of Service " +
                "not supported in this release");
    }

}
