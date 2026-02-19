package fish.payara.functional.server.security.client.cert.validation.disabled;

import fish.payara.functional.server.security.client.cert.BaseClientCertTest;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.core.Response;
import java.io.File;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ClientCertValidationDisabledTest extends BaseClientCertTest {

    private static final Logger LOGGER = Logger.getLogger(ClientCertValidationDisabledTest.class.getName());

    private static final String LOCALHOST_URL = "https://localhost:8181/client-cert-expiration-test/secure/hello";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "client-cert-expiration-test.war")
                .addClass(HelloServlet.class)
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/glassfish-web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/beans.xml"));
    }

    @Test
    @RunAsClient
    public void testWithExpiredCertificate() throws Exception {
        String certPath = new File("target", "expired-keystore.p12").getAbsolutePath();
        String certAlias = "client-expired";

        LOGGER.info("\n--- Starting Client Certificate Validation Disabled Test ---");
        LOGGER.info("Property 'certificate-validation' should be 'false' at the server level.");
        LOGGER.info("Using Keystore: " + certPath);
        LOGGER.info("Using Expired Certificate Alias: " + certAlias);

        sendRequest(certPath, certAlias, new URI(LOCALHOST_URL), response -> assertResponse(response));
    }

    @Override
    protected void assertCertificate(String alias, X509Certificate cert) {
        try {
            cert.checkValidity();
            fail("Certificate 'Not After' should be in the past in order to test the relaxed validation.");
        } catch (Exception e) {
            LOGGER.info("Certificate Validity Check: " + e.getMessage());
        }
    }

    private static void assertResponse(Response response) {
        int statusCode = response.getStatus();
        LOGGER.info("Response Status: HTTP " + statusCode);
        LOGGER.info("Response Headers: " + response.getHeaders());

        // Assert that the request does NOT return 401 Unauthorized
        // If expiration validation was ENABLED at the server, it would return 401.
        // Since it is DISABLED, it should return 200 OK (if mapping works) or maybe 403
        if (statusCode == 401) {
            fail("Received HTTP 401 Unauthorized. The server likely rejected the expired certificate despite configuration.");
        }

        assertEquals("Expected status code 200 OK", 200, statusCode);

        String body = response.readEntity(String.class);
        LOGGER.info("Response Body: " + body);

        String expectedPrincipal = "CN=ExpiredClient";
        assertEquals("Hello " + expectedPrincipal, body);
        LOGGER.info("Assertion PASSED: Body contains expected principal " + expectedPrincipal);
    }
}
