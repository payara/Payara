package fish.payara.test.containers.tools.container;

import static org.testcontainers.containers.BindMode.READ_WRITE;

import com.github.dockerjava.api.model.Ulimit;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Docker image manager usable to run Java applications. Prepares image and starts the container.
 *
 * @param <T> Supported successor of the {@link GenericContainer}
 * @param <C> Supported {@link JavaContainerConfiguration} type
 * @author David Matějček
 */
public abstract class JavaDockerImageManager<T extends GenericContainer<T>, C extends JavaContainerConfiguration>
    extends DockerImageManager {

    private static final Logger LOG = LoggerFactory.getLogger(JavaDockerImageManager.class);
    private static final String REPACKED_JAR_NAMEADDON = "";
    private static final String USERNAME = "payara";

    private final C cfg;


    /**
     * Creates the manager.
     *
     * @param network can be null
     * @param cfg mandatory
     */
    public JavaDockerImageManager(final Network network, final C cfg) {
        super(cfg.getDownloadedDockerImageName(), network);
        this.cfg = Objects.requireNonNull(cfg, "cfg");
    }


    /**
     * @return ID of the containr's logger (stdout) mapped into host's SLF4J+LOG4J
     */
    protected abstract String getLoggerId();


    /**
     * @return maven id in form groupId:artifactId
     */
    protected abstract String getRootDependencyId();


    /**
     * WARN: don't give the image directly to a container, it would be changed or deleted
     * (depends on settigs)
     *
     * @return T
     */
    protected abstract T createNewContainer();


    /**
     * @return each host and port must be reachable from the container inside.
     */
    protected abstract List<NetworkTarget> getTargetsToCheck();


    /**
     * Returns context to check if the container and application is completely started - then it
     * should respond with HTTP 200.
     *
     * @return slash by default
     */
    protected String getWebContextToCheck() {
        return "/";
    }


    /**
     * @return configuration given in constructor
     */
    protected C getConfiguration() {
        return this.cfg;
    }


    @Override
    public String getInstallCommand() {
        return "true" //
            + " && cat /etc/hosts && cat /etc/resolv.conf" //
            + " && apt-get update" //
            + " && apt-get -y install netcat net-tools inetutils-ping unzip zip locales psmisc wget gnupg lsb-core" //
            + " && sed -i -e 's/# cs_CZ.UTF-8 UTF-8/cs_CZ.UTF-8 UTF-8/' /etc/locale.gen"//
            + " && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen"//
            + " && locale-gen" //
            + " && export LANG=\"cs_CZ.UTF-8\"" //
            + " && export LANGUAGE=\"cs_CZ.UTF-8\"" //
            + " && mkdir -p /usr/share/man/man1" //
            + " && apt-get -y install openjdk-8-jdk-headless" //
            + " && apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*" //
            + " && adduser " + USERNAME //
        ;
    }


    @Override
    public T start() {
        LOG.debug("Creating and starting container from image {} ...", getNameOfPreparedImage());
        try {
            final T container = createNewContainer(); //
            container.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger(getLoggerId())));
            container.withCopyFileToContainer( //
                MountableFile.forClasspathResource("server-side/fixEclipseJars.sh", 0777),
                "/usr/local/bin/fixEclipseJars.sh");
            container.withFileSystemBind(this.cfg.getPayaraServerDirectory().getAbsolutePath(),
                this.cfg.getPayaraDirectoryInDocker().getAbsolutePath(), READ_WRITE); //
            if (this.cfg.isJaCoCoEnabled()) {
                final File jaCoCoReportDirectory = Objects.requireNonNull(getConfiguration().getJaCoCoReportDirectory(),
                    "configuration.jaCoCoReportDirectory");
                if (!jaCoCoReportDirectory.isDirectory() && !jaCoCoReportDirectory.mkdirs()) {
                    throw new IllegalStateException(
                        "Cannot create JaCoCo output directory needed for filesystem binding.");
                }
                container.withFileSystemBind(this.cfg.getJaCoCoReportDirectory().getAbsolutePath(),
                    this.cfg.getJaCoCoReportDirectoryInDocker().getAbsolutePath(), READ_WRITE);
            }
            container.withNetwork(getNetwork()); //
            container.withNetworkMode("bridge");
            container.withExposedPorts(this.cfg.getAdminPort(), this.cfg.getHttpPort()); //
            container.withEnv("TZ", "UTC").withEnv("LC_ALL", "en_US.UTF-8"); //
            container.withCreateContainerCmdModifier(cmd -> {
                // see https://github.com/zpapez/docker-java/wiki
                cmd.getHostConfig().withMemory(this.cfg.getSystemMemoryInBytes()); //
                cmd.getHostConfig().withUlimits(new Ulimit[] {new Ulimit("nofile", 4096, 8192)}); //
                cmd.withHostName(this.cfg.getHost());
                cmd.withUser(USERNAME);
            }); //

            final StringBuilder command = new StringBuilder();
            command.append("true"); //
            command.append(" && lsb_release -a"); //
            command.append(" && ulimit -a"); //
            command.append(" && fixEclipseJars.sh ").append(this.cfg.getPayaraDirectoryInDocker()).append("/*")
                .append(' ').append(REPACKED_JAR_NAMEADDON);
            for (final NetworkTarget hostAndPort : getTargetsToCheck()) {
                command.append(" && nc -v -z -w 1 ") //
                    .append(hostAndPort.getHost()).append(' ').append(hostAndPort.getPort());
            }
            command.append(" && (env | sort) && locale"); //
            command.append(" && hostname && netstat -r -n && netstat -ln"); //
            if (this.cfg.isJaCoCoEnabled()) {
                command.append(" && unzip -o ").append(this.cfg.getPayaraDirectoryInDocker()) // FIXME: to lib/domain directory!
                    .append("/org.jacoco.agent-").append(this.cfg.getJaCoCoVersion()).append(".jar")
                    .append(" \"jacocoagent.jar\" -d ").append(this.cfg.getPayaraDirectoryInDocker()); //
            }
            command.append(" && ls -la ").append(this.cfg.getPayaraDirectoryInDocker()); //

            command.append(" && ").append(getAsadminFile()).append(" start-domain domain1");
            command.append(" && sleep infinity"); //

            container.withCommand("/bin/sh", "-c", command.toString()) //
                .waitingFor(//
                    Wait.forHttp(getWebContextToCheck()).forStatusCode(200)
                        .withStartupTimeout(Duration.ofSeconds(this.cfg.getStartupTimeout()))) //
            ;
            container.start();
            LOG.info("Payara server container started.");
            return container;
        } catch (final Exception e) {
            throw new IllegalStateException("Could not install Payara Server Docker container!", e);
        }
    }


    private File getAsadminFile() {
        return new File(this.cfg.getPayaraDirectoryInDocker(), "/bin/asadmin");
    }
}
