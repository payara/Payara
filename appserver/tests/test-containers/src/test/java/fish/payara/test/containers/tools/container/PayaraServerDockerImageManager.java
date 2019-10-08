package fish.payara.test.containers.tools.container;

import java.util.ArrayList;
import java.util.List;

import org.testcontainers.containers.Network;

/**
 * Payara docker image manager. Prepares image and starts the container.
 *
 * @author David Matějček
 */
public class PayaraServerDockerImageManager
    extends JavaDockerImageManager<PayaraServerContainer, PayaraServerContainerConfiguration> {

    /**
     * Creates the manager with Docker's default network.
     *
     * @param cfg
     */
    public PayaraServerDockerImageManager(final PayaraServerContainerConfiguration cfg) {
        this(null, cfg);
    }


    /**
     * Creates the manager.
     *
     * @param network - nullable
     * @param cfg
     */
    public PayaraServerDockerImageManager(final Network network, final PayaraServerContainerConfiguration cfg) {
        super(network, cfg);
    }


    @Override
    protected String getLoggerId() {
        return "D-PAYARA";
    }


    @Override
    protected String getRootDependencyId() {
        return "fish.payara.distributions:payara:zip";
    }


    @Override
    protected String getWebContextToCheck() {
        return "/";
    }


    @Override
    protected PayaraServerContainer createNewContainer() {
        return new PayaraServerContainer(getNameOfPreparedImage());
    }


    @Override
    protected List<NetworkTarget> getTargetsToCheck() {
        final List<NetworkTarget> targets = new ArrayList<>();
        // TODO: targets that would be checked on startup to be available to telnet
        return targets;
    }
}
