package fish.payara.test.containers;

import fish.payara.test.containers.tools.env.DockerEnvironment;
import fish.payara.test.containers.tools.env.DockerEnvironmentConfiguration;
import fish.payara.test.containers.tools.env.DockerEnvironmentConfigurationParser;
import fish.payara.test.containers.tools.properties.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts one or more docker containers and keeps them running until user stops this application.
 *
 * @author David Matějček
 */
public final class ManualTesting {

    private static final Logger LOG = LoggerFactory.getLogger(ManualTesting.class);

    static {
        Thread.currentThread().setName("main");
    }


    private ManualTesting() {
        // hidden
    }


    /**
     * Starts one or more docker containers with the usage of local PostgreSQL database.
     *
     * @param args not used.
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final Properties properties = new Properties("manual.properties");
        final DockerEnvironmentConfiguration cfg = DockerEnvironmentConfigurationParser.parse(properties);
        final DockerEnvironment docker = DockerEnvironment.createEnvironment(cfg);
        try {
            while (true) {
                Thread.sleep(100L);
            }
        } catch (final InterruptedException e) {
            LOG.trace("Interrupted.");
        } finally {
            docker.close();
            LOG.info("Docker environment is closed.");
        }
    }
}
