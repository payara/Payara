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
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ProvisionerUtil;
import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.orchestrator.service.JavaEEServiceType;
import org.glassfish.paas.orchestrator.service.metadata.Property;
import org.glassfish.paas.orchestrator.service.metadata.ServiceCharacteristics;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
public class GlassFishPlugin implements Plugin<JavaEEServiceType> {

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

    public static final String JAVAEE_SERVICE_TYPE = "JavaEE";

    private static Logger logger = Logger.getLogger(GlassFishPlugin.class.getName());

    // TODO :: how can plugin hold the reference to the glassfish provisioned service?
    // TODO :: Plugin should be stateless, and its job is just to configure the service(s)
    private GlassFishProvisionedService glassfishProvisionedService;

    public JavaEEServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return true;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        /*if(referenceType.equals(JAVAEE_SERVICE_TYPE)){
            return true;
        }*/
        //GlassFish plugin would not be able to support any other reference types
        return false;
    }

    public Set<ServiceReference> getServiceReferences(File archive) {
        try {
            return getServiceReferences(archiveFactory.openArchive(archive));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Set<ServiceReference> getServiceReferences(
            ReadableArchive cloudArchive) {
        // Parse the archive and figure out resource references.
        return archiveProcessor.getServiceReferences(cloudArchive);
    }

    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        return null;
    }

    public boolean unprovisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
        String appNameParam = "";
        if (serviceDescription.getAppName() != null) {
            appNameParam = "--appname=" + serviceDescription.getAppName();
        }
        CommandResult result = commandRunner.run("_delete-glassfish-service",
                "--waitforcompletion=true", appNameParam,
                serviceDescription.getName());
        String clusterName = gfServiceUtil.getClusterName(serviceDescription.getName(), serviceDescription.getAppName());
        System.out.println("_delete-glassfish-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {
            return true;
        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return false;
        }
    }


    // TODO :: move this utility method to plugin-common module.
    private String formatArgument(List<Property> properties) {
        StringBuilder sb = new StringBuilder();
        if (properties != null) {
            for (Property p : properties) {
                sb.append(p.getName() + "=" + p.getValue() + ":");
            }
        }
        // remove the last ':'
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    public ProvisionedService provisionService(ServiceDescription serviceDescription, DeploymentContext dc) {
//        if (serviceDescription instanceof SimpleServiceDefinition) {
        // TODO :: Figure out that it is for GlassFish.
//            ServiceDescription serviceDefinition = (SimpleServiceDefinition) serviceDescription;
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "provisionService");

        String appNameParam = "";
        if (serviceDescription.getAppName() != null) {
            appNameParam = "--appname=" + serviceDescription.getAppName();
        }

        String serviceConfigurations = formatArgument(serviceDescription.getConfigurations());

        CommandResult result = null;
        // either template identifier or service characteristics are specified, not both.
        if (serviceDescription.getTemplateIdentifier() != null) {
            String templateId = serviceDescription.getTemplateIdentifier().getId();
            result = commandRunner.run("_create-glassfish-service",
                    "--templateid=" + templateId,
                    "--serviceconfigurations", serviceConfigurations,
                    "--waitforcompletion=true", appNameParam, serviceName);
        } else if (serviceDescription.getServiceCharacteristics() != null) {
            String serviceCharacteristics = formatArgument(serviceDescription.
                    getServiceCharacteristics().getServiceCharacteristics());
            result = commandRunner.run("_create-glassfish-service",
                    "--servicecharacteristics=" + serviceCharacteristics,
                    "--serviceconfigurations", serviceConfigurations,
                    "--waitforcompletion=true", appNameParam, serviceName);
        } else {
            // TODO :: remove this else block...in an ideal world we should not land up here....
            result = commandRunner.run("_create-glassfish-service",
                    "--instancecount=" + serviceDescription.getConfiguration("min.clustersize"),
                    "--waitforcompletion=true", appNameParam, serviceName);
        }

        String clusterName = gfServiceUtil.getClusterName(serviceDescription.getName(), serviceDescription.getAppName());
        System.out.println("_create-glassfish-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {

            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceDescription.getName());
            Properties serviceProperties = new Properties();
            serviceProperties.setProperty("host", dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);

            GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);

            GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
            GlassFishProvisionedService gfps = new GlassFishProvisionedService(serviceDescription, serviceProperties, provisionedGlassFish);
            glassfishProvisionedService = gfps; //TODO remove initializing this, once cloud-deploy is removed.

            //set the target to the newly created cluster.
            //TODO what if someone requests for multiple GlassFish services ?
            //TODO As of now, the last one is considered as deployment target.
            if (dc != null) { //TODO remove once "deploy-service" is made obselete
                DeployCommandParameters dcp = dc.getCommandParameters(DeployCommandParameters.class);
                dcp.target = clusterName;
            }

            return gfps;

        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return null;
        }
