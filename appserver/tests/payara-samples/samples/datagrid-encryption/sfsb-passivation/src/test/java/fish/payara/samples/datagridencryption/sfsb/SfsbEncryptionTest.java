package fish.payara.samples.datagridencryption.sfsb;

import fish.payara.samples.ServerOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URL;

@RunWith(Arquillian.class)
public class SfsbEncryptionTest {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "sfsb-passivation.war")
                .addPackage("fish.payara.samples.datagridencryption.sfsb");
    }

    @BeforeClass
    public static void enableSecurity() {
        ServerOperations.enableDataGridEncryption();
    }

    @AfterClass
    public static void resetSecurity() {
        ServerOperations.disableDataGridEncryption();
    }

    @Test
    public void testStateRestoredAfterPassivation() {
        Client client = ClientBuilder.newClient();
        Invocation endpoint1 = client.target(url + "TestEjb").request().buildGet();
        WebTarget endpoint2 = client.target(url + "TestEjb/2");
        WebTarget endpoint3 = client.target(url + "TestEjb/Lookup");

        // First, poke endpoint1 twice to store some state
        Response response = endpoint1.invoke();
        Assert.assertEquals("apple,pear", response.readEntity(String.class));

        response = endpoint1.invoke();
        Assert.assertEquals("apple,pear,apple,pear", response.readEntity(String.class));

        // Next, poke endpoint2 three times to store some state
        endpoint2.request().get();
        endpoint2.request().get();
        response = endpoint2.request().get();
        Assert.assertEquals("bapple,care,bapple,care,bapple,care", response.readEntity(String.class));

        // Now force passivation by spamming lookup of 1200 EJBs
        endpoint3.request().get();
        endpoint3.request().get();

        // Check endpoint1  and endpoint2 have restored their state and added another set upon invocation
        response = endpoint1.invoke();
        Assert.assertEquals("apple,pear,apple,pear,apple,pear", response.readEntity(String.class));

        response = endpoint2.request().get();
        Assert.assertEquals("bapple,care,bapple,care,bapple,care,bapple,care", response.readEntity(String.class));
    }
}
