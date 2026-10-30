/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018-2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.oidc.test;

import com.gargoylesoftware.htmlunit.TextPage;
import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.security.oidc.client.Callback;
import fish.payara.security.oidc.client.GetUserName;
import fish.payara.security.oidc.client.UnsecuredPage;
import fish.payara.security.oidc.client.defaulttests.SecuredPage;
import fish.payara.security.oidc.server.ApplicationConfig;
import fish.payara.security.oidc.server.OidcProvider;
import java.io.IOException;
import java.net.URL;
import jakarta.ws.rs.core.Response;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Gaurav Gupta
 * @author Jonathan
 */
public class OpenIdTestUtil {

    public static WebArchive createServerDeployment() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-server.war")
                .addClass(OidcProvider.class)
                .addClass(ApplicationConfig.class)
                .addAsResource("openid-configuration.json")
                .addAsWebInfResource("all-beans.xml", "beans.xml");
        return war;
    }

    public static WebArchive createClientDeployment() {
        WebArchive war = ShrinkWrap
                .create(WebArchive.class, "openid-client.war")
                .addClass(Callback.class)
                .addClass(UnsecuredPage.class)
                .addClass(GetUserName.class)
                .addAsWebInfResource("all-beans.xml", "beans.xml");
        return war;
    }

    public static WebArchive createClientDefaultDeployment() {
        return createClientDeployment().addClass(SecuredPage.class);

    }


    public static void testOpenIdConnect2Tenants(URL base, WebClient webClient1, WebClient webClient2) throws IOException {
        // authenticate first client with employee tenant
        String result = ((TextPage) webClient1.getPage(base + "Secured?tenant=employee")).getContent();
        result = ((TextPage) webClient1.getPage(base + "Username")).getContent();
        assertEquals("employee", result);

        // authenticate the second client with dealer tenant
        result = ((TextPage) webClient2.getPage(base + "Secured?tenant=dealer")).getContent();
        result = ((TextPage) webClient2.getPage(base + "Username")).getContent();
        assertEquals("dealer", result);
    }

    public static void testOpenIdConnect(WebClient webClient, URL base) throws IOException {
        // unsecure page should be accessible for an unauthenticated user
        TextPage unsecuredPage = (TextPage) webClient.getPage(base + "Unsecured");
        assertEquals(Response.Status.OK.getStatusCode(), unsecuredPage.getWebResponse().getStatusCode());
        assertEquals("This is an unsecured web page", unsecuredPage.getContent().trim());

        // access to secured web page authenticates the user and instructs to redirect to the callback URL
        TextPage securedPage = (TextPage) webClient.getPage(base + "Secured");
        assertEquals(Response.Status.OK.getStatusCode(), securedPage.getWebResponse().getStatusCode());
        assertEquals(String.format("%sCallback", base.getPath()), securedPage.getUrl().getPath());

        // access secured web page as an authenticated user
        securedPage = (TextPage) webClient.getPage(base + "Secured");
        assertEquals(Response.Status.OK.getStatusCode(), securedPage.getWebResponse().getStatusCode());
        assertEquals("This is a secured web page", securedPage.getContent().trim());

        // finally, access should still be allowed to an unsecured web page when already logged in
        unsecuredPage = ((TextPage) webClient.getPage(base + "Unsecured"));
        assertEquals(Response.Status.OK.getStatusCode(), unsecuredPage.getWebResponse().getStatusCode());
        assertEquals("This is an unsecured web page", unsecuredPage.getContent().trim());
    }

}
