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

package org.glassfish.virtualization.libvirt;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.ActionReport;
import org.glassfish.hk2.Services;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 * Customization of the KVM GlassFish template
 * @author Jerome Dochez
 */
@Service(name="libvirt-JavaEE")
public class GlassFishTemplateCustomizer implements TemplateCustomizer {

    @Inject
    Domain domain;

    @Inject
    Services services;

    @Inject
    RuntimeContext rtContext;

    @Override
    public void customize(VirtualCluster cluster, VirtualMachine virtualMachine) throws VirtException {
        ActionReport report = services.forContract(ActionReport.class).named("plain").get();
        final String nodeName = virtualMachine.getServerPool().getName() + "_" +
                virtualMachine.getMachine().getName() + "_" + virtualMachine.getName();
        // create-node-ssh --nodehost $ip_address --installdir $GLASSFISH_HOME $node_name
        String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
        rtContext.executeAdminCommand(report, "create-node-ssh", nodeName, "nodehost", virtualMachine.getAddress(),
                "sshUser", virtualMachine.getUser().getName(), "installdir", installDir);

        if (report.hasFailures()) {
            return;
        }
        rtContext.executeAdminCommand(report, "create-instance", nodeName + "Instance", "node", nodeName,
                "cluster", cluster.getConfig().getName());

    }

    @Override
    public void start(VirtualMachine virtualMachine) {
        // done by the clustering infrastructure
    }

    @Override
    public void stop(VirtualMachine virtualMachine) {
        // done by the clustering infrastructure
    }

    @Override
    public void clean(VirtualMachine virtualMachine) {

        // let's find our instance name.
        String vmName = virtualMachine.getName();
        String instanceName = virtualMachine.getServerPool().getName()+"_"+virtualMachine.getMachine().getName()+"_"+vmName+"Instance";
        Server server = domain.getServerNamed(instanceName);

        if (server!=null) {
            String nodeName = server.getNodeRef();
            ActionReport report = services.forContract(ActionReport.class).named("plain").get();
            rtContext.executeAdminCommand(report, "stop-instance", instanceName, "_vmShutdown", "false");
            rtContext.executeAdminCommand(report, "delete-instance", instanceName);

            Node node = domain.getNodeNamed(nodeName);
            if (node!=null) {
                if (node.getType().equals("SSH")) {
                    rtContext.executeAdminCommand(report, "delete-node-ssh", nodeName);
                }
            }
        }
    }
}
