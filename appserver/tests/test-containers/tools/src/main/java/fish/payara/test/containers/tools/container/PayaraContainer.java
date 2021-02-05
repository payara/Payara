/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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

import com.github.dockerjava.api.model.ContainerNetwork;

import fish.payara.test.containers.tools.rs.RestClientCache;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

import javax.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Payara implementation started as docker container.
 *
 * @param <SELF>
 * @author David Matějček
 */
public abstract class PayaraContainer<SELF extends PayaraContainer<SELF>> extends GenericContainer<SELF> {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraContainer.class);
    private final PayaraServerContainerConfiguration configuration;

    // FIXME: move it out, it is not a feature of the container. But it's life is bound to it.
    private final RestClientCache clientCache;


    /**
     * Creates an instance.
     *
     * @param nameOfBasicImage - name of the docker image to use
     * @param configuration
     */
    public PayaraContainer(final String nameOfBasicImage,
        final PayaraServerContainerConfiguration configuration) {
        super(nameOfBasicImage);
        this.configuration = configuration;
        this.clientCache = new RestClientCache();
    }


    // FIXME delete after making paths public
    public PayaraServerContainerConfiguration getConfiguration() {
        return this.configuration;
    }


    /**
     * @return {@link URL}, where tests can access the appllication HTTP port.
     */
    public URL getHttpUrl() {
        return getExternalUrl("http", TestablePayaraPort.DAS_HTTP_PORT);
    }


    /**
     * @return {@link URL}, where tests can access the application HTTPS port.
     */
    public URL getHttpsUrl() {
        return getExternalUrl("https", TestablePayaraPort.DAS_HTTPS_PORT);
    }


    /**
     * @return basic JAX-RS client for the {@link #getHttpUrl()}
     */
    public WebTarget getAnonymousBasicWebTarget() {
        return clientCache.getAnonymousClient().target(toURI(getHttpUrl()));
    }


    /**
     * Returns URL usable from the host (test)
     *
     * @param protocol
     * @param internalPort internal port, will be transformed using {@link #getMappedPort(int)}
     * @return protocol://ip:port/, ie. http://127.0.0.1:12345/
     */
    protected URL getExternalUrl(final String protocol, final TestablePayaraPort internalPort) {
        try {
            return new URL(protocol, getHost(), getMappedPort(internalPort.getPort()), "/");
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(
                "Could not create external url for protocol '" + protocol + "' and port " + internalPort, e);
        }
    }


    private URI toURI(final URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("This URL cannot be converted to URI: " + url, e);
        }
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
}
