package fish.payara.samples.rolesallowed.unprotected.methods;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.URL;

/**
 * @author Andrew Pielage <andrew.pielage@payara.fish>
 */
@RunWith(Arquillian.class)
public class HelloServletIT {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "rolesallowed-unprotected-methods.war")
                .addPackage("fish.payara.samples.rolesallowed.unprotected.methods");
    }

    @Test
    public void invokeServletIT() {
        Response response = ClientBuilder.newClient().target(url + "/" + "sayhello").request().get();
        String responseString = response.readEntity(String.class);
        Assert.assertTrue(responseString.contains("Hello ANONYMOUS"));
        Assert.assertTrue(!responseString.contains("Managed to get access!"));
    }

}