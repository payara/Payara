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
package org.glassfish.paas.dnsplugin;

import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.ProcessExecutor;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.glassfish.api.deployment.ApplicationContainer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.paas.orchestrator.PaaSDeploymentContext;
import org.glassfish.paas.orchestrator.ServiceOrchestrator;
import org.glassfish.paas.orchestrator.provisioning.ServiceInfo;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.metadata.ServiceReference;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ServicePlugin;
import org.glassfish.virtualization.spi.AllocationStrategy;
import javax.inject.Inject;

import org.glassfish.hk2.api.PerLookup;

import org.glassfish.paas.dnsplugin.logger.DnsPluginLogger;
import org.glassfish.paas.orchestrator.service.DnsServiceType;
import org.glassfish.paas.orchestrator.service.spi.Service;

/**
 * DNS plugin which can work with bind 9 dns server using nsupdate utility to
 * create or delete domain name entry. Currently creates <APP_NAME>.<DOMAIN_NAME>
 * entry mapping to load-balancer IP address in DNS.
 * 
 * @author Kshitiz Saxena
 */
@PerLookup
@org.jvnet.hk2.annotations.Service
public class DnsPlugin implements ServicePlugin {

    @Inject
    private DnsServiceUtil dnsServiceUtil;
    
    private static final String APPLICATION_DOMAIN_NAME = "application-domain-name";

    @Override
    public DnsServiceType getServiceType() {
        return new DnsServiceType();
    }

    @Override
    public boolean handles(ReadableArchive cloudArchive) {
        //For prototype, DNS Plugin has no role here.
        return true;
    }

    @Override
    public boolean handles(ServiceDescription serviceDescription) {
        return false;
    }

    @Override
    public boolean isReferenceTypeSupported(String referenceType) {
        return Constants.DNS.equalsIgnoreCase(referenceType);
    }

    @Override
    public Set getServiceReferences(String appName, ReadableArchive cloudArchive, PaaSDeploymentContext dc) {
        HashSet<ServiceReference> serviceReferences = new HashSet<ServiceReference>();
        //No need to provide any reference
        //Service(s) which want to get associated with DNS service will provide service ref to DNS
        //and since association is bidirectional, DNS will associate with that service
        return serviceReferences;
    }

    @Override
    public ServiceDescription getDefaultServiceDescription(String appName, ServiceReference svcRef) {
        //currently DNS is only supported as a external service
        //so will never be provisioned
        return null;
    }

    @Override
    public Set<ServiceDescription> getImplicitServiceDescriptions(
            ReadableArchive cloudArchive, String appName, PaaSDeploymentContext context) {
        //currently DNS is only supported as a external service
        //so will never be provisioned
        return new HashSet<ServiceDescription>();
    }

