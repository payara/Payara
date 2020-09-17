package fish.payara.samples.jaxws.test;

import fish.payara.samples.PayaraTestShrinkWrap;
import fish.payara.samples.jaxws.endpoint.TraceMonitor;

import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.xml.ws.Service;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestName;

import static fish.payara.samples.CliCommands.payaraGlassFish;

public abstract class JAXWSEndpointTest {
    private static final Logger LOG = Logger.getLogger(JAXWSEndpointTest.class.getName());

    protected Service jaxwsEndPointService;

    @Inject
    private TraceMonitor traceMonitor;

    @Rule
    public TestName name = new TestName();

    @ArquillianResource
    protected URL url;

    public static WebArchive createBaseDeployment() {
        return PayaraTestShrinkWrap
                .getWebArchive()
                .addPackage(TraceMonitor.class.getPackage())
                .addClass(JAXWSEndpointTest.class);
    }

    public boolean isTraceMonitorTriggered() {
        return traceMonitor.isObserverCalled();
    }

    @BeforeClass
    public static void enableRequesttracing() throws Exception {
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

    @Before
    public void logStart() {
        LOG.log(Level.INFO, "Test method {0} started.", name.getMethodName());
    }

    @After
    public void logEnd() {
        LOG.log(Level.INFO, "Test method {0} finished.", name.getMethodName());
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