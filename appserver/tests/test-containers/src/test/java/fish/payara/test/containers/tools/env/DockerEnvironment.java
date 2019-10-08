package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.container.PayaraServerContainer;
import fish.payara.test.containers.tools.container.PayaraServerDockerImageManager;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Manages docker environment for tests.
 *
 * @author David Matějček
 */
public class DockerEnvironment implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DockerEnvironment.class);

    private static DockerEnvironment environment;

    /** Docker network */
    private final Network network;
    private final PayaraServerContainer payaraContainer;

    private final LocalDateTime startupTime;


    /**
     * Creates new singleton instance or throws ISE if it already exists.
     *
     * @param cfg configuration of the docker environment
     * @return new instance.
     * @throws Exception
     */
    public static synchronized DockerEnvironment createEnvironment(final DockerEnvironmentConfiguration cfg)
        throws Exception {
        if (environment != null) {
            throw new IllegalStateException("Environment already created.");
        }

        environment = new DockerEnvironment(cfg);
        return environment;
    }


    /**
     * @return singleton instance.
     */
    public static DockerEnvironment getInstance() {
        return environment;
    }


    /**
     * Initializes the whole environment as configured.
     *
     * @param cfg
     * @throws Exception
     */
    protected DockerEnvironment(final DockerEnvironmentConfiguration cfg) throws Exception {

        // STEP1: Create images (they will be cached)
        LOG.info("Using docker environment configuration:\n{}", cfg);
        this.network = Network.newNetwork();

        final List<CompletableFuture<Void>> parallelFutures = new ArrayList<>();

        final PayaraServerDockerImageManager fbServerMgr = //
            new PayaraServerDockerImageManager(this.network, cfg.getPayaraServerConfiguration());
        parallelFutures.add(CompletableFuture.runAsync(() -> fbServerMgr.prepareImage(cfg.isForceNewPayaraServer())));

        try {
            CompletableFuture.allOf(parallelFutures.toArray(new CompletableFuture[parallelFutures.size()]))
                .get(cfg.getPreparationTimeout(), TimeUnit.SECONDS);
        } catch (final ExecutionException | InterruptedException | TimeoutException e) {
            throw new IllegalStateException("Could not initialize docker environment!", e);
        }

        // STEP2: Create and start containers sequentionally (respect dependencies!)
        this.payaraContainer = fbServerMgr.start();
        logContainerStarted(this.payaraContainer);

        this.startupTime = LocalDateTime.now();
    }


    /**
     * @return time when the environment has been completely initialized.
     */
    public LocalDateTime getStartupTime() {
        return this.startupTime;
    }


    /**
     * @return initialized internal network of the docker environment.
     */
    public Network getNetwork() {
        return this.network;
    }


    /**
     * Resets the logging to lighter (benchmarks) or heavier (functionalities) version.
     *
     * @param fileInContainer
     */
    public void reconfigureLogging(final File fileInContainer) {
        this.payaraContainer.reconfigureLogging(fileInContainer);
    }


    /**
     * @return basic URI of applications, for example: http://host:port/basicContext
     */
    public URI getBaseUri() {
        return this.payaraContainer.getBaseUri();
    }


    @Override
    public void close() {
        if (!this.payaraContainer.isRunning()) {
            return;
        }
        LOG.info("Closing docker containers ...");
        try {
            final ExecResult result = this.payaraContainer.execInContainer("killall", "-v", "java");
            LOG.info("killall output: \n OUT: {}\n ERR: {}", result.getStdout(), result.getStderr());
            Thread.sleep(5000L);
        } catch (final IOException | InterruptedException e) {
            LOG.error("Could not shutdown the server nicely.", e);
        }
        closeSilently(this.payaraContainer);
        closeSilently(this.network);
    }


    private static void logContainerStarted(final GenericContainer<?> container) {
        LOG.info("\n" //
            + "========================================\n"
            + "{}(name: '{}') started, you can use this urls:\n"
            + "https://{}:{}\n"
            + "http://{}:{}\n"
            + "========================================", //
            container.getClass().getSimpleName(), container.getContainerInfo().getName(),
            container.getContainerIpAddress(),
            container.getMappedPort(4848),
            container.getContainerIpAddress(),
            container.getMappedPort(8080));
    }


    private static void closeSilently(final AutoCloseable closeable) {
        LOG.trace("closeSilently(closeable={})", closeable);
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final Exception e) {
            LOG.warn("Close method caused an exception.", e);
        }
    }
}
