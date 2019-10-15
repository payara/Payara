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
package fish.payara.test.containers.tools.junit;

import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.TestConfiguration;
import fish.payara.test.containers.tools.rs.RestClientCache;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Common parent for all dockered tests.
 *
 * @author David Matějček
 */
@ExtendWith(DockerITestExtension.class)
public abstract class DockerITest {

    private static final Logger LOG = LoggerFactory.getLogger(DockerITest.class);

    private static DockerEnvironment dockerEnvironment;
    private static RestClientCache clientCache;


    /**
     * Automatically initializes the environment before all tests.
     *
     * @throws Exception
     */
    @BeforeAll
    public static void initEnvironment() throws Exception {
        LOG.info("initEnvironment()");
        dockerEnvironment = DockerEnvironment.getInstance();
        assertNotNull(dockerEnvironment, "dockerEnvironment");
        clientCache = new RestClientCache();
    }


    /**
     * Closes all cached clients.
     */
    @AfterAll
    public static void closeClientCache() {
        if (clientCache != null) {
            clientCache.close();
        }
    }


    /**
     * @return actual {@link DockerEnvironment} instance.
     */
    public static DockerEnvironment getDockerEnvironment() {
        return dockerEnvironment;
    }


    /**
     * @return configuration loaded from test.properties filtered by Maven.
     */
    public static TestConfiguration getTestConfiguration() {
        return TestConfiguration.getInstance();
    }


    /**
     * @return basic URI of the application, for example: http://host:port/basicContext
     */
    public static URI getBaseUri() {
        return dockerEnvironment.getBaseUri();
    }


    /**
     * @param context subcontext, f.e. /something
     * @return URI of a servlet (existing or non-existing), for example:
     *         http://host:port/basicContext/something
     */
    public static URI getBaseUri(final String context) {
        return URI.create(dockerEnvironment.getBaseUri() + context);
    }


    /**
     * @return basic JAX-RS client for the {@link #getBaseUri()}
     */
    public static WebTarget getAnonymousBasicWebTarget() {
        return clientCache.getAnonymousClient().target(getBaseUri());
    }


    /**
     * This client sends login and password only if the last response is "unauthenticated".
     * You don't need not close the client, it is cached.
     *
     * @param followRedirects
     * @param username
     * @param password
     * @return basic JAX-RS client
     */
    public static Client getClient(final boolean followRedirects, final String username, final String password) {
        return clientCache.getPreemptiveClient(followRedirects, username, password);
    }


    /**
     * Returns a client that sends login and password only if the last response is
     * "unauthenticated".
     *
     * @param followRedirects
     * @param username
     * @param password
     * @return {@link WebTarget} for the basic HTTP uri
     */
    public static WebTarget getBasicWebTarget(final boolean followRedirects, final String username,
        final String password) {
        return getClient(followRedirects, username, password).target(getBaseUri());
    }


    /**
     * Returns a client that sends login and password only if the last response is
     * "unauthenticated". The basic HTTP url is appended by the urlContextParts
     * separated by the '/' character.
     *
     * @param followRedirects
     * @param username
     * @param password
     * @param urlContextParts
     * @return {@link WebTarget}
     */
    public static WebTarget getWebTarget(final boolean followRedirects, final String username, final String password,
        final String... urlContextParts) {
        final Client client = getClient(followRedirects, username, password);
        WebTarget target = client.target(getBaseUri());
        if (urlContextParts == null) {
            return target;
        }
        for (final String urlContextPart : urlContextParts) {
            target = target.path(urlContextPart);
        }
        return target;
    }
}
