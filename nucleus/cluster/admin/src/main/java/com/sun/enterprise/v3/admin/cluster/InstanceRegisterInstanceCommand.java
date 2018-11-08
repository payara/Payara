/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import java.util.Map;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import java.beans.PropertyVetoException;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Node;
import com.sun.enterprise.config.serverbeans.Nodes;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Servers;
import com.sun.enterprise.config.serverbeans.SystemProperty;
import com.sun.enterprise.config.util.InstanceRegisterInstanceCommandParameters;
import org.glassfish.api.admin.*;


/**
 * The _register-instance (and _create-node) command that runs on instance
 * @author Jennifer Chou
 */
@Service(name="_register-instance-at-instance")
@PerLookup
@ExecuteOn(value={RuntimeType.INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.POST, 
        path="_register-instance-at-instance", 
        description="_register-instance-at-instance")
})
public class InstanceRegisterInstanceCommand extends InstanceRegisterInstanceCommandParameters implements AdminCommand {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(InstanceRegisterInstanceCommand.class);

    @Inject
    Domain domain;

    @Inject
    ServerEnvironment env;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    protected Server server;

    @Override
    public void execute(AdminCommandContext ctxt) {
        final ActionReport report = ctxt.getActionReport();

        try {
            // create node if it doesn't exist
            Node n = domain.getNodes().getNode(node);
            if (n == null) {
                ConfigSupport.apply(new SingleConfigCode<Nodes>() {

                    @Override
                    public Object run(Nodes param) throws PropertyVetoException, TransactionFailure {

                        Node newNode = param.createChild(Node.class);
                        newNode.setName(node);
                        if(installdir != null && !"".equals(installdir))
                            newNode.setInstallDir(installdir);
                        if (nodedir != null && !"".equals(nodedir))
                             newNode.setNodeDir(nodedir);
                        if (nodehost != null && !"".equals(nodehost))
                            newNode.setNodeHost(nodehost);
                        newNode.setType(type);

                        param.getNode().add(newNode);
                        return newNode;
                    }
                }, domain.getNodes());
            }

            // create server if it doesn't exist
            Server s = domain.getServers().getServer(instanceName);
            if (s == null) {
                ConfigSupport.apply(new SingleConfigCode<Servers>() {

                    public Object run(Servers param) throws PropertyVetoException, TransactionFailure {

                        Server newServer = param.createChild(Server.class);

                        newServer.setConfigRef(config);
                        newServer.setName(instanceName);
                        newServer.setNodeRef(node);

                        if (systemProperties != null) {
                            for (final Map.Entry<Object, Object> entry : systemProperties.entrySet()) {
                                final String propName = (String) entry.getKey();
                                final String propValue = (String) entry.getValue();
                                SystemProperty newSP = newServer.createChild(SystemProperty.class);
                                newSP.setName(propName);
                                newSP.setValue(propValue);
                                newServer.getSystemProperty().add(newSP);
                            }
                        }

                        param.getServer().add(newServer);
                        return newServer;
                    }
                }, domain.getServers());

                // create server-ref on cluster
                Cluster thisCluster = domain.getClusterNamed(clusterName);
                if (thisCluster != null) {
                    ConfigSupport.apply(new SingleConfigCode<Cluster>() {

                        @Override
                        public Object run(Cluster param) throws PropertyVetoException, TransactionFailure {

                            ServerRef newServerRef = param.createChild(ServerRef.class);
                            newServerRef.setRef(instanceName);
                            newServerRef.setLbEnabled(lbEnabled);
                            param.getServerRef().add(newServerRef);
                            return param;
                        }
                    }, thisCluster);
                }
            }


            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure tfe) {
            report.setMessage(localStrings.getLocalString("register.instance.failed",
                    "Instance {0} registration failed on {1}", instanceName, server.getName()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(tfe);
        } catch (Exception e) {
            report.setMessage(localStrings.getLocalString("register.instance.failed",
                    "Instance {0} registration failed on {1}", instanceName, server.getName()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }

    }


}
