package fish.payara.samples.classloaderdata.api;

import fish.payara.samples.classloaderdata.InstanceCountService;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
/**
 * A simple REST endpoint to return the count of
 * WebappClassLoader instances in the JVM
 * 
 * @author - Cuba Stanley
 */
@Path("instance-count")
public class InstanceResource {
    
    private InstanceCountService instanceCountService = new InstanceCountService();

    /**
     * Method handling HTTP GET request for Instance Count
     *
     * @return int WebappClassLoader instance count
     */
    @GET
    public int getClassLoaderCount() {
        return instanceCountService.getCurrentCount();
    }
    
    @GET
    @Path("previous")
    public int getPreviousInstanceCount() {
        return instanceCountService.getPreviousCount();
    }
}
