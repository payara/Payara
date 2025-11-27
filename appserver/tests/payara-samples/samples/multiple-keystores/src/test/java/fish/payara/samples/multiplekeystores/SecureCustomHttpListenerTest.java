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
package fish.payara.samples.multiplekeystores;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.samples.CliCommands;
import fish.payara.samples.SecurityUtils;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class SecureCustomHttpListenerTest {

    private static final Logger logger = Logger.getLogger(SecureCustomHttpListenerTest.class.getName());
    private static final int MAX_RETRIES = 30;
    private static final int RETRY_DELAY_MS = 1000;

    private static WebClient WEB_CLIENT;
    private static final String NEW_LISTENER_URL = "https://localhost:8282";
    private static final String HTTP_LISTENER_TWO_URL = "https://localhost:8181";

    @BeforeClass
    public static void setUp() {
        try {
            KeyPair clientKeyPair = SecurityUtils.generateRandomRSAKeys();
            X509Certificate clientCertificate = SecurityUtils.createSelfSignedCertificate(clientKeyPair);
            String path = SecurityUtils.createTempJKSKeyStore(clientKeyPair.getPrivate(), clientCertificate);

            //Used so the path is correct when run in the cli command
            path = path.replace("\\", "\\\\");
            path = path.replace(":", "\\:");

            CliCommands.payaraGlassFish("create-jvm-options", "\"-Dfish.payara.ssl.additionalKeyStores=" + path + "\"");

            // Set up the HTTP client
            WEB_CLIENT = new WebClient();
            WEB_CLIENT.getOptions().setThrowExceptionOnFailingStatusCode(false);
            WEB_CLIENT.getOptions().setUseInsecureSSL(true);

            // Log the test configuration
            logger.info("Test configuration:");
            logger.info("- Using NEW_LISTENER_URL: " + NEW_LISTENER_URL);
            logger.info("- Using HTTP_LISTENER_TWO_URL: " + HTTP_LISTENER_TWO_URL);

            // Execute asadmin commands to set up the HTTP listener
            CliCommands.payaraGlassFish("create-protocol", "--securityenabled=true", "--target=server-config", "wibbles-protocol");
            CliCommands.payaraGlassFish("create-http", "--defaultVirtualServer=server", "--target=server-config", "wibbles-protocol");
            CliCommands.payaraGlassFish("create-network-listener", "--address=0.0.0.0", "--listenerport=8282", "--protocol=wibbles-protocol", "wibbles");
            CliCommands.payaraGlassFish("set", "configs.config.server-config.network-config.protocols.protocol.wibbles-protocol.ssl.cert-nickname=omnikey");

            // Restart the domain to apply changes
            CliCommands.payaraGlassFish("restart-domain");

            // Wait for the server to be ready
            waitForServer(8282);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to set up test environment: " + e.getMessage(), e);
            throw new RuntimeException("Test setup failed", e);
        }
    }

    @Test
    public void secureHttpListenerTwoWithDefaultCert() throws IOException {
        WebResponse webResponse = WEB_CLIENT.getPage(HTTP_LISTENER_TWO_URL).getWebResponse();
        assertNotNull(webResponse);
        assertEquals("Status code should be 200", 200, webResponse.getStatusCode());
    }

    @Test
    public void secureNewHttpListenerWithAdditionalCert() throws IOException {
        WebResponse webResponse = WEB_CLIENT.getPage(NEW_LISTENER_URL).getWebResponse();
        assertNotNull(webResponse);
        assertEquals("Status code should be 200", 200, webResponse.getStatusCode());
    }

    private static void waitForServer(int port) throws InterruptedException {
        logger.info("Waiting for server to be ready on port " + port + "...");
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                java.net.Socket socket = new java.net.Socket("localhost", port);
                socket.close();
                logger.info("Server is ready on port " + port);
                return;
            } catch (IOException e) {
                retries++;
                if (retries >= MAX_RETRIES) {
                    throw new RuntimeException("Server did not start within the expected time");
                }
                logger.info("Waiting for server to start... (" + retries + "/" + MAX_RETRIES + ")");
                Thread.sleep(RETRY_DELAY_MS);
            }
        }
    }
}
