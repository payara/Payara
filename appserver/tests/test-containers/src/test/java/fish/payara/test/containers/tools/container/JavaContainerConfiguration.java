package fish.payara.test.containers.tools.container;

import java.io.File;
import java.util.Objects;
import java.util.stream.Stream;

import org.testcontainers.shaded.org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringStyle;

/**
 * Configuration for {@link JavaDockerImageManager}.
 *
 * @author David Matějček
 */
public class JavaContainerConfiguration {

    private String downloadedDockerImageName;
    private long preparationTimeout = 60L;
    private File pomFile;
    private String configDirectoryOnClasspath = "";
    private File testOutputDirectory;
    private File payaraServerDirectory;

    private String host;
    private int adminPort;
    private int httpPort;

    private int systemMemory;
    private String xms;
    private String xmx;
    private String xss;

    private String jaCoCoVersion;
    private File jaCoCoReportDirectory;

    /**
     * Returns a name of the available public docker image name that will be downloaded and cached
     * by the Docker, and used to create own docker image used as a base image for tests.
     *
     * @return f.e. debian:9.5-slim or ubuntu:19.04
     */
    public String getDownloadedDockerImageName() {
        return this.downloadedDockerImageName;
    }


    /**
     * Sets a name of the available public docker image name that will be downloaded and cached
     * by the Docker, and used to create own docker image used as a base image for tests.
     *
     * @param dockerImageName f.e. debian:9.5-slim or ubuntu:19.04
     */
    public void setDownloadedDockerImageName(final String dockerImageName) {
        this.downloadedDockerImageName = dockerImageName;
    }


    /**
     * @return timeout in seconds for preparation of docker images.
     */
    public long getPreparationTimeout() {
        return this.preparationTimeout;
    }


    /**
     * @param preparationTimeout timeout in seconds for preparation of docker images.
     */
    public void setPreparationTimeout(final long preparationTimeout) {
        this.preparationTimeout = preparationTimeout;
    }


    /**
     * @return timeout in seconds for startup of prepared image.
     */
    public long getStartupTimeout() {
        return getPreparationTimeout();
    }


    /**
     * @return path to the pom.xml file of this project.
     */
    public File getPomFile() {
        return this.pomFile;
    }


    /**
     * @param pomFile path to the pom.xml file of this project.
     */
    public void setPomFile(final File pomFile) {
        this.pomFile = pomFile;
    }


    /**
     * @return directory on classpath with the configuration files for docker container.
     */
    public String getConfigDirectoryOnClasspath() {
        return this.configDirectoryOnClasspath;
    }


    /**
     * Sets the directory on classpath used as a root for file system mapping of files
     * into the docker container.
     *
     * @param configDirectoryOnClasspath f.e. "server-side/"
     */
    public void setConfigDirectoryOnClasspath(String configDirectoryOnClasspath) {
        this.configDirectoryOnClasspath = configDirectoryOnClasspath;
    }


    /**
     * @return main configuration directory of the Java application in the container
     */
    public File getConfigDirectoryInDocker() {
        return new File(getPayaraDirectoryInDocker(), "/glassfish/domains/domain/config");
    }


    /**
     * Used to locate the test classes copyied into the container.
     *
     * @return path to the compiled test classes directory
     */
    public File getTestOutputDirectory() {
        return this.testOutputDirectory;
    }


    /**
     * Used to locate the test classes copyied into the container.
     *
     * @param testOutputDirectory path to the compiled test classes directory
     */
    public void setTestOutputDirectory(final File testOutputDirectory) {
        this.testOutputDirectory = testOutputDirectory;
    }


    /**
     * @return path to the directory for the classpath dependencies of the Java application
     *         on the local filesystem.
     */
    public File getPayaraServerDirectory() {
        return this.payaraServerDirectory;
    }


    /**
     * @param payaraServerDirectory path to the directory for the classpath dependencies
     *            of the Java application on the local filesystem.
     */
    public void setPayaraServerDirectory(final File payaraServerDirectory) {
        this.payaraServerDirectory = payaraServerDirectory;
    }


