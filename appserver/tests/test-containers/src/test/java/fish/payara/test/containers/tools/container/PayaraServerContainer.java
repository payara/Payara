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
package fish.payara.test.containers.tools.container;

import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.ContainerNetwork;

import fish.payara.test.containers.tools.rs.RestClientCache;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.FixedHostPortGenericContainer;

/**
 * Payara server started as docker container.
 *
 * @author David Matějček
 */
public class PayaraServerContainer extends FixedHostPortGenericContainer<PayaraServerContainer> {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraServerContainer.class);
    private final PayaraServerContainerConfiguration configuration;
    private final RestClientCache clientCache;


    /**
     * Creates an instance.
     *
     * @param nameOfBasicImage - name of the docker image to use
     * @param configuration
     */
    public PayaraServerContainer(final String nameOfBasicImage,
        final PayaraServerContainerConfiguration configuration) {
        super(nameOfBasicImage);
        this.configuration = configuration;
        this.clientCache = new RestClientCache();
    }


    /**
     * @return absolute path to asadmin command file in the container.
     */
    public File getAsadmin() {
        return new File(configuration.getPayaraMainDirectoryInDocker(), "bin/asadmin");
    }


    /**
     * @return {@link URL}, where tests can access the domain admin port.
     */
    public URL getAdminUrl() {
        return getExternalUrl("https", configuration.getAdminPort());
    }


    /**
     * @return {@link URL}, where tests can access the appllication HTTP port.
     */
    public URL getHttpUrl() {
        return getExternalUrl("http", configuration.getHttpPort());
    }


    /**
     * @return {@link URL}, where tests can access the application HTTPS port.
     */
    public URL getHttpsUrl() {
        return getExternalUrl("https", configuration.getHttpsPort());
    }


    private URL getExternalUrl(String protocol, int internalPort) {
        try {
            return new URL(protocol, getContainerIpAddress(), getMappedPort(internalPort), "/");
        } catch (MalformedURLException e) {
            throw new IllegalStateException(
                "Could not create external url for protocol '" + protocol + "' and port " + internalPort, e);
        }
    }


    @Override
    public void close() {
        this.clientCache.close();
    }


    /**
     * @param fileInContainer
     */
    public void reconfigureLogging(File fileInContainer) {
        // TODO: to be done later.
    }


    /**
     * @return IP address usable in container network
     */
    public String getVirtualNetworkIpAddress() {
        final Collection<ContainerNetwork> networks = getContainerInfo().getNetworkSettings().getNetworks().values();
        LOG.trace("networks: {}", networks);
        return networks.stream().filter(n -> n.getNetworkID().equals(getNetwork().getId())).findAny()
            .map(ContainerNetwork::getIpAddress).orElse("127.0.0.1");
    }


    /**
     * Executes asadmin without need to access to a running domain.
     *
     * @param command - command name
     * @param arguments - arguments. Can be null.
     * @return standard output
     */
    public String asLocalAdmin(final String command, final String... arguments) {
        try {
            // FIXME: --echo breaks change-admin-password
            final String[] defaultArgs = new String[] {"--terse"};
            final AsadminCommandExecutor executor = new AsadminCommandExecutor(this, defaultArgs);
            return executor.exec(command, arguments);
        } catch (AsadminCommandException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Executes asadmin command against running domain instance.
     *
     * @param command - command name
     * @param arguments - arguments. Can be null and should not contain parameters used before
     *            the command.
     * @return standard ouptut
     */
    public String asAdmin(final String command, final String... arguments) {
        try {
            final String[] defaultArgs = new String[] {"--terse", "--user", "admin", //
                "--passwordfile", this.configuration.getPasswordFileInDocker().getAbsolutePath()};
            final AsadminCommandExecutor executor = new AsadminCommandExecutor(this, defaultArgs);
            return executor.exec(command, arguments);
        } catch (AsadminCommandException e) {
            throw new IllegalStateException(e);
        }
    }


    /**
     * Calls hosts docker on port 2376. The port must be exposed into container with the
     * {@link Testcontainers#exposeHostPorts(int...)} before the container is created.
     *
     * @param path
     * @param json
     * @return stdout
     */
    public String docker(final String path, final String json) {
        LOG.debug("docker(path={}, json=\n{}\n)", path, json);
        try {
            final ExecResult result = execInContainer( //
                "curl", "-sS", //
                "-H", "Accept: application/json", "-H", "Content-Type: application/json", //
                "-i", "http://host.testcontainers.internal:2376/" + path, //
                "--data", json);
            if (result.getExitCode() != 0) {
                throw new DockerClientException(result.getStderr());
            }
            return result.getStdout();
        } catch (final UnsupportedOperationException | IOException | InterruptedException e) {
            throw new IllegalStateException("Could not execute docker command via curl.", e);
        }
    }
}
