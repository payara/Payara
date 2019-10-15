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

import fish.payara.test.containers.tools.rs.RestClientCache;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

/**
 * Payara server started as docker container.
 *
 * @author David Matějček
 */
public class PayaraServerContainer extends GenericContainer<PayaraServerContainer> {

    private static final Logger LOG = LoggerFactory.getLogger(PayaraServerContainer.class);
    private static final String PATH_ASADMIN = "/payara5/bin/asadmin";
    private final PayaraServerContainerConfiguration configuration;
    private final RestClientCache clientCache;
    private URI adminUri;


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
     * @return basic URI of the admin GUI, for example: http://payara-domain:4848
     */
    public URI getBaseUri() {
        // lazy init is needed because uri is valid only when the container is running
        if (this.adminUri == null) {
            try {
                final URL url = new URL("http", getContainerIpAddress(), getMappedPort(8080), "");
                this.adminUri = url.toURI();
                LOG.info("Payara domain uri base for requests: {}", this.adminUri);
            } catch (final MalformedURLException | URISyntaxException e) {
                throw new IllegalStateException("Could not initialize the adminUri", e);
            }
        }

        return this.adminUri;
    }


    /**
     * @return absolute path to asadmin command file in the container.
     */
    public String getAsadminPath() {
        return PATH_ASADMIN;
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
     * Executes asadmin without need to access to a running domain.
     *
     * @param command - command name
     * @param arguments - arguments. Can be null.
     */
    public void asLocalAdmin(final String command, final String... arguments) {
        try {
            // FIXME: --echo breaks change-admin-password
            final String[] defaultArgs = new String[] {"--terse"};
            final AsadminCommandExecutor executor = new AsadminCommandExecutor(this, defaultArgs);
            executor.exec(command, arguments);
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
     */
    public void asAdmin(final String command, final String... arguments) {
        try {
            final String[] defaultArgs = new String[] {"--terse", "--user", "admin", //
                "--passwordfile", this.configuration.getPasswordFileInDocker().getAbsolutePath()};
            final AsadminCommandExecutor executor = new AsadminCommandExecutor(this, defaultArgs);
            executor.exec(command, arguments);
        } catch (AsadminCommandException e) {
            throw new IllegalStateException(e);
        }
    }
}
