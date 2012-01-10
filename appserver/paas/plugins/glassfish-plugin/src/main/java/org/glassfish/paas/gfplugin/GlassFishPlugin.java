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

package org.glassfish.paas.gfplugin;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.archivist.ApplicationArchivist;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.deploy.shared.DeploymentPlanArchive;
import com.sun.enterprise.util.zip.ZipWriter;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.JavaEEServiceType;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceChangeEvent;
import org.glassfish.paas.orchestrator.service.spi.ServiceProvisioningException;
import org.glassfish.paas.spe.common.ProvisioningFuture;
import org.glassfish.paas.spe.common.ServiceProvisioningEngineBase;
import org.glassfish.resources.admin.cli.ResourcesXMLParser;
import org.glassfish.resources.api.Resource;
import org.glassfish.virtualization.spi.AllocationStrategy;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author bhavanishankar@java.net
 */
@Service
@Scoped(PerLookup.class)
public class GlassFishPlugin extends ServiceProvisioningEngineBase<JavaEEServiceType>
        implements GlassFishPluginConstants {

    @Inject
    private GlassFishCloudArchiveProcessor archiveProcessor;

    @Inject
    ArchivistFactory archivistFactory;

    @Inject
    ArchiveFactory archiveFactory;

    @Inject
    private CommandRunner commandRunner;

    @Inject
    private GlassFishServiceUtil gfServiceUtil;

    @Inject
    private org.glassfish.paas.gfplugin.cli.ProvisionerUtil provisionerUtil;

    @Inject
    private Domain domain;

    @Inject
    private Habitat habitat;

    private static Logger logger = Logger.getLogger(GlassFishPlugin.class.getName());

    public JavaEEServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return DeploymentUtils.isJavaEE(cloudArchive, habitat);
    }

    public boolean handles(ServiceDescription serviceDescription) {
        return false;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        if(referenceType.equals(JAVAEE_SERVICE_TYPE)){
            return true;
        }
        //GlassFish plugin would not be able to support any other reference types
        return false;
    }