//        }

    }

    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        String serviceName = serviceDescription.getName();
        logger.entering(getClass().getName(), "startService");

        String appNameParam = "";
        if (serviceDescription.getAppName() != null) {
            appNameParam = "--appname=" + serviceDescription.getAppName();
        }

        String serviceConfigurations = formatArgument(serviceDescription.getConfigurations());

        CommandResult result = null;
        // either template identifier or service characteristics are specified, not both.
        if (serviceDescription.getTemplateIdentifier() != null) {
            String templateId = serviceDescription.getTemplateIdentifier().getId();
/*
            result = commandRunner.run("_start-glassfish-service",
                    appNameParam, serviceName);
*/
        } else if (serviceDescription.getServiceCharacteristics() != null) {
            String serviceCharacteristics = formatArgument(serviceDescription.
                    getServiceCharacteristics().getServiceCharacteristics());

            result = commandRunner.run("_start-glassfish-service",
                    appNameParam, serviceName);

        } else {
            // TODO :: remove this else block...in an ideal world we should not land up here....
            result = commandRunner.run("_start-glassfish-service", appNameParam, serviceName);
        }

        String clusterName = gfServiceUtil.getClusterName(serviceDescription.getName(), serviceDescription.getAppName());
        System.out.println("_start-glassfish-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {

            String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceDescription.getName());
            Properties serviceProperties = new Properties();
            serviceProperties.setProperty("host", dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);

            GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                    provisionerUtil.getAppServerProvisioner(dasIPAddress);

            GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
            GlassFishProvisionedService gfps = new GlassFishProvisionedService(serviceDescription, serviceProperties, provisionedGlassFish);
            glassfishProvisionedService = gfps; //TODO remove initializing this, once cloud-deploy is removed.

            return gfps;

        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return null;
        }
    }

    public boolean stopService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        String appNameParam = "";
        if (serviceDescription.getAppName() != null) {
            appNameParam = "--appname=" + serviceDescription.getAppName();
        }
        CommandResult result = commandRunner.run("_stop-glassfish-service",
                appNameParam, "--cascade=true", serviceDescription.getName());

        System.out.println("_stop-glassfish-service command output [" + result.getOutput() + "]");
        if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {
            return true;
        } else {
            //TODO throw exception ?
            result.getFailureCause().printStackTrace();
            return false;
        }
    }


    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo){
        String dasIPAddress = gfServiceUtil.getDASIPAddress(serviceDescription.getName());
        Properties serviceProperties = new Properties();
        serviceProperties.setProperty("host", dasIPAddress);
//                serviceProperties.setProperty("domainName", domainName);

        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                provisionerUtil.getAppServerProvisioner(dasIPAddress);
        GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();
        GlassFishProvisionedService gfps =new GlassFishProvisionedService(serviceDescription, serviceProperties, provisionedGlassFish);
        glassfishProvisionedService = gfps; //TODO remove initializing this, once cloud-deploy is removed.
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
	    serviceProvider.getServiceType().toString().equals("Database")) {

            if (!beforeDeployment) return;

            // JDBC connection properties
            ServiceDescription serviceDescription = serviceProvider.getServiceDescription();
//                        (SimpleServiceDefinition) derbyProvisionedService.getServiceDefinition();
            Properties dbProperties = new Properties();
            dbProperties.putAll(svcRef.getProperties());
            String serverName = serviceProvider.getServiceProperties().getProperty("host");
            String url = serviceProvider.getServiceProperties().getProperty("URL");
            if(serverName != null) {
                dbProperties.setProperty("serverName", serverName);
            }
            if(url != null) {
                dbProperties.setProperty("URL", url);
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

            String poolName = serviceName + ".pool";
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
            //TODO temporary workaround. What if multiple resource-refs are present ?
            if (svcRef.getServiceRefType().equals("javax.sql.DataSource")) {
                //if (serviceProvider instanceof GlassFishProvisionedService) {
                    GlassFishProvisionedService glassfishProvisionedService = (GlassFishProvisionedService) serviceConsumer;
                    String serviceName = glassfishProvisionedService.getServiceDescription().getName();
                    String clusterName = gfServiceUtil.getClusterName(serviceName, glassfishProvisionedService.getServiceDescription().getAppName());
                    String poolName = serviceName + ".pool";
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
    }

    public boolean isRunning(ProvisionedService provisionedSvc) {
        boolean isRunning = false;
        try {
            if(provisionedSvc instanceof GlassFishProvisionedService){
                isRunning = (((GlassFishProvisionedService)provisionedSvc).getProvisionedGlassFish().getStatus()
                    == GlassFish.Status.STARTED);
            }
        } catch (Exception ex) {
            return false;
        }
        return isRunning;
    }

    public ProvisionedService match(ServiceReference svcRef) {
        return null;
    }

    public boolean reconfigureServices(ProvisionedService oldPS, ProvisionedService newPS) {
        return false;
    }

    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive readableArchive, String appName) {
        HashSet<ServiceDescription> defs = new HashSet<ServiceDescription>();

/*
        if (DeploymentUtils.isWebArchive(readableArchive) || DeploymentUtils.isEAR(readableArchive) ||
            DeploymentUtils.isRAR(readableArchive)) {
*/
            List<Property> characteristics = new ArrayList<Property>();
            characteristics.add(new Property("service-type", JAVAEE_SERVICE_TYPE));
//            characteristics.add(new Property("service-vendor", "GlassFish"));
//            characteristics.add(new Property("service-product-name", "GlassFish"));

            List<Property> configurations = new ArrayList<Property>();
            configurations.add(new Property("min.clustersize", "2"));
            configurations.add(new Property("max.clustersize", "4"));

            //we append -service in the service-name so that cluster-name and app-name
            //are not same. If they are same, delete-virtual-cluster gets initiated and fails.
            ServiceDescription sd = new ServiceDescription(
                    readableArchive.getName() + "-service", appName, "lazy",
                    new ServiceCharacteristics(characteristics), configurations);
            defs.add(sd);
/*
        }
*/
        return defs;
    }
}
