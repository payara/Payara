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
import org.glassfish.hk2.scopes.Singleton;
import org.glassfish.paas.gfplugin.GlassFishPluginConstants;
import org.glassfish.paas.gfplugin.GlassFishProvisionedService;
import org.glassfish.paas.orchestrator.service.spi.ProvisionedService;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.FileWriter;

/**
 * @author Bhavanishankar S
 */
@Service
@Scoped(Singleton.class)
public class RemoteInstanceProvisioner implements GlassFishPluginConstants {

    public void provision(GlassFishProvisionedService das, ProvisionedService... instances) {

        String passwordFile = createPasswordFile();

        CommandRunner cr = null;

        try {
            cr = das.getProvisionedGlassFish().getCommandRunner();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        for (ProvisionedService instance : instances) {
            VirtualMachine vm = ((VirtualMachine) instance.getServiceProperties().get("vm"));

            // setup-ssh
            cr.run(SETUP_SSH,
                    "--interactive=false",
                    "--passwordfile=" + passwordFile,
                    "--sshuser=cloud", // TODO :: get the user from the template
                    "--generatekey=true",
                    vm.getAddress().getHostAddress());

            // create-node-ssh
            cr.run(CREATE_NODE_SSH,
                    "--nodehost=" + vm.getAddress().getHostAddress(),
                    "--sshuser=cloud",
                    "--installdir=" + vm.getProperty(VirtualMachine.PropertyName.INSTALL_DIR),
                    getNodeName(vm));

            // create-instance
            cr.run("create-instance",
                    "--node=" + getNodeName(vm),
                    "--cluster=" + instance.getServiceDescription().getVirtualClusterName(),
                    getInstanceName(vm));

            // start-instance
            cr.run("start-instance", getInstanceName(vm));

            // TODO :-> add each instance to the DAS node
            // das.addChildService(instance);
        }
    }


    private String getNodeName(VirtualMachine virtualMachine) {
        String machineName = virtualMachine.getMachine() != null ?
                virtualMachine.getMachine().getName() :
                virtualMachine.getServerPool().getConfig().getVirtualization().getName();

        String args[] = new String[]{
                virtualMachine.getServerPool().getName(),
                machineName,
                virtualMachine.getName()
        };
        return NODE_NAME_FORMAT.format(args).toString();
    }

    private String getInstanceName(VirtualMachine virtualMachine) {
        String machineName = virtualMachine.getMachine() != null ?
                virtualMachine.getMachine().getName() :
                virtualMachine.getServerPool().getConfig().getVirtualization().getName();

        String args[] = new String[]{
                virtualMachine.getServerPool().getName(),
                machineName,
                virtualMachine.getName()
        };
        return INSTANCE_NAME_FORMAT.format(args).toString();
    }

    private String createPasswordFile() {
        FileWriter fw = null;
        try {
            File passFile = File.createTempFile("asadmin", "passwd");
            passFile.deleteOnExit();
            fw = new FileWriter(passFile);
            fw.write("AS_ADMIN_SSHPASSWORD=cloud\n"); // TODO :: get the password from the template
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
