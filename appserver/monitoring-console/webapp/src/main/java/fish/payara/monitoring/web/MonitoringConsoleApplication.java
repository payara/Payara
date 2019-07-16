package fish.payara.monitoring.web;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

@ApplicationPath("/")
public class MonitoringConsoleApplication extends Application {
    // required to trigger JAX-RS
}