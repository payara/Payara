package fish.payara.functional.server.security.client.cert;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.function.Consumer;
import java.util.logging.Logger;

public abstract class BaseClientCertTest {

    private static final Logger LOGGER = Logger.getLogger(BaseClientCertTest.class.getName());

    private static final String PAYARA_CERTIFICATE_ALIAS_PROPERTY = "fish.payara.jaxrs.client.certificate.alias";
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String KEYSTORE_PASSWORD = "changeit";

    public void sendRequest(String certPath, String certAlias, URI uri, Consumer<Response> consumer)
            throws Exception {

        KeyManagerFactory kmf = initKeyManagerFactory(certPath, certAlias);

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
        LOGGER.info("Initializing SSL Context with TLSv1.2");
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(kmf.getKeyManagers(), trustAllCerts, new SecureRandom());

        // Build JAX-RS client using Payara Extension
        LOGGER.info("\nBuilding JAX-RS Client with Payara Extension:");
        LOGGER.info("  Property: " + PAYARA_CERTIFICATE_ALIAS_PROPERTY);
        LOGGER.info("  Value: " + certAlias);

        // Make the request
        LOGGER.info("Making HTTPS request to: " + uri);
        try (
                Client client = ClientBuilder.newBuilder()
                        .sslContext(sslContext)
                        .hostnameVerifier((hostname, session) -> true)
                        // Use Payara's JAX-RS Extension to specify certificate alias
                        .property(PAYARA_CERTIFICATE_ALIAS_PROPERTY, certAlias)
                        .build();

                Response response =
                        client.target(uri).request().get()) {

            consumer.accept(response);
        }
    }

    protected abstract void assertCertificate(String alias, X509Certificate cert);

    protected void checkCertificate(KeyStore keyStore, String alias) throws GeneralSecurityException {
        if (keyStore.containsAlias(alias)) {
            // Print certificate details
            LOGGER.info("Certificate details of '" + alias + "':");
            if (keyStore.isCertificateEntry(alias) || keyStore.isKeyEntry(alias)) {
                if (keyStore.getCertificate(alias) instanceof X509Certificate x509) {
                    printCertificateDetails(x509);
                    assertCertificate(alias, x509);
                    return;
                }
            }
            throw new CertificateException("Certificate must be of type X.509");
        }
        else {
            LOGGER.info("Error: Alias '" + alias + "' not found in keystore");
            LOGGER.info("Alias " + alias + " not found in keystore");
            LOGGER.info("  Available aliases:");
            java.util.Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                LOGGER.info("    - " + aliases.nextElement());
            }
        }
    }

    private KeyManagerFactory initKeyManagerFactory(String certPath, String alias)
            throws GeneralSecurityException, IOException {

        // Verify keystore exists
        File keystoreFile = new File(certPath);
        if (!keystoreFile.exists()) {
            throw new FileNotFoundException("Keystore file does not exist: " + certPath);
        }

        // Load the keystore
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(certPath)) {
            keyStore.load(fis, KEYSTORE_PASSWORD.toCharArray());
        }
        LOGGER.info("Keystore loaded successfully");

        checkCertificate(keyStore, alias);

        // Set up key manager factory
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD.toCharArray());
        LOGGER.info("KeyManagerFactory initialized");

        return kmf;
    }

    protected void printCertificateDetails(X509Certificate cert) {
        LOGGER.info("Certificate Subject: " + cert.getSubjectX500Principal().getName());
        LOGGER.info("Certificate Issuer: " + cert.getIssuerX500Principal().getName());
        LOGGER.info("Certificate Not Before: " + cert.getNotBefore());
        LOGGER.info("Certificate Not After: " + cert.getNotAfter());
    }

}
