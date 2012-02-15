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
import org.glassfish.javaee.core.deployment.JavaEEDeploymentUtils;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.javaee.core.deployment.ApplicationHolder;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.gfplugin.customizer.DASProvisioner;
import org.glassfish.paas.gfplugin.customizer.InstanceProvisioner;
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
import org.glassfish.virtualization.spi.VirtualMachine;
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
 * @author Bhavanishankar S
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

    @Inject
    private DASProvisioner dasProvisioner;

    @Inject
    private InstanceProvisioner instanceProvisioner;

    private static Logger logger = Logger.getLogger(GlassFishPlugin.class.getName());

    public JavaEEServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return JavaEEDeploymentUtils.isJavaEE(cloudArchive, habitat);
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
        GlassFishProvisionedService gfps = null;

        if (provisionDAS) {
            ProvisioningFuture future = createService(serviceDescription);
            gfps = dasProvisioner.provision(future.get());
        } else { // TODO :: remove this else block when DAS provisioning becomes default.
            String dasIPAddress = LOCALHOST;
            Properties serviceProperties = new Properties();
            serviceProperties.setProperty(MIN_CLUSTERSIZE,
                    serviceDescription.getConfiguration(MIN_CLUSTERSIZE));
            serviceProperties.setProperty(MAX_CLUSTERSIZE,
                    serviceDescription.getConfiguration(MAX_CLUSTERSIZE));
            GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);
            GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
            gfps = new GlassFishProvisionedService(
                    serviceDescription, serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);
        }

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
            gfps.addChildService(provisionedService);
            // further customize the provisioned node/instance
        }

        if(provisionDAS) {
            instanceProvisioner.provision(gfps, gfps.getChildServices().toArray(new ProvisionedService[]{}));
        }

        /**
         * Step 3. Set the target to the newly created cluster.
         * TODO what if someone requests for multiple GlassFish services ? As of now, the last one is considered as deployment target.
         */
