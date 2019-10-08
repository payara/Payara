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
    private final RestClientCache clientCache;
    private URI adminUri;


    public PayaraServerContainer(final String nameOfBasicImage) {
        super(nameOfBasicImage);
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
}