/*
    public Set<ServiceReference> getServiceReferences(File archive, String appName,
                                                      PaaSDeploymentContext dc) {
        try {
            return getServiceReferences(appName, archiveFactory.openArchive(archive), dc);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
*/

    public Set<ServiceReference> getServiceReferences(String appName,
                                                      ReadableArchive cloudArchive, PaaSDeploymentContext dc) {
        // Parse the archive and figure out resource references.
        return archiveProcessor.getServiceReferences(cloudArchive, appName, dc);
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        return generateDefaultServiceDescription(appName);
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc) {

        ProvisionedService service = getProvisionedService(serviceDescription);

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
        commandRunner.run(DELETE_ELASTIC_SERVICE, serviceName);

        fireServiceChangeEvent(ServiceChangeEvent.Type.DELETED, service);

        return deleteSuccessful;
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription,
                                               PaaSDeploymentContext pdc) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Provision DAS and cluster
         * For now, just create provisioned service object pointing to the local DAS
         */
        String dasIPAddress = LOCALHOST;
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty(MIN_CLUSTERSIZE,
                serviceDescription.getConfiguration(MIN_CLUSTERSIZE));
        serviceProperties.setProperty(MAX_CLUSTERSIZE,
                serviceDescription.getConfiguration(MAX_CLUSTERSIZE));
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        GlassFishProvisionedService gfps = new GlassFishProvisionedService(
                serviceDescription, serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);

        /**
         * Step 2. Create as many GlassFish service instances as min.clustersize
         */
        int minClusterSize = Integer.parseInt(
                serviceDescription.getConfiguration(MIN_CLUSTERSIZE));
        List<ProvisioningFuture> futures  = new ArrayList();
        for (int i = 0; i < minClusterSize; i++) {
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + (i+1));
            futures.add(createService(sd)); // parallely create the nodes/instances.
        }
        // wait for the completion of node/instance creations.
        for(ProvisioningFuture future : futures) {
            ProvisionedService provisionedService = future.get();
            // further customize the provisioned node/instance
        }

        /**
         * Step 3. Set the target to the newly created cluster.
         * TODO what if someone requests for multiple GlassFish services ? As of now, the last one is considered as deployment target.
         */
        String clusterName = serviceName;
        DeploymentContext dc = pdc.getDeploymentContext();
        if(dc != null){
            DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
            dcp.target = clusterName;
        }

        /**
         * Step 4. Create elastic service.
         */
        commandRunner.run(CREATE_ELASTIC_SERVICE,
                "--min=" + serviceDescription.getConfiguration(MIN_CLUSTERSIZE),
                "--max=" + serviceDescription.getConfiguration(MAX_CLUSTERSIZE),
                serviceName);

        fireServiceChangeEvent(ServiceChangeEvent.Type.CREATED, gfps);

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
        commandRunner.run(START_CLUSTER, serviceDescription.getVirtualClusterName());

        /**
         * Step 3. Enable elastic service.
         */
        commandRunner.run(ENABLE_AUTO_SCALING, serviceName);

        /**
         * Step 4. Create a ProvisionedService object representing the DAS/cluster.
         */
        Properties serviceProperties = new Properties();
        String dasIPAddress = LOCALHOST;
        serviceProperties.setProperty(HOST, dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        ProvisionedService service =  new GlassFishProvisionedService(serviceDescription,
                serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);

        fireServiceChangeEvent(ServiceChangeEvent.Type.STARTED, service);

        return service;
    }

    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        String serviceName = serviceDescription.getName();
        /**
         * Step 1. Disable elastic service.
          */
        commandRunner.run(DISABLE_AUTO_SCALING, serviceName);

        /**
         * Step 2. Stop all the instances.
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
         * Step 3. Stop the cluster/DAS.
         * 
         * For now, stop local cluster and jjust create a new ProvisionedService object
         * representing cluster so that State gets updated correctly.
         */
        commandRunner.run(STOP_CLUSTER, serviceDescription.getVirtualClusterName());
        ProvisionedService service = new GlassFishProvisionedService(serviceDescription,
                new Properties(), ServiceStatus.STOPPED, null);

        fireServiceChangeEvent(ServiceChangeEvent.Type.STOPPED, service);

        return stopSuccessful;
    }


    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceDescription.getName());
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty(HOST, dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);

        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        //serviceDescription.setVirtualClusterName(serviceDescription.getName());
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
    public void associateServices(org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer, ServiceReference svcRef,
                                  org.glassfish.paas.orchestrator.service.spi.Service serviceProvider, boolean beforeDeployment, PaaSDeploymentContext dc) {
//        if (provisionedSvc instanceof DerbyProvisionedService) {
        if (JDBC_DATASOURCE.equals(svcRef.getType()) &&
	        serviceProvider.getServiceType().toString().equals(DATABASE_SERVICE_TYPE)  &&
            serviceConsumer.getServiceType().toString().equals(JAVAEE_SERVICE_TYPE)) {

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
                String serverName = serviceProvider.getServiceProperties().getProperty(HOST);
                String url = serviceProvider.getServiceProperties().getProperty(JDBC_URL);
                if(serverName != null) {
                    dbProperties.setProperty(JDBC_SERVERNAME, serverName);
                }
                if(url != null) {
                    dbProperties.setProperty(JDBC_URL, url);
                }
            }
//                serviceDescription.getProperties();

            // Get the domain and cluster names.
//                SimpleServiceDefinition serviceDefinition =
//                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDesription();
            String serviceName = serviceConsumer.getServiceDescription().getName();
//                String domainName = glassfishProvisionedService.getServiceProperties().getProperty("domainName"); // serviceUtil.getDomainName(serviceName);
            //String clusterName = gfServiceUtil.getClusterName(serviceName);
            //String dasIPAddress = glassfishProvisionedService.getServiceProperties().getProperty(HOST); // serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
            String clusterName = gfServiceUtil.getClusterName(serviceName, serviceDescription.getAppName());
            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceConsumer.getServiceDescription().getName());

            String poolName = svcRef.getName();
            String resourceName = svcRef.getName();

            // If the user has supplied glassfish-resources.xml with 'service-name' in it,
            // then substitute 'service-name' with actual co-ordinates of the service.
            if(dc != null && dc.getDeploymentContext() != null) {
                Map<Resource, ResourcesXMLParser> resourceXmlParsers =
                        dc.getDeploymentContext().getTransientAppMetaData(RESOURCE_XML_PARSERS, Map.class);
                List<Resource> nonConnectorResources =
                        dc.getDeploymentContext().getTransientAppMetaData(NON_CONNECTOR_RESOURCES, List.class);
                ResourcesXMLParser parser = null;
                // Find correct jdbc connection pool corresponding to the referenced resource
                // and substitute values for 'service-name' property.
                for(Resource res : nonConnectorResources) {
                    if(res.getType().equals(JDBC_RESOURCE)) {
                        if(res.getAttributes().get(JNDI_NAME).equals(resourceName)) {
                            Resource connPool = archiveProcessor.getConnectionPool(
                                    res, resourceXmlParsers);
                            parser = resourceXmlParsers.get(connPool);
                            // Clone the jdbc-connection-pool resource and modify
                            // "service-name" property with its actual co-ordinates
                            Resource modifiedConnPool = null;
                            modifiedConnPool = new Resource(connPool.getType());
                            modifiedConnPool.setDescription(connPool.getDescription());
                            modifiedConnPool.getAttributes().putAll(connPool.getAttributes());
                            modifiedConnPool.getProperties().putAll(connPool.getProperties());
                            modifiedConnPool.getProperties().putAll(dbProperties);
                            modifiedConnPool.getProperties().remove(SERVICE_NAME);
                            parser.updateDocumentNode(connPool, modifiedConnPool);
                            break; // jdbc-connection-pool with a given name is unique in glassfish-resources.xml
                        }
                    }
                }
                if (parser != null) { // generate deployment plan
                    File dir = new File(System.getProperty(TMR_DIR),
                            serviceName + DEPLOYMENT_PLAN_DIR);
                    dir.mkdirs();
                    String xmlName = parser.getResourceFile().getName();
                    parser.persist(new File(dir, xmlName));
                    try {
                        ZipWriter zipWriter = new ZipWriter(dir + JAR_EXTN, dir.getAbsolutePath());
                        zipWriter.write();
                        DeployCommandParameters dcp = dc.getDeploymentContext().
                                getCommandParameters(DeployCommandParameters.class);
                        dcp.deploymentplan = new File(dir + JAR_EXTN);
                        dcp.deploymentplan.deleteOnExit();
                        /**
                         * TODO ->
                         * Deployment backend itself should handle the deployment plan.
                         * But currently deployment backend does not process the plan after APPICATION_PREPARE phase.
                         * Instead it processes the DP only during the very beginning of the deployment.
                         * so the DP set by this code is ignored by the deployment backend.
                         * So, for now, we will process it manually here itself.
                         */
                        handleDeploymentPlan(dc.getDeploymentContext());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return; // we are done, deployment backend will take care of creating required jdbc pool and resources using the generated deployment plan.
                }
            }

            // Create global JDBC pool and resource.
            GlassFishProvisioner glassFishProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);
            glassFishProvisioner.createJdbcConnectionPool(dasIPAddress, clusterName,
                    dbProperties, poolName);
            glassFishProvisioner.createJdbcResource(dasIPAddress, clusterName,
                    poolName, resourceName);
        }
