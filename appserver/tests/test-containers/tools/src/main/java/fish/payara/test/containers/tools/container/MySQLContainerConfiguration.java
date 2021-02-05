package fish.payara.test.containers.tools.container;

import java.io.File;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Configuration of the MySQL docker container.
 *
 * @author David Matějček
 */
public class MySQLContainerConfiguration {

    private String downloadedDockerImageName;
    private long systemMemory;
    private String hostName;
    private Integer port;
    private String dbUser;
    private String dbPassword;
    private File workingDirectory;


    /**
     * Returns the a name of the available public docker image name that will be downloaded and
     * cached by the Docker, and used to create own docker image used as a base image.
     *
     * @return ie. mysql:8.0.18
     */
    public String getDownloadedDockerImageName() {
        return this.downloadedDockerImageName;
    }


    /**
     * Sets the a name of the available public docker image name that will be downloaded and
     * cached by the Docker, and used to create own docker image used as a base image.
     *
     * @param dockerImageName f.e. mysql:8.0.18
     */
    public void setDownloadedDockerImageName(final String dockerImageName) {
        this.downloadedDockerImageName = dockerImageName;
    }


    /**
     * @return local working directory of the container
     */
    public File getWorkingDirectory() {
        return this.workingDirectory;
    }


    /**
     * @param workingDirectory working directory of the container
     */
    public void setWorkingDirectory(final File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }


    /**
     * @param systemMemory the amount of container's total system memory in gigabytes
     */
    public void setSystemMemory(final long systemMemory) {
        this.systemMemory = systemMemory;
    }


    /**
     * @return the amount of container's total system memory in gigabytes
     */
    public long getSystemMemory() {
        return this.systemMemory;
    }


    /**
     * @return the amount of container's total system memory in bytes
     */
    public long getSystemMemoryInBytes() {
        return this.systemMemory * 1024L * 1024L * 1024L;
    }


    /**
     * @return internal hostname of the mysql docker container
     */
    public String getHostName() {
        return this.hostName;
    }


    /**
     * @param hostName internal hostname of the mysql docker container
     */
    public void setHostName(final String hostName) {
        this.hostName = hostName;
    }


    /**
     * @return internal port of the mysql in the docker container
     */
    public Integer getPort() {
        return this.port;
    }


    /**
     * @param port internal port of the mysql in the docker container
     */
    public void setPort(final Integer port) {
        this.port = port;
    }


    /**
     * @return username for the JDBC connection to the mysql instance in the container
     */
    public String getDbUser() {
        return this.dbUser;
    }


    /**
     * @param user username for the JDBC connection to the mysql instance in the container
     */
    public void setDbUser(final String user) {
        this.dbUser = user;
    }


    /**
     * @return password for the JDBC connection to the mysql instance in the container
     */
    public String getDbPassword() {
        return this.dbPassword;
    }


    /**
     * @param password password for the JDBC connection to the mysql instance in the container
     */
    public void setDbPassword(final String password) {
        this.dbPassword = password;
    }


    /**
     * Returns all properties - one property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
