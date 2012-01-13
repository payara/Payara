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

package org.glassfish.paas.gfplugin.customizer;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.ActionReport;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.gms.bootstrap.HealthHistory;
import org.glassfish.hk2.Services;
import org.glassfish.paas.gfplugin.GlassFishPluginConstants;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.TemplateCustomizer;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

/**
 * Customization of the GlassFish template for all virtualizations except Native.
 *
 * @author Jerome Dochez
 * @author Bhavanishankar S
 */
@Service(name="JavaEE")
public class GlassFishTemplateCustomizer implements TemplateCustomizer,
        GlassFishPluginConstants {

    @Inject
    Domain domain;

    @Inject
    Services services;

    @Inject
    RuntimeContext rtContext;

    @Inject
    GMSAdapterService gmsAdapterService;

    @Override
    public void customize(VirtualCluster cluster, VirtualMachine virtualMachine) throws VirtException {
        ActionReport report = services.forContract(ActionReport.class)
                .named(PLAIN_ACTION_REPORT).get();
        final String nodeName = getNodeName(virtualMachine);
        // create-node-ssh --nodehost $ip_address --installdir $GLASSFISH_HOME $node_name
        String installDir = virtualMachine.getProperty(VirtualMachine.PropertyName.INSTALL_DIR);
        rtContext.executeAdminCommand(report, CREATE_NODE_SSH, nodeName,
                NODE_HOST_ARG, virtualMachine.getAddress().getHostAddress(),
                SSH_USER_ARG, virtualMachine.getUser().getName(), /* TODO :: if vm.getUser() is null then should we use System.getProperty("user.name");*/
                INSTALL_DIR_ARG, installDir);

        if (report.hasFailures()) {
            return;
        }
        rtContext.executeAdminCommand(report, CREATE_INSTANCE, getInstanceName(virtualMachine),
                NODE_ARG, nodeName,
                CLUSTER_ARG, cluster.getConfig().getName());

    }

    public boolean isActive(VirtualCluster virtualCluster, VirtualMachine virtualMachine) throws VirtException {
        if (virtualMachine.getInfo().getState().equals(Machine.State.READY)) {
            GMSAdapter adapter = gmsAdapterService.getGMSAdapterByName(virtualCluster.getConfig().getName());
            String instanceName = getInstanceName(virtualMachine);
            HealthHistory.InstanceHealth instanceHealth = adapter.getHealthHistory().getHealthByInstance(instanceName);
            return instanceHealth.state.equals(HealthHistory.STATE.RUNNING);
        }
        return false;
    }

    @Override
    public void start(VirtualMachine virtualMachine, boolean firstStart) {
        ActionReport report = services.forContract(ActionReport.class)
                .named(PLAIN_ACTION_REPORT).get();
        if (firstStart) {
            // finally starts the instance.
            try {
                // TODO :: check for virtualMachine.getInfo().getState()??
                rtContext.executeAdminCommand(report, START_INSTANCE,
                        getInstanceName(virtualMachine));
            } catch (Exception e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    @Override
    public void clean(VirtualMachine virtualMachine) {

        // let's find our instance name.
        String instanceName = getInstanceName(virtualMachine);
        Server server = domain.getServerNamed(instanceName);

        if (server!=null) {
            String nodeName = server.getNodeRef();
            ActionReport report = services.forContract(ActionReport.class).
                    named(PLAIN_ACTION_REPORT).get();
            // TODO :: check for virtualMachine.getInfo().getState()??
            rtContext.executeAdminCommand(report, DELETE_INSTANCE, instanceName);
            Node node = domain.getNodeNamed(nodeName);
            if (node!=null) {
                if (node.getType().equals(NODE_TYPE_SSH)) {
                    rtContext.executeAdminCommand(report, DELETE_NODE_SSH, nodeName);
                }
            }
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

    @Override
    public void stop(VirtualMachine virtualMachine) {
        String instanceName = getInstanceName(virtualMachine);
        Server instance = domain.getServerNamed(instanceName);
        if (instance != null) {
            ActionReport report = services.forContract(ActionReport.class).
                    named(PLAIN_ACTION_REPORT).get();
            // TODO :: check for virtualMachine.getInfo().getState()??
            rtContext.executeAdminCommand(report,
                    STOP_INSTANCE, instanceName, VM_SHUTDOWN_ARG, "false");
        }
    }
}