/*        String clusterName = serviceName;
        DeploymentContext dc = pdc.getDeploymentContext();
        if(dc != null){
            DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
            dcp.target = clusterName;
        }*/

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
         * Step 1. Create a ProvisionedService object representing the DAS/cluster.
         */
        Properties serviceProperties = new Properties();
        String dasIPAddress = LOCALHOST;
        serviceProperties.setProperty(HOST, dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        GlassFishProvisionedService service =  new GlassFishProvisionedService(serviceDescription,
                serviceProperties, ServiceStatus.RUNNING, provisionedGlassFish);

        /**
         * Step 2. Start all the instances.
         */
        // Start all the instances
        String clusterName = serviceDescription.getVirtualClusterName();
        for (int currentClusterSize = domain.getClusterNamed(clusterName).
                getInstances().size(); currentClusterSize > 0 ; currentClusterSize--) {
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + currentClusterSize);
            ProvisionedService provisionedService = super.startService(sd);
            service.addChildService(provisionedService);
            // customize the VM after start???
        }
        //serviceDescription.setName(serviceName); // reset the name.

        /**
         * Step 3. Start cluster. TODO :: remove start-cluster call once LocalGlassFishTemplateCustomizer's start is fixed.
         */
        commandRunner.run(START_CLUSTER, serviceDescription.getVirtualClusterName());

        /**
         * Step 4. Enable elastic service.
         */
        commandRunner.run(ENABLE_AUTO_SCALING, serviceName);


        fireServiceChangeEvent(ServiceChangeEvent.Type.STARTED, service);

        return service;
    }

    public boolean stopService(ProvisionedService provisionedService, ServiceInfo serviceInfo) {
        ServiceDescription serviceDescription = provisionedService.getServiceDescription();
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
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + currentClusterSize);

            // customize the VM before stop???
            stopSuccessful =  super.stopService(sd) || stopSuccessful;
            for(org.glassfish.paas.orchestrator.service.spi.Service childService : provisionedService.getChildServices()){
                ProvisionedService ps = (ProvisionedService)childService;
                if(sd.getName().equals(ps.getName())){
                    if(stopSuccessful){
                        ps.setStatus(ServiceStatus.STOPPED);
                    }
                }
            }
        }
        //serviceDescription.setName(serviceName); // reset the service name.

        /**
         * Step 3. Stop the cluster/DAS.
         * 
         * For now, stop local cluster and jjust create a new ProvisionedService object
         * representing cluster so that State gets updated correctly.
         */
        commandRunner.run(STOP_CLUSTER, serviceDescription.getVirtualClusterName());
        //ProvisionedService service = new GlassFishProvisionedService(serviceDescription,
        //        new Properties(), ServiceStatus.STOPPED, null);
        provisionedService.setStatus(ServiceStatus.STOPPED);

        fireServiceChangeEvent(ServiceChangeEvent.Type.STOPPED, provisionedService);

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
        if(serviceInfo.getChildServices() != null){
            for(ServiceInfo childService : serviceInfo.getChildServices()){
                ServiceDescription childSD = new ServiceDescription(serviceDescription);
                childSD.setName(childService.getServiceName());
                Properties childServiceProperties = new Properties();
                childServiceProperties.putAll(serviceInfo.getProperties());
                GlassFishProvisionedService childPS =
                        new GlassFishProvisionedService(childSD, childServiceProperties, ServiceStatus.RUNNING, null);
                gfps.addChildService(childPS);
            }
        }
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
        if (JDBC_DATASOURCE.equals(svcRef.getType()) &&
	        serviceProvider.getServiceType().toString().equals(DATABASE_SERVICE_TYPE)  &&
            serviceConsumer.getServiceType().toString().equals(JAVAEE_SERVICE_TYPE)) {

            if (!beforeDeployment) return;

            Properties databaseServiceProperties = new Properties();
            if(svcRef.getProperties() != null){
                databaseServiceProperties.putAll(svcRef.getProperties());
            }
            if(serviceProvider.getServiceProperties() != null){
                databaseServiceProperties.putAll(serviceProvider.getServiceProperties());
                String serverName = serviceProvider.getServiceProperties().getProperty(HOST);
                String url = serviceProvider.getServiceProperties().getProperty(JDBC_URL);
                if(serverName != null) {
                    databaseServiceProperties.setProperty(JDBC_SERVERNAME, serverName);
                }
                if (url != null) {
                    databaseServiceProperties.setProperty(JDBC_URL, url);
                }
            }
            String serviceName = serviceConsumer.getServiceDescription().getName();
            String clusterName = serviceName;
            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceConsumer.getServiceDescription().getName());

            String poolName = svcRef.getName();
            String resourceName = svcRef.getName();

            if(processResourcesXML(dc, resourceName, databaseServiceProperties)) {
                // Deployment backend will take care of creating required jdbc pool
                // and resources using the generated deployment plan.
            } else {
                // Create global JDBC pool and resource.
                GlassFishProvisioner glassFishProvisioner = (GlassFishProvisioner)
                        provisionerUtil.getAppServerProvisioner(dasIPAddress);
                glassFishProvisioner.createJdbcConnectionPool(dasIPAddress, clusterName,
                        databaseServiceProperties, poolName);
                glassFishProvisioner.createJdbcResource(dasIPAddress, clusterName,
                        poolName, resourceName);
            }
        }
    }

    /**
     * Process glassfish-resources.xml and substitute the service-name property
     * in a resource with service's actual coordinates.
     *
     * @param dc PaaS deployment context
     * @param resourceName name of the resource that needs to be processed in glassfish-resources.xml
     * @param databaseServiceProperties Service properties that contains the actual co-ordinates of the Database service.
     *
     * @return TRUE if glassfish-resources.xml had a resource referring to a service
     * and it has been successfully substituted with actual service co-ordinates, FALSE otherwise.
     */
    private boolean processResourcesXML(PaaSDeploymentContext dc,
                                        String resourceName,
                                        Properties databaseServiceProperties) {
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
            boolean isDeploymentPlanComplete = true;
            Resource processedResource = null;
            for(Resource res : nonConnectorResources) {
                if(res.getType().equals(JDBC_RESOURCE)) {
                    if(res.getAttributes().get(JNDI_NAME).equals(resourceName)) {
                        Resource connPool = archiveProcessor.getConnectionPool(
                                res, resourceXmlParsers);
                        parser = resourceXmlParsers.get(connPool);
                        // Clone the jdbc-connection-pool resource and modify
                        // "service-name" property with its actual co-ordinates
                        databaseServiceProperties.remove(HOST);

                        // create the modified jdbc-connection-pool which has actual service' co-ordinates.
                        Resource modifiedConnPool = new Resource(connPool.getType());
                        modifiedConnPool.setDescription(connPool.getDescription());

                        // Set attributes for jdbc-connection-pool element
                        modifiedConnPool.getAttributes().putAll(connPool.getAttributes());
                        modifiedConnPool.getAttributes().put(JDBC_DS_CLASSNAME,
                                databaseServiceProperties.remove(CLASSNAME));
                        modifiedConnPool.getAttributes().put(JDBC_DS_RESTYPE,
                                databaseServiceProperties.remove(RESOURCE_TYPE));

                        // Set properties for jdbc-connection-pool element
                        modifiedConnPool.getProperties().putAll(connPool.getProperties());
                        modifiedConnPool.getProperties().putAll(databaseServiceProperties);
                        modifiedConnPool.getProperties().remove(SERVICE_NAME);

                        parser.updateDocumentNode(connPool, modifiedConnPool);

                        processedResource = res;
//                            break; // jdbc-connection-pool with a given name is unique in glassfish-resources.xml
                    } else { // see if there is any other resource that refers to a service
                        Resource connPool = archiveProcessor.getConnectionPool(
                                res, resourceXmlParsers);
                        if (connPool.getProperties().getProperty(SERVICE_NAME) != null) {
                            isDeploymentPlanComplete = false;
                        }
                    }
                }
            }

            if (processedResource != null) { // glassfish-resources.xml had a resource referring to a service.
                // When associateService is called next time, make sure we won't process
                // the already processed resource. Hence remove it from the datastructures.
                resourceXmlParsers.remove(processedResource);
                nonConnectorResources.remove(processedResource);

                // generate deployment plan only when all resources with service
                // references are substituted with service's actual co-ordinates.
                if (isDeploymentPlanComplete) {
                    try {
                        File dir = File.createTempFile(DEPLOYMENT_PLAN_DIR, null);
                        dir.delete();
                        dir.mkdirs();
                        dir.deleteOnExit();
                        String xmlName = parser.getResourceFile().getName();
                        parser.persist(new File(dir, xmlName));
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
                }
                return true;
            }
        }
        return false;
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
                //    GlassFishProvisionedService gfps = (GlassFishProvisionedService) serviceConsumer;
                 //   String serviceName = gfps.getServiceDescription().getName();
                    //String clusterName = gfServiceUtil.getClusterName(serviceName, gfps.getServiceDescription().getAppName());
                  //  String clusterName = serviceName;

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
                    //String clusterName = gfServiceUtil.getClusterName(serviceName, glassfishProvisionedService.getServiceDescription().getAppName());
                    String clusterName = serviceName;
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
        org.glassfish.paas.orchestrator.service.spi.Service service = null; // TODO :: should be passed in as argument.
        if (service instanceof GlassFishProvisionedService) {
            GlassFishProvisionedService gfps = (GlassFishProvisionedService) service;
            try {
                File archive = new File(cloudArchive.getURI());
                logger.info("Deploying " + archive + " using GlassFish plugin");
                VirtualMachine vm = (VirtualMachine) service.getServiceProperties().get("vm");
                vm.upload(archive, new File("/tmp"));
                gfps.getProvisionedGlassFish().getCommandRunner().run("deploy",
                        "--cluster=" + gfps.getServiceDescription().getVirtualClusterName(),
                        "/tmp/" + archive.getName());
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } else {
            logger.warning("Unable to deploy " + cloudArchive + " using GlassFish plugin. " +
                    "The service is not an instance of GlassFishProvisionedService");
        }
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

        if (JavaEEDeploymentUtils.isJavaEE(readableArchive, habitat)) {
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
    public ProvisionedService scaleService(ProvisionedService provisionedService,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        if(scaleCount > 0) {
            scaleUpService(provisionedService, scaleCount, allocStrategy);
        } else {
            scaleDownService(provisionedService, -scaleCount);
        }
    /*
        // TODO :: Return the provisioned service with the new cluster shape.
        String dasIPAddress = LOCALHOST; // TODO :: change it when DAS is also provisioned separately.
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty(HOST, dasIPAddress);
        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        return new GlassFishProvisionedService(serviceDescription, serviceProperties,
                ServiceStatus.RUNNING, provisionedGlassFish);
    */
        return provisionedService;
    }

    private void scaleUpService(ProvisionedService provisionedService,
                                           int scaleCount, AllocationStrategy allocStrategy) {
        // Check for max.clustersize bound.
        ServiceDescription serviceDescription = provisionedService.getServiceDescription();
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
        List<ProvisioningFuture> futures = new ArrayList<ProvisioningFuture>();
        for (int i = currentClusterSize+1; scaleCount > 0 ; scaleCount--, i++) {
            ServiceDescription sd = new ServiceDescription(serviceDescription);
            sd.setName(serviceName + "." + (i+1));
            futures.add(createService(sd, allocStrategy, null)); // parallely create the nodes/instances.
        }
        // wait for the completion of node/instance creations.
        for(ProvisioningFuture future : futures) {
            ProvisionedService ps = future.get();
            provisionedService.getChildServices().add(ps);
            // further customize the provisioned node/instance
        }
    }

    private void scaleDownService(ProvisionedService provisionedService,
                                           int scaleCount) throws ServiceProvisioningException {
        // Check for max.clustersize bound.
        // make sure we don't scale down below min.clustersize.
        ServiceDescription serviceDescription = provisionedService.getServiceDescription();
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
            if(deleteSuccessful){
                org.glassfish.paas.orchestrator.service.spi.Service serviceToPurge = null;
                for(org.glassfish.paas.orchestrator.service.spi.Service service : provisionedService.getChildServices()){
                    if(service.getName().equals(sd.getName())){
                        serviceToPurge = service;
                        break;
                    }
                }
                if(serviceToPurge != null){
                    provisionedService.getChildServices().remove(serviceToPurge);
                }
            }
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
