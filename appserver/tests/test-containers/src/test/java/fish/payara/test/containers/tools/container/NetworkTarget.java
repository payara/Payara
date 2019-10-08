package fish.payara.test.containers.tools.container;

import java.util.Objects;

/**
 * Network host and port.
 *
 * @author David Matějček
 */
public class NetworkTarget {

    private final String host;
    private final int port;


    /**
     * @param host mandatory, inet address or hostname
     * @param port mandatory positive number
     */
    public NetworkTarget(final String host, final int port) {
        this.host = Objects.requireNonNull(host, "host");
        this.port = port;
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid port");
        }
    }


    /**
     * @return hostname, never null
     */
    public String getHost() {
        return this.host;
    }


    /**
     * @return positive number
     */
    public int getPort() {
        return this.port;
    }


    @Override
    public String toString() {
        return this.host + ':' + this.port;
    }
}
