/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2024] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

package fish.payara.samples.headers;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jboss.arquillian.junit.InSequence;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;


import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.client.ClientBuilder.newClient;
import org.junit.Assert;

/**
 * @author Alfonso Valdez <alfonso.altamirano@payara.fish>
 */

@ExtendWith(ArquillianExtension.class)
public class PayaraValidationRFCHeadersIT {

    private static final Logger logger = Logger.getLogger(PayaraValidationRFCHeadersIT.class.getName());
    
    @ArquillianResource
    private URL base;

    private Client client;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "headers.war")
                .addPackage(SimpleServlet.class.getPackage());
    }

    @BeforeEach
    public void setup() {
        logger.info("setting client");
        this.client = newClient();
    }

    @Test
    @RunAsClient
    public void testInvalidLFHeader() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\n hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }

    @Test
    @RunAsClient
    public void testInvalidLFHeaderASCII() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\x0A hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }
    
    @Test
    @RunAsClient
    public void testInvalidNULHeader() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\0 hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }

    @Test
    @RunAsClient
    public void testInvalidNULHeaderASCII() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\x00 hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }

    @Test
    @RunAsClient
    public void testInvalidCRHeader() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\r hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }

    @Test
    @RunAsClient
    public void testInvalidCRHeaderASCII() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("InvalidValue", "\\x0D hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(400, headerResponse.getStatus());
        }
    }

    @Test
    @RunAsClient
    public void testValidHeader() throws Exception {
        logger.log(Level.INFO, " client: {0}, baseURL: {1}", new Object[]{client, base});
        final var headersTarget = this.client.target(new URL(this.base, "test").toExternalForm());
        try (final Response headerResponse = headersTarget.request()
                .accept(TEXT_PLAIN).header("ValidValue", "hello").get()) {
            logger.log(Level.INFO, " response: {0}", new Object[]{headerResponse});
            Assert.assertEquals(200, headerResponse.getStatus());
        }
    }
}