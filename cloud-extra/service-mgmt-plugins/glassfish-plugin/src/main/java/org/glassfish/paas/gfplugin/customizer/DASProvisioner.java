/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.paas.gfplugin.customizer;

import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.paas.gfplugin.GlassFishPluginConstants;
import org.glassfish.paas.gfplugin.GlassFishProvisionedService;
import org.glassfish.paas.gfplugin.GlassFishProvisioner;
import org.glassfish.paas.gfplugin.cli.ProvisionerUtil;
import org.glassfish.paas.orchestrator.service.ServiceStatus;
import org.glassfish.paas.orchestrator.service.metadata.ServiceDescription;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.paas.spe.common.BasicProvisionedService;
import org.glassfish.virtualization.spi.VirtualMachine;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileWriter;
import java.util.Properties;

/**
 * Provisions the plain service node to be a DAS node.
 *
 * @author Bhavanishankar S
 */
@Service
@Singleton
public class DASProvisioner implements GlassFishPluginConstants {

    @Inject
    private ProvisionerUtil provisionerUtil;

    // Convert the serviceNode to a GlassFish DAS node.
    public GlassFishProvisionedService provision(ProvisionedService serviceNode) {

        String serviceName = serviceNode.getName();
        String clusterName = serviceNode.getServiceDescription().getVirtualClusterName();

        VirtualMachine vm = ((BasicProvisionedService)serviceNode).getVM();

        ServiceDescription serviceDescription = serviceNode.getServiceDescription();

        Properties serviceProperties = new Properties();
        serviceProperties.putAll(serviceNode.getProperties());
        serviceProperties.setProperty(MIN_CLUSTERSIZE,
                serviceDescription.getConfiguration(MIN_CLUSTERSIZE));
        serviceProperties.setProperty(MAX_CLUSTERSIZE,
                serviceDescription.getConfiguration(MAX_CLUSTERSIZE));

        Properties glassfishProperties = new Properties();
        glassfishProperties.putAll(serviceNode.getProperties());
        glassfishProperties.put("vm", vm);

        GlassFishProvisioner gfProvisioner = (GlassFishProvisioner) provisionerUtil.
                getAppServerProvisioner(glassfishProperties);
        GlassFish das = gfProvisioner.getGlassFish();

        String domain = "paas-domain";

        CommandRunner cr = null;

        try {
            cr = das.getCommandRunner();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        // Step 1. create a new domain
        String passwordFile = createPasswordFile();
        cr.run("create-domain",
                "--user=admin",
                "--adminport=" + DEFAULT_DAS_PORT,
                "--passwordfile=" + passwordFile, domain);

        // Step 2. Start the newly created domain
        cr.run("start-domain", domain);

        // Step 3. Set admin listener address
        cr.run("set", "configs.config.server-config.network-config.network-listeners." +
                "network-listener.admin-listener.address=" + vm.getAddress().getHostAddress());

        // Step 4. Restart the domain
        // cr.run("stop-domain", domain);
        // cr.run("start-domain", domain);

        // Step 5. Create the cluster
        cr.run("create-cluster", clusterName);

        // Step 6. Create elastic service
        cr.run(CREATE_ELASTIC_SERVICE,
                "--min=" + serviceDescription.getConfiguration(MIN_CLUSTERSIZE),
                "--max=" + serviceDescription.getConfiguration(MAX_CLUSTERSIZE),
                serviceName);

        return new GlassFishProvisionedService(serviceDescription, serviceProperties,
                ServiceStatus.RUNNING, das);
    }

    private String createPasswordFile() {
        FileWriter fw = null;
        try {
            File passFile = File.createTempFile("asadmin", "passwd");
            passFile.deleteOnExit();
            fw = new FileWriter(passFile);
            fw.write("AS_ADMIN_PASSWORD=\n");
            fw.flush();
            fw.close();
            return passFile.getAbsolutePath();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (Exception ex) {
                }
            }
        }
    }

}
