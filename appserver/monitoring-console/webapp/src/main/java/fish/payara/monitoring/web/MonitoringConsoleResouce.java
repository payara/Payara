package fish.payara.monitoring.web;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.internal.api.Globals;

import fish.payara.monitoring.collect.SinkDataCollector;
import fish.payara.monitoring.collect.MonitoringDataCollector;
import fish.payara.monitoring.collect.MonitoringDataSource;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class MonitoringConsoleResouce {

    
    @GET
    @Path("/points")
    public Map<String, Long> getPoints(@QueryParam("t") long timestamp) {
        Map<String, Long> res = new TreeMap<>();
        List<MonitoringDataSource> sources = Globals.getDefaultBaseServiceLocator().getAllServices(MonitoringDataSource.class);
        MonitoringDataCollector collector = new SinkDataCollector((key, value) -> res.put(key.toString(), value));
        for (MonitoringDataSource source : sources) {
            try {
                source.collect(collector);
            } catch (RuntimeException e) {
                // ignore and continue with next
            }
        }
        return res;
    }
}
