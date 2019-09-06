/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
package fish.payara.samples.microprofile.endpoint.secure;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import fish.payara.samples.CliCommands;
import fish.payara.samples.ServerOperations;
import java.io.FileNotFoundException;
import java.io.IOException;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Gaurav Gupta
 */
@RunWith(Arquillian.class)
public class MicroprofileSecureEndpointTest {

    @ArquillianResource
    private URL base;
    
    private static String clientKeyStorePath;

    private WebClient webClient;
    private final DefaultCredentialsProvider correctCreds = new DefaultCredentialsProvider();
    private final DefaultCredentialsProvider incorrectCreds = new DefaultCredentialsProvider();

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        clientKeyStorePath = ServerOperations.createClientKeyStore();
        return create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public static void enableSecurity() {
        CliCommands.payaraGlassFish(asList("set-metrics-configuration", "--securityenabled", "true"));
        CliCommands.payaraGlassFish(asList("set-microprofile-healthcheck-configuration", "--securityenabled", "true"));
        CliCommands.payaraGlassFish(asList("set-openapi-configuration", "--securityenabled", "true"));
        ServerOperations.restartContainer();
    }

    @AfterClass
    public static void resetSecurity() {
        CliCommands.payaraGlassFish(asList("set-metrics-configuration", "--securityenabled", "false"));
        CliCommands.payaraGlassFish(asList("set-microprofile-healthcheck-configuration", "--securityenabled", "false"));
        CliCommands.payaraGlassFish(asList("set-openapi-configuration", "--securityenabled", "false"));
        ServerOperations.restartContainer();
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        webClient = new WebClient();
        correctCreds.addCredentials("mp", "mp");
        incorrectCreds.addCredentials("random", "random");
        ServerOperations.createClientTrustStore(webClient, base, clientKeyStorePath);
    }

    @After
    public void tearDown() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }

    @Test
    public void testMetricsWithCorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(correctCreds);
        Page page = webClient.getPage(base + "../metrics");
        assertEquals(SC_OK, page.getWebResponse().getStatusCode());
    }

    @Test
    public void testMetricsWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(base + "../metrics");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
            return;
        }

        fail("/metrics could be accessed without proper security credentials");
    }

    @Test
    public void testHealthCheckWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(base + "../health");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
            return;
        }

        fail("/health could be accessed without proper security credentials");
    }

    @Test
    public void testOpenAPIWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(base + "../openapi");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
            return;
        }

        fail("/openapi could be accessed without proper security credentials");
    }

}
