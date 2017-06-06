/*
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
// Portions Copyright [2016-2017] [Payara Foundation]
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

    public Class<PayaraManagedContainerConfiguration> getConfigurationClass() {
        return PayaraManagedContainerConfiguration.class;
    }

    public void setup(PayaraManagedContainerConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }

        this.configuration = configuration;
        
        payaraServerControl = new PayaraServerControl(configuration);
        payaraManager = new CommonPayaraManager<PayaraManagedContainerConfiguration>(configuration);
    }

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

    public void stop() throws LifecycleException {
        if (!connectedToRunningServer) {
            payaraServerControl.stop();
        }
    }

    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        return payaraManager.deploy(archive);
    }

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
