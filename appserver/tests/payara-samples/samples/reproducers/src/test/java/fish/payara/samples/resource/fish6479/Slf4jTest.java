package fish.payara.samples.resource.fish6479;

import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * bug reproducer for FISH-6479 and FISH-5981
 */
@RunWith(Arquillian.class)
public class Slf4jTest {

    @Deployment
    public static WebArchive createDeployment() {
        var war = ShrinkWrap.create(WebArchive.class, "slf4jtest.war")
                .addClasses(Slf4jResource.class, JaxrsApp.class)
                .addAsResource("simplelogger.properties")
                .addAsLibraries(
                        Maven.resolver()
                                .resolve("org.slf4j:slf4j-api:1.7.36",
                                        "org.slf4j:slf4j-simple:1.7.36")
                                .withTransitivity()
                                .asFile()
                )
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        System.out.println(war.toString(true));
        return war;
    }

    @ArquillianResource
    URI base;

    @Test
    public void testWarnLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("warn").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "WARN");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Warn message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testInfoLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("info").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "INFO");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Info message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testFatalLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("fatal").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "ERROR");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Fatal message ***"))
                .findAny()
                .isPresent());
    }

    @Test
    public void testDebugLogging() throws Exception {
        var response = ClientBuilder.newBuilder().build()
                .target(base).path("slf4j").path("debug").request().get();
        Assert.assertEquals(200, response.getStatus());
        String logFileName = System.getProperty("com.sun.aas.instanceRoot") + "/logs/server.log";
        List<String> logMessages = findLogMessages(logFileName, Slf4jResource.class.getName(), "DEBUG");
        Assert.assertTrue(logMessages.stream()
                .filter(p -> p.contains("*** Debug message ***"))
                .findAny()
                .isPresent());
    }

    private List<String> findLogMessages(String logFileName, String loggerName, String level) throws Exception {
        List<String> logMessages = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(logFileName));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(loggerName) && line.contains(level)) {
                logMessages.add(line);
            }
        }
        reader.close();
        return logMessages;
    }
}
