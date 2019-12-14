/** Copyright Payara Services Limited **/

package fish.payara.samples.jaccperapp;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenResolvedArtifact;
import org.junit.Test;
import org.junit.runner.RunWith;

import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This sample tests that we can install a custom JACC provider
 * using the Payara API.
 *
 * @author Arjan Tijms
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class InstallJaccProviderTest {

    @ArquillianResource
    private URL base;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        String pomPath = System.getProperty("pomPath");
        System.out.println(pomPath);
        assertNotNull("System property pomPath", pomPath);
        MavenResolvedArtifact jaccLibrary = Maven.resolver()
             .loadPomFromFile(pomPath)
             .resolve("org.omnifaces:jacc-provider")
             .withTransitivity()
             .asSingleResolvedArtifact();
        WebArchive archive =
            create(WebArchive.class)
                .addClasses(
                    JaccInstaller.class,
                    LoggingTestPolicy.class,
                    ProtectedServlet.class,
                    TestAuthenticationMechanism.class,
                    TestIdentityStore.class
                ).addAsLibraries(jaccLibrary.asFile())
                ;

        System.out.println("************************************************************");
        System.out.println(archive.toString(true));
        System.out.println("************************************************************");

        return archive;
    }

    @Test
    @RunAsClient
    public void testAuthenticated() throws IOException {

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "protected/servlet").toExternalForm()))
                     .queryParam("name", "test")
                     .queryParam("password", "secret")
                     .request(TEXT_PLAIN)
                     .get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        assertTrue(
            response.contains("web user has role \"a\": true")
        );

        // If these permissions are logged, our custom JACC provider has been invoked when the container was
        // checking for access to our protected servlet.

        assertTrue(
            response.contains("\"javax.security.jacc.WebResourcePermission\" \"/protected/servlet\" \"GET\"")
        );

        assertTrue(
            response.contains("\"javax.security.jacc.WebRoleRefPermission\" \"fish.payara.samples.jaccperapp.ProtectedServlet\" \"a\"")
        );

    }

    @Test
    @RunAsClient
    public void testNotAuthenticated() throws IOException {

        String response =
                newClient()
                     .target(
                         URI.create(new URL(base, "protected/servlet").toExternalForm()))
                     .request(TEXT_PLAIN)
                     .get(String.class);

        System.out.println("-------------------------------------------------------------------------");
        System.out.println("Response: \n\n" + response);
        System.out.println("-------------------------------------------------------------------------");

        assertFalse(response.contains("web user has role \"a\": true"));
    }

}
