/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.managed;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import fish.payara.arquillian.container.payara.CommonPayaraManager;

/**
 * Payara managed container using REST deployments
 *
 * @author <a href="http://community.jboss.org/people/LightGuard">Jason Porter</a>
 * @author <a href="http://community.jboss.org/people/dan.j.allen">Dan Allen</a>
 * @author Vineet Reynolds
 */
public class PayaraManagedDeployableContainer implements DeployableContainer<PayaraManagedContainerConfiguration> {

    private PayaraManagedContainerConfiguration configuration;
    private PayaraServerControl payaraServerControl;
    private CommonPayaraManager<PayaraManagedContainerConfiguration> payaraManager;
    private boolean connectedToRunningServer;

    @Override
    public Class<PayaraManagedContainerConfiguration> getConfigurationClass() {
        return PayaraManagedContainerConfiguration.class;
    }

    @Override
    public void setup(PayaraManagedContainerConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }

        this.configuration = configuration;
        
        payaraServerControl = new PayaraServerControl(configuration);
        payaraManager = new CommonPayaraManager<PayaraManagedContainerConfiguration>(configuration);
    }

    @Override
    public void start() throws LifecycleException {
        if (payaraManager.isDASRunning()) {
            if (configuration.isAllowConnectingToRunningServer()) {
                // If we are allowed to connect to a running server, then do not issue the 'asadmin start-domain' command.
                connectedToRunningServer = true;
                payaraManager.start();
            } else {
                throw new LifecycleException(
                    "The server is already running! " + 
                    "Managed containers do not support connecting to running server instances due to the " + 
                    "possible harmful effect of connecting to the wrong server. Please stop server before running or " + 
                    "change to another type of container.\n" + "To disable this check and allow Arquillian to connect to a running server, " + 
                    "set allowConnectingToRunningServer to true in the container configuration");
            }
        } else {
            payaraServerControl.start();
            payaraManager.start();
        }
    }

    @Override
    public void stop() throws LifecycleException {
        if (!connectedToRunningServer) {
            payaraServerControl.stop();
        }
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return payaraManager.deploy(archive);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        payaraManager.undeploy(archive);
    }

    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }
}
