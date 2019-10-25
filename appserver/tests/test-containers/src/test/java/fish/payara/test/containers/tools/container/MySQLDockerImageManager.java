package fish.payara.test.containers.tools.container;

import java.time.Duration;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * MySQL image manager. Prepares and starts the container for tests.
 *
 * @author David Matějček
 */
public class MySQLDockerImageManager extends DockerImageManager {

    private static final Logger LOG = LoggerFactory.getLogger(MySQLDockerImageManager.class);

    private final MySQLContainerConfiguration cfg;


    /**
     * @param network - mandatory
     * @param cfg - mandatory
     */
    public MySQLDockerImageManager(final Network network, final MySQLContainerConfiguration cfg) {
        super(cfg.getDownloadedDockerImageName(), network);
        Objects.requireNonNull(network, "network");
        this.cfg = Objects.requireNonNull(cfg, "cfg");
    }


    @Override
    public String getInstallCommand() {
        return "cat /etc/hosts && cat /etc/resolv.conf" //
        ;
    }


    @Override
    @SuppressWarnings("resource") // implements Closeable
    public MySQLContainer<?> start() {
        LOG.debug("Creating and starting container from image {} ...", getNameOfPreparedImage());
        final MySQLContainer<?> container = new MySQLContainer<>(getNameOfPreparedImage())
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("D-MYSQL"))) //
            // FIXME: move to the configuration!
            .withDatabaseName("testdb") //
            .withUsername(this.cfg.getDbUser()).withPassword(this.cfg.getDbPassword()) //
            .withStartupTimeout(Duration.ofSeconds(60))//
            .withExposedPorts(this.cfg.getPort()) //
            .withNetwork(getNetwork()) //
//            .withNetworkMode("bridge") //
            .withNetworkAliases(this.cfg.getHostName())
            .withEnv("TZ", "UTC").withEnv("LC_ALL", "en_US.UTF-8") //
            .withSharedMemorySize(256 * 1024L * 1024L) //
            .withWorkingDirectory(this.cfg.getWorkingDirectory().getAbsolutePath()) //
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withHostName(this.cfg.getHostName());
                cmd.getHostConfig().withMemory(this.cfg.getSystemMemoryInBytes()); //
            });
        container.start();
        LOG.info("MySQL container started.");
        return container;
    }
}
