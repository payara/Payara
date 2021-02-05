package fish.payara.test.containers.tst.log.war;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Used by the container to register the REST application.
 *
 * @author David Matějček
 */
// WARNING: don't move this class, it must be in root package of all your REST services.
@ApplicationPath("")
public class RestAppConfig extends ResourceConfig {

    /**
     * Instantiates a new rest app config.
     */
    public RestAppConfig() {
        packages(//
            RestAppConfig.class.getPackage().getName() //
        );
    }
}
