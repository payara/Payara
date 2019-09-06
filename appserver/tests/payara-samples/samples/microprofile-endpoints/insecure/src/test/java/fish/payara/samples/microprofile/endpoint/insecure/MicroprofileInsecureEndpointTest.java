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
package fish.payara.samples.microprofile.endpoint.insecure;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import fish.payara.samples.CliCommands;
import fish.payara.samples.ServerOperations;
import static java.util.Arrays.asList;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.Matchers.containsString;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;

/**
 * @author Gaurav Gupta
 */
@RunWith(Arquillian.class)
public class MicroprofileInsecureEndpointTest {

    @ArquillianResource
    private URL base;

    private WebClient webClient;

    @Deployment(testable = false)
    public static WebArchive createDeployment() {

        return create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public static void enableSecurity() {
        CliCommands.payaraGlassFish(asList("set-metrics-configuration", "--endpoint", "mpmetrics"));
        CliCommands.payaraGlassFish(asList("set-microprofile-healthcheck-configuration", "--endpoint", "mphealth"));
        ServerOperations.restartContainer();
    }

    @AfterClass
    public static void resetSecurity() {
        CliCommands.payaraGlassFish(asList("set-metrics-configuration", "--endpoint", "metrics"));
        CliCommands.payaraGlassFish(asList("set-microprofile-healthcheck-configuration", "--endpoint", "health"));
        ServerOperations.restartContainer();
    }

    @Before
    public void setup() {
        webClient = new WebClient();
    }

    @After
    public void tearDown() {
        webClient.getCookieManager().clearCookies();
        webClient.close();
    }

    @Test
    public void testMetrics() throws Exception {
        Page page = webClient.getPage(base + "../mpmetrics");
        assertEquals(SC_OK, page.getWebResponse().getStatusCode());
    }

    @Test
    public void testHeatlhCheck() throws Exception {
        try {
            Page page = webClient.getPage(base + "../mphealth");
            assertEquals(SC_OK, page.getWebResponse().getStatusCode());
        } catch (FailingHttpStatusCodeException ex) {
            assertEquals(SC_SERVICE_UNAVAILABLE, ex.getStatusCode());
            assertThat(ex.getResponse().getContentAsString(), containsString("No Application deployed"));
        }
    }

}
