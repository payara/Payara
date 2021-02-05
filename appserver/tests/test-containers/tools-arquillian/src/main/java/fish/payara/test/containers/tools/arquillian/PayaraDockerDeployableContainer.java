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

import fish.payara.test.containers.tools.container.AsadminCommandException;
import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.junit.DockerITestExtension;

import java.net.URL;
import java.util.Objects;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
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

    private static final String APP_CONTEXT_ROOT = "/";
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
        LOG.debug("stop()");
    }


    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }


    @Override
    public ProtocolMetaData deploy(final Archive<?> archive) throws DeploymentException {
        LOG.debug("deploy(archive={})", archive);
        Objects.requireNonNull(archive, "archive must not be null");

        final PayaraServerContainer payara = environment.getPayaraContainer();
        final String applicationName = payara.deploy(APP_CONTEXT_ROOT, archive);
        logDeployedApplicationStatus(payara, applicationName);

        // FIXME: HTTP or HTTPS?
        final URL httpUrl = payara.getHttpUrl();
        final HTTPContext httpContext = new HTTPContext(httpUrl.getHost(), httpUrl.getPort());
        httpContext.add(new Servlet(applicationName, APP_CONTEXT_ROOT));

        final ProtocolMetaData protocolMetaData = new ProtocolMetaData();
        protocolMetaData.addContext(httpContext);
        return protocolMetaData;
    }


    @Override
    public void undeploy(final Archive<?> archive) throws DeploymentException {
        LOG.debug("undeploy(archive={})", archive);
        Objects.requireNonNull(archive, "archive must not be null");
        final PayaraServerContainer payara = environment.getPayaraContainer();
        payara.undeploy(archive);
    }


    @Override
    public void deploy(final Descriptor descriptor) throws DeploymentException {
        LOG.debug("deploy(descriptor={})", descriptor);
        throw new UnsupportedOperationException("Not implemented");
    }


    @Override
    public void undeploy(final Descriptor descriptor) throws DeploymentException {
        LOG.debug("undeploy(descriptor={})", descriptor);
        throw new UnsupportedOperationException("Not implemented");
    }


    /** see logs for the response. No need to process the response. */
    private void logDeployedApplicationStatus(final PayaraServerContainer payara, final String applicationName)
        throws DeploymentException {
        try {
            payara.asAdmin("show-component-status", applicationName);
        } catch (final AsadminCommandException e) {
            throw new DeploymentException("Deployment passed, but asadmin command failed to get component status.", e);
        }
    }
}
