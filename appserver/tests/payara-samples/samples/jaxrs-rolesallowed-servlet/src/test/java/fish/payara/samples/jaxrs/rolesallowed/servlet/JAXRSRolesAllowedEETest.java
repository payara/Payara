/** Copyright Payara Services Limited **/

package fish.payara.samples.jaxrs.rolesallowed.servlet;

import fish.payara.samples.ServerOperations;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;

import jakarta.ws.rs.WebApplicationException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import static jakarta.ws.rs.client.ClientBuilder.newClient;
import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertTrue;

/**
 * This sample tests that we can install a custom JACC provider
 * using the Payara API.
 *
 * @author Arjan Tijms
 */
@RunWith(Arquillian.class)
public class JAXRSRolesAllowedEETest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        if (ServerOperations.isServer()) {
            ServerOperations.addUserToContainerIdentityStore("test", "a");
        }

        WebArchive archive =
            create(WebArchive.class)
            .addAsWebInfResource((new File("src/main/webapp/WEB-INF", "web.xml")))
                .addClasses(
                    JaxRsActivator.class,
                    Resource.class);

        System.out.println("************************************************************");
        System.out.println(archive.toString(true));
        System.out.println("************************************************************");

        return archive;
    }



    @Test
    @RunAsClient
    public void testAuthenticated() throws IOException {
        if (ServerOperations.isMicro()) {
            return;
        }

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "rest/resource/hi").toExternalForm()))
                     .request(TEXT_PLAIN)
                     .header("Authorization", "Basic " + Base64.getEncoder().encodeToString("test:secret".getBytes()))
                     .get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        assertTrue(
            response.contains("hi")
        );
    }

    @Test
    @RunAsClient
    public void testNotAuthenticated() throws IOException {

        try {
            String response =
                    newClient()
                         .target(
                             URI.create(new URL(base, "rest/resource/hi").toExternalForm()))
                         .request(TEXT_PLAIN)
                         .get(String.class);

            System.out.println("-------------------------------------------------------------------------");
            System.out.println("Response: \n\n" + response);
            System.out.println("-------------------------------------------------------------------------");

            assertTrue(
                !response.contains("hi")
            );
        } catch (WebApplicationException e) {
            System.out.println("-------------------------------------------------------------------------");
            System.out.println("Response headers: \n\n" + e.getResponse().getHeaders());
            System.out.println("-------------------------------------------------------------------------");
        }

    }

}
