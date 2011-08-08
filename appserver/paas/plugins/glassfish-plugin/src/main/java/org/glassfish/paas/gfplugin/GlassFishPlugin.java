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

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.gfplugin.cli.GlassFishServiceUtil;
import org.glassfish.paas.orchestrator.provisioning.ApplicationServerProvisioner;
import org.glassfish.paas.orchestrator.provisioning.CloudRegistryService;
import org.glassfish.paas.orchestrator.provisioning.LBProvisioner;
import org.glassfish.paas.orchestrator.provisioning.cli.ServiceType;
import org.glassfish.paas.javadbplugin.DerbyProvisionedService;
import org.glassfish.paas.orchestrator.service.HTTPLoadBalancerServiceType;
import org.glassfish.paas.orchestrator.service.JavaEEServiceType;
import org.glassfish.paas.orchestrator.service.ServiceReference;
import org.glassfish.paas.orchestrator.service.SimpleServiceDefinition;
import org.glassfish.paas.orchestrator.service.spi.Plugin;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServiceDefinition;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;

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
    private CloudRegistryService cloudRegistryService;

    public static final String JAVAEE_ServiceType = "JAVAEE";

    private GlassFishProvisionedService glassfishProvisionedService;

    public JavaEEServiceType getServiceType() {
        return new JavaEEServiceType();
    }

    public boolean handles(ReadableArchive cloudArchive) {
        return true;
    }

    public boolean isReferenceTypeSupported(String referenceType) {
        //GlassFish plugin would not be able to support any reference types
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

    public ServiceDefinition getDefaultServiceDefinition(ServiceReference svcRef) {
        return null;
    }

    public ProvisionedService provisionService(ServiceDefinition svcDefn) {
        if (svcDefn instanceof SimpleServiceDefinition) {
            // TODO :: Figure out that it is for GlassFish.
            SimpleServiceDefinition serviceDefinition = (SimpleServiceDefinition) svcDefn;
            CommandResult result = commandRunner.run("create-glassfish-service",
                    "--instancecount=" + serviceDefinition.getProperties().getProperty("min-cluster-size"),
                    "--waitforcompletion=true",
                    serviceDefinition.getProperties().getProperty("servicename"));
            System.out.println("create-glassfish-service command output [" + result.getOutput() + "]");
            if (result.getExitStatus() == CommandResult.ExitStatus.SUCCESS) {
                String domainName = gfServiceUtil.getDomainName(
                        serviceDefinition.getProperties().getProperty("servicename"));
                String dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);
                GlassFishProvisioner gfProvisioner = (GlassFishProvisioner)
                        cloudRegistryService.getAppServerProvisioner(dasIPAddress);

                GlassFish provisionedGlassFish = gfProvisioner.getGlassFish();

                glassfishProvisionedService =
                        new GlassFishProvisionedService(serviceDefinition, provisionedGlassFish);

                return glassfishProvisionedService;
            } else {
                result.getFailureCause().printStackTrace();
            }
        }
        return null;
    }

    public void associateServices(ProvisionedService provisionedSvc,
                                  ServiceReference svcRef, boolean beforeDeployment) {
        if (provisionedSvc instanceof DerbyProvisionedService) {
            if (svcRef.getServiceRefType().equals("javax.sql.DataSource")) {

                if (!beforeDeployment) return;

                DerbyProvisionedService derbyProvisionedService =
                        (DerbyProvisionedService) provisionedSvc;

                // JDBC connection properties
                SimpleServiceDefinition derbyServiceDefinition =
                        (SimpleServiceDefinition) derbyProvisionedService.getServiceDefinition();
                Properties derbyProperties = derbyServiceDefinition.getProperties();

                // Get the domain and cluster names.
                SimpleServiceDefinition serviceDefinition =
                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDefinition();
                String serviceName = serviceDefinition.getProperties().getProperty("servicename");
                String domainName = gfServiceUtil.getDomainName(serviceName);
                String clusterName = gfServiceUtil.getClusterName(serviceName);
                String dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);

                String poolName = serviceName + ".pool";
                String resourceName = svcRef.getServiceRefName();

                // Create JDBC resource and pool.
                GlassFishProvisioner glassFishProvisioner = (GlassFishProvisioner)
                        cloudRegistryService.getAppServerProvisioner(dasIPAddress);
                glassFishProvisioner.createJdbcConnectionPool(dasIPAddress, clusterName,
                        derbyProperties, poolName);
                glassFishProvisioner.createJdbcResource(dasIPAddress, clusterName,
                        poolName, resourceName);
            }
        }

        //if (provisionedSvc instanceof GlassFishLBProvisionedService) {
        if (provisionedSvc.getServiceType() instanceof HTTPLoadBalancerServiceType) {
            if (svcRef.getServiceRefType().equals("HTTP_LOAD_BALANCER")) {

                SimpleServiceDefinition gfServiceDefinition =
                        (SimpleServiceDefinition) glassfishProvisionedService.getServiceDefinition();
                String appServerServiceName = gfServiceDefinition.getProperties().getProperty("servicename");
                String domainName = gfServiceUtil.getDomainName(appServerServiceName);
                String clusterName = gfServiceUtil.getClusterName(appServerServiceName);
                String dasIPAddress = gfServiceUtil.getIPAddress(domainName, ServiceType.APPLICATION_SERVER);

                ApplicationServerProvisioner appServerProvisioner = cloudRegistryService.getAppServerProvisioner(dasIPAddress);

/*
                GlassFishLBProvisionedService gfLBProvisionedService =
                        (GlassFishLBProvisionedService) provisionedSvc;
*/
                SimpleServiceDefinition lbServiceDefinition = (SimpleServiceDefinition)
                        //gfLBProvisionedService.getServiceDefinition();
                        provisionedSvc.getServiceDefinition();
                String lbServiceName = lbServiceDefinition.getName();

                if (beforeDeployment) {
                    LBProvisioner lbProvisioner = cloudRegistryService.getLBProvisioner();
                    String lbIPAddress = gfServiceUtil.getIPAddress(lbServiceName, ServiceType.LOAD_BALANCER);
                    lbProvisioner.associateApplicationServerWithLB(lbIPAddress, dasIPAddress, domainName);

                    //restart
                    lbProvisioner.stopLB(lbIPAddress);
                    lbProvisioner.startLB(lbIPAddress);

                    appServerProvisioner.associateLBWithApplicationServer(dasIPAddress, clusterName, lbIPAddress, lbServiceName);
                } else {
                    appServerProvisioner.refreshLBConfiguration(dasIPAddress, lbServiceName);
                }
            }
        }
    }

    public ApplicationContainer deploy(ReadableArchive cloudArchive) {
        GlassFish provisionedGlassFish =
                glassfishProvisionedService.getProvisionedGlassFish();
        SimpleServiceDefinition serviceDefinition =
                (SimpleServiceDefinition) glassfishProvisionedService.getServiceDefinition();
        String serviceName = serviceDefinition.getProperties().getProperty("servicename");
        String clusterName = gfServiceUtil.getClusterName(serviceName);

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
        try {
            return (glassfishProvisionedService.getProvisionedGlassFish().getStatus()
                    == GlassFish.Status.STARTED);
        } catch (Exception ex) {
            return false;
        }
    }

    public ProvisionedService match(ServiceReference svcRef) {
        return null;
    }

    public boolean reconfigureServices(ProvisionedService oldPS, ProvisionedService newPS) {
        return false;
    }

    public Set<ServiceDefinition> getImplicitServiceDefinitions(
            ReadableArchive cloudArchive) {
        HashSet<ServiceDefinition> defs = new HashSet<ServiceDefinition>();

        //check if the cloudArchive is a Java EE archive.
        //XXX: For now, only check for the name war. Later detect 
        if (cloudArchive.getURI().toString().indexOf(".war") != -1) {
            String appName = cloudArchive.getName();
            Properties p = new Properties();
            p.put("min-cluster-size", "2");
            p.put("max-cluster-size", "4");
            p.put("servicename", "mydomain." + appName);
            ServiceDefinition sd = new SimpleServiceDefinition("default-glassfish", JAVAEE_ServiceType, p);
            defs.add(sd);
        }
        return defs;
    }
}
