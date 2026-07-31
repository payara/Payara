/*
 *    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *    The contents of this file are subject to the terms of either the GNU
 *    General Public License Version 2 only ("GPL") or the Common Development
 *    and Distribution License("CDDL") (collectively, the "License").  You
 *    may not use this file except in compliance with the License.  You can
 *    obtain a copy of the License at
 *    https://github.com/payara/Payara/blob/main/LICENSE.txt
 *    See the License for the specific
 *    language governing permissions and limitations under the License.
 *
 *    When distributing the software, include this License Header Notice in each
 *    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *    GPL Classpath Exception:
 *    The Payara Foundation designates this particular file as subject to the "Classpath"
 *    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *    file that accompanied this code.
 *
 *    Modifications:
 *    If applicable, add the following below the License Header, with the fields
 *    enclosed by brackets [] replaced by your own identifying information:
 *    "Portions Copyright [year] [name of copyright owner]"
 *
 *    Contributor(s):
 *    If you wish your version of this file to be governed by only the CDDL or
 *    only the GPL Version 2, indicate your decision by adding "[Contributor]
 *    elects to include this software in this distribution under the [CDDL or GPL
 *    Version 2] license."  If you don't indicate a single choice of license, a
 *    recipient has the option to distribute your version of this file under
 *    either the CDDL, the GPL Version 2 or to extend the choice of license to
 *    its licensees as provided above.  However, if you add GPL Version 2 code
 *    and therefore, elected the GPL Version 2 license, then the option applies
 *    only if the new code is made subject to such option by the copyright
 *    holder.
 */
package fish.payara.samples.jaxws.security.servlet;

import fish.payara.samples.ServerOperations;
import fish.payara.samples.jaxws.security.JAXWSEndpointTest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.net.ssl.HttpsURLConnection;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Shared test logic for WS-Security UsernameToken with nonce
 * (WSS Username Token Profile 1.0, section 3.1).
 *
 * Subclasses supply the deployment via {@code @Deployment}, allowing the same
 * tests to run against two different server configurations:
 * <ul>
 *   <li>{@link NonceAuthHaEndpointTest} — deployment includes HaEnvironmentForcer,
 *       exercising HANonceManager and the BackingStoreFactoryRegistry "replicated" lookup.</li>
 *   <li>{@link NonceAuthEndpointTest} — standard deployment without HA forcing,
 *       exercising NonHANonceManager (local in-memory nonce cache).</li>
 * </ul>
 */
public abstract class NonceAuthEndpointTestBase extends JAXWSEndpointTest {

    protected static final String PASSWORD = "password";

    protected static final String PASSWORD_TEXT_TYPE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
    protected static final String PASSWORD_DIGEST_TYPE =
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest";

    private HttpsURLConnection serviceConnection;

    @Before
    public void setUp() throws MalformedURLException, KeyManagementException, NoSuchAlgorithmException {
        URL baseHttpsUrl = ServerOperations.toContainerHttps(baseUrl);
        serviceUrl = new URL(baseHttpsUrl, "CalculatorService");
        insecureSSLConfigurator.enableInsecureSSL();
    }

    @After
    public void cleanUp() {
        if (serviceConnection != null) {
            serviceConnection.disconnect();
            serviceConnection = null;
        }
        insecureSSLConfigurator.revertSSLConfiguration();
    }

    @Test
    @RunAsClient
    public void testValidNoncePasswordTextSucceeds() throws Exception {
        byte[] nonce = generateNonce();
        String created = currentTimestamp();

        serviceConnection = sendNonceRequest("tester", PASSWORD_TEXT_TYPE, PASSWORD, nonce, created);

        assertResponseOK(serviceConnection);
    }

    @Test
    @RunAsClient
    public void testWrongPasswordIsRejected() throws Exception {
        byte[] nonce = generateNonce();
        String created = currentTimestamp();

        serviceConnection = sendNonceRequest("tester", PASSWORD_TEXT_TYPE, "wrongpassword", nonce, created);

        assertResponseFailedWithMessage(serviceConnection, "Authentication of Username Password Token Failed");
    }

