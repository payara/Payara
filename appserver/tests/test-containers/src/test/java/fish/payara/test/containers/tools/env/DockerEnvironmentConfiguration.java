package fish.payara.test.containers.tools.env;

import fish.payara.test.containers.tools.container.PayaraServerContainerConfiguration;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.testcontainers.shaded.org.apache.commons.lang.builder.ToStringStyle;

/**
 * Configuration of the docker environment.
 * <p>
 * Note: items in each subconfiguration should respect relations between containers, especially
 * ports and hostnames
 *
 * @author David Matějček
 */
public class DockerEnvironmentConfiguration {

    private boolean forceNewPayaraServer;

    private PayaraServerContainerConfiguration payaraServerConfiguration;


    /**
     * @return true to delete prepared docker image and to create it again
     */
    public boolean isForceNewPayaraServer() {
        return this.forceNewPayaraServer;
    }


    /**
     * @param forceNewPayaraServer true to delete prepared docker image and to create it again
     */
    public void setForceNewPayaraServer(final boolean forceNewPayaraServer) {
        this.forceNewPayaraServer = forceNewPayaraServer;
    }


    /**
     * @return {@link PayaraServerContainerConfiguration} - configuration of the Payara Docker
     *         container
     */
    public PayaraServerContainerConfiguration getPayaraServerConfiguration() {
        return this.payaraServerConfiguration;
    }


    /**
     * @param payaraServerConfiguration - configuration of the Payara Docker container
     */
    public void setPayaraServerConfiguration(final PayaraServerContainerConfiguration payaraServerConfiguration) {
        this.payaraServerConfiguration = payaraServerConfiguration;
    }


    /**
     * @return timeout of preparation of all container images in secodns.
     */
    public long getPreparationTimeout() {
        return this.payaraServerConfiguration.getPreparationTimeout();
    }


    /**
     * Returns all properties - each property on own line.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}
