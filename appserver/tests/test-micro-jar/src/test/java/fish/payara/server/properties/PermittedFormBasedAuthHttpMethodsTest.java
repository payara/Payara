package fish.payara.server.properties;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.admin.cli.AsAdminIntegrationTest;

/**
 * Verifies that the {@code fish.payara.permittedFormBasedAuthHttpMethods} system property has an effect.
 */
public class PermittedFormBasedAuthHttpMethodsTest extends AsAdminIntegrationTest {

    public PermittedFormBasedAuthHttpMethodsTest() {
        super("--deploy", "src/test/resources/formauth.war");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("fish.payara.permittedFormBasedAuthHttpMethods", "POST");
    }

    @AfterClass
    public static void shutdown() {
        System.clearProperty("fish.payara.permittedFormBasedAuthHttpMethods");
    }

    @Test
    public void formAuthIsForbiddenForMethodsNotPermitted() throws Exception {
        assertFormAuthResponse(HttpURLConnection.HTTP_FORBIDDEN, "GET");
        assertFormAuthResponse(HttpURLConnection.HTTP_FORBIDDEN, "HEAD");
        assertFormAuthResponse(HttpURLConnection.HTTP_FORBIDDEN, "PUT");
    }

    @Test
    public void formAuthIsOkForMethodsPermitted() throws Exception {
        assertFormAuthResponse(HttpURLConnection.HTTP_OK, "POST");
    }

    private void assertFormAuthResponse(int expectedStatus, String usedHttpMethod)
            throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL("http://localhost:" + server.getHttpPort()
                + "/formauth/j_security_check?j_username=foo&j_password=bar");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(usedHttpMethod);
        assertEquals(expectedStatus, connection.getResponseCode());
    }
}
