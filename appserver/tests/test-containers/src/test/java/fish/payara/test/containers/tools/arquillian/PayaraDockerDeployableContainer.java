/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tools.arquillian;

import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.junit.DockerITestExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import javax.enterprise.deploy.spi.Target;

import org.glassfish.deployment.client.DFDeploymentProperties;
import org.glassfish.deployment.client.DFDeploymentStatus;
import org.glassfish.deployment.client.DFProgressObject;
import org.glassfish.deployment.client.RemoteDeploymentFacility;
import org.glassfish.deployment.client.ServerConnectionIdentifier;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Arquillian container can deploy archives to running Payara Docker Container.
 *
 * @author David Matejcek
 */
public class PayaraDockerDeployableContainer implements DeployableContainer<PayaraDockerContainerConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraDockerDeployableContainer.class);

    private DockerEnvironment environment;

    @Override
    public Class<PayaraDockerContainerConfiguration> getConfigurationClass() {
        return PayaraDockerContainerConfiguration.class;
    }

    @Override
    public void setup(final PayaraDockerContainerConfiguration configuration) {
        LOG.debug("setup(configuration={})", configuration);
        environment = DockerEnvironment.getInstance();
        if (environment == null) {
            throw new IllegalStateException(
                "Docker environment is not initialized, did you forget to use JUnit5 extension "
                    + DockerITestExtension.class + "?");
        }
    }

    @Override
    public void start() throws LifecycleException {
        LOG.debug("start()");
    }

    @Override
    public void stop() throws LifecycleException {
        LOG.trace("stop()");
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        LOG.debug("deploy(archive={})", archive);
        if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        final String applicationName = createDeploymentName(archive.getName());
        final PayaraServerContainer payara = environment.getPayaraContainer();
        final RemoteDeploymentFacility deployer = connect(payara);
        try {
            deploy(applicationName, archive, deployer);

            final HTTPContext httpContext = new HTTPContext(payara.getContainerIpAddress(),
                payara.getHttpUrl().getPort());

            payara.asAdmin("show-component-status", applicationName);

            httpContext.add(new Servlet(applicationName, applicationName));
            final List<String> modules = getModuleNames(applicationName, deployer);
            LOG.info("modules: {}", modules);


            final ProtocolMetaData protocolMetaData = new ProtocolMetaData();
            protocolMetaData.addContext(httpContext);
            return protocolMetaData;
        } finally {
            deployer.disconnect();
        }
    }


    private List<String> getModuleNames(final String applicationName, final RemoteDeploymentFacility deployer) {
        try {
            return deployer.getSubModuleInfoForJ2EEApplication(applicationName);
        } catch (final IOException e) {
            throw new IllegalStateException("Could not get names of submodules", e);
        }
    }

    @Override
    public void undeploy(final Archive<?> archive) throws DeploymentException {
        // TODO Not implemented yet!

    }

    @Override
    public void deploy(final Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(final Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private String createDeploymentName(final String archiveName) {
        String correctedName = archiveName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }

        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }

        return correctedName;
    }


    private RemoteDeploymentFacility connect(final PayaraServerContainer payara) {
        LOG.trace("connect(payara={})", payara);
        final RemoteDeploymentFacility deployer = new RemoteDeploymentFacility();
        final ServerConnectionIdentifier sci = new ServerConnectionIdentifier();

        sci.setHostName(payara.getContainerIpAddress());
        sci.setHostPort(payara.getAdminUrl().getPort());
        // FIXME: get it from PayaraServerContainer
        sci.setUserName("admin");
        sci.setPassword("admin123");

        deployer.connect(sci);
        return deployer;
    }


    private void deploy(final String applicationName, final Archive<?> archive, final RemoteDeploymentFacility deployer)
        throws DeploymentException {
        LOG.debug("deploy(applicationName={}, archive, deployer={})", applicationName, deployer);
        final DFDeploymentProperties options = new DFDeploymentProperties();
        options.setName(applicationName);
        options.setEnabled(true);
        options.setForce(true);
        options.setUpload(true);
        options.setTarget("server");
        final Properties props = new Properties();
        props.setProperty("keepSessions", "true");
        options.setProperties(props);

        try (InputStream archiveStream = archive.as(ZipExporter.class).exportAsInputStream()) {
            final MemoryMappedArchive deployedArchive = new MemoryMappedArchive(archiveStream);
            final Target[] targets = new Target[] {deployer.createTarget("server")};
            final DFProgressObject progressObject = deployer.deploy(targets, deployedArchive, null, options);
            final DFDeploymentStatus deploymentStatus = progressObject.waitFor();
            LOG.info("Deployment status: {}", deploymentStatus);
            if (deploymentStatus.getStatus() == DFDeploymentStatus.Status.FAILURE) {
                throw new DeploymentException("Deployment failed!" + deploymentStatus.getAllStageMessages());
            }
        } catch (final IOException e) {
            throw new DeploymentException("Deployment failed!", e);
        }
    }
}