    @Test
    @RunAsClient
    public void testReplayedNonceIsRejected() throws Exception {
        byte[] nonce = generateNonce();
        String created = currentTimestamp();

        HttpsURLConnection first = sendNonceRequest("tester", PASSWORD_TEXT_TYPE, PASSWORD, nonce, created);
        assertResponseOK(first);
        first.disconnect();

        serviceConnection = sendNonceRequest("tester", PASSWORD_TEXT_TYPE, PASSWORD, nonce, created);
        assertTrue("Replayed nonce should be rejected with HTTP 5xx",
                serviceConnection.getResponseCode() >= 500);
    }

    /**
     * DefaultRealmAuthenticationAdapter does not support PasswordDigest because standard Java EE
     * realms do not expose stored plaintext passwords needed to verify the digest.
     *
     * This test acts as a canary: it passes while Metro rejects Digest requests (HTTP 5xx).
     * If Metro is upgraded to support PasswordDigest and this test starts failing with HTTP 200,
     * that is the signal to extend test coverage for the Digest authentication path.
     */
    @Test
    @RunAsClient
    public void testPasswordDigestIsRejected() throws Exception {
        byte[] nonce = generateNonce();
        String created = currentTimestamp();
        String digest = passwordDigest(nonce, created, PASSWORD);

        serviceConnection = sendNonceRequest("tester", PASSWORD_DIGEST_TYPE, digest, nonce, created);

        assertTrue("PasswordDigest should be rejected (HTTP 5xx) — if this fails with HTTP 200, "
                + "Metro now supports PasswordDigest and test coverage should be extended",
                serviceConnection.getResponseCode() >= 500);
    }

    // --- helpers ---

    private HttpsURLConnection sendNonceRequest(String username, String passwordType, String password,
            byte[] nonceBytes, String created) throws IOException {
        String nonce = Base64.getEncoder().encodeToString(nonceBytes);
        byte[] body = buildSoapEnvelope(username, passwordType, password, nonce, created)
                .getBytes(StandardCharsets.UTF_8);

        HttpsURLConnection conn = (HttpsURLConnection) serviceUrl.openConnection();
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));
        conn.setRequestMethod("POST");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.getOutputStream().write(body);
        return conn;
    }

    private static String buildSoapEnvelope(String username, String passwordType, String password,
            String nonce, String created) {
        return "<soapenv:Envelope"
                + " xmlns:calc=\"http://servlet.security.jaxws.samples.payara.fish/\""
                + " xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Header>"
                + "<wsse:Security soapenv:mustUnderstand=\"1\""
                + " xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""
                + " xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">"
                + "<wsse:UsernameToken>"
                + "<wsse:Username>" + username + "</wsse:Username>"
                + "<wsse:Password Type=\"" + passwordType + "\">" + password + "</wsse:Password>"
                + "<wsse:Nonce EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\">"
                + nonce + "</wsse:Nonce>"
                + "<wsu:Created>" + created + "</wsu:Created>"
                + "</wsse:UsernameToken>"
                + "</wsse:Security>"
                + "</soapenv:Header>"
                + "<soapenv:Body><calc:helloRestricted/></soapenv:Body>"
                + "</soapenv:Envelope>";
    }

    /**
     * Password_Digest = Base64(SHA-1(nonce + created + password))
     * per WSS Username Token Profile 1.0, section 3.1.
     * nonceBytes is the raw (pre-base64) bytes; created and password are UTF-8.
     */
    private static String passwordDigest(byte[] nonceBytes, String created, String password) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(nonceBytes);
            sha1.update(created.getBytes(StandardCharsets.UTF_8));
            sha1.update(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sha1.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-1 unavailable", e);
        }
    }

    private static byte[] generateNonce() {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    private static String currentTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(
                Instant.now().minusSeconds(5).truncatedTo(ChronoUnit.MILLIS));
    }
}