    /**
     * WARNING: don't use this path on local filesystem, it is intended for docker!
     *
     * @return path to the directory for the classpath dependencies in the docker filesystem.
     */
    public File getPayaraDirectoryInDocker() {
        return new File("/payara5");
    }


    /**
     * @return internal hostname of the docker container
     */
    public String getHost() {
        return this.host;
    }


    /**
     * @param host the internal hostname of the docker container
     */
    public void setHost(final String host) {
        this.host = host;
    }


    /**
     * @return internal port of the admin endpoint in the docker container.
     */
    public int getAdminPort() {
        return this.adminPort;
    }


    /**
     * @param port the internal httpPort of the admine endpoint in the docker container
     */
    public void setAdminPort(final int port) {
        this.adminPort = port;
    }


    /**
     * @return internal http port used by applications in the docker container.
     */
    public int getHttpPort() {
        return this.httpPort;
    }


    /**
     * @param port the internal http port used by applications in the docker container
     */
    public void setHttpPort(final int port) {
        this.httpPort = port;
    }


    /**
     * @param systemMemory the amount of container's total system memory in gigabytes
     */
    public void setSystemMemory(final int systemMemory) {
        this.systemMemory = systemMemory;
    }


    /**
     * @return the amount of container's total system memory in gigabytes
     */
    public int getSystemMemory() {
        return this.systemMemory;
    }


    /**
     * @return the amount of container's total system memory in bytes
     */
    public long getSystemMemoryInBytes() {
        return this.systemMemory * 1024L * 1024L * 1024L;
    }


    /**
     * @return JVM option: value for Xms, for example 1g
     */
    public String getXms() {
        return this.xms;
    }


    /**
     * @param xms JVM option: value for Xms, for example 1g
     */
    public void setXms(final String xms) {
        this.xms = xms;
    }


    /**
     * @return JVM option: value for Xmx, for example 1g
     */
    public String getXmx() {
        return this.xmx;
    }


    /**
     * @param xmx JVM option: value for Xmx, for example 1g
     */
    public void setXmx(final String xmx) {
        this.xmx = xmx;
    }


    /**
     * @return JVM option: value for Xss, for example 128k
     */
    public String getXss() {
        return this.xss;
    }


    /**
     * @param xss JVM option: value for Xss, for example 128k
     */
    public void setXss(final String xss) {
        this.xss = xss;
    }


    /**
     * @return true if JaCoCo properties are all set. False otherwise.
     */
    public boolean isJaCoCoEnabled() {
        return Stream.of(getJaCoCoVersion(), getJaCoCoReportDirectory(), getJaCoCoReportDirectoryInDocker())
            .filter(Objects::isNull).count() == 0;
    }


    /**
     * @return version of the jacoco-agent to be used.
     */
    public String getJaCoCoVersion() {
        return this.jaCoCoVersion;
    }


    /**
     * @param version version of the jacoco-agent to be used.
     */
    public void setJaCoCoVersion(final String version) {
        this.jaCoCoVersion = version;
    }


    /**
     * @return JaCoCo output directory on the test side.
     */
    public File getJaCoCoReportDirectory() {
        return this.jaCoCoReportDirectory;
    }


    /**
     * @param jaCoCoReportDirectory JaCoCo output directory on the test side.
     */
    public void setJaCoCoReportDirectory(final File jaCoCoReportDirectory) {
        this.jaCoCoReportDirectory = jaCoCoReportDirectory;
    }


    /**
     * @return JaCoCo output directory in docker container.
     */
    public File getJaCoCoReportDirectoryInDocker() {
        return new File("/tmp/jacoco");
    }


    /**
     * Note: Changes must be reflected also in SonarQube configuration where it is used.
     *
     * @return output file in docker container.
     */
    public File getJaCoCoReportFileInDocker() {
        return new File(getJaCoCoReportDirectoryInDocker(), "jacoco-docker.exec");
    }


    /**
     * Returns all properties - one property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
