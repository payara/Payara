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
