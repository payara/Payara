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
import fish.payara.samples.SecurityUtils;
import fish.payara.samples.ServerOperations;
import fish.payara.samples.SincePayara;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omnifaces.utils.security.Certificates;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.net.URL;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 *
 * @author James Hillyard
 */

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ClientValidationTest {

    private static String certPath;

    private static final String CERTIFICATE_ALIAS = "omnikey";
    private static final String KEYSTORE_PASSWORD = "changeit";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String LOCALHOST_URL = "https://localhost:8181/security/secure/hello";
    private static final String EXPECTED_VALIDATION_ERROR = "Certificate Validation Failed via API";
    private static final Logger logger = Logger.getLogger(ClientValidationTest.class.getName());

   @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "security.war")
                .addPackage(ClientValidationTest.class.getPackage())
                .addPackages(true, "org.bouncycastle")
                .addPackages(true, "com.gargoylesoftware")
                .addPackages(true, "net.sourceforge.htmlunit")
                .addPackages(true, "org.eclipse")
                .addPackages(true, PayaraArquillianTestRunner.class.getPackage())
                .addClasses(ServerOperations.class, SecurityUtils.class, Certificates.class)
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/web.xml"))
                .addAsWebInfResource(new File("src/main/webapp", "WEB-INF/beans.xml"));
    }

    @Test
    @InSequence(1)
    public void generateCertsInTrustStore() throws Exception {
        if (ServerOperations.isServer()) {
            certPath = ServerOperations.generateClientKeyStore(true, true, CERTIFICATE_ALIAS);

            // Verify keystore was created
            if (certPath == null || !new File(certPath).exists()) {
                throw new IOException("Failed to generate client keystore at: " + certPath);
            }

            try {
                // Load the keystore
                KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
                try (FileInputStream fis = new FileInputStream(certPath)) {
                    keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
                }

                // Set up key manager factory with PKCS12
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

                // Set up trust manager that trusts all certificates (for testing only)
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            public X509Certificate[] getAcceptedIssuers() {
                                return null;
                            }
                            public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                            public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                        }
                };

                // Initialize SSL context
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
                sslContext.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());

                // Set as default
                SSLContext.setDefault(sslContext);
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

                // For testing, accept all hostnames
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to configure SSL context: " + e.getMessage(), e);
                throw e;
            }
        }
    }

    @Test
    @InSequence(2)
    public void validationFailTest() throws Exception {
        // Configure SSL system properties
        String domainDir = System.getProperty("com.sun.aas.instanceRoot");
        String keystorePath = domainDir + "/config/keystore.p12";
        String truststorePath = domainDir + "/config/cacerts.p12";

        // Set all system properties for SSL at once
        System.setProperty("javax.net.ssl.keyStore", keystorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.keyStoreType", KEYSTORE_TYPE);
        System.setProperty("javax.net.ssl.trustStore", truststorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", KEYSTORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", KEYSTORE_TYPE);
        System.setProperty("javax.net.debug", "all");
        System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
        System.setProperty("https.protocols", "TLSv1.2");

        // Verify keystore exists and is readable
        File keystoreFile = new File(certPath);
        if (!keystoreFile.exists()) {
            fail("Keystore file does not exist: " + certPath);
        }
        if (!keystoreFile.canRead()) {
            fail("Cannot read keystore file: " + certPath);
        }

        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(certPath)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }

        // Set up key manager factory with PKCS12
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());

        // Create a trust manager that trusts all certificates (for testing only)
        TrustManager[] trustAllCerts = new TrustManager[]{
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

        // Initialize SSL context with the trust managers using TLSv1.2
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());

        // Get the socket factory and make the call
        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
        int statusCode = callEndpoint(sslSocketFactory);

        // Verify the response status code
        if (statusCode != 401) {
            fail("Expected status code 401 but got: " + statusCode);
        }

        // Verify the certificate validation failure was logged by the API
        boolean validationFailed = checkForAPIValidationFailure();
        if (!validationFailed) {
            fail("Expected certificate validation failure in server logs but none found");
        }
    };

    private static int callEndpoint(SSLSocketFactory sslSocketFactory) throws IOException {
        HttpsURLConnection connection = null;
        int responseCode = 500; // Default error code

        try {
            try {
                URL url = new URI(LOCALHOST_URL).toURL();
                connection = (HttpsURLConnection) url.openConnection();
            } catch (java.net.URISyntaxException e) {
                throw new IOException("Invalid URL: " + LOCALHOST_URL, e);
            }

            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setHostnameVerifier((hostname, session) -> true);

            // Set request method and headers
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Connection", "close");
            connection.setDoOutput(true);

            // Make the request and read the response
            responseCode = connection.getResponseCode();

            // Read the response
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }

                String responseBody = response.toString();
                logger.log(Level.FINER, "Response: {0}", responseBody);

                return responseCode;
            }
        } catch (SSLException e) {
            // Log SSL errors
            if (e instanceof SSLHandshakeException) {
                logger.log(Level.SEVERE, "SSL Handshake Failed: {0}", e.getMessage());
            } else {
                logger.log(Level.SEVERE, "SSL Error: {0}", e.getMessage());
            }
            logger.log(Level.FINER, "SSL Error details", e);

            // Try to get more error details if connection is available
            if (connection != null) {
                try (InputStream es = connection.getErrorStream()) {
                    if (es != null) {
                        try (BufferedReader in = new BufferedReader(new InputStreamReader(es))) {
                            String inputLine;
                            StringBuilder errorResponse = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                errorResponse.append(inputLine);
                            }
                            if (errorResponse.length() > 0) {
                                logger.log(Level.SEVERE, "Error response: {0}", errorResponse.toString());
                            }
                        }
                    }
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Could not read error response: {0}", ex.getMessage());
                }
            }

            // If we have a response code, use it, otherwise rethrow the exception
            if (connection != null) {
                try {
                    return connection.getResponseCode();
                } catch (IOException ex) {
                    // If we can't get the response code, rethrow the original exception
                    throw e;
                }
            } else {
                throw e;
            }

        } finally {
            // Ensure the connection is closed
            if (connection != null) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error disconnecting: {0}", e.getMessage());
                }
            }

            return responseCode;
        }
    }

    /**
     * @return true if the correct warning is found in the logs
     * @throws IOException
     */
    public boolean checkForAPIValidationFailure() throws IOException {
        List<String> log = viewLog();
        for (String line : log) {
            if (line.contains(EXPECTED_VALIDATION_ERROR)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the contents of the server log
     */
    private List<String> viewLog() throws IOException {
        Path serverLog = ServerOperations.getDomainPath("logs/server.log");
        return Files.readAllLines(serverLog);
    }
}