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

package org.glassfish.virtualization.local;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.OS;
import com.sun.enterprise.util.ProcessExecutor;
import org.glassfish.api.ActionReport;
import org.glassfish.hk2.Services;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;
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
public class LocalGlassFishTemplateCustomizer implements TemplateCustomizer {

    @Inject
    Domain domain;

    @Inject
    RuntimeContext rtContext;

    @Inject
    private ServerContext serverContext;

    @Inject
    Services services;

    @Override
    public void customize(final VirtualCluster cluster, final VirtualMachine virtualMachine) throws VirtException {

        ActionReport report = services.forContract(ActionReport.class).named("plain").get();
       // this line below needs to come from the template...
        String[] createArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + "lib" + File.separator + "nadmin" + (OS.isWindows()? ".bat" : "") , "create-local-instance",
                "--cluster", cluster.getConfig().getName(),
                 virtualMachine.getName()};
        String[] startArgs = {serverContext.getInstallRoot().getAbsolutePath() +
                File.separator + "lib" + File.separator + "nadmin" +  (OS.isWindows()? ".bat" : "") , "start-local-instance",
                 virtualMachine.getName()};
        ProcessExecutor createInstance = new ProcessExecutor(createArgs);
        ProcessExecutor startInstance = new ProcessExecutor(startArgs);

        try {
            createInstance.execute();
            startInstance.execute();
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

    @Override
    public void clean(VirtualMachine virtualMachine) {
        // let's find our instance name.
        String instanceName = virtualMachine.getName();
        Server instance = domain.getServerNamed(instanceName);
        if (instance != null) {
            ActionReport report = services.forContract(ActionReport.class).named("plain").get();
            rtContext.executeAdminCommand(report, "stop-instance", instanceName, "_vmShutdown", "false");
            rtContext.executeAdminCommand(report, "delete-instance", instanceName);
        }
    }

    @Override
    public void start(VirtualMachine virtualMachine, boolean firstStart) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void stop(VirtualMachine virtualMachine) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
