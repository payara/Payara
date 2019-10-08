package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.properties.Properties;

import java.io.File;
import java.net.SocketTimeoutException;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Test configuration, see test.properties (filtered by Maven).
 *
 * @author David Matějček
 */
public final class TestConfiguration {

    private static final TestConfiguration CONFIGURATION = new TestConfiguration();

    private final File buildDirectory;

    private final String payaraHost;
    private final int payaraPort;
    private final String payaraUsername;
    private final String payaraPassword;
    private final int jerseyClientConnectionTimeout;
    private final int jerseyClientReadTimeout;


    /**
     * @return instance loaded from the test.properties.
     */
    public static TestConfiguration getInstance() {
        return CONFIGURATION;
    }


    /**
     * Generic constructor. Uses {@link Properties} instance to get values of it's attributes.
     */
    private TestConfiguration() {
        final Properties properties = new Properties("test.properties");

        this.buildDirectory = properties.getFile("build.directory");

        this.payaraHost = properties.getString("docker.payara.host");
        this.payaraPort = properties.getInt("docker.payara.port", 0);
        this.payaraUsername = properties.getString("docker.payara.username");
        this.payaraPassword = properties.getString("docker.payara.password");

        this.jerseyClientConnectionTimeout = properties.getInt("benchmark.client.timeoutInMillis.connect", 0);
        this.jerseyClientReadTimeout = properties.getInt("benchmark.client.timeoutInMillis.read", 0);
    }


    /**
     * @return internal hostname of the payara docker container
     */
    public String getPayaraHost() {
        return this.payaraHost;
    }


    /**
     * @return internal port of the application in the docker container.
     */
    public int getPayaraPort() {
        return this.payaraPort;
    }


    /**
     * @return username valid to login into the application in the docker container.
     */
    public String getPayaraUsername() {
        return this.payaraUsername;
    }


    /**
     * @return password valid to login into the application in the docker container.
     */
    public String getPayaraPassword() {
        return this.payaraPassword;
    }


    /**
     * @return path to the target directory
     */
    public File getBuildDirectory() {
        return this.buildDirectory;
    }


    /**
     * @return path to the benchmark results.
     */
    public File getBenchmarkOutputDirectory() {
        return getBuildDirectory();
    }


    /**
     * @return time in millis; short time causes {@link SocketTimeoutException}
     *         with the "Connection timeout" message.
     */
    public int getJerseyClientConnectionTimeout() {
        return this.jerseyClientConnectionTimeout;
    }


    /**
     * @return time in millis; short time causes {@link SocketTimeoutException}
     *         with the "Read timed out" message.
     */
    public int getJerseyClientReadTimeout() {
        return this.jerseyClientReadTimeout;
    }


    /**
     * Returns all properties - one property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
