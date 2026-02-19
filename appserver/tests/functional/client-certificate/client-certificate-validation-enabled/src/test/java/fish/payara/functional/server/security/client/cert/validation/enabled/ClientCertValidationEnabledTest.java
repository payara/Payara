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

package fish.payara.functional.server.security.client.cert.validation.enabled;

import fish.payara.functional.server.security.client.cert.BaseClientCertTest;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.SincePayara;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.ws.rs.core.Response;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.logging.Logger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PayaraArquillianTestRunner.class)
@SincePayara("5.2021.8")
public class ClientCertValidationEnabledTest extends BaseClientCertTest {

    private static final Logger LOGGER = Logger.getLogger(ClientCertValidationEnabledTest.class.getName());

    private static final String LOCALHOST_URL = "https://localhost:8181/security/secure/hello";
    private static final String EXPECTED_VALIDATION_ERROR = "Certificate Validation Failed via API";

    private static String domainDir = Paths.get(System.getProperty("payara.home"), "glassfish", "domains",
            System.getProperty("payara.domain.name")).toString();
    private static int nextLogLineNumber;

    @BeforeClass
    public static void beforeClass() throws IOException {
        nextLogLineNumber = viewLog().size();
    }

    @Deployment
    public static WebArchive deploy() {
        return ShrinkWrap.create(WebArchive.class, "security.war")
                .addClass(HelloServlet.class)
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

    @Override
    protected void assertCertificate(String alias, X509Certificate cert) {
        try {
            cert.checkValidity();
            if (alias.equals("client-certificate-expired")) {
                fail("Certificate 'Not After' should be in the past in order to test expired certificates.");
            }
            LOGGER.info("Status: ✓ VALID");
            return;
        } catch (CertificateExpiredException e) {
            LOGGER.info("Status: ✗ EXPIRED");
        } catch (CertificateNotYetValidException e) {
            LOGGER.info("Status: ✗ NOT YET VALID");
        } catch (Exception e) {
            LOGGER.info("Certificate Validity Check: " + e.getMessage());
        }

        if (alias.equals("client-certificate-valid")) {
            fail("Certificate 'Not After' should be in the future in order to test valid certificates.");
        }
    }

    private void performTest(String certPath, String certAlias, int expectedStatusCode, boolean expectErrorInLog)
            throws Exception {
        LOGGER.info("\n========================================");
        LOGGER.info("Client Certificate Validation Enabled Test");
        LOGGER.info("Using Payara JAX-RS Extension");
        LOGGER.info("Expected status code: " + expectedStatusCode);
        LOGGER.info("Expect error in log: " + expectErrorInLog);
        LOGGER.info("========================================");
        LOGGER.info("Client Certificate Path: " + certPath);
        LOGGER.info("Certificate Alias: " + certAlias);

        LOGGER.info(String.format(
                "Starting Client Certificate Validation Test (Expected Status: %d, Alias: %s)",
                expectedStatusCode, certAlias));
        LOGGER.info("Client Certificate Path: " + certPath);

        sendRequest(certPath, certAlias, new URI(LOCALHOST_URL), response -> assertResponse(response, expectedStatusCode));

        String domainDir = Paths.get(System.getProperty("payara.home"), "glassfish", "domains",
                System.getProperty("payara.domain.name")).toString();
        LOGGER.info("Checking Server Logs in: " + domainDir);

        // Verify the certificate validation failure was logged by the API
        boolean errorLogged = checkForAPIValidationFailure();

        if (expectErrorInLog) {
            assertTrue("Expected certificate validation failure in server logs but none found", errorLogged);
            LOGGER.info("Verified certificate validation failure was correctly logged by the server");
        }
        else {
            assertFalse("Did not expect certificate validation failure in server logs", errorLogged);
            LOGGER.info("No validation error logged by the server");
        }

        LOGGER.info("========================================");
        LOGGER.info("Test completed successfully!");
    }

    private static void assertResponse(Response response, int expectedStatusCode) {
        int statusCode = response.getStatus();
        LOGGER.info("Response received: HTTP " + statusCode);

        // Verify the response status code
        LOGGER.info("\nValidating Response:");
        assertEquals("Expected status code " + expectedStatusCode + " but got: " + statusCode,
                expectedStatusCode, statusCode);
        LOGGER.info("HTTPS Request successful. Received expected status code " + statusCode);
    }

    /**
     * @return true if the correct warning is found in the logs
     * @throws IOException
     */
    private boolean checkForAPIValidationFailure() throws IOException {
        List<String> log = viewLog();
        try {
            for (String line : log.subList(nextLogLineNumber, log.size())) {
                if (line.contains(EXPECTED_VALIDATION_ERROR)) {
                    LOGGER.info("Found a validation error in the server log at line " + line);
                    return true;
                }
            }
            return false;
        }
        finally {
            nextLogLineNumber = log.size();
        }
    }

    /**
     * @return the contents of the server log
     */
    private static List<String> viewLog() throws IOException {
        Path serverLog = Paths.get(domainDir, "logs", "server.log");
        return Files.readAllLines(serverLog);
    }
}
