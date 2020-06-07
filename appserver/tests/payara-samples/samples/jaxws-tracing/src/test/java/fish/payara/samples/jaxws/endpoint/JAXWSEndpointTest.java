package fish.payara.samples.jaxws.endpoint;

import static fish.payara.samples.CliCommands.payaraGlassFish;

import java.net.URL;

import javax.inject.Inject;
import javax.xml.ws.Service;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import fish.payara.samples.PayaraTestShrinkWrap;

public abstract class JAXWSEndpointTest {

    protected Service jaxwsEndPointService;
    
    @Inject
    private TraceMonitor traceMonitor;

    @ArquillianResource
    protected URL url;

    public static WebArchive createBaseDeployment() {
        return PayaraTestShrinkWrap
                .getWebArchive()
                .addPackage(JAXWSEndpointTest.class.getPackage());
    }

    public boolean isTraceMonitorTriggered() {
        return traceMonitor.isObserverCalled();
    }

    @BeforeClass
    public static void enableRequesttracing() {
        payaraGlassFish(
            "set-requesttracing-configuration",
            "--thresholdValue=25",
            "--enabled=true",
            "--target=server-config",
            "--thresholdUnit=MICROSECONDS",
            "--dynamic=true"
        );
        
        payaraGlassFish(
            "notification-cdieventbus-configure",
            "--loopBack=true",
            "--dynamic=true",
            "--enabled=true",
            "--hazelcastEnabled=true"
        );
    }
    
    @AfterClass
    public static void disableRequestTracing() {
        payaraGlassFish(
            "set-requesttracing-configuration",
            "--enabled=false",
            "--dynamic=true"
        );

        payaraGlassFish(
            "notification-cdieventbus-configure",
            "--enabled=false",
            "--dynamic=true"
        );
    }

}