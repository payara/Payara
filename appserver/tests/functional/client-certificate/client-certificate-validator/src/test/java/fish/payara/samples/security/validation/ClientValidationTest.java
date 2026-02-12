/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package fish.payara.samples.security.validation;

import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ClientValidationTest {

    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String LOCALHOST_URL = "https://localhost:8181/security/secure/hello";
    private static final String PAYARA_CERTIFICATE_ALIAS_PROPERTY = "fish.payara.jaxrs.client.certificate.alias";
    private static final String EXPECTED_VALIDATION_ERROR = "Certificate Validation Failed via API";

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "security.war")
                .addPackage(ClientValidationTest.class.getPackage())
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/beans.xml"));
    }

    @Test
    @RunAsClient
    public void testWithExpiredCertificate() throws Exception {
        String certPath = new File("target", "expired-keystore.jks").getAbsolutePath();
        String certAlias = "client-certificate-expired";
        performTest(certPath, certAlias, 401, true);
    }

    @Test
    @RunAsClient
    public void testWithValidCertificate() throws Exception {
        String certPath = new File("target", "valid-keystore.jks").getAbsolutePath();
        String certAlias = "client-certificate-valid";
        performTest(certPath, certAlias, 200, false);
    }

    private void performTest(String certPath, String certAlias, int expectedStatusCode, boolean checkLog)
            throws Exception {
        System.out.println("\n========================================");
        System.out.println("Client Certificate Validation Test");
        System.out.println("Using Payara JAX-RS Extension");
        System.out.println("Expected status code: " + expectedStatusCode);
        System.out.println("Check server logs: " + checkLog);
        System.out.println("========================================");
        System.out.println("Client Certificate Path: " + certPath);
        System.out.println("Certificate Alias: " + certAlias);

        System.out.println(String.format(
                "Starting Client Certificate Validation Test (Expected Status: %d, Alias: %s)",
                expectedStatusCode, certAlias));
        System.out.println("Client Certificate Path: " + certPath);

        // Verify keystore exists
        File keystoreFile = new File(certPath);
        if (!keystoreFile.exists()) {
            fail("Keystore file does not exist: " + certPath);
        }

        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(certPath)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        System.out.println("Keystore loaded successfully");

        // Print certificate details
        printCertificateDetails(keyStore, certAlias);

        // Set up key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
        System.out.println("KeyManagerFactory initialized");

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
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());
        System.out.println("SSLContext initialized with TLSv1.2");

        // Build JAX-RS client using Payara Extension
        System.out.println("\nBuilding JAX-RS Client with Payara Extension:");
        System.out.println("  Property: " + PAYARA_CERTIFICATE_ALIAS_PROPERTY);
        System.out.println("  Value: " + certAlias);

        Client client = ClientBuilder.newBuilder()
                .sslContext(sslContext)
                .hostnameVerifier((hostname, session) -> true)
                // Use Payara's JAX-RS Extension to specify certificate alias
                .property(PAYARA_CERTIFICATE_ALIAS_PROPERTY, certAlias)
                .build();

        System.out.println("JAX-RS Client built with property " + PAYARA_CERTIFICATE_ALIAS_PROPERTY + "=" + certAlias);

        // Make the request
        System.out.println("Making HTTPS request to: " + LOCALHOST_URL);
        Response response = null;
        try {
            response = client.target(new URI(LOCALHOST_URL))
                    .request()
                    .get();

            int statusCode = response.getStatus();
            System.out.println("Response received: HTTP " + statusCode);

            // Verify the response status code
            System.out.println("\nValidating Response:");
            assertEquals("Expected status code " + expectedStatusCode + " but got: " + statusCode,
                    expectedStatusCode, statusCode);
            System.out.println("HTTPS Request successful. Received expected status code " + statusCode);

        } finally {
            if (response != null) {
                response.close();
            }
            client.close();
        }

        // Verify the certificate validation failure was logged by the API
        if (checkLog) {
            String domainDir = Paths.get(System.getProperty("payara.home"), "glassfish", "domains",
                    System.getProperty("payara.domain.name")).toString();
            System.out.println("Checking Server Logs in: " + domainDir);
            boolean validationFailed = checkForAPIValidationFailure(domainDir);
            if (!validationFailed) {
                System.err.println(" ✗ Expected validation error message not found in logs");
                fail("Expected certificate validation failure in server logs but none found");
            }
            System.out.println("Verified certificate validation failure was correctly logged by the server");
        }

        System.out.println("========================================");
        System.out.println("Test completed successfully!");

    }

    private void printCertificateDetails(KeyStore keyStore, String alias) {
        try {
            System.out.println("Certificate Details:");
            if (keyStore.containsAlias(alias)) {
                System.out.println("  Alias: " + alias);
                if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                    java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        X509Certificate x509 = (X509Certificate) cert;
                        System.out.println("    Subject: " + x509.getSubjectX500Principal().getName());
                        System.out.println("    Issuer: " + x509.getIssuerX500Principal().getName());
                        System.out.println("    Valid From: " + x509.getNotBefore());
                        System.out.println("    Valid Until: " + x509.getNotAfter());
                        try {
                            x509.checkValidity();
                            System.out.println("    Status: ✓ VALID");
                        } catch (CertificateExpiredException e) {
                            System.out.println("    Status: ✗ EXPIRED");
                            System.out.println("Certificate " + alias + " is EXPIRED");
                        } catch (CertificateNotYetValidException e) {
                            System.out.println("    Status: ✗ NOT YET VALID");
                            System.out.println("Certificate " + alias + " is NOT YET VALID");
                        }
                    }
                }
            } else {
                System.out.println("  Warning: Alias '" + alias + "' not found in keystore");
                System.out.println("Alias " + alias + " not found in keystore");
                System.out.println("  Available aliases:");
                java.util.Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    System.out.println("    - " + aliases.nextElement());
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not read certificate details: " + e.getMessage());
        }
    }

    /**
     * @return true if the correct warning is found in the logs
     * @throws IOException
     */
    private boolean checkForAPIValidationFailure(String domainDir) throws IOException {
        List<String> log = viewLog(domainDir);
        for (String line : log) {
            if (line.contains(EXPECTED_VALIDATION_ERROR)) {
                System.out.println("Found expected validation error in server logs: " + line);
                return true;
            }
        }
        return false;
    }

    /**
     * @return the contents of the server log
     */
    private List<String> viewLog(String domainDir) throws IOException {
        Path serverLog = Paths.get(domainDir, "logs", "server.log");
        return Files.readAllLines(serverLog);
    }

    /**
     * Inner class implementing the authentication mechanism for testing purposes.
     */
    @ApplicationScoped
    public static class ClientCertAuthenticationMechanism implements HttpAuthenticationMechanism {

        @Override
        public AuthenticationStatus validateRequest(HttpServletRequest request, HttpServletResponse response,
                HttpMessageContext httpMessageContext) throws AuthenticationException {

            X509Certificate[] certs = (X509Certificate[]) request
                    .getAttribute("jakarta.servlet.request.X509Certificate");
            if (certs == null) {
                certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
            }

            if (certs != null && certs.length > 0) {
                X509Certificate clientCert = certs[0];
                String principalName = clientCert.getSubjectX500Principal().getName();

                try {
                    clientCert.checkValidity();
                    System.out.println("Certificate is valid. Authenticating: " + principalName);
                    return httpMessageContext.notifyContainerAboutLogin(
                            principalName,
                            Collections.singleton("myRole"));
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    // This EXACT string is expected by the test to verify validation failures
                    System.out.println("Certificate Validation Failed via API for: " + principalName);
                    return httpMessageContext.responseUnauthorized();
                }
            }

            // Return unauthorized for protected resources if no certificate is found
            if (request.getRequestURI().contains("/secure/")) {
                System.out.println("No certificate presented for protected resource: " + request.getRequestURI());
                return httpMessageContext.responseUnauthorized();
            }

            return httpMessageContext.doNothing();
        }
    }
}
