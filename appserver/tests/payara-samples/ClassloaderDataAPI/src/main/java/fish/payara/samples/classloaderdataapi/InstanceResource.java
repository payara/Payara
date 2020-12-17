package fish.payara.samples.classloaderdataapi;

import java.lang.ref.Reference;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import org.glassfish.web.loader.WebappClassLoader;
import org.glassfish.web.loader.WebappClassLoaderFinalizer;

/**
 * A simple REST endpoint to return the count of
 * WebappClassLoader instances in the JVM
 * 
 * @author - Cuba Stanley
 */
@Path("instance-count")
public class InstanceResource {
    
    private int previousInstanceCount = 0;

    /**
     * Method handling HTTP GET request for Instance Count
     *
     * @return int WebappClassLoader instance count
     */
    @GET
    @Path("new")
    public int getClassLoaderCount() {
        System.gc();
        Reference<? extends WebappClassLoader> referenceFromQueue;
        while((referenceFromQueue = WebappClassLoader.referenceQueue.poll()) != null) {
            ((WebappClassLoaderFinalizer)referenceFromQueue).cleanupAction();
            referenceFromQueue.clear();
        }
        previousInstanceCount = WebappClassLoader.getInstanceCount();
        return previousInstanceCount;
    }
    
    @GET
    @Path("previous")
    public int getPreviousInstanceCount() {
        return previousInstanceCount;
    }
}
