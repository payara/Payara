package fish.payara.samples.resource.fish6479;

import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@Path("/")
@Stateless
public class Slf4jResource {

    private final Logger logger = LoggerFactory.getLogger(Slf4jResource.class);
    private final Marker fatal = MarkerFactory.getMarker("FATAL");

    @GET
    @Path("info")
    public String getInfo() {
        logger.info("*** Info message ***");
        return "[\"Info\"]";
    }

    @GET
    @Path("debug")
    public String getDebug() {
        logger.debug("*** Debug message ***");
        return "[\"Debug\"]";
    }

    @GET
    @Path("warn")
    public String getWarn() {
        logger.warn("*** Warn message ***");
        return "[\"Warn\"]";
    }

    @GET
    @Path("fatal")
    public String getFatal() {
        logger.error(fatal, "*** Fatal message ***");
        return "[\"Fatal\"]";
    }
}
