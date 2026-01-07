/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2019-2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.microprofile.endpoint.secure;

import com.gargoylesoftware.htmlunit.DefaultCredentialsProvider;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import fish.payara.samples.NotMicroCompatible;
import fish.payara.samples.PayaraArquillianTestRunner;
import fish.payara.samples.ServerOperations;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * @author Gaurav Gupta
 */
@RunWith(PayaraArquillianTestRunner.class)
@NotMicroCompatible
public class MicroprofileSecureEndpointTest {

    @ArquillianResource
    private URL base;
    private String serverBase;

    private static String clientKeyStorePath;

    private WebClient webClient;
    private final DefaultCredentialsProvider correctCreds = new DefaultCredentialsProvider();
    private final DefaultCredentialsProvider incorrectCreds = new DefaultCredentialsProvider();

    @Deployment(testable = false)
    public static WebArchive createDeployment() throws IOException {
        clientKeyStorePath = ServerOperations.createClientKeyStore();
        return create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Before
    public void setup() throws FileNotFoundException, IOException {
        webClient = new WebClient();
        correctCreds.addCredentials("mp", "mp");
        incorrectCreds.addCredentials("random", "random");
        serverBase = ServerOperations.createClientTrustStore(webClient, base, clientKeyStorePath).toString();
    }

    @After
    public void tearDown() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }

    @Test
    public void testMetricsWithCorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(correctCreds);
        Page page = webClient.getPage(serverBase + "../mpmetrics");
        assertEquals(SC_OK, page.getWebResponse().getStatusCode());
    }

    @Test
    public void testMetricsWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(serverBase + "../mpmetrics");
            fail("/metrics could be accessed without proper security credentials");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testHealthCheckWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(serverBase + "../mphealth");
            fail("/health could be accessed without proper security credentials");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
            return;
        }
    }

    @Test
    public void testOpenAPIWithIncorrectCredentials() throws Exception {
        webClient.setCredentialsProvider(incorrectCreds);

        try {
            webClient.getPage(serverBase + "../openapi");
            fail("/openapi could be accessed without proper security credentials");
        } catch (FailingHttpStatusCodeException e) {
            assertNotNull(e);
            assertEquals(SC_UNAUTHORIZED, e.getStatusCode());
        }
    }

}
