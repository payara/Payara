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
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import org.glassfish.api.ActionReport;
import org.glassfish.gms.bootstrap.GMSAdapter;
import org.glassfish.gms.bootstrap.GMSAdapterService;
import org.glassfish.gms.bootstrap.HealthHistory;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.paas.gfplugin.GlassFishPluginConstants;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.io.File;
import java.util.logging.Level;

/**
 * Implementation of the non virtualized glassfish template customizer.
 * @author Jerome Dochez
 */
@Service(name="Native-JavaEE")
public class LocalGlassFishTemplateCustomizer implements TemplateCustomizer,
        GlassFishPluginConstants {

    @Inject
    Domain domain;

    @Inject
    RuntimeContext rtContext;

    @Inject
    private ServerContext serverContext;

    @Inject
    Habitat services;

    @Inject
    GMSAdapterService gmsAdapterService;

    @Override
    public void customize(final VirtualCluster cluster, final VirtualMachine virtualMachine) throws VirtException {
        if(provisionDAS) {
            return;
        }
        ActionReport report = services.forContract(ActionReport.class)
                .named(PLAIN_ACTION_REPORT).get();
       // this line below needs to come from the template...
        String[] createArgs = {getAsAdminCommand(),
                CREATE_LOCAL_INSTANCE,
                "--cluster", cluster.getConfig().getName(),
                 virtualMachine.getName()};
        ProcessExecutor createInstance = new ProcessExecutor(createArgs);

        try {
            createInstance.execute();
        } catch (ExecException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        
        for (Server instance : cluster.getConfig().getInstances()) {
            if (instance.getName().equals(virtualMachine.getName())) {
                try {
                    ConfigSupport.apply(new SingleConfigCode<Server>() {
                        @Override
                        public Object run(Server wServer) throws PropertyVetoException, TransactionFailure {
                            Property property = wServer.createChild(Property.class);
                            property.setName("ServerPool");
                            property.setValue(virtualMachine.getServerPool().getName());
                            wServer.getProperty().add(property);
                            return null;
                        }
                    }, instance);
                } catch (TransactionFailure transactionFailure) {
                    RuntimeContext.logger.log(Level.SEVERE,
                            "Cannot add properties to newly created instance configuration", transactionFailure);
                    throw new VirtException(transactionFailure);
                }
            }
        }
    }

    public boolean isActive(VirtualCluster virtualCluster, VirtualMachine virtualMachine) throws VirtException {
        if (virtualMachine.getInfo().getState().equals(Machine.State.READY)) {
            GMSAdapter adapter = gmsAdapterService.getGMSAdapterByName(virtualCluster.getConfig().getName());
            HealthHistory.InstanceHealth instanceHealth = adapter.getHealthHistory().getHealthByInstance(virtualMachine.getName());
            return instanceHealth.state.equals(HealthHistory.STATE.RUNNING);
        }
        return false;
    }

    @Override
    public void clean(VirtualMachine virtualMachine) {
        if(provisionDAS) {
            return;
        }
        // let's find our instance name.
        String instanceName = virtualMachine.getName();
        Server instance = domain.getServerNamed(instanceName);
        if (instance != null) {
            ActionReport report = services.forContract(ActionReport.class)
                    .named(PLAIN_ACTION_REPORT).get();
            rtContext.executeAdminCommand(report, DELETE_INSTANCE, instanceName);
        }
    }

    public String getAsAdminCommand() {
        String args[] = new String[] {serverContext.getInstallRoot().getAbsolutePath()};
        return ASADMIN_COMMAND.format(args).toString();
    }

    @Override
    public void start(VirtualMachine virtualMachine, boolean firstStart) {
        if(provisionDAS) {
            return;
        }
        String[] startArgs = {getAsAdminCommand(),
                START_LOCAL_INSTANCE,
                 virtualMachine.getName()};
        ProcessExecutor startInstance = new ProcessExecutor(startArgs);
        try {
            startInstance.execute();
        } catch (ExecException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void stop(VirtualMachine virtualMachine) {
        if(provisionDAS) {
            return;
        }
        String instanceName = virtualMachine.getName();
        Server instance = domain.getServerNamed(instanceName);
        if (instance != null) {
            ActionReport report = services.forContract(ActionReport.class)
                    .named(PLAIN_ACTION_REPORT).get();
            rtContext.executeAdminCommand(report, STOP_INSTANCE, instanceName,
                    VM_SHUTDOWN_ARG, "false");
        }
    }
}
