/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.samples.rolespermitted;

import static fish.payara.samples.rolespermitted.IdentityStoreTest.ADMIN_USER;
import static fish.payara.samples.rolespermitted.IdentityStoreTest.PASSWORD;
import static fish.payara.samples.rolespermitted.IdentityStoreTest.STANDARD_USER;
import static java.lang.String.format;
import static jakarta.json.JsonValue.NULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.logging.Level;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import fish.payara.samples.PayaraArquillianTestRunner;

/**
 *
 * @author Susan Rai
 */
@RunWith(PayaraArquillianTestRunner.class)
public class RolesPermittedTest {

    private static final String WEBAPP_SOURCE = "src/main/webapp";

    private static WebClient WEB_CLIENT;

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "rolesPermitted.war")
                .addPackage("fish.payara.samples.rolespermitted")
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/web.xml"))
                .addAsWebInfResource(new File(WEBAPP_SOURCE, "WEB-INF/glassfish-web.xml"));
    }

    @BeforeClass
    public static void setUp() throws InterruptedException {
        WEB_CLIENT = new WebClient();
        //prevent spurious 404 errors
        WEB_CLIENT.getOptions().setThrowExceptionOnFailingStatusCode(false);
        java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
    }

    @Test
    @RunAsClient
    public void testAuthenticationWithInvalidUser() {
        WebResponse result = getResponse("wrongUser", PASSWORD);
        assertEquals(401, result.getStatusCode());
    }

    @Test
    @RunAsClient
    @InSequence(1)
    public void testAuthenticationWithAdminUser() {
        JsonObject result = getJsonResponse(ADMIN_USER, PASSWORD);
        assertNotNull("Invalid response", result);
        assertTrue("User doesn't have the correct role", result.getBoolean("payaraAdmin"));
        assertEquals(1, result.getInt("counter"));
    }

    @Test
    @RunAsClient
    @InSequence(2)
    public void testAdminUserSession() {
        JsonObject result = getJsonResponse(ADMIN_USER, PASSWORD);
        assertNotNull("Invalid response", result);
        assertTrue("User doesn't have the correct role", result.getBoolean("payaraAdmin"));
        assertEquals(2, result.getInt("counter"));
    }

    @Test
    @RunAsClient
    public void testAuthenticationWithStandardUser() {
        JsonObject result = getJsonResponse(STANDARD_USER, PASSWORD);
        assertNotNull("Invalid response", result);
        assertFalse("User doesn't have the correct role", result.getBoolean("payaraAdmin"));
        assertEquals(NULL, result.get("counter"));
    }

    private JsonObject getJsonResponse(String username, String password) {
        WebResponse response = getResponse(username, password);
        if (response != null) {
            try (JsonReader reader = Json.createReader(response.getContentAsStream())) {
                JsonObject result = reader.readObject();
                System.out.println(result);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println(response.getContentAsString());
            }
        }
        return null;
    }

    private WebResponse getResponse(String username, String password) {
        try {
            return WEB_CLIENT.getPage(format("%s/testServlet?username=%s&password=%s", this.url, username, password)).getWebResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @AfterClass
    public static void cleanUp() {
        WEB_CLIENT.getCookieManager().clearCookies();
        WEB_CLIENT.close();
    }
}
