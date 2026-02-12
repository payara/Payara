package fish.payara.samples.security.expiration;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ExpirationValidationTest {

    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String LOCALHOST_URL = "https://localhost:8181/client-cert-expiration-test/secure/hello";

    private static final String PAYARA_CERTIFICATE_ALIAS_PROPERTY = "fish.payara.jaxrs.client.certificate.alias";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "client-cert-expiration-test.war")
                .addPackage(ExpirationValidationTest.class.getPackage())
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/glassfish-web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/beans.xml"));
    }

    @Test
    @RunAsClient
    public void testWithExpiredCertificate() throws Exception {
        String certPath = new File("target", "expired-keystore.p12").getAbsolutePath();
        String certAlias = "client-expired";

        System.out.println("\n--- Starting Expiration Validation Test ---");
        System.out.println("Property 'certificate-validation' should be 'false' at the server level.");
        System.out.println("Using Keystore: " + certPath);
        System.out.println("Using Expired Certificate Alias: " + certAlias);

        performRequest(certPath, certAlias);
    }

    private void performRequest(String certPath, String certAlias) throws Exception {
        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(certPath)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }

        // Print certificate details for debugging
        X509Certificate cert = (X509Certificate) keyStore.getCertificate(certAlias);
        if (cert != null) {
            System.out.println("Certificate Subject: " + cert.getSubjectX500Principal());
            System.out.println("Certificate Issuer: " + cert.getIssuerX500Principal());
            System.out.println("Certificate Not Before: " + cert.getNotBefore());
            System.out.println("Certificate Not After: " + cert.getNotAfter());
            try {
                cert.checkValidity();
                System.out.println("Certificate is CURRENTLY VALID (this might be unexpected if testing expiration).");
            } catch (Exception e) {
                System.out.println("Certificate Validity Check: " + e.getMessage());
            }
        } else {
            System.out.println("WARNING: Certificate with alias '" + certAlias + "' not found in keystore!");
        }

        // Set up key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        // Create a trust manager that trusts all certificates (for testing only)
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Initialize SSL context
        System.out.println("Initializing SSL Context with TLSv1.2");
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());

        // Build JAX-RS client using Payara Extension property for alias
        System.out.println("Building JAX-RS client with alias property: " + certAlias);
        Client client = ClientBuilder.newBuilder()
                .sslContext(sslContext)
                .hostnameVerifier((hostname, session) -> true)
                .property(PAYARA_CERTIFICATE_ALIAS_PROPERTY, certAlias)
                .build();

        // Make the request to the secure endpoint
        System.out.println("Requesting: " + LOCALHOST_URL);
        try (Response response = client.target(new URI(LOCALHOST_URL)).request().get()) {
            int statusCode = response.getStatus();
            System.out.println("Response Status: HTTP " + statusCode);
            System.out.println("Response Headers: " + response.getHeaders());

            // Assert that the request does NOT return 401 Unauthorized
            // If expiration validation was ENABLED at the server, it would return 401.
            // Since it is DISABLED, it should return 200 OK (if mapping works) or maybe 403
            if (statusCode == 401) {
                fail("Received HTTP 401 Unauthorized. The server likely rejected the expired certificate despite configuration.");
            }

            assertEquals("Expected status code 200 OK", 200, statusCode);

            String body = response.readEntity(String.class);
            System.out.println("Response Body: " + body);

            String expectedPrincipal = "CN=ExpiredClient";
            assertEquals("Hello " + expectedPrincipal, body);
            System.out.println("Assertion PASSED: Body contains expected principal " + expectedPrincipal);
        } catch (Exception e) {
            System.err.println("Request failed with exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            client.close();
            System.out.println("Client closed.");
        }
    }
}
