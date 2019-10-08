package fish.payara.test.containers.tools.container;

import java.io.File;

/**
 * Configuration for the {@link PayaraServerContainer}
 *
 * @author David Matějček
 */
public class PayaraServerContainerConfiguration extends JavaContainerConfiguration {

    private static final String JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE = "jacoco-docker-payara-server.exec";

    /**
     * @return output file in docker container (path to the
     *         {@value #JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE})
     */
    @Override
    public File getJaCoCoReportFileInDocker() {
        return new File(getJaCoCoReportDirectoryInDocker(), JACOCO_DOCKER_PAYARA_SERVER_EXEC_FILE);
    }
}
