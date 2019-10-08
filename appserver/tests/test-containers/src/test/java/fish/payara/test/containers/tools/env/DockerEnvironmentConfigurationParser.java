package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import fish.payara.test.containers.tools.properties.Properties;

import java.util.Objects;

/**
 * Parses {@link DockerEnvironmentConfiguration} from standardized properties.
 *
 * @author David Matějček
 */
public final class DockerEnvironmentConfigurationParser {

    private DockerEnvironmentConfigurationParser() {
        // hidden
    }


    /**
     * Parses the properties.
     *
     * @param properties
     * @return {@link DockerEnvironmentConfiguration}, never null
     */
    public static DockerEnvironmentConfiguration parse(final Properties properties) {
        Objects.requireNonNull(properties, "properties");
        final DockerEnvironmentConfiguration cfg = new DockerEnvironmentConfiguration();

        cfg.setPayaraServerConfiguration(parseServerConfiguration(properties));
        cfg.setForceNewPayaraServer(properties.getBoolean("docker.payara.image.forceNew", false));

        return cfg;
    }


    private static PayaraServerContainerConfiguration parseServerConfiguration(final Properties properties) {
        final PayaraServerContainerConfiguration cfg = new PayaraServerContainerConfiguration();
        cfg.setDownloadedDockerImageName(properties.getString("docker.payara.image.base"));
        cfg.setPreparationTimeout(properties.getLong("docker.images.timeoutInSeconds", 60));

        cfg.setPayaraServerDirectory(properties.getFile("docker.payara.directory"));
        cfg.setTestOutputDirectory(properties.getFile("build.testOutputDirectory"));
        cfg.setPomFile(properties.getFile("build.pomFile"));
        cfg.setConfigDirectoryOnClasspath("server-side/");

        cfg.setHost(properties.getString("docker.payara.host"));
        cfg.setAdminPort(properties.getInt("docker.payara.port.admin", 4848));
        cfg.setHttpPort(properties.getInt("docker.payara.port.http", 8080));

        cfg.setSystemMemory(properties.getInt("docker.payara.memory.totalInGB", 1));
        cfg.setXms(properties.getString("docker.payara.jvm.xms"));
        cfg.setXmx(properties.getString("docker.payara.jvm.xmx"));
        cfg.setXss(properties.getString("docker.payara.jvm.xss"));

        cfg.setJaCoCoReportDirectory(properties.getFile("jacoco.reportDirectory"));
        cfg.setJaCoCoVersion(properties.getString("jacoco.version"));
        return cfg;
    }
}