    @Override
    public ProvisionedService provisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc) {
        //currently DNS is only supported as a external service
        //so will never be provisioned
        throw new UnsupportedOperationException("Provisioning of Dns Service " +
                "not supported in this release");
    }

    @Override
    public ProvisionedService getProvisionedService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        //currently DNS is only supported as a external service
        //so will never be provisioned
        throw new UnsupportedOperationException("Provisioning of Dns Service " +
                "not supported in this release");
    }

    @Override
    public boolean unprovisionService(ServiceDescription serviceDescription, PaaSDeploymentContext dc){
        //currently DNS is only supported as a external service
        //so will never be provisioned
        throw new UnsupportedOperationException("Unprovisioning of Dns Service " +
                "not supported in this release");
    }

    /**
     * {@inheritDoc}
     */
     @Override
     public boolean deploy(PaaSDeploymentContext dc, Service service){
         return true;
     }

     /**
      * {@inheritDoc}
      */
     @Override
     public boolean undeploy(PaaSDeploymentContext dc, Service service){
         return true;
     }

    @Override
    public ProvisionedService startService(ServiceDescription serviceDescription, ServiceInfo serviceInfo) {
        //currently DNS is only supported as a external service
        //so it cannot be controlled by this plugin
        throw new UnsupportedOperationException("Starting Dns Service " +
                "not supported in this release");
    }

    @Override
    public boolean stopService(ProvisionedService provisionedSvc, ServiceInfo serviceInfo) {
        //currently DNS is only supported as a external service
        //so it cannot be controlled by this plugin
        throw new UnsupportedOperationException("Stopping of Dns Service " +
                "not supported in this release");
    }

    @Override
    public boolean isRunning(ProvisionedService provisionedSvc) {
        //currently DNS is only supported as a external service
        //so it cannot be controlled by this plugin
        throw new UnsupportedOperationException("Status check of Dns Service " +
                "not supported in this release");
    }

    @Override
    public ProvisionedService match(ServiceReference svcRef) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    @Override
    public ProvisionedService scaleService(ProvisionedService provisionedService,
            int scaleCount, AllocationStrategy allocStrategy) {
        //no-op
        throw new UnsupportedOperationException("Scaling of Dns Service " +
                "not supported in this release");
    }

    @Override
    public boolean reconfigureServices(ProvisionedService oldPS,
            ProvisionedService newPS) {
        //no-op
        throw new UnsupportedOperationException("Reconfiguration of Dns Service " +
                "not supported in this release");
    }

    @Override
    public void associateServices(Service serviceConsumer, ServiceReference svcRef,
            Service serviceProvider, boolean beforeDeployment, PaaSDeploymentContext dc) {
        if(beforeDeployment){
            return;
        }

        if (!(Constants.DNS.equals(svcRef.getType())
                && serviceConsumer.getServiceType().toString().equals("LB")
                && serviceProvider.getServiceType().toString().equals(Constants.DNS))){
            return;
        }

        updateDnsEntry(serviceProvider, serviceConsumer, dc, true);
    }

    @Override
    public void dissociateServices(Service serviceConsumer, ServiceReference svcRef,
                                   Service serviceProvider, boolean beforeUndeploy, PaaSDeploymentContext dc){
        if(!beforeUndeploy){
            return;
        }

        if (!(Constants.DNS.equals(svcRef.getType())
                && serviceConsumer.getServiceType().toString().equals("LB")
                && serviceProvider.getServiceType().toString().equals(Constants.DNS))){
            return;
        }

        updateDnsEntry(serviceProvider, serviceConsumer, dc, false);
    }

    @Override
    public boolean reassociateServices(Service serviceConsumer,
            Service oldServiceProvider,
            Service newServiceProvider,
            ServiceOrchestrator.ReconfigAction reason) {
        //TBD
        //callAssociateService(serviceConsumer, newServiceProvider, true);
        return true;
    }

    private void updateDnsEntry(Service serviceConsumer,
            Service serviceProvider, PaaSDeploymentContext dc, boolean isAdd) {

        String appName = dc.getAppName();

        //TODO retrieve IP address from ServiceProvider's ServiceProperties ?
        String lbIPAddr = dnsServiceUtil.getIPAddress(
                serviceProvider.getServiceDescription().getName(),
                appName);

        Properties serviceProperties = serviceConsumer.getServiceProperties();
        String domainName = serviceProperties.getProperty(Constants.DOMAIN_NAME);
        String dnsIP = serviceProperties.getProperty(Constants.DNS_IP);
        String fileLoc = serviceProperties.getProperty(Constants.DNS_PRIVATE_KEY_FILE_LOCATION);

        /**
         * Creating a dns update file having following format
         * server <DNS SERVER IP>
         * zone <DOMAIN NAME>
         * update add <APP NAME>.<DOMAIN NAME> 86400 A <LB IP ADDR>
         * update delete <APP NAME>.<DOMAIN NAME> A
         * show
         * send
         */
        String appDomainName = replace(appName) + "." + domainName;
        File tmpFile = null;
        BufferedWriter writer = null;
        try {
            tmpFile = File.createTempFile("dns-entry", ".txt");
            writer = new BufferedWriter(new FileWriter(tmpFile));
            writer.append("server ").append(dnsIP);
            writer.newLine();
            writer.append("zone ").append(domainName);
            writer.newLine();
            if (isAdd) {
                writer.append("update add ").append(appDomainName).append(" 86400 A ").append(lbIPAddr);
            } else {
                writer.append("update delete ").append(appDomainName).append(" A");
            }
            writer.newLine();
            writer.append("show");
            writer.newLine();
            writer.append("send");
            writer.newLine();
            writer.flush();
        } catch (IOException ex) {
            DnsPluginLogger.getLogger().log(Level.SEVERE,
                    "Exception when creating temporary file for DNS update " + ex.getMessage());
            DnsPluginLogger.getLogger().log(Level.FINE, "Exception", ex);
            return;
        } finally {
            if (writer != null){
                try {
                    writer.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }

        //It is assumed dns update utility will be available in system path
        //No default path is provided here
        String[] dnsUpdateCmds = {Constants.DNS_UPDATE_UTILITY, "-k", fileLoc,
            "-v", tmpFile.getAbsolutePath()};
        ProcessExecutor executor = new ProcessExecutor(dnsUpdateCmds);
        try {
            executor.execute();
            int status = executor.getProcessExitValue();
            DnsPluginLogger.getLogger().log(Level.FINE, "DNS update command output : "
                    + executor.getLastExecutionOutput());
            if(status != 0){
               DnsPluginLogger.getLogger().log(Level.SEVERE, "DNS update failed");
               DnsPluginLogger.getLogger().log(Level.WARNING, "DNS update command output : "
                    + executor.getLastExecutionError());
            } else {
                if(isAdd){
                    dc.addTransientAppMetaData(APPLICATION_DOMAIN_NAME, appDomainName);
                }
            }
        } catch (ExecException ex) {
            DnsPluginLogger.getLogger().log(Level.SEVERE,
                    "Exception when running DNS update command " + ex.getMessage());
            DnsPluginLogger.getLogger().log(Level.FINE, "Exception", ex);
        }

        if(tmpFile != null){
            tmpFile.delete();
        }
        
    }

    /* Replaces all special characters(except hyphen) with hyphen */
    private String replace(String appName) {
        Pattern specialCharPattern = Pattern.compile("[^a-zA-Z0-9-]");
        Matcher matcher = specialCharPattern.matcher(appName);
        return matcher.replaceAll("-");
    }
    
}