//        }

//        if (provisionedSvc instanceof GlassFishLBProvisionedService) {
/*
        if (svcRef.getServiceRefType().equals("HTTP_LOAD_BALANCER")) {

//                SimpleServiceDefinition gfServiceDefinition =
//                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDesription();
            String appServerServiceName = serviceConsumer.getServiceDescription().getName();//gfServiceDefinition.getProperties().getProperty("servicename");
            //String domainName = glassfishProvisionedService.getServiceProperties().getProperty("domainName");//serviceUtil.getDomainName(appServerServiceName);
            //String clusterName = gfServiceUtil.getClusterName(appServerServiceName);
            //String dasIPAddress = glassfishProvisionedService.getServiceProperties().getProperty(HOST); //serviceUtil.getIPAddress(domainName, ServiceUtil.SERVICE_TYPE.APPLICATION_SERVER);
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
*/

            //if (svcRef.getServiceRefType().equals(JAVAEE_SERVICE_TYPE)) {
                //if (serviceConsumer instanceof GlassFishProvisionedService) {
         /*           if (beforeDeployment) {
                        GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceConsumer;
                        String clusterServiceName = gfServiceUtil.getClusterName(serviceConsumer.getName(), gfps.getServiceDescription().getAppName());
                        if (dc != null) { //TODO remove once "deploy-service" is made obselete
                            DeployCommandParameters ucp = dc.getCommandParameters(DeployCommandParameters.class);
                            ucp.target = clusterServiceName;
                        }
                  //  }
              //  }

        }*/
    }

    // This method is copied from DolProvider. This method is not used when glassfish-resources.xml is modified inline.
    protected void handleDeploymentPlan(DeploymentContext dc) throws IOException {
        DeployCommandParameters params =
                dc.getCommandParameters(DeployCommandParameters.class);
        File deploymentPlan = params.deploymentplan;
        ClassLoader cl = dc.getClassLoader(); // cl might be null, but it is not used for the operations done in this method.
        ReadableArchive sourceArchive = dc.getSource();
        Archivist archivist = archivistFactory.getArchivist(
                sourceArchive, cl);
        ApplicationHolder holder = dc.getModuleMetaData(ApplicationHolder.class);
        //Note in copying of deployment plan to the portable archive,
        //we should make sure the manifest in the deployment plan jar
        //file does not overwrite the one in the original archive
        if (deploymentPlan != null) {
            DeploymentPlanArchive dpa = new DeploymentPlanArchive();
            dpa.setParentArchive(sourceArchive);
            dpa.open(deploymentPlan.toURI());
            // need to revisit for ear case
            WritableArchive targetArchive = archiveFactory.createArchive(
                sourceArchive.getURI());
            if (archivist instanceof ApplicationArchivist) {
                ((ApplicationArchivist)archivist).copyInto(holder.app, dpa, targetArchive, false);
            } else {
               archivist.copyInto(dpa, targetArchive, false);
            }
        }
    }

    public void dissociateServices(org.glassfish.paas.orchestrator.service.spi.Service serviceConsumer, ServiceReference svcRef,
                                   org.glassfish.paas.orchestrator.service.spi.Service serviceProvider, boolean beforeUndeploy, PaaSDeploymentContext dc) {
        if (beforeUndeploy) {
            //if (serviceConsumer instanceof GlassFishProvisionedService) {
               // if (svcRef.getServiceRefType().equals(JAVAEE_SERVICE_TYPE)) {
                    GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceConsumer;
                    String serviceName = gfps.getServiceDescription().getName();
                    String clusterName = gfServiceUtil.getClusterName(serviceName, gfps.getServiceDescription().getAppName());

                    /*if (dc != null) { //TODO remove once "deploy-service" is made obselete
                        UndeployCommandParameters ucp = dc.getCommandParameters(UndeployCommandParameters.class);
                        ucp.target = clusterName;
                    }*/
                //}
            //}
        } else {
            if (svcRef.getType().equals(JDBC_DATASOURCE) &&
                serviceProvider.getServiceType().toString().equals(DATABASE_SERVICE_TYPE) &&
                serviceConsumer.getServiceType().toString().equals(JAVAEE_SERVICE_TYPE)) {
                //if (serviceProvider instanceof GlassFishProvisionedService) {
                    GlassFishProvisionedService glassfishProvisionedService = (GlassFishProvisionedService) serviceConsumer;
                    String serviceName = glassfishProvisionedService.getServiceDescription().getName();
                    String clusterName = gfServiceUtil.getClusterName(serviceName, glassfishProvisionedService.getServiceDescription().getAppName());
                    String poolName = svcRef.getName();
                    String resourceName = svcRef.getName();

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
        characteristics.add(new Property(SERVICE_TYPE, JAVAEE_SERVICE_TYPE));
        List<Property> configurations = new ArrayList<Property>();
        configurations.add(new Property(MIN_CLUSTERSIZE, DEFAULT_MIN_CLUSTERSIZE));
        configurations.add(new Property(MAX_CLUSTERSIZE, DEFAULT_MAX_CLUSTERSIZE));
        return new ServiceDescription(
                "gf-service-"+appName, appName, INIT_TYPE_LAZY,
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
        String dasIPAddress = LOCALHOST; // TODO :: change it when DAS is also provisioned separately.
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty(HOST, dasIPAddress);
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
                serviceDescription.getConfiguration(MAX_CLUSTERSIZE));
        if (currentClusterSize + scaleCount > maxClusterSize) {
            String errMsg = "\nUnable to scale the service beyond the " +
                    "maximum size [" + maxClusterSize + "], " +
                    "current size is [" + currentClusterSize + "]";
            throw new ServiceProvisioningException(errMsg);
        }
        List<ProvisioningFuture> futures = new ArrayList();
        for (int i = currentClusterSize+1; scaleCount > 0 ; scaleCount--, i++) {
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + (i+1));
            futures.add(createService(sd, allocStrategy, null)); // parallely create the nodes/instances.
        }
        // wait for the completion of node/instance creations.
        for(ProvisioningFuture future : futures) {
            ProvisionedService provisionedService = future.get();
            // further customize the provisioned node/instance
        }
    }

    private void scaleDownService(ServiceDescription serviceDescription,
                                           int scaleCount) throws ServiceProvisioningException {
        // Check for max.clustersize bound.
        // make sure we don't scale down below min.clustersize.
        String serviceName = serviceDescription.getName();
        String clusterName = serviceDescription.getVirtualClusterName();
        int currentClusterSize = domain.getClusterNamed(clusterName).getInstances().size();
        int minClusterSize = Integer.parseInt(
                serviceDescription.getConfiguration(MIN_CLUSTERSIZE));
        if (currentClusterSize - scaleCount < minClusterSize) {
            String errMsg = "\nUnable to scale the service below the " +
                    "minimum required size [" + minClusterSize + "], " +
                    "current size is [" + currentClusterSize + "]";
            throw new ServiceProvisioningException(errMsg);
        }
        for (int i = currentClusterSize; scaleCount > 0 ; scaleCount--, i--) {
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + (i+1));
            // perform operation on VM before deleting??
            boolean deleteSuccessful = super.deleteService(sd);
        }
    }

    @Override
    public boolean reconfigureServices(ProvisionedService oldPS,
            ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of Service " +
                "not supported in this release");
    }

    @Override
    public boolean reassociateServices(org.glassfish.paas.orchestrator.service.spi.Service svcConsumer,
            org.glassfish.paas.orchestrator.service.spi.Service oldSvcProvider,
            org.glassfish.paas.orchestrator.service.spi.Service newSvcProvider,
            ServiceOrchestrator.ReconfigAction reason) {
        //no-op
        throw new UnsupportedOperationException("Reassociation of Service " +
                "not supported in this release");
    }

}
